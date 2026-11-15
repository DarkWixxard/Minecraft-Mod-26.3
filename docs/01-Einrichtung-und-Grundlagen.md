# 01 – Einrichtung und Grundlagen

## 1. Was du brauchst

| Programm | Warum | Wo |
|---|---|---|
| **JDK 25** | Minecraft 26.x läuft nur mit Java 25. Mit Java 21 oder älter klappt nichts. | In IntelliJ: *File → Project Structure → Project → SDK → Download JDK…* (z. B. Eclipse Temurin 25) oder [adoptium.net](https://adoptium.net/) |
| **IntelliJ IDEA** | Entwicklungsumgebung. Für Java 25 und Gradle 9 brauchst du etwa Version **2025.2 oder neuer**. | *Help → Check for Updates* |
| **Git / GitHub** | Versionsverwaltung, dein Repo `darkwixxard/minecraft-mod-26.3` | – |
| **Blockbench** | Modelle für Items, Rüstung und Mobs | [blockbench.net](https://www.blockbench.net/) |

## 2. Projekt in IntelliJ öffnen

1. **File → Open** und den Ordner wählen, in dem `build.gradle` **direkt** liegt (bei dir `S:\Mods\26.3\Minecraft-Mod-26.3`), nicht einen Ordner darüber.
2. Bei der Frage nach Vertrauen: **Trust Project**.
3. Erscheint unten rechts *„Gradle build scripts found – Load Gradle Project“*, auf **Load** klicken.
4. Rechts muss danach das **Gradle-Fenster** (Elefanten-Symbol) mit dem Projekt `rubymod` erscheinen.
5. **Gradle auf Java 25 stellen** (häufigster Fehler!):
   *File → Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JVM* → **Project SDK** bzw. JDK 25.
6. Im Gradle-Fenster auf **Reload All Gradle Projects** klicken. Der erste Durchlauf dauert 5–15 Minuten, weil Minecraft und Forge heruntergeladen und vorbereitet werden.

## 3. Bauen und Starten

### Über das Terminal

In IntelliJ unten das **Terminal** öffnen (es muss im Projektordner stehen):

```bash
# Windows (PowerShell): das .\ ist wichtig!
.\gradlew.bat build
.\gradlew.bat runClient

# macOS / Linux
./gradlew build
./gradlew runClient
```

> **Warum `.\` vor `gradlew.bat`?** PowerShell führt aus Sicherheitsgründen keine Programme aus dem aktuellen Ordner aus, wenn man nicht ausdrücklich `.\` davorschreibt. Ohne kommt: *„Die Benennung "gradlew.bat" wurde nicht als Name eines Cmdlet … erkannt“*.

| Befehl | Was passiert |
|---|---|
| `build` | Baut die fertige Mod-Datei `build/libs/rubymod-1.0.0.jar` |
| `runClient` | Startet Minecraft mit deiner Mod |
| `runServer` | Startet einen Server (vorher in `run/eula.txt` `eula=true` setzen) |
| `clean build` | Löscht alles Gebaute und baut neu – hilft bei komischen Fehlern |

### Über eine Run-Configuration (mit Debugger)

Oben rechts **Edit Configurations… → + → Gradle**:

| Feld | Eintrag |
|---|---|
| Name | `Minecraft Client` (frei wählbar) |
| Run / *Tasks and arguments* | `runClient` |
| Gradle project | dein Projektordner bzw. `rubymod` |
| Environment variables / VM options | leer lassen |

Danach kannst du mit **▶** normal starten oder mit **🐞** im **Debug-Modus**. Im Debug-Modus setzt du mit einem Klick links neben eine Zeilennummer einen **Breakpoint**. Minecraft hält dort an, und du siehst alle Variablen mit ihren aktuellen Werten. Probier es z. B. bei `LOGGER.info("Ruby Mod loaded!");` in `RubyMod.java`.

> **Starte nicht über den grünen Pfeil neben `build.gradle`.** Dann startet IntelliJ Gradle „von außen“ mit der Option `-b`, und die gibt es in Gradle 9 nicht mehr: *„Unknown command-line option '-b'“*. Nimm immer das Gradle-Fenster, das Terminal oder eine eigene Run-Configuration.

### Mod im echten Minecraft spielen

`build/libs/rubymod-1.0.0.jar` und die **GeckoLib-Jar für Forge 26.3** in den `mods`-Ordner einer Forge-26.3-Installation legen.

## 4. Die Build-Dateien

### `build.gradle` (die wichtigen Teile)

```groovy
plugins {
    id 'net.minecraftforge.gradle' version '[7.0.17,8)'   // ForgeGradle 7
}

java.toolchain.languageVersion = JavaLanguageVersion.of(25)  // Java 25

minecraft {
    runs {
        configureEach {
            workingDir = layout.projectDirectory.dir('run')   // hier landen Welten, Logs, Configs
        }
        register('client')
        register('server') { args '--nogui' }
    }
}

repositories {
    // ...
    maven {
        name = 'GeckoLib'
        url = 'https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/'
    }
}

dependencies {
    implementation minecraft.dependency('net.minecraftforge:forge:26.3-66.1.0')
    annotationProcessor 'net.minecraftforge:eventbus-validator:7.0.6'
    implementation 'com.geckolib:geckolib-forge-26.3:5.5.7'
}
```

- `repositories` sagt Gradle, **wo** es Bibliotheken herunterladen darf.
- `dependencies` sagt, **welche** Bibliotheken das Projekt braucht.
- Für GeckoLib braucht es beides: das Cloudsmith-Repository **und** die Abhängigkeit.

### `META-INF/mods.toml`

Beschreibt die Mod für Forge: ID, Name, Version und **Abhängigkeiten**. GeckoLib ist dort als Pflicht eingetragen, damit Forge eine klare Meldung zeigt, wenn GeckoLib fehlt:

```toml
[[dependencies.rubymod]]
modId="geckolib"
mandatory=true
versionRange="[5.5.7,)"
ordering="AFTER"
side="BOTH"
```

Die `modId` in `mods.toml` muss exakt dem Wert von `RubyMod.MODID` entsprechen (`"rubymod"`).

### `pack.mcmeta`

```json
{ "pack": { "description": "rubymod resources", "min_format": 121, "max_format": 121 } }
```

Seit 1.21.9 gibt es `min_format`/`max_format` statt `pack_format`. 121 ist das Datenpaket-Format von 26.3.

## 5. Der Einstiegspunkt: `RubyMod.java`

```java
@Mod(RubyMod.MODID)
public final class RubyMod {
    public static final String MODID = "rubymod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RubyMod(FMLJavaModLoadingContext context) {
        var modBusGroup = context.getModBusGroup();

        ModBlocks.BLOCKS.register(modBusGroup);
        ModItems.ITEMS.register(modBusGroup);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modBusGroup);
        ModCreativeTabs.registerListeners();

        ModEntities.ENTITY_TYPES.register(modBusGroup);
        EntityAttributeCreationEvent.BUS.addListener(RubyMod::registerAttributes);

        FMLCommonSetupEvent.getBus(modBusGroup).addListener(this::commonSetup);
    }
    // ...
}
```

- `@Mod(...)` markiert die Klasse als Mod. Forge erzeugt beim Start ein Objekt davon und ruft dabei den Konstruktor auf.
- Jedes `DeferredRegister` (Blöcke, Items, Tabs, Entities) muss hier mit `.register(modBusGroup)` **angemeldet** werden. Vergisst du das, existieren deine Items einfach nicht – ohne Fehlermeldung.

### Events in Forge 26.3 (EventBus 7)

Forge benachrichtigt Mods über **Events** („jetzt werden Attribute registriert“, „jetzt wird ein Kreativ-Tab gefüllt“ …). In 26.3 hat **jedes Event seinen eigenen `BUS`**:

```java
// Variante 1: direkt anmelden
EntityAttributeCreationEvent.BUS.addListener(RubyMod::registerAttributes);
BuildCreativeModeTabContentsEvent.BUS.addListener(ModCreativeTabs::addToVanillaTabs);

