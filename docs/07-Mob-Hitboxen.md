# 07 – Mehrteilige Hitboxen (wie beim Enderdrachen)

## 1. Das Problem

Jede Entity hat **eine** rechteckige Hitbox (aus `sized(...)`). Ein Mob mit langem Hals, langem Schwanz und großen Flügeln passt aber nicht in einen Kasten:
- Kasten groß → man trifft den Mob auch dort, wo **Luft** ist.
- Kasten klein → Kopf und Schwanz sind **nicht** treffbar.

Der Enderdrache benutzt deshalb **mehrere kleine Hitboxen**, die sich mit ihm bewegen. Forge stellt dafür die Klasse **`PartEntity`** bereit.

**Wann lohnt sich das?** Nur bei großen Mobs mit langen Körperteilen. Für normale Mobs reicht `sized(...)`.

## 2. Ein Teil: `WyvernPart.java`

```java
public class WyvernPart extends PartEntity<WyvernEntity> {
    private final EntityDimensions size;
    private final float damageMultiplier;

    public WyvernPart(WyvernEntity parent, float width, float height, float damageMultiplier) {
        super(parent);
        this.size = EntityDimensions.scalable(width, height);
        this.damageMultiplier = damageMultiplier;
        this.refreshDimensions();
    }

    public float getDamageMultiplier() {
        return this.damageMultiplier;
    }

    // Schaden an den Mob weitergeben (z. B. Kopf = mehr Schaden)
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return this.getParent().hurtServer(level, source, amount * this.damageMultiplier);
    }

    @Override
    public boolean isPickable() { return true; }          // kann angeklickt/getroffen werden

    @Override
    public boolean is(Entity entity) {                     // zählt als der Mob selbst
        return this == entity || this.getParent() == entity;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) { return this.size; }

    @Override
    public boolean shouldBeSaved() { return false; }        // Teile werden nie einzeln gespeichert

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {}
    @Override protected void readAdditionalSaveData(ValueInput input) {}
    @Override protected void addAdditionalSaveData(ValueOutput output) {}
}
```

- **`PartEntity<WyvernEntity>`**: In den spitzen Klammern steht, **zu wem** der Teil gehört (der Parent). Steht dort aus Versehen die Teil-Klasse selbst, meldet `super(parent)`: *„… kann nicht in … konvertiert werden“*.
- **`EntityDimensions.scalable(breite, höhe)`**: Größe dieser Hitbox. `refreshDimensions()` wendet sie an.
- **`hurtServer`**: Der Teil hat kein eigenes Leben. Er gibt den Schaden **× Multiplikator** an den Parent weiter.
- **`getDimensions`**: Minecraft fragt hier nach der Größe. Ein Tippfehler im Namen (z. B. `getDimentions`) führt zu *„Methode überschreibt … keine Methode aus einem Supertyp“* – dafür ist `@Override` da.
- Die drei leeren Methoden am Ende sind in `Entity` abstrakt und **müssen** existieren.

## 3. Teile im Mob anlegen

```java
// Extra-Hitboxen (Breite, Höhe, Schadensmultiplikator)
private final WyvernPart head      = new WyvernPart(this, 2.0F, 1.7F, 1.5F);
private final WyvernPart neck      = new WyvernPart(this, 1.0F, 1.2F, 1.0F);
private final WyvernPart tail1     = new WyvernPart(this, 1.1F, 1.2F, 1.0F);
private final WyvernPart tail2     = new WyvernPart(this, 0.9F, 0.9F, 1.0F);
private final WyvernPart wingLeft  = new WyvernPart(this, 3.0F, 2.0F, 0.5F);
private final WyvernPart wingRight = new WyvernPart(this, 3.0F, 2.0F, 0.5F);
private final WyvernPart[] parts = {head, neck, tail1, tail2, wingLeft, wingRight};
```

Der Multiplikator macht Treffer interessanter: Kopf 1.5 (Schwachstelle), Flügel 0.5 (gepanzert).

### Forge mitteilen, dass es Teile gibt

```java
@Override
public boolean isMultipartEntity() { return true; }

@Override
public PartEntity<?>[] getParts() { return this.parts; }

// Jeder Teil braucht eine eigene ID direkt nach der des Mobs (wie beim Enderdrachen)
@Override
public void setId(int id) {
    super.setId(id);
    for (int i = 0; i < this.parts.length; i++) {
        this.parts[i].setId(id + i + 1);
    }
}
```

Jede Entity hat eine **Nummer** (ID), über die Server und Client sich verständigen („Entity 1234 wurde getroffen“). Beide Seiten müssen dieselben Nummern für die Teile benutzen → Parent-ID + 1, + 2, + 3 …

