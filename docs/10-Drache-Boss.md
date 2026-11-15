# 10 – Der Drache: Boss-Funktionen

## 1. Despawn verhindern

### Warum verschwand der Drache?

Der Drache ist ein `Monster`, deshalb behandelt Minecraft ihn wie einen Zombie. Die Regeln stehen in `Mob.checkDespawn()`:

| Situation | Was passiert |
|---|---|
| Spieler weiter als **128 Blöcke** entfernt | sofort gelöscht |
| Spieler weiter als **32 Blöcke** entfernt und der Mob hatte 30 Sekunden nichts zu tun | zufällig gelöscht (im Schnitt nach etwa 40 Sekunden) |
| Schwierigkeit **Friedlich** | alle Monster sofort gelöscht |

Seit er fliegen kann, ist er schnell mal mehr als 32 Blöcke weg.

### Lösung

```java
// Ein Boss soll nie verschwinden, nur weil der Spieler weit weg ist
@Override
public boolean removeWhenFarAway(double distanceSquared) {
    return false;
}
```

Minecraft fragt diese Methode bei jedem Despawn-Check. `false` = bleibt für immer und wird mit der Welt gespeichert. Wither und Enderdrache machen es genauso.

- Alternative für einen einzelnen Drachen: `/summon rubymod:ulyssan_dragon ~ ~ ~ {PersistenceRequired:1b}`.
- Auf **Friedlich** wird er trotzdem gelöscht. Wer das nicht will: `.allowedInPeaceful()` am `EntityType.Builder` in `ModEntities`. Bei Bossen lässt man das normalerweise weg.

## 2. Die Bossbar

Minecraft hat dafür die Klasse `ServerBossEvent` (benutzt auch der Wither).

```java
// Übersetzungs-Schlüssel des Bossbar-Namens. Der Client erkennt daran UNSERE Bar und zeichnet die eigene Textur
public static final String BOSS_BAR_NAME = "bossbar.rubymod.ulyssan_dragon";

private final ServerBossEvent bossEvent = new ServerBossEvent(
        Mth.createInsecureUUID(this.random),
        Component.translatable(BOSS_BAR_NAME),
        BossEvent.BossBarColor.BLUE,
        BossEvent.BossBarOverlay.PROGRESS);
```

| Parameter | Bedeutung |
|---|---|
| `Mth.createInsecureUUID(random)` | eine zufällige, eindeutige ID für diese Bar |
| `Component.translatable(...)` | der Name, aus den Sprachdateien übersetzt |
| `BossBarColor` | `PINK`, `BLUE`, `RED`, `GREEN`, `YELLOW`, `PURPLE`, `WHITE` |
| `BossBarOverlay` | `PROGRESS` (durchgehend), `NOTCHED_6`, `NOTCHED_10`, `NOTCHED_12`, `NOTCHED_20` (mit Strichen) |

Sprachdateien:

```json
"bossbar.rubymod.ulyssan_dragon": "Ulyssan, the Frost Dragon"      // en_us.json
"bossbar.rubymod.ulyssan_dragon": "Ulyssan, der Frostdrache"       // de_de.json
```

### Anzeigen und ausblenden

```java
// Spieler kommt nah genug, um den Drachen zu sehen → Bar anzeigen
@Override
public void startSeenByPlayer(ServerPlayer player) {
    super.startSeenByPlayer(player);
    this.bossEvent.addPlayer(player);
}

// Spieler ist zu weit weg → Bar ausblenden
@Override
public void stopSeenByPlayer(ServerPlayer player) {
    super.stopSeenByPlayer(player);
    this.bossEvent.removePlayer(player);
}
```

Minecraft ruft beide Methoden **automatisch** auf, wenn ein Spieler in Sichtweite kommt bzw. geht (die Reichweite kommt von `clientTrackingRange`).

### Lebensanzeige aktualisieren

In `customServerAiStep`, **direkt** unter `super.customServerAiStep(level);`:

```java
this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
```

`setProgress` erwartet 0.0 (leer) bis 1.0 (voll). Jeden Tick aufzurufen kostet praktisch nichts: `ServerBossEvent` schickt nur dann ein Paket, wenn sich der Wert wirklich ändert.

> **Fehler, der passiert ist:** Die Zeile stand zuerst **innerhalb** des `if` zum Abheben. Dann wurde die Bar nur im Moment des Abhebens aktualisiert, und Schaden am Boden war nicht zu sehen.