// Variante 2: Lebenszyklus-Events der Mod
FMLCommonSetupEvent.getBus(modBusGroup).addListener(this::commonSetup);

// Variante 3: ganze Klasse automatisch anmelden lassen
@Mod.EventBusSubscriber(modid = RubyMod.MODID, value = Dist.CLIENT)
public final class ModClientEvents {
    @SubscribeEvent   // aus net.minecraftforge.eventbus.api.listener !
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) { ... }
}
```

**Wichtig:**
- `@SubscribeEvent` muss aus `net.minecraftforge.eventbus.api.listener` kommen. IntelliJ schlägt manchmal `com.google.common.eventbus.Subscribe` vor – das ist eine ganz andere Bibliothek und tut nichts.
- Bei Events, die man **abbrechen** kann, gibt die Methode `boolean` zurück: `true` = abbrechen.
- `value = Dist.CLIENT` bedeutet: Diese Klasse wird nur beim Spieler geladen, nie auf einem Server. Alles mit Rendering, Tastatur oder Bildschirm gehört dorthin.

## 6. Pakete (Ordner) sauber organisieren

Java schreibt nicht vor, in welchem Paket eine Klasse liegt. Bewährt hat sich eine Aufteilung nach Themen (wie bei Kaupenjoe):

- `item/ModItems.java` registriert **alle** Items, auch einfache wie den Rubin.
- `item/custom/` enthält nur Items mit **eigenem Verhalten** (eigene Klasse, z. B. `NoxiumBowItem extends BowItem`).
- Gleiches Prinzip für `block/custom/` und `entity/custom/`.
- `entity/client/` enthält alles, was nur beim Spieler läuft (Renderer, Bildschirm-Anzeigen).

### Dateien richtig verschieben

**Nie im Windows-Explorer verschieben!** Dann stimmen die `package`- und `import`-Zeilen nicht mehr und nichts kompiliert mehr. In IntelliJ:

1. Rechtsklick auf das Paket → **New → Package** (z. B. `item`).
2. Die Klasse im Projektbaum per Drag & Drop in das neue Paket ziehen.
3. Im Dialog **Refactor** klicken. IntelliJ passt alle `package`- und `import`-Zeilen automatisch an.

Zum **Umbenennen** genauso: Rechtsklick → **Refactor → Rename** (Shift+F6).

## 7. Unterschiede zu Forge 1.16.5

Die Grundidee ist gleich geblieben. Code aus 1.16.5-Tutorials kompiliert in 26.3 aber praktisch **nie** ohne Anpassungen.

### Ordner

| Was | 1.16.5 | 26.3 |
|---|---|---|
| Rezepte | `data/<mod>/recipes/` | `data/<mod>/recipe/` (Einzahl!) |
| Loot-Tables | `loot_tables/` | `loot_table/` |
| Tags | `tags/blocks/`, `tags/items/` | `tags/block/`, `tags/item/` |
| Item-Modelle | nur `models/item/x.json` | zusätzlich `assets/<mod>/items/x.json` (seit 1.21.4) |
| Abbau-Werkzeug | im Java-Code | über Tags (`mineable/pickaxe.json`, `needs_iron_tool.json`) |
| `pack.mcmeta` | `pack_format: 6` | `min_format`/`max_format: 121` |

Falsch benannte Ordner werden **ohne Fehlermeldung** ignoriert – das Rezept fehlt dann einfach.

### Build

| | 1.16.5 | 26.3 |
|---|---|---|
| Java | 8 | 25 |
| Gradle / ForgeGradle | 6–7 / FG 4–5 | 9 / FG 7 |
| Mappings | MCP-Namen (`func_123`, `field_456`) | keine nötig: Minecraft ist seit 26.1 **nicht mehr verschleiert**, du siehst die echten Mojang-Namen |

### Code

```java
// 1.16.5
BLOCKS.register("ruby_block", () -> new Block(AbstractBlock.Properties
    .of(Material.METAL).harvestTool(ToolType.PICKAXE).harvestLevel(2).strength(5f, 6f)));

