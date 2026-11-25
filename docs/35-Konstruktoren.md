# 35 – Konstruktoren (am Beispiel von Items)

Jede eigene Item-, Block- oder Mob-Klasse beginnt mit einem **Konstruktor**:

```java
public VenomSwordItem(Properties properties) {
    super(properties);
}
```

Dieses Kapitel erklärt, was ein Konstruktor ist, was `Item.Properties` sind, wie man **eigene Parameter** hinzufügt, warum der Konstruktor nur **einmal** läuft – und alle Fehler, die dabei typisch sind.

> ✅ Getestet mit Forge 26.3-66.1.0 und Java 25. Das Zauberstab-Beispiel (Abschnitte 4–6) lief auf einem Server, die Ausgaben stehen dabei. Alle Fehlermeldungen wurden absichtlich erzeugt (Compiler auf Deutsch, wie in IntelliJ). Die Liste der `Item.Properties`-Methoden stammt aus der echten Minecraft-Klasse.

## 1. Was ist ein Konstruktor?

Ein Konstruktor ist der **Bauplan-Code**, der läuft, wenn mit `new` ein Objekt entsteht:

```java
new ChargedWandItem(properties, 5)   // ← ruft den Konstruktor auf
```

Er sieht aus wie eine Methode, hat aber zwei Besonderheiten:

| | Methode | Konstruktor |
|---|---|---|
| Name | frei wählbar | **genau** wie die Klasse |
| Rückgabetyp | `void`, `int`, `ItemStack` … | **keiner** – auch nicht `void` |
| wird aufgerufen | beliebig oft, mit `objekt.methode()` | **einmal pro Objekt**, mit `new` |
| wird vererbt | ja | **nein** – jede Klasse braucht eigene |

Seine Aufgabe: das neue Objekt **einrichten** – die Elternklasse einrichten lassen (`super(...)`) und die eigenen Felder mit Startwerten füllen.

**In IntelliJ:** In der Klasse Alt + Einfg (Alt + Insert) → *Constructor*. Oder Alt + Enter auf den rot unterstrichenen Klassennamen → *Create constructor matching super*.

## 2. Der Standard-Konstruktor für Items

```java
public class VenomSwordItem extends Item {
    public VenomSwordItem(Properties properties) {
        super(properties);
    }
}
```

Zeile für Zeile:

- `public` – damit `ModItems` (eine andere Klasse) ihn aufrufen darf.
- `VenomSwordItem` – Name = Klassenname.
- `(Properties properties)` – der Konstruktor **bekommt** die Eigenschaften von außen. `Properties` ist hier kurz für `Item.Properties` (weil die Klasse von `Item` erbt, kennt sie den Namen).
- `super(properties);` – gibt sie an den Konstruktor von `Item` weiter. `Item` speichert daraus Stapelgröße, Haltbarkeit, ID usw.

Registriert wird so ([02](02-Bloecke-Items-Werkzeuge.md)):

```java
public static final RegistryObject<Item> VENOM_SWORD = ITEMS.register("venom_sword",
        () -> new VenomSwordItem(new Item.Properties()
                .setId(ITEMS.key("venom_sword"))
                .sword(ModMaterials.SAPPHIRE_TOOL, 3.0F, -2.4F)));
```

