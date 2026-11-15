# 04 – Rüstung

In der Mod gibt es zwei Arten von Rüstung:

| | Rubin-Rüstung | Noxium-Rüstung |
|---|---|---|
| Aussehen am Körper | flache Textur wie Vanilla | echtes 3D-Modell mit GeckoLib |
| Klasse | normales `Item` | eigene Klasse `NoxiumArmorItem` |
| Besonderheit | – | Set-Effekte, wenn alle 4 Teile getragen werden |

## 1. Das Rüstungsmaterial (`materials/ModMaterials.java`)

```java
public static final ResourceKey<EquipmentAsset> NOXIUM_EQUIPMENT_ASSET =
        ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(RubyMod.MODID, "noxium"));

public static final ArmorMaterial NOXIUM_ARMOR = new ArmorMaterial(
        60,                       // Haltbarkeits-Multiplikator (Eisen 15, Diamant 33, Netherite 37)
        Map.of(
                ArmorType.HELMET, 5,
                ArmorType.CHESTPLATE, 10,
                ArmorType.LEGGINGS, 8,
                ArmorType.BOOTS, 5,
                ArmorType.BODY, 11  // Pferde-/Wolfsrüstung
        ),
        30,                       // Verzauberbarkeit
        SoundEvents.ARMOR_EQUIP_DIAMOND,
        5.0F,                     // Härte (Diamant 2, Netherite 3)
        1.0F,                     // Rückstoß-Resistenz (0 bis 1; Netherite 0.1 pro Teil)
        ModTags.REPAIRS_NOXIUM_ARMOR,
        NOXIUM_EQUIPMENT_ASSET
);
```

- **Haltbarkeit** = Multiplikator × Grundwert des Teils (Helm 11, Brust 16, Hose 15, Stiefel 13). Mit 60: Helm 660, Brust 960 …
- **Rüstungspunkte** pro Teil. Vanilla-Maximum für ein volles Set ist 20 (Diamant/Netherite). Noxium hat 28. Minecraft deckelt die Rüstung insgesamt bei 30.
- **Härte** verringert, wie stark große Treffer die Rüstung durchschlagen.
- Es gibt **keine Grenze bei 40** für den Multiplikator. Wenn eine Änderung im Spiel nicht ankommt: Spiel neu starten (Java-Code wird nicht mit F3+T neu geladen) und ein **neues** Rüstungsteil aus dem Kreativ-Tab nehmen. Mit **F3 + H** siehst du die Haltbarkeit im Tooltip.

## 2. Die Rüstungs-Items (`item/ModItems.java`)

```java
// normale Rüstung
public static final RegistryObject<Item> RUBY_HELMET = armor("ruby_helmet", ArmorType.HELMET);

private static RegistryObject<Item> armor(String name, ArmorType type) {
    return item(name, p -> p.humanoidArmor(ModMaterials.RUBY_ARMOR, type));
}

// 3D-Rüstung mit GeckoLib
public static final RegistryObject<Item> NOXIUM_HELMET =
        geoArmor("noxium_helmet", ModMaterials.NOXIUM_ARMOR, ArmorType.HELMET);

private static RegistryObject<Item> geoArmor(String name, ArmorMaterial material, ArmorType type) {
    return ITEMS.register(name, () -> new NoxiumArmorItem(new Item.Properties()
            .setId(ITEMS.key(name))
            .humanoidArmor(material, type)));
}
```

`humanoidArmor(material, type)` setzt alles auf einmal: Slot, Rüstungspunkte, Haltbarkeit, Reparatur-Material und Aussehen.

## 3. Wie Rüstung aussieht – zwei völlig verschiedene Dinge

| Wo sieht man es? | Dateien |
|---|---|
| **Im Inventar** (Icon) | `items/noxium_helmet.json` → `models/item/noxium_helmet.json` → Textur |
| **Am Körper** | `equipment/noxium.json` → `textures/entity/equipment/<Ebene>/noxium.png` (bzw. das GeckoLib-Modell) |

### Am Körper: `assets/rubymod/equipment/ruby.json`