// 26.3
BLOCKS.register("ruby_block", () -> new Block(BlockBehaviour.Properties.of()
    .setId(BLOCKS.key("ruby_block"))   // Pflicht seit 1.21.2
    .mapColor(MapColor.COLOR_RED)      // "Material" gibt es nicht mehr
    .requiresCorrectToolForDrops()     // Werkzeug kommt über Tags
    .strength(5f, 6f)));
```

Weitere typische Umbenennungen:

| 1.16.5 | 26.3 |
|---|---|
| `ResourceLocation` | `Identifier` |
| `ItemGroup` / `.tab(...)` | `CreativeModeTab`, eigener registrierter Tab oder Event |
| `Food.Builder().saturationMod(...)` | `FoodProperties.Builder().saturationModifier(...)` |
| `IItemTier` / `IArmorMaterial` (Enums) | `ToolMaterial` / `ArmorMaterial` (Records) |
| `SwordItem`, `PickaxeItem` … | normales `Item` mit `.sword(...)`, `.pickaxe(...)` in den Properties |
| `ArmorItem` | normales `Item` mit `.humanoidArmor(material, type)` |
| `CompoundNBT` beim Speichern | `ValueInput` / `ValueOutput` |
| `hurt(...)` | `hurtServer(ServerLevel, DamageSource, float)` |
