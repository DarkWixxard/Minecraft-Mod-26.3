# 34 – `implements`: Interfaces (Schnittstellen)

Neben `extends` ([33](33-extends.md)) steht oft noch ein zweites Wort in der Klassenzeile:

```java
public class WyvernEntity extends Monster implements GeoEntity { … }
```

Dieses Kapitel erklärt, was ein **Interface** ist, was `implements` bedeutet, wie man eigene Interfaces schreibt und wo Minecraft sie überall benutzt.

> ✅ Getestet mit Forge 26.3-66.1.0. Alle Fehlermeldungen wurden absichtlich erzeugt (Compiler auf Deutsch, wie in IntelliJ). Das Beispiel mit dem eigenen Interface `Chargeable` (Abschnitt 4) lief auf einem Server, die Ausgaben stehen dabei. Welche Interfaces die Minecraft-Klassen haben, wurde aus den echten Klassen ausgelesen.

## 1. Was ist ein Interface?

Ein Interface ist eine **Liste von Fähigkeiten**: „Wer mich implementiert, **kann** diese Methoden.“ Es sagt nur **was** – das **wie** schreibt die Klasse selbst.

```java
public interface ItemLike {          // aus Minecraft
    Item asItem();                   // keine geschweiften Klammern, kein Inhalt
}
```

Jede Klasse, die `implements ItemLike` schreibt, **verspricht**: „Ich habe eine Methode `asItem()`.“ Der Compiler prüft das Versprechen.

| | Klasse (`extends`) | Interface (`implements`) |
|---|---|---|
| bedeutet | „**ist ein** …“ | „**kann** …“ / „hält sich an …“ |
| wie viele? | genau **eine** | **beliebig viele**, mit Komma getrennt |
| bringt Code mit? | ja, alles Geerbte | nur `default`-Methoden |
| eigene Felder? | ja | nur Konstanten (`static final`) |
| `new` möglich? | ja (wenn nicht `abstract`) | **nein** |

Bild dazu: `extends` ist die **Familie** (ein Wyvern ist ein Monster ist ein Mob …), `implements` sind **Führerscheine**: Man kann mehrere haben, und jeder verlangt, dass man bestimmte Dinge beherrscht.

## 2. Schreibweise

```java
public class MeineKlasse extends Elternklasse implements InterfaceA, InterfaceB {
    …
}
```

- `extends` kommt **zuerst**, dann `implements`.
- Mehrere Interfaces mit **Komma**.
- Man kann auch **nur** `implements` schreiben (dann ist die Elternklasse automatisch `Object`).

## 3. Was „implementieren“ heißt: die Pflicht-Methoden

Ein Interface hat zwei Sorten Methoden:

| Sorte | erkennt man an | musst du schreiben? |
|---|---|---|
| **abstrakt** | kein Inhalt, nur `;` | **ja**, jede einzelne |
| **`default`** | Wort `default` und Inhalt `{ … }` | nein – kannst du aber überschreiben |
| `static` | Wort `static` | nein – gehört zum Interface selbst (`Interface.methode()`) |

Beispiel aus Minecraft (ausgelesen aus 26.3):

```java
public interface RangedAttackMob {
    void performRangedAttack(LivingEntity target, float power);       // Pflicht
    default float rangedAttackUncertainty(Level level) { … }          // geschenkt
}
```

Fehlt eine Pflicht-Methode (getestet):

```java
class MyArcher extends Monster implements RangedAttackMob { … }   // ohne performRangedAttack
```

```
Fehler: MyArcher ist nicht abstrakt und setzt die abstrakte Methode
        performRangedAttack(LivingEntity,float) in RangedAttackMob nicht außer Kraft
```

„Setzt … nicht außer Kraft“ ist die (holprige) Übersetzung von „does not override“. **Lösung:** Alt + Enter auf den roten Klassennamen → *Implement methods* – IntelliJ legt alle Pflicht-Methoden leer an, mit `@Override` ([25](25-Override.md)).

### Interface-Methoden sind immer `public`

Alle Methoden in einem Interface sind automatisch `public`. Deine Version **muss** auch `public` sein (getestet):

```java
class MyThing implements ItemLike {
    Item asItem() { return null; }     // "public" vergessen
}
```

