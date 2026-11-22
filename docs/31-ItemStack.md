# 31 – `ItemStack` und `new ItemStack(Items.…)`

Diese Zeile kam im Leitfaden schon oft vor:

```java
new ItemStack(Items.EMERALD)
```

Dieses Kapitel erklärt, was dahintersteckt: den Unterschied zwischen **Item** und **ItemStack**, die Klasse **`Items`**, wie man Stacks erzeugt, vergleicht, verändert, mit Namen und Verzauberungen versieht und an Spieler oder in Kisten gibt.

> ✅ Getestet mit Forge 26.3-66.1.0 auf einem Server: Alle Ausgaben, die unten mit „Test:“ gekennzeichnet sind, kommen aus einem Test-Befehl, der genau diesen Code ausgeführt hat. Das Geben an einen Spieler (Abschnitt 8) ist kompiliert und folgt Vanillas `/give`-Code, wurde aber ohne Spieler nicht ausgeführt.

## 1. Item oder ItemStack?

| | `Item` | `ItemStack` |
|---|---|---|
| Was | die **Sorte**: „Diamant“ | ein **konkreter Stapel**: „3 Diamanten“, „Diamantschwert mit Schärfe III, halb kaputt, Name ‚Frostfang‘“ |
| Wie viele gibt es? | **genau eins pro Sorte** im ganzen Spiel | beliebig viele, jeder Slot hat seinen eigenen |
| Enthält | Grundeigenschaften (max. Stapelgröße, Haltbarkeit, Essen …) | Item **+ Anzahl + Komponenten** (Name, Schaden, Verzauberungen …) |
| Veränderbar? | nein | **ja** |
| Beispiel | `Items.DIAMOND` | `new ItemStack(Items.DIAMOND, 3)` |

Vergleich: Das **Item** ist der Eintrag „Diamant“ im Lexikon. Der **ItemStack** ist der Stapel, der gerade in deinem Inventar liegt.

Überall, wo Minecraft mit „echten“ Gegenständen arbeitet – Inventar, Hand, Kiste, Drops, Rezept-Ergebnisse – sind es **ItemStacks**.

## 2. Die Klasse `Items`

`Items` (Paket `net.minecraft.world.item`) enthält **alle Vanilla-Items als Konstanten**:

```java
Items.DIAMOND
Items.DIAMOND_SWORD
Items.EMERALD
Items.APPLE
Items.ENDER_PEARL
Items.AIR            // "nichts"
```

- Name = Item-ID in Großbuchstaben: `minecraft:golden_apple` → `Items.GOLDEN_APPLE`.
- Alle anzeigen: Strg + N → `Items` öffnen, oder `Items.` tippen und auf die Vorschläge warten.
- Für Blöcke gibt es `Blocks` (`Blocks.STONE`). Als Item im Inventar heißt der Stein trotzdem `Items.STONE`.

**Eigene Items** stehen nicht in `Items`, sondern in deiner `ModItems`-Klasse – als `RegistryObject`, deshalb mit `.get()`:

```java
new ItemStack(ModItems.RUBY.get())          // eigenes Item
new ItemStack(Items.DIAMOND)                // Vanilla-Item
```

## 3. ItemStacks erzeugen

```java
ItemStack diamond  = new ItemStack(Items.DIAMOND);             // 1 Diamant
ItemStack emeralds = new ItemStack(Items.EMERALD, 5);          // 5 Smaragde
ItemStack rubies   = new ItemStack(ModItems.RUBY.get(), 3);    // 3 Rubine (eigenes Item)
ItemStack nothing  = ItemStack.EMPTY;                          // "kein Item"
```

Test: `diamond=1 minecraft:diamond emeralds=5 minecraft:emerald rubies=3 rubymod:ruby`

| Konstruktor | Bedeutung |
|---|---|
| `new ItemStack(item)` | Anzahl 1 |
| `new ItemStack(item, anzahl)` | bestimmte Anzahl |
| `new ItemStack(Blocks.STONE)` | geht auch mit Blöcken (alles, was ein `ItemLike` ist) |

### Der leere Stack

`ItemStack.EMPTY` steht für „hier ist nichts“ (leerer Slot, leere Hand). Minecraft benutzt dafür **nie `null`**.

