# 29 – `BusGroup` (und `getModBusGroup()`)

Eine **BusGroup** ist eine **Sammlung von Bussen**. Sie erklärt, warum man Mod-Events anders anmeldet als Spiel-Events, und sie ist der Weg, um ganze Klassen oder Objekte mit `@SubscribeEvent`-Methoden anzumelden.

> ✅ Getestet mit Forge 26.3-66.1.0 auf einem Server: Namen der Gruppen ausgegeben, Klasse und Objekt über `register` angemeldet, `unregister` (abgemeldete Listener liefen nicht mehr), eigene BusGroup mit eigenem Event. Fehlermeldungen sind echt.

## 1. Was ist eine BusGroup?

Jeder [EventBus (27)](27-EventBus.md) gehört zu genau **einer** Gruppe. In einer normalen Mod gibt es zwei, die wichtig sind:

| Gruppe | Name (im Test ausgegeben) | Enthält die Busse von … | Wie man sie bekommt |
|---|---|---|---|
| **Default-Gruppe** | `default` | allen Spiel- und Client-Events (`PlayerLoggedInEvent`, `BlockEvent.BreakEvent`, `TickEvent` …) – gemeinsam für **alle** Mods | `BusGroup.DEFAULT` |
| **Mod-Gruppe** deiner Mod | `modBusFormymod` | den Mod-Lebenszyklus-Events (`FMLCommonSetupEvent`, `FMLClientSetupEvent`, …) – **eine eigene pro Mod** | `context.getModBusGroup()` |

**Warum eine eigene Gruppe pro Mod?** Mod-Events betreffen jede Mod einzeln: „Mod *mymod* ist jetzt eingerichtet.“ Jede Mod hat deshalb ihre eigenen Busse für diese Events – sonst würde deine Mod die Setup-Events aller anderen Mods mitbekommen.

## 2. Die Methoden

| Methode | Bedeutung |
|---|---|
| `BusGroup.DEFAULT` | die gemeinsame Gruppe für Spiel-Events |
| `register(MethodHandles.lookup(), objekt)` | alle `@SubscribeEvent`-Methoden (nicht `static`) dieses Objekts anmelden |
| `register(MethodHandles.lookup(), Klasse.class)` | alle `static` `@SubscribeEvent`-Methoden dieser Klasse anmelden |
| `unregister(listener)` | die Rückgabe von `register` wieder abmelden |
| `name()` | Name der Gruppe |
| `BusGroup.create("name")` | eigene Gruppe erzeugen (selten nötig) |
| `startup()`, `shutdown()`, `dispose()`, `trim()` | Lebenszyklus – macht Forge für seine Gruppen selbst, **nicht** selbst aufrufen |

