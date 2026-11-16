# 03 – Erze und Weltgenerierung

Damit ein Erz in der Welt erscheint, braucht es **drei JSON-Dateien**, die aufeinander aufbauen. Java-Code ist nicht nötig.

```
data/mymod/worldgen/feature/ore_sapphire.json          WAS? (Block, Adergröße, welches Gestein wird ersetzt)
        ↓
data/mymod/worldgen/placed_feature/ore_sapphire.json   WO und WIE OFT? (Anzahl pro Chunk, Höhe)
        ↓
data/mymod/forge/biome_modifier/add_ore_sapphire.json  IN WELCHEN BIOMEN?
```

Der Name in `"feature": "mymod:ore_sapphire"` verweist jeweils auf den **Dateinamen** der Stufe davor.

## 1. Configured Feature – WAS

```json
{
  "type": "minecraft:ore",
  "size": 5,
  "discard_chance_on_air_exposure": 0.3,
  "targets": [
    {
      "state": "mymod:sapphire_ore",
      "target": { "predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables" }
    },
    {
      "state": "mymod:deepslate_sapphire_ore",
      "target": { "predicate_type": "minecraft:tag_match", "tag": "minecraft:deepslate_ore_replaceables" }
    }
  ]
}
```

| Feld | Bedeutung |
|---|---|
| `size` | Maximale Größe einer Ader (Diamant 4, Eisen 9, Kohle 17) |
| `discard_chance_on_air_exposure` | Wahrscheinlichkeit, dass ein Erzblock neben Luft (Höhle) **nicht** gesetzt wird. 0 = egal, 1 = nie an Höhlenwänden. Diamant: 0.5. |
| `targets` | Welcher Block welches Gestein ersetzt. Die Tiefenschiefer-Variante ersetzt Tiefenschiefer. |

Für Nether-Erze: Ziel-Tag `minecraft:base_stone_nether` bzw. ein `block_match` auf `minecraft:netherrack`.

