# 30 – `@Mod.EventBusSubscriber`

`@Mod.EventBusSubscriber` steht über einer **Klasse** und sagt Forge: *„Durchsuche diese Klasse beim Start und melde alle `@SubscribeEvent`-Methoden an.“* Es ist der bequemste Weg, Listener anzumelden – keine Zeile im Mod-Konstruktor nötig.

> ✅ Getestet mit Forge 26.3-66.1.0 auf einem Server: Klasse mit `Dist.CLIENT` wurde auf dem Server **nicht geladen**, Klasse mit `Dist.DEDICATED_SERVER` schon; falscher `bus`-Parameter und nicht-`static` Methoden ergeben die unten gezeigten echten Fehler.

## 1. Grundform

```java
@Mod.EventBusSubscriber(modid = MyMod.MODID)
public final class ModGameEvents {
    private ModGameEvents() {}

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) { … }

    @SubscribeEvent
    public static void onSetup(FMLCommonSetupEvent event) { … }   // auch Mod-Events gehen hier
}
```

- `Mod.EventBusSubscriber` ist eine **innere Annotation** der Annotation `@Mod` (deshalb `Mod.` davor). Import: `net.minecraftforge.fml.common.Mod`.
- Forge findet die Klasse **automatisch** (beim Durchsuchen deiner Mod-Datei) – du musst sie nirgends erwähnen.
- Alle `@SubscribeEvent`-Methoden müssen **`static`** sein (*„Listener method needs to be static“*).
- `private` Konstruktor und `final`: Niemand soll ein Objekt dieser Klasse erzeugen – sie ist nur ein Behälter für Listener.

## 2. Die Parameter

| Parameter | Standard | Bedeutung |
|---|---|---|
| `modid` | `""` | zu welcher Mod die Klasse gehört. **Immer angeben** (`MyMod.MODID`) |
| `value` | `{Dist.CLIENT, Dist.DEDICATED_SERVER}` (beide) | auf welcher **Programm-Seite** die Klasse geladen wird |
| `bus` | `Bus.BOTH` | an welche [BusGroup (29)](29-BusGroup.md) die Methoden gehen: `BOTH` (automatisch richtig), `FORGE` (nur Spiel-Events), `MOD` (nur Mod-Events) |

### `value` – Client, Server oder beide

```java
@Mod.EventBusSubscriber(modid = MyMod.MODID, value = Dist.CLIENT)
public final class ModClientEvents { … }   // Renderer, Tasten, HUD
```

| `value` | wird geladen … |
|---|---|
| (weglassen) | überall |
| `Dist.CLIENT` | nur im **Spiel-Programm** (auch im Einzelspieler – der läuft im Client-Programm) |
| `Dist.DEDICATED_SERVER` | nur auf einem **eigenständigen Server** (nicht im Einzelspieler!) |

Getestet auf einem Server:

```
[guide] ServerOnly class LOADED          ← value = Dist.DEDICATED_SERVER
                                         ← value = Dist.CLIENT: Klasse wurde NICHT geladen
```

**Das ist der Grund, warum Client-Code in `Dist.CLIENT`-Klassen sicher ist:** Die Klasse wird auf einem Server gar nicht erst geladen – also stört es nicht, dass sie `Minecraft`, `KeyMapping` oder Renderer benutzt, die es dort nicht gibt.

