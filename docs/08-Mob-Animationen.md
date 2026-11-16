# 08 – Mob-Animationen mit GeckoLib

## 1. Zwei Arten von Animationen

| Art | Beispiele | Wer entscheidet? | Methode |
|---|---|---|---|
| **Dauer-Animationen** (Zustände) | stehen, laufen, rennen, fliegen, schlafen, tot | ein Controller schaut **jeden Frame** auf den Mob und wählt | `thenLoop` / `thenPlayAndHold` |
| **Einmal-Animationen** (Aktionen) | beißen, Schwanzschlag, getroffen, brüllen, abheben | der **Server** löst sie in einem Moment aus | `thenPlay` + `triggerableAnim` |

Für jede Art gibt es einen **Controller** – wie zwei Spuren in einem Videoschnitt-Programm. Beide laufen gleichzeitig, der später registrierte liegt oben. So kann der Mob beim Rennen beißen.

## 2. Animationen als Konstanten

```java
private static final RawAnimation IDLE       = RawAnimation.begin().thenLoop("animation.wyvern.idle");
private static final RawAnimation WALK       = RawAnimation.begin().thenLoop("animation.wyvern.walk");
private static final RawAnimation IDLE_AGGRO = RawAnimation.begin().thenLoop("animation.wyvern.idle_aggro");
private static final RawAnimation RUN        = RawAnimation.begin().thenLoop("animation.wyvern.run");
private static final RawAnimation DEATH      = RawAnimation.begin().thenPlayAndHold("animation.wyvern.death");
private static final RawAnimation BITE       = RawAnimation.begin().thenPlay("animation.wyvern.attack_bite");
private static final RawAnimation CLAW       = RawAnimation.begin().thenPlay("animation.wyvern.attack_claw");
private static final RawAnimation TAIL       = RawAnimation.begin().thenPlay("animation.wyvern.attack_tail");
private static final RawAnimation HURT       = RawAnimation.begin().thenPlay("animation.wyvern.hurt");
```

| Methode | Verhalten |
|---|---|
| `thenLoop(name)` | endlos wiederholen |
| `thenPlay(name)` | einmal |
| `thenPlayAndHold(name)` | einmal, dann auf dem **letzten Bild** stehen bleiben (Tod: liegen bleiben) |
| `thenPlayXTimes(name, n)` | n-mal |
| `thenWait(ticks)` | Pause |

Verketten geht: `RawAnimation.begin().thenPlay("animation.wyvern.roar").thenLoop("animation.wyvern.idle_aggro")` = erst brüllen, dann aggressiv stehen.

`static final`: Die Animation wird **einmal** für alle Mobs erzeugt, nicht für jeden neu.

Die Namen müssen **exakt** denen in der `.animation.json` entsprechen.

## 3. Die Controller

```java
@Override
public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    // 1) Bewegung: entscheidet jeden Frame, welche Dauer-Animation läuft
    controllers.add(new AnimationController<WyvernEntity>("movement", 5, test -> {
        WyvernEntity mob = test.animatable();
        if (mob.isDeadOrDying()) {
            return test.setAndContinue(DEATH);
        }
        if (mob.isAggressive()) {                       // hat ein Ziel (setzt MeleeAttackGoal)
            return test.setAndContinue(test.isMoving() ? RUN : IDLE_AGGRO);
        }
        return test.setAndContinue(test.isMoving() ? WALK : IDLE);
    }));

    // 2) Aktionen: spielt von selbst nichts, nur wenn der Server eine Animation auslöst
    controllers.add(new AnimationController<WyvernEntity>("action", 2, test -> PlayState.STOP)
            .triggerableAnim("bite", BITE)
            .triggerableAnim("claw", CLAW)
            .triggerableAnim("tail", TAIL)
            .triggerableAnim("hurt", HURT));
}
```

### Erklärung

- `new AnimationController<>(name, übergangsTicks, handler)`
  - **name**: frei wählbar, wird beim Auslösen gebraucht.
  - **übergangsTicks**: wie lange zwischen zwei Animationen weich überblendet wird. 5 = ¼ s, 0 = harter Schnitt.
  - **handler**: Funktion, die GeckoLib **jeden Frame** aufruft.
- `test.animatable()` = der Mob.
- `test.setAndContinue(ANIM)` = „spiel diese, und mach weiter“. Ist es dieselbe wie gerade, läuft sie einfach weiter.
- `test.isMoving()` = bewegt er sich?
- `PlayState.STOP` = nichts spielen.
- **Die Reihenfolge der `if`s zählt.** Das erste passende gewinnt: Tod zuerst (sonst läuft er tot weiter), dann besondere Zustände (Fliegen, Schlafen – siehe [09](09-Mob-Fliegen.md)), dann Kampf, dann normal.