Test:

```
EMPTY.isEmpty=true   EMPTY.getItem=minecraft:air
new ItemStack(Items.AIR).isEmpty=true   new ItemStack(Items.DIAMOND, 0).isEmpty=true
```

→ Ob ein Slot leer ist, immer mit **`stack.isEmpty()`** prüfen – nicht mit `== null` und nicht mit `== ItemStack.EMPTY`.

> ⚠️ `ItemStack.EMPTY` **nie verändern** (kein `setCount`, `set(...)`, `grow`). Es ist ein einziges Objekt, das überall benutzt wird.

## 4. Vergleichen

```java
stack.is(Items.DIAMOND)               // ist es ein Diamant?
stack.is(ItemTags.SWORDS)             // ist es irgendein Schwert (Tag)?
stack.getItem() == Items.DIAMOND      // dasselbe wie is(Items.DIAMOND)
```

Test: `is(DIAMOND)=true  getItem()==DIAMOND=true  sword is(SWORDS)=true`

**Zwei Stacks** miteinander vergleichen – hier wird es tückisch:

```java
ItemStack d1 = new ItemStack(Items.DIAMOND);
ItemStack d2 = new ItemStack(Items.DIAMOND);
```

| Vergleich | Ergebnis (Test) | Bedeutung |
|---|---|---|
| `d1 == d2` | `false` | dasselbe **Objekt**? (nein – zwei Stapel) |
| `d1.equals(d2)` | `false` | ⚠️ bei ItemStack **nicht** inhaltlich! |
| `ItemStack.matches(d1, d2)` | `true` | gleiches Item, gleiche Anzahl, gleiche Komponenten |
| `ItemStack.isSameItem(d1, d2)` | `true` | gleiches Item (Anzahl und Komponenten egal) |
| `ItemStack.isSameItemSameComponents(d1, d2)` | `true` | gleiches Item und gleiche Komponenten (Anzahl egal) – „lassen sie sich stapeln?“ |

Nach `d2.set(DataComponents.CUSTOM_NAME, Component.literal("Shiny"))`:

Test: `isSameItem=true  isSameItemSameComponents=false`

**Merke:** ItemStacks nie mit `==` oder `equals` vergleichen. Für „ist das ein X?“ → `is(...)`, für zwei Stacks → eine der `ItemStack.…`-Methoden.

## 5. Anzahl ändern, teilen, kopieren

| Methode | Wirkung |
|---|---|
| `getCount()` / `setCount(n)` | Anzahl lesen / setzen |
| `grow(n)` / `shrink(n)` | Anzahl erhöhen / verringern |
| `split(n)` | nimmt n **heraus** – der alte Stack wird kleiner, zurück kommt ein neuer mit n |
| `copy()` | unabhängige Kopie |
| `copyWithCount(n)` | Kopie mit anderer Anzahl |
| `getMaxStackSize()` | maximale Stapelgröße (Diamant 64, Schwert 1, Enderperle 16) |
| `isEmpty()` | leer? |

Test:

```java
ItemStack a = new ItemStack(Items.EMERALD, 5);
ItemStack b = a.split(2);            // a=3  b=2
ItemStack c = a.copy();
c.grow(10);                          // a=3  c=13   ← Kopie ist unabhängig
ItemStack same = a;                  // KEINE Kopie, nur ein zweiter Name
same.shrink(1);                      // a=2         ← a hat sich mit verändert!
a.shrink(10);                        // count=0  isEmpty=true
```

### Die wichtigste Falle: ItemStacks sind veränderbar

`ItemStack same = a;` erzeugt **keinen** neuen Stapel – beide Namen zeigen auf **denselben**. Verändert man `same`, verändert man `a`. Deshalb:

> **Bevor du einen Stack weitergibst oder speicherst, der dir nicht gehört (aus einem Inventar, einem Event, einer Konstante): `copy()`.**

Typischer Fehler: `player.getMainHandItem()` holen, `setCount(64)` aufrufen – und damit das Item in der Hand des Spielers verändern.

### Keine automatische Grenze

Test: `new ItemStack(Items.DIAMOND, 100)` → Anzahl **100**, obwohl Diamanten nur bis 64 stapeln.

