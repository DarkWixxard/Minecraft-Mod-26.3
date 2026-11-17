# 18 – Eigene Tasten und Netzwerk-Pakete

Eine eigene Taste (z. B. **R** für einen Sprint-Sprung) betrifft beide Seiten:
- Die **Taste** gibt es nur beim **Client**.
- Die **Wirkung** (Spieler bewegen, Schaden, Items) muss der **Server** machen – sonst ist sie nicht echt und wird beim nächsten Abgleich zurückgesetzt.

Deshalb: Taste gedrückt → Client schickt ein **Paket** an den Server → Server führt aus.

```
Client: Taste R  ──DashPacket──▶  Server: Spieler nach vorne stoßen  ──Bewegung──▶  Client
```

> ✅ Getestet mit Forge 26.3-66.1.0: kompiliert, Server startet mit registriertem Kanal. Tastendruck und Paket brauchen einen echten Client und wurden nicht im Spiel ausgelöst.

## 1. Das Paket

```java
// Client -> Server: "der Spieler hat die Dash-Taste gedrückt". Enthält keine Daten.
public record DashPacket() {
    public static final StreamCodec<RegistryFriendlyByteBuf, DashPacket> STREAM_CODEC = StreamCodec.unit(new DashPacket());

    // Läuft auf dem SERVER (Hauptthread), wenn das Paket ankommt
    public static void handle(DashPacket packet, CustomPayloadEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null) {
            return;
        }
        Vec3 look = player.getLookAngle();
        double strength = MyModConfig.DASH_STRENGTH.get();   // Kapitel 19
        player.setDeltaMovement(look.x * strength, 0.3, look.z * strength);
        player.needsSync = true;   // neue Bewegung an den Client schicken
    }
}
```

- Ein Paket ist eine kleine Klasse – am besten ein **record**.
- Der **`StreamCodec`** beschreibt, wie das Paket in Bytes verwandelt wird und zurück. Ohne Daten: `StreamCodec.unit(...)`.
- `context.getSender()` = der Spieler, der das Paket geschickt hat (nur bei Client → Server).
- `needsSync = true`: Bewegungen, die der Server setzt, werden sonst nicht sofort an den Spieler geschickt. (In älteren Versionen hieß das Feld `hurtMarked`.)

### Paket mit Daten

```java
public record SelectSlotPacket(int slot, boolean offhand) {
    public static final StreamCodec<RegistryFriendlyByteBuf, SelectSlotPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SelectSlotPacket::slot,
            ByteBufCodecs.BOOL, SelectSlotPacket::offhand,
            SelectSlotPacket::new);
}
```

`StreamCodec.composite` liest/schreibt die Felder in der angegebenen Reihenfolge. Fertige Codecs in `ByteBufCodecs`: `VAR_INT`, `INT`, `FLOAT`, `DOUBLE`, `BOOL`, `STRING_UTF8`, … sowie `BlockPos.STREAM_CODEC`, `ItemStack.STREAM_CODEC`.

> ⚠️ **Dem Client nie blind vertrauen.** Ein manipulierter Client kann jedes Paket mit beliebigen Werten schicken. Der Server muss prüfen (Abklingzeit, Entfernung, ob der Spieler das darf).

## 2. Der Kanal

```java
public final class ModNetwork {
    // Ein Kanal pro Mod. Version erhöhen, wenn sich Pakete ändern – dann können alte Clients nicht beitreten.
    public static final SimpleChannel CHANNEL = ChannelBuilder
            .named(Identifier.fromNamespaceAndPath(MyMod.MODID, "main"))
            .networkProtocolVersion(1)
            .simpleChannel();

    // Einmal beim Common-Setup aufrufen
    public static void register() {
        CHANNEL.messageBuilder(DashPacket.class, NetworkDirection.PLAY_TO_SERVER)
                .codec(DashPacket.STREAM_CODEC)
                .consumerMainThread(DashPacket::handle)
                .add();
    }

    // Hilfsmethode für den Client
    public static void sendToServer(Object packet) {
        CHANNEL.send(packet, PacketDistributor.SERVER.noArg());
    }

    private ModNetwork() {}
}
```

