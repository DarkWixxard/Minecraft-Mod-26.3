# 20 – Spawn-Eier und natürliches Spawnen

Ein eigener Mob ([06](06-Eigener-Mob-mit-GeckoLib.md)) existiert erst, wenn man ihn mit `/summon` ruft. Dieses Kapitel zeigt, wie er per **Spawn-Ei** und **von selbst** in der Welt erscheint.

> ✅ Getestet mit Forge 26.3-66.1.0 auf dem Server: Spawn-Ei-Item registriert, Biome Modifier geladen – `/mymod spawns` ([13](13-Befehle.md)) zeigt den Mob mit dem eingestellten Gewicht in der Spawn-Liste der Ebene.

## 1. Spawn-Ei

```java
public static final RegistryObject<Item> WYVERN_SPAWN_EGG = ITEMS.register("wyvern_spawn_egg",
        () -> new SpawnEggItem(new Item.Properties()
                .setId(ITEMS.key("wyvern_spawn_egg"))
                .spawnEgg(ModEntities.WYVERN.get())));
```

- `.spawnEgg(type)` sagt dem Ei, welchen Mob es erzeugt.
- Seit 1.21.5 haben Spawn-Eier **keine zwei Farben** mehr, sondern eine eigene Textur – wie ein normales Item:

```json
// assets/mymod/items/wyvern_spawn_egg.json
{ "model": { "type": "minecraft:model", "model": "mymod:item/wyvern_spawn_egg" } }

// assets/mymod/models/item/wyvern_spawn_egg.json
{ "parent": "minecraft:item/generated", "textures": { "layer0": "mymod:item/wyvern_spawn_egg" } }
```

Vorlage für die Textur: `textures/item/zombie_spawn_egg.png` in der Minecraft-Bibliothek.

