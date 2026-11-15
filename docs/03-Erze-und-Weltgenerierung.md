# 03 – Erze und Weltgenerierung

Damit ein Erz in der Welt erscheint, braucht es **drei JSON-Dateien**, die aufeinander aufbauen. Java-Code ist dafür nicht nötig.

```
feature/ore_noxium.json           WAS wird gesetzt? (welcher Block, welche Ader-Größe, welches Gestein wird ersetzt)
        ↓
placed_feature/ore_noxium.json    WO und WIE OFT? (Anzahl pro Chunk, Höhe)
        ↓
forge/biome_modifier/add_ore_noxium.json   IN WELCHEN BIOMEN?
```

## 1. Configured Feature: `data/rubymod/worldgen/feature/ore_noxium.json`

```json
{
  "type": "minecraft:ore",
  "size": 5,
  "discard_chance_on_air_exposure": 0.3,
  "targets": [
    {
      "state": "rubymod:noxium_ore",
      "target": { ... "tag": "minecraft:stone_ore_replaceables" ... }
    },
    {
      "state": "rubymod:deepslate_noxium_ore",
      "target": { ... "tag": "minecraft:deepslate_ore_replaceables" ... }
    }
  ]
}
```

| Feld | Bedeutung |
|---|---|
| `size` | Maximale Größe einer Ader (Diamant: 8, Kohle: 17) |
| `discard_chance_on_air_exposure` | Wahrscheinlichkeit, dass ein Erzblock, der an Luft (Höhle) grenzt, **nicht** gesetzt wird. 0.3 = 30 %. Macht Erze in Höhlen seltener. |
| `targets` | Welcher Block welches Gestein ersetzt: normales Erz ersetzt Stein, die Tiefenschiefer-Variante ersetzt Tiefenschiefer. |

## 2. Placed Feature: `data/rubymod/worldgen/placed_feature/ore_noxium.json`

```json
{
  "feature": "rubymod:ore_noxium",
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
| `in_square` | Jeder Versuch an einer zufälligen X/Z-Position im Chunk |
| `height_range` mit `trapezoid` | Zufällige Höhe zwischen Weltboden (Y −64) und Y 32. **Trapez** heißt: in der Mitte (um Y −16) am häufigsten, zu den Rändern hin seltener. Alternativ `uniform` = überall gleich häufig. |
| `biome` | Prüft, ob das Biom an dieser Stelle das Feature überhaupt haben darf (kommt vom Biome Modifier) |

## 3. Biome Modifier: `data/rubymod/forge/biome_modifier/add_ore_noxium.json`

```json
{
  "type": "forge:add_features",
  "biomes": "#minecraft:is_overworld",
  "features": "rubymod:ore_noxium",
  "step": "underground_ores"
}
```

- `biomes`: in welchen Biomen. `#minecraft:is_overworld` = alle Oberwelt-Biome. Für den Nether wäre es `#minecraft:is_nether`.
- `step: underground_ores`: In welchem Schritt der Weltgenerierung das Erz gesetzt wird.

## 4. Was das Erz droppt

