# 12 – Werkzeuge und Quellen

## 1. IntelliJ-Tastenkürzel, die beim Modding am meisten helfen

| Kürzel | Was |
|---|---|
| **Strg + N** | Klasse über ihren Namen öffnen – auch Minecraft-Klassen wie `BowItem`, `EnderDragon`, `WitherBoss` |
| **2× Shift** | Überall suchen, auch Dateien wie `elytra.json` |
| **Strg + Shift + F** | Text in allen Dateien suchen. Bereich auf **All Places** stellen, sonst nur dein eigener Code. |
| **Strg + Klick** auf einen Namen | Zur Definition springen |
| **Strg + O** | Methode der Elternklasse überschreiben (fügt die richtige Signatur ein) |
| **Alt + Enter** | Lösungsvorschläge für rot markierten Code |
| **Shift + F6** | Umbenennen überall (Refactor → Rename) |
| **Strg + Alt + L** | Code automatisch formatieren |

### Minecraft-Code lesen

Seit 26.1 ist Minecraft nicht mehr verschleiert. Unter **External Libraries → `Gradle: net.minecraftforge:forge:26.3-66.1.0`** liegt der komplette Code mit echten Namen.

Der beste Weg, etwas Neues zu lernen: **schauen, wie Vanilla es macht.**

| Du willst … | Schau in … |
|---|---|
| mehrteilige Hitboxen | `EnderDragon`, `EnderDragonPart` |
| eine Bossbar | `WitherBoss` |
| einen fliegenden Mob | `Bee`, `Parrot`, `Allay`, `Phantom` |
| einen Bogen | `BowItem`, `CrossbowItem` |
| Despawn-Regeln | `Mob.checkDespawn` |
| eine eigene Elytra | `Items.ELYTRA` (Komponente `DataComponents.GLIDER`), `LivingEntity.canGlide`, `WingsLayer`, `equipment/elytra.json` |

## 2. Blockbench