Der Konstruktor prüft das nicht. Wer Stacks selbst baut, muss auf `getMaxStackSize()` achten – sonst entstehen Stapel, die das Spiel nicht vorsieht (Vanillas `/give` teilt deshalb in mehrere Stapel auf).

## 6. Komponenten: Name, Beschreibung, Schaden, Verzauberungen

Seit 1.20.5 hängen die Zusatzdaten eines Stacks als **Daten-Komponenten** daran (früher: NBT). Die Typen stehen in `DataComponents`.

```java
ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
sword.set(DataComponents.CUSTOM_NAME, Component.literal("Frostfang"));                  // Name
sword.set(DataComponents.LORE, new ItemLore(List.of(Component.literal("Forged in ice")))); // Beschreibungszeile
sword.setDamageValue(100);                                                                // Abnutzung

Holder<Enchantment> sharpness = level.registryAccess()
        .lookupOrThrow(Registries.ENCHANTMENT)
        .getOrThrow(Enchantments.SHARPNESS);
sword.enchant(sharpness, 3);                                                              // Schärfe III
```

Test: `sword damage=100/1561  enchanted=true  sharpness=3  hasName=true`

| Methode | Bedeutung |
|---|---|
| `stack.set(DataComponents.X, wert)` | Komponente setzen |
| `stack.get(DataComponents.X)` | lesen (`null`, wenn nicht vorhanden) |
| `stack.getOrDefault(DataComponents.X, standard)` | lesen mit Standardwert |
| `stack.has(DataComponents.X)` | vorhanden? |
| `stack.remove(DataComponents.X)` | entfernen |

Häufige Komponenten: `CUSTOM_NAME`, `LORE`, `DAMAGE`, `MAX_DAMAGE`, `MAX_STACK_SIZE`, `UNBREAKABLE`, `RARITY`, `ENCHANTMENTS`, `POTION_CONTENTS`, `FOOD`. Alle: Strg + N → `DataComponents`.

### Warum Verzauberungen so kompliziert sind

Verzauberungen sind seit 1.21 **datengetrieben** ([21](21-Verzauberungen.md)) – sie stehen nicht fest im Code, sondern werden mit der Welt geladen. `Enchantments.SHARPNESS` ist deshalb nur ein **Schlüssel** (`ResourceKey`). Den echten Eintrag (`Holder<Enchantment>`) holt man aus den Registries der **Welt** (`level.registryAccess()`). Ohne geladene Welt (z. B. beim Mod-Start) gibt es noch keine Verzauberungen.

### So sieht der Stack gespeichert aus

Den Stack in eine Kiste gelegt und mit `/data get block` ausgelesen (Test):

```
{id: "minecraft:diamond_sword", count: 1, components: {
    "minecraft:custom_name": "Frostfang",
    "minecraft:lore": ["Forged in ice"],
    "minecraft:damage": 100,
    "minecraft:enchantments": {"minecraft:sharpness": 3} }}
```

Genau dieses Format benutzt auch der `/give`-Befehl:

```
/give @s minecraft:diamond_sword[custom_name="Frostfang",damage=100,enchantments={"minecraft:sharpness":3}]
```

Test: Diese Zeile wird vom Server ohne Fehler gelesen. Ein Tippfehler im Komponentennamen (`custom_nam`) ergibt dagegen *„Unknown item component 'minecraft:custom_nam'“* – so findet man falsche Namen schnell.

## 7. Woher man ItemStacks bekommt