```json
{
  "layers": {
    "humanoid":          [ { "texture": "rubymod:ruby" } ],
    "humanoid_leggings": [ { "texture": "rubymod:ruby" } ],
    "humanoid_baby":     [ { "texture": "rubymod:ruby" } ]
  }
}
```

Minecraft baut aus dem **Namen der Ebene** den Pfad selbst zusammen:

```
textures/entity/equipment/<Ebene>/ruby.png
```

Deshalb **müssen** es drei Ordner sein – das lässt sich nicht zusammenlegen:

| Ebene | Größe | Wofür |
|---|---|---|
| `humanoid/ruby.png` | 64×32 | Helm, Brust, Stiefel an Erwachsenen |
| `humanoid_leggings/ruby.png` | 64×32 | Nur die Hose. Eigene Datei, weil Hose und Brust/Stiefel **dieselben Stellen** im Bild benutzen und sich sonst überschreiben würden. Die Hose wird auf ein etwas kleineres, inneres Modell gezeichnet. |
| `humanoid_baby/ruby.png` | 64×64 | Rüstung an Baby-Mobs (z. B. Baby-Zombies) |

### Im Inventar: 3D-Modelle

Die Noxium-Teile benutzen im Inventar echte 3D-Modelle (aus Blockbench als Java-Block/Item-Modell exportiert). Darin:

```json
"textures": {
  "0": "rubymod:item/armor/noxium_armor",
  "particle": "rubymod:item/armor/noxium_armor"
}
```

`particle` ist die Textur für die kleinen Splitter, wenn das Item kaputtgeht. Sie muss auf ein existierendes Bild zeigen.

## 4. 3D-Rüstung mit GeckoLib

### Was GeckoLib ist

