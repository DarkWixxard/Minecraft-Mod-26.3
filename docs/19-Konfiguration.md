# 19 – Konfigurationsdatei

Mit einer Konfigurationsdatei können Spieler und Server-Admins Werte ändern, **ohne** den Code anzufassen: Drop-Chancen, Schaden, ob eine Funktion an ist …

> ✅ Getestet mit Forge 26.3-66.1.0 auf dem Server: Datei `config/mymod-common.toml` wird beim ersten Start erzeugt (Inhalt unten ist die echte Datei), `/mymod config` liest die Werte.

## 1. Die Konfiguration definieren

```java
// Erzeugt beim ersten Start config/mymod-common.toml
public final class MyModConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue WELCOME_MESSAGE = BUILDER
            .comment("Show a welcome message when a player joins")
            .define("welcomeMessage", true);

    public static final ForgeConfigSpec.IntValue EXTRA_DROP_CHANCE = BUILDER
            .comment("Chance in percent that a zombie drops an extra emerald")
            .defineInRange("extraDropChance", 10, 0, 100);

    static {
        BUILDER.push("dash");   // beginnt einen Abschnitt [dash] in der Datei
    }

    public static final ForgeConfigSpec.DoubleValue DASH_STRENGTH = BUILDER
            .comment("How far the dash key pushes the player")
            .defineInRange("strength", 1.5, 0.1, 5.0);

    static {
        BUILDER.pop();          // beendet den Abschnitt
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private MyModConfig() {}
}
```

| Methode | Typ | Beispiel |
|---|---|---|
| `define(name, standard)` | `BooleanValue` (bei `boolean`) / `ConfigValue<T>` | an/aus, Text |
| `defineInRange(name, standard, min, max)` | `IntValue`, `LongValue`, `DoubleValue` | Zahlen mit Grenzen |
| `defineEnum(name, standard)` | `EnumValue<E>` | Auswahl aus einer Liste |
| `defineList(name, standard, prüfung)` | `ConfigValue<List<…>>` | Liste (z. B. erlaubte Block-IDs) |
| `comment("…")` | – | Kommentar über dem Eintrag |
| `push("abschnitt")` / `pop()` | – | Abschnitte `[abschnitt]` |

Die Felder werden **der Reihe nach** erzeugt (von oben nach unten). Deshalb stehen `push`/`pop` in `static { }`-Blöcken genau zwischen den Feldern.

## 2. Anmelden

Im Konstruktor von `MyMod`:

```java
context.registerConfig(ModConfig.Type.COMMON, MyModConfig.SPEC);
```

(`ModConfig` aus `net.minecraftforge.fml.config` – deshalb heißt die eigene Klasse `MyModConfig`, sonst gibt es einen Namenskonflikt.)

| Typ | Datei | Gilt für | Typisch für |
|---|---|---|---|
| `COMMON` | `config/mymod-common.toml` | Client und Server, je eigene Datei | allgemeine Einstellungen |
| `CLIENT` | `config/mymod-client.toml` | nur Client | Grafik, HUD, Sounds |
| `SERVER` | `<welt>/serverconfig/mymod-server.toml` | pro Welt, wird an Spieler geschickt | Spielregeln, die auf allen Clients gleich sein müssen |

## 3. Die erzeugte Datei

So sieht `config/mymod-common.toml` nach dem ersten Start aus:

```toml
#Show a welcome message when a player joins
welcomeMessage = true
#Chance in percent that a zombie drops an extra emerald
#Range: 0 ~ 100
extraDropChance = 10

[dash]
	#How far the dash key pushes the player
	#Range: 0.1 ~ 5.0
	strength = 1.5
```

- Ungültige Werte (z. B. 500 bei Bereich 0–100) setzt Forge beim Laden auf den Standardwert zurück und schreibt eine Warnung ins Log.
- Beim allerersten Start erscheinen im Log Warnungen wie *„Incorrect key welcomeMessage was corrected from null to its default“* – das ist normal (die Datei war noch leer).
- Forge lädt die Datei neu, wenn sie während des Spiels geändert und gespeichert wird.

## 4. Werte benutzen

```java
if (MyModConfig.WELCOME_MESSAGE.get()) { ... }
int chance = MyModConfig.EXTRA_DROP_CHANCE.get();
double strength = MyModConfig.DASH_STRENGTH.get();
```

- **Immer mit `.get()` in dem Moment lesen, in dem du den Wert brauchst** (in Events, Befehlen, Ticks) – nicht in einem `static final`-Feld zwischenspeichern. Die Datei wird erst **nach** dem Laden der Klassen gelesen; zu frühes `.get()` wirft einen Fehler. Außerdem würden Änderungen sonst nicht ankommen.
- Nicht für Dinge, die bei der **Registrierung** feststehen müssen (z. B. Haltbarkeit eines Items) – dann ist die Datei noch nicht geladen.

Beispiele in diesem Leitfaden: Begrüßung und Drop-Chance ([15](15-Spiel-Events.md)), Dash-Stärke ([18](18-Tasten-und-Netzwerk.md)), Anzeige per Befehl ([13](13-Befehle.md)).

## Checkliste

- [ ] Klasse mit `ForgeConfigSpec.Builder`, Kommentaren und Grenzen
- [ ] `SPEC = BUILDER.build()` als **letztes** Feld
- [ ] `context.registerConfig(...)` im Mod-Konstruktor
- [ ] Werte nur mit `.get()` zur Laufzeit lesen
- [ ] Datei nach dem ersten Start ansehen

## Zum Ausprobieren

- Eine Liste von Block-IDs, die durch das Spawner-Schutz-Event geschützt sind (`defineList`, im Event mit `BuiltInRegistries.BLOCK.getKey(state.getBlock())` vergleichen).
- Eine CLIENT-Option, die deine eigene Bossbar-Textur ([10](10-Boss-Funktionen.md)) ein- und ausschaltet.
