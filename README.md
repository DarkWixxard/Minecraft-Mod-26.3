# Ruby Mod (Minecraft Forge 26.3)

Eine Forge-Mod für **Minecraft 26.3** (Forge `26.3-66.1.0`), die Rubine ins Spiel bringt.

## Inhalt

| Inhalt | ID | Beschreibung |
|---|---|---|
| Rubin | `rubymod:ruby` | Edelstein, Grundzutat |
| Rubinblock | `rubymod:ruby_block` | Speicherblock, braucht mind. Eisenspitzhacke |
| Rubinapfel | `rubymod:ruby_apple` | Nahrung (6 Hunger, hohe Sättigung), immer essbar |

Alles erscheint im eigenen Kreativ-Tab **„Rubin-Mod“** und zusätzlich in den passenden Vanilla-Tabs.

### Rezepte

- **Rubin ×4**: 8 Redstone um 1 Diamant
- **Rubinblock**: 9 Rubine (3×3)
- **Rubin ×9**: 1 Rubinblock
- **Rubinapfel**: 8 Rubine um 1 Apfel

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
    ├── ModBlocks.java            # Blöcke
    ├── ModItems.java             # Items
    └── ModCreativeTabs.java      # Kreativ-Tab
src/main/resources/
├── META-INF/mods.toml            # Mod-Metadaten
├── assets/rubymod/               # Modelle, Texturen, Sprachdateien (de_de, en_us)
└── data/                         # Rezepte, Loot-Tables, Block-Tags
```
