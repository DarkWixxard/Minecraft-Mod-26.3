# 11 – Fehler und Lösungen

## 1. Fehler lesen – wo steht was?

| Wo | Was du siehst | Was wichtig ist |
|---|---|---|
| **IntelliJ-Editor** | rot unterstrichener Code | Maus drüber = Erklärung. **Alt + Enter** = Lösungsvorschläge (z. B. Import hinzufügen). |
| **Build-Fenster** | `Datei.java:Zeile: Fehler: Beschreibung` | immer die **erste** rote Zeile und *„What went wrong:“* |
| **Spiel-Log** (`run/logs/latest.log`, Run-Fenster) | `ERROR`/`WARN`-Zeilen | nach deiner Mod-ID suchen (Strg + F) |
| **Absturz** (`run/crash-reports/`) | langer Stacktrace | erste Zeile mit `Caused by:` und die erste Zeile darunter mit **deinem Paket** (`com.example.mymod`) – dort ist der Fehler |
| **Datenfehler** (Tags, Rezepte, Loot) | Spiel startet trotzdem, Log: `Couldn't load tag …`, `Couldn't parse …` | nach jedem Start einmal das Log nach der Mod-ID durchsuchen |

**Stille Fehler:** Falsch benannte Ordner oder Dateien (z. B. `recipes/` statt `recipe/`) werden **ohne jede Meldung** ignoriert. Wenn etwas „einfach nicht da“ ist: Pfad und Namen Buchstabe für Buchstabe prüfen.

## 2. Einrichtung und Gradle

