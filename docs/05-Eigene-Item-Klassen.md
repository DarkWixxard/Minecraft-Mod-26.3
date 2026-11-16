# 05 – Eigene Item-Klassen

Solange ein Item nur Werte braucht (Schaden, Haltbarkeit, Essen), reicht ein normales `Item` mit Properties. Sobald es **sich anders verhalten** soll, braucht es eine **eigene Klasse**, die eine Vanilla-Klasse erweitert und einzelne Methoden überschreibt.

## 1. Das Vorgehen – für jedes eigene Item

1. **Ähnliches Vanilla-Item suchen.** Bogen → `BowItem`, Armbrust → `CrossbowItem`, Schwert → `Item`, Angel → `FishingRodItem`, Schneeball → `SnowballItem`.
2. **Die Klasse öffnen** (Strg + N) und lesen: Welche Methoden gibt es, welche machen was?
3. **Eigene Klasse** in `item/custom/` anlegen: `public class MeinItem extends VanillaKlasse`.
4. **Nur die Methoden überschreiben**, die anders sein sollen (Strg + O). Immer überlegen: rufe ich `super.methode(...)` auf (Vanilla-Verhalten + meins) oder ersetze ich es ganz?
5. In `ModItems` mit `new MeinItem(...)` registrieren.

Häufig überschriebene Methoden von `Item`:

