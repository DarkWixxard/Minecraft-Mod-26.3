# 14 – Eigene Block-Klassen und Blockstates

Wie bei Items ([05](05-Eigene-Item-Klassen.md)): Solange ein Block nur Werte braucht, reicht `new Block(...)`. Soll er **reagieren** (Rechtsklick, Drauftreten, Zustände wechseln), braucht er eine eigene Klasse.

> ✅ Getestet mit Forge 26.3-66.1.0: kompiliert; `setblock mymod:sapphire_lamp[lit=true]` und Abfrage des Zustands auf dem Server. Rechtsklick und Drauftreten brauchen einen Spieler und sind nur kompiliert.

## 1. Häufig überschriebene Methoden

| Methode | Wann |
|---|---|
| `useWithoutItem(state, level, pos, player, hit)` | Rechtsklick (mit leerer Hand oder Item ohne eigene Aktion) |
| `useItemOn(stack, state, level, pos, player, hand, hit)` | Rechtsklick mit einem Item |
| `stepOn(level, pos, state, entity)` | eine Entity läuft darauf |
| `fallOn(level, state, pos, entity, fallDistance)` | eine Entity landet darauf – Fallschaden ändern (Heuballen, Schleimblock) |
| `randomTick(state, level, pos, random)` | zufällig, ca. alle 68 s (Pflanzen wachsen) – braucht `isRandomlyTicking` bzw. `.randomTicks()` |
| `tick(state, level, pos, random)` | geplant mit `level.scheduleTick(pos, this, ticks)` |
| `createBlockStateDefinition(builder)` | welche Eigenschaften der Block hat |

Immer mit **Strg + O** einfügen lassen – die Parameter ändern sich zwischen Versionen.

## 2. Blockstates: ein Block mit Zuständen

Ein **Blockstate** ist eine Variante eines Blocks: Eine Lampe ist an oder aus, eine Treppe zeigt nach Norden oder Süden, Weizen hat Stufe 0–7. Das ist **kein** eigener Block, sondern derselbe Block mit anderen Eigenschaften.

### Beispiel: Lampe zum An- und Ausschalten

```java
// Eine Lampe, die mit Rechtsklick an- und ausgeht
public class SapphireLampBlock extends Block {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public SapphireLampBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(LIT, false));   // beim Setzen aus
    }

    // Sagt Minecraft, welche Eigenschaften dieser Block hat
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    // Rechtsklick
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            boolean nowLit = !state.getValue(LIT);
            level.setBlock(pos, state.setValue(LIT, nowLit), Block.UPDATE_ALL);
            level.playSound(null, pos, ModSounds.LAMP_CLICK.get(), SoundSource.BLOCKS, 1.0F, nowLit ? 1.2F : 0.8F);
        }
        return InteractionResult.SUCCESS;
    }
}
```

| Teil | Erklärung |
|---|---|
| `BlockStateProperties.LIT` | fertige Vanilla-Eigenschaft (`true`/`false`). Weitere: `POWERED`, `FACING`, `HORIZONTAL_FACING`, `AGE_7`, `WATERLOGGED` … |
| eigene Eigenschaft | `BooleanProperty.create("glowing")`, `IntegerProperty.create("charge", 0, 3)`, `EnumProperty.create(...)` |
| `registerDefaultState` | Zustand beim Setzen |
| `createBlockStateDefinition` | **jede** Eigenschaft muss hier hinzugefügt werden, sonst Absturz beim `setValue` |
| `state.getValue(LIT)` / `state.setValue(LIT, x)` | lesen / **neuen** Zustand erzeugen (Blockstates sind unveränderlich) |
| `level.setBlock(pos, neuerZustand, Block.UPDATE_ALL)` | Zustand in der Welt setzen und an Spieler und Nachbarblöcke melden |
| `!level.isClientSide()` | nur der Server ändert die Welt; der Client bekommt die Änderung automatisch |
| `InteractionResult.SUCCESS` | Klick verarbeitet, Arm schwingt. `PASS` = nicht zuständig (dann kommt z. B. das Item dran). |
| `playSound(null, …)` | `null` als Spieler = alle hören es (auch der Klickende). Eigene Sounds: [17](17-Sounds.md) |