### Extras (im Konstruktor)

```java
this.bossEvent.setDarkenScreen(true);      // Himmel dunkler, wie beim Wither
this.bossEvent.setCreateWorldFog(true);    // Nebel, wie beim Enderdrachen
this.bossEvent.setPlayBossMusic(true);     // Boss-Musik (Enderdrache)
```

## 3. Eigene Bossbar-Textur

### Die Textur: `assets/rubymod/textures/gui/ulyssan_boss_bar.png` (256 × 32)

```
(0,0)  ┌─────────────── Rahmen 206 × 15 ───────────────┐
       │ ❄ ═══[ Bar-Fenster bei x 12–193, y 5–9 ]═══ ❄ │   ← Eis-Kristalle an den Enden, Eiszapfen unten
       └────────────────────────────────────────────────┘
(0,16) [ Lebensanzeige 182 × 5 – hellblauer Eis-Verlauf ]
```

- Das **Bar-Fenster** im Rahmen muss **undurchsichtig** sein, sonst scheint die Vanilla-Bar durch.
- 182 × 5 ist genau die Größe der Vanilla-Bar.
- Größerer Rahmen → `FRAME_WIDTH`, `FRAME_HEIGHT` und die Verschiebung `- 12` / `- 5` im Code anpassen.

### Warum es nicht einfach geht

Forge hat ein Event `CustomizeGuiOverlayEvent.BossEventProgress`, das feuert, **bevor** Minecraft eine Bossbar zeichnet. Eigentlich würde man es abbrechen und selbst zeichnen. In Forge 26.3 steht in `BossHealthOverlay` aber:

```java
var progressEvent = ForgeHooksClient.onCustomizeBossEventProgress(...);   // null, wenn abgebrochen
if (progressEvent != null) { ... zeichnen ... }
yOffset += progressEvent.getIncrement();                                  // ← NullPointerException!
```

Abbrechen = **Absturz**. Deshalb drei Schritte:

1. Wenn Minecraft eine Bossbar zeichnen will, **merken** wir uns ihre Position – aber nur, wenn es unsere ist.
2. Minecraft zeichnet ganz normal die Vanilla-Bar.
3. Direkt danach läuft **unsere eigene HUD-Ebene** und malt Rahmen und Lebensanzeige **darüber**. Weil das Bar-Fenster undurchsichtig ist, verschwindet die Vanilla-Bar darunter.

### Der Code: `entity/client/UlyssanBossBar.java`

```java
@Mod.EventBusSubscriber(modid = RubyMod.MODID, value = Dist.CLIENT)
public final class UlyssanBossBar {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(RubyMod.MODID, "textures/gui/ulyssan_boss_bar.png");
    private static final Identifier LAYER = Identifier.fromNamespaceAndPath(RubyMod.MODID, "ulyssan_boss_bar");

    // Aufbau der Textur (das png ist 256 x 32 Pixel)
    private static final int TEX_WIDTH = 256, TEX_HEIGHT = 32;
    private static final int FRAME_WIDTH = 206, FRAME_HEIGHT = 15;   // Rahmen bei (0, 0)
    private static final int FILL_V = 16;                              // Lebensanzeige bei (0, 16)
    private static final int BAR_WIDTH = 182, BAR_HEIGHT = 5;         // so groß wie die Vanilla-Bar

    // Wo unsere Bars in diesem Frame auf dem Bildschirm sind
    private record Bar(int x, int y, float progress) {}
    private static final List<Bar> BARS = new ArrayList<>();

    // 1) Minecraft will gleich eine Bossbar zeichnen: wenn es unsere ist, Position merken
    @SubscribeEvent
    public static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        LerpingBossEvent boss = event.getBossEvent();
        if (boss.getName().getContents() instanceof TranslatableContents name
                && name.getKey().equals(UlyssanDragonEntity.BOSS_BAR_NAME)) {
            BARS.add(new Bar(event.getX(), event.getY(), boss.getProgress()));
            event.setIncrement(event.getIncrement() + 6);   // etwas mehr Platz für die Eiszapfen
        }
    }

    // 2) Eigene HUD-Ebene direkt ÜBER den Vanilla-Bossbars anmelden, damit sie darüber gezeichnet wird
    @SubscribeEvent
    public static void addLayer(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().addAbove(ForgeLayeredDraw.PRE_SLEEP_STACK, LAYER, ForgeLayeredDraw.BOSS_OVERLAY,
                (graphics, deltaTracker) -> drawBars(graphics));
    }

    // 3) Rahmen + Lebensanzeige über jede gemerkte Bar zeichnen
    private static void drawBars(GuiGraphicsExtractor graphics) {
        for (Bar bar : BARS) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, bar.x() - 12, bar.y() - 5,
                    0, 0, FRAME_WIDTH, FRAME_HEIGHT, TEX_WIDTH, TEX_HEIGHT);
            int width = Mth.lerpDiscrete(bar.progress(), 0, BAR_WIDTH);
            if (width > 0) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, bar.x(), bar.y(),
                        0, FILL_V, width, BAR_HEIGHT, TEX_WIDTH, TEX_HEIGHT);
            }
        }
        BARS.clear();
    }
}
```

