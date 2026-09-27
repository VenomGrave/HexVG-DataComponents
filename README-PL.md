# HexVG-DataComponents

**Polski** | [English](README-EN.md)

> Addon do Skripta obsługujący Data Components przedmiotów i encji na serwerze **VenomGrave**

![version](https://img.shields.io/badge/wersja-1.2.0-blue)
![paper](https://img.shields.io/badge/Paper-1.21.4%2B%20%7C%2026.x-green)
![java](https://img.shields.io/badge/Java-21%2B-orange)
![skript](https://img.shields.io/badge/Skript-2.10%2B-purple)
![license](https://img.shields.io/badge/licencja-MIT-gray)

---

## O projekcie

HexVG-DataComponents to addon do Skripta stworzony na potrzeby serwera VenomGrave. Daje skryptom pełny dostęp do **Data Components API** Minecrafta, czyli systemu, który od wersji 1.20.5 zastąpił NBT. Nazwa, lore, enchanty, narzędzie, jedzenie, zakładanie na głowę, cooldown i dziesiątki innych właściwości przedmiotu to dziś osobne komponenty.

Całą złożoność API Papera bierze na siebie plugin. W Skripcie piszesz tylko, co chcesz zrobić z przedmiotem lub encją, bez NBT i bez Javy.

Sporo pracy poszło w to, żeby żaden skrypt nie mógł zepsuć przedmiotu. Minecraft odrzuca przy zapisie przedmioty z wartościami spoza swoich limitów (np. jednocześnie niszczalne i stackowalne), a taki przedmiot potrafi zniknąć z ekwipunku albo zablokować zapis chunka. Plugin pilnuje tych limitów, odrzuca `NaN`/`Infinity` i ogranicza rozmiar danych, więc bezpiecznie przekażesz do komponentu nawet tekst wpisany przez gracza. Odczyt działa w tym samym formacie co zapis: wartość odczytaną z jednego przedmiotu możesz od razu ustawić na innym.

---

## Funkcje

- **Ustawianie, odczyt i usuwanie** komponentów przedmiotów, także komponentów domyślnych (np. jedzenie z jabłka)
- **Ponad 30 komponentów** z pełną obsługą: enchanty, atrybuty, jedzenie, mikstury, narzędzia, zakładanie, cooldown, bannery, książki i inne
- **Każdy komponent-znacznik** (np. `glider`) obsługiwany automatycznie przez `true` / `false`
- **Odczyt zgodny z zapisem**: reguły kopania z diamentowego kilofa skopiujesz na patyk jedną linijką
- **Enchanty ponad limit** (np. Sharpness 255) i **własne enchanty z datapacków**
- **Reguły narzędzi z tagami bloków** (`#minecraft:mineable/pickaxe`)
- **Komponenty encji**: zdrowie, maks. HP, prędkość, obrażenia, pancerz, świecenie, grawitacja, nazwa i inne
- **Eventy** `data component change` i `data component remove` z anulowaniem i podmianą zapisywanej wartości
- **Kolory** `&a` oraz hex `&#RRGGBB` w nazwach, lore i książkach
- **Bezpieczeństwo typów**: wszystkie wartości przechodzą przez typowane API Papera, a nie surowe ciągi NBT
- **Ochrona przed zepsutymi przedmiotami** i **odrzucanie `NaN`/`Infinity`**
- **Limity rozmiaru** tekstów i list chroniące przed „chunk banem” z danych od graczy
- **Literówka nie niszczy przedmiotu**: błędny wpis nie nadpisze komponentu pustą ani domyślną wartością
- **Konsola nie zostanie zalana**: to samo ostrzeżenie pojawia się maks. raz na 10 s
- Brak dodatkowych zależności poza Skriptem

---

## Wymagania

| Wymaganie | Wersja |
|-----------|--------|
| Paper | 1.21.4+ / 26.x (także forki, np. Purpur) |
| Skript | 2.10+ (testowane na 2.14.3 i 2.16.2) |
| Java | 21+ |

> Spigot i Bukkit nie są obsługiwane, bo nie mają Data Components API.

---

## Konfiguracja

```yaml
debug: false   # dodatkowe logi, wyłącza tłumienie powtarzających się ostrzeżeń

limits:
  max-string-length: 8192   # maks. długość pojedynczego tekstu (nazwa, linia lore, strona książki)
  max-list-size: 256        # maks. liczba elementów listy (lore, enchanty, atrybuty...)
```

---

## Składnia Skript

### Efekty

```skript
# ustawienie komponentu przedmiotu
set data component "minecraft:custom_name" of {_item} to "&cNazwa"
set data component "minecraft:damage" of {_item} to 100
set data component "minecraft:unbreakable" of {_item} to true
set data component "minecraft:lore" of {_item} to {_lore::*}

# usunięcie komponentu przedmiotu
remove data component "minecraft:lore" from {_item}
delete data component "minecraft:food" of {_item}

# komponenty encji
set entity component "minecraft:custom_name" of {_entity} to "&cBoss"
set entity component "minecraft:max_health" of {_entity} to 200
remove entity component "minecraft:custom_name" from {_entity}
reset entity component "minecraft:max_health" of {_entity}

# anulowanie w eventach
cancel data component change
cancel data component removal
```

> Efekty `set` i `remove` wymagają przedmiotu, który da się nadpisać (zmienna, `player's tool`, `event-item`). Użycie stałej, np. `diamond sword`, zgłasza błąd już przy ładowaniu skryptu.

### Wyrażenia

```skript
# odczyt jednej wartości (listy jako tekst "[a, b]")
set {_dmg} to data component "minecraft:damage" of {_item}
set {_name} to data component "minecraft:custom_name" of {_item}

# odczyt listy element po elemencie (do loop / contains / amount of)
set {_lore::*} to data component values "minecraft:lore" of {_item}

# komponent encji
set {_hp} to entity component "minecraft:health" of {_entity}

# wszystkie komponenty przedmiotu i ich liczba
set {_all::*} to all data components of {_item}
set {_n} to data component count of {_item}

# wszystkie komponenty znane serwerowi
set {_known::*} to all known data components

# kopia przedmiotu z ustawionym komponentem (oryginał bez zmian)
set {_copy} to {_base} with data component "minecraft:custom_name" set to "&aNowy przedmiot"

# w eventach
set {_name} to component name
set {_old} to old component value
set {_new} to new component value
```

### Warunki

```skript
# obecność komponentu
if {_item} has data component "minecraft:custom_name":
if {_item} doesn't have data component "minecraft:unbreakable":

# porównanie wartości (liczby porównywane numerycznie: 5 = 5.0)
if data component "minecraft:damage" of {_item} is 100:
if data component "minecraft:custom_name" of {_item} is not "&cBoss":

# poprawność nazwy komponentu
if "minecraft:custom_name" is a valid data component:    # format namespace:path
if "minecraft:custom_name" is a known data component:    # istnieje na tym serwerze

# komponenty encji
if {_entity} has entity component "minecraft:custom_name":
```

### Eventy

```skript
on data component change:
    event-player is set
    event-player doesn't have permission "venomgrave.bypass"
    component name is "minecraft:enchantments"
    cancel data component change

on data component change:
    component name is "minecraft:rarity"
    new component value is "epic"
    set new component value to "rare"      # podmiana zapisywanej wartości

on data component remove:
    component name is "minecraft:unbreakable"
    cancel data component removal
```

Dostępne w eventach: `event-player` (jeśli zmianę wywołał gracz), `event-item`, `component name`, `old component value`, `new component value`.

> Eventy obejmują zmiany wykonane przez ten addon, a nie np. kowadło czy stół do enchantowania.

---

## Komponenty przedmiotów

Wszystkie wartości odczytane przez plugin mają ten sam format co przy zapisie, więc można je od razu przenieść na inny przedmiot.

### Tekst i wygląd

| Komponent | Format | Przykład |
|---|---|---|
| `minecraft:custom_name` | tekst, kolory `&` i `&#RRGGBB` | `"&4&lMroczny Miecz"` |
| `minecraft:item_name` | tekst, kolory `&` i `&#RRGGBB` | `"&7Żelazne Ostrze"` |
| `minecraft:lore` | lista tekstów (maks. 256 linii) | `"&7Linia 1" and "&8Linia 2"` |
| `minecraft:rarity` | `common` / `uncommon` / `rare` / `epic` | `"epic"` |
| `minecraft:item_model` | klucz | `"mojpack:bronie/miecz"` |
| `minecraft:custom_model_data` | liczba | `1001` |
| `minecraft:dyed_color` | `#RRGGBB`, `R,G,B` lub liczba | `"#FF4400"` |
| `minecraft:enchantment_glint_override` | `true` / `false` | wymusza lub ukrywa połysk |

### Wytrzymałość i stack

| Komponent | Format | Uwagi |
|---|---|---|
| `minecraft:damage` | liczba ≥ 0 | aktualne zużycie |
| `minecraft:max_damage` | liczba ≥ 1 | maks. wytrzymałość, tylko przedmioty z `max_stack_size` = 1 |
| `minecraft:max_stack_size` | liczba 1–99 | tylko przedmioty bez `max_damage` |
| `minecraft:repair_cost` | liczba ≥ 0 | koszt naprawy w kowadle |
| `minecraft:unbreakable` | `true` / `false` | niezniszczalność |

> Przedmiot nie może być jednocześnie niszczalny i stackowalny. Żeby dać `max_damage` przedmiotowi, który się stackuje, najpierw ustaw `max_stack_size` na 1.

### Enchanty

| Komponent | Format | Uwagi |
|---|---|---|
| `minecraft:enchantments` | lista `"nazwa:poziom"` | poziom 1–255 |
| `minecraft:stored_enchantments` | lista `"nazwa:poziom"` | dla zaklętych książek |

Obsługiwane są poziomy ponad limit oraz własne enchanty z datapacków:

```skript
# poziomy ponad vanilla
set {_e::1} to "sharpness:255"
set {_e::2} to "unbreaking:100"
set data component "minecraft:enchantments" of {_item} to {_e::*}

# enchanty z datapacka (razem z vanilla)
set {_e::1} to "mojserwer:dotyk_ciemnosci:1"
set {_e::2} to "sharpness:5"
set data component "minecraft:enchantments" of {_item} to {_e::*}
```

### Jedzenie i spożywanie

```skript
# food: "odżywianie:nasycenie[:zawszeJadalne]"
set data component "minecraft:food" of {_item} to "4:1.2:true"

# consumable: jak długo i z jaką animacją przedmiot się zjada/pije
set {_c::1} to "time:0.5"         # sekundy (domyślnie 1.6)
set {_c::2} to "animation:drink"  # none | eat | drink | block | bow | spear | crossbow | spyglass | toot_horn | brush | bundle
set {_c::3} to "sound:minecraft:entity.generic.drink"
set {_c::4} to "particles:false"
set data component "minecraft:consumable" of {_item} to {_c::*}
```

> Żeby spożycie przywracało głód, ustaw też `minecraft:food`.

### Wyposażenie i mechanika

```skript
# equippable: dowolny przedmiot można założyć w dowolnym slocie
set {_e::1} to "slot:head"        # head | chest | legs | feet | mainhand | offhand | body
set {_e::2} to "sound:minecraft:item.armor.equip_diamond"
set {_e::3} to "camera_overlay:minecraft:misc/pumpkinblur"   # opcjonalnie: nakładka na ekran
set {_e::4} to "asset_id:mojpack:korona"                     # opcjonalnie: model zbroi z resource packa
set {_e::5} to "swappable:true"
set {_e::6} to "dispensable:true"
set data component "minecraft:equippable" of {_item} to {_e::*}

# use_cooldown
set {_cd::1} to "time:2.0"                     # sekundy, musi być > 0
set {_cd::2} to "group:minecraft:ender_pearl"  # opcjonalna wspólna grupa cooldownu
set data component "minecraft:use_cooldown" of {_item} to {_cd::*}

# komponenty-znaczniki: true dodaje, false usuwa
set data component "minecraft:glider" of {_item} to true
```

> W `equippable`, `consumable` i `use_cooldown` jeden błędny wpis (np. `time:NaN`, zły slot) blokuje cały zapis, zamiast po cichu wstawiać wartości domyślne.

### Walka i atrybuty

```skript
# trim zbroi: "materiał:wzór"
set data component "minecraft:trim" of {_item} to "gold:coast"

# atrybuty: "atrybut:operacja:wartość[:slot]"
# operacje: add_value (add) | add_multiplied_base (multiply) | add_multiplied_total
# sloty:    any | mainhand | offhand | hand | head | chest | legs | feet | armor | body
set {_a::1} to "attack_damage:add_value:15:mainhand"
set {_a::2} to "armor:add_value:5:chest"
set {_a::3} to "movement_speed:add_multiplied_base:0.1:any"
set data component "minecraft:attribute_modifiers" of {_item} to {_a::*}
```

> Nazwę atrybutu można podać jako `attack_damage`, `generic.attack_damage` lub `minecraft:attack_damage`. Wartości `NaN` i `Infinity` są odrzucane.

### Mikstury

```skript
# "base:TYP" lub "effect:nazwa:ticki:amplifier[:ambient][:cząsteczki]"
set {_p::1} to "base:strong_swiftness"
set {_p::2} to "effect:strength:1200:2"
set {_p::3} to "effect:regeneration:600:1"
set data component "minecraft:potion_contents" of {_item} to {_p::*}
```

Ujemny czas efektu oznacza efekt nieskończony, a amplifier jest przycinany do 0–255.

Dostępne typy bazowe: `water`, `mundane`, `thick`, `awkward`, `night_vision`, `long_night_vision`, `invisibility`, `long_invisibility`, `leaping`, `strong_leaping`, `long_leaping`, `fire_resistance`, `long_fire_resistance`, `swiftness`, `strong_swiftness`, `long_swiftness`, `slowness`, `strong_slowness`, `long_slowness`, `water_breathing`, `long_water_breathing`, `healing`, `strong_healing`, `harming`, `strong_harming`, `poison`, `strong_poison`, `long_poison`, `regeneration`, `strong_regeneration`, `long_regeneration`, `strength`, `strong_strength`, `long_strength`, `weakness`, `long_weakness`, `luck`, `slow_falling`, `long_slow_falling`, `wind_charged`, `weaving`, `oozing`, `infested`

### Książki

```skript
set {_b::1} to "title:Moja Książka"
set {_b::2} to "author:Steve"
set {_b::3} to "page:Treść pierwszej strony"
set {_b::4} to "page:&0Druga strona &cz kolorem"
set data component "minecraft:written_book_content" of {_item} to {_b::*}
```

> Tytuł jest przycinany do 32 znaków, a książka do 100 stron.

### Bannery i dekoracje

```skript
# wzory banneru: "wzór:kolor"
set {_bp::1} to "stripe_top:red"
set {_bp::2} to "cross:white"
set data component "minecraft:banner_patterns" of {_item} to {_bp::*}

# kolor bazowy (tarcze, bannery)
set data component "minecraft:base_color" of {_item} to "blue"

# podejrzany gulasz: "efekt:ticki"
set {_s::1} to "speed:100"
set {_s::2} to "blindness:60"
set data component "minecraft:suspicious_stew_effects" of {_item} to {_s::*}
```

### Narzędzia

```skript
# "speed:X" | "damage:X" | "rule:<bloki lub #tag>:<prędkość>:<czy_dropi>"
set {_t::1} to "speed:4.0"
set {_t::2} to "damage:1"
set {_t::3} to "rule:stone,granite,diorite:8.0:true"
set {_t::4} to "rule:#minecraft:mineable/pickaxe:6.0:true"
set data component "minecraft:tool" of {_item} to {_t::*}

# kopiowanie reguł z przedmiotu vanilla
set data component "minecraft:tool" of {_item} to data component values "minecraft:tool" of diamond pickaxe
```

> Prędkość w regule musi być większa od 0. Prędkość `default` oznacza domyślną prędkość narzędzia, a trzecie pole `default` nie zmienia dropów.

### Muzyka i inne

```skript
# Ominous Bottle, poziom 0–4 (I–V)
set data component "minecraft:ominous_bottle_amplifier" of {_item} to 4

# Róg kozy: ponder_goat_horn | sing_goat_horn | seek_goat_horn | feel_goat_horn
#           admire_goat_horn | call_goat_horn | yearn_goat_horn | dream_goat_horn
set data component "minecraft:instrument" of {_item} to "dream_goat_horn"

# Szafa grająca: 13 | cat | blocks | chirp | far | mall | mellohi | stal | strad | ward
#                11 | wait | otherside | 5 | pigstep | relic | precipice | creator | tears ...
set data component "minecraft:jukebox_playable" of {_item} to "minecraft:pigstep"
```

---

## Komponenty encji

| Komponent | Typ | Wymaga |
|---|---|---|
| `minecraft:custom_name` | tekst | dowolna encja |
| `minecraft:custom_name_visible` | `true` / `false` | dowolna encja |
| `minecraft:is_silent` | `true` / `false` | dowolna encja |
| `minecraft:has_gravity` | `true` / `false` | dowolna encja |
| `minecraft:is_invulnerable` | `true` / `false` | dowolna encja |
| `minecraft:is_glowing` | `true` / `false` | dowolna encja |
| `minecraft:freeze_ticks` | liczba | dowolna encja |
| `minecraft:fire_ticks` | liczba | dowolna encja |
| `minecraft:health` | liczba | LivingEntity |
| `minecraft:max_health` | liczba | LivingEntity |
| `minecraft:attack_damage` | liczba | encja z tym atrybutem |
| `minecraft:armor` | liczba | LivingEntity |
| `minecraft:armor_toughness` | liczba | LivingEntity |
| `minecraft:movement_speed` | liczba | LivingEntity |
| `minecraft:follow_range` | liczba | encja z tym atrybutem (moby) |

```skript
set entity component "minecraft:custom_name" of {_entity} to "&cBoss"
set entity component "minecraft:max_health" of {_entity} to 200
set entity component "minecraft:movement_speed" of {_entity} to 0.5
set entity component "minecraft:is_invulnerable" of {_entity} to true

set {_hp} to entity component "minecraft:health" of {_entity}
if {_entity} has entity component "minecraft:custom_name":
```

> `reset` przywraca wartość domyślną dla danego typu moba (np. 100 HP golema), a nie jedną wspólną dla wszystkich. Wartości `NaN` i `Infinity` są odrzucane.

---

## Pełny przykład

```skript
command /mrocznymiecz:
    trigger:
        set {_item} to diamond sword

        set data component "minecraft:custom_name" of {_item} to "&4&lMroczny Miecz"

        set {_lore::1} to "&8Wykuty w cieniu"
        set {_lore::2} to "&8&oLegendarna broń"
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
        send "&aOtrzymujesz &4&lMroczny Miecz&a!" to player

on data component change:
    component name is "minecraft:enchantments"
    event-player is set
    send "&eEnchanty: &7%old component value% &e-> &7%new component value%" to event-player
```

---

## Ważne: bezpieczeństwo przedmiotów

- **Wartości spoza limitów gry są przycinane albo odrzucane**: poziom enchantu 1–255, amplifier efektu 0–255, maks. 256 linii lore, maks. 100 stron i tytuł do 32 znaków w książce. Przedmiotu z wartością spoza limitów serwer nie potrafiłby zapisać.
- **Błędne dane nie zmieniają przedmiotu.** W listach (enchanty, atrybuty, mikstury) błędne wpisy są pomijane, a jeśli żaden nie jest poprawny, komponent zostaje bez zmian. W `equippable`, `consumable` i `use_cooldown` jeden błędny wpis blokuje zapis.
- **Tekst od gracza jest bezpieczny do przekazania.** Zbyt długi tekst albo zbyt długa lista zostaną odrzucone zgodnie z limitami z `config.yml`.
- Każde odrzucenie kończy się czytelnym ostrzeżeniem w konsoli. Powtarzające się ostrzeżenia są tłumione (chyba że `debug: true`).

---

## Data Components a NBT

Od Minecrafta 1.20.5 NBT przedmiotów zastąpił system **Data Components**. HexVG-DataComponents korzysta wyłącznie z nowego API.

| | NBT (przed 1.20.5) | Data Components |
|---|---|---|
| Bezpieczeństwo typów | brak | ścisłe typy |
| Styl API | operacje na tekście | obiekty |
| Walidacja | ręczna | wbudowana |
| Przyszłość | przestarzałe | aktywnie rozwijane |

---

## Przykładowe skrypty

W repozytorium znajduje się skrypt `testy/hexvg-dc-test.sk`:

- `/dctest`: 49 automatycznych testów (zapis i odczyt, limity, eventy, encje) z wynikiem PASS/FAIL
- `/dcgive`: cztery przedmioty do ręcznego sprawdzenia `tool`, `equippable`, `consumable` i `use_cooldown` w grze, z opisem działania w lore

Obie komendy są tylko dla operatorów. Na serwerze produkcyjnym usuń albo wyłącz ten skrypt.

---

## Autorzy

Stworzony dla serwera **VenomGrave** przez HexVG Team.  
Błędy i propozycje: https://github.com/VenomGrave/HexVG-DataComponents/issues
