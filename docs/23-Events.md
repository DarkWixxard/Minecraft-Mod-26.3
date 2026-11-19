# 23 – Events: was sie sind und wie man sie benutzt

Kapitel [22](22-Handler.md) erklärt den **Handler** – die Methode, die reagiert. Dieses Kapitel erklärt das **Event** – das, worauf reagiert wird: was ein Event ist, welche Arten es gibt, wie es durch Forge läuft, wie man es liest, ändert oder abbricht, und wie man **eigene Events** baut.

> ✅ Getestet mit Forge 26.3-66.1.0 auf einem Server: beide Anmelde-Wege für ein Mod-Event, eigene Events (veränderbar und abbrechbar) ausgelöst, Reihenfolge nach Priorität, Verhalten nach dem Abbrechen und Monitor-Listener – die Ausgaben stehen bei den Beispielen.

## 1. Was ist ein Event?

Ein Event ist **zwei Dinge auf einmal**:

1. **Ein Ereignis** – etwas passiert gerade: Ein Spieler tritt bei, ein Block wird abgebaut, ein Mob stirbt, das Spiel startet.
2. **Ein Objekt mit Informationen darüber** – *welcher* Spieler, *welcher* Block, *woran* ist der Mob gestorben. Dieses Objekt bekommt dein Handler als Parameter.

```java
@SubscribeEvent
public static void onDrops(LivingDropsEvent event) {   // ← "event" ist das Objekt
    LivingEntity mob = event.getEntity();               //   wer ist gestorben?
    DamageSource source = event.getSource();            //   woran?
    Collection<ItemEntity> drops = event.getDrops();    //   was fällt gleich heraus?
}
```

Der **Klassenname** sagt, *welches* Ereignis (`LivingDropsEvent` = „ein Lebewesen lässt Beute fallen“). Der **Inhalt** sagt, *was genau*.

## 2. Der Weg eines Events

```
Minecraft/Forge-Code:  "Ein Zombie ist gestorben"
        │
        ▼   erzeugt   new LivingDropsEvent(zombie, quelle, drops, …)
        ▼   schickt es an   LivingDropsEvent.BUS.post(event)
        │
   ┌────┴──────────── BUS (die Liste aller Listener dieses Events) ─────────────┐
   │  1. Listener mit Priority.HIGHEST   (z. B. aus einer anderen Mod)           │
   │  2. Listener mit Priority.HIGH                                              │
   │  3. Listener mit Priority.NORMAL    ← dein Handler                         │
   │  4. … LOW, LOWEST                                                          │
   │  5. MONITOR-Listener (nur zuschauen)                                       │
   └────┬───────────────────────────────────────────────────────────────────────┘
        ▼
Minecraft/Forge-Code liest das Ergebnis: abgebrochen? Drops verändert?
        → macht entsprechend weiter
```

