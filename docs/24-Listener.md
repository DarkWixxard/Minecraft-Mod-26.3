# 24 – Listener: was sie sind und wie man sie benutzt

„Listener“ (englisch *to listen* = zuhören) ist das dritte Wort aus derselben Familie wie [Handler (22)](22-Handler.md) und [Event (23)](23-Events.md). Dieses Kapitel klärt, wie die drei zusammenhängen, und zeigt dann, was man mit Listenern **zusätzlich** machen kann:
- Listener-Objekte mit eigenem Zustand
- Listener, die sich selbst wieder abmelden
- Listener, die man im laufenden Spiel an- und ausschaltet
- **Reload-Listener**, die eigene Datendateien laden

> ✅ Getestet mit Forge 26.3-66.1.0 auf einem Server: Zähl-Objekt (2 Monster erschienen, 1 gestorben), Einmal-Listener (meldet sich beim ersten von zwei Schweinen genau einmal), Explosions-Schalter (Stein übersteht TNT nur, wenn der Listener aktiv ist), Reload-Listener (2 Datendateien geladen, beim Start und nach `/reload`).

## 1. Event, Listener, Handler – wie hängt das zusammen?

| Wort | Was es ist | Bild |
|---|---|---|
| **Event** | das Ereignis + Objekt mit den Infos | die Radiosendung |
| **Bus** | die Liste aller, die zuhören | der Radiosender |
| **Listener** | ein **Eintrag** in dieser Liste: „ich höre zu“ | ein eingeschaltetes Radio |
| **Handler** | der **Code**, der dann ausgeführt wird | was du tust, wenn die Durchsage kommt |

Im Alltag werden „Listener“ und „Handler“ oft gleich benutzt, und das ist meistens in Ordnung. Genau genommen ist der Listener die **Anmeldung** (Forge gibt dir dafür ein `EventListener`-Objekt zurück), und der Handler ist die **Methode** dahinter.

```java
EventListener listener = PlayerEvent.PlayerLoggedInEvent.BUS.addListener(ModHandlers::onLogin);
//   ↑ der Listener (Anmeldung)                                        ↑ der Handler (Code)
```

Mit dem `EventListener`-Objekt kann man den Listener später wieder **abmelden**, und man kann nachfragen: `eventType()` (welches Event?), `priority()` (welche Priorität?).

## 2. Die Formen eines Listeners

Welche Form ein Listener hat, hängt davon ab, was er tun soll:

| Form | Signatur | Wofür | anmelden |
|---|---|---|---|
| **Zuhören** | `void handler(Event e)` | lesen, ändern | `BUS.addListener(handler)` |
| **Abbrechen-Können** | `boolean handler(Event e)` | abbrechen, wenn `true` | `BUS.addListener(handler)` (nur abbrechbare Events) |
| **Immer abbrechen** | `void handler(Event e)` | bricht **jedes Mal** ab, ohne Bedingung | `BUS.addListener(true, handler)` bzw. `@SubscribeEvent(alwaysCancelling = true)` |
| **Beobachten** (Monitor) | `void handler(Event e, boolean cancelled)` | nur zuschauen, auch nach dem Abbrechen | `BUS.addListener((e, cancelled) -> …)` |

