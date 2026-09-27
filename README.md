# HexVG-DataComponents

[Polski](README-PL.md) | **English**

> A Skript addon for working with item and entity Data Components on the **VenomGrave** server

![version](https://img.shields.io/badge/version-1.2.0-blue)
![paper](https://img.shields.io/badge/Paper-1.21.4%2B%20%7C%2026.x-green)
![java](https://img.shields.io/badge/Java-21%2B-orange)
![skript](https://img.shields.io/badge/Skript-2.10%2B-purple)
![license](https://img.shields.io/badge/license-MIT-gray)

---

## About

HexVG-DataComponents is a Skript addon built for the VenomGrave server. It gives your scripts full access to Minecraft's **Data Components API**, the system that replaced NBT in 1.20.5. An item's name, lore, enchantments, tool behavior, food, equipping, cooldown and dozens of other properties are all separate components now.

The plugin handles all of the Paper API complexity. In Skript you just write what you want to do with an item or entity: no NBT, no Java.

A lot of work went into making sure no script can break an item. Minecraft refuses to save items whose values are outside its limits (for example an item that is both damageable and stackable), and such an item can vanish from an inventory or block a chunk from saving. The plugin enforces those limits, rejects `NaN`/`Infinity` and caps data size, so it is safe to pass even player-typed text into a component. Reading uses the same format as writing: a value read from one item can be set on another right away.

---

## Features

- **Set, read and remove** item components, including default ones (e.g. food from an apple)
- **30+ components** with full support: enchantments, attributes, food, potions, tools, equipping, cooldowns, banners, books and more
- **Every marker component** (e.g. `glider`) is handled automatically with `true` / `false`
- **Read format matches write format**: copy the mining rules of a diamond pickaxe onto a stick in one line
- **Over-limit enchantment levels** (e.g. Sharpness 255) and **custom datapack enchantments**
- **Tool rules with block tags** (`#minecraft:mineable/pickaxe`)
- **Entity components**: health, max health, speed, damage, armor, glowing, gravity, name and more
- `data component change` and `data component remove` **events** with cancelling and replacing the value being written
- **Colors**: `&a` codes and hex `&#RRGGBB` in names, lore and books
- **Type safety**: every value goes through Paper's typed API, not raw NBT strings
- **Protection against broken items** and **rejection of `NaN`/`Infinity`**
- **Size limits** for text and lists that protect against "chunk bans" built from player input
- **A typo never wrecks an item**: an invalid entry will not overwrite a component with an empty or default value
- **No console spam**: the same warning is shown at most once every 10 seconds
- No extra dependencies besides Skript

---

## Requirements

| Requirement | Version |
|-------------|---------|
| Paper | 1.21.4+ / 26.x (forks such as Purpur too) |
| Skript | 2.10+ (tested on 2.14.3 and 2.16.2) |
| Java | 21+ |

> Spigot and Bukkit are not supported, because they don't have the Data Components API.

---

## Configuration

```yaml
debug: false   # extra logging, disables suppression of repeated warnings

limits:
  max-string-length: 8192   # max length of a single text (name, lore line, book page)
  max-list-size: 256        # max number of list entries (lore, enchantments, attributes...)
```

---

## Skript syntax

### Effects

```skript
# set an item component
set data component "minecraft:custom_name" of {_item} to "&cName"
set data component "minecraft:damage" of {_item} to 100
set data component "minecraft:unbreakable" of {_item} to true
set data component "minecraft:lore" of {_item} to {_lore::*}

# remove an item component
remove data component "minecraft:lore" from {_item}
delete data component "minecraft:food" of {_item}

# entity components
set entity component "minecraft:custom_name" of {_entity} to "&cBoss"
set entity component "minecraft:max_health" of {_entity} to 200
remove entity component "minecraft:custom_name" from {_entity}
reset entity component "minecraft:max_health" of {_entity}

# cancelling inside events
cancel data component change
cancel data component removal
```

> The `set` and `remove` effects need an item that can be overwritten (a variable, `player's tool`, `event-item`). Using a constant such as `diamond sword` reports an error when the script is loaded.

### Expressions

```skript
# read a single value (lists come back as the text "[a, b]")
set {_dmg} to data component "minecraft:damage" of {_item}
set {_name} to data component "minecraft:custom_name" of {_item}

# read a list element by element (for loop / contains / amount of)
set {_lore::*} to data component values "minecraft:lore" of {_item}

# entity component
set {_hp} to entity component "minecraft:health" of {_entity}

# all components of an item and their count
set {_all::*} to all data components of {_item}
set {_n} to data component count of {_item}

# every component known to the server
set {_known::*} to all known data components

# copy of an item with a component set (the original stays unchanged)
set {_copy} to {_base} with data component "minecraft:custom_name" set to "&aNew item"

# inside events
set {_name} to component name
set {_old} to old component value
set {_new} to new component value
```

### Conditions

```skript
# component presence
if {_item} has data component "minecraft:custom_name":
if {_item} doesn't have data component "minecraft:unbreakable":

# value comparison (numbers are compared numerically: 5 = 5.0)
if data component "minecraft:damage" of {_item} is 100:
if data component "minecraft:custom_name" of {_item} is not "&cBoss":

# component name validity
if "minecraft:custom_name" is a valid data component:    # namespace:path format
if "minecraft:custom_name" is a known data component:    # exists on this server

# entity components
if {_entity} has entity component "minecraft:custom_name":
```

### Events

```skript
on data component change:
    event-player is set
    event-player doesn't have permission "venomgrave.bypass"
    component name is "minecraft:enchantments"
    cancel data component change

on data component change:
    component name is "minecraft:rarity"
    new component value is "epic"
    set new component value to "rare"      # replace the value being written

on data component remove:
    component name is "minecraft:unbreakable"
    cancel data component removal
```

Available in events: `event-player` (if a player caused the change), `event-item`, `component name`, `old component value`, `new component value`.

> Events cover changes made by this addon, not e.g. anvils or enchanting tables.

---

## Item components

Every value read by the plugin uses the same format as when writing, so it can be moved to another item right away.

### Text and appearance

| Component | Format | Example |
|---|---|---|
| `minecraft:custom_name` | text, `&` codes and `&#RRGGBB` | `"&4&lDark Sword"` |
| `minecraft:item_name` | text, `&` codes and `&#RRGGBB` | `"&7Iron Blade"` |
| `minecraft:lore` | list of texts (max 256 lines) | `"&7Line 1" and "&8Line 2"` |
| `minecraft:rarity` | `common` / `uncommon` / `rare` / `epic` | `"epic"` |
| `minecraft:item_model` | key | `"mypack:weapons/sword"` |
| `minecraft:custom_model_data` | number | `1001` |
| `minecraft:dyed_color` | `#RRGGBB`, `R,G,B` or a number | `"#FF4400"` |
| `minecraft:enchantment_glint_override` | `true` / `false` | forces or hides the glint |

### Durability and stacking

| Component | Format | Notes |
|---|---|---|
| `minecraft:damage` | number ≥ 0 | current damage |
| `minecraft:max_damage` | number ≥ 1 | max durability, only for items with `max_stack_size` = 1 |
| `minecraft:max_stack_size` | number 1–99 | only for items without `max_damage` |
| `minecraft:repair_cost` | number ≥ 0 | anvil repair cost |
| `minecraft:unbreakable` | `true` / `false` | unbreakable |

> An item cannot be both damageable and stackable. To give `max_damage` to a stackable item, set `max_stack_size` to 1 first.

### Enchantments

| Component | Format | Notes |
|---|---|---|
| `minecraft:enchantments` | list of `"name:level"` | level 1–255 |
| `minecraft:stored_enchantments` | list of `"name:level"` | for enchanted books |

Over-limit levels and custom datapack enchantments are supported:

```skript
# levels above vanilla
set {_e::1} to "sharpness:255"
set {_e::2} to "unbreaking:100"
set data component "minecraft:enchantments" of {_item} to {_e::*}

# datapack enchantments (together with vanilla)
set {_e::1} to "myserver:darkness_touch:1"
set {_e::2} to "sharpness:5"
set data component "minecraft:enchantments" of {_item} to {_e::*}
```

### Food and consuming

```skript
# food: "nutrition:saturation[:canAlwaysEat]"
set data component "minecraft:food" of {_item} to "4:1.2:true"

# consumable: how long and with which animation the item is eaten/drunk
set {_c::1} to "time:0.5"         # seconds (default 1.6)
set {_c::2} to "animation:drink"  # none | eat | drink | block | bow | spear | crossbow | spyglass | toot_horn | brush | bundle
set {_c::3} to "sound:minecraft:entity.generic.drink"
set {_c::4} to "particles:false"
set data component "minecraft:consumable" of {_item} to {_c::*}
```

> For consuming to restore hunger, set `minecraft:food` as well.

### Equipment and mechanics

```skript
# equippable: any item can be worn in any slot
set {_e::1} to "slot:head"        # head | chest | legs | feet | mainhand | offhand | body
set {_e::2} to "sound:minecraft:item.armor.equip_diamond"
set {_e::3} to "camera_overlay:minecraft:misc/pumpkinblur"   # optional: screen overlay
set {_e::4} to "asset_id:mypack:crown"                        # optional: armor model from a resource pack
set {_e::5} to "swappable:true"
set {_e::6} to "dispensable:true"
set data component "minecraft:equippable" of {_item} to {_e::*}

# use_cooldown
set {_cd::1} to "time:2.0"                     # seconds, must be > 0
set {_cd::2} to "group:minecraft:ender_pearl"  # optional shared cooldown group
set data component "minecraft:use_cooldown" of {_item} to {_cd::*}

# marker components: true adds, false removes
set data component "minecraft:glider" of {_item} to true
```

> In `equippable`, `consumable` and `use_cooldown` a single invalid entry (e.g. `time:NaN`, a wrong slot) blocks the whole write instead of silently falling back to default values.

### Combat and attributes

```skript
# armor trim: "material:pattern"
set data component "minecraft:trim" of {_item} to "gold:coast"

# attributes: "attribute:operation:value[:slot]"
# operations: add_value (add) | add_multiplied_base (multiply) | add_multiplied_total
# slots:      any | mainhand | offhand | hand | head | chest | legs | feet | armor | body
set {_a::1} to "attack_damage:add_value:15:mainhand"
set {_a::2} to "armor:add_value:5:chest"
set {_a::3} to "movement_speed:add_multiplied_base:0.1:any"
set data component "minecraft:attribute_modifiers" of {_item} to {_a::*}
```

> The attribute name can be written as `attack_damage`, `generic.attack_damage` or `minecraft:attack_damage`. `NaN` and `Infinity` values are rejected.

### Potions

```skript
# "base:TYPE" or "effect:name:ticks:amplifier[:ambient][:particles]"
set {_p::1} to "base:strong_swiftness"
set {_p::2} to "effect:strength:1200:2"
set {_p::3} to "effect:regeneration:600:1"
set data component "minecraft:potion_contents" of {_item} to {_p::*}
```

A negative effect duration means an infinite effect, and the amplifier is clamped to 0–255.

Available base types: `water`, `mundane`, `thick`, `awkward`, `night_vision`, `long_night_vision`, `invisibility`, `long_invisibility`, `leaping`, `strong_leaping`, `long_leaping`, `fire_resistance`, `long_fire_resistance`, `swiftness`, `strong_swiftness`, `long_swiftness`, `slowness`, `strong_slowness`, `long_slowness`, `water_breathing`, `long_water_breathing`, `healing`, `strong_healing`, `harming`, `strong_harming`, `poison`, `strong_poison`, `long_poison`, `regeneration`, `strong_regeneration`, `long_regeneration`, `strength`, `strong_strength`, `long_strength`, `weakness`, `long_weakness`, `luck`, `slow_falling`, `long_slow_falling`, `wind_charged`, `weaving`, `oozing`, `infested`

### Books

```skript
set {_b::1} to "title:My Book"
set {_b::2} to "author:Steve"
set {_b::3} to "page:First page content"
set {_b::4} to "page:&0Second page &cwith color"
set data component "minecraft:written_book_content" of {_item} to {_b::*}
```

> The title is trimmed to 32 characters and the book to 100 pages.

### Banners and decoration

```skript
# banner patterns: "pattern:color"
set {_bp::1} to "stripe_top:red"
set {_bp::2} to "cross:white"
set data component "minecraft:banner_patterns" of {_item} to {_bp::*}

# base color (shields, banners)
set data component "minecraft:base_color" of {_item} to "blue"

# suspicious stew: "effect:ticks"
set {_s::1} to "speed:100"
set {_s::2} to "blindness:60"
set data component "minecraft:suspicious_stew_effects" of {_item} to {_s::*}
```

### Tools

```skript
# "speed:X" | "damage:X" | "rule:<blocks or #tag>:<speed>:<correct_for_drops>"
set {_t::1} to "speed:4.0"
set {_t::2} to "damage:1"
set {_t::3} to "rule:stone,granite,diorite:8.0:true"
set {_t::4} to "rule:#minecraft:mineable/pickaxe:6.0:true"
set data component "minecraft:tool" of {_item} to {_t::*}

# copy the rules from a vanilla item
set data component "minecraft:tool" of {_item} to data component values "minecraft:tool" of diamond pickaxe
```

> The speed in a rule must be greater than 0. A speed of `default` means the tool's default speed, and `default` in the third field leaves drops unchanged.

### Music and misc

```skript
# Ominous Bottle, level 0–4 (I–V)
set data component "minecraft:ominous_bottle_amplifier" of {_item} to 4

# Goat horn: ponder_goat_horn | sing_goat_horn | seek_goat_horn | feel_goat_horn
#            admire_goat_horn | call_goat_horn | yearn_goat_horn | dream_goat_horn
set data component "minecraft:instrument" of {_item} to "dream_goat_horn"

# Jukebox: 13 | cat | blocks | chirp | far | mall | mellohi | stal | strad | ward
#          11 | wait | otherside | 5 | pigstep | relic | precipice | creator | tears ...
set data component "minecraft:jukebox_playable" of {_item} to "minecraft:pigstep"
```

---

## Entity components

| Component | Type | Requires |
|---|---|---|
| `minecraft:custom_name` | text | any entity |
| `minecraft:custom_name_visible` | `true` / `false` | any entity |
| `minecraft:is_silent` | `true` / `false` | any entity |
| `minecraft:has_gravity` | `true` / `false` | any entity |
| `minecraft:is_invulnerable` | `true` / `false` | any entity |
| `minecraft:is_glowing` | `true` / `false` | any entity |
| `minecraft:freeze_ticks` | number | any entity |
| `minecraft:fire_ticks` | number | any entity |
| `minecraft:health` | number | LivingEntity |
| `minecraft:max_health` | number | LivingEntity |
| `minecraft:attack_damage` | number | entity with this attribute |
| `minecraft:armor` | number | LivingEntity |
| `minecraft:armor_toughness` | number | LivingEntity |
| `minecraft:movement_speed` | number | LivingEntity |
| `minecraft:follow_range` | number | entity with this attribute (mobs) |

```skript
set entity component "minecraft:custom_name" of {_entity} to "&cBoss"
set entity component "minecraft:max_health" of {_entity} to 200
set entity component "minecraft:movement_speed" of {_entity} to 0.5
set entity component "minecraft:is_invulnerable" of {_entity} to true

set {_hp} to entity component "minecraft:health" of {_entity}
if {_entity} has entity component "minecraft:custom_name":
```

> `reset` restores the default value for the given mob type (e.g. 100 HP for an iron golem), not one shared value for every mob. `NaN` and `Infinity` values are rejected.

---

## Full example

```skript
command /darksword:
    trigger:
        set {_item} to diamond sword

        set data component "minecraft:custom_name" of {_item} to "&4&lDark Sword"

        set {_lore::1} to "&8Forged in shadow"
        set {_lore::2} to "&8&oA legendary weapon"
        set data component "minecraft:lore" of {_item} to {_lore::*}

        set data component "minecraft:rarity" of {_item} to "epic"
        set data component "minecraft:max_damage" of {_item} to 5000
        set data component "minecraft:repair_cost" of {_item} to 100
        set data component "minecraft:enchantment_glint_override" of {_item} to true
        set data component "minecraft:unbreakable" of {_item} to true

        set {_e::1} to "sharpness:100"
        set {_e::2} to "fire_aspect:10"
        set {_e::3} to "unbreaking:255"
        set data component "minecraft:enchantments" of {_item} to {_e::*}

        set {_a::1} to "attack_damage:add_value:20:mainhand"
        set data component "minecraft:attribute_modifiers" of {_item} to {_a::*}

        give {_item} to player
        send "&aYou received the &4&lDark Sword&a!" to player

on data component change:
    component name is "minecraft:enchantments"
    event-player is set
    send "&eEnchantments: &7%old component value% &e-> &7%new component value%" to event-player
```

---

## Important: item safety

- **Values outside game limits are clamped or rejected**: enchantment level 1–255, effect amplifier 0–255, max 256 lore lines, max 100 pages and a title of up to 32 characters in books. The server would not be able to save an item with an out-of-range value.
- **Invalid data doesn't change the item.** In lists (enchantments, attributes, potions) invalid entries are skipped, and if none are valid the component stays unchanged. In `equippable`, `consumable` and `use_cooldown` a single invalid entry blocks the write.
- **Player text is safe to pass in.** Text or lists that are too long are rejected according to the limits in `config.yml`.
- Every rejection ends with a clear warning in the console. Repeated warnings are suppressed (unless `debug: true`).

---

## Data Components vs NBT

Since Minecraft 1.20.5, item NBT has been replaced by the **Data Components** system. HexVG-DataComponents uses the new API exclusively.

| | NBT (before 1.20.5) | Data Components |
|---|---|---|
| Type safety | none | strongly typed |
| API style | string manipulation | object-based |
| Validation | manual | built-in |
| Future | deprecated | actively developed |

---

## Example scripts

The repository contains the script `testy/hexvg-dc-test.sk`:

- `/dctest`: 49 automated tests (writing and reading, limits, events, entities) with a PASS/FAIL result
- `/dcgive`: four items for checking `tool`, `equippable`, `consumable` and `use_cooldown` in game by hand, with the expected behavior described in their lore

Both commands are for operators only. Remove or disable this script on a production server.

---

## Authors

Made for the **VenomGrave** server by the HexVG Team.  
Bugs and suggestions: https://github.com/VenomGrave/HexVG-DataComponents/issues
