# 13 – Eigene Befehle (Commands)

Eigene Befehle wie `/mymod heal` sind ideal zum Testen und für Server-Admins. Minecraft benutzt dafür die Bibliothek **Brigadier** (von Mojang). Ein Befehl ist ein **Baum**: Jedes Wort und jedes Argument ist ein Ast.

> ✅ Getestet mit Forge 26.3-66.1.0: Befehle von der Server-Konsole ausgeführt (`config`, `heal` ohne Spieler, `spawns`, `zombies 5` → 5 Zombies).

## 1. Der Aufbau eines Befehls

```
/mymod heal                 → literal("mymod") → literal("heal") → executes(...)
/mymod heal Steve Alex      → literal("mymod") → literal("heal") → argument("targets", players) → executes(...)
/mymod zombies 5            → literal("mymod") → literal("zombies") → argument("count", integer) → executes(...)
```

| Baustein | Bedeutung |
|---|---|
| `Commands.literal("heal")` | festes Wort |
| `Commands.argument("count", IntegerArgumentType.integer(1, 20))` | Platzhalter mit Typ (hier: Zahl 1–20) |
| `.then(...)` | hängt einen Ast an |
| `.executes(ctx -> ...)` | was passiert, wenn der Befehl **hier endet** |
| `.requires(...)` | wer den Ast benutzen darf |

Ein Ast ohne `executes` ist kein vollständiger Befehl: `/mymod` allein gibt dann *„Unknown or incomplete command“*.

## 2. Befehle registrieren

```java
// Alle Befehle der Mod: /mymod heal, /mymod zombies, /mymod config, /mymod spawns
@Mod.EventBusSubscriber(modid = MyMod.MODID)
public final class ModCommands {
    private ModCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal(MyMod.MODID)
                // /mymod heal                -> heilt dich selbst
                // /mymod heal <targets>      -> heilt andere Spieler (braucht OP-Rechte)
                .then(Commands.literal("heal")
                        .executes(ctx -> heal(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException())))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .executes(ctx -> heal(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets")))))
                // /mymod zombies <count>     -> 1–20 Zombies um dich herum
                .then(Commands.literal("zombies")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 20))
                                .executes(ctx -> spawnZombies(ctx, IntegerArgumentType.getInteger(ctx, "count")))))
                // /mymod config              -> zeigt die Konfiguration (Kapitel 19)
                .then(Commands.literal("config")
                        .executes(ctx -> showConfig(ctx.getSource())))
                // /mymod spawns              -> welche Monster spawnen in diesem Biom (Kapitel 20)
                .then(Commands.literal("spawns")
                        .executes(ctx -> showSpawns(ctx.getSource()))));
    }
}
```

- `RegisterCommandsEvent` feuert beim Start der Welt bzw. bei `/reload`. Die Klasse braucht **kein** `Dist.CLIENT` – Befehle laufen auf dem Server.
- Alle Befehle unter **einem** Wort mit der Mod-ID zu sammeln (`/mymod …`) verhindert Konflikte mit anderen Mods.
- `getPlayerOrException()` wirft einen Fehler, wenn der Befehl nicht von einem Spieler kommt (z. B. Konsole, Befehlsblock). Minecraft zeigt dann automatisch *„A player is required to run this command here“*.

### Berechtigungen

| Konstante | Wer darf? |
|---|---|
| `Commands.LEVEL_ALL` | alle |
| `Commands.LEVEL_MODERATORS` | OP-Stufe 1 |
| `Commands.LEVEL_GAMEMASTERS` | OP-Stufe 2 (Befehlsblöcke; Standard für `/give`, `/kill`) |
| `Commands.LEVEL_ADMINS` | OP-Stufe 3 |
| `Commands.LEVEL_OWNERS` | OP-Stufe 4 |

```java
.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
```

Im Einzelspieler mit erlaubten Cheats hat man alle Stufen. Wer keine Berechtigung hat, sieht den Ast nicht einmal in der Tab-Vervollständigung.

> ⚠️ In älteren Versionen hieß es `.requires(source -> source.hasPermission(2))`. Das gibt es in 26.x nicht mehr.

## 3. Die Befehls-Methoden

```java
private static int heal(CommandSourceStack source, Collection<ServerPlayer> players) {
    for (ServerPlayer player : players) {
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
    }
    source.sendSuccess(() -> Component.literal("Healed " + players.size() + " player(s)"), true);
    return players.size();   // Rückgabe = "Ergebnis" des Befehls (z. B. für Befehlsblöcke / execute store)
}

private static int spawnZombies(CommandContext<CommandSourceStack> ctx, int count) throws CommandSyntaxException {
    CommandSourceStack source = ctx.getSource();
    ServerLevel level = source.getLevel();
    BlockPos center = BlockPos.containing(source.getPosition());
    int spawned = 0;
    for (int i = 0; i < count; i++) {
        BlockPos pos = center.offset(level.getRandom().nextInt(9) - 4, 0, level.getRandom().nextInt(9) - 4);
        if (EntityTypes.ZOMBIE.spawn(level, pos, EntitySpawnReason.COMMAND) != null) {
            spawned++;
        }
    }
    if (spawned == 0) {
        source.sendFailure(Component.literal("No zombie could be spawned here"));
        return 0;
    }
    int result = spawned;
    source.sendSuccess(() -> Component.literal("Spawned " + result + " zombies"), true);
    return spawned;
}
```

