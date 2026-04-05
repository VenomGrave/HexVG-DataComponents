package com.venomgrave.hexvg.datacomponents.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.*;
import java.util.logging.Logger;

public final class ComponentConverter {

    private static final Logger LOGGER = Logger.getLogger("HexVG-DataComponents");
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final int MAX_DEPTH = 16;
    private ComponentConverter() {}

    public static boolean isValidComponentName(String name) {
        if (name == null || name.isEmpty() || name.length() > 128) return false;
        String[] parts = name.split(":", 2);
        if (parts.length != 2) return false;
        return parts[0].matches("[a-z0-9_.-]+") && parts[1].matches("[a-z0-9_./-]+");
    }

    public static Optional<Object> toComponentValue(String componentName, Object skriptValue) {
        if (skriptValue == null) return Optional.empty();
        try {
            return convertValue(skriptValue, 0);
        } catch (Exception e) {
            LOGGER.warning("[HexVG-DC] Blad konwersji dla '" + componentName + "': " + e.getMessage());
            return Optional.empty();
        }
    }

    private static Optional<Object> convertValue(Object value, int depth) {
        if (depth > MAX_DEPTH) return Optional.empty();

        if (value instanceof List<?> list) {
            List<Object> result = new ArrayList<>();
            for (Object item : list) {
                Optional<Object> converted = convertValue(item, depth + 1);
                if (converted.isEmpty()) return Optional.empty();
                result.add(converted.get());
            }
            return Optional.of(result);
        }

        if (value instanceof Number || value instanceof Boolean) return Optional.of(value);

        return Optional.of(value.toString());
    }

    public static Component parseComponent(String input) {
        if (input == null) return Component.empty();
        return LEGACY.deserialize(input);
    }

    public static List<Component> parseComponentList(List<String> lines) {
        return lines.stream().map(ComponentConverter::parseComponent).toList();
    }

    public static Object toSkriptValue(Object componentValue) {
        if (componentValue == null) return null;

        if (componentValue instanceof Component comp) {
            return LEGACY.serialize(comp);
        }

        if (componentValue instanceof io.papermc.paper.datacomponent.item.ItemLore lore) {
            List<Object> result = new ArrayList<>();
            for (Component line : lore.lines()) result.add(LEGACY.serialize(line));
            return result.isEmpty() ? "(puste lore)" : result;
        }

        if (componentValue instanceof io.papermc.paper.datacomponent.item.ItemEnchantments enchants) {
            List<Object> result = new ArrayList<>();
            enchants.enchantments().forEach((enchant, level) ->
                result.add(enchant.key().asString() + ":" + level));
            return result.isEmpty() ? "(brak enchantow)" : result;
        }

        if (componentValue instanceof io.papermc.paper.datacomponent.item.CustomModelData cmd) {
            List<Float> floats = cmd.floats();
            return floats.isEmpty() ? 0 : floats.get(0).intValue();
        }

        if (componentValue instanceof io.papermc.paper.datacomponent.item.ItemAttributeModifiers mods) {
            List<Object> result = new ArrayList<>();
            for (io.papermc.paper.datacomponent.item.ItemAttributeModifiers.Entry entry : mods.modifiers()) {
                String attr = entry.attribute().getKey().asString();
                org.bukkit.attribute.AttributeModifier mod = entry.modifier();
                String op = switch (mod.getOperation()) {
                    case ADD_NUMBER -> "add_value";
                    case ADD_SCALAR -> "add_multiplied_base";
                    case MULTIPLY_SCALAR_1 -> "add_multiplied_total";
                };
                double val = mod.getAmount();
                String slot = mod.getSlotGroup().toString().toLowerCase();
                result.add(attr + ":" + op + ":" + val + ":" + slot);
            }
            return result.isEmpty() ? "(brak modyfikatorow)" : result;
        }
        if (componentValue instanceof io.papermc.paper.datacomponent.item.FoodProperties food) {
            return food.nutrition() + ":" + food.saturation() + ":" + food.canAlwaysEat();
        }

        if (componentValue instanceof io.papermc.paper.datacomponent.item.DyedItemColor dyed) {
            org.bukkit.Color c = dyed.color();
            return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
        }

        if (componentValue instanceof io.papermc.paper.datacomponent.item.OminousBottleAmplifier oba) {
            return oba.amplifier();
        }

        if (componentValue instanceof io.papermc.paper.datacomponent.item.JukeboxPlayable jp) {
            try {
                
                for (String methodName : new String[]{"jukeboxSong", "song", "getSong"}) {
                    try {
                        java.lang.reflect.Method m = jp.getClass().getMethod(methodName);
                        Object songObj = m.invoke(jp);
                        if (songObj instanceof org.bukkit.JukeboxSong song) return song.getKey().asString();
                        if (songObj != null) return songObj.toString();
                    } catch (NoSuchMethodException ignored) {}
                }
            } catch (Exception ignored) {}
            return jp.toString();
        }

        if (componentValue instanceof Integer i) {
            return i;
        }

        if (componentValue instanceof List<?> list) {
            List<Object> result = new ArrayList<>();
            for (Object item : list) result.add(toSkriptValue(item));
            return result;
        }

        if (componentValue instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                result.put(String.valueOf(entry.getKey()), toSkriptValue(entry.getValue()));
            }
            return result;
        }

        return componentValue;
    }

    public static String toColoredString(Component component) {
        if (component == null) return "";
        return LEGACY.serialize(component);
    }

    public static String toDisplayString(Object value) {
        if (value == null) return "null";
        if (value instanceof Component comp) return LEGACY.serialize(comp);
        if (value instanceof List<?> list) {
            return "[" + list.stream().map(ComponentConverter::toDisplayString).collect(java.util.stream.Collectors.joining(", ")) + "]";
        }
        return value.toString();
    }
}