[GeckoLib](https://modrinth.com/mod/geckolib) ist eine Bibliothek für **3D-Modelle mit Animationen**, für Mobs, Items und Rüstung. Die Modelle macht man in Blockbench mit dem GeckoLib-Plugin. Einbinden: siehe [01 – build.gradle](01-Einrichtung-und-Grundlagen.md#4-die-build-dateien).

### Die Dateien

| Datei | Inhalt |
|---|---|
| `assets/rubymod/geckolib/models/item/armor/noxium_armor.geo.json` | das 3D-Modell der ganzen Rüstung |
| `assets/rubymod/textures/item/armor/noxium_armor.png` | die Textur (128×128) |

GeckoLib baut die Pfade selbst aus dem Namen zusammen, den du im Code angibst (`"armor/noxium_armor"`). **Dateinamen müssen exakt passen und klein geschrieben sein** – eine Datei namens `CustomArmor.geo.json` wird nicht gefunden.

### Die Klasse `NoxiumArmorItem`

```java
public class NoxiumArmorItem extends Item implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoArmorRenderer<?, ?> renderer;

            @Override
            public GeoArmorRenderer<?, ?> getGeoArmorRenderer(ItemStack stack, EquipmentSlot slot) {
                if (this.renderer == null) {
                    // geckolib/models/item/armor/noxium_armor.geo.json + textures/item/armor/noxium_armor.png
                    this.renderer = new GeoArmorRenderer<>(new DefaultedItemGeoModel<>(
                            Identifier.fromNamespaceAndPath(RubyMod.MODID, "armor/noxium_armor")));
                }
                return this.renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // keine Animationen
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
```

- `implements GeoItem` sagt GeckoLib: Dieses Item hat ein GeckoLib-Modell.
- `createGeoRenderer` liefert den Renderer, der das Modell auf den Körper zeichnet. Er wird erst beim ersten Gebrauch erzeugt (`if (renderer == null)`) und dann wiederverwendet.
- `createInstanceCache(this)` speichert den Animations-Zustand. In GeckoLib 5 heißt die Methode so (in alten Tutorials: `createAnimatableInstanceCache`).
- `registerControllers` muss es geben, auch wenn die Rüstung keine Animationen hat.

## 5. Die Rüstung in Blockbench bauen

### Plugin und Projekt

1. **File → Plugins…** → nach **GeckoLib** suchen → **„GeckoLib Models & Animations“** installieren (aktuell 4.2.5, braucht Blockbench 5). Danach Blockbench **komplett neu starten**.
2. **File → New → GeckoLib Animated Model**.
3. Im Fenster **Model Type: Armor** wählen. Erscheint stattdessen das normale Fenster mit „File Name“ und „Default UV Mode“, ist das Plugin veraltet oder Blockbench wurde nicht neu gestartet.
4. Bereits angelegtes leeres Projekt: **File → Project…** öffnen und dort Model Type auf Armor stellen. Die Vorlage wird nur in einem **leeren** Projekt erzeugt.

### Die Knochen (Bones)

Das Plugin erzeugt eine Vorlage. Die Rüstungs-Knochen liegen **in** den Körper-Knochen:

```
bipedHead      → armorHead
bipedBody      → armorBody
bipedRightArm  → armorRightArm
bipedLeftArm   → armorLeftArm
bipedLeftLeg   → armorLeftLeg,  armorLeftBoot
bipedRightLeg  → armorRightLeg, armorRightBoot
```

GeckoLib sucht genau nach diesen Namen (Groß-/Kleinschreibung beachten!). Ein Würfel in `armorHead` erscheint nur, wenn der Helm getragen wird, ein Würfel in `armorLeftBoot` nur mit Stiefeln usw.

**Wichtig:** Der **Drehpunkt (Pivot)** jedes `armor…`-Knochens muss **genau gleich** sein wie der seines `biped…`-Elternknochens. Sonst dreht sich die Rüstung beim Laufen um eine falsche Stelle und rutscht vom Körper.

### Die Würfel aus den Inventar-Modellen übernehmen

1. **File → Project…**: *Default UV Mode* auf **Per-face UV**, *Texture Size* **128 × 128**, Textur `noxium_armor.png` hinzufügen.
2. Im Tab des Teils (z. B. `noxium_chestplate`) Würfel markieren, **Strg+C**, im GeckoLib-Tab den **Ziel-Knochen** anklicken, **Strg+V**.
3. Die Würfel heißen auf Portugiesisch. **„-1“ am Ende = links**, **„1“ = rechts**. Arm-Teile („braco“, „ombro“ …) gehören in die Arm-Knochen, der Hosenbund („Cintura das calcas“) in eines der Bein-Knochen.
4. Danach auf den Körper schieben. Die Inventar-Modelle sind um die Blockmitte (8, 8, 8) gebaut, die Rüstung muss auf die Spielerfigur.

### Tipps zum Verschieben

- Das Feld **Position** zeigt nur die **untere, hintere, linke Ecke** (`from`) – und bei mehreren markierten Würfeln nur die des ersten. Zahlen dort einzutippen geht oft schief.
- Besser: alle Würfel eines Teils markieren und mit dem **Verschieben-Werkzeug** an den Pfeilen ziehen. So wandern Würfel **und** ihre Drehpunkte mit. **Strg** beim Ziehen = feineres Einrasten.
- In der Werkzeugleiste **„Global“** statt **„Parent“** einstellen.

### Damit die Rüstung „perfekt sitzt“

Maße des Spielerkörpers in Blockbench-Koordinaten:

| Körperteil | X | Y | Z |
|---|---|---|---|
| Kopf | −4 bis 4 | 24 bis 32 | −4 bis 4 |
| Körper | −4 bis 4 | 12 bis 24 | −2 bis 2 |
| rechter Arm | 4 bis 8 | 12 bis 24 | −2 bis 2 |
| linker Arm | −8 bis −4 | 12 bis 24 | −2 bis 2 |
| linkes Bein | −4 bis 0 | 0 bis 12 | −2 bis 2 |
| rechtes Bein | 0 bis 4 | 0 bis 12 | −2 bis 2 |

Abstands-Regeln wie bei Vanilla:
- Helm, Brust, Arme, Stiefel: etwa **1 Pixel** Abstand zur Haut.
- Hose: etwa **0,5 Pixel**, damit sie unter Brust und Stiefeln liegt.
- Stiefelsohle darf bis **y = −1** gehen.
- Keine Fläche **innerhalb** der Körper-Bereiche (sonst schaut Haut durch). Deko wie Zacken und Schulterplatten darf abstehen.
- Tipp: graue Referenz-Würfel mit genau diesen Maßen in die `biped…`-Knochen legen und daran ausrichten. Vor dem Export wieder löschen.

### In der exportierten Datei ist X gespiegelt

Blockbench dreht beim Export das Vorzeichen von X um. Wenn du Pivots direkt in der `.geo.json` änderst, gilt in der **Datei**:

| Knochen | Pivot in der Datei |
|---|---|
| `armorLeftLeg` | `[2, 12, 0]` |
| `armorRightLeg` | `[-2, 12, 0]` |
| `armorLeftBoot` | `[2, 12, 0]` |
| `armorRightBoot` | `[-2, 12, 0]` |

Gemeint ist das `"pivot"` direkt beim Knochen, **nicht** die Pivots der einzelnen Würfel. Änderungen nur in der Datei gehen beim nächsten Export aus Blockbench verloren – am saubersten ist es, in Blockbench zu korrigieren und neu zu exportieren.

## 6. Set-Effekte (volle Rüstung)

```java
// Volles Set: Effekt + Stufe (0 = Stufe I, 1 = Stufe II, ...)
private static final List<SetEffect> FULL_SET_EFFECTS = List.of(
        new SetEffect(MobEffects.NIGHT_VISION, 0),
        new SetEffect(MobEffects.REGENERATION, 0),
        new SetEffect(MobEffects.RESISTANCE, 0),
        new SetEffect(MobEffects.STRENGTH, 0)
);
private record SetEffect(Holder<MobEffect> effect, int amplifier) {}

// Wird jeden Tick für jedes getragene Teil aufgerufen. Nur der Helm vergibt den Bonus, damit es nicht 4x passiert.
@Override
public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
    super.inventoryTick(stack, level, entity, slot);
    if (slot == EquipmentSlot.HEAD && entity instanceof LivingEntity living && isWearingFullSet(living)) {
        for (SetEffect setEffect : FULL_SET_EFFECTS) {
            MobEffectInstance current = living.getEffect(setEffect.effect());
            // kurz vor Ablauf erneuern (Nachtsicht flackert unter 10 Sekunden)
            if (current == null || current.getDuration() < 220) {
                living.addEffect(new MobEffectInstance(setEffect.effect(), 300, setEffect.amplifier(), true, false));
            }
        }
    }
}

private static boolean isWearingFullSet(LivingEntity entity) {
    for (EquipmentSlot armorSlot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
        if (!(entity.getItemBySlot(armorSlot).getItem() instanceof NoxiumArmorItem)) {
            return false;
        }
    }
    return true;
}
```

### So funktioniert es

- `inventoryTick` ruft Minecraft **jeden Tick** (20× pro Sekunde) für jedes Item im Inventar auf. `slot` sagt, wo es gerade steckt.
- Nur wenn `slot == HEAD`, also nur beim **Helm**, wird geprüft. Sonst würde jedes der 4 Teile die Effekte vergeben.
- `isWearingFullSet` prüft, ob in allen 4 Rüstungsslots ein `NoxiumArmorItem` steckt.
- Der Effekt dauert 300 Ticks (15 Sekunden) und wird erneuert, sobald er unter 220 Ticks fällt. So läuft er nie ab, solange man die Rüstung trägt, und hört kurz nach dem Ausziehen auf.
- `new MobEffectInstance(effekt, dauer, stufe, true, false)`: `true` = „ambient“ (dezente Partikel wie bei Beacons), `false` = keine Partikel.
- `record SetEffect(...)` ist eine kurze Schreibweise für eine Klasse, die nur Daten hält.

### Stufen

- Die Zahl ist **Stufe minus 1**: `0` = Stufe I, `1` = Stufe II.
- Resistenz ab Wert `4` (Stufe V) macht **immun gegen jeden Schaden**. Höher bringt nichts.
- Für ein starkes, aber faires Set sind Werte von `0` bis `2` üblich.
- Die Namen der Effekte in 26.3: `STRENGTH`, `RESISTANCE`, `SPEED`, `HASTE`, `JUMP_BOOST`, `NIGHT_VISION`, `REGENERATION`, `FIRE_RESISTANCE`, `WATER_BREATHING` … (in alten Tutorials heißen sie z. B. `DAMAGE_BOOST`, `MOVEMENT_SPEED`).
