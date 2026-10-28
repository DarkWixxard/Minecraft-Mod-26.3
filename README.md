# Ruby Mod (Minecraft Forge 26.3)

Eine Forge-Mod für **Minecraft 26.3** (Forge `26.3-66.1.0`), die Rubine ins Spiel bringt.

## Inhalt

| Inhalt | ID | Beschreibung |
|---|---|---|
| Rubin | `rubymod:ruby` | Edelstein, Grundzutat |
| Rubinblock | `rubymod:ruby_block` | Speicherblock, braucht mind. Eisenspitzhacke |
| Rubinapfel | `rubymod:ruby_apple` | Nahrung (6 Hunger, hohe Sättigung), immer essbar |
| Rubinerz / Tiefenschiefer-Rubinerz | `rubymod:ruby_ore`, `rubymod:deepslate_ruby_ore` | Generiert in allen Oberwelt-Biomen zwischen Y −64 und 32 (am häufigsten um Y −16), braucht mind. Eisenspitzhacke, Behutsamkeit und Glück funktionieren |
| Rubin-Werkzeuge | `rubymod:ruby_sword`, `_pickaxe`, `_axe`, `_shovel`, `_hoe` | Zwischen Eisen und Diamant: 1200 Haltbarkeit, Abbaugeschwindigkeit 8,5, verzauberbar |
| Rubin-Rüstung | `rubymod:ruby_helmet`, `_chestplate`, `_leggings`, `_boots` | 19 Rüstungspunkte (Eisen 15, Diamant 20), Härte 1, mit Rubinen reparierbar |

Alles erscheint im eigenen Kreativ-Tab **„Rubin-Mod“** und zusätzlich in den passenden Vanilla-Tabs.

### Rezepte

- **Rubin ×4**: 8 Redstone um 1 Diamant
- **Rubinblock**: 9 Rubine (3×3)
- **Rubin ×9**: 1 Rubinblock
- **Rubinapfel**: 8 Rubine um 1 Apfel
- **Rubin**: Rubinerz im Ofen schmelzen oder im Schmelzofen (doppelt so schnell)
- **Werkzeuge & Rüstung**: gleiche Muster wie bei Diamant, nur mit Rubinen

## Voraussetzungen

- **Java 25** (JDK) – Minecraft 26.x benötigt Java 25

## Bauen & Starten

```bash
./gradlew build          # Mod-JAR landet in build/libs/rubymod-1.0.0.jar
./gradlew runClient      # Minecraft-Client mit der Mod starten
./gradlew runServer      # Dedizierten Server starten
```

Zum Spielen die JAR aus `build/libs/` in den `mods`-Ordner einer Forge-26.3-Installation kopieren.

## Projektstruktur

```
src/main/java/com/darkwixxard/rubymod/
├── RubyMod.java                  # Einstiegspunkt (@Mod)
└── registry/
    ├── ModBlocks.java            # Blöcke (Rubinblock, Erze)
    ├── ModItems.java             # Items, Werkzeuge, Rüstung
    ├── ModMaterials.java         # Werkzeug- und Rüstungsmaterial (Werte)
    ├── ModTags.java              # eigene Item-Tags
    └── ModCreativeTabs.java      # Kreativ-Tab
src/main/resources/
├── META-INF/mods.toml            # Mod-Metadaten
├── assets/rubymod/               # Modelle, Texturen, Sprachdateien (de_de, en_us)
├── assets/rubymod/equipment/     # Aussehen der getragenen Rüstung
└── data/
    ├── rubymod/recipe/           # Rezepte
    ├── rubymod/loot_table/       # Drops der Blöcke
    ├── rubymod/worldgen/         # Erz-Feature (was & wo generiert wird)
    ├── rubymod/forge/biome_modifier/  # fügt das Erz den Biomen hinzu
    └── minecraft/tags/           # Werkzeug-/Rüstungs-Tags, Abbau-Tags
```

## Texturen

Die Texturen für Werkzeuge, Rüstung und Erze sind rot eingefärbte Versionen der Vanilla-Diamant-Texturen
und als Platzhalter gedacht. Vor einer Veröffentlichung (z. B. auf CurseForge/Modrinth) sollten sie durch
eigene Texturen ersetzt werden.
