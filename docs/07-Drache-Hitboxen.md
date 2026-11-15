# 07 – Der Drache: Hitboxen wie beim Enderdrachen

## 1. Das Problem

Jede Entity hat **eine** rechteckige Hitbox (die mit `sized(...)`). Ein Drache mit langem Hals, langem Schwanz und riesigen Flügeln passt aber nicht in einen einzigen Kasten:
- Ist der Kasten groß, trifft man den Drachen auch dort, wo **Luft** ist.
- Ist er klein, kann man Kopf und Schwanz **nicht** treffen.

Der Enderdrache löst das mit **mehreren kleinen Hitboxen**, die sich mit ihm bewegen. Forge stellt dafür die Klasse **`PartEntity`** bereit.

## 2. Ein Teil des Drachen: `UlyssanDragonPart.java`

```java
public class UlyssanDragonPart extends PartEntity<UlyssanDragonEntity> {
    private final EntityDimensions size;
    private final float damageMultiplier;

    public UlyssanDragonPart(UlyssanDragonEntity parent, float width, float height, float damageMultiplier) {
        super(parent);
        this.size = EntityDimensions.scalable(width, height);
        this.damageMultiplier = damageMultiplier;
        this.refreshDimensions();
    }

    public float getDamageMultiplier() {
        return this.damageMultiplier;
    }

    // Schaden an den Drachen weitergeben (z. B. Kopf = mehr Schaden)
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return this.getParent().hurtServer(level, source, amount * this.damageMultiplier);
    }

    @Override
    public boolean isPickable() { return true; }          // man kann ihn anklicken/treffen

    @Override
    public boolean is(Entity entity) {                     // zählt als der Drache selbst
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

### Erklärung

- **`PartEntity<UlyssanDragonEntity>`**: In den spitzen Klammern steht, **zu wem** der Teil gehört (der „Parent“). Steht dort versehentlich `<UlyssanDragonPart>`, kommt bei `super(parent)` der Fehler *„UlyssanDragonEntity kann nicht in UlyssanDragonPart konvertiert werden“*.
- **`EntityDimensions.scalable(breite, höhe)`**: die Größe dieser Hitbox. `refreshDimensions()` wendet sie sofort an.
- **`hurtServer`**: Wird der Teil getroffen, gibt er den Schaden **mal Multiplikator** an den Drachen weiter. Der Teil selbst hat kein Leben.
- **`getDimensions`**: Minecraft fragt hier nach der Größe. Ein Tippfehler im Namen (`getDimentions`) führt zu *„Methode überschreibt oder implementiert keine Methode aus einem Supertyp“* – genau dafür ist `@Override` gut: ohne die Annotation hättest du still eine nutzlose neue Methode gehabt.
- Die drei leeren Methoden am Ende **muss** jede Entity haben (sie sind in `Entity` abstrakt). Teile haben keine eigenen Daten, deshalb bleiben sie leer.

## 3. Die Teile im Drachen anlegen

```java
// Extra-Hitboxen (Breite, Höhe, Schadensmultiplikator)
private final UlyssanDragonPart head      = new UlyssanDragonPart(this, 2.0F, 1.7F, 1.5F);
private final UlyssanDragonPart neck      = new UlyssanDragonPart(this, 1.0F, 1.2F, 1.0F);
private final UlyssanDragonPart tail1     = new UlyssanDragonPart(this, 1.1F, 1.2F, 1.0F);
private final UlyssanDragonPart tail2     = new UlyssanDragonPart(this, 0.9F, 0.9F, 1.0F);
private final UlyssanDragonPart tail3     = new UlyssanDragonPart(this, 1.0F, 0.8F, 1.0F);
private final UlyssanDragonPart wingLeft  = new UlyssanDragonPart(this, 3.0F, 2.0F, 0.5F);
private final UlyssanDragonPart wingRight = new UlyssanDragonPart(this, 3.0F, 2.0F, 0.5F);
private final UlyssanDragonPart[] parts = {head, neck, tail1, tail2, tail3, wingLeft, wingRight};
```

| Teil | Multiplikator | Bedeutung |
|---|---|---|
| Kopf | 1.5 | Kopftreffer machen 50 % mehr Schaden |
| Hals, Schwanz | 1.0 | normaler Schaden |
| Flügel | 0.5 | nur halber Schaden |

### Forge mitteilen, dass es Teile gibt

```java
@Override
public boolean isMultipartEntity() { return true; }

@Override
public PartEntity<?>[] getParts() { return this.parts; }

