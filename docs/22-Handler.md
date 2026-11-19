# 22 – Handler: was sie sind und wie man sie benutzt

„Handler“ (englisch *to handle* = sich um etwas kümmern) ist eines der Wörter, das beim Modding ständig auftaucht: Event-Handler, Packet-Handler, Handler-Klasse … Dieses Kapitel erklärt, was dahintersteckt, und zeigt alle Arten von Handlern, die in diesem Leitfaden vorkommen.

> ✅ Getestet mit Forge 26.3-66.1.0: alle neuen Beispiele kompiliert; der Server startet mit eingeschalteten strengen Event-Prüfungen (`eventbus.api.strictRuntimeChecks`) ohne Fehler und lädt dabei keine Client-Klassen.

## 1. Die Idee in einem Satz

> **Ein Handler ist eine Methode, die du schreibst, aber nicht selbst aufrufst.** Du gibst sie an Minecraft/Forge weiter, und *die* rufen sie auf, **wenn etwas passiert**.

Vergleich aus dem Alltag: Du gibst deine Telefonnummer bei der Werkstatt ab („Ruf mich an, wenn das Auto fertig ist“). Du rufst nicht ständig an und fragst. Die Nummer ist dein Handler, „Auto fertig“ ist das Ereignis.

Ohne Handler müsstest du jeden Tick selbst nachschauen: *Ist ein Spieler beigetreten? Wurde ein Block abgebaut? Kam ein Paket an?* Mit Handlern sagst du einmal: *„Wenn ein Spieler beitritt, ruf diese Methode auf.“*

Andere Namen für dasselbe Prinzip: **Listener** (Zuhörer), **Callback** (Rückruf), **Consumer**.

## 2. Die drei Schritte – immer gleich

| Schritt | Frage | Beispiel |
|---|---|---|
| 1. **Schreiben** | Was soll passieren? | Methode `onLogin(PlayerLoggedInEvent event)` |
| 2. **Anmelden** (registrieren) | Wo soll Forge Bescheid sagen? | `PlayerLoggedInEvent.BUS.addListener(ModHandlers::onLogin)` |
| 3. **Aufgerufen werden** | Wann? | Forge ruft `onLogin` auf, sobald ein Spieler beitritt |

Der Handler bekommt dabei fast immer ein **Objekt mit Informationen** als Parameter: das Event (wer, was, wo), das Paket mit Daten, den Befehlskontext …

**Vergisst du Schritt 2, passiert nichts – ohne Fehlermeldung.** Das ist der häufigste Handler-Fehler überhaupt.

## 3. Event-Handler

Der wichtigste Handler-Typ. Forge feuert hunderte Events ([15](15-Spiel-Events.md)), und du hängst Handler daran.

### Weg 1: von Hand anmelden mit `addListener`

```java
// Handler, die von Hand mit addListener angemeldet werden
public final class ModHandlers {
    private static EventListener breakListener;

    private ModHandlers() {}

    // Einmal aus dem Mod-Konstruktor aufrufen: ModHandlers.register();
    public static void register() {
        PlayerEvent.PlayerLoggedInEvent.BUS.addListener(ModHandlers::onLogin);
        breakListener = BlockEvent.BreakEvent.BUS.addListener(Priority.HIGH, ModHandlers::onBreak);
        PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(event ->
                System.out.println(event.getEntity().getName().getString() + " left"));   // Handler als Lambda
    }

    // Handler für ein normales Event: gibt nichts zurück
    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ModNetwork.sendToPlayer(new ShowMessagePacket("Welcome back!"), player);   // Paket an den Client (Abschnitt 4)
        }
    }

    // Handler für ein abbrechbares Event: true = abbrechen
    private static boolean onBreak(BlockEvent.BreakEvent event) {
        if (event.getState().is(Blocks.BEDROCK)) {
            event.getPlayer().sendSystemMessage(Component.literal("Nope."));
            return true;
        }
        return false;
    }

    // Handler kann man auch wieder abmelden
    public static void disableBreakProtection() {
        BlockEvent.BreakEvent.BUS.removeListener(breakListener);
    }
}
```

**Was hier passiert:**

