# 02 – Blöcke, Items und Werkzeuge

## 1. Das Prinzip der Registrierung

Minecraft hat für alles eine **Registry** (ein großes Verzeichnis): Blöcke, Items, Entities, Sounds … Jeder Eintrag hat eine eindeutige ID wie `mymod:sapphire`.

Forge stellt dafür `DeferredRegister` bereit. „Deferred“ = *verzögert*: Du sagst nur, **was** registriert werden soll, und Forge erledigt es zum richtigen Zeitpunkt.

```java
public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, MyMod.MODID);

    public static final RegistryObject<Block> SAPPHIRE_BLOCK = BLOCKS.register("sapphire_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .setId(BLOCKS.key("sapphire_block"))
                    .mapColor(MapColor.COLOR_BLUE)
                    .requiresCorrectToolForDrops()
                    .strength(5.0F, 6.0F)
                    .sound(SoundType.METAL)));

    private ModBlocks() {}   // niemand soll ein Objekt dieser Klasse erzeugen
}
```

- **`RegistryObject<Block>`** ist ein **Platzhalter**. Der echte Block existiert erst nach der Registrierung. Mit `.get()` kommst du an ihn (`ModBlocks.SAPPHIRE_BLOCK.get()`). Rufst du `.get()` zu früh auf (z. B. in einem `static`-Feld), stürzt das Spiel ab.
- **`() -> new Block(...)`** ist ein **Lambda** – eine kleine Funktion, die Forge später aufruft.
- **`.setId(BLOCKS.key("sapphire_block"))`** ist seit 1.21.2 **Pflicht**. Der Name muss exakt dem in `register(...)` entsprechen. Fehlt es, stürzt das Spiel beim Start ab.
- In `MyMod` nicht vergessen: `ModBlocks.BLOCKS.register(modBusGroup);`

## 2. Blöcke

| Eigenschaft | Bedeutung |
|---|---|
| `mapColor(...)` | Farbe auf einer Minecraft-Karte |
| `requiresCorrectToolForDrops()` | Droppt nur mit dem richtigen Werkzeug. **Welches**, steht in Tags (siehe [03](03-Erze-und-Weltgenerierung.md)). |
| `strength(härte, explosionsresistenz)` | Härte = Abbau-Dauer (Erde 0.5, Stein 1.5, Eisenblock 5, Obsidian 50). Explosionsresistenz = wie gut er Explosionen aushält. |
| `strength(x)` | beides gleich |
| `sound(SoundType.METAL)` | Geräusche (STONE, WOOD, GLASS, DEEPSLATE, AMETHYST …) |
| `lightLevel(state -> 15)` | leuchtet (0–15) |
| `noOcclusion()` | durchsichtige Teile (Glas, Blätter) |
| `instrument(NoteBlockInstrument.BASEDRUM)` | Klang eines Notenblocks auf diesem Block |

Tipp: Mit **Strg + N** → `Blocks` siehst du, welche Werte Mojang für jeden Vanilla-Block benutzt. Einfach abschauen.

### Erze, die Erfahrung geben

```java
public static final RegistryObject<Block> SAPPHIRE_ORE = BLOCKS.register("sapphire_ore",
        () -> new DropExperienceBlock(UniformInt.of(3, 7), BlockBehaviour.Properties.of()
                .setId(BLOCKS.key("sapphire_ore"))
                .mapColor(MapColor.STONE)
                .instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops()
                .strength(3.0F, 3.0F)));

public static final RegistryObject<Block> DEEPSLATE_SAPPHIRE_ORE = BLOCKS.register("deepslate_sapphire_ore",
        () -> new DropExperienceBlock(UniformInt.of(3, 7), BlockBehaviour.Properties.of()
                .setId(BLOCKS.key("deepslate_sapphire_ore"))
                .mapColor(MapColor.DEEPSLATE)
                .instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops()
                .strength(4.5F, 3.0F)
                .sound(SoundType.DEEPSLATE)));
```

`DropExperienceBlock` ist die Vanilla-Klasse für Erze. `UniformInt.of(3, 7)` = beim Abbauen ohne Behutsamkeit 3–7 Erfahrungspunkte (wie Diamanterz).

### Was ein Block außer dem Java-Code braucht

