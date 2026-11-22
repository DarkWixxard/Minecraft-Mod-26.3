# 32 – Effekte hinzufügen: `addEffect` und alle Stellen, wo es geht

Statuseffekte (Tempo, Vergiftung, Regeneration …) sind eines der einfachsten Mittel, um Items, Blöcke und Mobs besonders zu machen. Dieses Kapitel erklärt zuerst **`addEffect`** selbst und zeigt dann **jede Stelle**, an der man Effekte vergeben kann – jeweils mit Beispiel-Code.

> ✅ Getestet mit Forge 26.3-66.1.0: Alle Java-Beispiele kompilieren. Ein Test-Befehl hat auf dem Server Effekte vergeben und die Ergebnisse ausgegeben – sie stehen unten als „Test:“. Was einen Spieler braucht (Rechtsklick mit dem Stab, echtes Essen, echter Schwertschlag), wurde über dieselben Methoden direkt aufgerufen oder ist nur kompiliert; das steht jeweils dabei.

## 1. `addEffect` – die Grundlagen

```java
entity.addEffect(new MobEffectInstance(MobEffects.SPEED, 200, 1));
//                                      ↑ welcher     ↑ wie lange  ↑ welche Stufe
//                                        Effekt        (Ticks)      (0 = I, 1 = II …)
```

- `addEffect` gibt es an **jedem `LivingEntity`** – Spieler, Mobs, Tiere, Rüstungsständer.
- Immer **auf dem Server** aufrufen (`!level.isClientSide()`). Auf dem Client gesetzte Effekte sind nicht echt.
- **Zeit in Ticks**: 20 Ticks = 1 Sekunde. 200 = 10 s, 1200 = 1 min.
- **Stufe = Anzahl − 1**: `0` ist Stufe I, `1` ist Stufe II.

### Alle Parameter von `MobEffectInstance`

```java
new MobEffectInstance(effekt)                                             // 0 Ticks – selten sinnvoll
new MobEffectInstance(effekt, dauer)                                      // Stufe I
new MobEffectInstance(effekt, dauer, stufe)
new MobEffectInstance(effekt, dauer, stufe, ambient, sichtbar)
new MobEffectInstance(effekt, dauer, stufe, ambient, sichtbar, symbol)
```

| Parameter | Bedeutung | Standard |
|---|---|---|
| `effekt` | `MobEffects.SPEED` usw. (ein `Holder<MobEffect>`) | – |
| `dauer` | Ticks. `MobEffectInstance.INFINITE_DURATION` (= −1) für **unendlich** | 0 |
| `stufe` | 0 = I, 1 = II … | 0 |
| `ambient` | „Umgebungs“-Effekt wie vom Leuchtfeuer: blassere Partikel | `false` |
| `sichtbar` | Partikel um die Entity anzeigen | `true` |
| `symbol` | Symbol oben rechts / im Inventar anzeigen | wie `sichtbar` |

Beispiel unendliches Leuchten ohne Partikel, aber mit Symbol:

```java
pig.addEffect(new MobEffectInstance(MobEffects.GLOWING, MobEffectInstance.INFINITE_DURATION, 0, false, false, true));
```

Test: `isInfinite=true`

### Wichtige Effekte (`MobEffects`)

| gut | schlecht | sonstiges |
|---|---|---|
| `SPEED`, `HASTE`, `STRENGTH`, `JUMP_BOOST`, `REGENERATION`, `RESISTANCE`, `FIRE_RESISTANCE`, `WATER_BREATHING`, `INVISIBILITY`, `NIGHT_VISION`, `HEALTH_BOOST`, `ABSORPTION`, `SATURATION`, `LUCK`, `SLOW_FALLING`, `CONDUIT_POWER`, `DOLPHINS_GRACE`, `HERO_OF_THE_VILLAGE` | `SLOWNESS`, `MINING_FATIGUE`, `NAUSEA`, `BLINDNESS`, `HUNGER`, `WEAKNESS`, `POISON`, `WITHER`, `LEVITATION`, `UNLUCK`, `DARKNESS`, `WIND_CHARGED`, `WEAVING`, `OOZING`, `INFESTED`, `INSTANT_DAMAGE` | `GLOWING`, `INSTANT_HEALTH`, `BAD_OMEN`, `TRIAL_OMEN`, `RAID_OMEN` |

