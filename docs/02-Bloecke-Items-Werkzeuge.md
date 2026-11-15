# 02 – Blöcke, Items und Werkzeuge

## 1. Das Prinzip der Registrierung

Minecraft hat für alles eine **Registry** (ein großes Verzeichnis): eine für Blöcke, eine für Items, eine für Entities usw. Jeder Eintrag hat eine eindeutige ID wie `rubymod:ruby`.

Forge stellt dafür `DeferredRegister` bereit. „Deferred“ heißt *verzögert*: Du sagst nur, **was** registriert werden soll, und Forge erledigt es zum richtigen Zeitpunkt beim Start.

```java
public static final DeferredRegister<Block> BLOCKS =
        DeferredRegister.create(ForgeRegistries.BLOCKS, RubyMod.MODID);

public static final RegistryObject<Block> RUBY_BLOCK = BLOCKS.register("ruby_block",
        () -> new Block(BlockBehaviour.Properties.of()
                .setId(BLOCKS.key("ruby_block"))
                ...));
```

- `RegistryObject<Block>` ist ein **Platzhalter**. Der echte Block existiert erst nach der Registrierung. Mit `.get()` kommst du an ihn heran (z. B. `ModBlocks.RUBY_BLOCK.get()`).
- `() -> new Block(...)` ist ein **Lambda** (eine kleine Funktion ohne Namen). Forge ruft sie erst auf, wenn es so weit ist.
- `.setId(BLOCKS.key("ruby_block"))` ist **seit 1.21.2 Pflicht**. Der Block muss seine eigene ID schon beim Erzeugen kennen. Der Name muss exakt derselbe sein wie bei `register(...)`. Fehlt `setId`, stürzt das Spiel beim Start ab.

## 2. Blöcke (`block/ModBlocks.java`)

```java
public static final RegistryObject<Block> NOXIUM_BLOCK = BLOCKS.register("noxium_block",
        () -> new Block(BlockBehaviour.Properties.of()
                .setId(BLOCKS.key("noxium_block"))
                .mapColor(MapColor.COLOR_BLACK)
                .requiresCorrectToolForDrops()
                .strength(10.0F, 15.0F)
                .sound(SoundType.METAL)));
```

| Eigenschaft | Bedeutung |
|---|---|
| `mapColor(...)` | Farbe des Blocks auf einer Minecraft-Karte |
| `requiresCorrectToolForDrops()` | Droppt nur mit dem richtigen Werkzeug. **Welches** Werkzeug, steht in Tags (siehe [03](03-Erze-und-Weltgenerierung.md)). |
| `strength(härte, explosionsresistenz)` | Härte = wie lange das Abbauen dauert (Stein 1.5, Eisenblock 5, Obsidian 50). Explosionsresistenz = wie gut er Explosionen aushält. |
| `sound(SoundType.METAL)` | Geräusche beim Laufen, Abbauen, Setzen |
| `instrument(NoteBlockInstrument.BASEDRUM)` | Klang eines Notenblocks auf diesem Block |

### Erze geben Erfahrung

```java
public static final RegistryObject<Block> RUBY_ORE = BLOCKS.register("ruby_ore",
        () -> new DropExperienceBlock(UniformInt.of(3, 7), BlockBehaviour.Properties.of()
                .setId(BLOCKS.key("ruby_ore"))
                ...));
```

`DropExperienceBlock` ist eine Vanilla-Klasse für Erze. `UniformInt.of(3, 7)` = beim Abbauen ohne Behutsamkeit fallen 3 bis 7 Erfahrungspunkte, wie bei Diamanterz.

### Was ein Block außer dem Java-Code noch braucht

| Datei | Wofür |
|---|---|
| `assets/rubymod/blockstates/noxium_ore.json` | Welches Modell für welchen Zustand des Blocks |
| `assets/rubymod/models/block/noxium_ore.json` | Das Modell (meist `"parent": "minecraft:block/cube_all"` + Textur) |
| `assets/rubymod/textures/block/noxium_ore.png` | Die Textur, 16×16 Pixel |
| `assets/rubymod/items/noxium_ore.json` | Wie der Block im Inventar aussieht |
| `data/rubymod/loot_table/blocks/noxium_ore.json` | Was beim Abbauen droppt (ohne Loot-Table droppt **nichts**!) |
| `data/minecraft/tags/block/mineable/pickaxe.json` | Mit welchem Werkzeug man ihn schnell abbaut |
| `lang/en_us.json`, `lang/de_de.json` | Name: `"block.rubymod.noxium_ore": "Noxium Ore"` |

## 3. Items (`item/ModItems.java`)