| Datei | Wofür | ohne sie |
|---|---|---|
| `assets/mymod/blockstates/sapphire_block.json` | welches Modell für welchen Zustand | lila-schwarzer Würfel |
| `assets/mymod/models/block/sapphire_block.json` | das Modell | lila-schwarz |
| `assets/mymod/textures/block/sapphire_block.png` | Textur, 16×16 | lila-schwarz |
| `assets/mymod/items/sapphire_block.json` | Aussehen im Inventar | Item lila-schwarz |
| `data/mymod/loot_table/blocks/sapphire_block.json` | was beim Abbauen droppt | **droppt nichts** |
| `data/minecraft/tags/block/mineable/pickaxe.json` | richtiges Werkzeug | Spitzhacke ist langsam wie die Hand |
| `lang/*.json` | `"block.mymod.sapphire_block": "Saphirblock"` | roher Schlüssel als Name |

**blockstates** (ein Zustand):
```json
{ "variants": { "": { "model": "mymod:block/sapphire_block" } } }
```

**models/block** (gleiche Textur auf allen Seiten):
```json
{ "parent": "minecraft:block/cube_all", "textures": { "all": "mymod:block/sapphire_block" } }
```

Andere Parents: `cube_column` (Seiten/oben verschieden, z. B. Stämme), `cube_bottom_top`, `cross` (Pflanzen).

## 3. Items

Mit kleinen **Hilfsmethoden** braucht jedes Item nur eine Zeile:

```java
public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MyMod.MODID);

    public static final RegistryObject<Item> SAPPHIRE = item("sapphire", p -> p);
    public static final RegistryObject<Item> SAPPHIRE_BLOCK = blockItem("sapphire_block", ModBlocks.SAPPHIRE_BLOCK);

    private static RegistryObject<Item> item(String name, UnaryOperator<Item.Properties> properties) {
        return ITEMS.register(name, () -> new Item(properties.apply(new Item.Properties().setId(ITEMS.key(name)))));
    }

    private static RegistryObject<Item> blockItem(String name, Supplier<Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()
                .setId(ITEMS.key(name))
                .useBlockDescriptionPrefix()));
    }
}
```

- `UnaryOperator<Item.Properties>` = eine Funktion, die Properties bekommt und veränderte zurückgibt. So schreibt man beim Aufruf `p -> p` (nichts ändern) oder `p -> p.stacksTo(16).rarity(Rarity.RARE)`.
- **Jeder Block braucht zusätzlich ein `BlockItem`**, sonst kann man ihn nicht im Inventar halten. `useBlockDescriptionPrefix()` = das Item benutzt den Block-Namen (`block.mymod.…`).

Nützliche Item-Eigenschaften:

| Eigenschaft | Wirkung |
|---|---|
| `stacksTo(16)` | maximale Stapelgröße |
| `durability(500)` | Haltbarkeit (nicht stapelbar) |
| `rarity(Rarity.UNCOMMON)` | Farbe des Namens (gelb/aqua/lila) |
| `fireResistant()` | verbrennt nicht in Lava (wie Netherite) |
| `food(...)` | essbar |

### Was ein Item außer dem Java-Code braucht

| Datei | Inhalt |
|---|---|
| `assets/mymod/items/sapphire.json` | **seit 1.21.4:** verweist auf das Modell |
| `assets/mymod/models/item/sapphire.json` | das Modell |
| `assets/mymod/textures/item/sapphire.png` | Textur (16×16 oder 32×32) |
| `lang/*.json` | `"item.mymod.sapphire": "Saphir"` |

```json
// assets/mymod/items/sapphire.json
{ "model": { "type": "minecraft:model", "model": "mymod:item/sapphire" } }
```

```json
// assets/mymod/models/item/sapphire.json – flaches Item
{ "parent": "minecraft:item/generated", "textures": { "layer0": "mymod:item/sapphire" } }

// Werkzeug (wird in der Hand schräg gehalten)
{ "parent": "minecraft:item/handheld", "textures": { "layer0": "mymod:item/sapphire_sword" } }

// Block-Item (zeigt den 3D-Block)
{ "parent": "mymod:block/sapphire_block" }
```

> **Stolperfallen:**
> - In Modell- und Textur-Pfaden **keine Dateiendung**: `"mymod:item/sapphire"`, nicht `"mymod:item/sapphire.json"` oder `.png`.
> - Block sieht richtig aus, Item ist lila-schwarz → meist fehlt die Datei in `assets/mymod/items/`.