Der Handler läuft auf dem **Client**. Er kann nur Werte lesen, die der Client kennt: Position, Bewegung, Leben, `isAggressive()`, und eigene **synchronisierte** Werte ([09](09-Mob-Fliegen.md#2-synchronisierte-daten)). Ein normales `private boolean` im Mob, das nur der Server setzt, sieht der Handler nie.

## 4. Einmal-Animationen auslösen (Server)

### Beim Angriff

```java
// Wird auf dem Server aufgerufen, wenn der Mob sein Ziel trifft
@Override
public boolean doHurtTarget(ServerLevel level, Entity target) {
    String[] attacks = {"bite", "claw", "tail"};
    this.triggerAnim("action", attacks[this.random.nextInt(attacks.length)]);   // zufälliger Angriff
    return super.doHurtTarget(level, target);
}
```

- `triggerAnim("action", "bite")` = im Controller `"action"` die Animation `"bite"` spielen. GeckoLib schickt das automatisch an alle Spieler in der Nähe.
- `random.nextInt(3)` gibt 0, 1 oder 2.

### Beim Getroffen-werden

```java
@Override
public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
    boolean hurt = super.hurtServer(level, source, amount);   // Schaden anwenden, Ergebnis merken
    if (hurt && !this.isDeadOrDying()) {
        this.triggerAnim("action", "hurt");                     // nur wenn wirklich verletzt und nicht tot
    }
    return hurt;
}
```

**Warum nicht `return super.hurtServer(...)` und danach `triggerAnim`?** `return` beendet die Methode **sofort** – alles dahinter läuft nie. Deshalb: Ergebnis in eine Variable, dann Animation, dann `return`.

`super.hurtServer` gibt `false` zurück, wenn kein Schaden ankam (z. B. kurze Unverwundbarkeit nach einem Treffer) – dann keine Animation.

### Timing: Schaden passend zur Animation

`doHurtTarget` macht den Schaden **sofort**, die Animation zeigt den Biss aber vielleicht erst nach 0,5 s. Für genaues Timing: im Angriffs-Goal zuerst die Animation auslösen, einen Zähler starten und den Schaden erst nach z. B. 10 Ticks machen. Für den Anfang ist „sofort“ völlig in Ordnung.

## 5. Todesanimation abwarten

Ein toter Mob verschwindet normal nach **20 Ticks** (1 s). Ist die Todesanimation länger, wird sie abgeschnitten.

```java
// Warten, bis die Todesanimation gespielt ist (hier 3 s = 60 Ticks)
private static final int DEATH_TICKS = 60;

@Override
protected void tickDeath() {
    this.deathTime++;
    if (this.deathTime >= DEATH_TICKS && !this.level().isClientSide() && !this.isRemoved()) {
        this.level().broadcastEntityEvent(this, (byte) 60);   // Rauch-Partikel
        this.remove(RemovalReason.KILLED);
    }
}
```

Das ist die Vanilla-Methode aus `LivingEntity`, nur mit anderer Zahl. `!isClientSide()` = nur der Server entfernt den Mob. Event 60 = die Rauchwölkchen beim Tod.

Die Länge jeder Animation steht in der `.animation.json` bei `"animation_length"` (Sekunden). × 20 = Ticks.

## 6. Neue Animation einbauen – Rezept

1. **Konstante** mit dem exakten Namen aus der JSON.
2. **Art entscheiden:**
   - **Zustand** → weiteres `if` im `"movement"`-Controller, an der richtigen Stelle der Reihenfolge.
   - **Aktion** → `.triggerableAnim("roar", ROAR)` am `"action"`-Controller + an passender Stelle im Server-Code `triggerAnim("action", "roar")`.
3. Testen. *„Unable to find animation“* = Name stimmt nicht.

### Ideen

| Idee | Wie |
|---|---|
| Brüllen, wenn er ein Ziel entdeckt | `setTarget(...)` überschreiben: vorher kein Ziel, jetzt eins → `triggerAnim("action", "roar")` |
| Wut-Phase unter 50 % Leben | in `customServerAiStep` einmalig auslösen, wenn `getHealth() < getMaxHealth() / 2` (mit einem `boolean`, damit es nur einmal passiert) |
| Schlafen nachts | synchronisierter Wert `SLEEPING` ([09](09-Mob-Fliegen.md#2-synchronisierte-daten)) + `if` im Movement-Controller |
| Feuer-/Eisatem | Animation reicht nicht – braucht ein eigenes **Goal**, das Schaden in einem Kegel vor dem Kopf macht und Partikel erzeugt |

## Checkliste

- [ ] Animationsnamen aus der JSON kopiert (nicht abgetippt)
- [ ] Controller `"movement"` mit sinnvoller `if`-Reihenfolge
- [ ] Controller `"action"` mit `triggerableAnim`
- [ ] `triggerAnim` nur auf dem Server aufrufen
- [ ] `tickDeath` auf die Länge der Todesanimation angepasst

## Zum Ausprobieren

- Spiel bei jedem Treffer zufällig `hurt_light` oder `hurt_heavy` (je nach Schadenshöhe).
- Lass den Mob nach dem Tod 5 Sekunden liegen, bevor er verschwindet.
- Gib dem Movement-Controller 0 Übergangs-Ticks und vergleiche, wie hart die Wechsel wirken.
