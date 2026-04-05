<div align="center">

# HexVG-DataComponents

**A Skript addon for Paper 1.21.x that exposes the Data Components API**

No NBT. No Java. Just clean Skript syntax.

[![Paper](https://img.shields.io/badge/Paper-1.21.x-blue)](https://papermc.io/)
[![Skript](https://img.shields.io/badge/Skript-2.9%2B-green)](https://github.com/SkriptLang/Skript)
[![Java](https://img.shields.io/badge/Java-21%2B-orange)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)


</div>

---

## About

HexVG-DataComponents is a Skript addon built for the VenomGrave server. It gives Skript scripts full access to Minecraft's **Data Components API** — the modern replacement for NBT introduced in 1.20.5.

All the complexity of the Paper API is handled by the plugin. In Skript you just write what you want to do with item and entity data — no Java required.

## Features

- Set, read and remove any Data Component on items
- Entity pseudo-components (health, speed, custom name, glowing and more)
- Support for all 33 item components including enchantments, attributes, food, potions, tools, banners and more
- Over-limit enchantment levels (e.g. Sharpness 255)
- Custom datapack enchants
- `on data component change` and `on data component remove` events with cancel support
- Type-safe — all values processed through Paper's typed API, not raw NBT strings
- All libraries bundled in the jar — no extra dependencies

## Requirements

| | Version |
|---|---|
| [Paper](https://papermc.io/) | 1.21.1 – 1.21.4 |
| [Skript](https://github.com/SkriptLang/Skript) | 2.9+ |
| Java | 21+ |

---

## Syntax Reference

### Effects

```skript
# Set a component on an item
set data component "minecraft:custom_name" of {_item} to colored "&cName"
set data component "minecraft:damage" of {_item} to 100
set data component "minecraft:unbreakable" of {_item} to true
set data component "minecraft:lore" of {_item} to {_lore::*}

# Remove a component from an item
remove data component "minecraft:lore" from {_item}

# Set a component on an entity
set entity component "minecraft:custom_name" of {_entity} to colored "&cBoss"
set entity component "minecraft:max_health" of {_entity} to 200
set entity component "minecraft:is_glowing" of {_entity} to true

# Remove / reset an entity component
remove entity component "minecraft:custom_name" from {_entity}
reset entity component "minecraft:max_health" of {_entity}

# Cancel inside events
cancel [the] [data] component change
cancel [the] [data] component removal
```

### Expressions

```skript
# Read a component value — always store in a variable first before using in send
set {_val} to data component "minecraft:damage" of {_item}
set {_name} to data component "minecraft:custom_name" of {_item}

# Read an entity component value
set {_hp} to entity component "minecraft:health" of {_entity}

# List all components on an item
set {_all::*} to all data components of {_item}

# Count components on an item
set {_n} to data component count of {_item}

# All known component names registered in the Paper API
set {_known::*} to all known data components

# Create a copy of an item with a component already set
set {_item} to {_base} with data component "minecraft:custom_name" set to colored "&aNew Item"

# Inside events
set {_name} to component name
set {_old}  to old component value
set {_new}  to new component value
```

### Conditions

```skript
# Item component checks
if {_item} has data component "minecraft:custom_name":
if {_item} doesn't have data component "minecraft:unbreakable":
if {_item} does not have data component "minecraft:lore":

# Compare a component value
if data component "minecraft:damage" of {_item} is 100:
if data component "minecraft:custom_name" of {_item} equals colored "&cBoss":
if data component "minecraft:custom_name" of {_item} isn't colored "&cBoss":

# Validate a component name
if "minecraft:custom_name" is a valid data component:
if "minecraft:custom_name" is a known data component:
if "fake:component" isn't a valid data component:

# Entity component checks
if {_entity} has entity component "minecraft:custom_name":
if {_entity} doesn't have entity component "minecraft:is_silent":
```

### Events

```skript
on data component change:
    set {_name} to component name
    set {_old}  to old component value
    set {_new}  to new component value
    cancel component change     # prevent the change

on data component remove:
    set {_name} to component name
    set {_old}  to old component value
    cancel component removal    # prevent the removal
```

---

## Item Components

### 📝 Text & Display

| Component | Format | Example |
|---|---|---|
| `minecraft:custom_name` | string with `&` codes | `colored "&4&lDark Sword"` |
| `minecraft:item_name` | string with `&` codes | `colored "&7Iron Blade"` |
| `minecraft:lore` | list of strings with `&` codes | `["&7Line 1", "&8Line 2"]` |
| `minecraft:rarity` | `common` / `uncommon` / `rare` / `epic` | `"epic"` |
| `minecraft:item_model` | NamespacedKey | `"mypack:weapons/sword"` |

### ⚙️ Durability & Stack

| Component | Format | Notes |
|---|---|---|
| `minecraft:damage` | number | Current damage value |
| `minecraft:max_damage` | number ≥ 1 | Max durability |
| `minecraft:max_stack_size` | number 1–99 | Max stack size |
| `minecraft:repair_cost` | number | Anvil cost in XP levels |
| `minecraft:unbreakable` | boolean | `true` / `false` |

### ✨ Enchantments

| Component | Format | Notes |
|---|---|---|
| `minecraft:enchantments` | list of `"name:level"` | `["sharpness:5", "unbreaking:3"]` |
| `minecraft:stored_enchantments` | list of `"name:level"` | For enchanted books |
| `minecraft:enchantment_glint_override` | boolean | Force or suppress glint |

Over-limit levels and custom datapack enchants are supported:

```skript
# Vanilla over-limit levels
set {_e::1} to "sharpness:255"
set {_e::2} to "unbreaking:100"
set data component "minecraft:enchantments" of {_item} to {_e::*}

# Custom datapack enchants (mixed with vanilla)
set {_e::1} to "myserver:darkness_touch:1"
set {_e::2} to "sharpness:5"
set data component "minecraft:enchantments" of {_item} to {_e::*}
```

### 🎨 Visual

| Component | Format | Notes |
|---|---|---|
| `minecraft:custom_model_data` | number | Resource pack model ID |
| `minecraft:dyed_color` | `"#RRGGBB"` or `"R,G,B"` or decimal | `"#FF4400"` |
| `minecraft:hide_tooltip` | boolean | Hides the entire tooltip |

### 🍖 Food & Consumable

```skript
# Food: "nutrition:saturation[:canAlwaysEat]"
set data component "minecraft:food" of {_item} to "4:1.2:true"

# Consumable — must also have minecraft:food set
set {_c::1} to "time:0.5"         # seconds (default 1.6)
set {_c::2} to "animation:drink"  # none | eat | drink | block | bow | spear | crossbow | spyglass | toot_horn | brush
set {_c::3} to "sound:minecraft:entity.generic.drink"
set data component "minecraft:consumable" of {_item} to {_c::*}
```

### 🛡️ Equipment & Mechanics

```skript
# Equippable — any item can be worn in any slot
set {_e::1} to "slot:head"        # head | chest | legs | feet | mainhand | offhand | body
set {_e::2} to "sound:minecraft:item.armor.equip_diamond"
set {_e::3} to "swappable:true"
set {_e::4} to "dispensable:true"
set data component "minecraft:equippable" of {_item} to {_e::*}

# Use cooldown
set {_cd::1} to "time:2.0"                       # seconds
set {_cd::2} to "group:minecraft:ender_pearl"     # optional shared cooldown group
set data component "minecraft:use_cooldown" of {_item} to {_cd::*}

# Simple boolean flags
set data component "minecraft:fire_resistant" of {_item} to true
set data component "minecraft:glider" of {_item} to true
```

### ⚔️ Combat & Attributes

```skript
# Armor trim: "material:pattern"
set data component "minecraft:trim" of {_item} to "gold:coast"

# Attribute modifiers: "attribute:operation:value:slot"
# Operations: add_value | add_multiplied_base | add_multiplied_total
# Slots: any | mainhand | offhand | hand | head | chest | legs | feet | armor | body
set {_a::1} to "generic.attack_damage:add_value:15:mainhand"
set {_a::2} to "generic.armor:add_value:5:chest"
set {_a::3} to "generic.movement_speed:add_multiplied_base:0.1:any"
set data component "minecraft:attribute_modifiers" of {_item} to {_a::*}
```

### 🧪 Potions

```skript
# "base:TYPE" or "effect:name:ticks:amplifier"
set {_p::1} to "base:strong_swiftness"
set {_p::2} to "effect:strength:1200:2"
set {_p::3} to "effect:regeneration:600:1"
set data component "minecraft:potion_contents" of {_item} to {_p::*}
```

Available base types: `water`, `mundane`, `thick`, `awkward`, `night_vision`, `long_night_vision`, `invisibility`, `long_invisibility`, `leaping`, `strong_leaping`, `long_leaping`, `fire_resistance`, `long_fire_resistance`, `swiftness`, `strong_swiftness`, `long_swiftness`, `slowness`, `strong_slowness`, `long_slowness`, `water_breathing`, `long_water_breathing`, `healing`, `strong_healing`, `harming`, `strong_harming`, `poison`, `strong_poison`, `long_poison`, `regeneration`, `strong_regeneration`, `long_regeneration`, `strength`, `strong_strength`, `long_strength`, `weakness`, `long_weakness`, `luck`, `slow_falling`, `long_slow_falling`, `wind_charged`, `weaving`, `oozing`, `infested`

### 📚 Books

```skript
set {_b::1} to "title:My Book"
set {_b::2} to "author:Steve"
set {_b::3} to "page:First page content"
set {_b::4} to "page:Second page content"
set data component "minecraft:written_book_content" of {_item} to {_b::*}
```

### 🚩 Banners & Decoration

```skript
# Banner patterns: "pattern:color"
set {_bp::1} to "stripe_top:red"
set {_bp::2} to "cross:white"
set data component "minecraft:banner_patterns" of {_item} to {_bp::*}

# Base color (for shields / banners)
set data component "minecraft:base_color" of {_item} to "blue"

# Suspicious stew: "effect:ticks"
set {_s::1} to "speed:100"
set {_s::2} to "blindness:60"
set data component "minecraft:suspicious_stew_effects" of {_item} to {_s::*}
```

### 🔧 Tools

```skript
# "speed:X" | "damage:X" | "rule:blocks:speed:drops_correctly"
set {_t::1} to "speed:4.0"
set {_t::2} to "damage:1"
set {_t::3} to "rule:stone,granite,diorite:8.0:true"
set {_t::4} to "rule:iron_ore,gold_ore:6.0:true"
set data component "minecraft:tool" of {_item} to {_t::*}
```

### 🎵 Music & Misc

```skript
# Ominous Bottle level 0–4 (I–V)
set data component "minecraft:ominous_bottle_amplifier" of {_item} to 4

# Goat Horn — ponder_goat_horn | sing_goat_horn | seek_goat_horn | feel_goat_horn
#              admire_goat_horn | call_goat_horn | yearn_goat_horn | dream_goat_horn
set data component "minecraft:instrument" of {_item} to "dream_goat_horn"

# Jukebox — 13 | cat | blocks | chirp | far | mall | mellohi | stal | strad | ward
#            11 | wait | otherside | 5 | pigstep | relic | precipice | creator | tears
set data component "minecraft:jukebox_playable" of {_item} to "minecraft:pigstep"
```

---

## Entity Components

| Component | Type | Requires |
|---|---|---|
| `minecraft:custom_name` | string | Any entity |
| `minecraft:custom_name_visible` | boolean | Any entity |
| `minecraft:is_silent` | boolean | Any entity |
| `minecraft:has_gravity` | boolean | Any entity |
| `minecraft:is_invulnerable` | boolean | Any entity |
| `minecraft:is_glowing` | boolean | Any entity |
| `minecraft:freeze_ticks` | number | Any entity |
| `minecraft:fire_ticks` | number | Any entity |
| `minecraft:health` | number | LivingEntity |
| `minecraft:max_health` | number | LivingEntity |
| `minecraft:attack_damage` | number | LivingEntity |
| `minecraft:armor` | number | LivingEntity |
| `minecraft:armor_toughness` | number | LivingEntity |
| `minecraft:movement_speed` | number | LivingEntity |
| `minecraft:follow_range` | number | Mob |

```skript
set entity component "minecraft:custom_name" of {_entity} to colored "&cBoss"
set entity component "minecraft:max_health" of {_entity} to 200
set entity component "minecraft:movement_speed" of {_entity} to 0.5
set entity component "minecraft:is_invulnerable" of {_entity} to true

set {_hp} to entity component "minecraft:health" of {_entity}
if {_entity} has entity component "minecraft:custom_name":
```

---

## Full Example

```skript
command /darksword:
    trigger:
        set {_item} to diamond sword

        set data component "minecraft:custom_name" of {_item} to colored "&4&lDark Sword"

        set {_lore::1} to colored "&8Forged in shadow"
        set {_lore::2} to colored "&8&oA legendary weapon"
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

        set {_a::1} to "generic.attack_damage:add_value:20:mainhand"
        set data component "minecraft:attribute_modifiers" of {_item} to {_a::*}

        give {_item} to player
        send colored "&aYou received the &4&lDark Sword&a!" to player

on data component change:
    if component name = "minecraft:damage":
        set {_old} to old component value
        set {_new} to new component value
        send colored "&eDurability changed: &7%{_old}% -> %{_new}%" to player
```

---

## Data Components vs NBT

Since Minecraft 1.20.5, NBT has been replaced by the **Data Components** system. HexVG-DataComponents uses the new API exclusively.

| | NBT (pre-1.20.5) | Data Components |
|---|---|---|
| Type safety | None | Strongly typed |
| API style | String manipulation | Object-based |
| Validation | Manual | Built-in |
| Future | Deprecated | Actively developed |

---

## License

[MIT](LICENSE) — free to use, modify and distribute.