Damit nicht jedes Item fünf Zeilen braucht, gibt es kleine **Hilfsmethoden**:

```java
private static RegistryObject<Item> item(String name, UnaryOperator<Item.Properties> properties) {
    return ITEMS.register(name, () -> new Item(properties.apply(new Item.Properties().setId(ITEMS.key(name)))));
}

private static RegistryObject<Item> blockItem(String name, Supplier<Block> block) {
    return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()
            .setId(ITEMS.key(name))
            .useBlockDescriptionPrefix()));
}
```

- `UnaryOperator<Item.Properties>` = eine Funktion, die Properties bekommt und veränderte Properties zurückgibt. So kann man beim Aufruf schreiben: `item("ruby", p -> p)` (nichts ändern) oder `item("ruby_sword", p -> p.sword(...))`.
- **Jeder Block braucht zusätzlich ein `BlockItem`**, sonst kann man ihn nicht im Inventar halten. `useBlockDescriptionPrefix()` sorgt dafür, dass das Item den Namen des Blocks (`block.rubymod.…`) benutzt.

```java
public static final RegistryObject<Item> RUBY = item("ruby", p -> p);
public static final RegistryObject<Item> RUBY_BLOCK = blockItem("ruby_block", ModBlocks.RUBY_BLOCK);
```

### Was ein Item außer dem Java-Code noch braucht

| Datei | Inhalt |
|---|---|
| `assets/rubymod/items/ruby.json` | **Neu seit 1.21.4:** verweist auf das Modell |
| `assets/rubymod/models/item/ruby.json` | Das Modell, meist `"parent": "minecraft:item/generated"` mit `"layer0": "rubymod:item/ruby"` |
| `assets/rubymod/textures/item/ruby.png` | Die Textur (16×16 oder 32×32) |
| `lang/*.json` | `"item.rubymod.ruby": "Ruby"` |

```json
// assets/rubymod/items/noxium_sword.json
{
  "model": {
    "type": "minecraft:model",
    "model": "rubymod:item/noxium_sword"
  }
}
```

> **Stolperfalle:** In Modell-Pfaden lässt man die Dateiendung weg. `"rubymod:item/noxium_boots.json"` sucht nach `noxium_boots.json.json` → lila-schwarzes Fehler-Muster. Richtig: `"rubymod:item/noxium_boots"`.

> **Item ohne Textur, Block aber richtig?** Dann fehlt meist die Datei in `assets/rubymod/items/`. Das Block-Modell ist dann zwar korrekt, aber das Item weiß nicht, welches Modell es nehmen soll.

## 4. Werkzeuge

### Das Material (`materials/ModMaterials.java`)

```java
public static final ToolMaterial RUBY_TOOL = new ToolMaterial(
    BlockTags.INCORRECT_FOR_DIAMOND_TOOL, // welche Blöcke das Werkzeug NICHT abbauen kann
    1200,  // Haltbarkeit (Eisen 250, Diamant 1561)
    8.5F,  // Abbaugeschwindigkeit (Eisen 6, Diamant 8)
    2.5F,  // Bonus-Angriffsschaden (Eisen 2, Diamant 3)
    15,    // Verzauberbarkeit (Eisen 14, Diamant 10)
    ModTags.RUBY_TOOL_MATERIALS   // womit man es am Amboss repariert
);
```

Der erste Wert ist die **Abbau-Stufe**: Rubin darf alles abbauen, was auch Diamant darf. Noxium benutzt `INCORRECT_FOR_NETHERITE_TOOL` und darf damit alles.

| Material | Stufe wie | Haltbarkeit | Geschwindigkeit | Bonus-Schaden | Verzauberbarkeit |
|---|---|---|---|---|---|
| Rubin | Diamant | 1200 | 8.5 | 2.5 | 15 |
| Eis | Eisen | 1500 | 9.5 | 3.5 | 17 |
| Sumpf | Holz | 2000 | 10.5 | 4.5 | 20 |
| Noxium | Netherite | 2000 | 10.5 | 4.5 | 20 |

### Die Items

Seit 1.21.5 gibt es keine `SwordItem`-Klasse mehr. Ein Schwert ist ein normales `Item` mit Schwert-Eigenschaften:

```java
public static final RegistryObject<Item> RUBY_SWORD   = item("ruby_sword",   p -> p.sword(ModMaterials.RUBY_TOOL, 3.0F, -2.4F));
public static final RegistryObject<Item> RUBY_PICKAXE = item("ruby_pickaxe", p -> p.pickaxe(ModMaterials.RUBY_TOOL, 1.0F, -2.8F));
public static final RegistryObject<Item> RUBY_AXE     = item("ruby_axe",     p -> p.axe(ModMaterials.RUBY_TOOL, 5.0F, -3.0F));
public static final RegistryObject<Item> RUBY_SHOVEL  = item("ruby_shovel",  p -> p.shovel(ModMaterials.RUBY_TOOL, 1.5F, -3.0F));
public static final RegistryObject<Item> RUBY_HOE     = item("ruby_hoe",     p -> p.hoe(ModMaterials.RUBY_TOOL, -2.5F, 0.0F));
```

Die zwei Zahlen sind **Angriffsschaden** und **Angriffsgeschwindigkeit** (dieselben Werte wie bei Diamant-Werkzeugen). Der echte Schaden ist: 1 (Faust) + Zahl + Bonus-Schaden des Materials. Rubin-Schwert: 1 + 3 + 2.5 = **6.5**.

### Der Reparatur-Tag (`tags/ModTags.java`)

```java
public static final TagKey<Item> NOXIUM_TOOL_MATERIALS = itemTag("noxium_tool_materials");

private static TagKey<Item> itemTag(String name) {
    return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(RubyMod.MODID, name));
}
```

Ein **Tag** ist eine benannte Liste. Der Java-Code kennt nur den Namen, der Inhalt steht in JSON:

```json
// data/rubymod/tags/item/noxium_tool_materials.json
{ "values": [ "rubymod:noxium_block" ] }
```

Die Rezepte benutzen denselben Tag mit `#` davor: `"X": "#rubymod:noxium_tool_materials"`.

### Werkzeuge in Vanilla-Tags eintragen

Damit z. B. Verzauberungen wie Schärfe auf deinem Schwert erlaubt sind, stehen deine Werkzeuge in den Vanilla-Tags:

```
data/minecraft/tags/item/swords.json     pickaxes.json   axes.json   shovels.json   hoes.json
data/minecraft/tags/item/head_armor.json chest_armor.json leg_armor.json foot_armor.json   bow.json
```

```json
{ "replace": false, "values": [ "rubymod:ruby_sword", "rubymod:noxium_sword", ... ] }
```

**`"replace": false` ist wichtig!** Mit `true` würdest du die Vanilla-Liste ersetzen, und Diamantschwerter wären plötzlich keine Schwerter mehr.

> Im Moment stehen in diesen Tags nur die **Rubin**-Sachen. Eis-, Sumpf- und Noxium-Werkzeuge sowie die Noxium-Rüstung fehlen noch – sie lassen sich deshalb am Zaubertisch nicht wie erwartet verzaubern. Einfach in die passende Liste dazuschreiben, z. B. `"rubymod:noxium_sword"` in `swords.json`.

## 5. Essen (`ruby_apple`)

```java
public static final RegistryObject<Item> RUBY_APPLE = item("ruby_apple", p -> p
        .rarity(Rarity.UNCOMMON)
        .food(new FoodProperties.Builder().nutrition(6).saturationModifier(1.2F).build()));
```

- `nutrition(6)` = füllt 3 Hunger-Keulen auf.
- `saturationModifier(1.2F)` = wie lange man satt bleibt (Goldene Karotte: 1.2).
- `rarity(Rarity.UNCOMMON)` = der Name erscheint gelb.

In `food/ModFoodProperties.java` stehen dieselben Werte als Konstante. Die Klasse ist dafür gedacht, Nahrungswerte an einer Stelle zu sammeln: `p.food(ModFoodProperties.RUBY_APPLE)`. Im Moment benutzt `ModItems` sie aber noch nicht.

## 6. Kreativ-Tabs (`creativemodetab/ModCreativeTabs.java`)

### Eigener Tab

```java
public static final RegistryObject<CreativeModeTab> NOXIUM_TAB = CREATIVE_MODE_TABS.register("noxium_tab",
        () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.rubymod.noxium_tab"))
                .withTabsBefore(CreativeModeTabs.COMBAT)   // erscheint nach dem Kampf-Tab
                .icon(() -> ModItems.NOXIUM_INGOT.get().getDefaultInstance())
                .displayItems((params, output) -> {
                    output.accept(ModItems.NOXIUM_INGOT.get());
                    output.accept(ModItems.NOXIUM_SWORD.get());
                    // ...
                })
                .build());
```

Du kannst auch Vanilla-Items hineinlegen, z. B. `output.accept(Items.ICE);` im Eis-Tab.

### In Vanilla-Tabs einsortieren