> `Dist` meint das **Programm** (Spiel oder eigenständiger Server), nicht die logische Seite. Im Einzelspieler läuft ein Server **im** Spiel-Programm mit – dort ist `Dist` also `CLIENT`. Deshalb ist `Dist.DEDICATED_SERVER` fast nie, was man will; für „nur Server-Logik“ prüft man stattdessen im Code `!level.isClientSide()` ([00](00-Uebersicht.md#client-und-server)).

### `bus` – meistens weglassen

Mit dem Standard `BOTH` ordnet Forge jede Methode automatisch richtig zu: Mod-Events an die Mod-Gruppe, Spiel-Events an `DEFAULT`. Setzt man `bus` falsch:

```java
@Mod.EventBusSubscriber(modid = MyMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class R3 {
    @SubscribeEvent public static void onSetup(FMLCommonSetupEvent event) { }
}
```

```
Failed to register …R3.modEventOnForgeBus: Event type …FMLCommonSetupEvent is on the mod BusGroup but you are asking
to register it on the default BusGroup (BusGroup.DEFAULT/EventBusSubscriber.Bus.FORGE). To fix this, remove the bus
param from your @EventBusSubscriber annotation or move this event listener to another class.
```

> In **alten Tutorials** steht oft `bus = Mod.EventBusSubscriber.Bus.MOD` für Registrierungs-Events, weil man das früher angeben **musste**. In 26.x: weglassen.

## 3. Wie man Klassen sinnvoll aufteilt

```
event/ModGameEvents.java        @Mod.EventBusSubscriber(modid = …)                      Spiel-Events
event/ModSpawnEvents.java       @Mod.EventBusSubscriber(modid = …)                      Spawn-Regeln
command/ModCommands.java        @Mod.EventBusSubscriber(modid = …)                      RegisterCommandsEvent
client/ModClientEvents.java     @Mod.EventBusSubscriber(modid = …, value = Dist.CLIENT) Renderer, HUD
client/ModKeyMappings.java      @Mod.EventBusSubscriber(modid = …, value = Dist.CLIENT) Tasten
```

Faustregel: **eine Klasse pro Thema**, und alles mit Grafik/Tastatur in `Dist.CLIENT`-Klassen.

## 4. Vergleich mit den anderen Wegen

| | `@Mod.EventBusSubscriber` | `BusGroup.register(lookup, …)` | `XyzEvent.BUS.addListener(...)` |
|---|---|---|---|
| Wo anmelden? | nirgends – automatisch | im Code (z. B. Konstruktor) | im Code |
| Methoden | `static` | Klasse: `static`, Objekt: nicht `static` | beliebige Methode/Lambda |
| nur eine Methode erlaubt? | ja | **nein** (mind. 2) | ja |
| abmelden | nein | ja (`unregister`) | ja (`removeListener`) |
| Client-Schutz eingebaut | ja (`value = Dist.CLIENT`) | nein | nein |

## 5. Typische Fehler

| Problem | Ursache | Lösung |
|---|---|---|
| Methoden werden nie aufgerufen | Annotation fehlt / falsche `modid` / falscher `@SubscribeEvent`-Import | prüfen ([26](26-SubscribeEvent.md#7-der-richtige-import)) |
| *Listener method needs to be static* | Methode ohne `static` | `static` ergänzen (oder Klasse über `BusGroup.register` als Objekt anmelden) |
| *is on the mod BusGroup but you are asking to register it on the default BusGroup* | `bus = Bus.FORGE` mit Mod-Event | `bus` weglassen |
| Server stürzt ab: `NoClassDefFoundError: net/minecraft/client/…` | Client-Code in Klasse ohne `value = Dist.CLIENT` | Client-Teile in eigene `Dist.CLIENT`-Klasse |
| Listener läuft im Einzelspieler nicht | `value = Dist.DEDICATED_SERVER` | weglassen, im Code `isClientSide()` prüfen |
| Listener läuft doppelt | zusätzlich noch per `addListener` angemeldet | nur einen Weg |

## Checkliste

- [ ] `@Mod.EventBusSubscriber(modid = MyMod.MODID)` über der Klasse
- [ ] Client-Klassen mit `value = Dist.CLIENT`
- [ ] `bus` weglassen
- [ ] alle Listener-Methoden `public static` mit `@SubscribeEvent`
- [ ] Klasse `final` mit `private` Konstruktor

## Zum Ausprobieren

- Leg eine Klasse mit `value = Dist.CLIENT` an, die in einem `static { … }`-Block etwas ins Log schreibt. Starte einmal `runClient` und einmal `runServer` – wann erscheint die Ausgabe?
- Setz absichtlich `bus = Mod.EventBusSubscriber.Bus.FORGE` an deine Klasse mit `FMLCommonSetupEvent` und lies die Fehlermeldung.
- Verschiebe einen Listener aus einer `@Mod.EventBusSubscriber`-Klasse in den Mod-Konstruktor (mit `addListener`). Was musst du ändern?
