package com.venomgrave.hexvg.datacomponents.utils;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

@SuppressWarnings("UnstableApiUsage")
public final class ComponentTypeRegistry {

    // Mapy sa budowane raz i publikowane jako niemodyfikowalne - bezpieczne przy odczycie z wielu watkow.
    private static volatile Map<String, DataComponentType> typeCache = Map.of();
    private static volatile Map<String, DataComponentType.Valued<?>> valuedCache = Map.of();
    private static volatile List<String> sortedNames = List.of();
    private static volatile boolean initialized = false;

    private ComponentTypeRegistry() {}

    public static synchronized void initialize() {
        if (initialized) return;

        Map<String, DataComponentType> types = new HashMap<>();
        Map<String, DataComponentType.Valued<?>> valued = new HashMap<>();

        for (Field field : DataComponentTypes.class.getFields()) {
            if (!Modifier.isStatic(field.getModifiers())) continue;
            if (!DataComponentType.class.isAssignableFrom(field.getType())) continue;
            try {
                DataComponentType type = (DataComponentType) field.get(null);
                if (type == null) continue;
                String key = type.key().asString();
                types.put(key, type);
                if (type instanceof DataComponentType.Valued<?> v) valued.put(key, v);
            } catch (Exception | LinkageError e) {
                ComponentLogger.debug("Nie mozna zaladowac pola: " + field.getName());
            }
        }

        typeCache = Collections.unmodifiableMap(types);
        valuedCache = Collections.unmodifiableMap(valued);
        sortedNames = types.keySet().stream().sorted().toList();
        initialized = true;

        ComponentLogger.info("Zaladowano " + types.size() + " typow komponentow (" + valued.size() + " valued).");
    }

    private static void ensureInitialized() {
        if (!initialized) initialize();
    }

    public static Optional<DataComponentType> getType(String name) {
        ensureInitialized();
        return name == null ? Optional.empty() : Optional.ofNullable(typeCache.get(name));
    }

    public static Optional<DataComponentType.Valued<?>> getValuedType(String name) {
        ensureInitialized();
        return name == null ? Optional.empty() : Optional.ofNullable(valuedCache.get(name));
    }

    public static boolean isKnown(String name) {
        ensureInitialized();
        return name != null && typeCache.containsKey(name);
    }

    public static int size() {
        ensureInitialized();
        return typeCache.size();
    }

    public static List<String> getAllComponentNames() {
        ensureInitialized();
        return sortedNames;
    }
}
