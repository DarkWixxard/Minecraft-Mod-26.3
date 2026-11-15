# 09 – Der Drache: Fliegen

Eine Flug-Animation allein lässt den Drachen nicht fliegen – sie bewegt nur die Flügel. Damit er wirklich in die Luft geht, braucht er **Flug-Logik**.

## 1. Die Idee

Jeder Mob hat zwei „Gehirne“ für die Bewegung:

| | Aufgabe | am Boden | in der Luft |
|---|---|---|---|
| **`MoveControl`** | *Wie* bewege ich mich zu einem Punkt? | `MoveControl` (laufen, springen) | `FlyingMoveControl` (in alle Richtungen) |
| **`PathNavigation`** | *Welchen Weg* nehme ich? | `GroundPathNavigation` (über Blöcke) | `FlyingPathNavigation` (durch die Luft) |

Minecraft hat die Flug-Versionen schon (Bienen, Papageien, Allays benutzen sie). Wir **tauschen** beim Abheben auf die Flug-Versionen um und **schalten die Schwerkraft aus**. Beim Landen tauschen wir zurück.

## 2. Felder

```java
// Fliegt der Drache? "Synched" = der Server schickt den Wert an jeden Spieler, damit die Animationen ihn kennen
private static final EntityDataAccessor<Boolean> FLYING =
        SynchedEntityData.defineId(UlyssanDragonEntity.class, EntityDataSerializers.BOOLEAN);

// Bewegungs-"Gehirne": eins zum Laufen, eins zum Fliegen
private final MoveControl groundMoveControl;
private final PathNavigation groundNavigation;
private final FlyingMoveControl<UlyssanDragonEntity> flyingMoveControl;
private final FlyingPathNavigation flyingNavigation;
private int flightTicks;     // wie lange der Drache schon in der Luft ist
```

### Was ist `SynchedEntityData`?

Die Entscheidung „fliegt / fliegt nicht“ trifft der **Server**. Die Animation läuft aber beim **Client**. Ein normales Feld (`private boolean flying`) wüsste der Client nie.

`SynchedEntityData` ist Minecrafts System für Werte, die **automatisch** vom Server an alle Spieler geschickt werden, sobald sie sich ändern. Vanilla benutzt es z. B. für „brennt“, „schleicht“, „Leben“.

```java
@Override
protected void defineSynchedData(SynchedEntityData.Builder builder) {
    super.defineSynchedData(builder);   // wichtig: Vanilla-Werte nicht vergessen!
    builder.define(FLYING, false);      // Startwert
}

public boolean isFlying() {
    return this.entityData.get(FLYING);
}
```

## 3. Konstruktor

```java
public UlyssanDragonEntity(EntityType<? extends Monster> type, Level level) {
    super(type, level);
    this.groundMoveControl = this.moveControl;          // die normalen, die Mob schon erzeugt hat
    this.groundNavigation = this.navigation;
    this.flyingMoveControl = new FlyingMoveControl<>(this, 10, true);
    this.flyingNavigation = new FlyingPathNavigation(this, level);
}
```

`FlyingMoveControl(mob, maxDrehung, schwebtAufDerStelle)`:
- `10` = wie viele Grad er sich pro Tick nach oben/unten neigen darf.
- `true` = er schwebt auf der Stelle, wenn er kein Ziel hat (sonst würde er herunterfallen).

## 4. Umschalten

```java
private void setFlying(boolean flying) {
    this.entityData.set(FLYING, flying);
    this.setNoGravity(flying);
    this.navigation.stop();
    this.moveControl = flying ? this.flyingMoveControl : this.groundMoveControl;
    this.navigation = flying ? this.flyingNavigation : this.groundNavigation;
    this.flightTicks = 0;
    this.resetFallDistance();
}
```