Hier sieht man, **wo** der Konstruktor aufgerufen wird: im `() -> new …`. Das Lambda ist ein `Supplier` ([34](34-implements.md#7-lambdas-sind-auch-interfaces)) – Forge ruft es **später** auf, wenn die Items registriert werden, nicht schon beim Laden von `ModItems`.

## 3. `Item.Properties` – die Eigenschaften

`new Item.Properties()` erzeugt ein leeres Eigenschaften-Objekt. Jede Methode ändert eine Eigenschaft und gibt **dasselbe Objekt** zurück – darum kann man sie mit Punkten aneinanderhängen.

Die wichtigsten (alle aus `Item.Properties` in 26.3):

| Methode | bewirkt |
|---|---|
| `setId(ITEMS.key("name"))` | **Pflicht.** Fehlt sie, stürzt das Spiel ab („Item id not set“) |
| `stacksTo(16)` | maximale Stapelgröße |
| `durability(500)` | Haltbarkeit (macht das Item automatisch nicht stapelbar) |
| `rarity(Rarity.RARE)` | Farbe des Namens |
| `fireResistant()` | verbrennt nicht in Lava/Feuer |
| `food(food)` / `food(food, consumable)` | essbar ([02](02-Bloecke-Items-Werkzeuge.md#5-essen), [32](32-Effekte-hinzufuegen.md)) |
| `sword` / `pickaxe` / `axe` / `shovel` / `hoe(material, schaden, tempo)` | Werkzeug ([02](02-Bloecke-Items-Werkzeuge.md#4-werkzeuge)) |
| `humanoidArmor(material, typ)` | Rüstungsteil ([04](04-Ruestung.md)) |
| `enchantable(15)` | verzauberbar am Zaubertisch |
| `repairable(item)` / `repairable(tag)` | im Amboss reparierbar mit … |
| `craftRemainder(item)` | bleibt beim Craften übrig (wie der leere Eimer) |
| `useCooldown(2.0F)` | Abklingzeit in Sekunden nach Benutzung |
| `spawnEgg(entityType)` | Spawn-Ei ([20](20-Spawn-Eier-und-Spawnen.md)) |
| `component(typ, wert)` | beliebige Komponente als Startwert ([31](31-ItemStack.md)) |

### Eigenschaften im Konstruktor festlegen

Wenn eine Eigenschaft **immer** gilt (ein Zauberstab ist **nie** stapelbar), kann man sie auch im Konstruktor setzen, statt sie bei jeder Registrierung zu wiederholen:

```java
public ChargedWandItem(Properties properties, int maxCharges) {
    super(properties.stacksTo(1));      // gilt für jeden Zauberstab
    …
}
```

Faustregel: Was für **alle** Objekte dieser Klasse gleich ist → in den Konstruktor. Was sich **pro Item** unterscheidet (ID, Material, Haltbarkeit) → bei der Registrierung.

## 4. Eigene Parameter

Ein Konstruktor darf beliebig viele Parameter haben. So kann **eine** Klasse mehrere verschiedene Items ergeben:

```java
public class ChargedWandItem extends Item {
    private final int maxCharges;                        // ① Feld

    public ChargedWandItem(Properties properties, int maxCharges) {   // ② Parameter
        super(properties);                               // ③ Elternklasse zuerst
        this.maxCharges = maxCharges;                    // ④ Feld = Parameter
    }

    public int getMaxCharges() {
        return maxCharges;
    }
}
```

① Ein **Feld** speichert den Wert für später. `final` = wird genau einmal gesetzt, danach nie wieder geändert.
② Der Wert kommt als **Parameter** herein.
③ `super(...)` zuerst.
④ `this.maxCharges` ist das **Feld**, `maxCharges` allein der **Parameter**. Weil beide gleich heißen, braucht man `this.`, um sie zu unterscheiden.

Registrierung – zwei Items, eine Klasse:

```java
public static final RegistryObject<Item> FIRE_WAND = ITEMS.register("fire_wand",
        () -> new ChargedWandItem(new Item.Properties().setId(ITEMS.key("fire_wand")), 5));
public static final RegistryObject<Item> FROST_WAND = ITEMS.register("frost_wand",
        () -> new ChargedWandItem(new Item.Properties().setId(ITEMS.key("frost_wand")), 3));
```

Test auf dem Server: `fire_wand` meldet 5 Ladungen, `frost_wand` 3.

Typische Extra-Parameter in Mods: Effekt und Dauer bei einem Gift-Schwert, Pfeil-Anzahl beim Bogen, Reichweite eines Zauberstabs, ein Block, den ein Item platziert (so macht es Vanilla bei `BlockItem(Block, Properties)`).

## 5. Der Konstruktor läuft nur **einmal** – nicht pro ItemStack

Das ist die wichtigste Lektion dieses Kapitels. Getestet: zwei Items registriert, danach viele ItemStacks erzeugt:

```
constructorCalls = 2          ← einmal pro registriertem Item, beim Spielstart
same item = true              ← zwei ItemStacks teilen sich DASSELBE Item-Objekt
```

Es gibt also **ein einziges** `ChargedWandItem`-Objekt für „fire_wand“ – egal, ob 1 oder 1000 Spieler einen Zauberstab haben. Alle ItemStacks zeigen darauf ([31](31-ItemStack.md#1-item-oder-itemstack)).

Folgen:

| ✅ in Feldern des Items speichern | ❌ **nicht** in Feldern des Items speichern |
|---|---|
| Werte, die für **alle** Stacks gleich sind: `maxCharges`, Effekt, Reichweite | Werte **pro Stack**: aktuelle Ladungen, Besitzer, Abklingzeit eines bestimmten Stabs |

Würde man `private int currentCharges;` ins Item schreiben, hätten **alle** Spieler zusammen **einen** Ladungs-Zähler. Werte pro Stack gehören in **Daten-Komponenten** des ItemStacks ([31](31-ItemStack.md)), z. B. `stack.set(DataComponents.DAMAGE, …)` oder eine eigene Komponente.

Darum sind Item-Felder fast immer `final`: Sie werden im Konstruktor gesetzt und ändern sich nie.

## 6. Mehrere Konstruktoren und `this(...)`

Eine Klasse darf mehrere Konstruktoren haben, wenn sich die **Parameter** unterscheiden („Überladen“). Mit `this(...)` ruft einer den anderen auf – so steht die eigentliche Arbeit nur an **einer** Stelle:

```java
public ChargedWandItem(Properties properties, int maxCharges) {
    super(properties);
    this.maxCharges = maxCharges;
}

public ChargedWandItem(Properties properties) {
    this(properties, 3);              // Standard: 3 Ladungen
}
```

Getestet: `new ChargedWandItem(props)` ergibt 3 Ladungen.

Regel: In jedem Konstruktor kommt entweder `super(...)` **oder** `this(...)` vor – nicht beides.

## 7. Was darf **vor** `super(...)` stehen? (neu in Java 25)

Früher musste `super(...)` **immer** die allererste Zeile sein. Seit Java 25 darf davor Code stehen – solange er das **noch unfertige Objekt** nicht benutzt. Praktisch zum Prüfen von Parametern (getestet):

```java
public ChargedWandItem(Properties properties, int maxCharges) {
    if (maxCharges < 1) {
        throw new IllegalArgumentException("maxCharges muss mindestens 1 sein");
    }
    super(properties);
    this.maxCharges = maxCharges;
}
```

Methoden des eigenen Objekts darf man davor **nicht** aufrufen – die Elternklasse ist ja noch nicht eingerichtet:

```java
public MyItem(Properties p) {
    System.out.println(this.getDescriptionId());
    super(p);
}
```

```
Fehler: this kann nicht referenziert werden, bevor der Supertypkonstruktor aufgerufen wurde
```

Im Zweifel: `super(...)` zuerst, dann der Rest. Viele Beispiele im Internet sind älter und machen es nur so.

## 8. Blöcke und Mobs – das gleiche Prinzip

**Blöcke** bekommen `BlockBehaviour.Properties` (kurz `Properties`, wenn die Klasse von `Block` erbt). Im Konstruktor setzt man bei Blöcken mit Zuständen außerdem den Start-Zustand ([14](14-Eigene-Block-Klassen.md)):

```java
public SapphireLampBlock(Properties properties) {
    super(properties);
    this.registerDefaultState(this.defaultBlockState().setValue(LIT, false));
}
```

Auch hier ist `setId(BLOCKS.key(...))` Pflicht („Block id not set“), und auch hier gibt es **ein** Block-Objekt für alle platzierten Blöcke – Werte pro Position gehören in den Blockstate oder eine Block-Entity.

**Mobs und Geschosse** bekommen `(EntityType, Level)`. Diese Form ist **Pflicht**, weil `EntityType.Builder.of(WyvernEntity::new, …)` genau diese Parameter liefert ([06](06-Eigener-Mob-mit-GeckoLib.md)):

```java
public WyvernEntity(EntityType<? extends Monster> type, Level level) {
    super(type, level);
}
```

Anders als bei Items läuft ein Entity-Konstruktor **für jedes einzelne Mob**, das spawnt oder geladen wird. Felder in einer Entity sind deshalb **pro Mob** – genau richtig für Leben, Ziel, Flug-Zustand. Zusätzliche Konstruktoren (z. B. für einen Pfeil mit Schütze, [31](31-ItemStack.md#9-itemstack-als-rückgabewert-überschriebene-methoden)) sind erlaubt, der `(EntityType, Level)`-Konstruktor muss aber bleiben.

| Klasse | Konstruktor bekommt | läuft wie oft? | Felder gelten für |
|---|---|---|---|
| `Item` | `Item.Properties` | einmal beim Start | alle Stacks dieses Items |
| `Block` | `BlockBehaviour.Properties` | einmal beim Start | alle Blöcke dieser Sorte |
| `Entity` | `EntityType`, `Level` | bei jedem Spawn/Laden | dieses eine Mob |

## 9. Typische Fehler

| Fehlermeldung (Deutsch, getestet) | Ursache | Lösung |
|---|---|---|
| *Konstruktor Item in Klasse Item kann nicht auf die angegebenen Typen angewendet werden. Erforderlich: Properties – Ermittelt: keine Argumente* | kein Konstruktor **oder** versehentlich `public void MyItem(...)` geschrieben (mit `void` ist es eine Methode!) | Konstruktor ohne Rückgabetyp, mit `super(properties)` |
| *Ungültige Methodendeklaration. Rückgabetyp erforderlich* | Konstruktor-Name ≠ Klassenname (Tippfehler, Klasse umbenannt) | Namen angleichen (Umbenennen immer mit Umschalt + F6) |
| *Variable seconds wurde möglicherweise nicht initialisiert* | `final`-Feld im Konstruktor nicht gesetzt | `this.seconds = seconds;` im Konstruktor |
| *Konstruktor X in Klasse X kann nicht … angewendet werden. Erforderlich: Properties,int – Ermittelt: Properties* | bei der Registrierung einen Parameter vergessen | `new X(props, 5)` |
| *this kann nicht referenziert werden, bevor der Supertypkonstruktor aufgerufen wurde* | eigene Methode vor `super(...)` benutzt | `super(...)` nach oben |
| Absturz beim Start: *Item id not set* / *Block id not set* | `.setId(...)` fehlt | `.setId(ITEMS.key("name"))` – Name genau wie in `register` |

**Tipp:** Strg + P mit dem Cursor in den Klammern von `new X(…)` zeigt, welche Parameter die Konstruktoren erwarten.

## Checkliste

- [ ] Konstruktor heißt genau wie die Klasse, **kein** Rückgabetyp
- [ ] `super(...)` mit den Parametern, die die Elternklasse erwartet
- [ ] eigene Parameter in `final`-Felder speichern (`this.feld = feld;`)
- [ ] bei Items/Blöcken: nur Werte, die für **alle** Stacks/Blöcke gelten
- [ ] bei der Registrierung: `.setId(...)` und alle eigenen Parameter übergeben
- [ ] bei Entities: `(EntityType, Level)`-Konstruktor vorhanden

## Zum Ausprobieren

- Schreib ein `PoisonSwordItem` mit den Parametern `Holder<MobEffect> effect` und `int seconds` und registriere damit ein Gift- und ein Schwäche-Schwert (Tipp: `hurtEnemy` überschreiben, [32](32-Effekte-hinzufuegen.md)).
- Füge in `ChargedWandItem` ein `System.out.println("Konstruktor!")` ein und starte das Spiel. Wie oft erscheint die Zeile? Ändert sich das, wenn du dir 64 Stäbe gibst?
- Öffne `BlockItem` (Strg + N) und sieh dir an, wie Vanilla den Block als Extra-Parameter im Konstruktor speichert.
