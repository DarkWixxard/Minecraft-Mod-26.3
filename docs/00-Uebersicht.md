# Ruby Mod – Dokumentation

Diese Dokumentation fasst alles zusammen, was beim Bau der **Ruby Mod** für **Minecraft Forge 26.3** gemacht und gelernt wurde: vom ersten Projekt-Setup über Erze, Werkzeuge und Rüstung bis zum fliegenden Boss-Drachen mit eigener Bossbar.

Jede Datei behandelt ein Thema. Sie erklärt nicht nur, *was* im Code steht, sondern auch *warum*, welche Fehler dabei aufgetreten sind und wie du ähnliche Dinge selbst baust.

## Inhaltsverzeichnis

| Datei | Thema |
|---|---|
| [01 – Einrichtung und Grundlagen](01-Einrichtung-und-Grundlagen.md) | Java 25, IntelliJ, Gradle, Starten und Debuggen, Projektaufbau, Unterschiede zu 1.16.5 |
| [02 – Blöcke, Items und Werkzeuge](02-Bloecke-Items-Werkzeuge.md) | Registrierung, Blöcke, Items, Werkzeug-Materialien, Essen, Kreativ-Tabs, Modelle, Texturen, Rezepte, Loot-Tables, Sprachdateien |
| [03 – Erze und Weltgenerierung](03-Erze-und-Weltgenerierung.md) | Feature, Placed Feature, Biome Modifier, Abbau-Stufen über Tags, Drop-Mengen |
| [04 – Rüstung](04-Ruestung.md) | Normale Rüstung (Rubin), 3D-Rüstung mit GeckoLib (Noxium), Blockbench, Set-Effekte |
| [05 – Der Noxium-Bogen](05-Noxium-Bogen.md) | Eigene Item-Klasse, drei Pfeile, Schaden, Spann-Animation |
| [06 – Der Drache: Entity und GeckoLib](06-Drache-Entity-GeckoLib.md) | Entity registrieren, Attribute, KI-Goals, Renderer, Modell- und Animationsdateien |
| [07 – Der Drache: Hitboxen](07-Drache-Hitboxen.md) | Mehrteilige Hitboxen wie beim Enderdrachen, Forge-Fehler beim Schlagen und die Umgehung |
| [08 – Der Drache: Animationen](08-Drache-Animationen.md) | Zwei Controller, Dauer- und Einmal-Animationen, Todesanimation |
| [09 – Der Drache: Fliegen](09-Drache-Fliegen.md) | Bewegungs-„Gehirne“ tauschen, synchronisierte Daten, Flug-Goal, Speichern |
| [10 – Der Drache: Boss-Funktionen](10-Drache-Boss.md) | Despawn verhindern, Bossbar, eigene Bossbar-Textur, Lebens-Grenze 1024 |
| [11 – Fehler und Lösungen](11-Fehler-und-Loesungen.md) | Alle Fehler aus dem Projekt mit Ursache und Lösung, plus offene Punkte |
| [12 – Werkzeuge und Quellen](12-Werkzeuge-und-Quellen.md) | Blockbench, GeckoLib-Plugin, IntelliJ-Tricks, Webseiten zum Lernen |

## Was die Mod enthält

| Bereich | Inhalt |
|---|---|
| **Rubin** | Rubin, Rubinblock, Rubinapfel, Rubinerz (Stein und Tiefenschiefer), 5 Werkzeuge, 4 Rüstungsteile |
| **Noxium** | Barren, Nugget, Block, Erz (Stein und Tiefenschiefer), 5 Werkzeuge, Bogen mit drei Pfeilen, 3D-Rüstung mit Set-Effekten |
| **Eis** | 5 Werkzeuge |
| **Sumpf (Bogged)** | 5 Werkzeuge |
| **Ominus / Demon Soul Fire** | Texturen sind vorbereitet, Code fehlt noch |
| **Ulyssan-Drache** | Boss-Mob mit GeckoLib-Modell, 57 Animationen, mehrteiligen Hitboxen, Flug-KI und eigener Bossbar |

## Technische Eckdaten

| Was | Version |
|---|---|
| Minecraft | 26.3 |
| Forge | 26.3-66.1.0 |
| ForgeGradle | 7 |
| Gradle | 9.7.1 |
| Java | 25 |
| GeckoLib | 5.5.7 (`com.geckolib:geckolib-forge-26.3:5.5.7`) |
| Mod-ID | `rubymod` |
| Paket | `com.darkwixxard.rubymod` |

## Aufbau des Codes

```
src/main/java/com/darkwixxard/rubymod/
├── RubyMod.java                      ← Einstiegspunkt der Mod
├── block/ModBlocks.java              ← alle Blöcke
├── item/
│   ├── ModItems.java                 ← alle Items
│   └── custom/
│       ├── NoxiumArmorItem.java      ← 3D-Rüstung + Set-Effekte
│       └── NoxiumBowItem.java        ← Bogen mit 3 Pfeilen
├── materials/ModMaterials.java       ← Werkzeug- und Rüstungsmaterialien
├── tags/ModTags.java                 ← eigene Item-Tags
├── food/ModFoodProperties.java       ← Nahrungswerte
├── creativemodetab/ModCreativeTabs.java
└── entity/
    ├── ModEntities.java              ← Entity-Registrierung
    ├── custom/
    │   ├── UlyssanDragonEntity.java  ← der Drache (KI, Fliegen, Bossbar …)
    │   └── UlyssanDragonPart.java    ← eine Hitbox des Drachen
    └── client/                       ← läuft nur beim Spieler, nicht auf dem Server
        ├── ModClientEvents.java      ← Renderer registrieren, Schläge umleiten
        ├── UlyssanDragonRenderer.java
        └── UlyssanBossBar.java       ← eigene Bossbar-Textur
```

```
src/main/resources/
├── META-INF/mods.toml                ← Mod-Beschreibung für Forge
├── pack.mcmeta
├── assets/rubymod/                   ← alles, was man SIEHT oder HÖRT (Client)
│   ├── blockstates/  items/  models/block/  models/item/
│   ├── textures/block/  textures/item/  textures/entity/  textures/gui/
│   ├── equipment/                    ← wie Rüstung am Körper aussieht
│   ├── geckolib/models/  geckolib/animations/
│   └── lang/en_us.json  lang/de_de.json
└── data/                             ← alles, was das Spiel REGELT (Server)
    ├── rubymod/recipe/  loot_table/  tags/item/  worldgen/  forge/biome_modifier/
    └── minecraft/tags/               ← Einträge in Vanilla-Tags (Abbau-Werkzeuge usw.)
```

**Faustregel:** `assets` ist für den Client (Aussehen), `data` ist für den Server (Regeln). Ein Server ohne Grafik braucht `assets` nicht, ein Spieler ohne `data` könnte keine Rezepte benutzen.
