# 10 – Boss-Funktionen

Was einen Mob zum **Boss** macht: Er verschwindet nicht von selbst, hat eine Bossbar oben am Bildschirm und hält viel aus.

## 1. Despawn verhindern

### Warum verschwinden Mobs?

Für Mobs der Kategorie `MONSTER` gelten diese Regeln (`Mob.checkDespawn()`):

| Situation | Was passiert |
|---|---|
| nächster Spieler weiter als **128 Blöcke** | sofort gelöscht |
| Spieler weiter als **32 Blöcke** und der Mob hatte 30 s nichts zu tun | zufällig gelöscht (im Schnitt nach ~40 s) |
| Schwierigkeit **Friedlich** | gelöscht, **wenn** der EntityType mit `.notInPeaceful()` gebaut wurde (alle Vanilla-Monster) |

Fliegende oder weit herumlaufende Mobs sind schnell über 32 Blöcke weg.

### Lösung

```java
// Ein Boss soll nie verschwinden, nur weil der Spieler weit weg ist
@Override
public boolean removeWhenFarAway(double distanceSquared) {
    return false;
}
```

Minecraft fragt diese Methode bei jedem Despawn-Check. `false` = bleibt und wird mit der Welt gespeichert (Wither und Enderdrache machen es genauso).

- Nur für einzelne Mobs: `/summon mymod:wyvern ~ ~ ~ {PersistenceRequired:1b}` oder ein Namensschild.
- **Friedlich:** Ein eigener EntityType ist standardmäßig auch auf Friedlich erlaubt. Soll er dort wie Vanilla-Monster verschwinden, `.notInPeaceful()` an den `EntityType.Builder` hängen.

## 2. Die Bossbar

Minecraft hat dafür `ServerBossEvent` (vom Wither benutzt).

```java
// Übersetzungs-Schlüssel des Bossbar-Namens (auch für die eigene Textur in Abschnitt 3)
public static final String BOSS_BAR_NAME = "bossbar.mymod.wyvern";

private final ServerBossEvent bossEvent = new ServerBossEvent(
        Mth.createInsecureUUID(this.random),
        Component.translatable(BOSS_BAR_NAME),
        BossEvent.BossBarColor.BLUE,
        BossEvent.BossBarOverlay.PROGRESS);
```

| Parameter | Bedeutung |
|---|---|
| `Mth.createInsecureUUID(random)` | zufällige, eindeutige ID für diese Bar |
| `Component.translatable(...)` | Name aus den Sprachdateien (`Component.literal("Text")` = fester Text) |
| `BossBarColor` | `PINK`, `BLUE`, `RED`, `GREEN`, `YELLOW`, `PURPLE`, `WHITE` |
| `BossBarOverlay` | `PROGRESS` (durchgehend), `NOTCHED_6`, `NOTCHED_10`, `NOTCHED_12`, `NOTCHED_20` (mit Strichen) |

```json
"bossbar.mymod.wyvern": "Wyvern, the Sky Terror"      // en_us.json
"bossbar.mymod.wyvern": "Wyvern, Schrecken der Lüfte" // de_de.json
```

Schlüssel im Code und in den Sprachdateien müssen **exakt** gleich sein, sonst steht der rohe Schlüssel über der Bar.

### Anzeigen und ausblenden

```java
// Spieler kommt nah genug, um den Mob zu sehen → Bar anzeigen
@Override
public void startSeenByPlayer(ServerPlayer player) {
    super.startSeenByPlayer(player);
    this.bossEvent.addPlayer(player);
}

// Spieler zu weit weg → Bar ausblenden
@Override
public void stopSeenByPlayer(ServerPlayer player) {
    super.stopSeenByPlayer(player);
    this.bossEvent.removePlayer(player);
}
```

Minecraft ruft beide **automatisch** auf (Reichweite = `clientTrackingRange`).

### Lebensanzeige aktualisieren

In `customServerAiStep`, **direkt** unter `super.customServerAiStep(level);` – nicht in einem `if`:

```java
this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
```

`setProgress` erwartet 0.0 (leer) bis 1.0 (voll). Jeden Tick aufrufen kostet nichts: Es wird nur dann ein Paket geschickt, wenn sich der Wert ändert.

Steht die Zeile versehentlich in einem `if` (z. B. dem fürs Abheben), aktualisiert sich die Bar nur in genau diesem Moment.

### Extras (im Konstruktor)

