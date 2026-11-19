# 01 – Einrichtung und Grundlagen

## 1. Was du brauchst

| Programm | Warum | Wo |
|---|---|---|
| **JDK 25** | Minecraft 26.x läuft nur mit Java 25. Mit älterem Java klappt nichts. | In IntelliJ: *File → Project Structure → Project → SDK → Download JDK…* (z. B. Eclipse Temurin 25) oder [adoptium.net](https://adoptium.net/) |
| **IntelliJ IDEA** (Community reicht) | Entwicklungsumgebung. Für Java 25 und Gradle 9 etwa Version **2025.2 oder neuer**. | *Help → Check for Updates* |
| **Forge MDK** | Vorlage-Projekt („Mod Development Kit“) | [files.minecraftforge.net](https://files.minecraftforge.net/) → Version wählen → *Mdk* |
| **Git** (optional) | Versionsverwaltung, Backup auf GitHub | – |
| **Blockbench** | Modelle für Items, Rüstung, Mobs | [blockbench.net](https://www.blockbench.net/) |

## 2. Neues Projekt aus dem MDK

1. MDK-Zip herunterladen und in einen eigenen Ordner entpacken.
2. In `gradle.properties` bzw. `build.gradle` die Platzhalter ändern: Mod-ID, Gruppe (`com.example.mymod`), Name.
3. Das Beispiel-Paket in `src/main/java` umbenennen (siehe [Abschnitt 7](#7-pakete-ordner-sauber-organisieren)) und die Hauptklasse anpassen.
4. In `src/main/resources/META-INF/mods.toml` dieselbe Mod-ID eintragen.

> Die Mod-ID muss **überall gleich** sein: `MODID`-Konstante, `mods.toml`, Ordner `assets/<modid>/` und `data/<modid>/`.

## 3. Projekt in IntelliJ öffnen

1. **File → Open** und den Ordner wählen, in dem `build.gradle` **direkt** liegt – nicht einen Ordner darüber.
2. Bei der Frage nach Vertrauen: **Trust Project**.
3. Erscheint *„Gradle build scripts found – Load Gradle Project“*, auf **Load** klicken.
4. Rechts muss das **Gradle-Fenster** (Elefanten-Symbol) erscheinen.
5. **Gradle auf Java 25 stellen** (häufigster Fehler!):
   *File → Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JVM* → **Project SDK** bzw. JDK 25.
6. Im Gradle-Fenster **Reload All Gradle Projects**. Der erste Durchlauf dauert 5–15 Minuten (Minecraft und Forge werden heruntergeladen und vorbereitet).

## 4. Bauen und Starten

### Über das Terminal

```bash
# Windows (PowerShell): das .\ ist wichtig!
.\gradlew.bat build
.\gradlew.bat runClient

# macOS / Linux
./gradlew build
./gradlew runClient
```

> **Warum `.\`?** PowerShell führt Programme aus dem aktuellen Ordner nur aus, wenn man ausdrücklich `.\` davorschreibt. Ohne kommt: *„Die Benennung "gradlew.bat" wurde nicht … erkannt“*.

| Befehl | Was passiert |
|---|---|
| `build` | Baut die fertige Mod-Datei nach `build/libs/<modid>-<version>.jar` |
| `runClient` | Startet Minecraft mit deiner Mod |
| `runServer` | Startet einen Server (vorher in `run/eula.txt` `eula=true` setzen) |
| `clean build` | Alles Gebaute löschen und neu bauen – hilft bei komischen Fehlern |

### Über eine Run-Configuration (mit Debugger)

Oben rechts **Edit Configurations… → + → Gradle**:

| Feld | Eintrag |
|---|---|
| Name | z. B. `Minecraft Client` |
| *Tasks and arguments* | `runClient` |
| Gradle project | dein Projekt |
| Environment variables / VM options | leer lassen |

- **▶** startet normal.
- **🐞** startet im **Debug-Modus**. Ein Klick links neben eine Zeilennummer setzt einen **Breakpoint**: Minecraft hält dort an, und du siehst alle Variablen mit ihren Werten. Das ist das beste Werkzeug, um Fehler zu verstehen.

> **Nicht über den grünen Pfeil neben `build.gradle` starten.** Dann ruft IntelliJ Gradle mit der Option `-b` auf, die es in Gradle 9 nicht mehr gibt: *„Unknown command-line option '-b'“*.

### Mod im echten Minecraft spielen

Die `.jar` aus `build/libs/` (und alle Bibliotheken, die die Mod braucht, z. B. GeckoLib) in den `mods`-Ordner einer passenden Forge-Installation legen.

### Was wann neu geladen wird

| Änderung an … | So wird sie aktiv |
|---|---|
| Texturen, Modelle, Sprachdateien | im Spiel **F3 + T** |
| Rezepte, Tags, Loot-Tables | `/reload` |
| Weltgenerierung (`worldgen`, Biome Modifier) | Spiel neu starten; wirkt nur in **neu erzeugten** Chunks |
| Java-Code | Spiel neu starten |

## 5. Die Build-Dateien

### `build.gradle` (wichtige Teile)

```groovy
plugins {
    id 'net.minecraftforge.gradle' version '[7.0.17,8)'   // ForgeGradle 7
}

java.toolchain.languageVersion = JavaLanguageVersion.of(25)  // Java 25

minecraft {
    runs {
        configureEach {
            workingDir = layout.projectDirectory.dir('run')   // Welten, Logs, Configs landen hier
        }
        register('client')
        register('server') { args '--nogui' }
    }
}

repositories {
    // ... (vom MDK)
    maven {                       // nur nötig, wenn du GeckoLib benutzt
        name = 'GeckoLib'
        url = 'https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/'
    }
}

dependencies {
    implementation minecraft.dependency('net.minecraftforge:forge:26.3-66.1.0')
    annotationProcessor 'net.minecraftforge:eventbus-validator:7.0.6'
    implementation 'com.geckolib:geckolib-forge-26.3:5.5.7'      // optional
}
```

- `repositories` = **wo** Gradle Bibliotheken herunterladen darf.
- `dependencies` = **welche** Bibliotheken das Projekt braucht.
- Eine neue Bibliothek braucht meist **beides**. Nach jeder Änderung: *Reload All Gradle Projects*.

### `META-INF/mods.toml`

Beschreibt die Mod für Forge (ID, Name, Version, Abhängigkeiten). Benutzt deine Mod eine Bibliothek, trag sie ein – dann zeigt Forge eine klare Meldung, wenn sie fehlt:

```toml
[[dependencies.mymod]]
modId="geckolib"
mandatory=true
versionRange="[5.5.7,)"
ordering="AFTER"
side="BOTH"
```

### `pack.mcmeta`

```json
{ "pack": { "description": "mymod resources", "min_format": 121, "max_format": 121 } }
```

Seit 1.21.9 gibt es `min_format`/`max_format` statt `pack_format`. Die Zahl hängt von der Minecraft-Version ab (121 = 26.3). Nachschauen: im Minecraft-Wiki unter „Pack format“.

## 6. Der Einstiegspunkt

```java
@Mod(MyMod.MODID)
public final class MyMod {
    public static final String MODID = "mymod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MyMod(FMLJavaModLoadingContext context) {
        var modBusGroup = context.getModBusGroup();

        ModBlocks.BLOCKS.register(modBusGroup);
        ModItems.ITEMS.register(modBusGroup);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modBusGroup);
        ModEntities.ENTITY_TYPES.register(modBusGroup);

        FMLCommonSetupEvent.getBus(modBusGroup).addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("My Mod loaded!");
    }
}
```

- `@Mod(...)` markiert die Hauptklasse. Forge erzeugt beim Start ein Objekt davon → der Konstruktor läuft.
- **Jedes `DeferredRegister` muss hier mit `.register(modBusGroup)` angemeldet werden.** Vergisst du das, existieren die Inhalte einfach nicht – ohne Fehlermeldung.
- `LOGGER.info(...)` schreibt ins Log. Gut zum Prüfen, ob Code überhaupt läuft.

## 7. Events

Forge benachrichtigt Mods über **Events** („jetzt werden Attribute registriert“, „jetzt wird ein Kreativ-Tab gefüllt“, „Spieler hat geklickt“ …). In Forge 26.x (EventBus 7) hat **jedes Event seinen eigenen `BUS`**.

```java
// Variante 1: direkt anmelden (z. B. im Konstruktor der Hauptklasse)
EntityAttributeCreationEvent.BUS.addListener(MyMod::registerAttributes);

// Variante 2: Lebenszyklus-Events der Mod
FMLCommonSetupEvent.getBus(modBusGroup).addListener(this::commonSetup);

// Variante 3: eine ganze Klasse automatisch anmelden lassen
@Mod.EventBusSubscriber(modid = MyMod.MODID, value = Dist.CLIENT)
public final class ModClientEvents {
    @SubscribeEvent   // aus net.minecraftforge.eventbus.api.listener !
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) { ... }
}
```

**Wichtig:**
- `@SubscribeEvent` muss aus **`net.minecraftforge.eventbus.api.listener`** kommen. IntelliJ schlägt manchmal `com.google.common.eventbus.Subscribe` vor – das ist eine andere Bibliothek und tut nichts.
- `@SubscribeEvent` wirkt nur in Klassen mit `@Mod.EventBusSubscriber`. Die Methoden müssen `static` sein.
- Bei **abbrechbaren** Events gibt die Methode `boolean` zurück: `true` = abbrechen.
- `value = Dist.CLIENT` = Klasse wird nur beim Spieler geladen. Alles mit Rendering, HUD oder Tastatur gehört dorthin.
- Was ein Handler genau ist und wann man welchen Weg nimmt: [22 – Handler](22-Handler.md).
- Welche Events es gibt: in IntelliJ im Paket `net.minecraftforge.event` (Server/beide) und `net.minecraftforge.client.event` (nur Client) stöbern.

## 8. Pakete (Ordner) sauber organisieren

Java schreibt nicht vor, in welchem Paket eine Klasse liegt. Bewährt (z. B. bei Kaupenjoe):

- `item/ModItems.java` registriert **alle** Items, auch einfache.
- `item/custom/` enthält nur Items mit **eigenem Verhalten** (eigene Klasse).
- Gleiches Prinzip für `block/custom/` und `entity/custom/`.
- `entity/client/` enthält alles, was nur beim Spieler läuft.

### Dateien richtig verschieben und umbenennen

**Nie im Windows-Explorer verschieben!** Dann stimmen die `package`- und `import`-Zeilen nicht mehr, und nichts kompiliert.

- **Verschieben:** In IntelliJ die Klasse im Projektbaum per Drag & Drop in das Ziel-Paket ziehen → **Refactor**.
- **Umbenennen:** Rechtsklick → **Refactor → Rename** (Shift + F6). IntelliJ passt alle Verweise an.

## 9. Code aus alten Tutorials (z. B. 1.16.5)

Die Grundidee ist gleich geblieben. Code aus alten Tutorials kompiliert in 26.x aber praktisch **nie** ohne Anpassungen. Übernimm die **Idee**, nicht den Code.

### Ordner

| Was | alt (1.16.5) | neu (26.x) |
|---|---|---|
| Rezepte | `data/<mod>/recipes/` | `data/<mod>/recipe/` (Einzahl!) |
| Loot-Tables | `loot_tables/` | `loot_table/` |
| Tags | `tags/blocks/`, `tags/items/` | `tags/block/`, `tags/item/` |
| Item-Modelle | nur `models/item/x.json` | zusätzlich `assets/<mod>/items/x.json` (seit 1.21.4) |
| Abbau-Werkzeug | im Java-Code | über Tags (`mineable/pickaxe.json`, `needs_iron_tool.json`) |

Falsch benannte Ordner werden **ohne Fehlermeldung** ignoriert.

### Build

| | alt | neu |
|---|---|---|
| Java | 8 | 25 |
| Gradle / ForgeGradle | 6–7 / FG 4–5 | 9 / FG 7 |
| Mappings | MCP-Namen (`func_123`) | keine nötig: Minecraft ist seit 26.1 nicht mehr verschleiert |

### Code

```java
// alt (1.16.5)
BLOCKS.register("sapphire_block", () -> new Block(AbstractBlock.Properties
    .of(Material.METAL).harvestTool(ToolType.PICKAXE).harvestLevel(2).strength(5f, 6f)));

// neu (26.x)
BLOCKS.register("sapphire_block", () -> new Block(BlockBehaviour.Properties.of()
    .setId(BLOCKS.key("sapphire_block"))   // Pflicht seit 1.21.2
    .mapColor(MapColor.COLOR_BLUE)         // "Material" gibt es nicht mehr
    .requiresCorrectToolForDrops()         // Werkzeug kommt über Tags
    .strength(5f, 6f)));
```

| alt | neu |
|---|---|
| `ResourceLocation` | `Identifier` |
| `ItemGroup` / `.tab(...)` | `CreativeModeTab` (eigener Tab oder Event) |
| `Food.Builder().saturationMod(...)` | `FoodProperties.Builder().saturationModifier(...)` |
| `IItemTier` / `IArmorMaterial` | `ToolMaterial` / `ArmorMaterial` |
| `SwordItem`, `PickaxeItem` … | normales `Item` mit `.sword(...)`, `.pickaxe(...)` |
| `ArmorItem` | normales `Item` mit `.humanoidArmor(material, type)` |
| `CompoundNBT` beim Speichern | `ValueInput` / `ValueOutput` |
| `hurt(...)` | `hurtServer(ServerLevel, DamageSource, float)` |
| `MobEffects.DAMAGE_BOOST`, `MOVEMENT_SPEED` | `MobEffects.STRENGTH`, `SPEED` |
| `EntityType.ZOMBIE` | `EntityTypes.ZOMBIE` |
| `level.random` | `level.getRandom()` |
| `entity.hurtMarked = true` | `entity.needsSync = true` |
| `.requires(source -> source.hasPermission(2))` | `.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))` |
| Brau-Rezepte in Java (`PotionBrewing`) | JSON-Rezepte `"type": "minecraft:brewing"` |
| Verzauberungen als Java-Klasse | JSON in `data/<modid>/enchantment/` |
| Spawn-Eier mit zwei Farben | eigene Textur, `Item.Properties.spawnEgg(type)` |

## Checkliste: neues Projekt

- [ ] JDK 25 installiert, Gradle JVM auf 25
- [ ] Mod-ID in `MODID`, `mods.toml`, `assets/<modid>`, `data/<modid>` gleich
- [ ] Paket umbenannt (Refactor, nicht Explorer)
- [ ] `runClient` startet, im Log steht deine `LOGGER.info`-Zeile
- [ ] Erst committen, wenn es startet – dann hast du immer einen funktionierenden Stand

## Zum Ausprobieren

- Setz einen Breakpoint in `commonSetup` und starte mit 🐞. Schau dir im Debugger die Variable `event` an.
- Öffne mit **Strg + N** die Klasse `Items` und schau, wie Mojang Diamant, Apfel und Elytra registriert.