`data/rubymod/loot_table/blocks/noxium_ore.json`:

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
          "name": "rubymod:noxium_ore"
        },
        {
          "type": "minecraft:item",
          "name": "rubymod:noxium_nugget",
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

- **`alternatives`**: Nimm den **ersten** Eintrag, dessen Bedingung erfüllt ist.
  1. Mit Behutsamkeit (`can_silk_touch`) → das Erz selbst.
  2. Sonst → ein Noxium-Nugget.
- **`apply_bonus` mit `ore_drops`**: Die Verzauberung **Glück**. Glück III macht aus 1 bis zu 4.
- **`explosion_decay`**: Bei einer Explosion geht ein Teil der Drops verloren.

### Drop-Menge ändern

`set_count` **vor** `apply_bonus` einfügen:

```json
"functions": [
  { "function": "minecraft:set_count", "count": 2 },                                         // immer 2
  // oder: { "function": "minecraft:set_count", "count": { "type": "minecraft:uniform", "min": 1, "max": 3 } }
  { "function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops" },
  { "function": "minecraft:explosion_decay" }
]
```

Beide Dateien (Stein- und Tiefenschiefer-Erz) gleich ändern!

## 5. Mit welchem Werkzeug man ein Erz abbauen darf

Das steht **nicht im Java-Code**, sondern in Block-Tags. Zwei Fragen:

### a) Welches Werkzeug ist das richtige? → `mineable/…`

```json
// data/minecraft/tags/block/mineable/pickaxe.json
{ "replace": false, "values": [ "rubymod:ruby_ore", "rubymod:noxium_ore", ... ] }
```

Ohne diesen Eintrag baut die Spitzhacke den Block nur so langsam ab wie die Hand.

### b) Welche Stufe braucht es mindestens? → `needs_…_tool` / `incorrect_for_…_tool`

Jede Werkzeug-Stufe hat einen Tag mit allem, was sie **nicht** abbauen darf:

| Werkzeug | Tag „darf nicht abbauen“ | enthält in Vanilla |
|---|---|---|
| Holz, Gold | `incorrect_for_wooden_tool` / `incorrect_for_gold_tool` | `needs_stone_tool`, `needs_iron_tool`, `needs_diamond_tool` |
| Stein, Kupfer | `incorrect_for_stone_tool` / `incorrect_for_copper_tool` | `needs_iron_tool`, `needs_diamond_tool` |
| Eisen | `incorrect_for_iron_tool` | `needs_diamond_tool` |
| Diamant | `incorrect_for_diamond_tool` | **leer** |
| Netherite | `incorrect_for_netherite_tool` | **leer** |

**Rubin** steht in `needs_iron_tool.json` → mindestens Eisenspitzhacke.

**Noxium** soll mindestens **Netherite** brauchen. Dafür braucht es zwei Einträge:

```json
// data/minecraft/tags/block/needs_diamond_tool.json   → Holz bis Eisen ausgeschlossen
// data/minecraft/tags/block/incorrect_for_diamond_tool.json   → auch Diamant ausgeschlossen
{
  "replace": false,
  "values": [ "rubymod:noxium_block", "rubymod:noxium_ore", "rubymod:deepslate_noxium_ore" ]
}
```

> ⚠️ **Achtung, Dateinamen und Ordner müssen exakt stimmen.** Bei dir heißen die Dateien im Moment `tags/block/need_diamond_tool.json` (es fehlt das **s** in `needs`) und `tags/block/mineable/incorrect_for_diamond_tool.json` (falscher Ordner, gehört direkt nach `tags/block/`). Minecraft ignoriert beide ohne Fehlermeldung. Deshalb lässt sich Noxium im Moment noch mit jeder Spitzhacke abbauen. Siehe [11 – Fehler und Lösungen](11-Fehler-und-Loesungen.md).

### Das Werkzeug-Material muss dazu passen

In `ModMaterials` hat `NOXIUM_TOOL` deshalb `BlockTags.INCORRECT_FOR_NETHERITE_TOOL`. Hätte es `INCORRECT_FOR_DIAMOND_TOOL`, könnte die Noxium-Spitzhacke ihr eigenes Erz nicht abbauen.

## 6. Häufigkeit testen und anpassen

Bei einem Testlauf (neue Welt, 64 Chunks gezählt) kam heraus:

| Erz | pro Chunk |
|---|---|
| Noxium (Stein) | 1,5 |
| Noxium (Tiefenschiefer) | 7,8 |
| Rubin (Stein) | 1,5 |
| Rubin (Tiefenschiefer) | 8,4 |

Noxium ist also **genauso häufig wie Rubin** und häufiger als Diamant (≈ 4 pro Chunk), weil die Dateien kopiert wurden. Wenn Noxium seltener sein soll:

```json
{ "type": "minecraft:count", "count": 2 }
...
"max_inclusive": { "absolute": -16 }      // nur noch tief im Tiefenschiefer
```

und in `feature/ore_noxium.json` `"size": 4`.

### Warum finde ich das Erz nicht?

1. **Alte Welt:** Erze entstehen nur, wenn ein Chunk **zum ersten Mal** erzeugt wird. Bereiche, die du vor dem Einbau des Erzes besucht hast, enthalten es nie. → Neue Welt erstellen oder weit wegreisen.
2. **Zu hoch gesucht:** Noxium liegt zwischen Y −64 und 32, am häufigsten um Y −16. Über 85 % sind die Tiefenschiefer-Variante. Höhe siehst du mit **F3**.
3. **Schwer zu sehen:** Dunkles Erz in dunklem Gestein.

**Schnelltest** in einer neuen Kreativ-Welt (entfernt allen Tiefenschiefer unter dir, die Erze schweben dann in der Luft):

```
/fill ~-10 -60 ~-10 ~10 0 ~10 minecraft:air replace minecraft:deepslate
```
