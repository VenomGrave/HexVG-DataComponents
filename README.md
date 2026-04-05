<div align="center">

# HexVG-DataComponents

**Dodatek Skript dla Paper 1.21.x udostępniający API Data Components**

Bez NBT. Bez Javy. Czysta składnia Skript.

[![Paper](https://img.shields.io/badge/Paper-1.21.x-blue)](https://papermc.io/)
[![Skript](https://img.shields.io/badge/Skript-2.9%2B-green)](https://github.com/SkriptLang/Skript)
[![Java](https://img.shields.io/badge/Java-21%2B-orange)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

🇬🇧 [English version → README.md](README.md)

</div>

---

## O projekcie

HexVG-DataComponents to addon do Skripta stworzony na potrzeby serwera VenomGrave. Daje skryptom Skript pełny dostęp do **API Data Components** — nowoczesnego zamiennika NBT wprowadzonego w Minecrafcie 1.20.5.

Cała złożoność Paper API jest obsługiwana przez plugin. W Skrypcie piszesz tylko co chcesz zrobić z danymi itemów i encji — żadnej Javy.

## Funkcje

- Ustawianie, odczyt i usuwanie dowolnego Data Component na itemach
- Pseudo-komponenty encji (zdrowie, prędkość, nazwa, świecenie i inne)
- Obsługa wszystkich 33 komponentów itemów — enchanty, atrybuty, jedzenie, mikstury, narzędzia, bannery i więcej
- Enchanty z ponadlimitowymi poziomami (np. Ostrość 255)
- Własne enchanty z datapacków
- Eventy `on data component change` i `on data component remove` z możliwością anulowania
- Bezpieczne typowanie — wartości przetwarzane przez typowane API Paper, nie surowe stringi NBT
- Wszystkie biblioteki spakowane w jarze — brak dodatkowych zależności

## Wymagania

| | Wersja |
|---|---|
| [Paper](https://papermc.io/) | 1.21.1 – 1.21.4 |
| [Skript](https://github.com/SkriptLang/Skript) | 2.9+ |
| Java | 21+ |

## Instalacja

1. Pobierz `HexVG-DataComponents.jar`
2. Wrzuć do folderu `plugins/` swojego serwera
3. Uruchom serwer ponownie — brak konfiguracji do ustawienia

---

## Składnia

### Efekty (Effects)

```skript
# Ustawianie komponentu na itemie
set data component "minecraft:custom_name" of {_item} to colored "&cNazwa"
set data component "minecraft:damage" of {_item} to 100
set data component "minecraft:unbreakable" of {_item} to true
set data component "minecraft:lore" of {_item} to {_lore::*}

# Usuwanie komponentu z itemu
remove data component "minecraft:lore" from {_item}

# Ustawianie komponentu na encji
set entity component "minecraft:custom_name" of {_entity} to colored "&cBoss"
set entity component "minecraft:max_health" of {_entity} to 200
set entity component "minecraft:is_glowing" of {_entity} to true

# Usuwanie / resetowanie komponentu encji
remove entity component "minecraft:custom_name" from {_entity}
reset entity component "minecraft:max_health" of {_entity}

# Anulowanie w eventach
cancel [the] [data] component change
cancel [the] [data] component removal
```

### Wyrażenia (Expressions)

```skript
# Odczyt wartości komponentu — zawsze zapisz do zmiennej przed użyciem w send
set {_val} to data component "minecraft:damage" of {_item}
set {_name} to data component "minecraft:custom_name" of {_item}

# Odczyt wartości komponentu encji
set {_hp} to entity component "minecraft:health" of {_entity}

# Lista wszystkich komponentów na itemie
set {_all::*} to all data components of {_item}

# Liczba komponentów na itemie
set {_n} to data component count of {_item}

# Wszystkie znane nazwy komponentów zarejestrowane w Paper API
set {_known::*} to all known data components

# Kopia itemu z już ustawionym komponentem
set {_item} to {_baza} with data component "minecraft:custom_name" set to colored "&aNowy"

# Wewnątrz eventów
set {_name} to component name
set {_old}  to old component value
set {_new}  to new component value
```

### Warunki (Conditions)

```skript
# Sprawdzanie komponentu itemu
if {_item} has data component "minecraft:custom_name":
if {_item} doesn't have data component "minecraft:unbreakable":
if {_item} does not have data component "minecraft:lore":

# Porównanie wartości komponentu
if data component "minecraft:damage" of {_item} is 100:
if data component "minecraft:custom_name" of {_item} equals colored "&cBoss":
if data component "minecraft:custom_name" of {_item} isn't colored "&cBoss":

# Walidacja nazwy komponentu
if "minecraft:custom_name" is a valid data component:
if "minecraft:custom_name" is a known data component:
if "fake:komponent" isn't a valid data component:

# Sprawdzanie komponentu encji
if {_entity} has entity component "minecraft:custom_name":
if {_entity} doesn't have entity component "minecraft:is_silent":
```

### Eventy

```skript
on data component change:
    set {_name} to component name
    set {_old}  to old component value
    set {_new}  to new component value
    cancel component change     # anuluj zmianę

on data component remove:
    set {_name} to component name
    set {_old}  to old component value
    cancel component removal    # anuluj usunięcie
```

---

## Komponenty itemów

### 📝 Tekst i wygląd

| Komponent | Format | Przykład |
|---|---|---|
| `minecraft:custom_name` | string z kodami `&` | `colored "&4&lMroczny Miecz"` |
| `minecraft:item_name` | string z kodami `&` | `colored "&7Żelazne Ostrze"` |
| `minecraft:lore` | lista stringów z kodami `&` | `["&7Linia 1", "&8Linia 2"]` |
| `minecraft:rarity` | `common` / `uncommon` / `rare` / `epic` | `"epic"` |
| `minecraft:item_model` | NamespacedKey | `"mojpack:bronie/miecz"` |

### ⚙️ Trwałość i rozmiar stosu

| Komponent | Format | Opis |
|---|---|---|
| `minecraft:damage` | liczba | Aktualne uszkodzenie |
| `minecraft:max_damage` | liczba ≥ 1 | Maksymalna trwałość |
| `minecraft:max_stack_size` | liczba 1–99 | Maksymalny rozmiar stosu |
| `minecraft:repair_cost` | liczba | Koszt naprawy na kowadle (poziomy XP) |
| `minecraft:unbreakable` | boolean | `true` / `false` |

### ✨ Enchanty

| Komponent | Format | Opis |
|---|---|---|
| `minecraft:enchantments` | lista `"nazwa:poziom"` | `["sharpness:5", "unbreaking:3"]` |
| `minecraft:stored_enchantments` | lista `"nazwa:poziom"` | Dla ksiąg z enchantami |
| `minecraft:enchantment_glint_override` | boolean | Wymuś lub wyłącz świecenie |

Obsługiwane poziomy ponadlimitowe i własne enchanty z datapacku:

```skript
# Vanilla z ponadlimitowymi poziomami
set {_e::1} to "sharpness:255"
set {_e::2} to "unbreaking:100"
set data component "minecraft:enchantments" of {_item} to {_e::*}

# Własny enchant z datapacku (można mieszać z vanilla)
set {_e::1} to "mojaserver:mroczny_dotyk:1"
set {_e::2} to "sharpness:5"
set data component "minecraft:enchantments" of {_item} to {_e::*}
```

### 🎨 Wygląd

| Komponent | Format | Opis |
|---|---|---|
| `minecraft:custom_model_data` | liczba | ID modelu w resource packu |
| `minecraft:dyed_color` | `"#RRGGBB"` lub `"R,G,B"` lub decimal | `"#FF4400"` |
| `minecraft:hide_tooltip` | boolean | Ukrywa cały tooltip |

### 🍖 Jedzenie i konsumpcja

```skript
# Food: "hunger:nasycenie[:zawsze_jedzony]"
set data component "minecraft:food" of {_item} to "4:1.2:true"

# Consumable — wymaga też ustawionego minecraft:food
set {_c::1} to "time:0.5"         # sekundy (domyślnie 1.6)
set {_c::2} to "animation:drink"  # none | eat | drink | block | bow | spear | crossbow | spyglass | toot_horn | brush
set {_c::3} to "sound:minecraft:entity.generic.drink"
set data component "minecraft:consumable" of {_item} to {_c::*}
```

### 🛡️ Ekwipunek i mechaniki

```skript
# Equippable — każdy przedmiot można założyć w dowolny slot
set {_e::1} to "slot:head"        # head | chest | legs | feet | mainhand | offhand | body
set {_e::2} to "sound:minecraft:item.armor.equip_diamond"
set {_e::3} to "swappable:true"
set {_e::4} to "dispensable:true"
set data component "minecraft:equippable" of {_item} to {_e::*}

# Cooldown po użyciu
set {_cd::1} to "time:2.0"                       # sekundy
set {_cd::2} to "group:minecraft:ender_pearl"     # opcjonalna wspólna grupa cooldownu
set data component "minecraft:use_cooldown" of {_item} to {_cd::*}

# Proste flagi boolean
set data component "minecraft:fire_resistant" of {_item} to true
set data component "minecraft:glider" of {_item} to true
```

### ⚔️ Walka i atrybuty

```skript
# Ozdoba zbroi: "material:wzor"
set data component "minecraft:trim" of {_item} to "gold:coast"

# Modyfikatory atrybutów: "atrybut:operacja:wartość:slot"
# Operacje: add_value | add_multiplied_base | add_multiplied_total
# Sloty: any | mainhand | offhand | hand | head | chest | legs | feet | armor | body
set {_a::1} to "generic.attack_damage:add_value:15:mainhand"
set {_a::2} to "generic.armor:add_value:5:chest"
set {_a::3} to "generic.movement_speed:add_multiplied_base:0.1:any"
set data component "minecraft:attribute_modifiers" of {_item} to {_a::*}
```

### 🧪 Mikstury

```skript
# "base:TYP" lub "effect:nazwa:ticki:wzmocnienie"
set {_p::1} to "base:strong_swiftness"
set {_p::2} to "effect:strength:1200:2"
set {_p::3} to "effect:regeneration:600:1"
set data component "minecraft:potion_contents" of {_item} to {_p::*}
```

Dostępne typy bazowe: `water`, `mundane`, `thick`, `awkward`, `night_vision`, `long_night_vision`, `invisibility`, `long_invisibility`, `leaping`, `strong_leaping`, `long_leaping`, `fire_resistance`, `long_fire_resistance`, `swiftness`, `strong_swiftness`, `long_swiftness`, `slowness`, `strong_slowness`, `long_slowness`, `water_breathing`, `long_water_breathing`, `healing`, `strong_healing`, `harming`, `strong_harming`, `poison`, `strong_poison`, `long_poison`, `regeneration`, `strong_regeneration`, `long_regeneration`, `strength`, `strong_strength`, `long_strength`, `weakness`, `long_weakness`, `luck`, `slow_falling`, `long_slow_falling`, `wind_charged`, `weaving`, `oozing`, `infested`

### 📚 Książki

```skript
set {_b::1} to "title:Moja Ksiazka"
set {_b::2} to "author:Steve"
set {_b::3} to "page:Tekst pierwszej strony"
set {_b::4} to "page:Tekst drugiej strony"
set data component "minecraft:written_book_content" of {_item} to {_b::*}
```

### 🚩 Bannery i dekoracje

```skript
# Wzory banneru: "wzor:kolor"
set {_bp::1} to "stripe_top:red"
set {_bp::2} to "cross:white"
set data component "minecraft:banner_patterns" of {_item} to {_bp::*}

# Kolor podstawowy (tarcza / banner)
set data component "minecraft:base_color" of {_item} to "blue"

# Podejrzany gulasz: "efekt:ticki"
set {_s::1} to "speed:100"
set {_s::2} to "blindness:60"
set data component "minecraft:suspicious_stew_effects" of {_item} to {_s::*}
```

### 🔧 Narzędzia

```skript
# "speed:X" | "damage:X" | "rule:bloki:prędkość:poprawne_dropy"
set {_t::1} to "speed:4.0"
set {_t::2} to "damage:1"
set {_t::3} to "rule:stone,granite,diorite:8.0:true"
set {_t::4} to "rule:iron_ore,gold_ore:6.0:true"
set data component "minecraft:tool" of {_item} to {_t::*}
```

### 🎵 Muzyka i inne

```skript
# Złowróżbna butelka — poziom 0–4 (I–V)
set data component "minecraft:ominous_bottle_amplifier" of {_item} to 4

# Róg kozi — ponder_goat_horn | sing_goat_horn | seek_goat_horn | feel_goat_horn
#             admire_goat_horn | call_goat_horn | yearn_goat_horn | dream_goat_horn
set data component "minecraft:instrument" of {_item} to "dream_goat_horn"

# Szafka grająca — 13 | cat | blocks | chirp | far | mall | mellohi | stal | strad | ward
#                  11 | wait | otherside | 5 | pigstep | relic | precipice | creator | tears
set data component "minecraft:jukebox_playable" of {_item} to "minecraft:pigstep"
```

---

## Komponenty encji

| Komponent | Typ | Wymaga |
|---|---|---|
| `minecraft:custom_name` | string | Dowolna encja |
| `minecraft:custom_name_visible` | boolean | Dowolna encja |
| `minecraft:is_silent` | boolean | Dowolna encja |
| `minecraft:has_gravity` | boolean | Dowolna encja |
| `minecraft:is_invulnerable` | boolean | Dowolna encja |
| `minecraft:is_glowing` | boolean | Dowolna encja |
| `minecraft:freeze_ticks` | liczba | Dowolna encja |
| `minecraft:fire_ticks` | liczba | Dowolna encja |
| `minecraft:health` | liczba | LivingEntity |
| `minecraft:max_health` | liczba | LivingEntity |
| `minecraft:attack_damage` | liczba | LivingEntity |
| `minecraft:armor` | liczba | LivingEntity |
| `minecraft:armor_toughness` | liczba | LivingEntity |
| `minecraft:movement_speed` | liczba | LivingEntity |
| `minecraft:follow_range` | liczba | Mob |

```skript
set entity component "minecraft:custom_name" of {_entity} to colored "&cBoss"
set entity component "minecraft:max_health" of {_entity} to 200
set entity component "minecraft:movement_speed" of {_entity} to 0.5
set entity component "minecraft:is_invulnerable" of {_entity} to true

set {_hp} to entity component "minecraft:health" of {_entity}
if {_entity} has entity component "minecraft:custom_name":
```

---

## Pełny przykład

```skript
command /mroczny_miecz:
    trigger:
        set {_item} to diamond sword

        set data component "minecraft:custom_name" of {_item} to colored "&4&lMroczny Miecz"

        set {_lore::1} to colored "&8Wykuty w ciemności"
        set {_lore::2} to colored "&8&oLegendarny oręż"
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
        send colored "&aDostałeś &4&lMroczny Miecz&a!" to player

on data component change:
    if component name = "minecraft:damage":
        set {_old} to old component value
        set {_new} to new component value
        send colored "&eTrwałość zmieniona: &7%{_old}% -> %{_new}%" to player
```

---

## Data Components vs NBT

Od Minecrafta 1.20.5 system NBT został zastąpiony przez **Data Components**. HexVG-DataComponents korzysta wyłącznie z nowego API.

| | NBT (przed 1.20.5) | Data Components |
|---|---|---|
| Bezpieczeństwo typów | Brak | Silnie typowane |
| Styl API | Manipulacja stringami | Obiektowe |
| Walidacja | Ręczna | Wbudowana |
| Wsparcie | Wycofywane | Aktywnie rozwijane |

---

## Licencja

[MIT](LICENSE) — można używać, modyfikować i dystrybuować bez ograniczeń.
