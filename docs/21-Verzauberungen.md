# 21 – Eigene Verzauberungen

Seit 1.21 sind Verzauberungen **komplett datengetrieben**: eine JSON-Datei, kein Java-Code. Die Wirkungen setzt man aus fertigen Bausteinen zusammen (Effekt geben, Feuer legen, Schaden erhöhen, Explosion …).

> ✅ Getestet mit Forge 26.3-66.1.0 auf dem Server: Verzauberung lädt; ein Wüstenzombie mit „Frostbiss II“-Schwert hat einen Dorfbewohner getroffen, der danach den Frost-Effekt ([16](16-Effekte-und-Traenke.md)) hatte.

## 1. Die Datei

`data/mymod/enchantment/frostbite.json`:

```json
{
  "description": { "translate": "enchantment.mymod.frostbite" },
  "supported_items": "#minecraft:enchantable/sharp_weapon",
  "primary_items": "#minecraft:enchantable/melee_weapon",
  "weight": 5,
  "max_level": 3,
  "min_cost": { "base": 10, "per_level_above_first": 10 },
  "max_cost": { "base": 40, "per_level_above_first": 10 },
  "anvil_cost": 2,
  "slots": [ "mainhand" ],
  "effects": {
    "minecraft:post_attack": [
      {
        "enchanted": "attacker",
        "affected": "victim",
        "effect": {
          "type": "minecraft:apply_mob_effect",
          "to_apply": "mymod:frost",
          "min_duration": { "type": "minecraft:linear", "base": 2, "per_level_above_first": 1 },
          "max_duration": { "type": "minecraft:linear", "base": 2, "per_level_above_first": 1 },
          "min_amplifier": 0,
          "max_amplifier": 0
        },
        "requirements": {
          "type": "minecraft:damage_source_properties",
          "predicate": { "is_direct": true }
        }
      }
    ]
  }
}
```

### Die Felder

| Feld | Bedeutung |
|---|---|
| `description` | Name, übersetzt über `lang`: `"enchantment.mymod.frostbite": "Frostbiss"` |
| `supported_items` | Items, auf die man sie **am Amboss** bringen kann (Tag) |
| `primary_items` | Items, für die sie **am Zaubertisch** erscheint (optional, sonst = `supported_items`) |
| `weight` | Häufigkeit am Zaubertisch: 10 häufig (Schärfe), 5 mittel, 2 selten, 1 sehr selten |
| `max_level` | höchste Stufe |
| `min_cost` / `max_cost` | welche Zaubertisch-Stufen (Level) welche Verzauberungsstufe ergeben |
| `anvil_cost` | Kosten-Faktor am Amboss |
| `slots` | wo das Item stecken muss, damit es wirkt: `mainhand`, `offhand`, `head`, `chest`, `legs`, `feet`, `armor`, `any` |
| `effects` | die Wirkungen (siehe unten) |
| `exclusive_set` (optional) | Tag mit Verzauberungen, die sich ausschließen (wie Schärfe/Bann) |

### Wirkung: `post_attack` + `apply_mob_effect`

- `minecraft:post_attack` = **nach einem Treffer**.
- `enchanted: attacker` = die Verzauberung sitzt auf der Waffe des Angreifers. `affected: victim` = die Wirkung trifft das Opfer.
- `apply_mob_effect` gibt einen Effekt – hier den eigenen `mymod:frost`. Dauer in **Sekunden**.
- `minecraft:linear` = Wert steigt pro Stufe: Stufe I 2 s, II 3 s, III 4 s.
- `requirements` = Bedingung. `is_direct: true` = nur direkte Treffer (nicht Dornen, nicht Pfeile).

Weitere Wirkungs-Arten (Komponenten) aus Vanilla zum Abschauen:

| Komponente | Vorbild in `data/minecraft/enchantment/` |
|---|---|
| `minecraft:damage` (mehr Schaden) | `sharpness.json`, `smite.json` |
| `minecraft:post_attack` mit `ignite` | `fire_aspect.json` |
| `minecraft:knockback` | `knockback.json` |
| `minecraft:attributes` (Attribut solange getragen) | `swift_sneak.json` |
| `minecraft:damage_protection` | `protection.json` |
| `minecraft:tick` (jeden Tick, z. B. mit `run_function`) | – |
| `minecraft:post_attack` mit `explode` (Explosion nach dem Treffer) | `wind_burst.json` |

## 2. Wo die Verzauberung auftaucht – Tags

Ohne Tag-Eintrag existiert die Verzauberung nur über `/enchant` oder den Amboss mit einem Buch.

```json
// data/minecraft/tags/enchantment/in_enchanting_table.json  → am Zaubertisch
// data/minecraft/tags/enchantment/non_treasure.json         → gilt nicht als "Schatz"
{ "replace": false, "values": [ "mymod:frostbite" ] }
```

| Tag | Wirkung |
|---|---|
| `in_enchanting_table` | erscheint am Zaubertisch |
| `non_treasure` / `treasure` | normal bzw. Schatz-Verzauberung (wie Reparatur) |
| `on_random_loot` | kann in Truhen-Beute erscheinen |
| `on_traded_equipment` / `tradeable` | Dorfbewohner-Handel |
| `curse` | ist ein Fluch |

## 3. Die Item-Tags müssen stimmen

> ⚠️ **Hier ist ein Fehler besonders teuer:** Zeigt `supported_items` auf einen Tag, den es nicht gibt (z. B. `#minecraft:enchantable/sword`), lädt der **ganze Datensatz** nicht – die Welt startet nicht: *„Failed to load registries due to errors … Missing tag: 'minecraft:enchantable/sword'“*.

Die Vanilla-Tags für Verzauberungen in 26.3 (Ordner `data/minecraft/tags/item/enchantable/`):

`armor`, `head_armor`, `chest_armor`, `leg_armor`, `foot_armor`, `equippable`, `durability`, `vanishing`, `melee_weapon` (Schwerter + Speere), `sharp_weapon` (Nahkampf + Äxte), `weapon` (+ Streitkolben), `sweeping`, `fire_aspect`, `mining`, `mining_loot`, `bow`, `crossbow`, `trident`, `mace`, `lunge`, `fishing`

Eigene Waffen kommen dort automatisch hinein, wenn sie in `#minecraft:swords`, `#minecraft:axes` usw. stehen ([02](02-Bloecke-Items-Werkzeuge.md#in-vanilla-tags-eintragen)).

## 4. Testen

```
/enchant @s mymod:frostbite 3
/give @s minecraft:diamond_sword[enchantments={"mymod:frostbite":2}]
```

Daten-Änderungen mit `/reload` neu laden – **aber:** Verzauberungen sind Teil der Welt-Registries. Bei Fehlern lässt sich die Welt nicht mehr öffnen, bis die Datei korrigiert ist. Deshalb in einer **Testwelt** ausprobieren.

## Checkliste

- [ ] `data/<modid>/enchantment/<name>.json`
- [ ] `supported_items` zeigt auf einen **existierenden** Tag
- [ ] `lang`: `enchantment.<modid>.<name>`
- [ ] Tags `in_enchanting_table` / `non_treasure` (oder `treasure`)
- [ ] in einer Testwelt mit `/enchant` geprüft

## Zum Ausprobieren

- „Lebensraub“: `post_attack` mit `affected: attacker` und einem Heil-Effekt (`minecraft:instant_health`).
- Eine Stiefel-Verzauberung, die mit `minecraft:attributes` die Sprunghöhe (`minecraft:jump_strength`) erhöht.
- Lies `wind_burst.json` – wie Mojang beim Streitkolben nach einem Treffer eine Wind-Explosion auslöst.
