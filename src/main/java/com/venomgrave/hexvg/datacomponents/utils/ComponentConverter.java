package com.venomgrave.hexvg.datacomponents.utils;

import io.papermc.paper.datacomponent.item.BannerPatternLayers;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.CustomModelData;
import io.papermc.paper.datacomponent.item.DyedItemColor;
import io.papermc.paper.datacomponent.item.Equippable;
import io.papermc.paper.datacomponent.item.FoodProperties;
import io.papermc.paper.datacomponent.item.ItemArmorTrim;
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers;
import io.papermc.paper.datacomponent.item.ItemEnchantments;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.JukeboxPlayable;
import io.papermc.paper.datacomponent.item.OminousBottleAmplifier;
import io.papermc.paper.datacomponent.item.PotionContents;
import io.papermc.paper.datacomponent.item.SuspiciousStewEffects;
import io.papermc.paper.datacomponent.item.Tool;
import io.papermc.paper.datacomponent.item.UseCooldown;
import io.papermc.paper.registry.set.RegistryKeySet;
import io.papermc.paper.registry.tag.Tag;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Color;
import net.kyori.adventure.util.TriState;
import org.bukkit.block.banner.Pattern;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.potion.PotionEffect;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("UnstableApiUsage")
public final class ComponentConverter {

    // &a, &l itd. oraz kolory hex w formacie &#RRGGBB
    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.builder().character('&').hexColors().build();
    private static final java.util.regex.Pattern NAMESPACE = java.util.regex.Pattern.compile("[a-z0-9_.-]+");
    private static final java.util.regex.Pattern PATH = java.util.regex.Pattern.compile("[a-z0-9_./-]+");
    private static final int MAX_DEPTH = 16;

    public static final int DEFAULT_MAX_STRING_LENGTH = 8192;
    public static final int DEFAULT_MAX_LIST_SIZE = 256;

    // Limity chronia przed tworzeniem gigantycznych przedmiotow (tzw. "chunk ban"/"book ban"),
    // gdy skrypt przekazuje do komponentu dane wpisane przez gracza.
    private static volatile int maxStringLength = DEFAULT_MAX_STRING_LENGTH;
    private static volatile int maxListSize = DEFAULT_MAX_LIST_SIZE;

    private ComponentConverter() {}

    public static void configureLimits(int maxString, int maxList) {
        maxStringLength = maxString > 0 ? maxString : DEFAULT_MAX_STRING_LENGTH;
        maxListSize = maxList > 0 ? maxList : DEFAULT_MAX_LIST_SIZE;
    }

    public static int getMaxListSize() { return maxListSize; }

    public static boolean isValidComponentName(String name) {
        if (name == null || name.isEmpty() || name.length() > 128) return false;
        int colon = name.indexOf(':');
        if (colon <= 0 || colon == name.length() - 1) return false;
        return NAMESPACE.matcher(name.substring(0, colon)).matches()
                && PATH.matcher(name.substring(colon + 1)).matches();
    }

    public static Optional<Object> toComponentValue(String componentName, Object skriptValue) {
        if (skriptValue == null) return Optional.empty();
        try {
            return convertValue(componentName, skriptValue, 0);
        } catch (Exception e) {
            ComponentLogger.warn("Blad konwersji dla '" + componentName + "': " + e.getMessage());
            return Optional.empty();
        }
    }

    private static Optional<Object> convertValue(String componentName, Object value, int depth) {
        if (depth > MAX_DEPTH) return Optional.empty();

        if (value instanceof Collection<?> list) {
            if (list.size() > maxListSize) {
                ComponentLogger.warn("Odrzucono '" + componentName + "': lista ma " + list.size()
                        + " elementow (limit " + maxListSize + ").");
                return Optional.empty();
            }
            List<Object> result = new ArrayList<>(list.size());
            for (Object item : list) {
                if (item == null) continue;
                Optional<Object> converted = convertValue(componentName, item, depth + 1);
                if (converted.isEmpty()) return Optional.empty();
                result.add(converted.get());
            }
            return Optional.of(result);
        }

        if (value instanceof Number || value instanceof Boolean) return Optional.of(value);

        String str = value.toString();
        if (str.length() > maxStringLength) {
            ComponentLogger.warn("Odrzucono '" + componentName + "': tekst ma " + str.length()
                    + " znakow (limit " + maxStringLength + ").");
            return Optional.empty();
        }
        return Optional.of(str);
    }