Wichtig:
- **Jedes Event hat seinen eigenen Bus** (`LivingDropsEvent.BUS`). Der Bus ist die Liste, in die sich Handler eintragen.
- **Alle** Listener bekommen **dasselbe** Objekt – nacheinander. Ändert einer etwas, sieht der nächste schon die Änderung.
- **Nach dem Abbrechen** bekommen weitere normale Listener das Event **nicht mehr**. Nur `MONITOR`-Listener sehen es noch (siehe [Abschnitt 6](#6-reihenfolge-priorität-und-monitor)).
- Der auslösende Code wartet, bis alle fertig sind, und schaut dann auf das Ergebnis. Deshalb können Events Vanilla-Verhalten **ändern**.

## 3. Die drei großen Gruppen

### a) Mod-Lebenszyklus-Events („Mod-Bus“)

Feuern **einmal beim Start** des Spiels, für **jede Mod einzeln**. Hier meldet man Dinge an.

| Event | Wofür |
|---|---|
| `FMLCommonSetupEvent` | allgemeine Einrichtung nach der Registrierung (z. B. Netzwerk-Pakete) |
| `FMLClientSetupEvent` | Einrichtung nur auf dem Client |
| `EntityAttributeCreationEvent` | Attribute eigener Mobs ([06](06-Eigener-Mob-mit-GeckoLib.md)) |
| `EntityRenderersEvent.RegisterRenderers` (Client) | Renderer anmelden |
| `RegisterKeyMappingsEvent` (Client) | Tasten anmelden ([18](18-Tasten-und-Netzwerk.md)) |
| `AddGuiOverlayLayersEvent` (Client) | eigene HUD-Ebenen ([10](10-Boss-Funktionen.md)) |
| `BuildCreativeModeTabContentsEvent` | Items in Vanilla-Kreativ-Tabs ([02](02-Bloecke-Items-Werkzeuge.md)) |
| `SpawnPlacementRegisterEvent` | Spawn-Regeln ([20](20-Spawn-Eier-und-Spawnen.md)) |

Weil sie zu **deiner** Mod gehören, holt man ihren Bus bei manuellem Anmelden aus der `BusGroup` der Mod:

```java
public MyMod(FMLJavaModLoadingContext context) {
    var modBusGroup = context.getModBusGroup();
    FMLCommonSetupEvent.getBus(modBusGroup).addListener(this::commonSetup);   // ← getBus(modBusGroup), nicht .BUS
}
```

Mit `@Mod.EventBusSubscriber` + `@SubscribeEvent` erledigt Forge das automatisch – getestet: beide Wege werden aufgerufen.

```
[guide] common setup (addListener)
[guide] common setup (@SubscribeEvent)
```

### b) Spiel-Events

Feuern **während des Spielens**, immer wieder, für alle Mods gemeinsam. Damit verändert man das Spielgeschehen ([15](15-Spiel-Events.md)).

| Paket (`net.minecraftforge.event…`) | Beispiele |
|---|---|
| `entity.player` | `PlayerEvent.PlayerLoggedInEvent`, `PlayerInteractEvent.RightClickBlock`, `AttackEntityEvent` |
| `entity.living` | `LivingDeathEvent`, `LivingDropsEvent`, `LivingHurtEvent`, `LivingFallEvent` |
| `entity` | `EntityJoinLevelEvent`, `ProjectileImpactEvent` |
| `level` | `BlockEvent.BreakEvent`, `BlockEvent.EntityPlaceEvent`, `ExplosionEvent` |
| `event` (oberste Ebene) | `TickEvent.*`, `RegisterCommandsEvent`, `ServerChatEvent` |

Bus: `XyzEvent.BUS`.

### c) Client-Events

Feuern **nur beim Spieler** (Paket `net.minecraftforge.client.event`): Eingaben, Zeichnen, HUD, Tooltips. Handler-Klassen dafür **immer** mit `value = Dist.CLIENT`.

| Event | Wann |
|---|---|
| `InputEvent.InteractionKeyMappingTriggered.Attack` | Angriffs-Taste ([07](07-Mob-Hitboxen.md)) |
| `CustomizeGuiOverlayEvent.BossEventProgress` | eine Bossbar wird gezeichnet ([10](10-Boss-Funktionen.md)) |
| `TickEvent.ClientTickEvent.Post` | jeder Client-Tick ([18](18-Tasten-und-Netzwerk.md)) |
| `ItemTooltipEvent` | Tooltip eines Items wird gebaut |

## 4. Der Aufbau eines Event-Objekts

In Forge 26.x (EventBus 7) gibt es zwei Bauformen. Man erkennt sie an der Klasse (Strg + Klick auf den Event-Namen):

### Record-Events – Werte nur lesen

```java
public record PlayerLoggedInEvent(Player getEntity) implements RecordEvent, PlayerEvent { … }
public record LivingDropsEvent(LivingEntity getEntity, DamageSource getSource,
                               Collection<ItemEntity> getDrops, boolean isRecentlyHit) implements Cancellable, … { … }
```

- Die Werte sind fest. Man liest sie mit dem Namen aus der Klammer: `event.getEntity()`, `event.getDrops()`.
- Bei neueren Events heißen sie ohne `get`: `event.player()`, `event.side()` (`TickEvent.PlayerTickEvent`).
- **Verändern** geht trotzdem, wenn ein Wert selbst veränderbar ist – z. B. die Drop-**Liste**: `event.getDrops().add(...)`.

### Mutable Events – Werte ändern

```java
public final class BreakEvent extends MutableEvent implements Cancellable, BlockEvent, HasResult {
    public int getExpToDrop() { … }
    public void setExpToDrop(int exp) { … }
}
```

- `extends MutableEvent` mit **Gettern und Settern**. Listener ändern Werte über Setter, und der auslösende Code liest sie danach.

### Eigenschaften (Characteristics)

| Interface | Bedeutung | Für dich heißt das |
|---|---|---|
| `Cancellable` | Event kann abgebrochen werden | Handler gibt `boolean` zurück (`true` = abbrechen) |
| `SelfDestructing` | Event feuert nur ein einziges Mal, danach räumt Forge den Bus auf (z. B. `FMLClientSetupEvent`, `RegisterKeyMappingsEvent`, `GameShuttingDownEvent`) | Listener müssen **vorher** angemeldet sein – im Mod-Konstruktor oder per `@SubscribeEvent` |
| `MonitorAware` | Event weiß, ob gerade ein Monitor-Listener läuft | selten wichtig |
| `HasResult` (Forge) | statt nur „abbrechen“ ein dreiteiliges Ergebnis: erlauben / Standard / verbieten | `event.setResult(...)` |

### Pre und Post

Viele Events gibt es doppelt: `TickEvent.PlayerTickEvent.Pre` (vor dem Vanilla-Code) und `.Post` (danach). Mit `Pre` kann man Vanilla vorgreifen, mit `Post` auf das Ergebnis reagieren.

## 5. Lesen, ändern, abbrechen – die drei Möglichkeiten

```java
// 1) Nur LESEN – auf etwas reagieren
@SubscribeEvent
public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
    event.getEntity().sendSystemMessage(Component.literal("Hallo!"));
}

// 2) ÄNDERN – das Ergebnis beeinflussen
@SubscribeEvent
public static void moreXp(BlockEvent.BreakEvent event) {
    event.setExpToDrop(event.getExpToDrop() * 2);
}

// 3) ABBRECHEN – das Ereignis verhindern
@SubscribeEvent
public static boolean protectSpawners(BlockEvent.BreakEvent event) {
    return event.getState().is(Blocks.SPAWNER) && !event.getPlayer().isCreative();   // true = Block bleibt
}
```

## 6. Reihenfolge, Priorität und Monitor

Wenn mehrere Listener dasselbe Event bekommen (deine, die anderer Mods, Forge selbst), bestimmt die **Priorität** die Reihenfolge:

```java
GemDropEvent.BUS.addListener(Priority.HIGH, event -> event.setAmount(event.getAmount() + 1));   // zuerst: +1

@SubscribeEvent                                        // danach (NORMAL): ×2
public static void doubleGems(GemDropEvent event) {
    event.setAmount(event.getAmount() * 2);
}
// Start mit 1  →  (1 + 1) × 2 = 4        Getestet: "gems=4"
```

`HIGHEST` → `HIGH` → `NORMAL` (Standard) → `LOW` → `LOWEST` → `MONITOR`.

### Nach dem Abbrechen

Getestet mit einem abbrechbaren Event, bei dem ein normaler Listener ab Radius 10 abbricht, plus einem `LOWEST`- und einem Monitor-Listener:

```
[guide] LOWEST saw radius=5.0              ← Radius 5: nicht abgebrochen, LOWEST bekommt es
                                           ← Radius 20: abgebrochen, LOWEST bekommt es NICHT
[guide] MONITOR radius=5.0 cancelled=false
[guide] MONITOR radius=20.0 cancelled=true ← Monitor sieht beides und weiß, ob abgebrochen wurde
```

**Monitor-Listener** sind zum Beobachten (Statistik, Log, „hat das geklappt?“), nicht zum Ändern. Bei abbrechbaren Events meldet man sie mit zwei Parametern an:

```java
WyvernRoarEvent.BUS.addListener((event, cancelled) -> System.out.println("cancelled=" + cancelled));
```

> **Stolperfalle beim Anmelden per Lambda:** Bei **abbrechbaren** Events gibt es `addListener` für Handler *mit* und *ohne* `boolean`-Rückgabe. Ein Einzeiler wie `event -> System.out.println(...)` ist dann mehrdeutig: *„reference to addListener is ambiguous“*. Lösung: geschweifte Klammern `event -> { System.out.println(...); }` oder eine Methode mit eindeutigem Rückgabetyp.

## 7. Eigene Events

Eigene Events lohnen sich, wenn **andere** (andere Mods, Addons zu deiner Mod, oder du selbst an anderer Stelle) in deinen Ablauf eingreifen sollen – ohne deinen Code zu ändern. Genau so macht es Forge mit Vanilla.

### Veränderbares Event

```java
// Eigenes Event: "ein Spieler bekommt gleich Edelsteine". Listener dürfen die Menge ändern.
public final class GemDropEvent extends MutableEvent {
    public static final EventBus<GemDropEvent> BUS = EventBus.create(GemDropEvent.class);

    private final Player player;
    private int amount;

    public GemDropEvent(Player player, int amount) {
        this.player = player;
        this.amount = amount;
    }

    public Player getPlayer() { return player; }
    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = Math.max(0, amount); }
}
```

### Abbrechbares Event

```java
// Eigenes abbrechbares Event: "der Boss will gleich brüllen". Listener dürfen es verhindern.
public record WyvernRoarEvent(Entity wyvern, double radius) implements RecordEvent, Cancellable {
    public static final CancellableEventBus<WyvernRoarEvent> BUS = CancellableEventBus.create(WyvernRoarEvent.class);
}
```

| Teil | Bedeutung |
|---|---|
| `public static final … BUS = EventBus.create(…)` | jedes Event braucht seinen Bus. **Genau so** benennen (`BUS`), dann findet `@SubscribeEvent` ihn automatisch |
| `EventBus` / `CancellableEventBus` | normal / abbrechbar (dann zusätzlich `implements Cancellable`) |
| `extends MutableEvent` | Werte änderbar (Setter) |
| `record … implements RecordEvent` | Werte fest |

### Auslösen

```java
// an der Stelle in DEINEM Code, wo das Ereignis passiert:
GemDropEvent gems = GemDropEvent.BUS.fire(new GemDropEvent(player, 1));   // fire = schicken und Event zurückgeben
giveGems(player, gems.getAmount());                                        // das (evtl. geänderte) Ergebnis benutzen

if (!WyvernRoarEvent.BUS.post(new WyvernRoarEvent(this, 16.0))) {          // post = true, wenn abgebrochen
    roar();
}
```

| Methode | Rückgabe | wann benutzen |
|---|---|---|
| `BUS.post(event)` | `boolean` – abgebrochen? | abbrechbare Events |
| `BUS.fire(event)` | das Event-Objekt | wenn Listener Werte ändern sollen |

Andere hören darauf genau wie auf Forge-Events: `@SubscribeEvent public static void x(GemDropEvent e)` oder `GemDropEvent.BUS.addListener(...)`.

## 8. Das richtige Event finden

1. **Im Paket stöbern:** In IntelliJ unter *External Libraries → forge-26.3-… → net.minecraftforge.event* (und `client.event`). Die Namen sind sprechend, und über jeder Klasse steht ein Kommentar, **wann** sie feuert und ob sie abbrechbar ist.
2. **Rückwärts suchen:** Du weißt, welche Vanilla-Stelle dich interessiert? Darin nach `ForgeEventFactory` suchen – dort wird das passende Event gefeuert. Beispiel: In `LivingEntity.die(...)` steht gleich in der ersten Zeile `ForgeEventFactory.onLivingDeath(this, source)` – das ist `LivingDeathEvent`, und ein paar Zeilen weiter `onLivingDrops(...)` für `LivingDropsEvent`.
3. **Wo wird ein Event gefeuert?** Auf den Event-Klassennamen klicken und **Alt + F7** (Find Usages): zeigt, an welchen Stellen es erzeugt wird. So siehst du genau, was vorher und nachher passiert.
4. **Gibt es keins?** Dann bleibt: Vanilla-Klasse in einer eigenen Klasse überschreiben (für eigene Blöcke/Mobs), datengetrieben lösen (Loot-Tables, Tags) oder – für Fortgeschrittene – ein Mixin.

## 9. Typische Fehler

| Problem | Ursache | Lösung |
|---|---|---|
| Handler läuft nie | nicht angemeldet / falscher Import / nicht `static` ([22](22-Handler.md#7-typische-fehler)) | – |
| Mod-Event (`FMLCommonSetupEvent`) mit `.BUS` angemeldet – rot | Mod-Events haben keinen globalen Bus | `FMLCommonSetupEvent.getBus(modBusGroup)` oder `@SubscribeEvent` |
| Anmelde-Event zu spät angemeldet, Handler läuft nie | `SelfDestructing`-Event ist schon vorbei | im Mod-Konstruktor bzw. per `@SubscribeEvent` anmelden |
| `reference to addListener is ambiguous` | Einzeiler-Lambda bei abbrechbarem Event | `event -> { …; }` |
| Änderung am Event wirkt nicht | anderer Listener mit niedrigerer Priorität überschreibt sie, oder Event wurde vorher abgebrochen | Priorität anpassen, Monitor-Listener zum Nachsehen |
| Event feuert doppelt | feuert auf Server **und** Client (Ticks, Interaktion) | Seite prüfen |
| Eigenes Event wird nicht gefunden | Feld heißt nicht `BUS` oder ist nicht `public static final` | genau so benennen |

## Checkliste

- [ ] Gruppe klar: Mod-Lebenszyklus (einmal, `getBus(modBusGroup)`), Spiel-Event (`.BUS`) oder Client-Event (`Dist.CLIENT`)?
- [ ] Event-Klasse angeschaut: Record oder Mutable? Abbrechbar? Pre/Post?
- [ ] Handler liest / ändert / bricht ab – passend zur Bauform
- [ ] Priorität nur ändern, wenn es einen Grund gibt
- [ ] bei eigenen Events: `BUS`-Feld, `post` oder `fire`, Ergebnis wirklich benutzen

## Zum Ausprobieren

- Feuere in deinem Boss ([10](10-Boss-Funktionen.md)) ein eigenes `BossPhaseChangeEvent(int newPhase)`, wenn er unter 50 % Leben fällt, und schreib einen Listener, der dann eine Nachricht an alle Spieler schickt.
- Schreib einen `MONITOR`-Listener für `BlockEvent.BreakEvent`, der zählt, wie viele Abbau-Versuche insgesamt abgebrochen wurden.
- Such mit **Alt + F7** heraus, wo `LivingDropsEvent` gefeuert wird, und lies den Code davor und danach.
