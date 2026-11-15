# 11 – Fehler und Lösungen

Alle Fehler, die beim Bau der Mod aufgetreten sind – mit Ursache und Lösung. Ganz unten stehen die **Punkte, die im aktuellen Stand noch offen sind**.

## Wie man Fehler liest

- **In IntelliJ:** Rot unterstrichener Code. Maus drüber → Erklärung. **Alt + Enter** → Lösungsvorschläge (z. B. fehlenden Import hinzufügen).
- **Beim Bauen:** im Build-Fenster unten. Wichtig ist immer die **erste** rote Zeile und der Teil unter *„What went wrong:“*. Format: `Datei.java:Zeile: Fehler: Beschreibung`.
- **Im Spiel:** im Log (`run/logs/latest.log`) oder im Run-Fenster. Bei einem Absturz zusätzlich `run/crash-reports/`. Wichtig ist die erste Zeile mit `Caused by:` und die erste Zeile darunter mit `com.darkwixxard`.
- **Datenfehler** (Tags, Rezepte, Loot-Tables): Minecraft startet trotzdem, schreibt aber `ERROR … Couldn't load tag …` ins Log. Diese Zeilen nach dem Start einmal suchen (Strg+F im Run-Fenster nach `rubymod`).

## 1. Einrichtung und Gradle