Alle: Strg + N → `MobEffects`. Eigene Effekte: [16](16-Effekte-und-Traenke.md).

### Mit Verursacher

```java
target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 1), attacker);
```

Der zweite Parameter sagt, **wer** den Effekt verursacht hat (z. B. für Statistiken, Fortschritte, Events). Optional.

## 2. Was `addEffect` zurückgibt – und wann es nicht klappt

`addEffect` gibt `true` zurück, wenn sich etwas geändert hat, und `false`, wenn nicht.

Test:

```
pig speed added=true                          → hat geklappt
skeleton poison added=false  regen added=false → Untote sind immun gegen Gift UND Regeneration
pig slowness added=false                       → ein eigener Event-Listener verbietet es (Abschnitt 4.9)
```

Immun sein können Entities durch:
- **Tags**: `#minecraft:ignores_poison_and_regen` (enthält alle Untoten), `immune_to_infested`, `immune_to_oozing`.
- **Überschriebenes `canBeAffected(...)`** in der Mob-Klasse (z. B. Spinnen gegen Gift, Wither gegen Wither-Effekt).
- **Events** (`MobEffectEvent.Applicable`).

### Was passiert, wenn der Effekt schon da ist?

Test (Schwein):

| Vorher | Dazu | Ergebnis | Regel |
|---|---|---|---|
| Sprungkraft I, 100 Ticks | Sprungkraft I, 400 Ticks | **400 Ticks** | gleiche Stufe, länger → wird verlängert |
| Sprungkraft I, 400 Ticks | Sprungkraft I, 50 Ticks | **400 Ticks** | gleiche Stufe, kürzer → nichts passiert |
| Stärke II, 200 Ticks | Stärke I, 1200 Ticks | **Stufe II, 200 Ticks** | höhere Stufe bleibt; die schwächere wird als „versteckter“ Effekt gemerkt und läuft weiter, wenn die stärkere vorbei ist |
| Stärke I | Stärke II | Stufe II | höhere Stufe ersetzt immer |