| Fehler | Ursache | Lösung |
|---|---|---|
| `Unknown command-line option '-b'` | IntelliJ startet `build.gradle` „von außen“ mit `-b`; gibt es in Gradle 9 nicht mehr. | Ordner mit `build.gradle` richtig öffnen (*Load Gradle Project*), über Gradle-Fenster/Terminal starten, Run-Configs mit `-b` löschen, IntelliJ aktualisieren. |
| `Die Benennung "gradlew.bat" wurde nicht … erkannt` | PowerShell braucht `.\` | `.\gradlew.bat build` |
| `Dependency requires at least JVM runtime version 25` | Gradle läuft mit altem Java. | *Settings → Build Tools → Gradle → Gradle JVM* auf JDK 25. |
| `Unsupported class file major version …` | irgendwo falsches Java | wie oben + *Project Structure → SDK* |
| `Could not resolve …` / Download-Fehler | Netzwerk, fehlendes `repositories`-Repo | Repository prüfen, *Reload All Gradle Projects* |

## 3. Java-Code

| Fehler | Ursache | Lösung |
|---|---|---|
| `Cannot resolve symbol X` | Import fehlt oder zeigt aufs falsche Paket. | Alt + Enter → *Import class*. |
| Nach dem Verschieben ist alles rot | Dateien im Explorer verschoben → `package`/`import` falsch. | In IntelliJ mit **Refactor** verschieben (Drag & Drop). Nachträglich: `package`-Zeile und Imports anpassen. |
| Viele Imports kaputt nach Umbenennen eines Pakets | Umbenannt ohne Refactor. | Imports anpassen; künftig **Shift + F6**. |
| `Cannot resolve method 'xyz'` | Methode gelöscht/auskommentiert, Aufruf noch da (oder umgekehrt). | Beides zusammen ändern. |
| Code aus Tutorial kompiliert nicht | andere Minecraft-Version | Idee übernehmen, Code neu schreiben; Vanilla-Klasse lesen ([01](01-Einrichtung-und-Grundlagen.md#9-code-aus-alten-tutorials-z-b-1165)). |
| Methode „gibt es nicht“ (z. B. `createAnimatableInstanceCache`) | alter Name einer Bibliothek | Doku/Changelog der Bibliothek; Strg + Klick auf die Klasse und Methoden ansehen. GeckoLib 5: `createInstanceCache`. |
| `cannot find symbol: class XyzRenderer` | Klasse existiert noch nicht / falsch geschrieben | Datei anlegen, Namen prüfen. |
| `@SubscribeEvent` macht nichts | falscher Import (`com.google.common.eventbus.Subscribe`) oder Klasse ohne `@Mod.EventBusSubscriber`, Methode nicht `static` | `net.minecraftforge.eventbus.api.listener.SubscribeEvent`, Klasse annotieren, `static` |
| `X kann nicht in Y konvertiert werden` bei `super(...)` | falscher Typ in spitzen Klammern (Generics), z. B. `PartEntity<Teil>` statt `PartEntity<Mob>` | Generic-Typ prüfen |
| `Methode überschreibt oder implementiert keine Methode aus einem Supertyp` | Tippfehler im Namen oder falsche Parameter | **Strg + O** und die Methode einfügen lassen |
| `';' expected`, `class, interface … expected` | Klammern/Semikolon falsch (oft bei anonymen Klassen) | Klammern zählen; IntelliJ markiert die Gegenklammer, wenn der Cursor daneben steht |
| `Missing return statement` | Methode mit Rückgabetyp gibt nicht in jedem Fall etwas zurück | `return` am Ende ergänzen |
| Code nach `return` wird nie ausgeführt | `return` beendet die Methode | Ergebnis in Variable speichern, danach weitermachen, am Ende `return` |
| `NullPointerException` mit `RegistryObject.get()` | `.get()` zu früh (z. B. in `static`-Feld, bevor registriert) | erst in Lambdas/Methoden aufrufen, die später laufen |

## 4. Ressourcen und Aussehen

| Problem | Ursache | Lösung |
|---|---|---|
| Lila-schwarzes Muster | Modell oder Textur nicht gefunden | Pfade prüfen, **keine Dateiendung** in Pfaden, Kleinschreibung, Log nach `Unable to load model` / `Missing texture` durchsuchen |
| Item lila-schwarz, Block in der Welt richtig | `assets/<modid>/items/<name>.json` fehlt | anlegen |
| Name erscheint als `item.mymod.xyz` | Eintrag in `lang` fehlt oder Kommafehler macht die Datei kaputt | Eintrag ergänzen, JSON prüfen (z. B. mit einem JSON-Validator) |
| Partikel beim Abbauen/Zerbrechen ohne Textur | `"particle"` im Modell zeigt auf fehlendes Bild | auf existierende Textur setzen |
| GeckoLib-Modell unsichtbar | Dateiname passt nicht zum Namen im Code | alle drei Dateien gleich benennen, klein |
| `Unable to find animation: '…'` | Animationsname im Code ≠ in der JSON | Namen aus der JSON kopieren |
| GeckoLib-Rüstung verschoben, wandert beim Laufen | Pivots der `armor…`-Knochen ≠ `biped…`-Knochen (in der Datei ist X gespiegelt) | Pivots angleichen ([04](04-Ruestung.md#in-der-exportierten-datei-ist-x-gespiegelt)) |
| Keine Knochen in Blockbench | Projekt nicht als *Armor*, Plugin alt, Blockbench nicht neu gestartet | Plugin aktualisieren, neu starten, *File → Project… → Model Type: Armor* |
| Feld „Position“ in Blockbench verwirrend | zeigt nur Ecke des **ersten** markierten Würfels | mit dem Verschieben-Werkzeug ziehen, Modus **Global** |
| Änderung wirkt nicht | Java-Code wird nur beim Neustart geladen | Neustart; F3 + T nur für Texturen/Modelle, `/reload` für Daten |

## 5. Daten (Tags, Rezepte, Loot, Welt)

| Problem | Ursache | Lösung |
|---|---|---|
| Rezept fehlt | falscher Ordner (`recipes/`), Tippfehler in einer ID, JSON-Fehler | Log prüfen; Ordner `recipe/` |
| `Couldn't load tag … missing following references: …` | ein Eintrag existiert nicht (z. B. `minecraft:vines` statt `minecraft:vine`) → **ganzer Tag** lädt nicht | ID korrigieren |
| Tag im Tag ohne `#` | `"mymod:my_tag"` statt `"#mymod:my_tag"` wird als **Item** gesucht | `#` davor |
| Vanilla-Tag wirkt nicht | Dateiname ist kein echter Vanilla-Tag (z. B. `tags/item/bow.json` statt `tags/item/enchantable/bow.json`) | Namen in der Minecraft-Bibliothek nachschauen |
| Vanilla-Items verlieren Eigenschaften | `"replace": true` in einem Vanilla-Tag | `"replace": false` |
| Block droppt nichts | Loot-Table fehlt oder falscher Pfad | `data/<modid>/loot_table/blocks/<name>.json` |
| Block mit jeder Spitzhacke abbaubar | kein Stufen-Tag, oder Tag-Datei falsch benannt/abgelegt (`need_…` statt `needs_…`, Unterordner `mineable/`) | [03](03-Erze-und-Weltgenerierung.md#5-mit-welchem-werkzeug-man-abbauen-darf) |
| Werkzeug baut eigenes Erz nicht ab | Material-Stufe zu niedrig | `INCORRECT_FOR_…` passend wählen |
| Erz nicht zu finden | alte Chunks, falsche Höhe, schwer sichtbar | neue Welt, F3, `/fill … replace` ([03](03-Erze-und-Weltgenerierung.md#6-testen)) |

## 6. Mobs und Spielverhalten

| Problem | Ursache | Lösung |
|---|---|---|
| Absturz beim Spawnen | Attribute nicht angemeldet / Renderer nicht registriert | [06](06-Eigener-Mob-mit-GeckoLib.md#4-attribute-anmelden) |
| Mob winzig treffbar / läuft durch enge Gänge | `sized(...)` fehlt | Haupt-Hitbox setzen |
| Attributwert wirkt nicht | über Maximum (z. B. `KNOCKBACK_RESISTANCE` > 1, `MAX_HEALTH` > 1024) → still gekappt | Grenzen in `Attributes` nachschauen |
| Mob greift nicht an | im Kreativ-Modus getestet | Survival |
| Mob verschwindet | normales Despawn | `removeWhenFarAway` → `false` ([10](10-Boss-Funktionen.md#1-despawn-verhindern)) |
| Nahkampf auf Teil-Hitbox wirkungslos | Forge 26.3 findet Mod-Teile beim Schlag nicht | Umleitung im Client ([07](07-Mob-Hitboxen.md#6-forge-eigenheit-nahkampf-schläge-auf-teile-kommen-nicht-an)) |
| Animation passt nicht zum Zustand | Reihenfolge der `if`s im Controller | Tod → Sonderzustände → Kampf → normal |
| Client-Animation kennt einen Server-Wert nicht | normales Feld statt `SynchedEntityData` | [09](09-Mob-Fliegen.md#2-synchronisierte-daten) |
| Zustand nach Neuladen weg | nicht gespeichert | `addAdditionalSaveData` / `readAdditionalSaveData` |
| Bossbar bewegt sich nicht | `setProgress` nicht jeden Tick (z. B. in einem `if`) | direkt unter `super.customServerAiStep(level)` |
| Absturz beim Abbrechen des Bossbar-Events | Forge-Eigenheit (NullPointerException) | nicht abbrechen, eigene HUD-Ebene ([10](10-Boss-Funktionen.md#3-eigene-bossbar-textur)) |
| Welt startet nicht: *„Failed to load registries due to errors“* | fehlerhafte Datei in einer Welt-Registry (Verzauberung, Biom, Dimension …), oft ein falscher Tag | Zeile darunter im Log nennt Datei und Grund ([21](21-Verzauberungen.md#3-die-item-tags-müssen-stimmen)) |
| `/mymod` → *„Unknown or incomplete command“* | Ast ohne `executes` oder Klasse nicht angemeldet | [13](13-Befehle.md) |
| Taste erscheint nicht in den Optionen | nicht über `RegisterKeyMappingsEvent` registriert oder Klasse ohne `Dist.CLIENT` | [18](18-Tasten-und-Netzwerk.md) |
| Aktion per Taste wirkt nur kurz / springt zurück | nur auf dem Client ausgeführt | Paket an den Server schicken ([18](18-Tasten-und-Netzwerk.md)) |
| Konfigurationswert „noch nicht geladen“ | `.get()` zu früh (in `static`-Feld oder bei der Registrierung) | erst zur Laufzeit lesen ([19](19-Konfiguration.md)) |
| Kein Ton | Name in `sounds.json` passt nicht, Datei kein `.ogg`, Log: *„Unable to play unknown soundEvent“* | [17](17-Sounds.md) |

## 7. Allgemeine Strategie bei Fehlern

1. **Die erste Fehlermeldung lesen**, nicht die letzte. Folgefehler verschwinden oft mit dem ersten.
2. **Was hat sich zuletzt geändert?** Mit Git: `git diff` zeigt alle Änderungen seit dem letzten Commit.
3. **Klein testen:** Neue Funktion erst minimal einbauen, starten, dann erweitern.
4. **Log-Ausgaben** einbauen: `MyMod.LOGGER.info("Wert: {}", wert);` – läuft der Code überhaupt? Welche Werte kommen an?
5. **Debugger** mit Breakpoint ([01](01-Einrichtung-und-Grundlagen.md#über-eine-run-configuration-mit-debugger)).
6. **Vanilla vergleichen:** Wie macht Mojang dasselbe?
7. **Oft committen** – dann kannst du jederzeit zum letzten funktionierenden Stand zurück.

## 8. Lehren aus der Entstehung dieses Leitfadens

Beim Schreiben dieses Leitfadens sind selbst Fehler passiert. Erst durch Kompilieren und Testen auf einem Server sind sie aufgefallen. Genau diese Fehler passieren beim Modding ständig, deshalb stehen sie hier.

| Was schiefging | Wie es auffiel | Lehre |
|---|---|---|
| Ein Verzauberungs-Tag `#minecraft:enchantable/sword` wurde „aus dem Gedächtnis“ benutzt – den gibt es nicht (richtig: `sharp_weapon` / `melee_weapon`). | Die Testwelt ließ sich nicht starten: *„Failed to load registries … Missing tag“*. | Namen von Tags, Dateien und IDs **nachschauen**, nie raten. Die echten Dateien liegen in der Minecraft-Bibliothek. |
| Forges Code-Kommentar zu `forge:add_spawns` zeigt `minCount`/`maxCount`. In 26.3 heißt es `count`. | Vergleich mit Vanillas `plains.json` und Test mit einem Debug-Befehl | Kommentare und Wikis können veraltet sein. **Der Code und die Vanilla-Dateien gelten.** |
| `level.random` und `player.hurtMarked` aus älteren Versionen benutzt | Compiler: *„random has protected access“*, *„cannot find symbol“* | Mojang benennt zwischen Versionen um. Bei „cannot find symbol“: Strg + Klick auf die Klasse und nach ähnlichen Namen suchen. |
| `org.lwjgl.glfw.GLFW` für Tasten-Codes benutzt | *„package org.lwjgl.glfw does not exist“* | Nicht jede Bibliothek, die Minecraft benutzt, steht dir beim Kompilieren zur Verfügung. Die Minecraft-eigene Klasse nehmen (`InputConstants`). |
| Behauptet, Mod-Mobs verschwinden auf „Friedlich“ automatisch. Gesagt wurde außerdem `.allowedInPeaceful()` – die Methode heißt `notInPeaceful()`, und der Standard ist umgekehrt. | Nachlesen in `EntityType.Builder` | Wie sich etwas **standardmäßig** verhält, steht im Code (Feld-Startwerte). Annahmen prüfen. |
| Trampolin-Beispiel mit einer Methode, die es in 26.3 nicht mehr gibt | Compiler: *„method does not override … supertype“* | `@Override` immer benutzen – dann sagt der Compiler sofort Bescheid. Und: im aktuellen Vanilla-Vorbild nachsehen (hier `Blocks.SLIME_BLOCK` → `.bounceRestitution(1.0F)`). |
| Ein Kommentar in der Konfiguration sprach von „Saphir“, der Code droppt Smaragde | Beim Lesen der erzeugten Datei | Kommentare beim Ändern des Codes mit ändern. |
| `displayClientMessage(text, true)` benutzt – heißt in 26.3 `sendOverlayMessage(text)` | Compiler: *„cannot find symbol“* | Siehe oben: Namen ändern sich; im Zweifel in der Klasse (`Player`) nach ähnlichen Methoden suchen. |
| Einzeiler-Lambda an ein abbrechbares Event gehängt und einen Monitor-Listener mit Priorität angemeldet | Compiler: *„reference to addListener is ambiguous“*, *„no suitable method found“* | Bei Überladungen (mehrere Methoden gleichen Namens) entscheidet der Compiler anhand der Lambda-Form. `{ … }` macht es eindeutig ([23](23-Events.md#6-reihenfolge-priorität-und-monitor)). |

### Der Ablauf, der diese Fehler gefunden hat

1. **Kompilieren** (`gradlew compileJava`) – findet falsche Namen und Signaturen.
2. **Server starten** und das Log nach `ERROR` und der Mod-ID durchsuchen – findet kaputte JSON-Dateien, fehlende Tags und Registrierungsfehler.
3. **Mit Befehlen testen** (`/setblock`, `/effect`, `/summon`, eigene Debug-Befehle) – zeigt, ob es wirklich funktioniert.
4. **Im echten Spiel** testen, was einen Spieler braucht (Tasten, Klicks, Sounds, Aussehen).

Je früher eine Stufe einen Fehler findet, desto billiger ist er. Deshalb jede Stufe ausführen, bevor man weiterbaut.