- `lang`: `"item.mymod.wyvern_spawn_egg": "Wyvern-Spawn-Ei"`
- In den Kreativ-Tab `CreativeModeTabs.SPAWN_EGGS` einsortieren ([02](02-Bloecke-Items-Werkzeuge.md#in-vanilla-tabs-einsortieren)).

## 2. Natürliches Spawnen: zwei Teile

| Teil | Frage | Wo |
|---|---|---|
| **Biome Modifier** `forge:add_spawns` | In welchen Biomen, wie oft, wie viele auf einmal? | JSON |
| **Spawn-Regeln** (`SpawnPlacementRegisterEvent`) | Auf welchem Untergrund, bei welchem Licht? | Java |

### Biome Modifier

`data/mymod/forge/biome_modifier/add_wyvern_spawns.json`:

```json
{
  "type": "forge:add_spawns",
  "biomes": "#minecraft:is_mountain",
  "spawners": [
    { "type": "mymod:wyvern", "count": 1, "weight": 5 }
  ]
}
```

| Feld | Bedeutung |
|---|---|
| `biomes` | Biom, Liste oder Tag (`#minecraft:is_overworld`, `#minecraft:is_mountain`, `minecraft:plains` …) |
| `type` | der Mob |
| `count` | wie viele **in einer Gruppe** erscheinen. Fester Wert oder Bereich: `{ "type": "minecraft:uniform", "min_inclusive": 1, "max_inclusive": 3 }` |
| `weight` | Gewicht im Vergleich zu den anderen Mobs der Kategorie im Biom |

**Gewichte zum Einordnen** (Monster in der Ebene): Spinne, Skelett, Creeper, Schleim je 100, Zombie 90, Enderman 10, Hexe 5. Ein Boss mit Gewicht 5 ist also selten; mit 100 wäre er so häufig wie Creeper.

> ⚠️ **Format-Falle:** Im Code-Kommentar von Forge 26.3 (`ForgeBiomeModifiers`) steht noch das alte Format mit `"minCount"` und `"maxCount"`. Das gilt nicht mehr – es heißt `"count"`. Getestet ist das Format oben. Allgemein: Wenn eine Doku und der Code sich widersprechen, gilt der Code bzw. das, was Vanilla selbst benutzt (`data/minecraft/worldgen/biome/plains.json` → `natural_mob_spawns`).

In welcher **Kategorie** (Monster, Tier, Wasser …) gespawnt wird, kommt aus der `MobCategory` des EntityTypes ([06](06-Eigener-Mob-mit-GeckoLib.md#3-entitytype-registrieren)). Monster spawnen nur im Dunkeln und zählen gegen die Monster-Obergrenze.

### Spawn-Regeln

```java
@Mod.EventBusSubscriber(modid = MyMod.MODID)
public final class ModSpawnEvents {
    private ModSpawnEvents() {}

    // Wo darf der Mob natürlich erscheinen? Gleiche Regeln wie Zombies: auf dem Boden, im Dunkeln
    @SubscribeEvent
    public static void registerSpawnPlacements(SpawnPlacementRegisterEvent event) {
        event.register(ModEntities.WYVERN.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules,
                SpawnPlacementRegisterEvent.Operation.REPLACE);
    }
}
```

| Parameter | Bedeutung |
|---|---|
| `SpawnPlacementTypes.ON_GROUND` | auf festem Boden. `IN_WATER` für Fische, `NO_RESTRICTIONS` für Fliegendes |
| `Heightmap.Types.MOTION_BLOCKING_NO_LEAVES` | Höhe, ab der gesucht wird (nicht auf Blättern) |
| Prüf-Methode | `Monster::checkMonsterSpawnRules` (dunkel genug, nicht friedlich), `Animal::checkAnimalSpawnRules` (auf Gras, hell), `Mob::checkMobSpawnRules` (fast überall) |
| `Operation.REPLACE` | eigene Regel ersetzt eine bestehende. Bei einem neuen Mob egal; für Vanilla-Mobs gibt es `AND`/`OR`. |

Eigene Regel als Lambda, z. B. „nur über Y 100“:

```java
(type, level, reason, pos, random) -> pos.getY() > 100 && Monster.checkMonsterSpawnRules(type, level, reason, pos, random)
```

## 3. Testen

1. `/mymod spawns` (Debug-Befehl aus [13](13-Befehle.md#5-beispiel-spieldaten-auslesen)) in einem passenden Biom: Steht der Mob in der Liste?
2. Zum schnellen Ausprobieren das Gewicht vorübergehend auf 1000 setzen, im Survival-Modus nachts in das Biom gehen.
3. Natürliches Spawnen braucht einen Spieler in der Nähe (24–128 Blöcke) und Platz für die Hitbox des Mobs. Riesige Mobs finden selten genug Platz.
4. Bossen gibt man meist **kein** natürliches Spawnen, sondern einen Beschwörungs-Weg (Item, Altar-Block, Struktur).

## Checkliste

- [ ] Spawn-Ei mit `.spawnEgg(type)`, Textur, Modell, `lang`, Kreativ-Tab
- [ ] `forge:add_spawns`-JSON mit `count` und `weight`
- [ ] Spawn-Regeln im `SpawnPlacementRegisterEvent`
- [ ] mit Debug-Befehl geprüft, dass der Eintrag im Biom ankommt

## Zum Ausprobieren

- Ein friedliches Tier, das nur in Wüsten spawnt (`MobCategory.CREATURE`). Einen Tag `#minecraft:is_desert` gibt es in 26.3 **nicht** – hier das Biom direkt angeben (`"biomes": "minecraft:desert"`) oder im Ordner `data/minecraft/tags/worldgen/biome/` nachsehen, welche Tags es gibt.
- Einen Altar-Block ([14](14-Eigene-Block-Klassen.md)), der bei Rechtsklick mit einem bestimmten Item den Boss beschwört (`ModEntities.WYVERN.get().spawn(serverLevel, pos, EntitySpawnReason.TRIGGERED)`).