**[blockbench.net](https://www.blockbench.net/)** – kostenlos, auch im Browser. Damit macht man Modelle für Blöcke, Items, Rüstung und Mobs.

| Format | Wofür |
|---|---|
| **Java Block/Item** | Block- und Item-Modelle (`models/block`, `models/item`) |
| **Modded Entity** | Mob-Modelle als Java-Code (`LayerDefinition`) |
| **GeckoLib Animated Model** | Modelle mit Animationen für GeckoLib (Mobs, Items, Rüstung) |

- **`.bbmodel`** ist die **Projektdatei** von Blockbench (wie `.psd` bei Photoshop). Minecraft kann sie nicht lesen – man **exportiert** daraus.
- **GeckoLib-Plugin:** *File → Plugins… → „GeckoLib Models & Animations“* (aktuell 4.2.5, braucht Blockbench 5). Nach der Installation Blockbench neu starten.
- **Export für GeckoLib:** *File → Export → Export GeckoLib Model* (`.geo.json`) und *Animation → Export Animations* (`.animation.json`).

## 3. Modelle aus dem Internet

| Quelle | Was |
|---|---|
| **[Sketchfab](https://sketchfab.com/)** | Viele Modelle, oft als `.bbmodel`. Nicht jedes ist herunterladbar. Formate wie glTF/FBX/OBJ kann Minecraft nicht direkt benutzen – sie müssen in Blockbench importiert und meist stark vereinfacht werden. |
| **[Planet Minecraft](https://www.planetminecraft.com/)** | Modelle und Ressourcenpakete der Community |
| **[Modrinth](https://modrinth.com/) / [CurseForge](https://www.curseforge.com/minecraft)** | Open-Source-Mods zum Anschauen |
| **Vanilla-Modelle** | in IntelliJ, z. B. `ZombieModel`, `WolfModel` |

**Lizenz beachten!** „CC0“, „CC-BY“ (Namen nennen) oder „free to use“ sind meist okay. Steht nichts dabei: den Ersteller fragen oder nur zum Üben benutzen. Das Ulyssan-Modell hast du direkt vom Ersteller bekommen.

> **BBCode** (z. B. `[b]fett[/b]` in Foren) hat nichts mit Blockbench (`.bbmodel`) zu tun.

## 4. Webseiten zum Lernen

### Offizielle Dokumentation

| Seite | Was |
|---|---|
| [docs.minecraftforge.net](https://docs.minecraftforge.net/en/latest/) | Forge-Grundlagen: Registrierung, Events, Blöcke, Items, Datengenerierung. Deckt bisher bis 1.21.x ab, die Konzepte gelten fast alle auch für 26.3. |
| [github.com/MinecraftForge/MDKExamples](https://github.com/MinecraftForge/MDKExamples) | Offizielle Beispielprojekte (Mixins, Access Transformers, Jar-in-Jar) |
| [docs.neoforged.net](https://docs.neoforged.net/) | NeoForge (Abspaltung von Forge). Oft ausführlicher und aktueller, vieles lässt sich übertragen. Unterschied: NeoForge schreibt `DeferredHolder`, Forge `RegistryObject`. |
| [GeckoLib-Wiki](https://github.com/bernie-g/geckolib/wiki) | Alles zu GeckoLib: Controller, Trigger, Renderer |

### Tutorials

| Seite | Was |
|---|---|
| **Kaupenjoe** ([kaupenjoe.net](https://kaupenjoe.net/) + YouTube) | Bekannteste Tutorial-Reihe, Schritt für Schritt. **Auf die Versionsnummer achten.** |
| **McJty Modding Wiki** ([mcjty.eu](https://www.mcjty.eu/docs/intro)) | Schriftlich, gut für Fortgeschrittenes (Block-Entities, GUIs, Netzwerk) |

### Nachschlagen für JSON-Dateien

| Seite | Was |
|---|---|
| [minecraft.wiki](https://minecraft.wiki/) | Genaues Format jeder JSON-Datei (Rezepte, Loot-Tables, Tags, Modelle). Versions-Seiten wie „Java Edition 26.3“ zeigen Änderungen. |
| [misode.github.io](https://misode.github.io/) | Online-Generatoren für Rezepte, Loot-Tables, Weltgenerierung – für die gewählte Version |

### Java lernen (Deutsch)

| Seite | Was |
|---|---|
| [„Java ist auch eine Insel“](https://openbook.rheinwerk-verlag.de/javainsel/) | Kostenloses, umfassendes Java-Buch. Solide Grundlagen (Klassen, Vererbung, Lambdas, Generics) helfen mehr als jedes Tutorial. |

## 5. Java-Begriffe, die in dieser Mod vorkommen

| Begriff | Beispiel aus der Mod | Bedeutung |
|---|---|---|
| **Klasse / Objekt** | `UlyssanDragonEntity` / jeder einzelne Drache | Bauplan / ein gebautes Exemplar |
| **Konstruktor** | `public UlyssanDragonEntity(EntityType<…> type, Level level)` | heißt wie die Klasse, kein Rückgabetyp, läuft beim Erzeugen |
| **Vererbung** | `extends Monster`, `extends BowItem` | übernimmt alles von der Elternklasse |
| **Interface** | `implements GeoEntity` | Versprechen: „diese Methoden gibt es bei mir“ |
| **`@Override`** | `@Override public boolean hurtServer(...)` | „überschreibt eine Methode der Elternklasse“ – der Compiler prüft das |
| **`super.…`** | `super.hurtServer(level, source, amount)` | ruft die Version der Elternklasse auf |
| **`static final`** | `RawAnimation IDLE` | eine Konstante, die es nur einmal gibt |
| **Lambda** | `() -> new Block(...)`, `test -> PlayState.STOP` | kleine Funktion ohne Namen |
| **Methodenreferenz** | `UlyssanDragonEntity::new`, `RubyMod::registerAttributes` | Verweis auf eine vorhandene Methode |
| **Generics** | `RegistryObject<Item>`, `PartEntity<UlyssanDragonEntity>` | Typ in spitzen Klammern: „wovon“ |
| **Anonyme Klasse** | `new WaterAvoidingRandomStrollGoal(this, 0.8) { … }` | Unterklasse ohne Namen, nur für diese Stelle |
| **Innere Klasse** | `private class DragonFlyAroundGoal` | Klasse in einer Klasse, kann auf deren Felder zugreifen |
| **record** | `record SetEffect(…)`, `record Bar(…)` | kurze Klasse, die nur Daten hält |
| **Pattern Matching** | `if (x instanceof Player player)` | Typ prüfen und gleich einen Namen vergeben |
| **Ternärer Operator** | `flying ? a : b` | kurzes if/else |
| **switch-Ausdruck** | `switch (index) { case 1 -> …; default -> …; }` | Wert je nach Fall |
| **Optional** | `getBoundingBox().clip(eye, end)` | „vielleicht ein Wert, vielleicht keiner“ |