// Jeder Teil braucht eine eigene ID direkt nach der des Drachen (wie beim Enderdrachen)
@Override
public void setId(int id) {
    super.setId(id);
    for (int i = 0; i < this.parts.length; i++) {
        this.parts[i].setId(id + i + 1);
    }
}
```

Jede Entity hat eine **Nummer** (ID), mit der Server und Client sich verständigen („Entity 1234 wurde getroffen“). Server und Client müssen dieselben Nummern für die Teile benutzen. Deshalb bekommt jeder Teil die Nummer des Drachen + 1, + 2, + 3 …

## 4. Die Teile jeden Tick mitbewegen

```java
@Override
public void aiStep() {
    super.aiStep();
    //             Teil         vorne  seitlich  hoch   (Blöcke, ab den Füßen des Drachen)
    this.placePart(this.head,       2.1,  0.0, 2.9);
    this.placePart(this.neck,       1.1,  0.0, 2.3);
    this.placePart(this.tail1,     -2.9,  0.0, 1.2);
    this.placePart(this.tail2,     -4.2,  0.0, 0.9);
    this.placePart(this.tail3,     -5.5,  0.0, 0.5);
    this.placePart(this.wingLeft,  -1.8,  3.2, 1.2);
    this.placePart(this.wingRight, -1.8, -3.2, 1.2);
}

