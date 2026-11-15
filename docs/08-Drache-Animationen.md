# 08 – Der Drache: Animationen

## 1. Zwei Arten von Animationen

| Art | Beispiele | Wer entscheidet? | Methode |
|---|---|---|---|
| **Dauer-Animationen** (Zustände) | stehen, laufen, rennen, fliegen, tot | Der Controller schaut **jeden Frame** auf den Drachen und wählt die passende | `thenLoop` / `thenPlayAndHold` |
| **Einmal-Animationen** (Aktionen) | beißen, Kralle, Schwanzschlag, getroffen, abheben, landen | Der **Server** löst sie in einem bestimmten Moment aus | `thenPlay` + `triggerableAnim` |

Für beides gibt es je einen **Controller**. Ein Controller ist wie eine eigene „Spur“: Beide laufen gleichzeitig, und der später registrierte liegt oben drauf. So kann der Drache z. B. beim Rennen beißen.

## 2. Animationen als Konstanten

```java
private static final RawAnimation IDLE       = RawAnimation.begin().thenLoop("animation.ulyssan_dragon.idle_calm");
private static final RawAnimation WALK       = RawAnimation.begin().thenLoop("animation.ulyssan_dragon.walk_calm");
private static final RawAnimation IDLE_AGGRO = RawAnimation.begin().thenLoop("animation.ulyssan_dragon.idle_aggro");
private static final RawAnimation RUN        = RawAnimation.begin().thenLoop("animation.ulyssan_dragon.run_charge");
private static final RawAnimation DEATH      = RawAnimation.begin().thenPlayAndHold("animation.ulyssan_dragon.death_ground");
private static final RawAnimation BITE       = RawAnimation.begin().thenPlay("animation.ulyssan_dragon.attack_bite");
private static final RawAnimation CLAW       = RawAnimation.begin().thenPlay("animation.ulyssan_dragon.attack_claw_left");
private static final RawAnimation TAIL       = RawAnimation.begin().thenPlay("animation.ulyssan_dragon.attack_tail_sweep");
private static final RawAnimation HURT       = RawAnimation.begin().thenPlay("animation.ulyssan_dragon.hurt_light");
// Fliegen
private static final RawAnimation FLY_HOVER  = RawAnimation.begin().thenLoop("animation.ulyssan_dragon.fly_hover");
private static final RawAnimation FLY_CRUISE = RawAnimation.begin().thenLoop("animation.ulyssan_dragon.fly_cruise");
private static final RawAnimation FLY_DIVE   = RawAnimation.begin().thenLoop("animation.ulyssan_dragon.fly_dive");
private static final RawAnimation TAKEOFF    = RawAnimation.begin().thenPlay("animation.ulyssan_dragon.fly_takeoff");
private static final RawAnimation LAND       = RawAnimation.begin().thenPlay("animation.ulyssan_dragon.fly_land");
```

| Methode | Verhalten |
|---|---|
| `thenLoop(name)` | endlos wiederholen |
| `thenPlay(name)` | einmal abspielen |
| `thenPlayAndHold(name)` | einmal abspielen und auf dem **letzten Bild stehen bleiben** (für den Tod: er bleibt liegen) |
| `thenPlayXTimes(name, n)` | n-mal abspielen |
| `thenWait(ticks)` | Pause |

Man kann sie verketten: `RawAnimation.begin().thenPlay("…roar").thenLoop("…idle_aggro")` = erst brüllen, dann aggressiv stehen.

`static final` heißt: Die Animation wird nur **einmal** für alle Drachen erzeugt, nicht für jeden neu.

## 3. Die Controller

```java
@Override
public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    // 1) Bewegung: entscheidet jeden Frame, welche Dauer-Animation läuft
    controllers.add(new AnimationController<UlyssanDragonEntity>("movement", 5, test -> {
        UlyssanDragonEntity dragon = test.animatable();
        if (dragon.isDeadOrDying()) {
            return test.setAndContinue(DEATH);
        }
        if (dragon.isFlying()) {
            double fallSpeed = dragon.yo - dragon.getY();   // wie weit er seit dem letzten Tick gesunken ist
            if (fallSpeed > 0.3) {
                return test.setAndContinue(FLY_DIVE);
            }
            return test.setAndContinue(test.isMoving() ? FLY_CRUISE : FLY_HOVER);
        }
        if (dragon.isAggressive()) {                         // hat ein Ziel (setzt MeleeAttackGoal)
            return test.setAndContinue(test.isMoving() ? RUN : IDLE_AGGRO);
        }
        return test.setAndContinue(test.isMoving() ? WALK : IDLE);
    }));

    // 2) Aktionen: spielt von selbst nichts, nur wenn der Server eine Animation auslöst
    controllers.add(new AnimationController<UlyssanDragonEntity>("action", 2, test -> PlayState.STOP)
            .triggerableAnim("bite", BITE)
            .triggerableAnim("claw", CLAW)
            .triggerableAnim("tail", TAIL)
            .triggerableAnim("hurt", HURT)
            .triggerableAnim("takeoff", TAKEOFF)
            .triggerableAnim("land", LAND));
}
```

### Erklärung

