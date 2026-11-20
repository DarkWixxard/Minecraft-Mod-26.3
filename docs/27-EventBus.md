# 27 – `EventBus` (und das Feld `BUS`)

Überall in diesem Leitfaden steht `XyzEvent.BUS.addListener(...)`. Dieses Kapitel erklärt, was hinter `BUS` steckt: die Klasse **`EventBus`** – was sie ist, welche Methoden sie hat, und wie man sie für eigene Events erzeugt.

> ✅ Getestet mit Forge 26.3-66.1.0 (EventBus 7.0.6): alle Methoden unten aufgerufen; eigene Events auf dem Server ausgelöst; die Warnungen des Prüfers sind echte Compiler-Ausgaben.

## 1. Was ist ein `EventBus`?

Ein **Bus** ist ein Verteiler: eine Liste von Listenern **für genau ein Event**. Wird das Event „gepostet“, geht der Bus seine Liste durch und ruft jeden Listener auf.

```
               PlayerLoggedInEvent.BUS
             ┌──────────────────────────┐
post(event) →│ Listener 1  (deine Mod)  │→ aufgerufen
             │ Listener 2  (andere Mod) │→ aufgerufen
             │ Listener 3  (Forge)      │→ aufgerufen
             └──────────────────────────┘
```

Seit Forge für 1.21.6 (EventBus 7) hat **jedes Event seinen eigenen Bus**, als Feld direkt in der Event-Klasse:

```java
public record PlayerLoggedInEvent(Player getEntity) implements RecordEvent, PlayerEvent {
    public static final EventBus<PlayerLoggedInEvent> BUS = EventBus.create(PlayerLoggedInEvent.class);
}
```

> **Früher** (EventBus 6, bis Minecraft 1.21.5, viele Tutorials): **ein** großer Bus für alle Events: `MinecraftForge.EVENT_BUS.addListener(...)`, `MinecraftForge.EVENT_BUS.post(...)`, `modEventBus.addListener(...)`.
>
> In 26.x gibt es `MinecraftForge.EVENT_BUS` **nur noch als Migrationshilfe** (Klasse `EventBusMigrationHelper`): Sie kann `register(Klasse oder Objekt)` und `unregister(...)` – das leitet an [`BusGroup.DEFAULT`](29-BusGroup.md) weiter. `addListener` und `post` gibt es dort **nicht** mehr. Forge selbst schreibt dazu, dass man besser direkt `BusGroup.DEFAULT.register(MethodHandles.lookup(), …)` benutzt, weil die Hilfe auf Java-Interna beruht, die ohne Vorwarnung brechen können.

`EventBus<PlayerLoggedInEvent>`: In den spitzen Klammern steht, **welches** Event dieser Bus verteilt. Darum kann man an diesen Bus nur Listener hängen, die genau dieses Event annehmen – der Compiler prüft das.

## 2. Die Methoden

| Methode | Was sie macht | Rückgabe |
|---|---|---|
| `addListener(listener)` | Listener anhängen (Priorität `NORMAL`) | `EventListener` (zum Abmelden) |
| `addListener(Priority.HIGH, listener)` | mit Priorität | `EventListener` |
| `addListener(eventListener)` | einen früher entfernten Listener wieder anhängen | `EventListener` |
| `removeListener(eventListener)` | Listener abmelden | – |
| `post(event)` | Event an alle schicken | `boolean`: abgebrochen? (bei normalen Bussen immer `false`) |
| `fire(event)` | Event an alle schicken | das Event-Objekt (um geänderte Werte zu lesen) |
| `hasListeners()` | hört überhaupt jemand zu? | `boolean` |
| `EventBus.create(Klasse)` | (statisch) neuen Bus erzeugen | `EventBus` |
| `EventBus.create(busGroup, Klasse)` | neuen Bus in einer bestimmten [BusGroup](29-BusGroup.md) | `EventBus` |

### Listener anhängen und abmelden

```java
EventListener listener = PlayerEvent.PlayerLoggedInEvent.BUS.addListener(event -> { … });
…
PlayerEvent.PlayerLoggedInEvent.BUS.removeListener(listener);
```