- `bedingung ? a : b` ist der **ternäre Operator**: wenn `flying`, dann `a`, sonst `b`.
- `navigation.stop()` bricht den alten Weg ab, bevor das Gehirn getauscht wird.
- `resetFallDistance()` setzt die „gefallene Höhe“ zurück, damit beim Landen kein Fallschaden entsteht (zusätzlich ist `FALL_DAMAGE_MULTIPLIER` 0).

## 5. Wann hebt er ab, wann landet er?

```java
// Läuft jeden Tick auf dem Server
@Override
protected void customServerAiStep(ServerLevel level) {
    super.customServerAiStep(level);
    this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());   // Bossbar, siehe Kapitel 10
    LivingEntity target = this.getTarget();
    if (!this.isFlying()) {
        boolean targetFarAway = target != null && this.distanceToSqr(target) > 16 * 16;
        boolean targetHigh = target != null && target.getY() > this.getY() + 4;
        if (this.onGround() && (targetFarAway || targetHigh || this.random.nextInt(1200) == 0)) {
            this.setFlying(true);
            this.setDeltaMovement(this.getDeltaMovement().add(0, 0.6, 0));   // in die Luft springen
            this.triggerAnim("action", "takeoff");
        }
    } else {
        this.flightTicks++;
        // nach einer Weile wieder Boden berührt → landen
        if (this.onGround() && this.flightTicks > 40) {
            this.setFlying(false);
            this.triggerAnim("action", "land");
        }
    }
}
```

**Abheben**, wenn er am Boden steht und:
- sein Ziel weiter als **16 Blöcke** weg ist (`distanceToSqr` > 16² – Abstand zum Quadrat spart die Wurzel),
- oder das Ziel mehr als **4 Blöcke höher** steht (z. B. Spieler auf einem Turm),
- oder zufällig: `nextInt(1200) == 0` passiert im Schnitt einmal in 1200 Ticks = **einmal pro Minute**.

**Landen**, wenn er nach mindestens 40 Ticks (2 Sekunden) Flug wieder den Boden berührt. Die 40 Ticks verhindern, dass er direkt nach dem Abheben wieder „landet“.

`customServerAiStep` läuft **nur auf dem Server** – genau richtig, denn nur der Server soll entscheiden.

## 6. Schneller fliegen

```java
// Wie stark der Drache in der Luft beschleunigt (Minecraft-Standard für Mobs ist nur 0.02)
@Override
protected float getFlyingSpeed() {
    return this.isFlying() ? this.getSpeed() * 0.1F : super.getFlyingSpeed();
}
```

Ohne diese Methode beschleunigt jeder Mob in der Luft mit festen 0.02 – der Drache kroch dann mit etwa 1 Block pro Sekunde. Mit `getSpeed() * 0.1` (bei `FLYING_SPEED` 0.5) kommt er auf etwa **3 Blöcke pro Sekunde**. Im Test flog er damit 8–17 Blöcke über dem Boden und drehte seine Runden.

## 7. Herumfliegen ohne Ziel: ein eigenes Goal

Eine **innere Klasse** (steht innerhalb von `UlyssanDragonEntity` und kann deshalb direkt auf `isFlying()`, `getX()`, `random` usw. zugreifen):

```java
// Fliegt ohne Ziel herum; nach ~20 Sekunden fliegt er zum Landen herunter
private class DragonFlyAroundGoal extends Goal {
    private double wantedX, wantedY, wantedZ;

    DragonFlyAroundGoal() {
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));   // dieses Goal braucht die Bewegung
    }

    @Override
    public boolean canUse() {
        return isFlying() && getTarget() == null;    // nur in der Luft und ohne Ziel
    }

    @Override
    public void start() {
        this.pickNewPoint();
    }

    @Override
    public void tick() {
        boolean reached = distanceToSqr(wantedX, wantedY, wantedZ) < 3 * 3;
        if (reached || random.nextInt(200) == 0) {
            this.pickNewPoint();
        }
        // FlyingMoveControl vergisst das Ziel nach jedem Tick, deshalb jeden Tick neu setzen
        getMoveControl().setWantedPosition(wantedX, wantedY, wantedZ, 1.0);
    }

    private void pickNewPoint() {
        wantedX = getX() + (random.nextDouble() * 2 - 1) * 20;
        wantedZ = getZ() + (random.nextDouble() * 2 - 1) * 20;
        double ground = level().getHeight(Heightmap.Types.MOTION_BLOCKING, (int) wantedX, (int) wantedZ);
        boolean wantsToLand = flightTicks > 400 && random.nextInt(3) == 0;
        wantedY = wantsToLand ? ground : ground + 8 + random.nextInt(10);   // landen oder 8–17 Blöcke hoch
    }
}
```

