# 09 – Fliegende Mobs

Eine Flug-Animation allein lässt einen Mob nicht fliegen – sie bewegt nur die Flügel. Dieses Kapitel zeigt einen Mob, der **zwischen Laufen und Fliegen wechselt**. Dabei lernst du zwei Dinge, die man oft braucht: **synchronisierte Daten** und **eigene Goals**.

## 1. Die Idee

Jeder Mob hat zwei „Gehirne“ für Bewegung:

| | Aufgabe | am Boden | in der Luft |
|---|---|---|---|
| **`MoveControl`** | *Wie* bewege ich mich zu einem Punkt? | `MoveControl` | `FlyingMoveControl` |
| **`PathNavigation`** | *Welchen Weg* nehme ich? | `GroundPathNavigation` | `FlyingPathNavigation` |

Minecraft hat die Flug-Versionen schon (Biene, Papagei, Allay). Wir **tauschen** beim Abheben auf die Flug-Versionen und **schalten die Schwerkraft aus**. Beim Landen umgekehrt.

> Ein Mob, der **nur** fliegt (wie eine Biene), braucht keinen Tausch: einfach im Konstruktor `this.moveControl = new FlyingMoveControl<>(this, 20, true);` setzen und `createNavigation` überschreiben, sodass es eine `FlyingPathNavigation` zurückgibt.

## 2. Synchronisierte Daten

Die Entscheidung „fliegt / fliegt nicht“ trifft der **Server**. Die Animation läuft auf dem **Client**. Ein normales Feld (`private boolean flying`) wüsste der Client nie.

`SynchedEntityData` ist Minecrafts System für Werte, die **automatisch** vom Server an alle Spieler geschickt werden, sobald sie sich ändern. Vanilla nutzt es z. B. für „brennt“, „schleicht“, „Leben“, „Farbe des Schafs“.

```java
// 1) Schlüssel definieren (static: einer für alle Mobs dieser Klasse)
private static final EntityDataAccessor<Boolean> FLYING =
        SynchedEntityData.defineId(WyvernEntity.class, EntityDataSerializers.BOOLEAN);

// 2) Startwert festlegen
@Override
protected void defineSynchedData(SynchedEntityData.Builder builder) {
    super.defineSynchedData(builder);   // wichtig: Vanilla-Werte nicht vergessen!
    builder.define(FLYING, false);
}

// 3) Lesen (Server und Client) und Schreiben (nur Server)
public boolean isFlying() {
    return this.entityData.get(FLYING);
}
// this.entityData.set(FLYING, true);
```

Andere Typen: `EntityDataSerializers.INT`, `FLOAT`, `STRING`, `BLOCK_POS` …

**Faustregel:** Alles, was der Client für Aussehen oder Animation wissen muss und sich während des Spiels ändert, gehört in `SynchedEntityData`.

## 3. Felder und Konstruktor

```java
// Bewegungs-"Gehirne": eins zum Laufen, eins zum Fliegen
private final MoveControl groundMoveControl;
private final PathNavigation groundNavigation;
private final FlyingMoveControl<WyvernEntity> flyingMoveControl;
private final FlyingPathNavigation flyingNavigation;
private int flightTicks;     // wie lange er schon in der Luft ist

public WyvernEntity(EntityType<? extends Monster> type, Level level) {
    super(type, level);
    this.groundMoveControl = this.moveControl;          // die normalen, die Mob schon erzeugt hat
    this.groundNavigation = this.navigation;
    this.flyingMoveControl = new FlyingMoveControl<>(this, 10, true);
    this.flyingNavigation = new FlyingPathNavigation(this, level);
}
```

`FlyingMoveControl(mob, maxNeigung, schwebtAufDerStelle)`:
- `10` = wie viele Grad er sich pro Tick nach oben/unten neigen darf.
- `true` = schwebt, wenn er kein Ziel hat (sonst sinkt er).

Neue Attribute:

```java
.add(Attributes.FLYING_SPEED, 0.5)
.add(Attributes.FALL_DAMAGE_MULTIPLIER, 0.0);   // kein Fallschaden
```

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

- `bedingung ? a : b` = **ternärer Operator** (kurzes if/else).
- `navigation.stop()` bricht den alten Weg ab, bevor getauscht wird.
- `resetFallDistance()` = gefallene Höhe vergessen → kein Fallschaden beim Landen.

## 5. Wann abheben, wann landen?

```java
// Läuft jeden Tick nur auf dem Server
@Override
protected void customServerAiStep(ServerLevel level) {
    super.customServerAiStep(level);
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
- das Ziel weiter als **16 Blöcke** weg ist (`distanceToSqr` > 16² – Quadrat spart die Wurzel),
- das Ziel mehr als **4 Blöcke höher** steht,
- oder zufällig: `nextInt(1200) == 0` ≈ **einmal pro Minute** (1200 Ticks).

**Landen**, wenn er nach mindestens 40 Ticks Flug den Boden berührt. Die 40 Ticks verhindern, dass er direkt nach dem Abheben „landet“.

`customServerAiStep` läuft **nur auf dem Server** – genau richtig, nur der Server entscheidet.

## 6. Schneller fliegen

```java
// Beschleunigung in der Luft (Minecraft-Standard für Mobs: nur 0.02)
@Override
protected float getFlyingSpeed() {
    return this.isFlying() ? this.getSpeed() * 0.1F : super.getFlyingSpeed();
}
```

Ohne diese Methode beschleunigt jeder Mob in der Luft mit festen 0.02 – ein großer Mob kriecht dann mit ~1 Block/s. Mit `getSpeed() * 0.1` und `FLYING_SPEED 0.5` sind es etwa **3 Blöcke/s**.

## 7. Ein eigenes Goal: herumfliegen

Eine **innere Klasse** (in `WyvernEntity`), die deshalb direkt auf `isFlying()`, `getX()`, `random` usw. zugreifen kann:

```java
// Fliegt ohne Ziel herum; nach ~20 Sekunden fliegt er zum Landen herunter
private class FlyAroundGoal extends Goal {
    private double wantedX, wantedY, wantedZ;

