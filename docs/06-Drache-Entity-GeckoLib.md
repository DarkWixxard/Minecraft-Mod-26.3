# 06 – Der Drache: Entity und GeckoLib

Der **Ulyssan-Drache** ist ein Boss-Mob. Das Modell (`Ulyssan.bbmodel`) ist ein sehr aufwendiges, fertiges GeckoLib-Modell mit 64 Knochen und 57 Animationen, das du von Maul bekommen hast.

Ein eigener Mob braucht immer diese Bausteine:

| Baustein | Datei | Seite |
|---|---|---|
| **EntityType registrieren** | `entity/ModEntities.java` | beide |
| **Entity-Klasse** (Verhalten, KI, Werte) | `entity/custom/UlyssanDragonEntity.java` | beide |
| **Attribute anmelden** | `RubyMod.java` | beide |
| **Renderer** (Aussehen) | `entity/client/UlyssanDragonRenderer.java` | nur Client |
| **Renderer anmelden** | `entity/client/ModClientEvents.java` | nur Client |
| **Modell, Animationen, Textur** | `assets/rubymod/geckolib/…`, `textures/entity/…` | nur Client |
| **Name** | `lang/*.json`: `"entity.rubymod.ulyssan_dragon"` | nur Client |

> **Client und Server:** Minecraft läuft immer in zwei Hälften. Der **Server** entscheidet alles (Leben, KI, Schaden, wo der Drache ist). Der **Client** zeigt es nur an (Modell, Animation, Bossbar). Im Einzelspieler laufen beide in einem Programm, aber getrennt. Deshalb gibt es Dinge, die nur in `client/` dürfen.

## 1. EntityType registrieren (`entity/ModEntities.java`)

```java
public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, RubyMod.MODID);

public static final RegistryObject<EntityType<UlyssanDragonEntity>> ULYSSAN_DRAGON =
        ENTITY_TYPES.register("ulyssan_dragon", () ->
                EntityType.Builder.of(UlyssanDragonEntity::new, MobCategory.MONSTER)
                        .sized(5.0F, 4.0F)            // Hitbox: 5 Blöcke breit, 4 hoch
                        .clientTrackingRange(10)      // großer Mob: auch aus der Ferne sichtbar
                        .build(ENTITY_TYPES.key("ulyssan_dragon")));
```

| Teil | Bedeutung |
|---|---|
| `UlyssanDragonEntity::new` | **Methodenreferenz** auf den Konstruktor: „So erzeugst du einen neuen Drachen.“ |
| `MobCategory.MONSTER` | Kategorie für Spawn-Regeln und Despawn (siehe [10](10-Drache-Boss.md)) |
| `sized(breite, höhe)` | Die Haupt-Hitbox in Blöcken. Ohne `sized` nimmt Minecraft 0.6 × 1.8 (Spielergröße). |
| `clientTrackingRange(10)` | In Chunks: Bis zu dieser Entfernung bekommen Spieler den Drachen geschickt. |
| `.build(ENTITY_TYPES.key(...))` | Seit 1.21.2 braucht auch der EntityType seine ID beim Bauen. |

