# 26 – `@SubscribeEvent`

`@SubscribeEvent` macht aus einer normalen Methode einen **Event-Listener** ([24](24-Listener.md)). Dieses Kapitel erklärt die Annotation vollständig: wo sie wirkt, welche Formen die Methode haben darf, was `priority` und `alwaysCancelling` tun, und was die Fehlermeldungen bedeuten.

> ✅ Getestet mit Forge 26.3-66.1.0: Alle Fehlermeldungen unten sind echt – die Compile-Fehler kommen vom Annotations-Prüfer `eventbus-validator`, die Laufzeit-Fehler von einem Server-Start. Private Methoden und Vererbung wurden auf einem Server ausprobiert.

## 1. Was die Annotation macht

```java
@Mod.EventBusSubscriber(modid = MyMod.MODID)
public final class ModGameEvents {
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) { … }
}
```

`@SubscribeEvent` („abonnieren“) heißt: *„Diese Methode will benachrichtigt werden.“* **Für welches Event**, liest Forge am **Typ des ersten Parameters** ab (`PlayerLoggedInEvent`). Der Methodenname ist egal.

Die Annotation allein tut **nichts**. Sie wird erst wirksam, wenn jemand die Klasse oder das Objekt durchsucht:

| Wer sucht? | Methoden müssen sein | Kapitel |
|---|---|---|
| `@Mod.EventBusSubscriber` an der Klasse | `static` | [30](30-EventBusSubscriber.md) |
| `BusGroup.DEFAULT.register(MethodHandles.lookup(), objekt)` | **nicht** `static` | [29](29-BusGroup.md), [24](24-Listener.md#c-ein-objekt-mit-eigenem-zustand) |
| `BusGroup.DEFAULT.register(MethodHandles.lookup(), Klasse.class)` | `static` | [29](29-BusGroup.md) |

Eine `@SubscribeEvent`-Methode in einer Klasse **ohne** eins davon wird nie aufgerufen – ohne Fehlermeldung.

## 2. Die erlaubten Formen

| Form | Bedeutung | nur bei abbrechbaren Events? |
|---|---|---|
| `void m(XyzEvent e)` | zuhören, lesen, ändern | nein |
| `boolean m(XyzEvent e)` | `true` = abbrechen | **ja** |
| `@SubscribeEvent(alwaysCancelling = true) void m(XyzEvent e)` | bricht jedes Mal ab | **ja** |
| `@SubscribeEvent(priority = Priority.MONITOR) void m(XyzEvent e, boolean cancelled)` | beobachten, erfährt ob abgebrochen | **ja** |

Alles andere ist ein Fehler (Abschnitt 5).

## 3. `priority`

```java
@SubscribeEvent(priority = Priority.HIGH)
public static void early(GemDropEvent event) { … }
```

| Wert | Bedeutung |
|---|---|
| `Priority.HIGHEST` | ganz zuerst |
| `Priority.HIGH` | früh |
| `Priority.NORMAL` | Standard (wenn nichts angegeben) |
| `Priority.LOW` | spät |
| `Priority.LOWEST` | ganz zuletzt (von denen, die ändern dürfen) |
| `Priority.MONITOR` | nur beobachten, nach allen anderen; darf nichts ändern und nicht abbrechen |

`Priority` kommt aus `net.minecraftforge.eventbus.api.listener`. Die Werte sind Zahlen (`byte`), keine Enum – deshalb `Priority.HIGH`, nicht `EventPriority.HIGH` wie in alten Versionen.

**Wann ändern?** Nur mit Grund: Willst du Werte setzen, **bevor** andere Mods sie lesen → `HIGH`. Willst du das **Endergebnis** sehen oder korrigieren → `LOW`. Nach einem Abbruch bekommen niedrigere Prioritäten das Event nicht mehr ([23](23-Events.md#6-reihenfolge-priorität-und-monitor)).

## 4. `alwaysCancelling`

```java
@SubscribeEvent(alwaysCancelling = true)
public static void noBreaking(BlockEvent.BreakEvent event) {}   // void! bricht immer ab
```

Für Listener, die **ohne Bedingung** abbrechen. Die Methode muss `void` sein. Schreibst du stattdessen eine `boolean`-Methode, die immer `true` zurückgibt, schlägt der Prüfer genau das vor (siehe Warnung unten).

Praktisch vor allem mit `addListener(true, …)` zum An- und Ausschalten ([24](24-Listener.md#5-listener-im-laufenden-spiel-an--und-ausschalten)).

## 5. Fehlermeldungen beim Kompilieren

In deiner `build.gradle` steht:

```groovy
annotationProcessor 'net.minecraftforge:eventbus-validator:7.0.6'
```

Das ist ein **Annotations-Prozessor**: ein Programm, das beim Kompilieren deine `@SubscribeEvent`-Methoden prüft. Echte Meldungen:

| Was falsch ist | Meldung |
|---|---|
| `boolean` bei einem **nicht** abbrechbaren Event | *error: Return type boolean is only valid for cancellable events* |
| Rückgabetyp weder `void` noch `boolean` | *error: Invalid return type: int (expected void or boolean)* |
| erster Parameter ist kein Event | *error: First parameter of a @SubscribeEvent method must be an event* |
| zweiter Parameter, aber kein `boolean` | *error: Second parameter of a cancellation-aware monitoring listener must be a boolean* |
| `(event, boolean cancelled)` ohne `priority = MONITOR` | *error: Cancellation-aware monitoring listeners must have a priority of MONITOR* |
| `alwaysCancelling` bei nicht abbrechbarem Event | *error: Always cancelling listeners are only valid for cancellable events* |
| `boolean`-Methode gibt immer `false` zurück | *warning: Listener always returns false, consider using a void return type instead* |
| `boolean`-Methode gibt immer `true` zurück | *warning: Listener always returns true, consider using a void return type combined with @SubscribeEvent(alwaysCancelling = true) or CancellableEventBus#addListener(true, ...) instead* |

Warnungen (`warning`) verhindern das Bauen nicht, sind aber fast immer berechtigt.

## 6. Fehlermeldungen beim Starten

Manche Fehler findet der Prüfer nicht. Dann bricht das **Laden der Mod** ab (*„Mod Loading has failed“*). Die wichtige Zeile beginnt mit `java.lang.IllegalArgumentException: Failed to register …`:

| Was falsch ist | Meldung (gekürzt) |
|---|---|
| Methode **nicht `static`** in einer `@Mod.EventBusSubscriber`-Klasse | *Failed to register …R1.notStatic: Listener method needs to be static* |
| `alwaysCancelling = true` mit `boolean`-Rückgabe | *Always cancelling listeners must have a void return type* |
| Mod-Event (z. B. `FMLCommonSetupEvent`) in einer Klasse mit `bus = Bus.FORGE` | *Event type …FMLCommonSetupEvent is on the mod BusGroup but you are asking to register it on the default BusGroup … To fix this, remove the bus param from your @EventBusSubscriber annotation …* |
| Objekt mit nur **einer** Listener-Methode über `BusGroup.register` | *Only a single listener found. You should directly call addListener() …* ([24](24-Listener.md)) |
| Unterklasse ohne eigene `@SubscribeEvent`-Methoden über `BusGroup.register` | *No declared methods found … listener inheritance is not supported … use @Override and @SubscribeEvent on the method in the subclass* ([25](25-Override.md#7-override-und-subscribeevent-zusammen)) |

### Was **kein** Fehler ist (getestet)

- **`private`-Methoden** werden in Forge 26.3 in `@Mod.EventBusSubscriber`-Klassen trotzdem gefunden und aufgerufen. Üblich und empfohlen ist trotzdem `public` – dann funktioniert es auch mit anderen Anmelde-Wegen sicher.
- Eine Klasse mit nur **einer** `@SubscribeEvent`-Methode ist mit `@Mod.EventBusSubscriber` erlaubt. Die „single listener“-Regel gilt nur für `BusGroup.register`.

## 7. Der richtige Import

```java
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;   // ✅ richtig (Forge 26.x)
```

| Falscher Import | Woher | Folge |
|---|---|---|
| `com.google.common.eventbus.Subscribe` | Google-Bibliothek Guava | wird ignoriert, Methode läuft nie |
| `net.minecraftforge.eventbus.api.SubscribeEvent` | ältere Forge-Versionen bis Minecraft 1.21.5 (EventBus 6, steht in vielen Tutorials) | kompiliert in 26.x nicht |
| `org.greenrobot.eventbus.Subscribe` u. ä. | andere Bibliotheken | wird ignoriert |

IntelliJ schlägt beim Tippen von `@Subscribe` mehrere vor – den aus `net.minecraftforge.eventbus.api.listener` wählen.

## 8. `@SubscribeEvent` oder `addListener`?

| | `@SubscribeEvent` | `BUS.addListener(...)` |
|---|---|---|
| Anmeldung | automatisch (über Klasse/Objekt) | von Hand |
| Prüfung beim Kompilieren | ja (Validator) | nur Typprüfung von Java |
| als Lambda | nein | ja |
| wieder abmelden | nur ganze Gruppe (`unregister`) | einzeln (`removeListener`) |
| Lesbarkeit bei vielen Listenern | gut (eine Klasse, viele Methoden) | wird unübersichtlich |

Vergleich mit Beispielen: [22](22-Handler.md#welcher-weg-wann).

## Checkliste

- [ ] Import aus `net.minecraftforge.eventbus.api.listener`
- [ ] Klasse hat `@Mod.EventBusSubscriber` (→ Methoden `static`) **oder** wird über `BusGroup.register` angemeldet (→ Objekt-Methoden nicht `static`)
- [ ] erster Parameter = das Event
- [ ] `void`, oder `boolean` nur bei abbrechbaren Events
- [ ] `priority` / `alwaysCancelling` nur mit Grund
- [ ] Compiler-Warnungen des Validators gelesen

## Zum Ausprobieren

- Schreib absichtlich `public static boolean` für `PlayerLoggedInEvent` und bau das Projekt. Lies die Meldung, dann korrigiere.
- Schreib zwei `@SubscribeEvent`-Methoden für dasselbe Event mit `HIGH` und `LOW`, die etwas ins Log schreiben. In welcher Reihenfolge erscheinen sie?
- Schreib einen Monitor-Listener `(BlockEvent.BreakEvent e, boolean cancelled)` mit `priority = Priority.MONITOR`, der zählt, wie oft Abbauen abgebrochen wurde.
