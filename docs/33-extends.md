# 33 – `extends`: Klassen erweitern (Vererbung)

`extends` steht in fast jeder Klasse einer Mod: `extends Item`, `extends Block`, `extends Monster`, `extends AbstractArrow` … Dieses Kapitel erklärt, was das Wort bedeutet, was man dabei **erbt**, wie Konstruktoren mit `super(...)` funktionieren, und wie man die **richtige** Elternklasse auswählt.

> ✅ Geprüft mit Forge 26.3-66.1.0: Die Vererbungsketten wurden aus den echten Minecraft-Klassen ausgelesen, alle Fehlermeldungen wurden absichtlich erzeugt (Compiler auf Deutsch, wie in IntelliJ). Die positiven Beispiele stammen aus früheren Kapiteln und liefen dort auf einem Server.

## 1. Was `extends` bedeutet

```java
public class IceArrowEntity extends AbstractArrow { … }
```

heißt: **„Ein `IceArrowEntity` ist ein `AbstractArrow` – mit ein paar Änderungen.“**

- `IceArrowEntity` ist die **Unterklasse** (Kind, *subclass*).
- `AbstractArrow` ist die **Elternklasse** (Oberklasse, *superclass*).
- Die Unterklasse **erbt alles** von der Elternklasse: alle Methoden und Felder, die nicht `private` sind. Ein Pfeil, der `AbstractArrow` erweitert, kann also schon fliegen, stecken bleiben, Schaden machen und aufgehoben werden – ohne eine einzige Zeile eigenen Code.
- Dann **ändert** (überschreibt, [25](25-Override.md)) oder **ergänzt** man nur das, was anders sein soll.

Das ist der Kern des Moddings: **Man baut nicht alles neu, sondern erweitert, was Minecraft schon hat.**

### „ist ein“ – der Test

`extends` passt, wenn der Satz „X **ist ein** Y“ stimmt:

| Klasse | „ist ein …“ | stimmt? |
|---|---|---|
| `IceArrowEntity extends AbstractArrow` | Ein Eis-Pfeil ist ein Pfeil | ✅ |
| `VenomSwordItem extends Item` | Ein Giftschwert ist ein Item | ✅ |
| `SpeedPadBlock extends Block` | Eine Tempo-Platte ist ein Block | ✅ |
| `WyvernEntity extends Monster` | Ein Wyvern ist ein Monster | ✅ |
| `IceArrowItem extends ArrowItem` mit Code für einen **fliegenden** Pfeil | Das Inventar-Item ist ein fliegender Pfeil? | ❌ – das sind zwei Dinge, zwei Klassen ([31](31-ItemStack.md#9-itemstack-als-rückgabewert-überschriebene-methoden)) |

## 2. Die Vererbungskette

Eine Elternklasse kann selbst wieder eine Elternklasse haben. So entsteht eine **Kette** – und deine Klasse erbt von **allen** darüber. Ausgelesen aus Minecraft 26.3:

```
Zombie → Monster → PathfinderMob → Mob → LivingEntity → Entity
Pig    → Animal → AgeableMob → PathfinderMob → Mob → LivingEntity → Entity
Arrow  → AbstractArrow → Projectile → Entity
BowItem           → ProjectileWeaponItem → Item
SpectralArrowItem → ArrowItem → Item
SlimeBlock        → HalfTransparentBlock → Block → BlockBehaviour
DropExperienceBlock → Block → BlockBehaviour
```

(Ganz oben stehen bei Entities noch Forge-Klassen für „Capabilities“ – die kann man für den Anfang ignorieren.)

Was erbt welche Ebene? Am Beispiel deines Wyverns (`extends Monster`):

| Ebene | bringt mit |
|---|---|
| `Entity` | Position, Bewegung, Hitbox, ID, Speichern, Feuer, Wasser |
| `LivingEntity` | Leben, Schaden (`hurtServer`), Effekte (`addEffect`), Rüstung, Tod (`tickDeath`) |
| `Mob` | KI-Goals, Navigation, Ziele, Despawn, Ausrüstung in der Hand |
| `PathfinderMob` | Wegfindung mit Bewertung („wo laufe ich gern hin“) |
| `Monster` | feindlich, Spawn-Regeln für Monster, Angriffs-Attribute |
| **`WyvernEntity`** | nur noch: Fliegen, Animationen, Bossbar … |

**In IntelliJ:** Klasse öffnen und **Strg + H** (Type Hierarchy) zeigt die ganze Kette. Auf einen Namen in der Kette klicken öffnet die Klasse.

## 3. Konstruktoren und `super(...)`

Konstruktoren werden **nicht** vererbt. Jede Klasse braucht eigene – und **die erste Zeile** muss den Konstruktor der Elternklasse aufrufen: `super(...)`.

```java
public class VenomSwordItem extends Item {
    public VenomSwordItem(Properties properties) {
        super(properties);        // ← ruft den Konstruktor von Item auf
    }
}
```

Die Parameter von `super(...)` müssen zu **einem Konstruktor der Elternklasse passen**. Welche es gibt: Strg + Klick auf die Elternklasse und nach `public NameDerKlasse(` suchen.

| Elternklasse | typischer Konstruktor |
|---|---|
| `Item`, `ArrowItem`, `BowItem` … | `(Item.Properties properties)` |
| `Block` | `(BlockBehaviour.Properties properties)` |
| `Monster`, `Animal`, `Zombie` … | `(EntityType<? extends …> type, Level level)` |
| `AbstractArrow` | `(EntityType, Level)` und `(EntityType, LivingEntity owner, Level, ItemStack pickup, ItemStack weapon)` |
| `MobEffect` | `(MobEffectCategory category, int color)` |

### Die echten Fehlermeldungen

**Falsche Parameter für `super`** – z. B. Entity-Parameter in einer Item-Klasse:

```java
public class IceArrowItem extends ArrowItem {
    public IceArrowItem(EntityType<? extends Arrow> type, Level level) { super(type, level); }
}
```

```
Fehler: Konstruktor ArrowItem in Klasse ArrowItem kann nicht auf die angegebenen Typen angewendet werden.
  Erforderlich: Properties
  Ermittelt:    EntityType<CAP#1>,Level
```

„Erforderlich“ = was die Elternklasse erwartet, „Ermittelt“ = was du übergeben hast.

**Gar kein Konstruktor:**

```java
public class IceArrowItem extends ArrowItem { }
```

```
Fehler: Konstruktor ArrowItem in Klasse ArrowItem kann nicht auf die angegebenen Typen angewendet werden.
  Ermittelt:    keine Argumente
```

Ohne eigenen Konstruktor versucht Java automatisch `super()` **ohne** Argumente – das gibt es bei `ArrowItem` nicht. **Lösung:** Alt + Enter auf den Klassennamen → *Create constructor matching super*.

## 4. Was erbt man – und was nicht?

| In der Elternklasse | in der Unterklasse benutzbar? | überschreibbar? |
|---|---|---|
| `public` | ✅ | ✅ |
| `protected` | ✅ | ✅ |
| (ohne Angabe) | nur im selben Paket – bei Minecraft-Klassen also praktisch **nein** | ebenso |
| `private` | ❌ – **unsichtbar** | ❌ |
| `final` (Methode) | ✅ benutzen | ❌ |
| `static` | ✅ (gehört zur Klasse, nicht vererbt im eigentlichen Sinn) | ❌ |

**Private ist wirklich unsichtbar** (getestet): Wer in einer Unterklasse von `Arrow` die private Methode `updateColor()` aufruft, bekommt nicht „keine Berechtigung“, sondern:

```
Fehler: Symbol nicht gefunden
  Symbol: Methode updateColor()
```

Deshalb funktioniert Code, den man aus einer Vanilla-Klasse **kopiert**, oft nicht: Er benutzt private Felder und Methoden dieser Klasse, die deine Unterklasse nicht sieht. Lösung: nicht kopieren, sondern die Vanilla-Klasse **erweitern** und nur das Nötige überschreiben – oder die privaten Teile selbst neu schreiben.

**Methoden einer anderen Sorte gibt es nicht:** Eine Item-Klasse kennt keine Entity-Methoden.

```java
public class IceArrowItem extends ArrowItem {
    void test() { this.level(); }   // level() gibt es bei Entities, nicht bei Items
}
```

```
Fehler: Symbol nicht gefunden
  Symbol: Methode level()
```

## 5. `abstract` und `final`

### Abstrakte Klassen: unfertige Vorlagen

```java
public abstract class AbstractArrow extends Projectile {
    protected abstract ItemStack getDefaultPickupItem();   // ohne Inhalt!
}
```

- Von einer `abstract`-Klasse kann man **kein Objekt** erzeugen (`new AbstractArrow(...)` geht nicht) – nur **erweitern**.
- `abstract`-Methoden **muss** die Unterklasse liefern. Sonst: *„… ist nicht abstrakt und setzt die abstrakte Methode getDefaultPickupItem() in AbstractArrow nicht außer Kraft“* ([31](31-ItemStack.md)).
- Abstrakte Minecraft-Klassen, die man oft erweitert: `AbstractArrow`, `Monster`, `PathfinderMob`, `Animal`, `Projectile`, `MobEffect` (mit `protected` Konstruktor).

### Finale Klassen: nicht erweiterbar

```java
public final class ItemStack … { }
```

```java
public class MyStack extends ItemStack { }
// Fehler: Erben aus finalem ItemStack-Element nicht möglich
```

`final` heißt: „Diese Klasse ist fertig, nicht erweitern.“ Statt `ItemStack` zu erweitern, benutzt man ihn ([31](31-ItemStack.md)).

## 6. Nur **eine** Elternklasse

Java erlaubt genau **eine** Klasse nach `extends`:

```java
public class Thing extends Item, Block { }   // Fehler: '{' erwartet
```

Für „kann zusätzlich X“ gibt es **Interfaces** mit `implements` – davon beliebig viele:

```java
public class WyvernEntity extends Monster implements GeoEntity { … }
public class CrystalArmorItem extends Item implements GeoItem { … }
```

| | `extends` | `implements` |
|---|---|---|
| hinter … | Klasse | Interface |
| wie viele? | genau eine | beliebig viele, mit Komma |
| bedeutet | „**ist ein** …“ | „**kann** …“ / „hält sich an …“ |
| erbt Code? | ja | nur `default`-Methoden; die anderen musst du liefern |
| Beispiel | `extends Monster` | `implements GeoEntity` (verlangt `registerControllers`, `getAnimatableInstanceCache`) |

## 7. Welche Klasse soll ich erweitern?

Die **nächste passende** Vanilla-Klasse – je mehr sie schon kann, desto weniger musst du schreiben.

| Du willst … | erweitere | Kapitel |
|---|---|---|
| ein einfaches Item mit eigenem Verhalten | `Item` | [05](05-Eigene-Item-Klassen.md) |
| einen Bogen | `BowItem` | [05](05-Eigene-Item-Klassen.md) |
| ein Pfeil-Item | `ArrowItem` | [31](31-ItemStack.md) |
| einen fliegenden Pfeil | `AbstractArrow` | [31](31-ItemStack.md) |
| einen Block mit Verhalten | `Block` | [14](14-Eigene-Block-Klassen.md) |
| einen Erz-Block mit Erfahrung | `DropExperienceBlock` (meist ohne eigene Klasse) | [02](02-Bloecke-Items-Werkzeuge.md) |
| ein feindliches Mob | `Monster` | [06](06-Eigener-Mob-mit-GeckoLib.md) |
| ein Tier (vermehrbar) | `Animal` | – |
| einen Zombie mit Extra | `Zombie` | [32](32-Effekte-hinzufuegen.md#48-mob-beim-angriff) |
| ein Teil eines Mehrteil-Mobs | `PartEntity<DeinMob>` | [07](07-Mob-Hitboxen.md) |
| einen Statuseffekt | `MobEffect` | [16](16-Effekte-und-Traenke.md) |
| ein KI-Verhalten | `Goal` | [09](09-Mob-Fliegen.md) |
| ein eigenes veränderbares Event | `MutableEvent` | [23](23-Events.md#7-eigene-events) |
| einen Reload-Listener | `SimpleJsonResourceReloadListener<T>` | [24](24-Listener.md) |

**Vorgehen:** Ähnliches Vanilla-Ding suchen (Strg + N), mit **Strg + H** die Kette ansehen, die passende Stufe wählen, dann mit **Strg + O** gezielt überschreiben.

## 8. `instanceof` – „ist das ein …?“

Weil eine Unterklasse auch **alles ist, was ihre Elternklassen sind**, kann man prüfen:

```java
if (entity instanceof Monster) { … }                    // Zombie, Skelett, Creeper, dein Wyvern …
if (entity instanceof LivingEntity living) {            // prüfen UND gleich als LivingEntity benutzen
    living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60));
}
```

Ein Zombie ist gleichzeitig `Zombie`, `Monster`, `PathfinderMob`, `Mob`, `LivingEntity` und `Entity` – `instanceof` ist für jede dieser Klassen `true`. (So zählte der Monster-Zähler in [24](24-Listener.md#c-ein-objekt-mit-eigenem-zustand) Zombie **und** Skelett.)

`instanceof X x` (mit Name dahinter) prüft und wandelt in einem Schritt um – danach kann man die Methoden von `X` benutzen.

## 9. `extends` in spitzen Klammern

`extends` kommt auch in **Generics** vor – dort heißt es „**oder eine Unterklasse davon**“:

```java
public WyvernEntity(EntityType<? extends Monster> type, Level level)
//                            ↑ ein EntityType für Monster – oder für irgendeine Unterklasse von Monster

public class WyvernRenderer<R extends LivingEntityRenderState & GeoRenderState> …
//                            ↑ R muss beides sein: ein LivingEntityRenderState UND ein GeoRenderState
```

| Schreibweise | Bedeutung |
|---|---|
| `List<Monster>` | genau Monster |
| `EntityType<? extends Monster>` | Monster **oder** Unterklasse (`?` = „irgendeine“) |
| `<T extends Entity>` | ein Platzhalter `T`, der mindestens ein Entity sein muss |
| `<R extends A & B>` | muss A **und** B sein |

Hier wird nichts vererbt – es ist nur eine **Bedingung** an den Typ. Darum steht im Fehler oben auch `EntityType<CAP#1>`: `CAP#1` ist Javas interner Name für „das `?`“.

## 10. Typische Fehler

| Fehlermeldung (Deutsch) | Ursache | Lösung |
|---|---|---|
| *Konstruktor X in Klasse X kann nicht auf die angegebenen Typen angewendet werden* | `super(...)` passt nicht / fehlt | Konstruktor der Elternklasse nachsehen; Alt + Enter → *Create constructor matching super* |
| *Symbol nicht gefunden – Methode …* | Methode ist `private` in der Elternklasse **oder** gehört zu einer anderen Sorte (Entity-Methode im Item) | richtige Klasse erweitern; private Teile selbst schreiben |
| *… ist nicht abstrakt und setzt die abstrakte Methode … nicht außer Kraft* | Pflicht-Methode einer `abstract`-Klasse fehlt | Alt + Enter → *Implement methods* |
| *Erben aus finalem …-Element nicht möglich* | Elternklasse ist `final` | benutzen statt erweitern |
| *'{' erwartet* bei `extends A, B` | zwei Elternklassen | eine Klasse + Interfaces mit `implements` |
| *Methode überschreibt oder implementiert keine Methode aus einem Supertyp* | Methodenname/Parameter passen nicht | [25](25-Override.md) |
| Code aus einer Vanilla-Klasse kopiert, nichts kompiliert | kopierter Code benutzt `private` Teile | erweitern statt kopieren |

## Checkliste

- [ ] „X ist ein Y“ stimmt
- [ ] möglichst spezifische Elternklasse gewählt (Strg + H)
- [ ] Konstruktor mit passendem `super(...)` als erste Zeile
- [ ] `abstract`-Methoden geliefert
- [ ] nur überschrieben, was anders sein soll (`@Override`, Strg + O)
- [ ] Item-Code und Entity-Code in getrennten Klassen

## Zum Ausprobieren

- Öffne `Zombie` und drück **Strg + H**. Welche Klassen erweitern `Zombie` selbst? (Tipp: Wüstenzombie, Ertrunkener …)
- Schreib `class Test extends ArrowItem { }` und lass IntelliJ mit Alt + Enter den Konstruktor erzeugen. Was steht drin?
- Erweitere `Pig` zu einem „Glücksschwein“, das beim Tod einen Smaragd droppt – nur durch Überschreiben einer Methode (Tipp: Strg + O, nach „drop“ suchen).
