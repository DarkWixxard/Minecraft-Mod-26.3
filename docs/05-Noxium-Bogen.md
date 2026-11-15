# 05 – Der Noxium-Bogen

Der Noxium-Bogen ist das erste Item mit **eigenem Verhalten**. Deshalb hat er eine eigene Klasse in `item/custom/`.

**Was er kann:**
- schießt immer **3 Pfeile** auf einmal (wie Mehrfachschuss), verbraucht aber nur einen,
- Pfeile fliegen **doppelt so schnell**,
- jeder Pfeil macht **5× so viel Schaden** wie normal.

## 1. Registrierung

```java
public static final RegistryObject<Item> NOXIUM_BOW = ITEMS.register("noxium_bow",
        () -> new NoxiumBowItem(new Item.Properties().setId(ITEMS.key("noxium_bow")).durability(500)));
```

`durability(500)` = 500 Schüsse bis er kaputtgeht (Vanilla-Bogen: 384).

## 2. Die Klasse `NoxiumBowItem`

```java
public class NoxiumBowItem extends BowItem {
    private static final double ARROW_DAMAGE = 10.0;      // normale Pfeile: 2.0 -> 5x Schaden
    private static final float SPEED_MULTIPLIER = 2.0F;   // Pfeile fliegen doppelt so schnell
    private static final float SIDE_ARROW_ANGLE = 10.0F;  // Winkel der zwei Extra-Pfeile (Grad)
    private static final int COOLDOWN_TICKS = 2;          // kleine Pause zwischen Schüssen (20 Ticks = 1 Sekunde)

    public NoxiumBowItem(Properties properties) {
        super(properties);
    }
```

`extends BowItem` heißt: Er **erbt** alles vom Vanilla-Bogen (Spannen, Pfeile suchen, Verzauberungen) und überschreibt nur drei Methoden. Die Konstanten oben machen es leicht, die Werte später zu ändern.

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

- `projectiles` ist die Liste der Pfeile, die der Bogen verschießen würde (normalerweise einer).
- Wir hängen **zwei Kopien** an. Die bekommen `INTANGIBLE_PROJECTILE`: Man kann sie nicht aufheben. So kann man keine Pfeile „vermehren“ – genau wie bei Mehrfachschuss.
- `velocity * SPEED_MULTIPLIER` verdoppelt die Geschwindigkeit.
- Danach kommt eine kurze Abklingzeit.

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

Wird **für jeden Pfeil einzeln** aufgerufen. Der Grundschaden eines Pfeils ist 2.0; der echte Schaden hängt zusätzlich von der Fluggeschwindigkeit ab. Weil die Pfeile doppelt so schnell fliegen, steigt der Schaden noch weiter.

`instanceof AbstractArrow arrow` ist **Pattern Matching**: Prüft den Typ und gibt dem Objekt gleich einen Namen.

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

`index` ist die Nummer des Pfeils: 0 fliegt geradeaus, 1 um 10° nach links, 2 um 10° nach rechts.

## 3. Das Spannen im Inventar: `assets/rubymod/items/noxium_bow.json`

```json
{
  "model": {
    "type": "minecraft:condition",
    "property": "minecraft:using_item",
    "on_false": { "type": "minecraft:model", "model": "rubymod:item/noxium_bow" },
    "on_true": {
      "type": "minecraft:range_dispatch",
      "property": "minecraft:use_duration",
      "scale": 0.05,
      "fallback": { "type": "minecraft:model", "model": "rubymod:item/noxium_bow_pulling_0" },
      "entries": [
        { "threshold": 0.65, "model": { "type": "minecraft:model", "model": "rubymod:item/noxium_bow_pulling_1" } },
        { "threshold": 0.9,  "model": { "type": "minecraft:model", "model": "rubymod:item/noxium_bow_pulling_2" } }
      ]
    }
  }
}
```

So liest du die Datei:

- `condition` / `using_item`: Wird der Bogen gerade benutzt (gespannt)?
  - Nein (`on_false`) → normales Bild.
  - Ja (`on_true`) → je nach Spann-Dauer ein anderes Bild.
- `range_dispatch` / `use_duration` mit `scale: 0.05`: Spann-Dauer in Ticks × 0.05 = Sekunden.
  - unter 0.65 s → `pulling_0`
  - ab 0.65 s → `pulling_1`
  - ab 0.9 s → `pulling_2`

Dafür braucht es **4 Texturen** (`noxium_bow.png`, `noxium_bow_pulling_0/1/2.png`) und **4 Modelle** in `models/item/`. Die Modelle haben `"parent": "minecraft:item/bow"`, damit der Bogen in der Hand richtig gehalten wird.

Damit Verzauberungen wie Stärke und Unendlichkeit gehen, muss der Bogen im Vanilla-Tag **`data/minecraft/tags/item/enchantable/bow.json`** stehen:

```json
{ "replace": false, "values": [ "rubymod:noxium_bow" ] }
```

> ⚠️ Im Moment gibt es bei dir stattdessen `data/minecraft/tags/item/bow.json` – einen Tag, den Minecraft gar nicht kennt – und darin steht `rubymod:noxium_axe` statt des Bogens. Siehe [11 – Fehler und Lösungen](11-Fehler-und-Loesungen.md).

## 4. Warum der Code aus 1.16.5 nicht ging

Die alte Version arbeitete mit `releaseUsing`, `ArrowItem`, `AbstractArrowEntity`, `getPowerForTime` usw. In 26.3 hat `BowItem` eine ganz andere Struktur mit `shoot`, `createProjectile` und `shootProjectile`. Man kann die Idee übernehmen, aber nicht den Code. Faustregel: **Immer erst in die Vanilla-Klasse schauen** (in IntelliJ mit **Strg+N** → `BowItem`), welche Methoden es dort gibt, und dann gezielt überschreiben.
