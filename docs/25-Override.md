# 25 – `@Override`

`@Override` steht in diesem Leitfaden über fast jeder zweiten Methode. Dieses Kapitel erklärt, was es bedeutet, warum es so wichtig ist, und wie man die Fehlermeldungen dazu liest.

> ✅ Getestet mit Forge 26.3-66.1.0: Alle Fehlermeldungen unten wurden absichtlich erzeugt und sind echte Compiler-Ausgaben – auf **Deutsch** (wie in IntelliJ) und Englisch. Das Vererbungs-Beispiel in [Abschnitt 7](#7-override-und-subscribeevent-zusammen) lief auf einem Server.

## 1. Was ist „Überschreiben“?

Wenn deine Klasse eine andere **erweitert** (`extends`), erbt sie alle Methoden der Elternklasse. **Überschreiben** heißt: Du schreibst eine Methode mit **genau demselben Namen und denselben Parametern** neu – dann wird **deine** Version benutzt statt der geerbten.

```java
public class SpeedPadBlock extends Block {          // erbt alles von Block

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        // DEINE Version: Minecraft ruft jetzt diese auf, wenn jemand auf den Block tritt
        super.stepOn(level, pos, state, entity);    // optional: die Version von Block auch ausführen
    }
}
```

Das ist der Kern des Moddings: Minecraft ruft bei jedem Block `stepOn(...)` auf. Bei normalen Blöcken passiert nichts (leere Methode in `Block`), bei **deinem** Block läuft dein Code.

Genauso bei Interfaces (`implements`): Ein Interface verlangt Methoden (z. B. `GeoEntity` verlangt `getAnimatableInstanceCache()`), und deine Klasse liefert sie. Auch dafür schreibt man `@Override`.

## 2. Was `@Override` macht – und was nicht

`@Override` ist eine **Annotation** (eine Markierung mit `@`). Sie ändert **nichts** am Verhalten des Programms. Sie sagt dem Compiler nur:

> „Ich **will** hier eine Methode der Elternklasse überschreiben. Prüf bitte, ob es die wirklich gibt.“

Ohne `@Override`:

```java
public class SpeedPadBlock extends Block {
    public void stepon(Level level, BlockPos pos, BlockState state, Entity entity) { ... }   // Tippfehler: stepon
}
```

Das **kompiliert ohne Fehler**. Java denkt, du willst eine **neue** Methode `stepon` anlegen. Minecraft ruft aber `stepOn` auf – deine Methode läuft **nie**, und du suchst stundenlang den Fehler.

Mit `@Override` meldet der Compiler sofort:

```
Fehler: Methode überschreibt oder implementiert keine Methode aus einem Supertyp
error: method does not override or implement a method from a supertype
```

**Darum: Immer `@Override` hinschreiben, wenn du überschreiben willst.** Es ist eine kostenlose Versicherung gegen Tippfehler und gegen Methoden, die in einer neuen Minecraft-Version umbenannt wurden.

## 3. Die Regeln – und die echten Fehlermeldungen

Damit eine Methode wirklich überschreibt, muss alles passen:

| Regel | Falsch | Fehlermeldung (Deutsch / Englisch) |
|---|---|---|
| **Name** exakt gleich (Groß-/Kleinschreibung!) | `hurtSever` statt `hurtServer` | *Methode überschreibt oder implementiert keine Methode aus einem Supertyp* / *method does not override or implement a method from a supertype* |
| **Parameter-Typen** gleich, in gleicher Reihenfolge | `int amount` statt `float amount` | dieselbe Meldung |
| **Rückgabetyp** gleich (oder ein Untertyp) | `int isPickable()` statt `boolean` | *isPickable() in TypoMob kann nicht isPickable() in LivingEntity außer Kraft setzen – Rückgabetyp int ist nicht mit boolean kompatibel* / *return type int is not compatible with boolean* |
| **Zugriff** gleich oder **offener** | `private` statt `protected` | *… kann nicht … außer Kraft setzen – Versuch, niedrigere Zugriffsberechtigungen zuzuweisen. War protected* / *attempting to assign weaker access privileges; was protected* |
| Methode darf nicht **`final`** sein | eine `final`-Methode überschreiben | *… kann nicht … außer Kraft setzen – Außer Kraft gesetzte Methode ist final* / *overridden method is final* |
| **`static`**-Methoden kann man nicht überschreiben | – | sie gehören zur Klasse, nicht zum Objekt |

„Außer Kraft setzen“ ist die deutsche Übersetzung von „override“.

### Zugriff: offener ist erlaubt

| Elternklasse | erlaubt in deiner Klasse |
|---|---|
| `public` | nur `public` |
| `protected` | `protected` oder `public` |
| (ohne Angabe) | ohne Angabe, `protected` oder `public` |
| `private` | gar nicht überschreibbar (unsichtbar) |

Beispiel aus diesem Leitfaden: `tickDeath()` ist in `LivingEntity` `protected` – also schreibt man `protected void tickDeath()` ([08](08-Mob-Animationen.md#5-todesanimation-abwarten)).

### Vergessene Pflicht-Methoden

Bei `implements` (oder einer `abstract`-Elternklasse) **musst** du bestimmte Methoden liefern:

```java
public class WyvernEntity extends Monster implements GeoEntity {
    // getAnimatableInstanceCache() vergessen
}
```

```
Fehler: NoCache ist nicht abstrakt und setzt die abstrakte Methode getAnimatableInstanceCache() in GeoAnimatable nicht außer Kraft
error: NoCache is not abstract and does not override abstract method getAnimatableInstanceCache() in GeoAnimatable
```

Lösung: **Alt + Enter** auf den Klassennamen → *Implement methods*. IntelliJ legt alle fehlenden Methoden an.

## 4. `super` – die Elternversion trotzdem ausführen

Wer überschreibt, **ersetzt** die Elternversion. Soll sie trotzdem laufen, ruft man sie mit `super.methode(...)` auf:

```java
@Override
public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
    amount *= 0.5F;                                           // vorher: Schaden halbieren
    boolean hurt = super.hurtServer(level, source, amount);   // Vanilla: Leben abziehen, Rückstoß, Sound …
    if (hurt) { /* nachher: z. B. Animation */ }
    return hurt;
}
```

| Muster | Wann |
|---|---|
| `super` **zuerst**, dann eigener Code | Vanilla-Verhalten behalten und **ergänzen** (`registerGoals`, `defineSynchedData`, `addAdditionalSaveData`) |
| eigener Code, dann `super` | Werte **vorher ändern** (Schaden verringern) |
| **ohne** `super` | Vanilla-Verhalten **ersetzen** (`tickDeath` mit anderer Dauer, `fallOn` ohne Fallschaden) |

> ⚠️ Bei `defineSynchedData`, `addAdditionalSaveData`, `readAdditionalSaveData` und `registerGoals` (falls die Elternklasse Goals hat) **immer** `super` aufrufen – sonst fehlen Vanilla-Daten, und es kommt zu Abstürzen oder verlorenen Spielständen ([09](09-Mob-Fliegen.md#9-speichern-und-laden)).

## 5. Welche Methoden kann ich überschreiben?

- **Strg + O** (Override Methods) in IntelliJ zeigt **alle** überschreibbaren Methoden der Elternklassen und fügt die gewählte mit richtiger Signatur und `@Override` ein. Das ist der sicherste Weg – so passieren die Fehler aus Abschnitt 3 gar nicht erst.
- Links am Rand zeigt IntelliJ ein kleines **O mit Pfeil nach oben** neben überschreibenden Methoden. Klick darauf springt zur Elternversion – dort steht oft, wann Minecraft die Methode aufruft.

## 6. Nach einem Minecraft-Update

Mojang benennt zwischen Versionen Methoden um oder ändert Parameter (z. B. `hurt(...)` → `hurtServer(ServerLevel, …)`). Mit `@Override` zeigt der Compiler nach dem Update **jede** betroffene Stelle sofort an. Ohne `@Override` kompiliert alles – und funktioniert still nicht mehr.

## 7. `@Override` und `@SubscribeEvent` zusammen

Ein Sonderfall bei Listener-Objekten ([24](24-Listener.md#c-ein-objekt-mit-eigenem-zustand)): Forge sucht `@SubscribeEvent`-Methoden nur in der **Klasse selbst**, nicht in ihren Elternklassen. Meldet man ein Objekt einer Unterklasse an, die nur erbt, kommt:

```
Failed to register …$Child: No declared methods found in …$Child. Note that listener inheritance is not supported.
If you are trying to inherit listeners, please use @Override and @SubscribeEvent on the method in the subclass.
```

Lösung – genau wie die Meldung sagt:

```java
public static class Base {
    @SubscribeEvent public void onJoin(EntityJoinLevelEvent e) { … }
    @SubscribeEvent public void onBreak(BlockEvent.BreakEvent e) { … }
}

public static class Child extends Base {
    @Override @SubscribeEvent
    public void onJoin(EntityJoinLevelEvent e) {
        super.onJoin(e);   // Code von Base läuft auch
        …                  // eigener Code
    }
    @Override @SubscribeEvent
    public void onBreak(BlockEvent.BreakEvent e) {}
}
```

Getestet: Ein neues Schwein → `Base.onJoin` und `Child.onJoin` werden beide ausgegeben.

## Checkliste

- [ ] `@Override` über **jeder** Methode, die überschreiben soll
- [ ] Methode mit **Strg + O** einfügen statt abtippen
- [ ] überlegt: `super` aufrufen (ergänzen) oder nicht (ersetzen)?
- [ ] bei Speichern/Sync-Daten/Goals: `super` nicht vergessen

## Zum Ausprobieren

- Lösch bei einer deiner Methoden das `@Override` und bau einen Tippfehler in den Namen ein. Kompiliert es? Passiert im Spiel etwas? Dann `@Override` wieder hin.
- Öffne `Monster` mit Strg + N und drück darin Strg + O: Wie viele Methoden könntest du überschreiben?
- Überschreib in einem Mob `getAmbientSound()` und `isSunSensitive()` … Gibt es `isSunSensitive` in 26.3 überhaupt? `@Override` sagt es dir.