## 4. Teile jeden Tick mitbewegen

```java
@Override
public void aiStep() {
    super.aiStep();
    //             Teil         vorne  seitlich  hoch   (Blöcke, ab den Füßen)
    this.placePart(this.head,       2.1,  0.0, 2.9);
    this.placePart(this.neck,       1.1,  0.0, 2.3);
    this.placePart(this.tail1,     -2.9,  0.0, 1.2);
    this.placePart(this.tail2,     -4.2,  0.0, 0.9);
    this.placePart(this.wingLeft,  -1.8,  3.2, 1.2);
    this.placePart(this.wingRight, -1.8, -3.2, 1.2);
}

private void placePart(WyvernPart part, double forward, double side, double up) {
    double yaw = Math.toRadians(this.yBodyRot);
    double forwardX = -Math.sin(yaw);   // Blickrichtung des Körpers
    double forwardZ = Math.cos(yaw);
    double sideX = forwardZ;            // 90° dazu = links vom Mob
    double sideZ = -forwardX;

    double oldX = part.getX(), oldY = part.getY(), oldZ = part.getZ();
    part.setPos(this.getX() + forwardX * forward + sideX * side,
            this.getY() + up,
            this.getZ() + forwardZ * forward + sideZ * side);
    // alte Position merken, damit sich die Hitbox flüssig bewegt
    part.xo = oldX;  part.yo = oldY;  part.zo = oldZ;
    part.xOld = oldX; part.yOld = oldY; part.zOld = oldZ;
}
```

### Die Mathematik

- `aiStep()` läuft **jeden Tick** auf Server **und** Client → Hitboxen stimmen auf beiden Seiten.
- `yBodyRot` = Drehung des Körpers in Grad (0 = Süden, 90 = Westen, 180 = Norden, 270 = Osten). `Math.toRadians` rechnet in Bogenmaß, das `sin`/`cos` brauchen.
- **Vorwärts-Vektor** `(-sin(yaw), cos(yaw))`: die Blickrichtung als X- und Z-Anteil. Bei yaw = 0: (0, 1) = Süden.
- **Seitwärts-Vektor** steht senkrecht dazu. Positiv = links vom Mob.
- Position = Mob + vorwärts × `forward` + seitwärts × `side` + nach oben `up`.
- `xo/yo/zo`, `xOld/…` = Position des letzten Ticks. Minecraft rechnet zwischen alt und neu weich hin und her → flüssige Bewegung.

## 5. Die Werte für dein Modell finden

1. In Blockbench die Knochen (Kopf, Hals, Schwanzsegmente, Flügel) anklicken und ihre **Pivot**-Position ablesen. Blockbench-Einheiten sind Pixel: **16 Pixel = 1 Block**.
2. Im Modell ist **−Z vorne** (die Seite, in die der Mob schaut). `forward` = −Z / 16.
3. Die Skalierung aus dem Renderer (`withScale`) mit einrechnen: bei 0.5 alle Werte halbieren.
4. Die Größe der Teile aus der Größe der Würfel des Körperteils abschätzen.
5. Im Spiel mit **F3 + B** prüfen und nachjustieren. Das geht am schnellsten mit `/summon … {NoAI:1b}`.

**Einschränkung:** Die Teile folgen nur der **Drehung** des Mobs, nicht den Animationen. Senkt er in einer Animation den Kopf, bleibt die Kopf-Box oben. Beim Enderdrachen ist das genauso.

## 6. Forge-Eigenheit: Nahkampf-Schläge auf Teile kommen nicht an

> Stand **Forge 26.3-66.1.0**. In späteren Versionen kann das behoben sein – dann ist Abschnitt 6 überflüssig. Testen: Schlag auf den Kopf → passiert etwas?

### Was passiert

1. Das Fadenkreuz zeigt auf die Kopf-Hitbox. Der Client schickt: *„Ich schlage Entity Nr. 1235“* (ID des Kopfes).
2. Der Server sucht mit `ServerLevel.getEntityOrPart(1235)`.
3. Diese Methode schaut nur in der Liste der **Enderdrachen**-Teile nach. Forge führt Mod-Teile in einer eigenen Liste (`partEntities`), die hier nicht abgefragt wird.
4. Nichts gefunden → der Schlag verschwindet.

**Pfeile** treffen Teile trotzdem, weil die Kollision direkt auf dem Server berechnet wird.

### Umgehung 1: Schlag auf den Parent umleiten (Client)

