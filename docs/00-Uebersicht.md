# Minecraft-Modding mit Forge – Leitfaden

Dieser Leitfaden erklärt Schritt für Schritt, wie man eine Mod für **Minecraft Forge** baut: vom Projekt-Setup über Blöcke, Items, Erze und Rüstung bis zu einem fliegenden Boss-Mob mit Animationen und eigener Bossbar.

Er ist so geschrieben, dass du ihn **für jede eigene Mod** benutzen kannst. Jedes Kapitel erklärt:
- **was** man braucht (Java-Code und Dateien),
- **warum** es so funktioniert,
- welche **Fehler** typischerweise passieren,
- und am Ende eine **Checkliste** und Ideen **zum Ausprobieren**.

> **Stand:** Minecraft **26.3**, Forge **26.3-66.1.0**, GeckoLib **5.5.7**, Java **25**. In neueren Versionen können sich Namen ändern. Das Vorgehen bleibt meist gleich – im Zweifel im Minecraft-Code nachschauen (siehe [12](12-Werkzeuge-und-Quellen.md)).

## Inhaltsverzeichnis

| Kapitel | Thema |
|---|---|
| [01 – Einrichtung und Grundlagen](01-Einrichtung-und-Grundlagen.md) | Java, IntelliJ, Gradle, Starten und Debuggen, Events, Projektaufbau, Unterschiede zu alten Versionen |
| [02 – Blöcke, Items und Werkzeuge](02-Bloecke-Items-Werkzeuge.md) | Registrierung, Blöcke, Items, Werkzeug-Materialien, Essen, Kreativ-Tabs, Modelle, Texturen, Rezepte, Loot-Tables, Sprachdateien |
| [03 – Erze und Weltgenerierung](03-Erze-und-Weltgenerierung.md) | Erze in der Welt verteilen, Abbau-Stufen über Tags, Drops |
| [04 – Rüstung](04-Ruestung.md) | Normale Rüstung, 3D-Rüstung mit GeckoLib, Blockbench, Set-Effekte |
| [05 – Eigene Item-Klassen](05-Eigene-Item-Klassen.md) | Vanilla-Verhalten überschreiben am Beispiel eines Bogens mit drei Pfeilen |
| [06 – Eigener Mob mit GeckoLib](06-Eigener-Mob-mit-GeckoLib.md) | Entity registrieren, Attribute, KI-Goals, Renderer, Modell- und Animationsdateien |
| [07 – Mehrteilige Hitboxen](07-Mob-Hitboxen.md) | Hitboxen wie beim Enderdrachen, Forge-Eigenheiten beim Schlagen |
| [08 – Mob-Animationen](08-Mob-Animationen.md) | Controller, Dauer- und Einmal-Animationen, Todesanimation |
| [09 – Fliegende Mobs](09-Mob-Fliegen.md) | Zwischen Laufen und Fliegen wechseln, synchronisierte Daten, eigenes Goal, Speichern |
| [10 – Boss-Funktionen](10-Boss-Funktionen.md) | Despawn verhindern, Bossbar, eigene Bossbar-Textur, Lebens-Grenze |
| [11 – Fehler und Lösungen](11-Fehler-und-Loesungen.md) | Fehlermeldungen lesen, typische Fehler mit Ursache und Lösung |
| [12 – Werkzeuge und Quellen](12-Werkzeuge-und-Quellen.md) | IntelliJ-Tricks, Blockbench, Lern-Webseiten, Java-Begriffe |

**Empfohlene Reihenfolge:** 01 → 02 → 03 → 04. Danach je nach Interesse 05 (eigene Items) oder 06–10 (Mobs, die bauen aufeinander auf). 11 und 12 sind zum Nachschlagen.

## Platzhalter in diesem Leitfaden

Alle Beispiele benutzen eine erfundene Mod. Ersetze die Namen durch deine eigenen:

| Platzhalter | Bedeutung | in deiner Mod z. B. |
|---|---|---|
| `mymod` | Mod-ID (nur Kleinbuchstaben, Zahlen, `_`) | `magicmod` |
| `MyMod` / `MyMod.MODID` | Hauptklasse der Mod / ihre ID-Konstante | `MagicMod` |
| `com.example.mymod` | Java-Paket | `com.deinname.magicmod` |
| `sapphire`, `sapphire_ore`, `sapphire_block` | Beispiel-Material | `ruby`, `mithril` … |
| `crystal_armor` / `CrystalArmorItem` | Beispiel für 3D-Rüstung | – |
| `triple_bow` / `TripleShotBowItem` | Beispiel für eine eigene Item-Klasse | – |
| `wyvern` / `WyvernEntity` | Beispiel-Mob (fliegender Boss) | – |

**Regeln für IDs:** Mod-ID und alle Registry-Namen (`sapphire_ore`) nur **klein**, keine Leerzeichen, keine Umlaute. Dasselbe gilt für alle Dateinamen in `resources`.

## Aufbau einer typischen Mod

```
src/main/java/com/example/mymod/
├── MyMod.java                       ← Einstiegspunkt der Mod
├── block/
│   ├── ModBlocks.java               ← alle Blöcke registrieren
│   └── custom/                      ← Blöcke mit eigenem Verhalten
├── item/
│   ├── ModItems.java                ← alle Items registrieren
│   ├── ModCreativeTabs.java
│   └── custom/                      ← Items mit eigenem Verhalten (TripleShotBowItem …)
├── materials/ModMaterials.java      ← Werkzeug- und Rüstungsmaterialien
├── tags/ModTags.java                ← eigene Tags
└── entity/
    ├── ModEntities.java             ← Mobs registrieren
    ├── custom/                      ← Mob-Klassen (WyvernEntity …)
    └── client/                      ← nur beim Spieler: Renderer, HUD
```

```
src/main/resources/
├── META-INF/mods.toml               ← Mod-Beschreibung für Forge
├── pack.mcmeta
├── assets/mymod/                    ← alles, was man SIEHT oder HÖRT (Client)
│   ├── blockstates/  items/  models/block/  models/item/
│   ├── textures/block/  textures/item/  textures/entity/  textures/gui/
│   ├── equipment/                   ← wie Rüstung am Körper aussieht
│   ├── geckolib/models/  geckolib/animations/
│   └── lang/en_us.json  lang/de_de.json
└── data/                            ← alles, was das Spiel REGELT (Server)
    ├── mymod/recipe/  loot_table/  tags/  worldgen/  forge/biome_modifier/
    └── minecraft/tags/              ← Einträge in Vanilla-Tags
```

**Faustregel:** `assets` = Aussehen (Client), `data` = Regeln (Server). Ein Server ohne Grafik braucht `assets` nicht, ein Spiel ohne `data` hätte keine Rezepte.

## Client und Server

Minecraft läuft **immer** in zwei Hälften, auch im Einzelspieler:

| | Server | Client |
|---|---|---|
| Aufgabe | entscheidet alles: Leben, Schaden, KI, Positionen, Drops | zeigt an: Modelle, Animationen, HUD, Sounds; nimmt Eingaben an |
| Typische Methoden | `hurtServer`, `customServerAiStep`, `doHurtTarget` | Renderer, `EntityRenderersEvent`, HUD-Events |
| Erkennen im Code | `!level.isClientSide()` | `level.isClientSide()` |

Code, der Grafik oder Tastatur benutzt, darf **nur** auf dem Client laufen (Klassen mit `@Mod.EventBusSubscriber(value = Dist.CLIENT)`), sonst stürzt ein Server ab.