`ModEntities.ENTITY_TYPES.register(modBusGroup);` steht in `RubyMod`. Bei dir ist `.sized(...)` gerade auskommentiert, siehe [07 – Hitboxen](07-Drache-Hitboxen.md#7-die-haupt-hitbox).

## 2. Attribute anmelden (`RubyMod.java`)

Jeder lebende Mob **muss** Attribute haben, sonst stürzt das Spiel beim Spawnen ab.

```java
EntityAttributeCreationEvent.BUS.addListener(RubyMod::registerAttributes);

private static void registerAttributes(EntityAttributeCreationEvent event) {
    event.put(ModEntities.ULYSSAN_DRAGON.get(), UlyssanDragonEntity.createAttributes().build());
}
```

## 3. Die Entity-Klasse – Grundgerüst

```java
public class UlyssanDragonEntity extends Monster implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public UlyssanDragonEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
```

- `extends Monster`: Der Drache erbt alles, was ein feindlicher Mob kann: Er zählt als feindlich, kann angreifen, verschwindet auf „Friedlich“ und folgt den Despawn-Regeln für Monster.
- `implements GeoEntity`: Für GeckoLib. Verlangt `registerControllers` (Animationen, siehe [08](08-Drache-Animationen.md)) und `getAnimatableInstanceCache`.
- **Konstruktor:** die Methode, die genauso heißt wie die Klasse und **keinen Rückgabetyp** hat. Sie läuft bei jedem neuen Drachen (Spawnen, `/summon`, Laden der Welt).

### Attribute

```java
public static AttributeSupplier.Builder createAttributes() {
    return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 1024.0)
            .add(Attributes.MOVEMENT_SPEED, 0.25)
            .add(Attributes.ATTACK_DAMAGE, 50.0)
            .add(Attributes.FOLLOW_RANGE, 30.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 10.0)
            .add(Attributes.STEP_HEIGHT, 2.0)
            .add(Attributes.FLYING_SPEED, 0.5)
            .add(Attributes.FALL_DAMAGE_MULTIPLIER, 0.0);   // kein Fallschaden
}
```

| Attribut | Wert | Erklärung | Grenzen |
|---|---|---|---|
| `MAX_HEALTH` | 1024 | Leben (1 Herz = 2) | **max. 1024** (siehe [10](10-Drache-Boss.md#4-warum-maximal-1024-leben)) |
| `MOVEMENT_SPEED` | 0.25 | Laufgeschwindigkeit. Zombie 0.23, Spieler 0.1 (anders berechnet). 1.0 wäre extrem schnell. | – |
| `ATTACK_DAMAGE` | 50 | Schaden pro Biss. Netherite-Rüstung hält das nicht lange aus. | max. 2048 |
| `FOLLOW_RANGE` | 30 | Bis zu dieser Entfernung (Blöcke) verfolgt er Ziele | max. 2048 |
| `KNOCKBACK_RESISTANCE` | 10 | Rückstoß-Resistenz | **max. 1** (1 = gar kein Rückstoß). 10 wird auf 1 gesetzt. |
| `STEP_HEIGHT` | 2 | Wie hoch er ohne Springen steigen kann | – |
| `FLYING_SPEED` | 0.5 | Fluggeschwindigkeit (siehe [09](09-Drache-Fliegen.md)) | – |
| `FALL_DAMAGE_MULTIPLIER` | 0 | Fallschaden × 0 = kein Fallschaden | – |

Zu große Werte werden **ohne Fehlermeldung** auf das Maximum gesetzt.

### Die KI (Goals)

```java
@Override
protected void registerGoals() {
    this.goalSelector.addGoal(0, new FloatGoal(this));
    this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
    this.goalSelector.addGoal(4, new DragonFlyAroundGoal());
    this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8) {
        @Override
        public boolean canUse() {
            return !isFlying() && super.canUse();   // nur am Boden herumlaufen
        }
    });
    this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
    this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

    this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
}
```

Es gibt **zwei Listen**:
- `goalSelector` – **was** der Mob tut (laufen, angreifen, schauen).
- `targetSelector` – **wen** er angreift.

Die Zahl ist die **Priorität**: kleiner = wichtiger. Ein Goal mit kleiner Zahl kann eines mit größerer Zahl unterbrechen, wenn beide dieselben „Körperteile“ brauchen (Bewegung, Blick …).

| Goal | Was es tut |
|---|---|
| `FloatGoal` | Schwimmt im Wasser an der Oberfläche, statt zu ertrinken |
| `MeleeAttackGoal(this, 1.2, false)` | Läuft zum Ziel und beißt. 1.2 = 120 % Geschwindigkeit dabei. `false` = vergisst das Ziel, wenn es außer Sicht ist. |
| `DragonFlyAroundGoal` | Eigenes Goal: in der Luft herumfliegen ([09](09-Drache-Fliegen.md)) |
| `WaterAvoidingRandomStrollGoal(this, 0.8)` | Zufällig herumlaufen, Wasser meiden |
| `LookAtPlayerGoal` | Spieler in 16 Blöcken Umkreis anschauen |
| `RandomLookAroundGoal` | Ab und zu zufällig umschauen |
| `HurtByTargetGoal` | Wer ihn verletzt, wird zum Ziel |
| `NearestAttackableTargetGoal<>(…, Player.class, true)` | Greift den nächsten Spieler an. `true` = nur wenn er ihn sehen kann. |

**Anonyme Klasse:** `new WaterAvoidingRandomStrollGoal(this, 0.8) { ... }` erzeugt eine kleine Unterklasse ohne Namen und überschreibt darin `canUse`. Die Klammern am Ende: `}` schließt die Klasse, `)` schließt `addGoal(`, `;` beendet den Befehl. Steht das `);` direkt hinter `(this, 0.8)`, hängt `canUse` lose in der Methode und nichts kompiliert.

## 4. Die GeckoLib-Dateien

```
assets/rubymod/geckolib/models/entity/ulyssan_dragon.geo.json            ← Modell (64 Knochen)
assets/rubymod/geckolib/animations/entity/ulyssan_dragon.animation.json  ← 57 Animationen
assets/rubymod/textures/entity/ulyssan_dragon.png                        ← Textur
```

GeckoLib 5 baut die Pfade aus **einem Namen** zusammen: `<Unterordner>/<Name>`. Für Entities ist der Unterordner `entity`. Deshalb müssen **alle drei Dateien gleich heißen** (`ulyssan_dragon`).

### Aus `.bbmodel` exportieren

`.bbmodel` ist die **Projektdatei** von Blockbench (wie `.psd` bei Photoshop). Minecraft kann sie nicht lesen. In Blockbench mit installiertem GeckoLib-Plugin:
- **File → Export → Export GeckoLib Model** → `.geo.json`
- **Animation → Export Animations** → `.animation.json`
- Textur über Rechtsklick → **Save As** → `.png`

### Die Animations-Namen

In der `.animation.json` hat jede Animation einen Namen, z. B. `"animation.ulyssan_dragon.idle_calm"`. **Genau dieser Text** muss im Code stehen. Passt er nicht, kommt:

```
Unable to find animation: 'animation.ulyssan_dragon.idle_calm' for ulyssan_dragon
```

Ursprünglich hießen die Animationen `animation.ulyssan.*`. Du hast sie in der JSON auf `animation.ulyssan_dragon.*` umbenannt, jetzt passen Datei und Code zusammen.

Alle 57 Animationen nach Gruppen:

| Gruppe | Animationen |
|---|---|
| Stehen | `idle_calm`, `idle_alert`, `idle_aggro` |
| Laufen | `walk_calm`, `walk_prowl`, `run_charge`, `step_back`, `turn_left`, `turn_right` |
| Nahkampf | `attack_bite`, `attack_bite_double`, `attack_claw_left`, `attack_claw_right`, `attack_claw_slam`, `attack_stomp`, `attack_tail_slam`, `attack_tail_sweep`, `attack_wing_buffet` |
| Luftangriffe | `attack_air_dive_bite`, `attack_air_wing_slash_left`, `attack_air_wing_slash_right` |
| Zauber | `cast_freezing`, `cast_frost_halo`, `cast_ice_armor`, `cast_ice_beam_vertical`, `cast_ice_breath`, `cast_ice_breath_air`, `cast_ice_breath_sweep`, `cast_snowstorm`, `cast_wind_control` |
| Fliegen | `fly_takeoff`, `fly_hover`, `fly_cruise`, `fly_glide`, `fly_dive`, `fly_bank_left`, `fly_bank_right`, `fly_land` |
| Fallen | `fall_stagger`, `fall_tumble`, `fall_spiral`, `fall_impact`, `fall_recover` |
| Treffer | `hurt_light`, `hurt_heavy` |
| Tod | `death_ground`, `death_air`, `death_frozen`, `death_shatter` |
| Sonstiges | `roar`, `enrage_roar`, `spawn_intro`, `sleep_coil_down`, `sleep_doze`, `sleep_loop_curled`, `sleep_loop_tucked`, `sleep_wake` |

(alle mit dem Präfix `animation.ulyssan_dragon.`)

## 5. Der Renderer (`entity/client/UlyssanDragonRenderer.java`)

```java
public class UlyssanDragonRenderer<R extends LivingEntityRenderState & GeoRenderState>
        extends GeoEntityRenderer<UlyssanDragonEntity, R> {

    public UlyssanDragonRenderer(EntityRendererProvider.Context context) {
        // geckolib/models/entity/ulyssan_dragon.geo.json,
        // geckolib/animations/entity/ulyssan_dragon.animation.json,
        // textures/entity/ulyssan_dragon.png
        super(context, new DefaultedEntityGeoModel<>(
                Identifier.fromNamespaceAndPath(RubyMod.MODID, "ulyssan_dragon")));
        withScale(0.5F);   // das Modell ist riesig (ca. 24 Blöcke Spannweite) → halbe Größe
    }
}
```

- `GeoEntityRenderer` macht die ganze Arbeit: Modell laden, Animation abspielen, zeichnen.
- `DefaultedEntityGeoModel` sucht die drei Dateien nach dem Schema oben.
- `<R extends LivingEntityRenderState & GeoRenderState>` ist ein **Generic** mit zwei Bedingungen. Seit 1.21.2 zeichnet Minecraft nicht mehr direkt die Entity, sondern einen „RenderState“ (eine Momentaufnahme). GeckoLib verlangt, dass der RenderState beides kann.
- `withScale(0.5F)` halbiert das Modell. Alle Hitbox-Positionen in [07](07-Drache-Hitboxen.md) sind für diese Größe gemessen.

### Renderer anmelden (`entity/client/ModClientEvents.java`)

```java
@Mod.EventBusSubscriber(modid = RubyMod.MODID, value = Dist.CLIENT)
public final class ModClientEvents {
    private ModClientEvents() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ULYSSAN_DRAGON.get(), UlyssanDragonRenderer::new);
    }
}
```

Ohne diese Anmeldung weiß der Client nicht, wie er den Drachen zeichnen soll, und das Spiel stürzt ab, sobald einer in Sichtweite kommt.

**Name des Drachen:** Für den Namen über dem Kopf, in Todesnachrichten und beim Spawn-Ei braucht es noch einen Eintrag `"entity.rubymod.ulyssan_dragon"` in den Sprachdateien. Der fehlt im Moment noch (siehe [11](11-Fehler-und-Loesungen.md)).

## 6. Testen

```
/summon rubymod:ulyssan_dragon ~ ~ ~5
/summon rubymod:ulyssan_dragon ~ ~ ~ {Flying:1b}         ← direkt fliegend
/summon rubymod:ulyssan_dragon ~ ~ ~ {NoAI:1b}           ← bewegt sich nicht (zum Anschauen)
/damage @e[type=rubymod:ulyssan_dragon,limit=1] 200
/kill @e[type=rubymod:ulyssan_dragon]
```

- **F3 + B** zeigt alle Hitboxen.
- Im Survival testen, sonst greift er dich nicht an (Kreativ-Spieler werden ignoriert).