`MinecraftForge.EVENT_BUS.register(objekt)` aus alten Tutorials funktioniert in 26.x noch: Es ist eine Migrationshilfe, die an `BusGroup.DEFAULT.register(...)` weiterleitet ([27](27-EventBus.md#1-was-ist-ein-eventbus)). Für neuen Code direkt `BusGroup.DEFAULT` benutzen.

### Anmelden und Abmelden

```java
// ganze Klasse mit static-Methoden:
Collection<EventListener> listeners = BusGroup.DEFAULT.register(MethodHandles.lookup(), StaticGroup.class);
// Test-Ausgabe: "registered 2 static listeners"

// Objekt mit Zustand (Kapitel 24):
BusGroup.DEFAULT.register(MethodHandles.lookup(), MOB_COUNTER);

// wieder abmelden:
BusGroup.DEFAULT.unregister(listeners);
```

- `MethodHandles.lookup()` (aus `java.lang.invoke`) erlaubt Forge, deine Methoden aufzurufen – einfach so hinschreiben.
- Die Rückgabe ist eine `Collection<EventListener>` – alle Listener, die gefunden wurden. Für `unregister` aufbewahren.

### Regeln, die beim Anmelden geprüft werden (echte Meldungen)

| Regel | Meldung |
|---|---|
| mindestens **zwei** `@SubscribeEvent`-Methoden | *Only a single listener found. You should directly call addListener() on the EventBus of … instead.* |
| Methoden müssen in der Klasse **selbst** stehen (keine Vererbung) | *No declared methods found in … Note that listener inheritance is not supported. If you are trying to inherit listeners, please use @Override and @SubscribeEvent on the method in the subclass.* ([25](25-Override.md#7-override-und-subscribeevent-zusammen)) |
| Mod-Events nur an der Mod-Gruppe, Spiel-Events nur an `DEFAULT` | *Event type … is on the mod BusGroup but you are asking to register it on the default BusGroup …* (und umgekehrt) |

## 3. Die Mod-BusGroup

Im Konstruktor deiner Hauptklasse:

```java
public MyMod(FMLJavaModLoadingContext context) {
    var modBusGroup = context.getModBusGroup();                     // ← die Gruppe DEINER Mod

    ModBlocks.BLOCKS.register(modBusGroup);                         // DeferredRegister an die Gruppe hängen
    ModItems.ITEMS.register(modBusGroup);
    FMLCommonSetupEvent.getBus(modBusGroup).addListener(this::commonSetup);   // Bus eines Mod-Events holen
    context.registerConfig(ModConfig.Type.COMMON, MyModConfig.SPEC);
}
```

| Code | Erklärung |
|---|---|
| `context.getModBusGroup()` | holt die Gruppe deiner Mod |
| `DeferredRegister.register(modBusGroup)` | das Register hört auf die Registrierungs-Events **deiner** Mod-Gruppe – deshalb braucht es die Gruppe |
| `FMLCommonSetupEvent.getBus(modBusGroup)` | Mod-Events haben **kein** festes `BUS`-Feld, sondern liefern ihren Bus **für eine bestimmte Gruppe** |

Mod-Events erkennt man daran, dass sie `IModBusEvent` sind und eine statische Methode `getBus(BusGroup)` haben (`FMLCommonSetupEvent`, `FMLClientSetupEvent`, `FMLLoadCompleteEvent`, …). Viele Registrierungs-Events (`EntityAttributeCreationEvent`, `RegisterKeyMappingsEvent`, `EntityRenderersEvent.RegisterRenderers`) haben dagegen ein normales `BUS`-Feld – im Zweifel: Strg + Klick auf das Event.

Mit `@Mod.EventBusSubscriber` musst du das nicht unterscheiden – Forge ordnet jede Methode automatisch der richtigen Gruppe zu ([30](30-EventBusSubscriber.md)).

## 4. Eigene BusGroup (für Fortgeschrittene)

```java
public static final BusGroup MY_GROUP = BusGroup.create("mymod_custom");

public record PingEvent(String text) implements RecordEvent {
    public static final EventBus<PingEvent> BUS = EventBus.create(MY_GROUP, PingEvent.class);
}
```

Im Test funktionierte das sofort (`ping received: hello`). Normalerweise reicht für eigene Events `EventBus.create(Klasse)` – das legt den Bus in der Default-Gruppe an. Eine eigene Gruppe lohnt sich nur, wenn man viele eigene Events gemeinsam verwalten (oder als Gruppe entsorgen) will, z. B. in einer großen Bibliotheks-Mod.

## 5. Typische Fehler

| Problem | Ursache | Lösung |
|---|---|---|
| `FMLCommonSetupEvent.BUS` gibt es nicht | Mod-Event | `getBus(modBusGroup)` |
| Items/Blöcke existieren nicht | `register(modBusGroup)` beim DeferredRegister vergessen | im Konstruktor ergänzen |
| *Only a single listener found* | `register` mit nur einer Methode | `XyzEvent.BUS.addListener(...)` |
| *listener inheritance is not supported* | Unterklasse ohne eigene `@SubscribeEvent`-Methoden | Methoden mit `@Override @SubscribeEvent` neu angeben |
| *is on the mod BusGroup but you are asking to register it on the default BusGroup* | Mod-Event an `BusGroup.DEFAULT` oder in Klasse mit `bus = Bus.FORGE` | Mod-Event über `modBusGroup` / ohne `bus`-Parameter |
| `startup()`/`dispose()` selbst aufgerufen, alles kaputt | Lebenszyklus gehört Forge | nicht aufrufen |

## Checkliste

- [ ] `modBusGroup` aus `context.getModBusGroup()` für DeferredRegister und Mod-Events
- [ ] Spiel-Events: `BusGroup.DEFAULT` bzw. `XyzEvent.BUS`
- [ ] `register(...)`: mindestens zwei Methoden, Rückgabe für `unregister` aufheben
- [ ] keine Vererbung von Listenern ohne `@Override @SubscribeEvent`

## Zum Ausprobieren

- Gib im Konstruktor `context.getModBusGroup().name()` und `BusGroup.DEFAULT.name()` aus.
- Schreib eine Klasse mit drei `static` `@SubscribeEvent`-Methoden und melde sie per `BusGroup.DEFAULT.register(MethodHandles.lookup(), Klasse.class)` an. Wie viele `EventListener` bekommst du zurück?
- Melde sie mit einem Befehl ([13](13-Befehle.md)) ab und wieder an.