```java
// in ModClientEvents (Dist.CLIENT)
// Forge findet Mod-Teile auf dem Server nicht, wenn der Spieler sie schlägt.
// Deshalb greifen wir beim Schlag auf einen Teil stattdessen den Mob selbst an.
@SubscribeEvent
public static boolean onAttack(InputEvent.InteractionKeyMappingTriggered.Attack event) {
    Minecraft mc = Minecraft.getInstance();
    if (mc.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof WyvernPart part
            && mc.player != null && mc.gameMode != null) {
        mc.gameMode.attack(mc.player, part.getParent());
        return true;   // true = normalen Angriff abbrechen (der würde den Teil treffen)
    }
    return false;
}
```

- Das Event feuert bei der Angriffstaste.
- `mc.hitResult` = worauf das Fadenkreuz zeigt.
- Teil erkannt → selbst einen Angriff auf den **Parent** schicken und `true` zurückgeben, damit der normale (verlorene) Angriff abgebrochen wird. Der Arm schwingt trotzdem.

Bei mehreren Mobs mit Teilen: statt `instanceof WyvernPart` auf `instanceof PartEntity<?> part` prüfen – dann funktioniert es für alle.

### Umgehung 2: Multiplikator auf dem Server wiederherstellen

Jetzt wird der Parent getroffen, der Multiplikator des Teils fehlt. Der Server prüft deshalb, **wohin der Spieler geschaut hat**:

```java
@Override
public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
    if (source.getDirectEntity() instanceof Player player) {
        WyvernPart part = this.getPartLookedAt(player);
        if (part != null) {
            amount *= part.getDamageMultiplier();
        }
    }
    return super.hurtServer(level, source, amount);
}

// Unsichtbarer Strahl aus den Augen des Spielers: gibt den nächsten getroffenen Teil zurück
// (null = der Körper selbst ist näher, oder kein Teil wurde getroffen)
private WyvernPart getPartLookedAt(Player player) {
    Vec3 eye = player.getEyePosition();
    Vec3 end = eye.add(player.getViewVector(1.0F).scale(8.0));
    double bestDistance = this.getBoundingBox().clip(eye, end)
            .map(eye::distanceToSqr).orElse(Double.MAX_VALUE);
    WyvernPart best = null;
    for (WyvernPart part : this.parts) {
        var hit = part.getBoundingBox().clip(eye, end);
        if (hit.isPresent() && eye.distanceToSqr(hit.get()) < bestDistance) {
            bestDistance = eye.distanceToSqr(hit.get());
            best = part;
        }
    }
    return best;
}
```

- `getDirectEntity()` = **wer direkt** getroffen hat: beim Schwert der Spieler, beim Pfeil der Pfeil. Pfeile treffen die Teile ja direkt (dort wirkt schon `WyvernPart.hurtServer`) → nichts wird doppelt gerechnet.
- `getViewVector(1.0F).scale(8.0)` = 8 Blöcke in Blickrichtung.
- `AABB.clip(start, ende)` = Schnittpunkt der Linie mit dem Kasten, als `Optional` (vielleicht gibt es keinen).
- Der **nächste** Treffer gewinnt. Ist die Haupt-Hitbox näher, bleibt es bei × 1.
- `distanceToSqr` = Abstand zum Quadrat. Spart die Wurzel und reicht zum Vergleichen.

### Ablauf eines Schwerthiebs auf den Kopf

```
Spieler klickt auf Kopf-Hitbox
  → Client (onAttack): "das ist ein Teil" → greift stattdessen den Mob an
    → Server: WyvernEntity.hurtServer
      → getPartLookedAt: Spieler schaut auf den Kopf → × 1.5
        → super.hurtServer: Leben wird abgezogen
```

## 7. Die Haupt-Hitbox

`sized(...)` in `ModEntities` sollte nur den **Rumpf** abdecken. Ist sie zu groß (reicht z. B. über den Kopf), trifft man von vorne zuerst die große Box statt den Kopf. Fehlt `sized` ganz, ist der Mob nur 0.6 × 1.8 groß – der Körper ist kaum treffbar und er passt durch Gänge, durch die er optisch nicht passt.

## Checkliste

- [ ] Teil-Klasse `extends PartEntity<DeinMob>`
- [ ] Teile als Felder + Array
- [ ] `isMultipartEntity`, `getParts`, `setId`
- [ ] `aiStep` mit `placePart`
- [ ] Werte mit F3 + B geprüft
- [ ] Forge 26.3: Angriffs-Umleitung im Client + Multiplikator im Server
- [ ] Haupt-Hitbox nur Rumpf

## Zum Ausprobieren

- Gib dem Schwanz Multiplikator 0 – dann ist er „unverwundbar“.
- Lass die Flügel-Hitboxen nur existieren, wenn der Mob fliegt (Größe 0, wenn nicht).
- Lies `EnderDragon.aiStep()` und vergleiche, wie Mojang die Teile bewegt.