> Genaues Format für deine Version: am einfachsten mit [misode.github.io → Configured Feature](https://misode.github.io/worldgen/feature/) zusammenklicken oder eine Vanilla-Datei kopieren (`data/minecraft/worldgen/configured_feature/ore_diamond.json` in der Minecraft-Bibliothek).

## 2. Placed Feature – WO und WIE OFT

```json
{
  "feature": "mymod:ore_sapphire",
  "placement": [
    { "type": "minecraft:count", "count": 4 },
    { "type": "minecraft:in_square" },
    {
      "type": "minecraft:height_range",
      "height": {
        "type": "minecraft:trapezoid",
        "min_inclusive": { "above_bottom": 0 },
        "max_inclusive": { "absolute": 32 }
      }
    },
    { "type": "minecraft:biome" }
  ]
}
```

Die Einträge werden **der Reihe nach** angewendet:

| Schritt | Bedeutung |
|---|---|
| `count: 4` | 4 Versuche pro Chunk |
| `in_square` | jeder Versuch an zufälliger X/Z-Position im Chunk |
| `height_range` | zufällige Höhe im Bereich |
| `biome` | prüft, ob das Biom das Feature haben darf (kommt vom Biome Modifier) – immer ans Ende |

### Höhenverteilung

| Typ | Verteilung |
|---|---|
| `uniform` | überall im Bereich gleich häufig |
| `trapezoid` | in der **Mitte** des Bereichs am häufigsten, zu den Rändern seltener. Bereich −64 bis 32 → am häufigsten um −16. |

| Höhenangabe | Bedeutung |
|---|---|
| `{ "absolute": 32 }` | genau Y 32 |
| `{ "above_bottom": 0 }` | Weltboden (Y −64) + 0 |
| `{ "below_top": 0 }` | Weltobergrenze − 0 |

Seltene Erze: `rarity_filter` statt `count`: `{ "type": "minecraft:rarity_filter", "chance": 4 }` = nur in jedem 4. Chunk ein Versuch.

### Zum Vergleich (Vanilla, ungefähr)

| Erz | Versuche pro Chunk | Adergröße | Höhe |
|---|---|---|---|
| Kohle | 30 + 20 | 17 | 0 bis 192 |
| Eisen | 90 + 10 + 10 | 9 | −64 bis 320 (mehrere Schichten) |
| Diamant | 7 (+ selten große Adern) | 4 / 12 | −64 bis 16 |

## 3. Biome Modifier – IN WELCHEN BIOMEN (Forge)

```json
{
  "type": "forge:add_features",
  "biomes": "#minecraft:is_overworld",
  "features": "mymod:ore_sapphire",
  "step": "underground_ores"
}
```

| Feld | Werte |
|---|---|
| `biomes` | `#minecraft:is_overworld`, `#minecraft:is_nether`, `#minecraft:is_end`, `#minecraft:is_mountain`, `#minecraft:is_ocean` … oder ein einzelnes Biom `minecraft:plains` |
| `step` | `underground_ores` für Erze. Weitere: `vegetal_decoration` (Pflanzen), `underground_decoration` … |

## 4. Was das Erz droppt

`data/mymod/loot_table/blocks/sapphire_ore.json`:

```json
{
  "type": "minecraft:block",
  "pools": [ {
    "rolls": 1,
    "entries": [ {
      "type": "minecraft:alternatives",
      "children": [
        {
          "type": "minecraft:item",
          "condition": "minecraft:tool/can_silk_touch",
          "name": "mymod:sapphire_ore"
        },
        {
          "type": "minecraft:item",
          "name": "mymod:sapphire",
          "functions": [
            { "function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops" },
            { "function": "minecraft:explosion_decay" }
          ]
        }
      ]
    } ]
  } ]
}
```

- **`alternatives`**: Der **erste** Eintrag, dessen Bedingung passt, gewinnt.
  1. Mit Behutsamkeit (`minecraft:tool/can_silk_touch` ist eine fertige Vanilla-Bedingung) → das Erz selbst.
  2. Sonst → das Material.
- **`apply_bonus` / `ore_drops`**: Verzauberung **Glück** (Glück III → bis zu 4×).
- **`explosion_decay`**: Bei Explosionen geht ein Teil verloren.

Am einfachsten: Vanilla-Datei kopieren (`data/minecraft/loot_table/blocks/diamond_ore.json`) und nur die Namen ändern. So stimmt das Format sicher für deine Version.

### Drop-Menge ändern

`set_count` **vor** `apply_bonus`:

```json
"functions": [
  { "function": "minecraft:set_count", "count": 2 },
  { "function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops" },
  { "function": "minecraft:explosion_decay" }
]
```

Zufällig 1–3: `"count": { "type": "minecraft:uniform", "min": 1, "max": 3 }`.

Stein- **und** Tiefenschiefer-Variante gleich ändern.

## 5. Mit welchem Werkzeug man abbauen darf

Steht **nicht im Java-Code**, sondern in Block-Tags. Zwei Fragen:

### a) Welches Werkzeug ist das richtige? → `mineable/…`

```json
// data/minecraft/tags/block/mineable/pickaxe.json   (auch axe, shovel, hoe)
{ "replace": false, "values": [ "mymod:sapphire_ore", "mymod:deepslate_sapphire_ore", "mymod:sapphire_block" ] }
```

Ohne Eintrag baut auch die Spitzhacke so langsam ab wie die Hand.

### b) Welche Stufe braucht es mindestens? → `needs_…_tool` / `incorrect_for_…_tool`

Jede Stufe hat einen Tag mit allem, was sie **nicht** abbauen darf:

| Werkzeug | Tag „darf nicht abbauen“ | enthält in Vanilla |
|---|---|---|
| Holz, Gold | `incorrect_for_wooden_tool` / `incorrect_for_gold_tool` | `needs_stone_tool`, `needs_iron_tool`, `needs_diamond_tool` |
| Stein, Kupfer | `incorrect_for_stone_tool` / `incorrect_for_copper_tool` | `needs_iron_tool`, `needs_diamond_tool` |
| Eisen | `incorrect_for_iron_tool` | `needs_diamond_tool` |
| Diamant | `incorrect_for_diamond_tool` | **leer** |
| Netherite | `incorrect_for_netherite_tool` | **leer** |

Daraus folgt:

| Gewünschte Mindeststufe | Block eintragen in |
|---|---|
| Stein | `data/minecraft/tags/block/needs_stone_tool.json` |
| Eisen | `needs_iron_tool.json` |
| Diamant | `needs_diamond_tool.json` |
| **Netherite** | `needs_diamond_tool.json` **und** `incorrect_for_diamond_tool.json` |

```json
{ "replace": false, "values": [ "mymod:sapphire_ore", "mymod:deepslate_sapphire_ore", "mymod:sapphire_block" ] }
```

> ⚠️ **Dateiname und Ordner müssen exakt stimmen.** `need_diamond_tool.json` (ohne s) oder `mineable/incorrect_for_diamond_tool.json` (falscher Ordner) werden **ohne Fehlermeldung** ignoriert – der Block lässt sich dann mit jeder Spitzhacke abbauen. Richtig: `tags/block/needs_diamond_tool.json` und `tags/block/incorrect_for_diamond_tool.json`.

### Das eigene Werkzeug muss dazu passen

Braucht dein Erz Netherite, muss die Spitzhacke aus diesem Material `BlockTags.INCORRECT_FOR_NETHERITE_TOOL` als Stufe haben ([02](02-Bloecke-Items-Werkzeuge.md#4-werkzeuge)). Sonst kann sie ihr eigenes Erz nicht abbauen.

## 6. Testen

### Erz findet sich nicht?

1. **Alte Welt:** Erze entstehen nur, wenn ein Chunk **zum ersten Mal** erzeugt wird. Gebiete, die vor dem Einbau besucht wurden, enthalten es nie. → Neue Welt oder weit wegreisen.
2. **Falsche Höhe:** Mit **F3** die Y-Koordinate prüfen.
3. **Schwer zu sehen:** Dunkles Erz in dunklem Gestein → Textur auffälliger machen.
4. **Tippfehler** in einer der drei Dateien: Log nach `mymod` und `worldgen` durchsuchen.

### Schnelltest

In einer **neuen** Kreativ-Welt (entfernt Stein bzw. Tiefenschiefer unter dir, die Erze schweben dann in der Luft):

```
/fill ~-10 -60 ~-10 ~10 0 ~10 minecraft:air replace minecraft:deepslate
/fill ~-10 0 ~-10 ~10 60 ~10 minecraft:air replace minecraft:stone
```

### Häufigkeit messen

Wer es genau wissen will: einen Bereich mit `/fill … replace` freilegen und zählen, oder einen Server starten und mit einem kleinen Befehl/Programm die Blöcke in vielen Chunks zählen. Zum Einordnen: Diamant kommt auf etwa 4 Blöcke pro Chunk.

## Checkliste: neues Erz

- [ ] Block(s) in `ModBlocks` (Stein + Tiefenschiefer), BlockItems
- [ ] Modelle, Blockstates, Texturen, `lang`
- [ ] Loot-Tables (Behutsamkeit, Glück)
- [ ] `mineable/pickaxe.json` + Stufen-Tag
- [ ] `worldgen/feature/…`
- [ ] `worldgen/placed_feature/…`
- [ ] `forge/biome_modifier/…`
- [ ] Schmelz-Rezepte (Ofen und Schmelzofen)
- [ ] In einer **neuen** Welt testen

## Zum Ausprobieren

- Mach ein Erz, das nur in Bergen über Y 100 vorkommt.
- Mach ein Nether-Erz, das Netherrack ersetzt.
- Öffne `data/minecraft/worldgen/placed_feature/ore_diamond.json` in der Minecraft-Bibliothek und vergleiche es mit deinem.