```java
this.bossEvent.setDarkenScreen(true);      // Himmel dunkler (Wither)
this.bossEvent.setCreateWorldFog(true);    // Nebel (Enderdrache)
this.bossEvent.setPlayBossMusic(true);     // Boss-Musik
```

Mit eigenem Namen (Namensschild) soll sich auch die Bar ändern:

```java
@Override
public void setCustomName(@Nullable Component name) {
    super.setCustomName(name);
    this.bossEvent.setName(this.getDisplayName());
}
```

## 3. Eigene Bossbar-Textur

### Die Textur

`assets/mymod/textures/gui/wyvern_boss_bar.png`, z. B. 256 × 32:

```
(0,0)  ┌─────────────── Rahmen 206 × 15 ───────────────┐
       │ ◆ ═══[ Bar-Fenster bei x 12–193, y 5–9 ]═══ ◆ │
       └────────────────────────────────────────────────┘
(0,16) [ Lebensanzeige 182 × 5 ]
```

- 182 × 5 = Größe der Vanilla-Bar.
- Das **Bar-Fenster** im Rahmen muss **undurchsichtig** sein (siehe unten, warum).
- Andere Maße → die Konstanten im Code anpassen.

### Die Forge-Eigenheit

> Stand **Forge 26.3-66.1.0**.

Forge hat das Event `CustomizeGuiOverlayEvent.BossEventProgress`, das feuert, **bevor** eine Bossbar gezeichnet wird. Naheliegend: abbrechen und selbst zeichnen. In `BossHealthOverlay` steht aber:

```java
var progressEvent = ForgeHooksClient.onCustomizeBossEventProgress(...);   // null, wenn abgebrochen
if (progressEvent != null) { ... zeichnen ... }
yOffset += progressEvent.getIncrement();                                  // ← NullPointerException!
```

Abbrechen = **Absturz**. Deshalb drei Schritte:
1. Beim Event die Position **merken** (nur für unsere Bar) – nicht abbrechen.
2. Minecraft zeichnet die Vanilla-Bar.
3. Eine **eigene HUD-Ebene** direkt danach malt Rahmen und Füllung **darüber**. Das undurchsichtige Bar-Fenster verdeckt die Vanilla-Bar.

### Der Code (Client)

```java
@Mod.EventBusSubscriber(modid = MyMod.MODID, value = Dist.CLIENT)
public final class WyvernBossBar {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(MyMod.MODID, "textures/gui/wyvern_boss_bar.png");
    private static final Identifier LAYER = Identifier.fromNamespaceAndPath(MyMod.MODID, "wyvern_boss_bar");

    // Aufbau der Textur
    private static final int TEX_WIDTH = 256, TEX_HEIGHT = 32;
    private static final int FRAME_WIDTH = 206, FRAME_HEIGHT = 15;   // Rahmen bei (0, 0)
    private static final int FRAME_OFFSET_X = 12, FRAME_OFFSET_Y = 5; // wo im Rahmen das Bar-Fenster liegt
    private static final int FILL_V = 16;                              // Füllung bei (0, 16)
    private static final int BAR_WIDTH = 182, BAR_HEIGHT = 5;

    // Wo unsere Bars in diesem Frame auf dem Bildschirm sind
    private record Bar(int x, int y, float progress) {}
    private static final List<Bar> BARS = new ArrayList<>();

    private WyvernBossBar() {}

    // 1) Minecraft will gleich eine Bossbar zeichnen: wenn es unsere ist, Position merken
    @SubscribeEvent
    public static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        LerpingBossEvent boss = event.getBossEvent();
        if (boss.getName().getContents() instanceof TranslatableContents name
                && name.getKey().equals(WyvernEntity.BOSS_BAR_NAME)) {
            BARS.add(new Bar(event.getX(), event.getY(), boss.getProgress()));
            event.setIncrement(event.getIncrement() + 6);   // etwas mehr Abstand zur nächsten Bar
        }
    }

    // 2) Eigene HUD-Ebene direkt ÜBER den Vanilla-Bossbars anmelden
    @SubscribeEvent
    public static void addLayer(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().addAbove(ForgeLayeredDraw.PRE_SLEEP_STACK, LAYER, ForgeLayeredDraw.BOSS_OVERLAY,
                (graphics, deltaTracker) -> drawBars(graphics));
    }

    // 3) Rahmen + Füllung über jede gemerkte Bar zeichnen
    private static void drawBars(GuiGraphicsExtractor graphics) {
        for (Bar bar : BARS) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                    bar.x() - FRAME_OFFSET_X, bar.y() - FRAME_OFFSET_Y,
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

- **Unsere Bar erkennen:** Der Client kennt die UUID nicht, aber den Namen. Ist der Name `translatable`, steckt der **Schlüssel** darin. `getContents() instanceof TranslatableContents name` prüft das und holt ihn. Deshalb muss der Name `translatable` sein, nicht `literal`.
- **`setIncrement`:** Abstand zur nächsten Bossbar darunter.
- **HUD-Ebenen:** Forge zeichnet das HUD in fester Reihenfolge (Fadenkreuz, Hotbar, Effekte, Bossbars, Chat …). `addAbove(stapel, neueEbene, andereEbene, zeichnen)` fügt eine Ebene direkt **nach** den Bossbars ein. Später gezeichnet = liegt oben.
- **`blit(pipeline, textur, x, y, u, v, breite, höhe, texBreite, texHöhe)`:** kopiert ein Rechteck aus der Textur auf den Bildschirm. `u, v` = Startpunkt im Bild.
- **`Mth.lerpDiscrete(progress, 0, 182)`:** 0.0–1.0 → 0–182 Pixel.
- **`LerpingBossEvent`:** Der Client-Wert gleitet weich zum neuen Wert.
- **`BARS.clear()`:** jeder Frame fängt neu an. Bei ausgeblendetem HUD (F1) wird nichts gemerkt und nichts gezeichnet.

Dasselbe Muster (Event merken + eigene Ebene) funktioniert für jede HUD-Erweiterung.

## 4. Warum maximal 1024 Leben?

In Minecrafts `Attributes.java`:

```java
public static final Holder<Attribute> MAX_HEALTH = register(
    "max_health", new RangedAttribute("attribute.name.max_health", 20.0, 1.0, 1024.0)...);