| Code | Bedeutung |
|---|---|
| `PlayerLoggedInEvent.BUS` | Jedes Event hat seinen eigenen „Bus“ – die Liste aller Handler, die bei diesem Event Bescheid bekommen wollen |
| `.addListener(ModHandlers::onLogin)` | trägt den Handler in die Liste ein. `ModHandlers::onLogin` ist eine **Methodenreferenz**: „diese Methode“, nicht „ruf sie jetzt auf“ (keine Klammern!) |
| `event -> ...` | ein **Lambda**: ein Handler ohne eigenen Methodennamen, direkt an Ort und Stelle. Gut für ein, zwei Zeilen |
| `Priority.HIGH` | Reihenfolge, wenn mehrere Handler (auch aus anderen Mods) dasselbe Event bekommen: `HIGHEST`, `HIGH`, `NORMAL` (Standard), `LOW`, `LOWEST`. `MONITOR` = nur zuschauen, nichts ändern |
| Rückgabe `boolean` | nur bei abbrechbaren Events: `true` = Event abbrechen (der Block bleibt) |
| `EventListener` / `removeListener` | `addListener` gibt einen „Beleg“ zurück, mit dem man den Handler wieder entfernen kann |

`ModHandlers::onLogin` und `event -> onLogin(event)` bedeuten dasselbe. Methodenreferenzen sind kürzer, Lambdas praktisch für Einzeiler.