### Erklärung

- **Unsere Bar erkennen:** Der Client kennt die UUID der Bar nicht, aber den Namen. Weil der Name ein `translatable` ist, steckt darin der **Schlüssel** (`bossbar.rubymod.ulyssan_dragon`). `getContents() instanceof TranslatableContents name` prüft das und holt ihn heraus. Deshalb muss der Name `translatable` sein, nicht `literal`.
- **`setIncrement`:** Wie weit die nächste Bossbar darunter beginnt. +6 Pixel Platz für die Eiszapfen.
- **HUD-Ebenen (Layer):** Forge zeichnet das HUD in einer festen Reihenfolge von Ebenen (Fadenkreuz, Hotbar, Effekte, Bossbars …). `addAbove(stack, neueEbene, andereEbene, zeichenFunktion)` fügt eine eigene Ebene direkt **nach** den Bossbars ein. Was später gezeichnet wird, liegt oben.
- **`blit(pipeline, textur, x, y, u, v, breite, höhe, texBreite, texHöhe)`:** Kopiert ein Rechteck aus der Textur auf den Bildschirm. `u, v` = wo im Bild das Rechteck beginnt.
- **`Mth.lerpDiscrete(progress, 0, 182)`:** rechnet 0.0–1.0 in 0–182 Pixel um.
- **`LerpingBossEvent.getProgress()`:** Der Client-Wert gleitet weich zum neuen Wert, statt zu springen.
- **`BARS.clear()`:** Jede Frame fängt neu an. Ist die Oberfläche ausgeblendet (F1), wird nichts gemerkt und nichts gezeichnet.

## 4. Warum maximal 1024 Leben?

In Minecrafts `Attributes.java`:

```java
public static final Holder<Attribute> MAX_HEALTH = register(
    "max_health", new RangedAttribute("attribute.name.max_health", 20.0, 1.0, 1024.0)...);
```

Die drei Zahlen sind Standardwert (20 = 10 Herzen), Minimum (1) und **Maximum (1024)**. `RangedAttribute` setzt jeden Wert mit `Mth.clamp(wert, min, max)` in diesen Bereich – 3000 wird stillschweigend zu 1024. Zum Vergleich: Wither 300, Enderdrache 200.

Ändern ginge nur mit einem **Mixin** (Eingriff direkt in Minecrafts Code) – für den Anfang zu kompliziert und anfällig für Konflikte mit anderen Mods.

### Der Trick: weniger Schaden nehmen

Für den Spieler ist es egal, ob der Drache 3000 Leben hat oder 1024 und jeden Treffer nur zu einem Drittel nimmt – es braucht gleich viele Schläge.

```java
@Override
public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
    // MAX_HEALTH darf höchstens 1024 sein. Damit sich der Drache wie ~3000 Leben anfühlt,
    // nimmt er nur etwa ein Drittel des Schadens
    amount *= 1024.0F / 3000.0F;
    ...
}
```

und ehrlich `MAX_HEALTH 1024.0`. Die Bossbar stimmt trotzdem, weil sie `getHealth() / getMaxHealth()` rechnet.

- `/kill` wirkt trotzdem sofort (umgeht den Schaden).
- `/damage` wird auch gedrittelt → zum Testen mehr eingeben.
- Alternative wäre das Attribut `ARMOR` (max. 30). Rüstung wirkt aber je nach Waffe unterschiedlich und wird von manchen Schadensarten ignoriert. Der feste Faktor ist vorhersehbarer.