| Fehler | Ursache | Lösung |
|---|---|---|
| `Unknown command-line option '-b'` | IntelliJ startet `build.gradle` „von außen“ mit `-b`. Die Option gibt es in Gradle 9 nicht mehr. | Projekt richtig als Gradle-Projekt öffnen (Ordner mit `build.gradle` öffnen, *Load Gradle Project*). Über das Gradle-Fenster oder das Terminal starten, nicht über den Pfeil neben `build.gradle`. Alte Run-Configs mit `-b` löschen. IntelliJ aktualisieren. |
| `Die Benennung "gradlew.bat" wurde nicht … erkannt` | PowerShell führt Programme im aktuellen Ordner nur mit `.\` aus. | `.\gradlew.bat build` |
| `Dependency requires at least JVM runtime version 25` | Gradle läuft mit altem Java. | *Settings → Build Tools → Gradle → Gradle JVM* auf JDK 25. |
| `Unsupported class file major version 69` | Irgendwo ein falsches Java. | wie oben, auch *Project Structure → SDK* prüfen. |
| Netzwerk-/Download-Fehler beim ersten Sync | Download abgebrochen | Einfach noch einmal *Reload All Gradle Projects*. |

## 2. Java-Code

| Fehler | Ursache | Lösung |
|---|---|---|
| `Cannot resolve symbol ModBlocks` (o. ä.) | Klasse liegt in einem anderen Paket, der `import` fehlt oder ist alt. | Alt + Enter → *Import class*. Oder `import com.darkwixxard.rubymod.block.ModBlocks;` von Hand. |
| Nach dem Verschieben ist alles rot | Dateien im Explorer verschoben → `package`-Zeilen und `import`s stimmen nicht mehr. | In IntelliJ per Drag & Drop mit **Refactor** verschieben. Nachträglich: `package`-Zeile oben in jeder Datei an den Ordner anpassen und Imports neu setzen. |
| `RubyMod.java` „zerfetzt“ nach Umbenennen von `registry` → `materials` | Die `import`-Zeilen zeigen noch auf das alte Paket. | Imports auf `…rubymod.materials.…` ändern. Für die Zukunft: Umbenennen mit **Shift + F6** (Refactor → Rename). |
| `Cannot resolve method 'registerListeners'` | Methode in `ModCreativeTabs` auskommentiert/gelöscht, aber in `RubyMod` noch aufgerufen. | Beides zusammen ein- oder auskommentieren. |
| Code aus 1.16.5 kompiliert nicht | Fast alle Namen haben sich geändert. | Idee übernehmen, Code neu schreiben. Vorher die Vanilla-Klasse anschauen (Strg + N). Siehe [01](01-Einrichtung-und-Grundlagen.md#7-unterschiede-zu-forge-1165). |
| `createAnimatableInstanceCache` → „rename reference“ | Alter Name aus GeckoLib 4. | GeckoLib 5: `GeckoLibUtil.createInstanceCache(this)` |
| `cannot find symbol: class UlyssanDragonRenderer` | Die Klasse gab es noch gar nicht. | Datei `UlyssanDragonRenderer.java` in `entity/client/` anlegen. |
| `@SubscribeEvent` macht nichts | Falscher Import: `com.google.common.eventbus.Subscribe`. | `net.minecraftforge.eventbus.api.listener.SubscribeEvent` |
| `UlyssanDragonEntity kann nicht in UlyssanDragonPart konvertiert werden` bei `super(parent)` | `extends PartEntity<UlyssanDragonPart>` – in den spitzen Klammern steht der falsche Typ. | `extends PartEntity<UlyssanDragonEntity>` (der Parent ist der Drache). |
| `Methode überschreibt oder implementiert keine Methode aus einem Supertyp` | Tippfehler im Methodennamen oder falscher Parametertyp, z. B. `getDimentions`. | Exakt `getDimensions(Pose pose)`. Tipp: **Strg + O** (Override Methods) fügt die richtige Signatur ein. |
| `canUse` in `registerGoals` rot | `);` direkt hinter `new WaterAvoidingRandomStrollGoal(this, 0.8)`, dann hängt `canUse` lose in der Methode. | Anonyme Klasse: `new …(this, 0.8) { @Override public boolean canUse() {…} });` |
| `Unable to find animation: 'animation.ulyssan_dragon.idle_calm'` | Name im Code ≠ Name in der `.animation.json`. | Namen exakt angleichen (in der JSON nachschauen). |

## 3. Ressourcen und Aussehen

| Problem | Ursache | Lösung |
|---|---|---|
| Item im Inventar lila-schwarz, Block in der Welt richtig | Datei in `assets/rubymod/items/` fehlt. | `items/<name>.json` mit Verweis aufs Modell anlegen. |
| Stiefel lila-schwarz | `"model": "rubymod:item/noxium_boots.json"` – die Endung ist zu viel. | `"rubymod:item/noxium_boots"` |
| Partikel beim Zerbrechen ohne Textur | `"particle"` im Modell zeigt auf ein Bild, das es nicht gibt. | auf `"rubymod:item/armor/noxium_armor"` setzen |
| GeckoLib-Rüstung unsichtbar | Datei hieß `CustomArmor.geo.json`, GeckoLib sucht `noxium_armor.geo.json`. | Dateinamen exakt so, klein geschrieben. |
| Keine Knochen in Blockbench | Projekt nicht als *Armor* angelegt, oder GeckoLib-Plugin veraltet / Blockbench nicht neu gestartet. | Plugin aktualisieren, Blockbench neu starten, *File → Project… → Model Type: Armor*. |
| Feld „Position“ in Blockbench zeigt komische Werte | Zeigt nur die untere hintere linke Ecke des **ersten** markierten Würfels. | Mit dem Verschieben-Werkzeug an den Pfeilen ziehen statt Zahlen tippen, Modus **Global**. |
| Hose verschoben, Beine wandern beim Laufen | Pivots der `armor…Leg/Boot`-Knochen ≠ Pivots der `biped…Leg`-Knochen. In der Datei ist X außerdem gespiegelt. | Pivots angleichen: in der Datei `[±2, 12, 0]`. Siehe [04](04-Ruestung.md#in-der-exportierten-datei-ist-x-gespiegelt). |
| Stiefel zu tief / zu hoch | Würfel nicht passend zur Fußsohle verschoben. | Sohle bis y = −1, Rest darüber. |
| Rechtes Bein 1 Pixel zu weit außen | Beim Verschieben verrutscht. | korrigierte `.geo.json` |
| Änderung an Java-Code wirkt nicht | Java wird nur beim Neustart geladen. | Spiel neu starten. F3 + T lädt nur Texturen/Modelle neu. |

## 4. Welt und Spielverhalten

| Problem | Ursache | Lösung |
|---|---|---|
| Noxium-Erz nie gefunden, Rubin schon | Alte Welt: Erze entstehen nur in neu erzeugten Chunks. Oder zu hoch gesucht. | Neue Welt, auf Y −16 bis −30 suchen. Test: `/fill ~-10 -60 ~-10 ~10 0 ~10 minecraft:air replace minecraft:deepslate` |
| Noxium droppt mit jeder Spitzhacke | Nur in `mineable/pickaxe.json`, aber in keinem Stufen-Tag. | `needs_diamond_tool.json` + `incorrect_for_diamond_tool.json` (siehe [03](03-Erze-und-Weltgenerierung.md#5-mit-welchem-werkzeug-man-ein-erz-abbauen-darf)) – **und die Dateinamen müssen stimmen**, siehe offene Punkte. |
| Drop-Menge ändern | – | `set_count` in der Loot-Table, vor `apply_bonus`. |
| Haltbarkeits-Multiplikator „geht nicht auf 40“ | Es gibt keine Grenze. Meist alter Gegenstand oder Spiel nicht neu gestartet. | Neues Teil aus dem Kreativ-Tab, F3 + H für Haltbarkeit. |
| Drache verschwindet nach einer Weile | Normales Monster-Despawn (> 32 Blöcke). | `removeWhenFarAway` → `false` ([10](10-Drache-Boss.md#1-despawn-verhindern)) |
| Drache am Kopf nicht schlagbar | Forge 26.3 findet Teile beim Schlag-Paket nicht. | Schlag im Client auf den Parent umleiten ([07](07-Drache-Hitboxen.md#5-der-forge-fehler-schläge-auf-teile-kommen-nicht-an)) |
| `MAX_HEALTH 3000` wird zu 1024 | Hartes Maximum in Minecraft. | Schaden in `hurtServer` skalieren ([10](10-Drache-Boss.md#4-warum-maximal-1024-leben)) |
| Bossbar bewegt sich nicht bei Schaden | `setProgress` stand im Abheben-`if`. | Direkt unter `super.customServerAiStep(level);` |
| Über der Bossbar steht der rohe Schlüssel | Tippfehler `uyssan` im Schlüssel, passte nicht zur Sprachdatei. | Schlüssel in Code und Sprachdateien angleichen. |
| Absturz beim Abbrechen des Bossbar-Events | Forge-Fehler (NullPointerException in `BossHealthOverlay`). | Nicht abbrechen, sondern eigene HUD-Ebene darüber zeichnen ([10](10-Drache-Boss.md#3-eigene-bossbar-textur)). |

---

## 5. ⚠️ Offene Punkte im aktuellen Stand

Diese Dinge sind mir beim Durchsehen deines aktuellen Repos (Commit „Fix the Progressbar“) aufgefallen:

### 5.1 Noxium lässt sich mit jeder Spitzhacke abbauen

Die beiden Tag-Dateien liegen an falscher Stelle bzw. sind falsch benannt. Minecraft ignoriert sie ohne Fehlermeldung.

| jetzt | richtig |
|---|---|
| `data/minecraft/tags/block/need_diamond_tool.json` | `data/minecraft/tags/block/needs_diamond_tool.json` (mit **s**) |
| `data/minecraft/tags/block/mineable/incorrect_for_diamond_tool.json` | `data/minecraft/tags/block/incorrect_for_diamond_tool.json` (ohne `mineable/`) |

### 5.2 Tag-Fehler beim Serverstart

Im Log stehen beim Start diese zwei Fehler:

```
Couldn't load tag rubymod:bogged_tool_materials as it is missing following references: minecraft:vines
Couldn't load tag rubymod:repairs_noxium_armor as it is missing following references: rubymod:noxium_tool_materials
```

- `bogged_tool_materials.json`: Das Item heißt `minecraft:vine` (Einzahl), nicht `vines`. Weil ein Eintrag fehlt, lädt der **ganze Tag** nicht → Sumpf-Werkzeuge lassen sich am Amboss nicht reparieren (und spätere Rezepte mit diesem Tag würden nicht funktionieren).
- `repairs_noxium_armor.json`: `rubymod:noxium_tool_materials` ist ein **Tag**, kein Item. Es fehlt das `#`: `"#rubymod:noxium_tool_materials"`. Bis dahin lässt sich die Noxium-Rüstung nicht reparieren.