    FlyAroundGoal() {
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
        // FlyingMoveControl vergisst das Ziel nach jedem Tick → jeden Tick neu setzen
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

### Aufbau eines Goals

| Methode | Wann | Was |
|---|---|---|
| `canUse()` | ständig geprüft | darf es starten? |
| `start()` | beim Start | Vorbereitung |
| `tick()` | jeden Tick, solange es läuft | die eigentliche Arbeit |
| `canContinueToUse()` | jeden Tick | darf es weiterlaufen? (Standard: wie `canUse`) |
| `stop()` | am Ende | aufräumen |

- `setFlags(EnumSet.of(Goal.Flag.MOVE))`: belegt die Bewegung. Zwei Goals mit `MOVE` laufen nie gleichzeitig – kleinere Prioritätszahl gewinnt. Weitere Flags: `LOOK`, `JUMP`, `TARGET`.
- `random.nextDouble() * 2 - 1` = Zahl zwischen −1 und 1 → × 20 = Punkt bis 20 Blöcke entfernt.
- `level().getHeight(Heightmap.Types.MOTION_BLOCKING, x, z)` = Höhe des Bodens an dieser Stelle.
- Nach 400 Ticks (20 s) wählt er manchmal einen Punkt **am Boden** → fliegt hinunter, berührt den Boden, `customServerAiStep` lässt ihn landen.

### Goals anmelden

```java
this.goalSelector.addGoal(4, new FlyAroundGoal());
this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8) {
    @Override
    public boolean canUse() {
        return !isFlying() && super.canUse();   // nur am Boden herumlaufen
    }
});
```

Sonst würde das Lauf-Goal den fliegenden Mob zu Punkten am Boden schicken.

**Mit Ziel** übernimmt `MeleeAttackGoal`. Es benutzt `getNavigation()` – im Flug automatisch die `FlyingPathNavigation`. Er fliegt also direkt auf das Ziel zu und landet, wenn er dabei den Boden berührt.

## 8. Flug-Animationen

Im Movement-Controller ([08](08-Mob-Animationen.md)) **vor** dem Kampf-Block:

```java
if (mob.isFlying()) {
    double fallSpeed = mob.yo - mob.getY();         // wie weit er seit dem letzten Tick gesunken ist
    if (fallSpeed > 0.3) {
        return test.setAndContinue(FLY_DIVE);
    }
    return test.setAndContinue(test.isMoving() ? FLY_CRUISE : FLY_HOVER);
}
```

Und am Action-Controller: `.triggerableAnim("takeoff", TAKEOFF).triggerableAnim("land", LAND)`.

> Steht der `isAggressive()`-Block **vor** dem Flug-Block, zeigt ein fliegender Mob mit Ziel die **Renn**-Animation in der Luft.

## 9. Speichern und Laden

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

- Beim Speichern der Welt schreibt Minecraft alle Daten der Entity in die Datei; `putBoolean` fügt unseren Wert hinzu. Weitere: `putInt`, `putFloat`, `putString` / `getIntOr` …
- `getBooleanOr(name, standard)` → `false`, wenn der Wert fehlt (z. B. bei alten Mobs).
- **Immer `super.…` aufrufen**, sonst gehen Leben, Position, Name usw. verloren.
- Damit geht auch `/summon mymod:wyvern ~ ~ ~ {Flying:1b}`.

## 10. Werte zum Anpassen

| Was | Wo |
|---|---|
| Flughöhe | `ground + 8 + random.nextInt(10)` |
| Fluggeschwindigkeit | `FLYING_SPEED` und `0.1F` in `getFlyingSpeed` |
| Wie oft er zufällig abhebt | `random.nextInt(1200)` (größer = seltener) |
| Ab wann er landen will | `flightTicks > 400` |
| Ab welcher Ziel-Entfernung er abhebt | `16 * 16` |
| Radius beim Herumfliegen | `* 20` in `pickNewPoint` |

## Checkliste

- [ ] `SynchedEntityData` für den Zustand
- [ ] beide Bewegungs-Gehirne im Konstruktor
- [ ] `setFlying` mit Schwerkraft + Tausch
- [ ] Entscheidungslogik in `customServerAiStep`
- [ ] eigenes Flug-Goal, Lauf-Goal nur am Boden
- [ ] `getFlyingSpeed`, `FLYING_SPEED`, kein Fallschaden
- [ ] Speichern/Laden
- [ ] Flug-Animationen vor den Kampf-Animationen

## Zum Ausprobieren

- Lass ihn nachts schlafen: neuer synchronisierter Wert `SLEEPING`, gesetzt wenn `level.isDarkOutside()` und kein Ziel.
- Sturzflug-Angriff: Wenn das Ziel unter ihm ist, mit hoher Geschwindigkeit darauf zufliegen und beim Aufprall Schaden in einem Radius machen.
- Lies `Phantom` – wie kreist der Phantom um den Spieler?
