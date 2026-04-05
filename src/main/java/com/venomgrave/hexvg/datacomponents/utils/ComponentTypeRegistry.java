package com.venomgrave.hexvg.datacomponents.utils;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;

import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Logger;

@SuppressWarnings("UnstableApiUsage")
public final class ComponentTypeRegistry {

    private static final Logger LOGGER = Logger.getLogger("HexVG-DataComponents");
    private static final Map<String, DataComponentType> TYPE_CACHE = new HashMap<>();
    private static final Map<String, DataComponentType.Valued<?>> VALUED_CACHE = new HashMap<>();
    private static boolean initialized = false;

    private ComponentTypeRegistry() {}

    public static void initialize() {
        if (initialized) return;

        for (Field field : DataComponentTypes.class.getDeclaredFields()) {
            try {
                if (!DataComponentType.class.isAssignableFrom(field.getType())) continue;
                field.setAccessible(true);
                DataComponentType type = (DataComponentType) field.get(null);
                if (type == null) continue;
                String key = type.key().asString();
                TYPE_CACHE.put(key, type);
                if (type instanceof DataComponentType.Valued<?> valued) {
                    VALUED_CACHE.put(key, valued);
                }
            } catch (Exception e) {
                LOGGER.fine("[HexVG-DC] Nie mozna zaladowac pola: " + field.getName());
            }
        }

        LOGGER.info("[HexVG-DC] Zaladowano " + TYPE_CACHE.size() + " typow komponentow (" + VALUED_CACHE.size() + " valued).");
        initialized = true;
    }

    public static Optional<DataComponentType> getType(String name) {
        if (!initialized) initialize();
        return Optional.ofNullable(TYPE_CACHE.get(name));
    }

    public static Optional<DataComponentType.Valued<?>> getValuedType(String name) {
        if (!initialized) initialize();
        return Optional.ofNullable(VALUED_CACHE.get(name));
    }

    public static boolean isKnown(String name) {
        if (!initialized) initialize();
        return TYPE_CACHE.containsKey(name);
    }

    public static List<String> getAllComponentNames() {
        if (!initialized) initialize();
        return TYPE_CACHE.keySet().stream().sorted().toList();
    }
}
