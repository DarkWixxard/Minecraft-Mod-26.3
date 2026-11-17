# 16 – Eigene Effekte und Tränke

Drei Dinge, die zusammengehören, aber getrennt sind:

| Was | Beispiel | Wo |
|---|---|---|
| **Effekt** (`MobEffect`) | „Frost“ – das, was auf einer Entity wirkt | Java |
| **Trank** (`Potion`) | „Trank des Frosts“ – Effekt + Dauer | Java |
| **Brau-Rezept** | Seltsamer Trank + Schneeball → Trank des Frosts | **JSON** (seit 26.x datengetrieben) |

> ✅ Getestet mit Forge 26.3-66.1.0 auf dem Server: Effekt per `/effect give` auf ein Schwein (Stufe II: 10 → 4 Leben in 3 s), Braustand mit Seltsamem Trank + Schneeball + Lohenstaub → Trank `mymod:frost`.

## 1. Der Effekt

```java
// Friert die Entity ein: 1 Schaden pro Sekunde (schneller bei höheren Stufen)
public class FrostEffect extends MobEffect {
    public FrostEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    // Wird auf dem Server aufgerufen, wann immer shouldApplyEffectTickThisTick true liefert
    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        entity.hurtServer(level, level.damageSources().freeze(), 1.0F);
        entity.setTicksFrozen(Math.max(entity.getTicksFrozen(), entity.getTicksRequiredToFreeze()));   // Frost-Rand am Bildschirm
        return true;   // false würde den Effekt entfernen
    }

    // Stufe I: alle 20 Ticks, Stufe II: alle 10, Stufe III: alle 5 …
    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        int interval = 20 >> amplifier;
        return interval <= 0 || tickCount % interval == 0;
    }
}
```

- Der Konstruktor von `MobEffect` ist `protected` → für eigene Effekte **immer eine Unterklasse**, auch wenn sie nur Attribute ändert.
- `MobEffectCategory`: `BENEFICIAL` (gut, blau im Tooltip), `HARMFUL` (schlecht, rot), `NEUTRAL`.
- Farbe als Hex-Zahl: `0x9FE0FF` = hellblau (Partikel und Trankfarbe).
- `amplifier` ist Stufe − 1 (0 = I).
- `20 >> amplifier` = 20 geteilt durch 2^amplifier (Bit-Verschiebung). Vorbild: `RegenerationMobEffect`.

### Registrieren, mit Attribut-Änderung

```java
public final class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, MyMod.MODID);

    public static final RegistryObject<MobEffect> FROST = MOB_EFFECTS.register("frost",
            () -> new FrostEffect(MobEffectCategory.HARMFUL, 0x9FE0FF)
                    // zusätzlich 15 % langsamer pro Stufe (wie Langsamkeit)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED,
                            Identifier.fromNamespaceAndPath(MyMod.MODID, "effect.frost"),
                            -0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    private ModEffects() {}
}
// in MyMod: ModEffects.MOB_EFFECTS.register(modBusGroup);
```

`addAttributeModifier` ändert ein Attribut, solange der Effekt aktiv ist, und skaliert mit der Stufe. Für reine Attribut-Effekte (schneller, mehr Schaden, mehr Leben) braucht `applyEffectTick` gar nichts zu tun.

| Operation | Wirkung von `-0.15` |
|---|---|
| `ADD_VALUE` | Wert − 0.15 |
| `ADD_MULTIPLIED_BASE` | Basiswert × (1 − 0.15) |
| `ADD_MULTIPLIED_TOTAL` | Endwert × (1 − 0.15) |

### Dateien

| Datei | Inhalt |
|---|---|
| `assets/mymod/textures/mob_effect/frost.png` | Symbol, 18×18 (Ordner `mob_effect`, Dateiname = Registry-Name) |
| `lang`: `"effect.mymod.frost": "Frost"` | Name |

Testen: `/effect give @s mymod:frost 30 1`

## 2. Der Trank

```java
public final class ModPotions {
    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(ForgeRegistries.POTIONS, MyMod.MODID);

    // "frost" ist der Name für die Übersetzung: item.minecraft.potion.effect.frost
    public static final RegistryObject<Potion> FROST = POTIONS.register("frost",
            () -> new Potion("frost", new MobEffectInstance(ModEffects.FROST.getHolder().orElseThrow(), 900)));      // 45 s
    public static final RegistryObject<Potion> LONG_FROST = POTIONS.register("long_frost",
            () -> new Potion("frost", new MobEffectInstance(ModEffects.FROST.getHolder().orElseThrow(), 2400)));     // 2 min

    private ModPotions() {}
}
// in MyMod: ModPotions.POTIONS.register(modBusGroup);
```