| Wo | Code |
|---|---|
| Hand eines Spielers/Mobs | `entity.getMainHandItem()`, `entity.getOffhandItem()`, `entity.getItemInHand(hand)` |
| Rüstung | `entity.getItemBySlot(EquipmentSlot.HEAD)` ([04](04-Ruestung.md#6-set-effekte-volle-rüstung)) |
| Inventar | `player.getInventory().getItem(slot)` |
| Kiste / Behälter | `container.getItem(slot)` |
| Item auf dem Boden | `itemEntity.getItem()` |
| Drops beim Tod | `LivingDropsEvent` → `getDrops()` ([15](15-Spiel-Events.md)) |
| Item, das gerade benutzt wird | Parameter `ItemStack stack` in `use`, `inventoryTick`, `hurtEnemy` … ([05](05-Eigene-Item-Klassen.md)) |

Das sind **die echten Stacks** – Änderungen wirken sofort im Spiel. Nur lesen? Dann ist alles gut. Weitergeben oder speichern? Dann `copy()`.

## 8. ItemStacks in die Welt bringen

### In eine Kiste

```java
if (level.getBlockEntity(chestPos) instanceof Container chest) {
    chest.setItem(0, sword);                                   // Slot 0
    chest.setItem(1, new ItemStack(ModItems.RUBY.get(), 16));  // Slot 1
}
```

Test (Kiste ausgelesen): Slot 0 = das Schwert mit allen Komponenten, Slot 1 = `16 rubymod:ruby`.

### Auf den Boden

```java
Block.popResource(level, pos, new ItemStack(Items.DIAMOND, 3));
```

Test: ein Item-Entity mit `{count: 3, id: "minecraft:diamond"}` lag danach in der Welt. Für Entities außerdem: `entity.spawnAtLocation(level, stack)`.

### An einen Spieler (wie `/give`)

```java
ItemStack stack = new ItemStack(Items.DIAMOND, 3);
player.getInventory().add(stack);        // nimmt so viel wie passt – und VERKLEINERT dabei "stack"
if (!stack.isEmpty()) {                  // Rest passte nicht mehr ins Inventar
    Block.popResource(player.level(), player.blockPosition(), stack);
}
```

`Inventory.add(...)` verändert den übergebenen Stack: Was ins Inventar wandert, wird aus `stack` herausgenommen. Danach steht in `stack` nur noch der **Rest**. Genau so arbeitet Vanillas `/give` (`GiveCommand`): erst `add`, dann prüfen, ob noch etwas übrig ist. Deshalb nie denselben Stack danach noch woanders benutzen.

Immer auf dem **Server** ausführen (`!level.isClientSide()`), sonst taucht das Item nur scheinbar auf.

## 9. Typische Fehler

| Problem | Ursache | Lösung |
|---|---|---|
| `new ItemStack(ModItems.RUBY)` rot | `RegistryObject` statt Item | `ModItems.RUBY.get()` |
| Vergleich klappt nie | `==` oder `equals` | `is(...)`, `ItemStack.matches`, `isSameItem…` |
| `NullPointerException` | leerer Slot mit `null` erwartet | `isEmpty()` prüfen; Minecraft liefert `ItemStack.EMPTY` |
| Item in der Hand ändert sich mit | Stack nicht kopiert | `copy()` |
| Seltsame 100er-Stapel | Anzahl über `getMaxStackSize()` | selbst begrenzen bzw. aufteilen |
| Verzauberung lässt sich nicht holen | `Enchantments.X` ist nur ein Schlüssel / keine Welt geladen | `level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(...)` zur Laufzeit |
| Name verschwindet nach Neustart | auf dem Client gesetzt | auf dem Server setzen |
| `stack.getTag()` / `setTag` gibt es nicht | Code aus der NBT-Zeit (vor 1.20.5) | Daten-Komponenten (`set`/`get`) |

## Checkliste

- [ ] Item (Sorte) vs. ItemStack (Stapel) unterschieden
- [ ] eigene Items mit `.get()`
- [ ] Leer-Prüfung mit `isEmpty()`
- [ ] Vergleiche mit `is(...)` bzw. `ItemStack.…`
- [ ] fremde Stacks vor dem Ändern/Speichern `copy()`
- [ ] Anzahl ≤ `getMaxStackSize()`
- [ ] Änderungen auf dem Server

## Zum Ausprobieren

- Ein Befehl `/mymod kit`, der dem Spieler ein Diamantschwert mit Namen, Beschreibung und Schärfe V gibt.
- Ein Event-Listener ([15](15-Spiel-Events.md)), der beim Abbauen von Diamanterz mit 10 % Chance einen zweiten Diamanten mit dem Namen „Glücksfund“ droppen lässt.
- Lies `GiveCommand` (Strg + N): Wie teilt Vanilla große Mengen auf mehrere Stapel auf?
