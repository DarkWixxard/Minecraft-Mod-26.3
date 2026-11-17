# 15 – Auf Spiel-Ereignisse reagieren (Events)

Mit Events kannst du das Verhalten von **Vanilla** ändern, ohne Vanilla-Klassen zu ersetzen: Zombies droppen etwas Neues, Spieler bekommen eine Begrüßung, bestimmte Blöcke sind geschützt … Wie Events grundsätzlich angemeldet werden, steht in [01](01-Einrichtung-und-Grundlagen.md#7-events).

> ✅ Getestet mit Forge 26.3-66.1.0: kompiliert, Server startet und registriert alle Listener. Die Events selbst brauchen einen Spieler und wurden nicht im Spiel ausgelöst.

## 1. Das Grundgerüst

```java
// Reaktionen auf Dinge, die im Spiel passieren
@Mod.EventBusSubscriber(modid = MyMod.MODID)
public final class ModGameEvents {
    private ModGameEvents() {}

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) { ... }
}
```

- **Ohne** `value = Dist.CLIENT` – diese Events laufen auf dem Server (und teils auch auf dem Client, siehe unten).
- Jede Methode: `public static`, genau **ein** Parameter (das Event), `@SubscribeEvent`.
- Bei **abbrechbaren** Events gibt die Methode `boolean` zurück: `true` = abbrechen.

## 2. Beispiele

### Spieler betritt den Server

```java
@SubscribeEvent
public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
    if (MyModConfig.WELCOME_MESSAGE.get()) {   // Wert aus der Konfiguration (Kapitel 19)
        event.getEntity().sendSystemMessage(Component.literal("Welcome, " + event.getEntity().getName().getString() + "!"));
    }
}
```

### Block abbauen verhindern (abbrechbar)

```java
@SubscribeEvent
public static boolean onBreak(BlockEvent.BreakEvent event) {
    Player player = event.getPlayer();
    if (event.getState().is(Blocks.SPAWNER) && !player.isCreative()) {
        player.sendSystemMessage(Component.literal("Spawners are protected!"));
        return true;   // true = abbrechen: der Block bleibt
    }
    return false;
}
```

`event.getState().is(...)` funktioniert auch mit Tags: `is(BlockTags.LOGS)`.

### Zusätzliche Drops

```java
@SubscribeEvent
public static void onDrops(LivingDropsEvent event) {
    var entity = event.getEntity();
    if (entity.getType() == EntityTypes.ZOMBIE && event.getSource().getEntity() instanceof Player
            && entity.getRandom().nextInt(100) < MyModConfig.EXTRA_DROP_CHANCE.get()) {
        event.getDrops().add(new ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(),
                new ItemStack(Items.EMERALD)));
    }
}
```

- `event.getSource().getEntity()` = wer **verursacht** hat (bei Pfeilen der Schütze). `getDirectEntity()` wäre der Pfeil selbst.
- `getDrops()` ist die Liste der Item-Entities, die gleich fallen. Hinzufügen oder entfernen erlaubt.
- Für einfache Zusatz-Drops geht es auch **ohne Java**: mit einer *Global Loot Modifier*-JSON oder durch Ersetzen der Loot-Table. Events sind gut, wenn Bedingungen kompliziert sind.

### Jeden Tick für jeden Spieler

```java
@SubscribeEvent
public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
    Player player = event.player();
    if (event.side() != LogicalSide.SERVER || player.tickCount % 20 != 0) {
        return;   // nur auf dem Server und nur einmal pro Sekunde
    }
    if (player.getY() > 200) {
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 0));   // dünne Luft in großer Höhe
    }
}
```

- Tick-Events laufen **20× pro Sekunde** – teure Dinge (Suchen, Schleifen über viele Blöcke) nicht jeden Tick machen. `tickCount % 20 == 0` = einmal pro Sekunde.
- `PlayerTickEvent` feuert auf **Server und Client**. `event.side()` sagt, wo. Ohne die Prüfung passiert alles doppelt (und auf dem Client ohne Wirkung).
- `.Pre` = vor dem Vanilla-Tick, `.Post` = danach.
- In 26.x sind viele Events **records**: Werte holt man mit `event.player()`, `event.side()` – ohne `get`. Ältere Events haben `getPlayer()`. Im Zweifel: Strg + Klick aufs Event.

## 3. Nützliche Events

| Event | Wann | abbrechbar |
|---|---|---|
| `PlayerEvent.PlayerLoggedInEvent` / `PlayerLoggedOutEvent` | Spieler kommt / geht | – |
| `PlayerEvent.PlayerRespawnEvent` | nach dem Tod wiederbelebt | – |
| `PlayerEvent.Clone` | Spieler-Objekt wird nach Tod/Dimensionswechsel neu erzeugt – eigene Daten hier kopieren | – |
| `PlayerEvent.ItemCraftedEvent` / `ItemSmeltedEvent` | Item hergestellt / geschmolzen | – |
| `PlayerInteractEvent.RightClickBlock` / `RightClickItem` / `LeftClickBlock` | Klicks | ja |
| `AttackEntityEvent` | Spieler greift an | ja |
| `BlockEvent.BreakEvent` | Spieler baut Block ab | ja |
| `BlockEvent.EntityPlaceEvent` | Block wird gesetzt | ja |
| `LivingDeathEvent` | etwas stirbt | ja |
| `LivingDropsEvent` | Drops nach dem Tod | ja |
| `LivingHurtEvent` / `LivingDamageEvent` | Schaden (vorher / endgültig) | ja |
| `LivingFallEvent` | Fallschaden | ja |
| `EntityJoinLevelEvent` | Entity erscheint in der Welt | ja |
| `TickEvent.PlayerTickEvent` / `LevelTickEvent` / `ServerTickEvent` | jeden Tick | – |
| `ServerChatEvent` | Chat-Nachricht | ja |
| `ItemTooltipEvent` (Client) | Tooltip eines Items wird gebaut | – |

Alle Events: Paket `net.minecraftforge.event` (Unterordner `entity`, `level`, `player` …) in IntelliJ durchstöbern. Die Kommentare dort erklären, wann ein Event feuert.

## 4. Typische Fehler

| Problem | Ursache |
|---|---|
| Methode wird nie aufgerufen | Klasse ohne `@Mod.EventBusSubscriber`, Methode nicht `static`, falscher `@SubscribeEvent`-Import, oder `modid` falsch |
| Passiert doppelt | Tick-/Interaktions-Event läuft auf Server **und** Client – Seite prüfen |
| Absturz auf dem Server | Client-Klassen (`Minecraft`, Renderer) in einer Klasse ohne `Dist.CLIENT` benutzt |
| Abbrechen wirkt nicht | Methode gibt `void` statt `boolean` zurück |

## Checkliste

- [ ] passendes Event gefunden (Kommentar im Event gelesen)
- [ ] Klasse mit `@Mod.EventBusSubscriber`, Methode `public static` mit `@SubscribeEvent`
- [ ] Seite geprüft (Server/Client), wo nötig
- [ ] bei Tick-Events: nicht jeden Tick teure Arbeit
- [ ] bei abbrechbaren Events: `boolean` zurückgeben

## Zum Ausprobieren

- Spieler, die in Lava fallen, bekommen beim ersten Mal 10 Sekunden Feuerresistenz (`LivingHurtEvent`, Schadensart prüfen mit `event.getSource().is(DamageTypes.LAVA)`).
- Creeper droppen manchmal Feuerwerksraketen.
- Zeige im Tooltip deiner Items einen Hinweistext (`ItemTooltipEvent`, nur Client).
