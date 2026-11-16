# 04 – Rüstung

Es gibt zwei Wege:

| | Normale Rüstung | 3D-Rüstung mit GeckoLib |
|---|---|---|
| Aussehen am Körper | flache Textur wie Vanilla | echtes 3D-Modell (Stacheln, Platten …) |
| Aufwand | gering | hoch (Modell in Blockbench) |
| Klasse | normales `Item` | eigene Klasse (`implements GeoItem`) |
| Bibliothek | keine | GeckoLib |

## 1. Das Rüstungsmaterial

```java
// Verweist auf assets/mymod/equipment/sapphire.json
public static final ResourceKey<EquipmentAsset> SAPPHIRE_EQUIPMENT_ASSET =
        ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(MyMod.MODID, "sapphire"));

public static final ArmorMaterial SAPPHIRE_ARMOR = new ArmorMaterial(
        28,                       // Haltbarkeits-Multiplikator
        Map.of(
                ArmorType.HELMET, 3,
                ArmorType.CHESTPLATE, 7,
                ArmorType.LEGGINGS, 6,
                ArmorType.BOOTS, 3,
                ArmorType.BODY, 9  // Pferde-/Wolfsrüstung
        ),
        15,                       // Verzauberbarkeit
        SoundEvents.ARMOR_EQUIP_DIAMOND,
        1.0F,                     // Härte
        0.0F,                     // Rückstoß-Resistenz (0 bis 1)
        ModTags.REPAIRS_SAPPHIRE_ARMOR,
        SAPPHIRE_EQUIPMENT_ASSET
);
```

### Die Werte verstehen

| Wert | Bedeutung | Vanilla zum Vergleich |
|---|---|---|
| Haltbarkeits-Multiplikator | × Grundwert des Teils (Helm 11, Brust 16, Hose 15, Stiefel 13) | Leder 5, Eisen 15, Diamant 33, Netherite 37 |
| Rüstungspunkte | pro Teil, ½ Brustplatte-Symbol = 1 | Diamant 3/8/6/3 = 20; Obergrenze insgesamt **30** |
| Verzauberbarkeit | höher = bessere Verzauberungen | Eisen 9, Diamant 10, Gold 25 |
| Härte | schwächt große Treffer ab | Diamant 2, Netherite 3 |
| Rückstoß-Resistenz | pro Teil, 4 Teile addieren sich, max. 1 | Netherite 0.1 |

Es gibt keine kleinen Grenzen bei den Multiplikatoren (40 geht z. B. problemlos). Kommt eine Änderung im Spiel nicht an: Spiel neu starten und ein **neues** Teil aus dem Kreativ-Tab nehmen. **F3 + H** zeigt die Haltbarkeit im Tooltip.

## 2. Die Items

```java
public static final RegistryObject<Item> SAPPHIRE_HELMET     = armor("sapphire_helmet",     ModMaterials.SAPPHIRE_ARMOR, ArmorType.HELMET);
public static final RegistryObject<Item> SAPPHIRE_CHESTPLATE = armor("sapphire_chestplate", ModMaterials.SAPPHIRE_ARMOR, ArmorType.CHESTPLATE);
public static final RegistryObject<Item> SAPPHIRE_LEGGINGS   = armor("sapphire_leggings",   ModMaterials.SAPPHIRE_ARMOR, ArmorType.LEGGINGS);
public static final RegistryObject<Item> SAPPHIRE_BOOTS      = armor("sapphire_boots",      ModMaterials.SAPPHIRE_ARMOR, ArmorType.BOOTS);

private static RegistryObject<Item> armor(String name, ArmorMaterial material, ArmorType type) {
    return item(name, p -> p.humanoidArmor(material, type));
}
```

`humanoidArmor(material, type)` setzt alles auf einmal: Slot, Rüstungspunkte, Haltbarkeit, Reparatur-Material und Aussehen. Das Material als **Parameter** zu übergeben (statt fest im Code) macht die Methode für jedes Rüstungs-Set wiederverwendbar.

## 3. Wie Rüstung aussieht – zwei verschiedene Dinge

| Wo? | Dateien |
|---|---|
| **Im Inventar** (Icon) | `items/sapphire_helmet.json` → `models/item/sapphire_helmet.json` → `textures/item/sapphire_helmet.png` (wie jedes Item) |
| **Am Körper** | `equipment/sapphire.json` → `textures/entity/equipment/<Ebene>/sapphire.png` |