## 4. Werkzeuge

### Das Material

```java
public static final ToolMaterial SAPPHIRE_TOOL = new ToolMaterial(
    BlockTags.INCORRECT_FOR_DIAMOND_TOOL, // welche Blöcke das Werkzeug NICHT abbauen kann
    1200,  // Haltbarkeit
    8.5F,  // Abbaugeschwindigkeit
    2.5F,  // Bonus-Angriffsschaden
    15,    // Verzauberbarkeit
    ModTags.SAPPHIRE_TOOL_MATERIALS   // womit man es am Amboss repariert
);
```

Vergleichswerte aus Vanilla:

| Material | erster Wert (Stufe) | Haltbarkeit | Geschwindigkeit | Bonus-Schaden | Verzauberbarkeit |
|---|---|---|---|---|---|
| Holz | `INCORRECT_FOR_WOODEN_TOOL` | 59 | 2 | 0 | 15 |
| Stein | `INCORRECT_FOR_STONE_TOOL` | 131 | 4 | 1 | 5 |
| Eisen | `INCORRECT_FOR_IRON_TOOL` | 250 | 6 | 2 | 14 |
| Diamant | `INCORRECT_FOR_DIAMOND_TOOL` | 1561 | 8 | 3 | 10 |
| Gold | `INCORRECT_FOR_GOLD_TOOL` | 32 | 12 | 0 | 22 |
| Netherite | `INCORRECT_FOR_NETHERITE_TOOL` | 2031 | 9 | 4 | 15 |

Der **erste Wert** bestimmt die Abbau-Stufe. Mit `INCORRECT_FOR_DIAMOND_TOOL` darf das Werkzeug alles, was Diamant darf.

### Die Items

Seit 1.21.5 gibt es keine `SwordItem`-Klasse mehr. Ein Schwert ist ein normales `Item` mit Schwert-Eigenschaften:

```java
public static final RegistryObject<Item> SAPPHIRE_SWORD   = item("sapphire_sword",   p -> p.sword(ModMaterials.SAPPHIRE_TOOL, 3.0F, -2.4F));
public static final RegistryObject<Item> SAPPHIRE_PICKAXE = item("sapphire_pickaxe", p -> p.pickaxe(ModMaterials.SAPPHIRE_TOOL, 1.0F, -2.8F));
public static final RegistryObject<Item> SAPPHIRE_AXE     = item("sapphire_axe",     p -> p.axe(ModMaterials.SAPPHIRE_TOOL, 5.0F, -3.0F));
public static final RegistryObject<Item> SAPPHIRE_SHOVEL  = item("sapphire_shovel",  p -> p.shovel(ModMaterials.SAPPHIRE_TOOL, 1.5F, -3.0F));
public static final RegistryObject<Item> SAPPHIRE_HOE     = item("sapphire_hoe",     p -> p.hoe(ModMaterials.SAPPHIRE_TOOL, -2.5F, 0.0F));
```

Die zwei Zahlen sind **Angriffsschaden** und **Angriffsgeschwindigkeit** (die Werte oben sind die von Diamant-Werkzeugen). Echter Schaden = 1 (Faust) + Zahl + Bonus-Schaden des Materials. Beispiel: 1 + 3 + 2.5 = **6.5**.

### Eigene Tags (`tags/ModTags.java`)

```java
public final class ModTags {
    public static final TagKey<Item> SAPPHIRE_TOOL_MATERIALS = itemTag("sapphire_tool_materials");

    private static TagKey<Item> itemTag(String name) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MyMod.MODID, name));
    }
}
```

Ein **Tag** ist eine benannte Liste. Java kennt nur den Namen, der Inhalt steht in JSON:

```json
// data/mymod/tags/item/sapphire_tool_materials.json
{ "values": [ "mymod:sapphire" ] }
```

- In Tag-Listen und Rezepten heißt **`#` davor: das ist ein Tag**, ohne `#`: ein Item. `"#mymod:sapphire_tool_materials"` vs. `"mymod:sapphire"`.
- Steht in einem Tag ein Name, den es **nicht gibt** (Tippfehler, z. B. `minecraft:vines` statt `minecraft:vine`), lädt der **ganze Tag** nicht. Im Log steht dann `Couldn't load tag … missing following references`.