```

Standardwert 20 (10 Herzen), Minimum 1, **Maximum 1024**. `RangedAttribute` setzt jeden Wert mit `Mth.clamp(wert, min, max)` in den Bereich – 3000 wird stillschweigend 1024. Vergleich: Wither 300, Enderdrache 200, Warden 500.

Ändern ginge nur mit einem **Mixin** (Eingriff in Minecrafts Code) – kompliziert und anfällig für Konflikte mit anderen Mods.

### Der Trick: weniger Schaden nehmen

Für den Spieler ist es egal, ob der Boss 3000 Leben hat oder 1024 und jeden Treffer nur zu einem Drittel nimmt – gleich viele Schläge.

```java
private static final float VIRTUAL_HEALTH = 3000.0F;

@Override
public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
    // MAX_HEALTH darf höchstens 1024 sein. Damit sich der Boss wie VIRTUAL_HEALTH anfühlt,
    // nimmt er nur einen Teil des Schadens
    amount *= 1024.0F / VIRTUAL_HEALTH;
    return super.hurtServer(level, source, amount);
}
```

…und in `createAttributes` ehrlich `MAX_HEALTH 1024.0`. Die Bossbar stimmt weiter, weil sie `getHealth() / getMaxHealth()` rechnet.

- `/kill` wirkt trotzdem sofort.
- `/damage` wird mit verringert → zum Testen mehr eingeben.
- Alternative: `ARMOR` (max. 30). Rüstung wirkt aber je nach Waffe unterschiedlich und wird von manchen Schadensarten ignoriert. Der feste Faktor ist vorhersehbarer.

## Checkliste: Boss

- [ ] `removeWhenFarAway` → `false`
- [ ] `ServerBossEvent` mit `translatable`-Namen + Sprachdateien
- [ ] `startSeenByPlayer` / `stopSeenByPlayer`
- [ ] `setProgress` jeden Tick in `customServerAiStep`
- [ ] Leben ≤ 1024, ggf. Schadens-Faktor
- [ ] optional: eigene Bossbar-Textur, Musik, Nebel
- [ ] optional: Loot-Table mit besonderer Beute (`data/mymod/loot_table/entities/wyvern.json`)

## Zum Ausprobieren

- Ändere die Farbe der Bar auf Rot, sobald der Boss unter 30 % Leben hat (`bossEvent.setColor(...)`).
- Lass den Boss bei 50 % Leben einmalig Verstärkung rufen (z. B. drei Phantome spawnen).
- Lies `WitherBoss` – wie macht der Wither seine Unverwundbarkeits-Phase am Anfang?
