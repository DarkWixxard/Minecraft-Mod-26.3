# 17 – Eigene Sounds

> ✅ Getestet mit Forge 26.3-66.1.0: Registrierung kompiliert, Server startet. Abspielen und `sounds.json` sind Client-Sache und wurden nicht im Spiel angehört – das Format entspricht Vanillas `sounds.json`.

## 1. Die drei Teile

| Teil | Datei |
|---|---|
| **SoundEvent** registrieren | `sound/ModSounds.java` |
| **`sounds.json`**: welches SoundEvent welche Datei(en) abspielt | `assets/mymod/sounds.json` |
| **Audiodatei(en)** im Format `.ogg` | `assets/mymod/sounds/...` |

## 2. Registrieren

```java
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MyMod.MODID);

    // Der Name muss zum Eintrag in assets/mymod/sounds.json passen
    public static final RegistryObject<SoundEvent> LAMP_CLICK = register("block.sapphire_lamp.click");
    public static final RegistryObject<SoundEvent> WYVERN_ROAR = register("entity.wyvern.roar");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name,
                () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(MyMod.MODID, name)));
    }

    private ModSounds() {}
}
// in MyMod: ModSounds.SOUND_EVENTS.register(modBusGroup);
```

- Namenskonvention wie Vanilla: `block.<block>.<aktion>`, `entity.<mob>.<aktion>`, `item.<item>.<aktion>`.
- `createVariableRangeEvent` = Hörweite hängt von der Lautstärke ab (Standard, 16 Blöcke bei Lautstärke 1). `createFixedRangeEvent(id, 64.0F)` = feste Reichweite (z. B. für ein lautes Boss-Brüllen).

## 3. `assets/mymod/sounds.json`

```json
{
  "block.sapphire_lamp.click": {
    "subtitle": "subtitles.mymod.block.sapphire_lamp.click",
    "sounds": [ "mymod:block/sapphire_lamp_click" ]
  },
  "entity.wyvern.roar": {
    "subtitle": "subtitles.mymod.entity.wyvern.roar",
    "sounds": [
      { "name": "mymod:entity/wyvern/roar1", "volume": 1.0, "pitch": 1.0 },
      { "name": "mymod:entity/wyvern/roar2", "weight": 2 }
    ]
  }
}
```

- Schlüssel = Name aus `register(...)` (ohne Mod-ID).
- `sounds`: eine oder mehrere Dateien. Bei mehreren wählt Minecraft zufällig (`weight` = Häufigkeit).
- `"mymod:block/sapphire_lamp_click"` → Datei `assets/mymod/sounds/block/sapphire_lamp_click.ogg` (**ohne** `.ogg` im JSON).
- `subtitle` = Text für Untertitel (Barrierefreiheit). In `lang` eintragen: `"subtitles.mymod.block.sapphire_lamp.click": "Lampe klickt"`.

### Die Audiodatei

- Format **Ogg Vorbis** (`.ogg`). MP3/WAV gehen nicht.
- Umwandeln z. B. mit [Audacity](https://www.audacityteam.org/) (kostenlos): *Datei → Exportieren → OGG*.
- Für Sounds, die aus einer **Richtung** kommen sollen (Mob, Block), muss die Datei **Mono** sein. Stereo-Dateien klingen überall gleich laut.
- Kurze Sounds (unter ein paar Sekunden) einfach so; lange Musik mit `"stream": true` im Eintrag.

## 4. Sounds abspielen

```java
// Block/Welt: alle in der Nähe hören es
level.playSound(null, pos, ModSounds.LAMP_CLICK.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
//              ↑ null = auch der auslösende Spieler hört es      Lautstärke ↑  ↑ Tonhöhe (0.5–2.0)

// Von einer Entity aus
entity.playSound(ModSounds.WYVERN_ROAR.get(), 2.0F, 1.0F);
```

- Auf dem **Server** aufrufen → alle Spieler in der Nähe hören es.
- Gibt man als ersten Parameter einen Spieler an, hört **dieser** Spieler es nicht (gedacht für Fälle, in denen sein Client den Sound schon selbst abgespielt hat).
- `SoundSource` bestimmt, welcher Lautstärkeregler gilt: `BLOCKS`, `HOSTILE`, `NEUTRAL`, `PLAYERS`, `AMBIENT`, `MUSIC`, `RECORDS` …

### Mob-Sounds

```java
@Override
protected SoundEvent getAmbientSound() { return ModSounds.WYVERN_ROAR.get(); }   // ab und zu

@Override
protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.ENDER_DRAGON_HURT; }

@Override
protected SoundEvent getDeathSound() { return SoundEvents.ENDER_DRAGON_DEATH; }
```

Diese Methoden in der Mob-Klasse überschreiben – Minecraft spielt die Sounds dann automatisch.

### Block-Sounds (Laufen, Abbauen)

Über `.sound(SoundType.…)` in den Block-Properties ([02](02-Bloecke-Items-Werkzeuge.md#2-blöcke)). Für ganz eigene: `new SoundType(volume, pitch, breakSound, stepSound, placeSound, hitSound, fallSound)` mit eigenen SoundEvents.

## 5. Testen

```
/playsound mymod:block.sapphire_lamp.click block @s
```

Kein Ton? Im Log nach `Unable to play unknown soundEvent` oder `Missing sound` suchen → Namen in `sounds.json` und Dateipfad prüfen. **F3 + T** lädt Sounds neu.

## Checkliste

- [ ] SoundEvent registriert (`SOUND_EVENTS.register(modBusGroup)`)
- [ ] Eintrag in `assets/<modid>/sounds.json` mit gleichem Namen
- [ ] `.ogg`-Datei (Mono für Positionssounds) im passenden Unterordner von `sounds/`
- [ ] Untertitel in `lang`
- [ ] auf dem Server abspielen

## Zum Ausprobieren

- Gib deinem Boss ein eigenes Brüllen, das mit der Brüll-Animation zusammen ausgelöst wird ([08](08-Mob-Animationen.md)).
- Eine Musik-Disc: Vorbild in der Minecraft-Bibliothek `data/minecraft/jukebox_song/` (datengetrieben) + ein Item mit der Komponente `jukebox_playable`.