Beispiele mit Ein- und Ausschalten: [24](24-Listener.md#4-listener-die-sich-selbst-abmelden).

### Posten – normalerweise macht das Forge

`post` und `fire` rufst du nur für **eigene** Events auf. Forge-Events postet Forge selbst (meist über die Klasse `ForgeEventFactory`).

```java
GemDropEvent gems = GemDropEvent.BUS.fire(new GemDropEvent(player, 1));   // Ergebnis lesen
give(player, gems.getAmount());
```

### `hasListeners()` – Arbeit sparen

```java
if (GemDropEvent.BUS.hasListeners()) {
    GemDropEvent event = GemDropEvent.BUS.fire(new GemDropEvent(player, amount));
    amount = event.getAmount();
}
```

Ein Event-Objekt zu bauen kostet etwas. Bei Events, die sehr oft feuern (jeder Tick, jeder Block), kann man es weglassen, wenn sowieso niemand zuhört. Für den Anfang unnötig – aber gut zu wissen, dass es geht.

## 3. Einen Bus für ein eigenes Event erzeugen

```java
public final class GemDropEvent extends MutableEvent {
    public static final EventBus<GemDropEvent> BUS = EventBus.create(GemDropEvent.class);
    …
}
```

Regeln (der Prüfer `eventbus-validator` achtet darauf):

| Regel | Warum | Meldung bei Verstoß (echt) |
|---|---|---|
| `public static final` | einmal pro Klasse, nie austauschbar, von überall erreichbar | *warning: EventBus field BUS should be final* |
| Feldname **`BUS`** | `@SubscribeEvent` sucht den Bus über diesen Namen | (Listener werden nicht gefunden) |
| Typ passt zum Event (`EventBus<GemDropEvent>` in `GemDropEvent`) | sonst gehen Listener an den falschen Bus | Compiler-Fehler |
| **abbrechbares** Event → `CancellableEventBus` statt `EventBus` | nur der kann abbrechen ([28](28-CancellableEventBus.md)) | *warning: EventBus field %s should be CancellableEventBus<%s> instead of EventBus<%s> because %s inherits from Cancellable* |

(Die letzte Meldung enthält wirklich `%s` statt der Namen – ein kleiner Fehler im Prüfer selbst. Gemeint ist: Feld `BUS` in deinem Event.)

### Welcher Event-Typ braucht welchen Bus?

| Event | Bus |
|---|---|
| `record … implements RecordEvent` | `EventBus` |
| `… extends MutableEvent` | `EventBus` |
| zusätzlich `implements Cancellable` | `CancellableEventBus` |
| Mod-Lebenszyklus-Event (`IModBusEvent`) | kein festes `BUS`-Feld, sondern `getBus(modBusGroup)` ([29](29-BusGroup.md#3-die-mod-busgroup)) |

## 4. Einen Bus finden

- Strg + Klick auf ein Event → in der Klasse steht `public static final … BUS`.
- Steht dort `CancellableEventBus`, ist das Event abbrechbar.
- Steht dort **kein** `BUS`, sondern eine Methode `getBus(BusGroup)`, ist es ein Mod-Event.
- Verschachtelte Events haben jeweils **eigene** Busse: `BlockEvent.BreakEvent.BUS`, `BlockEvent.EntityPlaceEvent.BUS`, `TickEvent.PlayerTickEvent.Post.BUS` …

## 5. Typische Fehler

| Problem | Ursache | Lösung |
|---|---|---|
| `MinecraftForge.EVENT_BUS.addListener(...)` / `.post(...)` rot | Code aus einem alten Tutorial (EventBus 6) – die Migrationshilfe kann nur noch `register`/`unregister` | `XyzEvent.BUS.addListener(...)` / `XyzEvent.BUS.post(...)` oder `@SubscribeEvent` |
| `cannot find symbol: BUS` bei `FMLCommonSetupEvent.BUS` | Mod-Event hat keinen globalen Bus | `FMLCommonSetupEvent.getBus(modBusGroup)` |
| `addListener(...)` rot: falscher Typ | Listener nimmt ein anderes Event als der Bus | Parameter-Typ des Handlers prüfen |
| `boolean`-Listener lässt sich nicht anhängen | normaler `EventBus` kennt kein Abbrechen | nur bei `CancellableEventBus` ([28](28-CancellableEventBus.md)) |
| eigenes Event: Listener mit `@SubscribeEvent` werden nie aufgerufen | Feld heißt nicht `BUS` / nicht `public static final` | genau so benennen |

## Checkliste

- [ ] Bus über `XyzEvent.BUS` (Spiel-Events) bzw. `getBus(modBusGroup)` (Mod-Events)
- [ ] `EventListener` merken, wenn wieder abgemeldet werden soll
- [ ] eigene Events: `public static final EventBus<…> BUS = EventBus.create(….class)`
- [ ] abbrechbar? → `CancellableEventBus`

## Zum Ausprobieren

- Gib beim Start aus, ob `LivingDeathEvent.BUS.hasListeners()` `true` ist – auch ohne eigene Listener? (Forge und andere Mods hören oft schon zu.)
- Mach aus deinem `GemDropEvent` ein `public static EventBus` (ohne `final`) und lies die Warnung beim Bauen.