    public static Component parseComponent(String input) {
        if (input == null || input.isEmpty()) return Component.empty();
        return LEGACY.deserialize(input);
    }

    public static List<Component> parseComponentList(List<String> lines) {
        return lines.stream().map(ComponentConverter::parseComponent).toList();
    }

    /**
     * Zamienia wartosc komponentu Papera na wartosc zrozumiala dla Skriptu
     * (String / Number / Boolean / List). Format odczytu jest zgodny z formatem zapisu,
     * dzieki czemu odczytana wartosc mozna bezposrednio ustawic na innym przedmiocie.
     */
    public static Object toSkriptValue(Object componentValue) {
        if (componentValue == null) return null;

        if (componentValue instanceof String || componentValue instanceof Number || componentValue instanceof Boolean) {
            return componentValue;
        }

        if (componentValue instanceof Component comp) return LEGACY.serialize(comp);

        if (componentValue instanceof ItemLore lore) {
            List<Object> result = new ArrayList<>(lore.lines().size());
            for (Component line : lore.lines()) result.add(LEGACY.serialize(line));
            return result;
        }

        if (componentValue instanceof ItemEnchantments enchants) {
            List<Object> result = new ArrayList<>();
            enchants.enchantments().forEach((enchant, level) -> result.add(enchant.key().asString() + ":" + level));
            return result;
        }

        if (componentValue instanceof CustomModelData cmd) {
            List<Float> floats = cmd.floats();
            return floats.isEmpty() ? 0 : floats.get(0).intValue();
        }

        if (componentValue instanceof ItemAttributeModifiers mods) {
            List<Object> result = new ArrayList<>();
            for (ItemAttributeModifiers.Entry entry : mods.modifiers()) {
                String attr = entry.attribute().key().asString();
                org.bukkit.attribute.AttributeModifier mod = entry.modifier();
                String op = switch (mod.getOperation()) {
                    case ADD_NUMBER -> "add_value";
                    case ADD_SCALAR -> "add_multiplied_base";
                    case MULTIPLY_SCALAR_1 -> "add_multiplied_total";
                };
                String slot = mod.getSlotGroup().toString().toLowerCase(Locale.ROOT);
                result.add(attr + ":" + op + ":" + mod.getAmount() + ":" + slot);
            }
            return result;
        }

        if (componentValue instanceof FoodProperties food) {
            return food.nutrition() + ":" + food.saturation() + ":" + food.canAlwaysEat();
        }

        if (componentValue instanceof DyedItemColor dyed) return toHex(dyed.color());

        if (componentValue instanceof OminousBottleAmplifier oba) return oba.amplifier();

        if (componentValue instanceof JukeboxPlayable jp) return jp.jukeboxSong().key().asString();

        if (componentValue instanceof ItemArmorTrim trim) {
            ArmorTrim armorTrim = trim.armorTrim();
            return armorTrim.getMaterial().key().asString() + ":" + armorTrim.getPattern().key().asString();
        }

        if (componentValue instanceof PotionContents potion) {
            List<Object> result = new ArrayList<>();
            if (potion.potion() != null) result.add("base:" + potion.potion().key().asString());
            for (PotionEffect effect : potion.customEffects()) {
                result.add("effect:" + effect.getType().key().asString() + ":" + effect.getDuration() + ":"
                        + effect.getAmplifier() + ":" + effect.isAmbient() + ":" + effect.hasParticles());
            }
            return result;
        }

        if (componentValue instanceof BannerPatternLayers banner) {
            List<Object> result = new ArrayList<>();
            for (Pattern pattern : banner.patterns()) {
                result.add(pattern.getPattern().key().asString() + ":" + pattern.getColor().name().toLowerCase(Locale.ROOT));
            }
            return result;
        }

        if (componentValue instanceof SuspiciousStewEffects stew) {
            List<Object> result = new ArrayList<>();
            stew.effects().forEach(e -> result.add(e.effect().key().asString() + ":" + e.duration()));
            return result;
        }

        if (componentValue instanceof UseCooldown cooldown) {
            List<Object> result = new ArrayList<>();
            result.add("time:" + cooldown.seconds());
            if (cooldown.cooldownGroup() != null) result.add("group:" + cooldown.cooldownGroup().asString());
            return result;
        }

        if (componentValue instanceof Tool tool) {
            List<Object> result = new ArrayList<>();
            result.add("speed:" + tool.defaultMiningSpeed());
            result.add("damage:" + tool.damagePerBlock());
            for (Tool.Rule rule : tool.rules()) {
                result.add("rule:" + blocksToString(rule.blocks()) + ":"
                        + (rule.speed() == null ? "default" : rule.speed()) + ":"
                        + triStateToString(rule.correctForDrops()));
            }
            return result;
        }

        if (componentValue instanceof Equippable equippable) {
            List<Object> result = new ArrayList<>();
            result.add("slot:" + slotToString(equippable.slot()));
            result.add("sound:" + equippable.equipSound().asString());
            if (equippable.assetId() != null) result.add("asset_id:" + equippable.assetId().asString());
            if (equippable.cameraOverlay() != null) result.add("camera_overlay:" + equippable.cameraOverlay().asString());
            result.add("swappable:" + equippable.swappable());
            result.add("dispensable:" + equippable.dispensable());
            return result;
        }

        if (componentValue instanceof Consumable consumable) {
            List<Object> result = new ArrayList<>();
            result.add("time:" + consumable.consumeSeconds());
            result.add("animation:" + consumable.animation().name().toLowerCase(Locale.ROOT));
            result.add("sound:" + consumable.sound().asString());
            result.add("particles:" + consumable.hasConsumeParticles());
            return result;
        }

        if (componentValue instanceof Color color) return toHex(color);

        if (componentValue instanceof Key key) return key.asString();

        if (componentValue instanceof net.kyori.adventure.key.Keyed keyed) {
            try {
                return keyed.key().asString();
            } catch (Exception ignored) {
                // niektore wartosci rejestrowe (np. inline) nie maja klucza
            }
        }

        if (componentValue instanceof Enum<?> e) return e.name().toLowerCase(Locale.ROOT);

        if (componentValue instanceof Collection<?> list) {
            List<Object> result = new ArrayList<>(list.size());
            for (Object item : list) result.add(toSkriptValue(item));
            return result;
        }

        if (componentValue instanceof Map<?, ?> map) {
            List<Object> result = new ArrayList<>(map.size());
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                result.add(toSkriptValue(entry.getKey()) + ":" + toSkriptValue(entry.getValue()));
            }
            return result;
        }