- `new AnimationController<>(name, übergangsTicks, handler)`
  - **name**: frei wählbar, wird beim Auslösen gebraucht (`"action"`).
  - **übergangsTicks**: wie viele Ticks zwischen zwei Animationen weich überblendet wird. 5 = ¼ Sekunde. 0 = harter Schnitt.
  - **handler**: eine Funktion, die GeckoLib **jeden Frame** aufruft.
- `test.animatable()` gibt dir den Drachen.
- `test.setAndContinue(ANIM)` = „spiel diese Animation, und mach weiter“. Ist es dieselbe wie gerade, läuft sie einfach weiter.
- `test.isMoving()` = bewegt sich der Drache gerade?
- `PlayState.STOP` = „spiel nichts“. Der Aktions-Controller ist also normalerweise still.
- **Die Reihenfolge der `if`s ist wichtig.** Das erste passende gewinnt. Tod kommt zuerst (sonst würde er tot weiterlaufen), dann Fliegen, dann Kampf, dann normal.

> ⚠️ **In deinem Code steht `isAggressive()` im Moment *vor* `isFlying()`.** Dann zeigt ein Drache, der ein Ziel hat und fliegt, die **Renn**-Animation in der Luft. Tausch die beiden Blöcke, so wie oben.

## 4. Einmal-Animationen auslösen (Server)

```java
// Wird auf dem Server aufgerufen, wenn der Drache sein Ziel trifft
@Override
public boolean doHurtTarget(ServerLevel level, Entity target) {
    String[] attacks = {"bite", "claw", "tail"};
    this.triggerAnim("action", attacks[this.random.nextInt(attacks.length)]);   // zufälliger Angriff
    return super.doHurtTarget(level, target);
}
```

- `triggerAnim("action", "bite")` = Spiel im Controller `"action"` die Animation `"bite"`. GeckoLib schickt das automatisch an alle Spieler in der Nähe.
- `random.nextInt(3)` gibt 0, 1 oder 2 → zufälliger Angriff.
- Getroffen-Animation in `hurtServer` (siehe [07](07-Drache-Hitboxen.md#die-umgehung-teil-2-kopf-multiplikator-auf-dem-server)):

```java
boolean hurt = super.hurtServer(level, source, amount);   // Schaden anwenden, Ergebnis merken
if (hurt && !this.isDeadOrDying()) {
    this.triggerAnim("action", "hurt");                     // nur wenn wirklich verletzt und nicht tot
}
return hurt;
```

**Warum nicht einfach `return super.hurtServer(...)` und danach `triggerAnim`?** Weil `return` die Methode **sofort** beendet. Alles dahinter würde nie laufen. Deshalb erst das Ergebnis in eine Variable speichern, dann die Animation, dann `return`.

`super.hurtServer` gibt `false` zurück, wenn kein Schaden ankam (z. B. kurze Unverwundbarkeit nach einem Treffer). Dann soll auch keine Animation kommen.

## 5. Die Todesanimation abwarten

Normalerweise verschwindet ein toter Mob nach **20 Ticks** (1 Sekunde). Die Todesanimation dauert aber **3,4 Sekunden = 68 Ticks**.

```java
// Warten, bis die Todesanimation (3,4 s = 68 Ticks) gespielt ist
@Override
protected void tickDeath() {
    this.deathTime++;
    if (this.deathTime >= 68 && !this.level().isClientSide() && !this.isRemoved()) {
        this.level().broadcastEntityEvent(this, (byte) 60);   // Rauch-Partikel
        this.remove(RemovalReason.KILLED);
    }
}
```

Das ist eine Kopie der Vanilla-Methode aus `LivingEntity`, nur mit 68 statt 20. `!isClientSide()` = nur der Server entfernt den Drachen. Event 60 sind die Rauchwölkchen, die beim Tod eines Mobs erscheinen.

Die Länge jeder Animation steht in der `.animation.json` bei `"animation_length"` (in Sekunden). × 20 = Ticks.

## 6. Weitere Animationen einbauen – Rezept

1. **Konstante anlegen** mit dem exakten Namen aus der JSON.
2. **Art entscheiden:**
   - **Zustand** (z. B. `sleep_loop_curled`, `fly_glide`) → ein weiteres `if` im `"movement"`-Controller, an der richtigen Stelle der Reihenfolge.
   - **Aktion** (z. B. `roar`, `cast_ice_breath`) → `.triggerableAnim("roar", ROAR)` am `"action"`-Controller und an der passenden Stelle im Code `triggerAnim("action", "roar")`.
3. Testen. Kommt *„Unable to find animation“*, stimmt der Name nicht.

### Ideen

| Idee | Wie |
|---|---|
| Brüllen, wenn er ein Ziel entdeckt | In `setTarget(...)` überschreiben: wenn vorher kein Ziel und jetzt eins → `triggerAnim("action", "roar")` |
| Gleiten beim langsamen Sinken | Im Flug-Block: `fallSpeed > 0.05` → `FLY_GLIDE` |
| Wut-Phase unter 50 % Leben | In `customServerAiStep`: einmalig `enrage_roar` auslösen, wenn `getHealth() < getMaxHealth() / 2` |
| Eisatem | Animation reicht nicht – braucht ein eigenes **Goal**, das Schaden in einem Kegel vor dem Kopf macht und Partikel erzeugt |