| Methode | Wann | Beispiel |
|---|---|---|
| `use(Level, Player, InteractionHand)` | Rechtsklick in die Luft | Zauberstab schießt etwas |
| `useOn(UseOnContext)` | Rechtsklick auf einen Block | Meißel verwandelt Block |
| `hurtEnemy(ItemStack, LivingEntity, LivingEntity)` | Treffer mit dem Item | Schwert setzt Gegner in Brand |
| `inventoryTick(ItemStack, ServerLevel, Entity, EquipmentSlot)` | jeden Tick im Inventar | Rüstungs-Effekte ([04](04-Ruestung.md#6-set-effekte-volle-rüstung)) |
| `finishUsingItem(...)` | nach dem Essen/Trinken | Essen mit Spezialwirkung |

Signaturen ändern sich zwischen Versionen. Deshalb immer per **Strg + O** einfügen lassen, nicht aus alten Tutorials abschreiben.

## 2. Beispiel: ein Bogen, der drei Pfeile schießt

**Was er kann:** immer 3 Pfeile (verbraucht nur einen), Pfeile doppelt so schnell, 5× Schaden.

### Registrierung

```java
public static final RegistryObject<Item> TRIPLE_BOW = ITEMS.register("triple_bow",
        () -> new TripleShotBowItem(new Item.Properties().setId(ITEMS.key("triple_bow")).durability(500)));
```

`durability(500)` = 500 Schüsse (Vanilla-Bogen: 384).

### Die Klasse

```java
public class TripleShotBowItem extends BowItem {
    private static final double ARROW_DAMAGE = 10.0;      // normale Pfeile: 2.0 -> 5x Schaden
    private static final float SPEED_MULTIPLIER = 2.0F;   // Pfeile fliegen doppelt so schnell
    private static final float SIDE_ARROW_ANGLE = 10.0F;  // Winkel der zwei Extra-Pfeile (Grad)
    private static final int COOLDOWN_TICKS = 2;          // kleine Pause zwischen Schüssen (20 Ticks = 1 s)

    public TripleShotBowItem(Properties properties) {
        super(properties);
    }
```

`extends BowItem` = er **erbt** alles vom Vanilla-Bogen (Spannen, Pfeile suchen, Verzauberungen) und überschreibt nur drei Methoden. Die Konstanten oben machen das Anpassen leicht.

### Methode 1: `shoot` – drei Pfeile statt einem

```java
@Override
protected void shoot(ServerLevel level, LivingEntity shooter, InteractionHand hand, ItemStack weapon,
                     List<ItemStack> projectiles, float velocity, float inaccuracy, boolean crit, LivingEntity target) {
    List<ItemStack> allProjectiles = new ArrayList<>(projectiles);
    if (projectiles.size() == 1) {
        ItemStack extra = projectiles.get(0).copyWithCount(1);
        extra.set(DataComponents.INTANGIBLE_PROJECTILE, Unit.INSTANCE);
        allProjectiles.add(extra);
        allProjectiles.add(extra.copy());
    }
    super.shoot(level, shooter, hand, weapon, allProjectiles, velocity * SPEED_MULTIPLIER, inaccuracy, crit, target);

    if (shooter instanceof Player player) {
        player.getCooldowns().addCooldown(weapon, COOLDOWN_TICKS);
    }
}
```

- `projectiles` = die Pfeile, die der Bogen verschießen würde (normal einer).
- Zwei **Kopien** dazu. `INTANGIBLE_PROJECTILE` = man kann sie nicht aufheben (wie bei Mehrfachschuss), sonst ließen sich Pfeile vermehren.
- `velocity * SPEED_MULTIPLIER` = doppelte Geschwindigkeit.
- Kurze Abklingzeit danach.

### Methode 2: `createProjectile` – mehr Schaden

```java
@Override
protected Projectile createProjectile(Level level, LivingEntity shooter, ItemStack weapon, ItemStack ammo, boolean crit) {
    Projectile projectile = super.createProjectile(level, shooter, weapon, ammo, crit);
    if (projectile instanceof AbstractArrow arrow) {
        arrow.setBaseDamage(ARROW_DAMAGE);
        // arrow.igniteForSeconds(5); // optional: brennende Pfeile
    }
    return projectile;
}
```

Wird **für jeden Pfeil** aufgerufen. Grundschaden eines Pfeils ist 2.0, der echte Schaden hängt zusätzlich von der Geschwindigkeit ab.

`instanceof AbstractArrow arrow` = **Pattern Matching**: Typ prüfen und gleich einen Namen vergeben.

### Methode 3: `shootProjectile` – die Richtung

```java
@Override
protected void shootProjectile(LivingEntity shooter, Projectile projectile, int index, float velocity,
                               float inaccuracy, float angle, LivingEntity target) {
    float spread = switch (index) {
        case 1 -> -SIDE_ARROW_ANGLE;
        case 2 -> SIDE_ARROW_ANGLE;
        default -> angle;
    };
    super.shootProjectile(shooter, projectile, index, velocity, inaccuracy, spread, target);
}
```

`index` = Nummer des Pfeils: 0 geradeaus, 1 um 10° links, 2 um 10° rechts.

## 3. Animierte Item-Bilder: das Spannen

`assets/mymod/items/triple_bow.json` – wählt je nach Zustand ein anderes Modell:

```json
{
  "model": {
    "type": "minecraft:condition",
    "property": "minecraft:using_item",
    "on_false": { "type": "minecraft:model", "model": "mymod:item/triple_bow" },
    "on_true": {
      "type": "minecraft:range_dispatch",
      "property": "minecraft:use_duration",
      "scale": 0.05,
      "fallback": { "type": "minecraft:model", "model": "mymod:item/triple_bow_pulling_0" },
      "entries": [
        { "threshold": 0.65, "model": { "type": "minecraft:model", "model": "mymod:item/triple_bow_pulling_1" } },
        { "threshold": 0.9,  "model": { "type": "minecraft:model", "model": "mymod:item/triple_bow_pulling_2" } }
      ]
    }
  }
}
```

- `condition` / `using_item`: Wird der Bogen gerade gespannt?
  - Nein → normales Bild.
  - Ja → je nach Dauer ein anderes.
- `range_dispatch` / `use_duration`, `scale 0.05`: Ticks × 0.05 = Sekunden. Unter 0.65 s → `pulling_0`, ab 0.65 → `pulling_1`, ab 0.9 → `pulling_2`.

Modelle:

```json
// models/item/triple_bow.json
{ "parent": "minecraft:item/bow", "textures": { "layer0": "mymod:item/triple_bow" } }

// models/item/triple_bow_pulling_0.json (1 und 2 genauso)
{ "parent": "mymod:item/triple_bow", "textures": { "layer0": "mymod:item/triple_bow_pulling_0" } }
```

`"parent": "minecraft:item/bow"` sorgt dafür, dass der Bogen in der Hand richtig gehalten wird. Dazu 4 Texturen.

Das System (`condition`, `range_dispatch`, `select`) funktioniert für viele Dinge: Kompass, Uhr, Armbrust geladen/leer … In der Minecraft-Bibliothek unter `assets/minecraft/items/` gibt es Beispiele für alles.

### Verzauberbar machen

```json
// data/minecraft/tags/item/enchantable/bow.json
{ "replace": false, "values": [ "mymod:triple_bow" ] }
```

Dann gehen Stärke, Flamme, Unendlichkeit usw. Für Haltbarkeit/Reparatur zusätzlich `enchantable/durability.json` (Vanilla-Bogen steht auch dort).

## 4. Warum Code aus alten Versionen nicht passt

Ein 1.16.5-Bogen arbeitete mit `releaseUsing`, `ArrowItem`, `AbstractArrowEntity`, `getPowerForTime` … In 26.x hat `BowItem` eine ganz andere Struktur mit `shoot`, `createProjectile` und `shootProjectile`. **Die Idee** lässt sich übernehmen, der Code nicht. Darum Schritt 2 im Vorgehen: immer erst die aktuelle Vanilla-Klasse lesen.

## Checkliste: eigenes Item mit Verhalten

- [ ] passende Vanilla-Klasse gefunden und gelesen
- [ ] Klasse in `item/custom/`, `extends …`, Konstruktor mit `super(properties)`
- [ ] Methoden mit **Strg + O** überschrieben, `@Override` drüber
- [ ] in `ModItems` mit `new MeinItem(...)` registriert
- [ ] Modelle/Texturen, ggf. `items/…json` mit Zuständen
- [ ] passende Vanilla-Tags (`enchantable/…`)

## Zum Ausprobieren

- Schwert, das Gegner beim Treffer 3 Sekunden in Brand setzt (`hurtEnemy` + `target.igniteForSeconds(3)`).
- Zauberstab, der bei Rechtsklick einen Feuerball schießt (`use`, Vanilla-Vorbild: `FireChargeItem`).
- Bogen, der Pfeile mit Gravitation 0 schießt (`arrow.setNoGravity(true)` in `createProjectile`).