Deshalb kann man Effekte **regelmäßig neu vergeben** (z. B. jede Sekunde), ohne dass sie sich „aufaddieren“ – genau so funktionieren Rüstungs-Set-Boni ([04](04-Ruestung.md#6-set-effekte-volle-rüstung)).

## 3. Effekte lesen und entfernen

```java
entity.hasEffect(MobEffects.SPEED)                     // hat er Tempo?
MobEffectInstance e = entity.getEffect(MobEffects.SPEED);   // null, wenn nicht
e.getAmplifier();  e.getDuration();  e.isInfiniteDuration();
entity.removeEffect(MobEffects.SPEED);                 // ein Effekt weg
entity.removeAllEffects();                             // alle weg (wie Milch)
```

Test: `removeEffect speed=true  hasEffect=false`

---

## 4. Alle Stellen, an denen man Effekte vergeben kann

| # | Stelle | Wann | Code oder JSON? |
|---|---|---|---|
| 4.1 | Essen / Trinken | beim Verzehr | Java (Consumable) – oder Item-Komponente |
| 4.2 | Waffe | nach einem Treffer | Java (`postHurtEnemy`) |
| 4.3 | Item mit Rechtsklick | beim Benutzen | Java (`use`) |
| 4.4 | Effektwolke | Bereich am Boden | Java (`AreaEffectCloud`) |
| 4.5 | Rüstung | solange getragen | Java (`inventoryTick`) |
| 4.6 | Block | beim Drauftreten | Java (`stepOn`) |
| 4.7 | Pfeil | beim Treffer | Java (`doPostHurtEffects`) |
| 4.8 | Mob | beim Angriff | Java (`doHurtTarget`) |
| 4.9 | Events | bei beliebigen Ereignissen; auch Effekte **verbieten** | Java (Event-Listener) |
| 4.10 | Effekt-Klasse selbst | jeden Tick, solange aktiv | Java (`applyEffectTick`) |
| 4.11 | Trank / Brauen / getränkte Pfeile | beim Trinken / Treffen | Java (Potion) + JSON (Rezept) |
| 4.12 | Verzauberung | beim Treffer u. v. m. | **nur JSON** |
| 4.13 | Befehle | auf Befehl | `/effect` oder eigener Befehl |

### 4.1 Essen und Trinken

Seit 1.21.2 bestimmt die Komponente **`Consumable`**, was beim Essen passiert. Effekte gibt man mit `ApplyStatusEffectsConsumeEffect`:

```java
public static final FoodProperties FROST_APPLE_FOOD = new FoodProperties.Builder()
        .nutrition(4).saturationModifier(0.6F).alwaysEdible().build();

public static final Consumable FROST_APPLE_CONSUMABLE = Consumables.defaultFood()
        .onConsume(new ApplyStatusEffectsConsumeEffect(List.of(
                new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 600, 0),    // immer: 30 s Feuerresistenz
                new MobEffectInstance(MobEffects.SPEED, 200, 1))))            // immer: 10 s Tempo II
        .onConsume(new ApplyStatusEffectsConsumeEffect(
                new MobEffectInstance(MobEffects.SLOWNESS, 100, 0), 0.25F))  // 25 % Chance: 5 s Langsamkeit
        .build();

public static final RegistryObject<Item> FROST_APPLE = ITEMS.register("frost_apple",
        () -> new Item(new Item.Properties().setId(ITEMS.key("frost_apple"))
                .food(FROST_APPLE_FOOD, FROST_APPLE_CONSUMABLE)));
```

| Teil | Bedeutung |
|---|---|
| `FoodProperties` | Hunger und Sättigung ([02](02-Bloecke-Items-Werkzeuge.md#5-essen)) |
| `Consumables.defaultFood()` | Vorlage „Essen“: 1,6 s Essdauer, Ess-Animation und -Geräusch. Für Getränke: `Consumables.defaultDrink()` |
| `.onConsume(...)` | was beim Fertig-Essen passiert. Mehrmals aufrufbar |
| `ApplyStatusEffectsConsumeEffect(liste)` | gibt alle Effekte der Liste |
| `ApplyStatusEffectsConsumeEffect(effekt, 0.25F)` | gibt den Effekt nur mit **25 % Wahrscheinlichkeit** |
| `.food(food, consumable)` | beides ans Item hängen |

Test: Ein Zombie hat den Frostapfel „gegessen“ (`stack.finishUsingItem(level, zombie)` – dieselbe Methode, die beim Essen aufgerufen wird): `fireRes=true speed=2` (Tempo II).

Vanilla-Vorbilder in der Klasse `Consumables`: `GOLDEN_APPLE`, `ENCHANTED_GOLDEN_APPLE`, `PUFFERFISH`, `ROTTEN_FLESH` (80 % Hunger), `MILK_BUCKET` (`ClearAllStatusEffectsConsumeEffect` – entfernt alle Effekte).

**Ohne Code** geht es auch mit `/give` – jedes Item lässt sich so essbar machen:

```
/give @s minecraft:apple[consumable={consume_seconds:1.6,on_consume_effects:[{type:"minecraft:apply_effects",effects:[{id:"minecraft:speed",duration:200,amplifier:1}]}]}]
```

(Test: wird vom Server ohne Fehler gelesen.)

### 4.2 Waffe: Effekt beim Treffer

```java
// Ein Schwert, das Getroffene vergiftet
public class VenomSwordItem extends Item {
    public VenomSwordItem(Properties properties) {
        super(properties);
    }

    // Wird (auf dem Server) aufgerufen, nachdem das Schwert "target" getroffen hat
    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.postHurtEnemy(stack, target, attacker);   // Vanilla-Teil behalten (Haltbarkeit)
        target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 1), attacker);   // 3 s Gift II
    }
}

// Registrierung mit Schwert-Werten:
public static final RegistryObject<Item> VENOM_SWORD = ITEMS.register("venom_sword",
        () -> new VenomSwordItem(new Item.Properties().setId(ITEMS.key("venom_sword"))
                .sword(ToolMaterial.IRON, 3.0F, -2.4F)));
```

Test: `postHurtEnemy` direkt aufgerufen → `pig after venom sword: poison=true`.

`hurtEnemy` (vor dem Vanilla-Teil) und `postHurtEnemy` (danach) gibt es beide; in 26.x geben beide **nichts** zurück (`void`) – in alten Tutorials steht noch `boolean hurtEnemy`.

### 4.3 Item mit Rechtsklick

```java
// Rechtsklick: sich selbst heilen und eine Heilwolke für andere hinterlassen
public class HealingWandItem extends Item {
    public HealingWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            // a) Effekt auf den Spieler, der den Stab benutzt
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));

            // b) eine Wolke am Boden (Abschnitt 4.4)
            AreaEffectCloud cloud = new AreaEffectCloud(level, player.getX(), player.getY(), player.getZ());
            cloud.setOwner(player);
            cloud.setRadius(3.0F);
            cloud.setDuration(200);                                  // 10 s
            cloud.setWaitTime(0);
            cloud.setCustomParticle(ParticleTypes.HEART);
            cloud.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0));
            level.addFreshEntity(cloud);

            player.getCooldowns().addCooldown(player.getItemInHand(hand), 100);   // 5 s Abklingzeit
        }
        return InteractionResult.SUCCESS;
    }
}
```

- `use` wird bei **Rechtsklick in die Luft** aufgerufen ([05](05-Eigene-Item-Klassen.md)).
- Die **Abklingzeit** verhindert Dauerfeuer. Ohne sie kann man den Effekt jeden Tick neu holen.
- Nur kompiliert (braucht einen Spieler); die Wolke wurde einzeln getestet.

### 4.4 Effektwolke (`AreaEffectCloud`)

Eine Wolke am Boden, die allen darin einen Effekt gibt – wie ein Verweiltrank.

```java
AreaEffectCloud cloud = new AreaEffectCloud(level, x, y, z);
cloud.setRadius(3.0F);                                        // Radius in Blöcken
cloud.setDuration(200);                                       // wie lange die Wolke bleibt (Ticks)
cloud.setWaitTime(0);                                         // ab wann sie wirkt (Standard 20 Ticks)
cloud.setCustomParticle(ParticleTypes.HEART);                 // Aussehen
cloud.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0));
level.addFreshEntity(cloud);                                  // in die Welt setzen
```

Weitere Einstellungen: `setRadiusPerTick(-0.01F)` (schrumpft mit der Zeit), `setRadiusOnUse(-0.5F)` (schrumpft bei jeder Benutzung), `setDurationOnUse(...)`, `setOwner(entity)`.

Test: Wolke neben einer Kuh → `Cow has the following entity data: [{duration: 45, id: "minecraft:regeneration"}]`.

### 4.5 Rüstung: solange sie getragen wird

```java
@Override
public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
    super.inventoryTick(stack, level, entity, slot);
    if (slot == EquipmentSlot.HEAD && entity instanceof LivingEntity living) {
        living.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, false));
    }
}
```

`inventoryTick` läuft jeden Tick für jedes Item im Inventar. Volles Set mit Erneuern vor Ablauf: [04](04-Ruestung.md#6-set-effekte-volle-rüstung). Funktioniert genauso für Items, die man nur **im Inventar trägt** (Talisman): dann ohne `slot`-Prüfung.

### 4.6 Block: beim Drauftreten

```java
@Override
public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
    if (!level.isClientSide() && entity instanceof LivingEntity living) {
        living.addEffect(new MobEffectInstance(MobEffects.SPEED, 60, 2));   // 3 s Tempo III
    }
    super.stepOn(level, pos, state, entity);
}
```

Vollständig: [14](14-Eigene-Block-Klassen.md#3-beispiel-block-der-beim-drauftreten-wirkt).

### 4.7 Pfeil: beim Treffer

```java
@Override
protected void doPostHurtEffects(LivingEntity target) {
    super.doPostHurtEffects(target);
    target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 1));   // 5 s Langsamkeit II
}
```

Vollständig mit Pfeil-Klasse: [31](31-ItemStack.md#9-itemstack-als-rückgabewert-überschriebene-methoden).

### 4.8 Mob: beim Angriff

```java
// Ein Mob, dessen Biss das Ziel verlangsamt
public class FrostBiteZombie extends Zombie {
    public FrostBiteZombie(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 1), this);
        }
        return hit;
    }
}
```

- `doHurtTarget` wird bei jedem Nahkampf-Angriff des Mobs aufgerufen. Erst `super` (der eigentliche Treffer), dann nur bei Erfolg (`hit`) den Effekt geben.
- So machen es Vanilla-Mobs: `Husk` (Hunger), `CaveSpider` (Gift), `Stray`-Pfeile (Langsamkeit).
- Effekte auf den **Mob selbst** (z. B. Boss wird bei wenig Leben schneller): in `customServerAiStep` ([09](09-Mob-Fliegen.md#5-wann-abheben-wann-landen)).

### 4.9 Events: bei beliebigen Ereignissen – und Effekte verbieten

**Effekt geben**, wenn etwas passiert – hier: Ein brennender Spieler bekommt Feuerresistenz:

```java
@SubscribeEvent
public static void onHurt(LivingHurtEvent event) {
    if (event.getEntity() instanceof Player player && event.getSource().is(DamageTypeTags.IS_FIRE)
            && !player.hasEffect(MobEffects.FIRE_RESISTANCE)) {
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 0));
    }
}
```

Weitere Beispiele: in großer Höhe Langsamkeit (`PlayerTickEvent`, [15](15-Spiel-Events.md#jeden-tick-für-jeden-spieler)), beim Betreten der Welt (`PlayerLoggedInEvent`).

**Effekt verbieten** – mit `MobEffectEvent.Applicable`:

```java
// Schweine sind immun gegen Langsamkeit
@SubscribeEvent
public static void onApplicable(MobEffectEvent.Applicable event) {
    if (event.getEntity() instanceof Pig && event.getEffectInstance().is(MobEffects.SLOWNESS)) {
        event.setResult(Result.DENY);
    }
}
```

Test: `pig slowness added=false`. Mit `Result.ALLOW` kann man einen Effekt umgekehrt **erzwingen** (z. B. Gift bei Untoten erlauben). `Result` aus `net.minecraftforge.common.util`.

Weitere Effekt-Events: `MobEffectEvent.Added` (wurde hinzugefügt), `MobEffectEvent.Expired` (ist abgelaufen), `MobEffectEvent.Remove` (wird entfernt – abbrechbar).

### 4.10 In der Effekt-Klasse selbst

Ein eigener Effekt kann jeden Tick etwas tun – und dabei auch **andere** Effekte geben:

```java
@Override
public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
    entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, amplifier));   // Frost macht auch langsam
    return true;
}
```

Vollständig (mit `shouldApplyEffectTickThisTick`): [16](16-Effekte-und-Traenke.md#1-der-effekt).

### 4.11 Tränke, Brauen, getränkte Pfeile

Ein **Trank** ist eine fertige Effekt-Liste. Sobald es einen gibt, erzeugt Minecraft automatisch Trank, Wurftrank, Verweiltrank (= Effektwolke) und getränkten Pfeil:

```java
public static final RegistryObject<Potion> FROST = POTIONS.register("frost",
        () -> new Potion("frost", new MobEffectInstance(ModEffects.FROST.getHolder().orElseThrow(), 900)));
```

Brau-Rezepte als JSON, alle Details: [16](16-Effekte-und-Traenke.md#2-der-trank).

### 4.12 Verzauberung – ohne Code

```json
"effects": {
  "minecraft:post_attack": [ {
    "enchanted": "attacker", "affected": "victim",
    "effect": { "type": "minecraft:apply_mob_effect", "to_apply": "minecraft:slowness",
                "min_duration": 2, "max_duration": 2, "min_amplifier": 0, "max_amplifier": 0 }
  } ]
}
```

Dauer hier in **Sekunden** (nicht Ticks!). Vollständig: [21](21-Verzauberungen.md).

### 4.13 Befehle

```
/effect give @s minecraft:speed 30 1                       ← 30 SEKUNDEN, Stufe II
/effect give @s minecraft:night_vision infinite 0 true     ← unendlich, Stufe I, ohne Partikel
/effect clear @s
```

Test (an einem Schwein): `speed` → `duration: 580` Ticks nach 1 s, `amplifier: 1b`; `night_vision` → `duration: -1`, `show_particles: 0b`, **und auch `show_icon: 0b`** – das `true` am Ende versteckt also Partikel *und* Symbol.

Im eigenen Befehl ([13](13-Befehle.md)) einfach `player.addEffect(...)` aufrufen.

**Unterschied zu `addEffect`:** Die **Stufe** ist bei beiden „Anzahl − 1“ (`1` = Stufe II). Die **Zeit** aber nicht: `/effect` nimmt **Sekunden**, `MobEffectInstance` **Ticks**.

---

## 5. Zeit-Einheiten im Überblick

| Stelle | Einheit |
|---|---|
| `new MobEffectInstance(…, dauer, …)` | **Ticks** (20 = 1 s) |
| `Potion`, `Consumable`, `AreaEffectCloud`, `inventoryTick` | Ticks |
| `/effect give … <sekunden>` | **Sekunden** |
| Verzauberungs-JSON `apply_mob_effect` | **Sekunden** |
| Item-Komponente `consumable` → `effects[].duration` | Ticks |

## 6. Typische Fehler

| Problem | Ursache | Lösung |
|---|---|---|
| Effekt kurz sichtbar, dann weg / wirkt nicht | auf dem Client vergeben | nur bei `!level.isClientSide()` |
| Effekt dauert 1/20 so lang wie gedacht | Sekunden statt Ticks | × 20 |
| Effekt ist eine Stufe zu hoch | Stufe ist „Anzahl − 1“ | `0` für Stufe I |
| `addEffect` gibt `false`, nichts passiert | Ziel ist immun (Untote: Gift/Regeneration) oder ein Event verbietet es | Ziel prüfen, `canBeAffected` / Events |
| schwächerer, längerer Effekt „verschwindet“ | stärkere Stufe läuft noch, der schwächere ist versteckt | warten oder `removeEffect` vorher |
| Effekt wird jeden Tick neu gesetzt, Partikel flackern | ständiges Neuvergeben | nur erneuern, wenn `getDuration()` klein ist ([04](04-Ruestung.md)), oder `sichtbar = false` |
| `MobEffectInstance` will kein `RegistryObject` | eigener Effekt braucht `Holder` | `ModEffects.X.getHolder().orElseThrow()` |
| `hurtEnemy` mit `boolean` lässt sich nicht überschreiben | alte Signatur | in 26.x `void hurtEnemy(...)` / `void postHurtEnemy(...)` |

## Checkliste

- [ ] Stelle ausgewählt (Essen, Waffe, Rechtsklick, Rüstung, Block, Pfeil, Mob, Event …)
- [ ] auf dem Server vergeben
- [ ] Dauer in Ticks, Stufe = Anzahl − 1
- [ ] Partikel/Symbol bewusst gewählt (`ambient`, `sichtbar`, `symbol`)
- [ ] Rückgabe von `addEffect` beachtet, wo es wichtig ist
- [ ] bei wiederholter Vergabe: Abklingzeit oder nur bei kurzer Restdauer erneuern

## Zum Ausprobieren

- Ein „Honigbrot“, das Regeneration I gibt und mit 50 % Chance zusätzlich Absorption.
- Ein Schwert, das bei jedem 5. Treffer Blindheit gibt (Zähler in einer Komponente oder einfach `level.getRandom().nextInt(5) == 0`).
- Ein Block, der allen in 5 Blöcken Umkreis Nachtsicht gibt (`randomTick` oder geplanter `tick` + `level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(5))`).
- Ein Event, das Spieler im Regen langsamer macht (`player.isInWaterOrRain()` im `PlayerTickEvent`).