- Ein Trank ist eine Liste von `MobEffectInstance`s (mehrere Effekte gehen).
- `MobEffectInstance` braucht einen **`Holder<MobEffect>`**, kein `RegistryObject`. `.getHolder().orElseThrow()` holt ihn – und zwar **im Lambda**, also erst wenn der Effekt schon registriert ist.
- Der **erste** Parameter von `new Potion(...)` ist der Übersetzungsname. Lange und starke Varianten benutzen denselben Namen → gleicher Anzeigename.

Für jeden Trank erzeugt Minecraft automatisch: Trank, Wurftrank, Verweiltrank und getränkten Pfeil.

```json
"item.minecraft.potion.effect.frost": "Potion of Frost",
"item.minecraft.splash_potion.effect.frost": "Splash Potion of Frost",
"item.minecraft.lingering_potion.effect.frost": "Lingering Potion of Frost",
"item.minecraft.tipped_arrow.effect.frost": "Arrow of Frost"
```

(Die Schlüssel beginnen wirklich mit `item.minecraft.`, auch für Mod-Tränke.)

Testen: `/give @s minecraft:potion[potion_contents={potion:"mymod:frost"}]`

## 3. Brau-Rezepte (JSON)

Seit 26.x sind Brau-Rezepte **normale Rezepte** in `data/mymod/recipe/`. Vanilla-Vorbilder liegen in der Minecraft-Bibliothek unter `data/minecraft/recipe/brewing/`.

`data/mymod/recipe/brewing/potion_awkward_frost.json`:

```json
{
  "type": "minecraft:brewing",
  "input": {
    "item": "minecraft:potion",
    "potion_contents": { "potions": "minecraft:awkward" }
  },
  "reagent": { "item": "minecraft:snowball" },
  "output": {
    "id": "minecraft:potion",
    "components": {
      "minecraft:potion_contents": { "potion": "mymod:frost" }
    }
  }
}
```

| Feld | Bedeutung |
|---|---|
| `input` | was in der Flasche sein muss (hier: Seltsamer Trank). Achtung: hier heißt es `potions` (Mehrzahl). |
| `reagent` | die Zutat oben im Braustand |
| `output` | das Ergebnis als Item mit Komponenten. Hier heißt es `potion` (Einzahl). |

Verlängern mit Redstone (`potion_frost_redstone.json`):

```json
{
  "type": "minecraft:brewing",
  "input": { "item": "minecraft:potion", "potion_contents": { "potions": "mymod:frost" } },
  "reagent": { "item": "minecraft:redstone" },
  "output": { "id": "minecraft:potion", "components": { "minecraft:potion_contents": { "potion": "mymod:long_frost" } } }
}
```

**Wurf- und Verweiltränke** brauchen eigene Rezepte (`"item": "minecraft:splash_potion"` in `input` und `"id": "minecraft:splash_potion"` in `output`). Vanilla hat dafür je eine Datei pro Kombination – kopieren und anpassen.

## Checkliste

- [ ] Effekt-Klasse `extends MobEffect` (auch wenn leer)
- [ ] `MOB_EFFECTS` und `POTIONS` registriert
- [ ] Symbol `textures/mob_effect/<name>.png`
- [ ] `lang`: `effect.<modid>.<name>` + 4 Trank-Namen
- [ ] Brau-Rezepte in `data/<modid>/recipe/` (Trank, ggf. Wurf/Verweil, Redstone/Glowstone)
- [ ] testen mit `/effect` und echtem Braustand

## Zum Ausprobieren

- Ein guter Effekt „Leichtfüßig“: kein Fallschaden (Attribut `FALL_DAMAGE_MULTIPLIER` mit `-1.0`, `ADD_MULTIPLIED_TOTAL`).
- Ein Effekt, der alle 2 Sekunden eine Explosion um die Entity herum erzeugt (`level.explode(...)`, Vorbild: Creeper).
- Den Frost-Effekt als Wirkung einer Verzauberung benutzen – siehe [21](21-Verzauberungen.md).