private void placePart(UlyssanDragonPart part, double forward, double side, double up) {
    double yaw = Math.toRadians(this.yBodyRot);
    double forwardX = -Math.sin(yaw);   // Blickrichtung des Körpers
    double forwardZ = Math.cos(yaw);
    double sideX = forwardZ;            // 90° dazu = links vom Drachen
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

### Die Mathematik dahinter

- `aiStep()` läuft **jeden Tick** auf Server **und** Client. Deshalb stimmen die Hitboxen auf beiden Seiten.
- `yBodyRot` ist die Drehung des Körpers in Grad (0 = Süden, 90 = Westen …). `Math.toRadians` rechnet in Bogenmaß um, weil `sin`/`cos` das brauchen.
- `(-sin(yaw), cos(yaw))` ist der **Vorwärts-Vektor**: die Richtung, in die der Drache schaut, als X- und Z-Anteil. Bei yaw = 0 ist das (0, 1), also Süden.
- Der **Seitwärts-Vektor** steht senkrecht dazu: (Z, −X) des Vorwärts-Vektors. Positiv = links vom Drachen.
- Position eines Teils = Drache + vorwärts × `forward` + seitwärts × `side` + nach oben `up`.
- `xo/yo/zo` und `xOld/…` sind die Positionen vom letzten Tick. Minecraft rechnet zwischen alter und neuer Position weich hin und her, damit sich die Box flüssig bewegt.

### Werte anpassen

Die Zahlen wurden aus dem Modell (`Ulyssan.bbmodel`) bei `withScale(0.5F)` ausgemessen:

| Teil | vorne | seitlich | Höhe (Unterkante) | ungefähre Größe im Modell |
|---|---|---|---|---|
| Kopf | 1.9 | 0 | 3.0 | 1.3 breit, 3.5 lang, 1.6 hoch |
| Hals | 0.9–1.4 | 0 | 2.1–3.0 | 0.9 breit |
| Brust | 0.5 | 0 | 0 | 1.9 breit |
| Becken | −1.7 | 0 | 0 | 1.8 breit |
| Schwanz | −2.9 bis −6.3 | 0 bis 1.7 (biegt nach links) | 1.2 → 0.2 | 1.0 → schmaler |
| Flügel | −1.8 | ±3.2 | 1.1 | 5.5 × 5.4 |

- Drück im Spiel **F3 + B**, dann siehst du alle Hitboxen als weiße Kästen.
- Sitzt eine Box falsch, änderst du die drei Zahlen hinter dem Teil in `aiStep()`.
- Änderst du `withScale` im Renderer, musst du alle Zahlen **im selben Verhältnis** ändern (doppelte Größe → doppelte Werte).
- **Einschränkung:** Die Boxen folgen nur der **Drehung** des Drachen, nicht den Animationen. Senkt er in einer Animation den Kopf, bleibt die Kopf-Box oben. Beim Enderdrachen ist das genauso.

## 5. Der Forge-Fehler: Schläge auf Teile kommen nicht an

### Was passiert

1. Dein Fadenkreuz zeigt auf die Kopf-Hitbox. Der Client schickt: *„Ich schlage Entity Nr. 1235“* (die ID des Kopfes).
2. Der Server sucht die Entity mit `ServerLevel.getEntityOrPart(1235)`.
3. Diese Methode schaut in Forge 26.3 **nur in der Liste der Enderdrachen-Teile** nach. Forge trägt deine Teile zwar in eine eigene Liste (`partEntities`) ein, aber `getEntityOrPart` fragt diese Liste nicht ab.
4. Der Server findet nichts → der Schlag verschwindet einfach.

Pfeile treffen die Teile trotzdem, weil die Kollision direkt auf dem Server berechnet wird.

### Die Umgehung, Teil 1: Schlag auf den Drachen umleiten (Client)

In `ModClientEvents`:

```java
// Forge 26.3 findet unsere Teile auf dem Server nicht, wenn der Spieler sie schlägt.
// Deshalb greifen wir beim Schlag auf einen Teil stattdessen den Drachen selbst an.
@SubscribeEvent
public static boolean onAttack(InputEvent.InteractionKeyMappingTriggered.Attack event) {
    Minecraft mc = Minecraft.getInstance();
    if (mc.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof UlyssanDragonPart part
            && mc.player != null && mc.gameMode != null) {
        mc.gameMode.attack(mc.player, part.getParent());
        return true;   // true = normalen Angriff abbrechen (der würde den Teil treffen)
    }
    return false;
}
```

- `InteractionKeyMappingTriggered.Attack` feuert, wenn der Spieler die Angriffstaste (Linksklick) drückt.
- `mc.hitResult` ist das, worauf das Fadenkreuz gerade zeigt.
- Ist es ein Drachen-Teil, schicken wir selbst einen Angriff auf den **Parent** (den Drachen) und geben `true` zurück. Dann wird der normale (kaputte) Angriff abgebrochen. Der Arm schwingt trotzdem.

### Die Umgehung, Teil 2: Kopf-Multiplikator auf dem Server

Weil jetzt der Drache selbst getroffen wird, geht der Multiplikator des Teils verloren. Der Server prüft deshalb, **wohin der Spieler geschaut hat**:

```java
@Override
public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
    amount *= 1024.0F / 3000.0F;   // siehe Kapitel 10
    if (source.getDirectEntity() instanceof Player player) {
        UlyssanDragonPart part = this.getPartLookedAt(player);
        if (part != null) {
            amount *= part.getDamageMultiplier();
        }
    }
    boolean hurt = super.hurtServer(level, source, amount);
    if (hurt && !this.isDeadOrDying()) {
        this.triggerAnim("action", "hurt");   // siehe Kapitel 08
    }
    return hurt;
}

// Schießt einen unsichtbaren Strahl aus den Augen des Spielers und gibt den nächsten getroffenen Teil zurück
// (null = der Körper selbst ist näher, oder kein Teil wurde getroffen)
private UlyssanDragonPart getPartLookedAt(Player player) {
    Vec3 eye = player.getEyePosition();
    Vec3 end = eye.add(player.getViewVector(1.0F).scale(8.0));
    double bestDistance = this.getBoundingBox().clip(eye, end)
            .map(eye::distanceToSqr).orElse(Double.MAX_VALUE);
    UlyssanDragonPart best = null;
    for (UlyssanDragonPart part : this.parts) {
        var hit = part.getBoundingBox().clip(eye, end);
        if (hit.isPresent() && eye.distanceToSqr(hit.get()) < bestDistance) {
            bestDistance = eye.distanceToSqr(hit.get());
            best = part;
        }
    }
    return best;
}
```

- `getDirectEntity()` ist, **wer direkt** getroffen hat. Bei einem Schwerthieb der Spieler, bei einem Pfeil der Pfeil. Pfeile treffen die Teile ja direkt, dort greift schon der Multiplikator in `UlyssanDragonPart.hurtServer` – so wird nichts doppelt gerechnet.
- `getViewVector(1.0F).scale(8.0)` = 8 Blöcke in Blickrichtung.
- `AABB.clip(start, ende)` prüft, ob die Linie den Kasten schneidet, und liefert den Schnittpunkt (als `Optional`, weil es auch keinen geben kann).
- Der **nächste** Treffer gewinnt. Ist die Haupt-Hitbox näher als jeder Teil, bleibt der Multiplikator bei 1.
- `distanceToSqr` = Abstand zum Quadrat. Spart das Wurzelziehen und reicht zum Vergleichen.

## 6. Ablauf eines Schwerthiebs auf den Kopf (zusammengefasst)

```
Spieler klickt auf Kopf-Hitbox
  → ModClientEvents.onAttack: "das ist ein Teil" → greift stattdessen den Drachen an
    → Server: UlyssanDragonEntity.hurtServer
      → getPartLookedAt: Spieler schaut auf den Kopf → × 1.5
        → super.hurtServer: Leben wird abgezogen
          → triggerAnim("action", "hurt")
```

## 7. Die Haupt-Hitbox

`.sized(5.0F, 4.0F)` in `ModEntities` ist sehr groß und reicht bis über den Kopf. Wenn du von vorne zielst, triffst du dann oft zuerst diese große Box statt den Kopf. Besser ist ungefähr **`.sized(3.0F, 3.0F)`** – dann deckt sie nur den Körper ab, und Kopf, Schwanz und Flügel übernehmen die Teile.

Bei dir ist die Zeile gerade **auskommentiert**. Dann ist die Haupt-Hitbox nur 0.6 × 1.8 Blöcke groß (Standard) – der Körper lässt sich dann kaum treffen, und der Drache passt durch Gänge, durch die er optisch nicht passt. Nimm sie wieder rein, am besten mit `3.0F, 3.0F`.