```java
public static void registerListeners() {
    BuildCreativeModeTabContentsEvent.BUS.addListener(ModCreativeTabs::addToVanillaTabs);
}

private static void addToVanillaTabs(BuildCreativeModeTabContentsEvent event) {
    var tab = event.getTabKey();
    if (tab == CreativeModeTabs.INGREDIENTS) {
        event.accept(ModItems.RUBY);
    } else if (tab == CreativeModeTabs.COMBAT) {
        event.accept(ModItems.RUBY_SWORD);
        // ...
    }
}
```

`registerListeners()` wird in `RubyMod` aufgerufen. Wenn du den Aufruf auskommentierst, aber die Methode löschst (oder umgekehrt), kommt *„Cannot resolve method 'registerListeners'“*.

## 7. Rezepte (`data/rubymod/recipe/`)

### Geformt (shaped)

```json
{
  "type": "minecraft:crafting_shaped",
  "category": "equipment",
  "pattern": [ "X", "X", "#" ],
  "key": {
    "X": "#rubymod:noxium_tool_materials",
    "#": "minecraft:stick"
  },
  "result": { "id": "rubymod:noxium_sword" }
}
```

- `pattern`: jede Zeile ist eine Reihe im Werkbank-Raster, Leerzeichen = leeres Feld.
- `key`: was jedes Zeichen bedeutet. `#` vor einem Namen heißt **Tag**, ohne `#` heißt **Item**.
- Mehr als 1 Ergebnis: `"result": { "id": "rubymod:noxium_ingot", "count": 9 }`.

### Schmelzen

```json
{
  "type": "minecraft:smelting",          // oder "minecraft:blasting" (Schmelzofen, doppelt so schnell)
  "ingredient": "rubymod:ruby_ore",
  "result": { "id": "rubymod:ruby" },
  "experience": 1.0,
  "cookingtime": 200                      // Ticks; Schmelzofen: 100
}
```

Tipp: Mit [misode.github.io](https://misode.github.io/) kannst du Rezepte für die passende Version online zusammenklicken.

## 8. Loot-Tables (`data/rubymod/loot_table/blocks/`)

Ohne Loot-Table droppt ein Block **gar nichts**. Für einen einfachen Block reicht:

```json
{
  "type": "minecraft:block",
  "pools": [ { "rolls": 1, "entries": [ { "type": "minecraft:item", "name": "rubymod:ruby_block" } ],
               "conditions": [ { "condition": "minecraft:survives_explosion" } ] } ]
}
```

Für Erze (Behutsamkeit, Glück, Drop-Menge) siehe [03 – Erze und Weltgenerierung](03-Erze-und-Weltgenerierung.md#4-was-das-erz-droppt).

## 9. Sprachdateien (`assets/rubymod/lang/`)

```json
{
  "item.rubymod.ruby": "Rubin",
  "block.rubymod.ruby_block": "Rubinblock",
  "itemGroup.rubymod.ruby_tab": "Rubin-Mod",
  "entity.rubymod.ulyssan_dragon": "Ulyssan",
  "bossbar.rubymod.ulyssan_dragon": "Ulyssan, der Frostdrache"
}
```

- `en_us.json` ist die Pflicht-Sprache. Fehlt ein Eintrag in `de_de.json`, nimmt Minecraft den englischen.
- Fehlt er in beiden, siehst du im Spiel den rohen Schlüssel, z. B. `item.rubymod.ruby`.
- **Komma** hinter jeder Zeile außer der letzten! Ein fehlendes oder überzähliges Komma macht die ganze Datei kaputt.

## 10. Texturen

| Was | Größe | Ordner |
|---|---|---|
| Blöcke | 16×16 | `textures/block/` |
| Items | 16×16 (oder 32×32 für mehr Details) | `textures/item/` |
| Rüstung am Körper | 64×32 / 64×64 | `textures/entity/equipment/…` |
| Mob-Textur | beliebig, wie im Modell angegeben | `textures/entity/` |
| GUI | beliebig | `textures/gui/` |

- Dateinamen **nur klein**, keine Leerzeichen, keine Umlaute: `noxium_ingot.png`, nicht `Noxium Ingot.png`.
- Texturen kannst du im laufenden Spiel mit **F3 + T** neu laden. Java-Code dagegen nur durch Neustart.
- Für die Mod wurden Texturen für **Noxium** (Erz, Block, Barren, Nugget, Werkzeuge im Kristall-Stil, Bogen), **Ominus** (Erz, Block, Barren) und **Demon Soul Fire** (Erz, Block, Barren in Flammenform, Schwert, Axt, Hacke, Bogen, Armbrust) erstellt. Ominus und Demon Soul Fire haben noch keinen Code.