### In Vanilla-Tags eintragen

Damit Verzauberungen und Vanilla-Mechaniken greifen, gehören eigene Werkzeuge und Rüstungen in die Vanilla-Tags:

```
data/minecraft/tags/item/swords.json   pickaxes.json   axes.json   shovels.json   hoes.json
data/minecraft/tags/item/head_armor.json   chest_armor.json   leg_armor.json   foot_armor.json
data/minecraft/tags/item/enchantable/bow.json   (für Bögen)
```

```json
{ "replace": false, "values": [ "mymod:sapphire_sword" ] }
```

- **`"replace": false` ist wichtig!** Mit `true` ersetzt du die Vanilla-Liste, und Diamantschwerter wären keine Schwerter mehr.
- Der Dateiname muss **exakt** ein Vanilla-Tag sein. Einen Tag `minecraft:bow` gibt es z. B. nicht – nur `minecraft:enchantable/bow`. Welche Tags es gibt: in IntelliJ unter *External Libraries → … → data/minecraft/tags/* nachschauen oder im Minecraft-Wiki.

## 5. Essen

```java
public static final RegistryObject<Item> SAPPHIRE_APPLE = item("sapphire_apple", p -> p
        .rarity(Rarity.UNCOMMON)
        .food(new FoodProperties.Builder().nutrition(6).saturationModifier(1.2F).build()));
```

- `nutrition(6)` = 3 Hunger-Keulen.
- `saturationModifier(1.2F)` = wie lange man satt bleibt (Brot 0.6, Goldene Karotte 1.2).

Bei vielen Essens-Items lohnt eine eigene Klasse mit Konstanten:

```java
public final class ModFoodProperties {
    public static final FoodProperties SAPPHIRE_APPLE = new FoodProperties.Builder()
            .nutrition(6).saturationModifier(1.2F).build();
}
// in ModItems: item("sapphire_apple", p -> p.food(ModFoodProperties.SAPPHIRE_APPLE))
```

## 6. Kreativ-Tabs

### Eigener Tab

```java
public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MyMod.MODID);

    public static final RegistryObject<CreativeModeTab> MY_TAB = CREATIVE_MODE_TABS.register("my_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.mymod.my_tab"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)   // erscheint nach dem Kampf-Tab
                    .icon(() -> ModItems.SAPPHIRE.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        output.accept(ModItems.SAPPHIRE.get());
                        output.accept(ModItems.SAPPHIRE_SWORD.get());
                        output.accept(Items.DIAMOND);          // Vanilla-Items gehen auch
                    })
                    .build());
}
```

### In Vanilla-Tabs einsortieren

```java
public static void registerListeners() {
    BuildCreativeModeTabContentsEvent.BUS.addListener(ModCreativeTabs::addToVanillaTabs);
}

private static void addToVanillaTabs(BuildCreativeModeTabContentsEvent event) {
    var tab = event.getTabKey();
    if (tab == CreativeModeTabs.INGREDIENTS) {
        event.accept(ModItems.SAPPHIRE);
    } else if (tab == CreativeModeTabs.COMBAT) {
        event.accept(ModItems.SAPPHIRE_SWORD);
    }
}
```

`registerListeners()` im Konstruktor von `MyMod` aufrufen. Weitere Tabs: `BUILDING_BLOCKS`, `NATURAL_BLOCKS`, `TOOLS_AND_UTILITIES`, `FOOD_AND_DRINKS`, `SPAWN_EGGS` …

## 7. Rezepte (`data/mymod/recipe/`)

### Geformt (shaped)

```json
{
  "type": "minecraft:crafting_shaped",
  "category": "equipment",
  "pattern": [ "X", "X", "#" ],
  "key": {
    "X": "#mymod:sapphire_tool_materials",
    "#": "minecraft:stick"
  },
  "result": { "id": "mymod:sapphire_sword" }
}
```

- `pattern`: eine Zeile pro Reihe, Leerzeichen = leeres Feld. Kleinere Muster (2×2) können überall im 3×3-Raster liegen.
- `key`: Bedeutung jedes Zeichens. `#` vor dem Namen = Tag.
- Mehr Ergebnisse: `"result": { "id": "mymod:sapphire", "count": 9 }`.

### Formlos (shapeless)

```json
{
  "type": "minecraft:crafting_shapeless",
  "category": "misc",
  "ingredients": [ "mymod:sapphire_block" ],
  "result": { "id": "mymod:sapphire", "count": 9 }
}
```

### Schmelzen

```json
{
  "type": "minecraft:smelting",          // oder "minecraft:blasting" (Schmelzofen)
  "ingredient": "mymod:sapphire_ore",
  "result": { "id": "mymod:sapphire" },
  "experience": 1.0,
  "cookingtime": 200                      // Ticks (Schmelzofen üblich: 100)
}
```

Tipp: [misode.github.io](https://misode.github.io/) erzeugt Rezepte für die gewählte Version.

## 8. Loot-Tables (`data/mymod/loot_table/blocks/`)

Ohne Loot-Table droppt ein Block **gar nichts**. Für einen Block, der sich selbst droppt:

```json
{
  "type": "minecraft:block",
  "pools": [ {
    "rolls": 1,
    "entries": [ { "type": "minecraft:item", "name": "mymod:sapphire_block" } ],
    "conditions": [ { "condition": "minecraft:survives_explosion" } ]
  } ]
}
```

Erze mit Behutsamkeit und Glück: siehe [03](03-Erze-und-Weltgenerierung.md#4-was-das-erz-droppt).

## 9. Sprachdateien (`assets/mymod/lang/`)

```json
{
  "item.mymod.sapphire": "Saphir",
  "block.mymod.sapphire_block": "Saphirblock",
  "itemGroup.mymod.my_tab": "Meine Mod",
  "entity.mymod.wyvern": "Wyvern"
}
```

- `en_us.json` ist die Pflicht-Sprache. Fehlt ein Eintrag in `de_de.json`, nimmt Minecraft den englischen. Fehlt er überall, siehst du den rohen Schlüssel.
- **Komma** hinter jeder Zeile außer der letzten! Ein Kommafehler macht die ganze Datei kaputt.

## 10. Texturen

| Was | Größe | Ordner |
|---|---|---|
| Blöcke | 16×16 | `textures/block/` |
| Items | 16×16 (oder 32×32 für mehr Details) | `textures/item/` |
| Rüstung am Körper | 64×32 / 64×64 | `textures/entity/equipment/…` |
| Mob-Textur | wie im Modell angegeben | `textures/entity/` |
| GUI / HUD | beliebig | `textures/gui/` |

- Dateinamen klein, ohne Leerzeichen und Umlaute.
- PNG mit Transparenz. Programme: [Aseprite](https://www.aseprite.org/), [Paint.NET](https://www.getpaint.net/), [LibreSprite](https://libresprite.github.io/) (kostenlos), oder direkt in Blockbench.

## Checklisten

**Neues einfaches Item**
- [ ] `ModItems`: Zeile mit `item("name", p -> p)`
- [ ] `assets/mymod/items/name.json`
- [ ] `assets/mymod/models/item/name.json`
- [ ] `assets/mymod/textures/item/name.png`
- [ ] `lang/en_us.json` (+ `de_de.json`)
- [ ] in einen Kreativ-Tab
- [ ] optional: Rezept

**Neuer Block**
- [ ] `ModBlocks`: Block mit `setId`
- [ ] `ModItems`: `blockItem(...)`
- [ ] `blockstates/`, `models/block/`, `textures/block/`, `items/`
- [ ] Loot-Table
- [ ] `mineable/<werkzeug>.json` (+ evtl. `needs_…_tool.json`)
- [ ] `lang`
- [ ] Kreativ-Tab

**Neues Werkzeug-Set**
- [ ] `ToolMaterial` in `ModMaterials`
- [ ] Reparatur-Tag in `ModTags` + JSON in `data/mymod/tags/item/`
- [ ] 5 Items in `ModItems`
- [ ] je Item: `items/`, `models/item/` (Parent `item/handheld`), Textur, `lang`
- [ ] Vanilla-Tags `swords.json`, `pickaxes.json` …
- [ ] 5 Rezepte

## Zum Ausprobieren

- Mach einen Block, der leuchtet (`lightLevel`) und nach Glas klingt.
- Mach ein Werkzeug-Set, das schneller als Netherite abbaut, aber nur 100 Haltbarkeit hat.
- Schau dir mit Strg + N die Klasse `Items` an und such `GOLDEN_APPLE` – wie bekommt sie ihre Effekte?
