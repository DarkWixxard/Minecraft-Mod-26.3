# 06 – Eigener Mob mit GeckoLib

In den Kapiteln 06–10 entsteht als durchgehendes Beispiel ein **Wyvern**: ein großer, fliegender Boss-Mob mit GeckoLib-Modell. Jedes Kapitel baut auf dem vorherigen auf, die Teile lassen sich aber auch einzeln für andere Mobs verwenden.

## 1. Die Bausteine eines Mobs

| Baustein | Datei | läuft auf |
|---|---|---|
| EntityType registrieren | `entity/ModEntities.java` | beiden |
| Entity-Klasse (Verhalten, KI, Werte) | `entity/custom/WyvernEntity.java` | beiden |
| Attribute anmelden | `MyMod.java` | beiden |
| Renderer (Aussehen) | `entity/client/WyvernRenderer.java` | Client |
| Renderer anmelden | `entity/client/ModClientEvents.java` | Client |
| Modell, Animationen, Textur | `assets/mymod/geckolib/…`, `textures/entity/…` | Client |
| Name | `lang`: `"entity.mymod.wyvern"` | Client |
| optional: Spawn-Ei, natürliches Spawnen, Loot-Table | | |

> Erinnerung: Der **Server** entscheidet (Leben, KI, Schaden, Position), der **Client** zeigt an (Modell, Animation, Bossbar). Siehe [00](00-Uebersicht.md#client-und-server).

## 2. Modell: GeckoLib oder Vanilla?

| | Vanilla-Modell (Java-Code) | GeckoLib |
|---|---|---|
| Format | Blockbench *Modded Entity* → Java-Klasse | Blockbench *GeckoLib Animated Model* → JSON |
| Animationen | in Java programmiert (Sinus-Kurven für Beine …) | in Blockbench mit Keyframes gezeichnet |
| Gut für | einfache Mobs, die laufen und schauen | komplexe Mobs, Angriffs-Animationen, Bosse |
| Einstieg | mit Vanilla-Modell starten: z. B. eigener Zombie mit eigener Textur | – |

Dieser Leitfaden benutzt GeckoLib. Einbinden siehe [01](01-Einrichtung-und-Grundlagen.md#5-die-build-dateien).

## 3. EntityType registrieren

```java
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MyMod.MODID);

    public static final RegistryObject<EntityType<WyvernEntity>> WYVERN =
            ENTITY_TYPES.register("wyvern", () ->
                    EntityType.Builder.of(WyvernEntity::new, MobCategory.MONSTER)
                            .sized(3.0F, 3.0F)            // Hitbox: 3 Blöcke breit, 3 hoch
                            .clientTrackingRange(10)      // großer Mob: auch aus der Ferne sichtbar
                            .build(ENTITY_TYPES.key("wyvern")));

    private ModEntities() {}
}
```

| Teil | Bedeutung |
|---|---|
| `WyvernEntity::new` | **Methodenreferenz** auf den Konstruktor: „So erzeugst du einen neuen Wyvern.“ |
| `MobCategory` | `MONSTER`, `CREATURE` (Tiere), `AMBIENT` (Fledermaus), `WATER_CREATURE` … – bestimmt Spawn- und Despawn-Regeln |
| `sized(breite, höhe)` | Haupt-Hitbox in Blöcken. **Ohne** nimmt Minecraft 0.6 × 1.8 (Spielergröße). |
| `clientTrackingRange(n)` | in Chunks: bis zu dieser Entfernung bekommen Spieler den Mob geschickt. Normal 8–10. |
| `fireImmune()` | brennt nicht |
| `.build(ENTITY_TYPES.key(...))` | seit 1.21.2 braucht der EntityType seine ID beim Bauen |

In `MyMod`: `ModEntities.ENTITY_TYPES.register(modBusGroup);`

## 4. Attribute anmelden

Jeder lebende Mob **muss** Attribute haben, sonst stürzt das Spiel beim Spawnen ab.

```java
// in MyMod, im Konstruktor:
EntityAttributeCreationEvent.BUS.addListener(MyMod::registerAttributes);

// in MyMod:
private static void registerAttributes(EntityAttributeCreationEvent event) {
    event.put(ModEntities.WYVERN.get(), WyvernEntity.createAttributes().build());
}
```

Nur **einmal** anmelden – nicht zusätzlich noch eine `@SubscribeEvent`-Methode dafür.

## 5. Die Entity-Klasse – Grundgerüst

```java
public class WyvernEntity extends Monster implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public WyvernEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Animationen, siehe Kapitel 08
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
```

- **`extends Monster`**: Der Mob erbt alles, was ein feindlicher Mob kann. Für Tiere: `extends Animal`, für neutrale Mobs `extends PathfinderMob`.
- **`implements GeoEntity`**: für GeckoLib. Verlangt `registerControllers` und `getAnimatableInstanceCache`.
- **Konstruktor**: heißt wie die Klasse, kein Rückgabetyp. Läuft bei jedem neuen Mob (Spawnen, `/summon`, Laden der Welt).

### Attribute

```java
public static AttributeSupplier.Builder createAttributes() {
    return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 300.0)
            .add(Attributes.MOVEMENT_SPEED, 0.25)
            .add(Attributes.ATTACK_DAMAGE, 15.0)
            .add(Attributes.FOLLOW_RANGE, 30.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
            .add(Attributes.STEP_HEIGHT, 2.0)
            .add(Attributes.ARMOR, 10.0);
}
```

| Attribut | Bedeutung | Vergleich / Grenze |
|---|---|---|
| `MAX_HEALTH` | Leben (1 Herz = 2) | Zombie 20, Wither 300; **max. 1024** ([10](10-Boss-Funktionen.md#4-warum-maximal-1024-leben)) |
| `MOVEMENT_SPEED` | Laufgeschwindigkeit | Zombie 0.23, Eisengolem 0.25. 1.0 ist extrem schnell. |
| `ATTACK_DAMAGE` | Schaden pro Angriff | Zombie 3, Eisengolem 15; max. 2048 |
| `FOLLOW_RANGE` | ab welcher Entfernung (Blöcke) er Ziele verfolgt | Zombie 35; max. 2048 |
| `KNOCKBACK_RESISTANCE` | Rückstoß-Resistenz | **0 bis 1** (1 = kein Rückstoß) |
| `STEP_HEIGHT` | Stufen ohne Springen | Spieler 0.6 |
| `ARMOR` | Rüstungspunkte | max. 30 |
| `FLYING_SPEED`, `FALL_DAMAGE_MULTIPLIER` | siehe [09](09-Mob-Fliegen.md) | |

Zu große Werte werden **ohne Fehlermeldung** auf das Maximum gesetzt. Alle Attribute und Grenzen: Strg + N → `Attributes`.

## 6. Die KI (Goals)

```java
@Override
protected void registerGoals() {
    this.goalSelector.addGoal(0, new FloatGoal(this));
    this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
    this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
    this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
    this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

    this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
}
```

Zwei Listen:
- **`goalSelector`** – **was** der Mob tut.
- **`targetSelector`** – **wen** er angreift.

Die Zahl ist die **Priorität**: kleiner = wichtiger. Ein wichtigeres Goal unterbricht ein unwichtigeres, wenn beide dasselbe brauchen (Bewegung, Blick …).

| Goal | Was es tut |
|---|---|
| `FloatGoal` | schwimmt im Wasser oben, statt zu ertrinken |
| `MeleeAttackGoal(mob, tempo, merktSichZiel)` | läuft hin und greift an; `tempo` = Faktor auf die Geschwindigkeit |
| `WaterAvoidingRandomStrollGoal(mob, tempo)` | zufällig herumlaufen, Wasser meiden |
| `LookAtPlayerGoal(mob, Player.class, reichweite)` | Spieler anschauen |
| `RandomLookAroundGoal` | ab und zu umschauen |
| `PanicGoal`, `AvoidEntityGoal` | fliehen (für Tiere) |
| `TemptGoal`, `BreedGoal`, `FollowParentGoal` | Tiere: anlocken, vermehren |
| `HurtByTargetGoal` | wer ihn verletzt, wird Ziel |
| `NearestAttackableTargetGoal<>(mob, Typ.class, mussSehen)` | greift den nächsten von diesem Typ an |

Weitere Goals: Paket `net.minecraft.world.entity.ai.goal` in IntelliJ durchstöbern.

### Ein Goal nur unter einer Bedingung

**Anonyme Klasse** – eine kleine Unterklasse ohne Namen, direkt an Ort und Stelle:

```java
this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8) {
    @Override
    public boolean canUse() {
        return !isFlying() && super.canUse();   // nur am Boden herumlaufen
    }
});
```

Auf die Klammern achten: `}` schließt die Klasse, `)` schließt `addGoal(`, `;` beendet den Befehl. Steht das `);` schon direkt hinter `(this, 0.8)`, hängt `canUse` lose in der Methode und nichts kompiliert.

## 7. Die GeckoLib-Dateien

```
assets/mymod/geckolib/models/entity/wyvern.geo.json
assets/mymod/geckolib/animations/entity/wyvern.animation.json
assets/mymod/textures/entity/wyvern.png
```

GeckoLib baut alle drei Pfade aus **einem Namen** (`wyvern`) – deshalb müssen alle drei Dateien **gleich heißen**.

### Aus Blockbench exportieren

`.bbmodel` ist die **Projektdatei** von Blockbench (wie `.psd` bei Photoshop) – Minecraft kann sie nicht lesen. In Blockbench mit GeckoLib-Plugin:
- **File → Export → Export GeckoLib Model** → `.geo.json`
- **Animation → Export Animations** → `.animation.json`
- Textur: Rechtsklick → **Save As** → `.png`

### Animations-Namen

Jede Animation in der `.animation.json` hat einen Namen, z. B. `"animation.wyvern.idle"`. **Genau dieser Text** muss im Code stehen. Sonst:

```
Unable to find animation: 'animation.wyvern.idle' for wyvern
```

→ In der JSON nachschauen und angleichen (entweder im Code oder in der Datei umbenennen). Tipp: Eine Liste aller Namen bekommst du, indem du in der Datei nach `"animation.` suchst.

## 8. Der Renderer

```java
public class WyvernRenderer<R extends LivingEntityRenderState & GeoRenderState>
        extends GeoEntityRenderer<WyvernEntity, R> {

    public WyvernRenderer(EntityRendererProvider.Context context) {
        // geckolib/models/entity/wyvern.geo.json,
        // geckolib/animations/entity/wyvern.animation.json,
        // textures/entity/wyvern.png
        super(context, new DefaultedEntityGeoModel<>(Identifier.fromNamespaceAndPath(MyMod.MODID, "wyvern")));
        withScale(0.5F);   // optional: Modell verkleinern/vergrößern
    }
}
```

- `GeoEntityRenderer` macht die Arbeit: Modell laden, Animation abspielen, zeichnen.
- `DefaultedEntityGeoModel` sucht die drei Dateien nach dem Schema oben.
- `<R extends LivingEntityRenderState & GeoRenderState>`: **Generic** mit zwei Bedingungen. Seit 1.21.2 zeichnet Minecraft einen „RenderState“ (eine Momentaufnahme der Entity), und GeckoLib braucht beides.
- `withScale(…)`: Ist das Modell zu groß/klein. Hitbox (`sized`) und Teil-Hitboxen ([07](07-Mob-Hitboxen.md)) entsprechend anpassen.

### Renderer anmelden

```java
@Mod.EventBusSubscriber(modid = MyMod.MODID, value = Dist.CLIENT)
public final class ModClientEvents {
    private ModClientEvents() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.WYVERN.get(), WyvernRenderer::new);
    }
}
```

Ohne Anmeldung weiß der Client nicht, wie er den Mob zeichnen soll → Absturz, sobald einer in Sichtweite kommt.

## 9. Testen

```
/summon mymod:wyvern ~ ~ ~5
/summon mymod:wyvern ~ ~ ~ {NoAI:1b}                  ← bewegt sich nicht (zum Anschauen)
/summon mymod:wyvern ~ ~ ~ {PersistenceRequired:1b}   ← verschwindet nicht
/damage @e[type=mymod:wyvern,limit=1] 50
/kill @e[type=mymod:wyvern]
```

- **F3 + B** zeigt Hitboxen und Blickrichtung.
- Im **Survival** testen – Kreativ-Spieler werden von feindlichen Mobs ignoriert.

## Checkliste: neuer Mob

- [ ] `EntityType` in `ModEntities` (mit `sized`!)
- [ ] Entity-Klasse mit Konstruktor, `createAttributes`, `registerGoals`
- [ ] Attribute in `MyMod` angemeldet
- [ ] Modell, Animationen, Textur mit gleichem Namen
- [ ] Renderer + Anmeldung in `ModClientEvents`
- [ ] `lang`: `entity.mymod.wyvern`
- [ ] optional: Spawn-Ei, Loot-Table (`data/mymod/loot_table/entities/wyvern.json`), natürliches Spawnen

## Zum Ausprobieren

- Mach den Mob neutral: kein `NearestAttackableTargetGoal`, nur `HurtByTargetGoal`.
- Lass ihn vor Wölfen fliehen (`AvoidEntityGoal<>(this, Wolf.class, 8.0F, 1.0, 1.2)`).
- Öffne `IronGolem` und schau, welche Goals der benutzt.