Diesen Weg benutzt auch die Hauptklasse für Mod-Events, z. B. `EntityAttributeCreationEvent.BUS.addListener(MyMod::registerAttributes)` ([06](06-Eigener-Mob-mit-GeckoLib.md#4-attribute-anmelden)).

### Weg 2: automatisch mit Annotationen

```java
// Handler, die Forge automatisch über Annotationen findet
@Mod.EventBusSubscriber(modid = MyMod.MODID)
public final class AnnotatedHandlers {
    private AnnotatedHandlers() {}

    @SubscribeEvent(priority = Priority.LOW)   // läuft nach Handlern mit normaler/hoher Priorität
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        event.getEntity().sendSystemMessage(Component.literal("Try again!"));
    }
}
```

- `@Mod.EventBusSubscriber` sagt Forge: „Durchsuche diese Klasse beim Start.“
- Jede Methode mit `@SubscribeEvent` wird angemeldet. **Am Parameter-Typ** erkennt Forge, für welches Event.
- Die Methoden müssen `public static` sein und genau einen Parameter haben.
- Für Client-Handler: `@Mod.EventBusSubscriber(modid = MyMod.MODID, value = Dist.CLIENT)`.

### Welcher Weg wann?

| | `addListener` | `@SubscribeEvent` |
|---|---|---|
| Anmelden | von Hand, z. B. im Mod-Konstruktor | automatisch |
| Vergessen möglich? | ja (Aufruf von `register()` vergessen) | nein, solange die Annotation stimmt |
| Abmelden möglich | ja | nein |
| Handler als Lambda | ja | nein |
| Typisch für | Mod-Lebenszyklus (`FMLCommonSetupEvent`, Attribute), Handler, die man an/aus schalten will | Spiel-Events, Client-Events (Renderer, Tasten, HUD) |

Beides mischen ist erlaubt. Nur **denselben** Handler nicht auf beiden Wegen anmelden – sonst läuft er doppelt.

## 4. Packet-Handler

Ein Packet-Handler wird aufgerufen, wenn ein **Netzwerk-Paket ankommt** ([18](18-Tasten-und-Netzwerk.md)). Er ist der „Empfänger“ auf der anderen Seite.

### Anmelden

```java
CHANNEL.messageBuilder(ShowMessagePacket.class, NetworkDirection.PLAY_TO_CLIENT)
        .codec(ShowMessagePacket.STREAM_CODEC)
        .consumerMainThread(ShowMessagePacket::handle)   // ← hier wird der Handler angemeldet
        .add();
```

### Server → Client: Handler mit Client-Klasse

```java
// Server -> Client: Nachricht über der Hotbar anzeigen
public record ShowMessagePacket(String text) {
    public static final StreamCodec<RegistryFriendlyByteBuf, ShowMessagePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ShowMessagePacket::text,
            ShowMessagePacket::new);

    // Packet-Handler: läuft auf dem CLIENT. Leitet nur an die Client-Klasse weiter.
    public static void handle(ShowMessagePacket packet, CustomPayloadEvent.Context context) {
        ClientPacketHandler.showMessage(packet.text());
    }
}
```

```java
// Wird nur auf dem Client geladen: der einzige Ort, der Minecraft.getInstance() benutzen darf
public final class ClientPacketHandler {
    private ClientPacketHandler() {}

    public static void showMessage(String text) {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.sendOverlayMessage(Component.literal(text));   // erscheint über der Hotbar
        }
    }
}
```

Senden vom Server:

```java
CHANNEL.send(new ShowMessagePacket("Welcome back!"), PacketDistributor.PLAYER.with(player));
```

**Warum die Extra-Klasse `ClientPacketHandler`?** Die Paket-Klasse wird auf **beiden** Seiten geladen (sie wird auf dem Server registriert). Stünde `Minecraft.getInstance()` direkt darin, würde ein Server beim Laden abstürzen, weil es die Klasse `Minecraft` dort nicht gibt. Java lädt eine Klasse aber erst, wenn Code sie **wirklich ausführt**. `handle` läuft nur auf dem Client, also wird `ClientPacketHandler` auch nur dort geladen.

> Diese Aufteilung ist ein allgemeines Muster: **Client-Code in eigene Handler-Klassen auslagern**, die nur von Client-Code aufgerufen werden.

### Client → Server

Umgekehrt (z. B. Tastendruck, [18](18-Tasten-und-Netzwerk.md)) läuft der Handler auf dem **Server**. Dort gilt:
- `context.getSender()` = der Spieler, der geschickt hat.
- **Nichts glauben, alles prüfen** (darf der Spieler das? ist er nah genug? Abklingzeit?).

| Methode beim Anmelden | Wo läuft der Handler? |
|---|---|
| `consumerMainThread(...)` | auf dem Haupt-Thread des Spiels – sicher für alles, was die Welt ändert. **Fast immer die richtige Wahl.** |
| `consumerNetworkThread(...)` | auf dem Netzwerk-Thread – nur für Spezialfälle; Welt nicht anfassen |

## 5. Weitere Handler, die dir schon begegnet sind

Das Prinzip „Methode übergeben, Minecraft ruft sie auf“ steckt an vielen Stellen, auch wenn sie nicht „Handler“ heißen:

| Wo | Der Handler | Wann er aufgerufen wird | Kapitel |
|---|---|---|---|
| Befehle | `.executes(ctx -> heal(...))` | wenn jemand den Befehl eingibt | [13](13-Befehle.md) |
| GeckoLib-Animation | `new AnimationController<>("movement", 5, test -> ...)` – im Code heißt das Interface wirklich `AnimationStateHandler` | jeden Frame beim Zeichnen | [08](08-Mob-Animationen.md) |
| HUD-Ebene | `(graphics, deltaTracker) -> drawBars(graphics)` | jeden Frame beim Zeichnen des HUD | [10](10-Boss-Funktionen.md) |
| Registrierung | `() -> new Block(...)` | wenn Forge den Block registriert | [02](02-Bloecke-Items-Werkzeuge.md) |
| Licht eines Blocks | `.lightLevel(state -> ...)` | wenn Minecraft das Licht berechnet | [14](14-Eigene-Block-Klassen.md) |
| Spawn-Regel | `Monster::checkMonsterSpawnRules` | wenn Minecraft einen Spawn-Platz prüft | [20](20-Spawn-Eier-und-Spawnen.md) |

### Überschreiben ist auch eine Art Handler

`hurtServer`, `tick`, `useWithoutItem`, `stepOn` … in eigenen Klassen sind ebenfalls Methoden, die Minecraft aufruft, wenn etwas passiert. Der Unterschied:

| | Überschreiben (`@Override`) | Event-Handler |
|---|---|---|
| Wirkt auf | nur **deine** Klasse (dein Block, dein Mob) | **alles** im Spiel, auch Vanilla (jeder Zombie, jeder Block) |
| Anmelden | nicht nötig – die Klasse selbst ist registriert | nötig (`addListener` / `@SubscribeEvent`) |

Faustregel: **Eigenes Zeug → überschreiben. Fremdes Zeug (Vanilla, andere Mods) → Event-Handler.**

## 6. Handler-Klassen sinnvoll aufteilen

Größere Mods sammeln Handler in eigenen Klassen, z. B.:

```
event/
├── ModGameEvents.java        Spiel-Events (Login, Drops, Abbauen) – Server/beide
├── ModSpawnEvents.java       Spawn-Regeln
client/
├── ModClientEvents.java      Renderer, Tasten, HUD – nur Client
└── ClientPacketHandler.java  Client-Teil der Packet-Handler
network/
└── …Packet.java              jedes Paket mit seinem handle()
```

Regeln, die Ärger ersparen:
1. **Client und Server trennen**: Alles mit `Minecraft`, Renderern, Tasten in Klassen mit `Dist.CLIENT` oder in Client-Handler-Klassen.
2. **Handler kurz halten**: Prüfen, dann an eine Methode mit sprechendem Namen weitergeben. Ein Handler mit 200 Zeilen ist schwer zu lesen.
3. **Schnell zurückkehren**: Viele Handler laufen sehr oft (Ticks, jeder Block, jede Entity). Erst die billigen Prüfungen (`if (!(entity instanceof Player)) return;`), dann die teure Arbeit.
4. **Seite prüfen**, wenn ein Event auf beiden Seiten feuert (`level.isClientSide()`, `event.side()`).

## 7. Typische Fehler

| Problem | Ursache | Lösung |
|---|---|---|
| Handler wird nie aufgerufen | nicht angemeldet; `@Mod.EventBusSubscriber` fehlt; Methode nicht `static`; falscher `@SubscribeEvent`-Import (`com.google…`) | Anmeldung prüfen; Log-Ausgabe/Breakpoint in den Handler |
| Handler läuft doppelt | auf beiden Wegen angemeldet, oder Event feuert auf Server **und** Client | nur einen Weg; Seite prüfen |
| `addListener(ModHandlers.onLogin())` rot | Methode wird **aufgerufen** statt **übergeben** | `ModHandlers::onLogin` (ohne Klammern) |
| Abbrechen wirkt nicht | Handler gibt `void` zurück | `boolean` zurückgeben |
| Server stürzt beim Start ab: `NoClassDefFoundError: net/minecraft/client/…` | Client-Klasse in Code, der auf dem Server läuft | in eine Client-Handler-Klasse auslagern |
| Paket kommt an, aber nichts passiert | Handler nicht beim Kanal angemeldet oder falsche Richtung (`PLAY_TO_SERVER`/`PLAY_TO_CLIENT`) | `messageBuilder(...)` prüfen |

**Debug-Tipp:** Setz einen Breakpoint in die erste Zeile des Handlers und starte mit 🐞 ([01](01-Einrichtung-und-Grundlagen.md#über-eine-run-configuration-mit-debugger)). Hält das Spiel nicht an, ist der Handler nicht angemeldet. Hält es an, kannst du dir das Event-Objekt mit allen Werten ansehen.

## Checkliste: neuer Handler

- [ ] richtiges Event / richtige Stelle gefunden
- [ ] Methode mit passendem Parameter (und `boolean`, falls abbrechbar)
- [ ] **angemeldet** (`addListener` oder `@SubscribeEvent` + `@Mod.EventBusSubscriber`)
- [ ] Seite (Client/Server) bedacht, Client-Code ausgelagert
- [ ] mit Log-Ausgabe oder Breakpoint geprüft, dass er wirklich läuft

## Zum Ausprobieren

- Schreib einen Handler für `LivingDeathEvent`, der in den Chat schreibt, welches Mob gerade gestorben ist – einmal mit `addListener` und einmal mit `@SubscribeEvent`. Was passiert, wenn du beide gleichzeitig angemeldet lässt?
- Melde einen Handler an, der sich nach dem ersten Aufruf selbst wieder abmeldet (`removeListener` im Handler).
- Gib zwei Handlern für dasselbe Event verschiedene Prioritäten, lass beide etwas ins Log schreiben, und prüfe die Reihenfolge.