```
Fehler: asItem() in MyThing kann nicht asItem() in ItemLike implementieren
  Versuch, niedrigere Zugriffsberechtigungen zuzuweisen. War public
```

## 4. Ein eigenes Interface schreiben

Wann lohnt sich das? Wenn **verschiedene** Klassen, die **nicht** voneinander erben, eine gemeinsame Fähigkeit haben sollen – z. B. „hat Ladungen“ für einen Zauberstab-Item **und** einen Block **und** ein Mob.

```java
package com.example.mymod;

public interface Chargeable {
    int getMaxCharges();                       // muss jede Klasse liefern

    default String describeCharges() {         // bekommt jede Klasse geschenkt
        return "max " + getMaxCharges() + " Ladungen";
    }
}
```

```java
public class ChargedWandItem extends Item implements Chargeable {
    private final int maxCharges;

    public ChargedWandItem(Properties properties, int maxCharges) {
        super(properties);
        this.maxCharges = maxCharges;
    }

    @Override
    public int getMaxCharges() {
        return maxCharges;
    }
}
```

Benutzen – egal, **welche** Klasse dahintersteckt:

```java
Item item = stack.getItem();
if (item instanceof Chargeable c) {
    player.sendSystemMessage(Component.literal(c.describeCharges()));
}
```

Test auf dem Server (zwei Zauberstäbe mit 5 und 3 Ladungen, dazu ein Diamant):

```
fire max 5 Ladungen
ice max 3 Ladungen
diamond chargeable=false
```

Der Code fragt nicht „bist du ein `ChargedWandItem`?“, sondern „**kannst** du Ladungen?“. Kommt später ein Block oder Mob mit `implements Chargeable` dazu, funktioniert derselbe Code ohne Änderung.

## 5. Interfaces als Typ

Ein Interface kann man **nicht** mit `new` erzeugen (getestet):

```java
new ItemLike();
// Fehler: ItemLike ist abstrakt, kann nicht instanziiert werden
```

Man kann es aber als **Typ** für Variablen, Parameter und Listen benutzen. Dann passt **alles** hinein, was das Interface implementiert.

Bestes Beispiel: **`ItemLike`**. Ausgelesen aus Minecraft 26.3:

```
public class Item  implements ItemLike, FeatureElement
public class Block extends BlockBehaviour implements ItemLike
```

Darum geht `new ItemStack(...)` mit Items **und** Blöcken ([31](31-ItemStack.md)) – der Konstruktor will nur ein `ItemLike`:

```java
List<ItemLike> things = List.of(Items.DIAMOND, Blocks.STONE, ModItems.FIRE_WAND.get());
for (ItemLike t : things) {
    System.out.println(new ItemStack(t));
}
```

Test:

```
Item -> 1 minecraft:diamond
Block -> 1 minecraft:stone
ChargedWandItem -> 1 mymod:fire_wand
```

## 6. Wo Minecraft und Bibliotheken Interfaces benutzen

| Interface | wer implementiert es | Pflicht-Methoden | wofür |
|---|---|---|---|
| `ItemLike` | `Item`, `Block` | `asItem()` | alles, woraus ein ItemStack werden kann |
| `Enemy` | `Monster` (und damit alle Monster) | **keine** | nur ein **Markierungs-Interface**: „ich bin feindlich“ – Eisengolems greifen z. B. alles an, was `Enemy` ist (außer Creeper) |
| `RangedAttackMob` | Skelette (`AbstractSkeleton`), Hexen, Schneegolem … | `performRangedAttack` | Fernkampf-Goal `RangedAttackGoal` funktioniert nur mit solchen Mobs |
| `EntityBlock` | `BaseEntityBlock` (Truhe, Ofen …) | `newBlockEntity` | Blöcke mit eigenen Daten (Block-Entity) |
| `SimpleWaterloggedBlock` | Treppen, Truhen … | **keine** (alles `default`) | Block kann Wasser enthalten |
| `MenuProvider` | Block-Entities mit GUI | `getDisplayName`, `createMenu` | öffnet ein Inventar-Fenster |
| `GeoEntity` (GeckoLib) | dein Mob ([06](06-Eigener-Mob-mit-GeckoLib.md)) | `registerControllers`, `getAnimatableInstanceCache` | Animationen |
| `GeoItem` (GeckoLib) | deine 3D-Rüstung ([04](04-Ruestung.md)) | dieselben + Renderer | Item mit GeckoLib-Modell |