| Was | Erklärung |
|---|---|
| `CommandSourceStack` | **wer** den Befehl ausführt und **wo**: `getLevel()`, `getPosition()`, `getEntity()`, `getPlayer()` (kann `null` sein) |
| `sendSuccess(() -> text, true)` | Erfolgsmeldung. `true` = auch anderen OPs anzeigen (wie bei `/give`). Der Text ist ein **Supplier** (`() -> …`), damit er nur erzeugt wird, wenn ihn jemand sieht. |
| `sendFailure(text)` | rote Fehlermeldung |
| Rückgabewert `int` | 0 = „nichts passiert“, > 0 = Erfolg. Befehlsblöcke und `/execute store result` benutzen diese Zahl. |
| `int result = spawned;` | Lambdas dürfen nur Variablen benutzen, die sich nicht mehr ändern („effectively final“). `spawned` ändert sich in der Schleife, deshalb die Kopie. |

> In 26.x stehen die Vanilla-EntityTypes in der Klasse **`EntityTypes`** (`EntityTypes.ZOMBIE`), nicht mehr in `EntityType`. `level.random` ist nicht mehr öffentlich – `level.getRandom()` benutzen.

## 4. Argument-Typen

| Typ | Erzeugen | Auslesen |
|---|---|---|
| ganze Zahl | `IntegerArgumentType.integer(min, max)` | `IntegerArgumentType.getInteger(ctx, "name")` |
| Kommazahl | `DoubleArgumentType.doubleArg(min, max)` | `DoubleArgumentType.getDouble(ctx, "name")` |
| ja/nein | `BoolArgumentType.bool()` | `BoolArgumentType.getBool(ctx, "name")` |
| ein Wort | `StringArgumentType.word()` | `StringArgumentType.getString(ctx, "name")` |
| Text bis zum Ende | `StringArgumentType.greedyString()` | `StringArgumentType.getString(ctx, "name")` |
| ein Spieler | `EntityArgument.player()` | `EntityArgument.getPlayer(ctx, "name")` |
| mehrere Spieler (`@a`) | `EntityArgument.players()` | `EntityArgument.getPlayers(ctx, "name")` |
| Entities (`@e[...]`) | `EntityArgument.entities()` | `EntityArgument.getEntities(ctx, "name")` |
| Block-Position | `BlockPosArgument.blockPos()` | `BlockPosArgument.getLoadedBlockPos(ctx, "name")` |

`IntegerArgumentType` & Co. kommen aus `com.mojang.brigadier.arguments`, `EntityArgument` & Co. aus `net.minecraft.commands.arguments`. Für Vorbilder: die Vanilla-Befehle im Paket `net.minecraft.server.commands` (z. B. `GiveCommand`, `EffectCommands`).

## 5. Beispiel: Spieldaten auslesen

```java
private static int showSpawns(CommandSourceStack source) {
    BlockPos pos = BlockPos.containing(source.getPosition());
    Holder<Biome> biome = source.getLevel().getBiome(pos);
    MobSpawnSettings spawns = biome.value().getAttributes()
            .applyModifier(EnvironmentAttributes.NATURAL_MOB_SPAWNS, MobSpawnSettings.EMPTY);
    StringBuilder text = new StringBuilder(biome.getRegisteredName() + ":");
    spawns.getMobsToSpawn(MobCategory.MONSTER).unwrap().forEach(entry ->
            text.append(' ').append(entry.value().type().getDescriptionId()).append('(').append(entry.weight()).append(')'));
    source.sendSuccess(() -> Component.literal(text.toString()), false);
    return 1;
}
```

Solche **Debug-Befehle** sind beim Entwickeln Gold wert: Du siehst sofort, ob z. B. dein Mob wirklich in einem Biom spawnen darf ([20](20-Spawn-Eier-und-Spawnen.md)). In 26.x stehen die Spawn-Listen eines Bioms in seinen „Environment Attributes“.

## 6. Testen

- Im Spiel: `/mymod ` tippen und **Tab** drücken – die Vervollständigung zeigt alle Äste.
- In der Server-Konsole ohne `/`: `mymod zombies 5`.
- `execute positioned 0 64 0 run mymod spawns` – Befehl an einer bestimmten Stelle ausführen.
- `execute store result score #z test run mymod zombies 5` – Rückgabewert speichern.

## Checkliste

- [ ] Klasse mit `@Mod.EventBusSubscriber(modid = …)` und `@SubscribeEvent` für `RegisterCommandsEvent`
- [ ] alles unter `Commands.literal(MODID)`
- [ ] jede Stelle, an der der Befehl enden darf, hat `executes`
- [ ] `requires(...)` für alles, was die Welt verändert
- [ ] Rückgabewert sinnvoll (0 bei Fehlschlag)
- [ ] Meldungen mit `sendSuccess` / `sendFailure`

## Zum Ausprobieren

- `/mymod fly <player> <true|false>` – schaltet `player.getAbilities().mayfly` um (danach `player.onUpdateAbilities()`).
- `/mymod home set` und `/mymod home` – Position speichern und zurückteleportieren (Speicherort: zunächst einfach eine `Map<UUID, BlockPos>`; später mit Spielerdaten).
- Lies `net.minecraft.server.commands.EffectCommands` und schau, wie Mojang Unterbefehle aufbaut.