In `MyMod`:

```java
FMLCommonSetupEvent.getBus(modBusGroup).addListener(event -> event.enqueueWork(ModNetwork::register));
```

| Teil | Bedeutung |
|---|---|
| `NetworkDirection.PLAY_TO_SERVER` / `PLAY_TO_CLIENT` | Richtung des Pakets |
| `consumerMainThread` | Handler läuft auf dem Haupt-Thread (sicher für Welt-Änderungen) |
| `PacketDistributor.SERVER.noArg()` | an den Server |
| `PacketDistributor.PLAYER.with(spieler)` | an einen Spieler |
| `PacketDistributor.TRACKING_ENTITY.with(entity)` | an alle, die eine Entity sehen |
| `PacketDistributor.ALL.noArg()` | an alle |

**Server → Client** funktioniert genauso, nur mit `PLAY_TO_CLIENT`. Der Handler darf dann aber keine Client-Klassen direkt im Code haben, der auch auf dem Server geladen wird → Client-Code in eine eigene Klasse auslagern.

## 3. Die Taste

```java
@Mod.EventBusSubscriber(modid = MyMod.MODID, value = Dist.CLIENT)
public final class ModKeyMappings {
    // Eigener Abschnitt in Optionen → Steuerung → Tastenbelegung ("key.category.mymod.main" in lang)
    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MyMod.MODID, "main"));

    // Standard: R, der Spieler kann sie in den Optionen ändern
    public static final KeyMapping DASH = new KeyMapping("key.mymod.dash", InputConstants.KEY_R, CATEGORY);

    private ModKeyMappings() {}

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(DASH);
    }

    // Jeden Client-Tick prüfen
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        while (DASH.consumeClick()) {                 // jeder Druck zählt genau einmal
            if (Minecraft.getInstance().player != null) {
                ModNetwork.sendToServer(new DashPacket());
            }
        }
    }
}
```

- **`Dist.CLIENT`** ist Pflicht: `KeyMapping` und `Minecraft` gibt es auf einem Server nicht.
- `KeyMapping.Category.register(...)` legt einen eigenen Abschnitt an. Alternativ eine Vanilla-Kategorie: `KeyMapping.Category.GAMEPLAY`, `MISC`, `MOVEMENT` … (seit 1.21.9 sind Kategorien Objekte statt Texte).
- Tasten-Codes: `InputConstants.KEY_R`, `KEY_G`, `KEY_LALT` … (`com.mojang.blaze3d.platform.InputConstants`).
- `consumeClick()` = wurde seit dem letzten Mal gedrückt? Die `while`-Schleife verarbeitet auch schnelle Mehrfachdrücke. Für „gedrückt halten“: `DASH.isDown()`.

`lang`:

```json
"key.category.mymod.main": "Meine Mod",
"key.mymod.dash": "Vorwärts-Sprung"
```

## Checkliste

- [ ] Paket als record mit `StreamCodec` und `handle`-Methode
- [ ] Server-Handler prüft, ob die Aktion erlaubt ist
- [ ] Kanal mit Version, Paket registriert (Common-Setup)
- [ ] Taste in einer `Dist.CLIENT`-Klasse, registriert über `RegisterKeyMappingsEvent`
- [ ] im Client-Tick `consumeClick()` → Paket senden
- [ ] `lang` für Taste und Kategorie

## Zum Ausprobieren

- Abklingzeit für den Dash: im Handler eine `Map<UUID, Long>` mit `level.getGameTime()` merken und erst nach 40 Ticks wieder erlauben.
- Eine Taste, die einen Zauber deines Stabs wechselt (Paket mit `int`-Daten).
- Server → Client: Ein Paket, das beim Spieler eine Nachricht als Titel anzeigt.