        // Nieznane obiekty Papera zamieniamy na tekst - Skript nie potrafi ich zapisac w zmiennych.
        return componentValue.toString();
    }

    /** Wartosc dla wyrazen pojedynczych: listy sa laczone w jeden tekst "[a, b]". */
    public static Object toSingleValue(Object skriptValue) {
        return skriptValue instanceof List<?> ? toDisplayString(skriptValue) : skriptValue;
    }

    /** Wartosc dla wyrazen mnogich: listy sa rozwijane na osobne elementy. */
    public static Object[] toValueArray(Object skriptValue) {
        if (skriptValue == null) return new Object[0];
        if (skriptValue instanceof List<?> list) return list.stream().filter(Objects::nonNull).toArray();
        return new Object[]{skriptValue};
    }

    public static String toColoredString(Component component) {
        if (component == null) return "";
        return LEGACY.serialize(component);
    }

    public static String toDisplayString(Object value) {
        if (value == null) return "null";
        if (value instanceof Component comp) return LEGACY.serialize(comp);
        if (value instanceof List<?> list) {
            return "[" + list.stream().map(ComponentConverter::toDisplayString).collect(Collectors.joining(", ")) + "]";
        }
        return value.toString();
    }

    /** Tag zapisujemy jako "#namespace:sciezka", liste blokow jako "blok1,blok2". */
    private static String blocksToString(RegistryKeySet<?> blocks) {
        if (blocks instanceof Tag<?> tag) return "#" + tag.tagKey().key().asString();
        return blocks.values().stream().map(k -> k.key().asString()).collect(Collectors.joining(","));
    }

    private static String triStateToString(TriState state) {
        return switch (state) {
            case TRUE -> "true";
            case FALSE -> "false";
            case NOT_SET -> "default";
        };
    }

    private static String slotToString(EquipmentSlot slot) {
        return switch (slot) {
            case HAND -> "mainhand";
            case OFF_HAND -> "offhand";
            default -> slot.name().toLowerCase(Locale.ROOT);
        };
    }

    private static String toHex(Color c) {
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }
}
