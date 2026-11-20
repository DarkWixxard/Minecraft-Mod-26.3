# 28 – `CancellableEventBus`

Ein `CancellableEventBus` ist ein [EventBus (27)](27-EventBus.md) mit einer Zusatzfähigkeit: Listener können das Event **abbrechen**. Dann macht Minecraft das Ereignis nicht (der Block bleibt, der Schaden fällt weg, die Explosion findet nicht statt).

> ✅ Getestet mit Forge 26.3-66.1.0: alle Listener-Formen unten auf einem Server ausgeführt – Abbrechen, Verhalten nach dem Abbrechen, Monitor-Listener und `alwaysCancelling` (Explosionen an/aus). Compiler-Meldungen sind echt.

## 1. Woran erkennt man ihn?

```java
public record LivingDeathEvent(LivingEntity getEntity, DamageSource getSource) implements Cancellable, … {
    public static final CancellableEventBus<LivingDeathEvent> BUS = CancellableEventBus.create(LivingDeathEvent.class);
}
```

- Das Event `implements Cancellable`.
- Das Feld heißt `CancellableEventBus<…>`.

Abbrechbare Events in diesem Leitfaden: `BlockEvent.BreakEvent`, `LivingDeathEvent`, `LivingDropsEvent`, `EntityJoinLevelEvent`, `ExplosionEvent.Start`, `InputEvent.InteractionKeyMappingTriggered.Attack`, `CustomizeGuiOverlayEvent.BossEventProgress` …

## 2. Zusätzliche Methoden

Ein `CancellableEventBus` hat alles von `EventBus` **plus**:

| Methode | Listener-Form | Bedeutung |
|---|---|---|
| `addListener(Predicate<T>)` | `event -> true/false` | `true` = abbrechen |
| `addListener(Priority, Predicate<T>)` | dito mit Priorität | |
| `addListener(true, Consumer<T>)` | `event -> { }` | bricht **immer** ab (`alwaysCancelling`) |
| `addListener(Priority, true, Consumer<T>)` | dito mit Priorität | |
| `addListener(ObjBooleanBiConsumer<T>)` | `(event, cancelled) -> { }` | **Monitor**: beobachten, erfährt ob abgebrochen |
| `post(event)` | – | gibt `true` zurück, wenn **abgebrochen** |

Und natürlich weiterhin `addListener(Consumer<T>)` für Listener, die nur zuhören.

## 3. Die vier Listener-Formen im Beispiel

```java
// 1) Nur zuhören
BlockEvent.BreakEvent.BUS.addListener(event -> { System.out.println("Block abgebaut"); });

// 2) Abbrechen mit Bedingung
BlockEvent.BreakEvent.BUS.addListener(event -> event.getState().is(Blocks.BEDROCK));   // true = abbrechen

// 3) Immer abbrechen (z. B. zum An-/Ausschalten, siehe Kapitel 24)
EventListener blocker = ExplosionEvent.Start.BUS.addListener(true, event -> {});

// 4) Beobachten
BlockEvent.BreakEvent.BUS.addListener((event, cancelled) -> {
    if (cancelled) System.out.println("Ein Abbau wurde verhindert");
});
```