### 5.3 Bogen-Tag

`data/minecraft/tags/item/bow.json` gibt es in Minecraft nicht, und darin steht `rubymod:noxium_axe`. Richtig: `data/minecraft/tags/item/enchantable/bow.json` mit `"rubymod:noxium_bow"`.

### 5.4 Fehlende Werkzeuge und Rüstung in Vanilla-Tags

In `swords.json`, `pickaxes.json`, `axes.json`, `shovels.json`, `hoes.json`, `head_armor.json` usw. stehen nur die Rubin-Sachen. Eis-, Sumpf- und Noxium-Werkzeuge sowie die Noxium-Rüstung fehlen.

### 5.5 Doppelte Attribut-Registrierung in `RubyMod.java`

```java
@SubscribeEvent
public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) { ... }
```

Diese Methode ist überflüssig. Die Attribute werden schon über `EntityAttributeCreationEvent.BUS.addListener(RubyMod::registerAttributes)` angemeldet. `@SubscribeEvent` allein bewirkt hier nichts, weil `RubyMod` kein `@Mod.EventBusSubscriber` hat. → Löschen.

### 5.6 Haupt-Hitbox auskommentiert

In `ModEntities` ist `.sized(5.0F, 4.0F)` auskommentiert. Damit ist der Drache für Minecraft nur 0.6 × 1.8 Blöcke groß. → Wieder einkommentieren, am besten mit `.sized(3.0F, 3.0F)`.

### 5.7 Reihenfolge im Animations-Controller

`isAggressive()` steht vor `isFlying()`. Ein fliegender Drache mit Ziel zeigt dann die Renn-Animation in der Luft. → Flug-Block vor den Kampf-Block setzen.

### 5.8 `KNOCKBACK_RESISTANCE 10.0`

Maximum ist 1.0. Wird stillschweigend auf 1 gesetzt. → Ehrlich `1.0` hinschreiben.

### 5.9 Name des Drachen fehlt in den Sprachdateien

`"entity.rubymod.ulyssan_dragon": "Ulyssan"` in `en_us.json` und `de_de.json` ergänzen (für Todesnachrichten, Namensschild-Anzeige usw.).

### 5.10 Kleinigkeiten

- `ModFoodProperties.RUBY_APPLE` wird nicht benutzt (die Werte stehen direkt in `ModItems`). Entweder benutzen (`p.food(ModFoodProperties.RUBY_APPLE)`) oder löschen.
- `ModEntities` importiert sich selbst (`import com.darkwixxard.rubymod.entity.ModEntities;`). Harmlos, kann weg.
- In `mods.toml` steht in der Beschreibung noch nur „Adds rubies, the Block of Ruby and the Ruby Apple“.