### Am Körper: `assets/mymod/equipment/sapphire.json`

```json
{
  "layers": {
    "humanoid":          [ { "texture": "mymod:sapphire" } ],
    "humanoid_leggings": [ { "texture": "mymod:sapphire" } ],
    "humanoid_baby":     [ { "texture": "mymod:sapphire" } ]
  }
}
```

Minecraft baut aus dem **Namen der Ebene** den Pfad selbst:

```
textures/entity/equipment/<Ebene>/sapphire.png
```

Deshalb **müssen** es drei Ordner sein:

| Ebene | Größe | Wofür |
|---|---|---|
| `humanoid/sapphire.png` | 64×32 | Helm, Brust, Stiefel an Erwachsenen |
| `humanoid_leggings/sapphire.png` | 64×32 | nur die Hose. Eigene Datei, weil Hose und Brust/Stiefel **dieselben Stellen** im Bild benutzen. Die Hose wird auf ein etwas kleineres, inneres Modell gezeichnet. |
| `humanoid_baby/sapphire.png` | 64×64 | Rüstung an Baby-Mobs |

**Tipp:** Die Vanilla-Texturen (z. B. `textures/entity/equipment/humanoid/diamond.png` in der Minecraft-Bibliothek) als Vorlage nehmen – dann stimmt die Aufteilung (welcher Pixel wohin gehört) automatisch.

## 4. 3D-Rüstung mit GeckoLib

### Was GeckoLib ist