### So funktioniert ein Goal

| Methode | Wann | Was |
|---|---|---|
| `canUse()` | ständig geprüft | Darf das Goal starten? |
| `start()` | beim Start | Vorbereitung |
| `tick()` | jeden Tick, solange es läuft | die eigentliche Arbeit |
| `canContinueToUse()` | jeden Tick | Darf es weiterlaufen? (Standard: dasselbe wie `canUse`) |
| `stop()` | beim Ende | Aufräumen |

- `setFlags(EnumSet.of(Goal.Flag.MOVE))`: Das Goal „belegt“ die Bewegung. Zwei Goals mit `MOVE` laufen nie gleichzeitig – das mit der kleineren Prioritätszahl gewinnt.
- `random.nextDouble() * 2 - 1` gibt eine Zahl zwischen −1 und 1. × 20 = ein zufälliger Punkt bis zu 20 Blöcke entfernt.
- `level().getHeight(Heightmap.Types.MOTION_BLOCKING, x, z)` = Höhe des höchsten festen Blocks an dieser Stelle (der Boden).
- Nach 400 Ticks (20 Sekunden) Flug wählt er mit 1/3 Wahrscheinlichkeit einen Punkt **am Boden** → er fliegt hinunter, berührt den Boden, und `customServerAiStep` lässt ihn landen.

**Mit Ziel** übernimmt `MeleeAttackGoal`. Es benutzt `getNavigation()`, und das ist im Flug automatisch die `FlyingPathNavigation`. Er fliegt also direkt auf den Spieler zu. Berührt er dabei den Boden, landet er und kämpft dort weiter.

### Herumlaufen nur am Boden

```java
this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8) {
    @Override
    public boolean canUse() {
        return !isFlying() && super.canUse();
    }
});
```

Sonst würde das Lauf-Goal den Drachen in der Luft zu Punkten am Boden schicken.

## 8. Speichern

```java
@Override
protected void addAdditionalSaveData(ValueOutput output) {
    super.addAdditionalSaveData(output);
    output.putBoolean("Flying", this.isFlying());
}

@Override
protected void readAdditionalSaveData(ValueInput input) {
    super.readAdditionalSaveData(input);
    this.setFlying(input.getBooleanOr("Flying", false));
}
```

- Beim Speichern der Welt schreibt Minecraft alle Daten der Entity in die Datei. Mit `putBoolean` kommt unser Wert dazu.
- Beim Laden lesen wir ihn wieder. `getBooleanOr(name, standard)` gibt `false`, wenn der Wert fehlt (z. B. bei alten Drachen).
- **Immer `super.…` aufrufen!** Sonst gehen Leben, Position, Name usw. verloren.
- Dadurch funktioniert auch `/summon rubymod:ulyssan_dragon ~ ~ ~ {Flying:1b}`.

## 9. Anpassen

| Was | Wo |
|---|---|
| Flughöhe | `ground + 8 + random.nextInt(10)` |
| Fluggeschwindigkeit | `FLYING_SPEED` und das `0.1F` in `getFlyingSpeed` |
| Wie oft er zufällig abhebt | `random.nextInt(1200)` (größer = seltener) |
| Ab wann er landen will | `flightTicks > 400` (20 Sekunden) |
| Ab welcher Entfernung zum Ziel | `16 * 16` |
| Wie weit er herumfliegt | `* 20` in `pickNewPoint` |