Dazu jeweils optional eine **Priorität** als erster Parameter: `addListener(Priority.HIGH, handler)` ([23](23-Events.md#6-reihenfolge-priorität-und-monitor)).

## 3. Drei Arten, Listener anzumelden

### a) Einzeln mit `addListener`

```java
PlayerEvent.PlayerLoggedInEvent.BUS.addListener(ModHandlers::onLogin);
```

Gut für einzelne Listener und für alles, was man wieder abmelden will. Details: [22](22-Handler.md#weg-1-von-hand-anmelden-mit-addlistener).

### b) Eine Klasse mit `@Mod.EventBusSubscriber`

```java
@Mod.EventBusSubscriber(modid = MyMod.MODID)
public final class ModGameEvents {
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) { … }
}
```

Forge findet die Klasse beim Start und meldet alle `static`-Methoden mit `@SubscribeEvent` an. Details: [22](22-Handler.md#weg-2-automatisch-mit-annotationen).

### c) Ein **Objekt** mit eigenem Zustand

Manchmal soll ein Listener sich etwas **merken**: zählen, sammeln, einen Zustand halten. Dann sind die Methoden **nicht** `static`, sondern gehören zu einem Objekt:

```java
// Listener-Objekt mit eigenem Zustand: zählt Monster, die erscheinen und die sterben
public final class MobCounter {
    private int appeared;
    private int died;

    @SubscribeEvent   // NICHT static: gehört zu diesem einen Objekt
    public void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && !event.loadedFromDisk() && event.getEntity() instanceof Monster) {
            appeared++;
        }
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide() && event.getEntity() instanceof Monster) {
            died++;
        }
    }

    public int getAppeared() { return appeared; }
    public int getDied() { return died; }
}
```

Anmelden – im Mod-Konstruktor:

```java
public static final MobCounter MOB_COUNTER = new MobCounter();

// im Konstruktor von MyMod:
BusGroup.DEFAULT.register(MethodHandles.lookup(), MOB_COUNTER);
```

- `BusGroup.DEFAULT` = die Gruppe aller Spiel-Events.
- `MethodHandles.lookup()` gibt Forge die Erlaubnis, deine Methoden aufzurufen (ein Java-Sicherheitsmechanismus). Einfach so hinschreiben.
- Ergebnis im Test: `monsters appeared=2 died=1` nach Zombie + Skelett + `/kill` des Zombies.
- `!event.loadedFromDisk()` zählt nur neu erschienene Monster, nicht die, die beim Laden eines Chunks zurückkommen.

> ⚠️ **Getestete Falle:** Hat das Objekt nur **eine** `@SubscribeEvent`-Methode, verweigert Forge 26.3 das Anmelden mit `register(...)` – und die ganze Mod startet nicht:
> *„Failed to register …MobCounter: Only a single listener found. You should directly call addListener() on the EventBus of EntityJoinLevelEvent instead.“*
> Bei nur einer Methode also so anmelden: `EntityJoinLevelEvent.BUS.addListener(MOB_COUNTER::onJoin);`

Wieder abmelden: `register(...)` gibt eine Liste von `EventListener`s zurück, die man mit `BusGroup.DEFAULT.unregister(liste)` wieder entfernt.

### Welche Art wann?

| Situation | Art |
|---|---|
| ein, zwei feste Reaktionen | `@Mod.EventBusSubscriber` mit `static`-Methoden |
| Listener soll gezählt/gesammelt/gemerkt werden | Objekt mit `BusGroup.DEFAULT.register(…)` (ab 2 Methoden) |
| Listener soll an- und abschaltbar sein | `addListener` und `EventListener` merken |
| Mod-Lebenszyklus (Setup, Registrierung) | `XyzEvent.getBus(modBusGroup).addListener(…)` oder `@SubscribeEvent` |

## 4. Listener, die sich selbst abmelden

```java
// Ein Listener, der sich nach dem ersten Mal selbst entfernt
public final class OneTimeListener {
    private static EventListener listener;

    private OneTimeListener() {}

    public static void register() {
        listener = EntityJoinLevelEvent.BUS.addListener(OneTimeListener::onJoin);
    }

    private static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Pig) {
            System.out.println("[guide] the first pig is here - listener removes itself now");
            EntityJoinLevelEvent.BUS.removeListener(listener);
        }
    }
}
```

Im Test wurden zwei Schweine gespawnt – die Meldung kam **einmal**. Gut für: „beim ersten Betreten der Welt“, „beim ersten Boss-Kill“, einmalige Einrichtung.

> In 26.x stehen viele Mobs in Unterpaketen: `Pig` liegt in `net.minecraft.world.entity.animal.pig`, nicht mehr direkt in `animal`. IntelliJ findet sie mit Alt + Enter.

## 5. Listener im laufenden Spiel an- und ausschalten

Beispiel: Ein Befehl schaltet **alle Explosionen** ab und wieder an.

```java
// Schaltet alle Explosionen im laufenden Spiel ab und an (benutzt von /mymod explosions)
public final class ExplosionSwitch {
    private static EventListener blocker;   // null = Explosionen erlaubt

    private ExplosionSwitch() {}

    public static void disableExplosions() {
        if (blocker == null) {
            // alwaysCancelling = true: dieser Listener bricht JEDE Explosion ab, ohne Bedingung
            blocker = ExplosionEvent.Start.BUS.addListener(true, event -> {});
        }
    }

    public static void enableExplosions() {
        if (blocker != null) {
            ExplosionEvent.Start.BUS.removeListener(blocker);
            blocker = null;
        }
    }
}
```

Befehl dazu ([13](13-Befehle.md)):

```java
.then(Commands.literal("explosions").then(Commands.argument("allowed", BoolArgumentType.bool()).executes(ctx -> {
    if (BoolArgumentType.getBool(ctx, "allowed")) {
        ExplosionSwitch.enableExplosions();
    } else {
        ExplosionSwitch.disableExplosions();
    }
    ctx.getSource().sendSuccess(() -> Component.literal("explosions allowed: " + BoolArgumentType.getBool(ctx, "allowed")), true);
    return 1;
})))
```

- **`alwaysCancelling`**: Der Listener muss nichts prüfen und nichts zurückgeben – sein bloßes Dasein bricht ab. Das Lambda `event -> {}` ist deshalb leer. (Mit Annotation: `@SubscribeEvent(alwaysCancelling = true)` an einer `void`-Methode.)
- Der Trick zum Ein- und Ausschalten: den `EventListener` in einem Feld merken (`null` = aus).
- Ergebnis im Test: Mit `/mymod explosions false` überlebt ein Stein direkt unter einer TNT, mit `true` wird er zerstört.
- Alternative ohne An-/Abmelden: ein normaler Listener, der eine `boolean`-Variable prüft. Beides ist richtig. Ab- und Anmelden spart Arbeit, wenn der Listener meistens aus ist.

## 6. Reload-Listener: eigene Datendateien

Ein ganz anderer, sehr nützlicher Listener: Er hört nicht auf Spiel-Events, sondern auf **das Laden der Datenpakete** (Weltstart und `/reload`). Damit kann deine Mod eigene JSON-Dateien lesen – so wie Minecraft Rezepte und Loot-Tables liest. Server-Admins und Datenpakete können die Werte dann ändern, ohne deinen Code anzufassen.

Beispiel: In `data/<modid>/gem_values/*.json` steht, wie viel ein Item „wert“ ist.

```json
// data/mymod/gem_values/diamond.json
{ "item": "minecraft:diamond", "value": 10 }

// data/mymod/gem_values/emerald.json
{ "item": "minecraft:emerald", "value": 6 }
```

### Das Datenformat (Codec)

```java
// Ein Eintrag aus data/<modid>/gem_values/*.json, z. B. { "item": "minecraft:diamond", "value": 10 }
public record GemValue(Item item, int value) {
    public static final Codec<GemValue> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(GemValue::item),
            Codec.INT.fieldOf("value").forGetter(GemValue::value)
    ).apply(instance, GemValue::new));
}
```

Ein **Codec** beschreibt, wie JSON in ein Java-Objekt verwandelt wird (und zurück) – Feld für Feld:
- `BuiltInRegistries.ITEM.byNameCodec()` liest eine Item-ID wie `"minecraft:diamond"` und gibt das Item. Unbekannte IDs werden als Fehler gemeldet.
- `Codec.INT`, `Codec.STRING`, `Codec.BOOL`, `Codec.FLOAT` für einfache Werte, `.optionalFieldOf("name", standard)` für optionale Felder.

### Der Reload-Listener

```java
// Reload-Listener: liest alle Dateien in data/*/gem_values/ beim Weltstart und bei /reload
public final class GemValueManager extends SimpleJsonResourceReloadListener<GemValue> {
    public static final GemValueManager INSTANCE = new GemValueManager();

    private Map<Item, Integer> values = Map.of();

    private GemValueManager() {
        super(GemValue.CODEC, FileToIdConverter.json("gem_values"));
    }

    // Wird aufgerufen, nachdem alle Dateien gelesen (und mit dem Codec umgewandelt) wurden
    @Override
    protected void apply(Map<Identifier, GemValue> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<Item, Integer> newValues = new HashMap<>();
        files.values().forEach(gem -> newValues.put(gem.item(), gem.value()));
        this.values = Map.copyOf(newValues);
        System.out.println("[guide] loaded " + values.size() + " gem values");
    }

    public int getValue(Item item) {
        return values.getOrDefault(item, 0);
    }

    public int size() {
        return values.size();
    }
}
```

- `SimpleJsonResourceReloadListener` ist eine Vanilla-Klasse, die die ganze Arbeit macht: Dateien finden, JSON lesen, mit dem Codec umwandeln. Du schreibst nur `apply`.
- `FileToIdConverter.json("gem_values")` = Ordner `data/<irgendeine-mod>/gem_values/`. **Alle** Mods und Datenpakete können dort Dateien hinzufügen.
- In `apply` die alte Map **ersetzen**, nicht ergänzen – sonst bleiben bei `/reload` gelöschte Einträge übrig.

### Anmelden – über ein Event

Reload-Listener werden mit einem **Event-Listener** angemeldet:

```java
@Mod.EventBusSubscriber(modid = MyMod.MODID)
public final class GuideListeners {
    // Ein Event-Listener, der einen Reload-Listener anmeldet
    @SubscribeEvent
    public static void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(GemValueManager.INSTANCE);
    }
}
```

Benutzen irgendwo im Code: `GemValueManager.INSTANCE.getValue(Items.DIAMOND)` → `10`.

Im Test: `[guide] loaded 2 gem values` beim Start und noch einmal nach `/reload`.

> Reload-Listener über `AddReloadListenerEvent` laufen auf dem **Server** (Ordner `data/`). Für Client-Ressourcen aus `assets/` gibt es ein eigenes Client-Event (`RegisterClientReloadListenersEvent`, Paket `net.minecraftforge.client.event`). Vanilla-Vorbilder für beide Seiten: `WaypointStyleManager`, `EquipmentAssetManager`.

## 7. Andere „Listener“ in Minecraft

Das Wort taucht auch außerhalb des Event-Systems auf – immer mit derselben Idee „Bescheid sagen, wenn etwas passiert“:

| Name | Was | Beispiel |
|---|---|---|
| `PreparableReloadListener` | lädt Daten bei Start/`/reload` | Abschnitt 6 |
| `GameEventListener` | reagiert auf **Spiel-Ereignisse in der Welt** (Schritte, Vibrationen) – nicht dasselbe wie Forge-Events! | Sculk-Sensor, Warden |
| `ContainerListener` | Inventar eines Menüs hat sich geändert | Braustand, Pferde-Inventar |
| `onSyncedDataUpdated(...)` (Methode in `Entity`) | ein synchronisierter Wert ([09](09-Mob-Fliegen.md#2-synchronisierte-daten)) hat sich geändert | Größe neu berechnen, wenn sich ein Wert ändert |

## 8. Typische Fehler

| Problem | Ursache | Lösung |
|---|---|---|
| Mod startet nicht: *„Only a single listener found. You should directly call addListener()“* | Objekt mit nur **einer** `@SubscribeEvent`-Methode über `BusGroup.register` angemeldet | `BUS.addListener(objekt::methode)` |
| Objekt-Methoden werden nie aufgerufen | Methoden `static` (dann gehören sie zur Klasse, nicht zum Objekt) oder Objekt nie registriert | nicht-`static`, `BusGroup.DEFAULT.register(MethodHandles.lookup(), objekt)` |
| Listener lässt sich nicht abmelden | `EventListener`-Rückgabe von `addListener` nicht gemerkt | in einem Feld speichern |
| Listener doppelt aktiv | zweimal `addListener` (z. B. Befehl zweimal ausgeführt) | vorher prüfen (`if (blocker == null)`) |
| Reload-Daten bleiben nach dem Löschen einer Datei erhalten | in `apply` ergänzt statt ersetzt | neue Map bauen und zuweisen |
| Reload-Listener lädt nichts | falscher Ordnername in `FileToIdConverter.json(...)` oder JSON passt nicht zum Codec (Log!) | Pfad und Log prüfen |

## Checkliste

- [ ] Form passend gewählt (zuhören / abbrechen / immer abbrechen / beobachten)
- [ ] Art der Anmeldung passend (statische Klasse / Objekt / einzeln)
- [ ] `EventListener` gemerkt, wenn er wieder weg soll
- [ ] Objekt-Listener: mindestens 2 Methoden oder einzeln mit `addListener`
- [ ] Reload-Listener: Codec, `apply` ersetzt die Daten, Anmeldung über `AddReloadListenerEvent`
- [ ] im Test geprüft, dass der Listener läuft (Log-Ausgabe oder Debug-Befehl)

## Zum Ausprobieren

- Erweitere `MobCounter` um eine `Map<EntityType<?>, Integer>`, die für jede Mob-Art einzeln zählt, und gib sie mit einem Befehl aus.
- Ein `/mymod peaceful on|off`, das per `alwaysCancelling`-Listener auf `EntityJoinLevelEvent` … – Moment: Warum wäre das eine schlechte Idee? (Tipp: Was gehört alles zu „Entities“? Spieler, Items, Pfeile …) Bau stattdessen einen normalen Listener, der nur `Monster` abbricht.
- Lass deinen Gem-Wert bei `LivingDropsEvent` zählen: Wie viel „Wert“ hat ein Spieler insgesamt schon eingesammelt? (Tipp: `PlayerEvent.ItemPickupEvent` oder `EntityItemPickupEvent` anschauen.)