Mit Annotationen ([26](26-SubscribeEvent.md#2-die-erlaubten-formen)):

```java
@SubscribeEvent public static void listen(BlockEvent.BreakEvent e) { … }
@SubscribeEvent public static boolean cancelIf(BlockEvent.BreakEvent e) { return …; }
@SubscribeEvent(alwaysCancelling = true) public static void always(BlockEvent.BreakEvent e) {}
@SubscribeEvent(priority = Priority.MONITOR) public static void watch(BlockEvent.BreakEvent e, boolean cancelled) { … }
```

## 4. Was nach dem Abbrechen passiert

Getestet mit einem eigenen abbrechbaren Event und Listenern in verschiedenen Prioritäten:

```
Radius 5  → kein Listener bricht ab  → alle laufen, auch LOWEST; Monitor: cancelled=false
Radius 20 → NORMAL-Listener bricht ab → LOWEST läuft NICHT mehr;   Monitor: cancelled=true
```

Also:
1. Die Listener laufen nach Priorität.
2. Sobald einer abbricht, **hören die normalen Listener auf**.
3. **Monitor-Listener laufen immer** und bekommen mitgeteilt, ob abgebrochen wurde.
4. `post(...)` liefert dem auslösenden Code `true` → er führt das Ereignis nicht aus.

Folge für dich: Willst du **sicher** etwas tun, auch wenn eine andere Mod abbricht, brauchst du `Priority.HIGHEST` (läuft vor den meisten) oder einen Monitor-Listener (nur zum Beobachten).

## 5. Die Lambda-Falle

Weil es `addListener` mit `Consumer` **und** mit `Predicate` gibt, kann der Compiler bei einem Einzeiler nicht entscheiden, welches gemeint ist:

```java
WyvernRoarEvent.BUS.addListener(Priority.LOWEST, event -> System.out.println("…"));
// error: reference to addListener is ambiguous
```

Lösungen:

```java
WyvernRoarEvent.BUS.addListener(Priority.LOWEST, event -> { System.out.println("…"); });   // { } = Consumer
WyvernRoarEvent.BUS.addListener(Priority.LOWEST, MyListeners::onRoar);                    // Methode mit eindeutigem Rückgabetyp
```

Und Monitor-Listener **ohne** Priorität anmelden – `addListener(Priority.MONITOR, (event, cancelled) -> …)` gibt es nicht (*no suitable method found*). `addListener((event, cancelled) -> …)` ist automatisch ein Monitor.

## 6. Eigenes abbrechbares Event

```java
// Der Boss will gleich brüllen. Listener dürfen es verhindern.
public record WyvernRoarEvent(Entity wyvern, double radius) implements RecordEvent, Cancellable {
    public static final CancellableEventBus<WyvernRoarEvent> BUS = CancellableEventBus.create(WyvernRoarEvent.class);
}

// auslösen:
if (!WyvernRoarEvent.BUS.post(new WyvernRoarEvent(this, 16.0))) {
    roar();   // nur wenn niemand abgebrochen hat
}
```

> ⚠️ Nicht `EventBus.create(...)` für ein abbrechbares Event benutzen (und dann casten) – der Prüfer warnt: *„should call CancellableEventBus#create(Class) directly instead of casting the result from EventBus#create(Class)“*, und mit einem normalen `EventBus` kann niemand abbrechen.

## 7. Abbrechen oder Ergebnis?

Einige Forge-Events haben statt (oder zusätzlich zu) „abbrechen“ ein **Ergebnis** mit drei Werten (`HasResult`, z. B. `BlockEvent.BreakEvent`): erlauben / Standard / verbieten. Das ist feiner: „Standard“ heißt „Vanilla entscheidet“. In der Event-Klasse steht dann `getResult()` / `setResult(...)`.

## Checkliste

- [ ] Event abbrechbar? (`CancellableEventBus`, `implements Cancellable`)
- [ ] passende Form gewählt (zuhören / bedingt abbrechen / immer / beobachten)
- [ ] Einzeiler-Lambdas mit `{ }`, wenn mehrdeutig
- [ ] Priorität bedacht: Nach einem Abbruch laufen niedrigere nicht mehr
- [ ] eigene Events: `CancellableEventBus.create(...)`, Rückgabe von `post` auswerten

## Zum Ausprobieren

- Mach einen Listener, der Creeper-Explosionen verhindert, aber TNT erlaubt (`event.getExplosion()` untersuchen – wer ist die Quelle?).
- Häng an `LivingDeathEvent` einen Listener, der den Tod eines Spielers einmal pro Spielsitzung abbricht und ihn auf 1 Herz setzt (eine Art „Totem“).
- Schreib zwei Listener für dasselbe Event, einer bricht ab (`HIGH`), einer schreibt ins Log (`LOW`). Prüfe, dass der zweite nicht mehr läuft – und mach ihn dann zum Monitor.