[GeckoLib](https://modrinth.com/mod/geckolib) ist eine Bibliothek für **3D-Modelle mit Animationen** – für Mobs, Items und Rüstung. Modelle baut man in Blockbench mit dem GeckoLib-Plugin. Einbinden siehe [01](01-Einrichtung-und-Grundlagen.md#5-die-build-dateien).

### Dateien

| Datei | Inhalt |
|---|---|
| `assets/mymod/geckolib/models/item/armor/crystal_armor.geo.json` | das 3D-Modell der ganzen Rüstung |
| `assets/mymod/textures/item/armor/crystal_armor.png` | die Textur |

GeckoLib baut die Pfade aus dem Namen im Code (`"armor/crystal_armor"`). **Dateinamen müssen exakt passen und klein geschrieben sein.**

### Items registrieren

```java
public static final RegistryObject<Item> CRYSTAL_HELMET = geoArmor("crystal_helmet", ModMaterials.CRYSTAL_ARMOR, ArmorType.HELMET);
// ... CHESTPLATE, LEGGINGS, BOOTS

private static RegistryObject<Item> geoArmor(String name, ArmorMaterial material, ArmorType type) {
    return ITEMS.register(name, () -> new CrystalArmorItem(new Item.Properties()
            .setId(ITEMS.key(name))
            .humanoidArmor(material, type)));
}
```

### Die Klasse

```java
// Rüstungsteil, das am Körper mit dem 3D-GeckoLib-Modell gezeichnet wird
public class CrystalArmorItem extends Item implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public CrystalArmorItem(Properties properties) {
        super(properties);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoArmorRenderer<?, ?> renderer;

            @Override
            public GeoArmorRenderer<?, ?> getGeoArmorRenderer(ItemStack stack, EquipmentSlot slot) {
                if (this.renderer == null) {
                    // geckolib/models/item/armor/crystal_armor.geo.json + textures/item/armor/crystal_armor.png
                    this.renderer = new GeoArmorRenderer<>(new DefaultedItemGeoModel<>(
                            Identifier.fromNamespaceAndPath(MyMod.MODID, "armor/crystal_armor")));
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

- `implements GeoItem` = dieses Item hat ein GeckoLib-Modell.
- `createGeoRenderer` liefert den Renderer, der das Modell auf den Körper zeichnet. Er wird beim ersten Gebrauch erzeugt und dann wiederverwendet (`if (renderer == null)`).
- `GeckoLibUtil.createInstanceCache(this)` – so heißt es in GeckoLib 5 (ältere Tutorials: `createAnimatableInstanceCache`).
- `registerControllers` muss es geben, auch ohne Animationen.
- Das Material braucht trotzdem ein `equipment/…json` (siehe oben). GeckoLib ersetzt nur das Zeichnen.

## 5. Die Rüstung in Blockbench bauen

### Plugin und Projekt

1. **File → Plugins…** → *GeckoLib* suchen → **„GeckoLib Models & Animations“** installieren (aktuell 4.2.5, braucht Blockbench 5). Danach Blockbench **komplett neu starten**.
2. **File → New → GeckoLib Animated Model**.
3. Im Fenster **Model Type: Armor** wählen.
   - Erscheint stattdessen das normale Fenster mit „File Name“ und „Default UV Mode“: Plugin veraltet oder Blockbench nicht neu gestartet.
   - Bereits angelegtes leeres Projekt: **File → Project…** → Model Type auf Armor. Die Vorlage entsteht nur in einem **leeren** Projekt.

### Die Knochen (Bones)

Die Vorlage enthält eine Spielerfigur. Die Rüstungs-Knochen liegen **in** den Körper-Knochen:

```
bipedHead      → armorHead
bipedBody      → armorBody
bipedRightArm  → armorRightArm
bipedLeftArm   → armorLeftArm
bipedLeftLeg   → armorLeftLeg,  armorLeftBoot
bipedRightLeg  → armorRightLeg, armorRightBoot
```

- GeckoLib sucht **genau diese Namen** (Groß-/Kleinschreibung!).
- Würfel in `armorHead` erscheinen nur mit Helm, in `armorLeftBoot` nur mit Stiefeln usw. Die Hose besteht nur aus den Bein-Knochen – ein Hosenbund muss in eines der Beine.
- Der **Drehpunkt (Pivot)** jedes `armor…`-Knochens muss **gleich** dem seines `biped…`-Elternknochens sein. Sonst dreht sich die Rüstung beim Laufen um eine falsche Stelle und rutscht vom Körper.

### Vorhandene Modelle übernehmen

Hast du schon Inventar-Modelle (Java Block/Item) der Rüstungsteile:
1. **File → Project…**: *Default UV Mode* **Per-face UV** (sonst verrutschen freie UVs), *Texture Size* wie die Textur, Textur hinzufügen.
2. Im Tab des Teils Würfel markieren, **Strg + C**, im GeckoLib-Tab den **Ziel-Knochen** anklicken, **Strg + V**.
3. Auf den Körper schieben. Inventar-Modelle sind um die Blockmitte (8, 8, 8) gebaut, die Rüstung muss auf die Figur.

### Verschieben ohne Ärger

- Das Feld **Position** zeigt nur die **untere, hintere, linke Ecke** (`from`) – bei mehreren markierten Würfeln nur die des ersten. Zahlen dort einzutippen geht oft schief.
- Besser: alle Würfel eines Teils markieren und mit dem **Verschieben-Werkzeug** an den Pfeilen ziehen – Würfel **und** Drehpunkte wandern mit. **Strg** beim Ziehen = feineres Einrasten.
- In der Werkzeugleiste **Global** statt **Parent**.

### Damit die Rüstung perfekt sitzt

Maße des Spielerkörpers in Blockbench:

| Körperteil | X | Y | Z |
|---|---|---|---|
| Kopf | −4 bis 4 | 24 bis 32 | −4 bis 4 |
| Körper | −4 bis 4 | 12 bis 24 | −2 bis 2 |
| rechter Arm | 4 bis 8 | 12 bis 24 | −2 bis 2 |
| linker Arm | −8 bis −4 | 12 bis 24 | −2 bis 2 |
| linkes Bein | −4 bis 0 | 0 bis 12 | −2 bis 2 |
| rechtes Bein | 0 bis 4 | 0 bis 12 | −2 bis 2 |

(„Schlanke“ Skins haben 3 statt 4 Pixel breite Arme.)

Abstands-Regeln wie bei Vanilla:
- Helm, Brust, Arme, Stiefel: etwa **1 Pixel** Abstand zur Haut.
- Hose: etwa **0,5 Pixel**, damit sie unter Brust und Stiefeln liegt.
- Stiefelsohle bis **y = −1**.
- Nichts **innerhalb** der Körper-Bereiche (sonst schaut Haut durch). Deko wie Zacken darf abstehen.
- Tipp: graue Referenz-Würfel mit genau diesen Maßen in die `biped…`-Knochen legen, daran ausrichten, vor dem Export löschen.

### In der exportierten Datei ist X gespiegelt

Blockbench dreht beim Export das Vorzeichen von X um. Wer Pivots direkt in der `.geo.json` ändert:

| Knochen | Pivot in der **Datei** |
|---|---|
| `armorLeftLeg`, `armorLeftBoot` | `[2, 12, 0]` |
| `armorRightLeg`, `armorRightBoot` | `[-2, 12, 0]` |

Gemeint ist das `"pivot"` beim Knochen, nicht die der einzelnen Würfel. Änderungen nur in der Datei gehen beim nächsten Export verloren – am saubersten in Blockbench korrigieren.

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

// Wird jeden Tick für jedes Item im Inventar aufgerufen. Nur der Helm vergibt den Bonus, sonst passiert es 4x.
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
        if (!(entity.getItemBySlot(armorSlot).getItem() instanceof CrystalArmorItem)) {
            return false;
        }
    }
    return true;
}
```

Das funktioniert in **jeder** Rüstungs-Klasse, nicht nur bei GeckoLib. Für normale Rüstung brauchst du dafür eine eigene Klasse (`extends Item`) statt `item(...)`.

### So funktioniert es

- `inventoryTick` ruft Minecraft **jeden Tick** (20× pro Sekunde) für jedes Item im Inventar auf. `slot` sagt, wo es steckt. Der Parameter `ServerLevel` zeigt: läuft nur auf dem Server.
- Nur beim **Helm** (`slot == HEAD`) wird geprüft, sonst vergäbe jedes der 4 Teile die Effekte.
- `instanceof CrystalArmorItem` prüft alle 4 Slots. Willst du nur ein bestimmtes Material, vergleiche stattdessen die Items (`== ModItems.CRYSTAL_HELMET.get()` …).
- 300 Ticks (15 s) Dauer, erneuert unter 220 Ticks → läuft nie ab, solange man das Set trägt, und endet kurz nach dem Ausziehen.
- `new MobEffectInstance(effekt, dauer, stufe, ambient, sichtbar)`: `ambient = true` = dezente Partikel, `sichtbar = false` = keine Partikel.
- `record SetEffect(...)` = kurze Klasse, die nur Daten hält.

### Stufen

- Die Zahl ist **Stufe − 1**: `0` = I, `1` = II.
- **Resistenz** ab `4` (Stufe V) = immun gegen jeden Schaden.
- Über etwa Stufe X zeigt das Spiel keine schöne Zahl mehr (`potion.potency.40`).
- Faire Sets: `0` bis `2`.

Effekt-Namen in 26.x: `STRENGTH`, `RESISTANCE`, `SPEED`, `HASTE`, `JUMP_BOOST`, `NIGHT_VISION`, `REGENERATION`, `FIRE_RESISTANCE`, `WATER_BREATHING`, `SLOW_FALLING`, `ABSORPTION` … (alle in der Klasse `MobEffects`).

## Checklisten

**Normale Rüstung**
- [ ] `ArmorMaterial` + `EquipmentAsset`-Key in `ModMaterials`
- [ ] Reparatur-Tag (`ModTags` + JSON)
- [ ] 4 Items mit `humanoidArmor`
- [ ] `equipment/<name>.json`
- [ ] 3 Körper-Texturen (`humanoid`, `humanoid_leggings`, `humanoid_baby`)
- [ ] 4 Inventar-Icons (`items/`, `models/item/`, `textures/item/`)
- [ ] `lang`, Kreativ-Tab, Rezepte
- [ ] Vanilla-Tags `head_armor.json` usw.

**3D-Rüstung zusätzlich**
- [ ] GeckoLib in `build.gradle` und `mods.toml`
- [ ] Modell in Blockbench (*Model Type: Armor*), Pivots geprüft
- [ ] `.geo.json` und Textur unter den richtigen Namen
- [ ] eigene Klasse mit `GeoItem` und `createGeoRenderer`

## Zum Ausprobieren

- Gib einem Set nur dann Feuerresistenz, wenn der Spieler in Lava steht (`living.isInLava()`).
- Mach Stiefel, die allein schon Sprungkraft geben (`slot == EquipmentSlot.FEET`).
- Schau in die Vanilla-Klasse `Items` → `TURTLE_HELMET`: Wie bekommt der Schildkrötenpanzer seine Wasseratmung?