### Licht abhängig vom Zustand

```java
public static final RegistryObject<Block> SAPPHIRE_LAMP = BLOCKS.register("sapphire_lamp",
        () -> new SapphireLampBlock(BlockBehaviour.Properties.of()
                .setId(BLOCKS.key("sapphire_lamp"))
                .mapColor(MapColor.COLOR_BLUE)
                .strength(0.3F)
                .sound(SoundType.GLASS)
                .lightLevel(state -> state.getValue(SapphireLampBlock.LIT) ? 15 : 0)));
```

`lightLevel` bekommt den Blockstate und gibt 0–15 zurück.

### Der Blockstate in JSON

`assets/mymod/blockstates/sapphire_lamp.json`:

```json
{
  "variants": {
    "lit=false": { "model": "mymod:block/sapphire_lamp" },
    "lit=true":  { "model": "mymod:block/sapphire_lamp_on" }
  }
}
```

- **Jede Kombination** aller Eigenschaften braucht einen Eintrag (bei zwei Eigenschaften z. B. `"facing=north,lit=true"`).
- Zwei Modelle mit zwei Texturen (`sapphire_lamp.png`, `sapphire_lamp_on.png`).
- Für drehbare Blöcke kann man statt eigener Modelle `"y": 90` im Eintrag angeben (Drehung in 90°-Schritten).
- Vorbilder in der Minecraft-Bibliothek: `assets/minecraft/blockstates/redstone_lamp.json`, `furnace.json`, `wheat.json`.

### Testen

```
/setblock ~ ~ ~ mymod:sapphire_lamp[lit=true]
/execute if block ~ ~ ~ mymod:sapphire_lamp[lit=true] run say an
```

Mit **F3** siehst du rechts die Eigenschaften des Blocks, auf den du schaust.

## 3. Beispiel: Block, der beim Drauftreten wirkt

```java
// Gibt allem, was darüber läuft, Tempo
public class SpeedPadBlock extends Block {
    public SpeedPadBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide() && entity instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.SPEED, 60, 2));   // 3 Sekunden, Tempo III
        }
        super.stepOn(level, pos, state, entity);
    }
}
```

`stepOn` wird jeden Tick aufgerufen, solange etwas darauf steht. Das Prüfen auf `LivingEntity` schließt Items, Pfeile usw. aus.

## 4. Richtung beim Platzieren (Vorlage)

Für Blöcke, die zum Spieler zeigen sollen (Öfen, Truhen):

```java
public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

@Override
public BlockState getStateForPlacement(BlockPlaceContext context) {
    return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
}
// + FACING in createBlockStateDefinition und 4 Varianten im Blockstate-JSON ("facing=north" mit "y": 0, "east" mit "y": 90 …)
```

Vorbild: `FurnaceBlock` / `AbstractFurnaceBlock`.

## Checkliste: Block mit Zuständen

- [ ] Klasse `extends Block`, Konstruktor mit `super(properties)` und `registerDefaultState`
- [ ] alle Eigenschaften in `createBlockStateDefinition`
- [ ] Welt nur auf dem Server ändern (`!level.isClientSide()`)
- [ ] Blockstate-JSON mit **allen** Kombinationen
- [ ] Modelle und Texturen für jede Variante
- [ ] in `ModBlocks` mit `new MeinBlock(...)` registrieren, BlockItem nicht vergessen

## Zum Ausprobieren

- Eine Lampe mit `IntegerProperty.create("level", 0, 3)`, die bei jedem Klick heller wird (Licht 0/5/10/15).
- Ein Trampolin: In 26.x reicht dafür die Eigenschaft `.bounceRestitution(1.0F)` in den Block-Properties (1.0 = springt so hoch zurück, wie man gefallen ist). Gegen Fallschaden `fallOn` überschreiben und `entity.causeFallDamage(fallDistance, 0.0F, level.damageSources().fall())` aufrufen. Vorbild: `Blocks.SLIME_BLOCK` und `SlimeBlock`.
- Ein Block, der sich nach 5 Sekunden selbst zerstört: in `onPlace` mit `level.scheduleTick(pos, this, 100)` planen, in `tick` mit `level.destroyBlock(pos, false)` entfernen.