Außerdem baut Forge eigene Interfaces in Minecraft-Klassen ein (z. B. `IForgeItem` in `Item`) – daher kommen `default`-Methoden wie `onEntitySwing`, die man in eigenen Items überschreiben kann.

### Interfaces erweitern Interfaces

Ein Interface kann **andere Interfaces** mit `extends` (nicht `implements`!) übernehmen:

```
public interface SimpleWaterloggedBlock extends BucketPickup, LiquidBlockContainer
public interface MenuProvider extends MenuConstructor
```

Wer `MenuProvider` implementiert, muss also auch die Pflicht-Methode von `MenuConstructor` (`createMenu`) liefern.

### Markierungs-Interfaces

`Enemy` hat **keine** Methoden (nur ein paar Erfahrungs-Konstanten). Es dient nur dazu, dass man fragen kann:

```java
if (entity instanceof Enemy) { … }    // ist das ein feindliches Wesen?
```

Ein freundliches Mob, das trotzdem als Feind zählen soll, schreibt einfach `implements Enemy` – fertig.

## 7. Lambdas sind auch Interfaces

Jedes `() -> …` und `x -> …` in deiner Mod ist in Wahrheit ein Objekt, das ein Interface mit **genau einer** Pflicht-Methode implementiert (ein „funktionales Interface“):

| du schreibst | Interface dahinter | Pflicht-Methode |
|---|---|---|
| `ITEMS.register("x", () -> new Item(…))` | `Supplier<Item>` | `get()` |
| `item("x", p -> p.stacksTo(16))` | `UnaryOperator<Item.Properties>` | `apply(p)` |
| `ServerStartedEvent.BUS.addListener(event -> { … })` | `Consumer<ServerStartedEvent>` | `accept(event)` |
| `.executes(ctx -> { … return 1; })` ([13](13-Befehle.md)) | `Command<CommandSourceStack>` | `run(ctx)` |
| `EntityType.Builder.of(WyvernEntity::new, …)` | `EntityType.EntityFactory<T>` | `create(type, level)` |

Lambdas sind nur eine **Kurzschreibweise** für „eine kleine Klasse, die dieses Interface implementiert“.

## 8. Typische Fehler

| Fehlermeldung (Deutsch, getestet) | Ursache | Lösung |
|---|---|---|
| *Hier wird eine Schnittstelle erwartet* | `implements Item` – `Item` ist eine **Klasse** | `extends Item` |
| *Hier wird keine Schnittstelle erwartet* | `extends ItemLike` – `ItemLike` ist ein **Interface** | `implements ItemLike` |
| *… ist nicht abstrakt und setzt die abstrakte Methode … nicht außer Kraft* | Pflicht-Methode fehlt | Alt + Enter → *Implement methods* |
| *… kann nicht … implementieren – Versuch, niedrigere Zugriffsberechtigungen zuzuweisen. War public* | `public` vergessen | `public` davor |
| *… ist abstrakt, kann nicht instanziiert werden* | `new` auf ein Interface | eine Klasse erzeugen, die es implementiert |

„Schnittstelle“ ist das deutsche Wort für Interface. **Klasse oder Interface?** Strg + Klick auf den Namen: Oben steht entweder `class` oder `interface`. In IntelliJ haben Interfaces außerdem ein grünes **I**-Symbol, Klassen ein blaues **C**.

## Checkliste

- [ ] Klasse nach `extends`, Interfaces nach `implements`
- [ ] alle Pflicht-Methoden geschrieben (Alt + Enter → *Implement methods*)
- [ ] jede Interface-Methode `public` und mit `@Override`
- [ ] im Code mit `instanceof Interface x` prüfen statt mit einer bestimmten Klasse

## Zum Ausprobieren

- Öffne `RangedAttackGoal` (Strg + N). Welchen Typ hat der Mob im Konstruktor? Was passiert, wenn dein Mob `RangedAttackMob` nicht implementiert?
- Schreib ein Interface `Glowing` mit `int getGlowLevel()` und implementiere es in einem Item und einem Block. Gib in einem Event für beide die Stufe aus – mit **einer** `instanceof`-Prüfung.
- Schreib `implements Enemy` an ein friedliches eigenes Mob und beobachte, ob Eisengolems es jetzt angreifen.
