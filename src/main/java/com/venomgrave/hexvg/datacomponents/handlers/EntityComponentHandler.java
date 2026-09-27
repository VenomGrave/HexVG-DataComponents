package com.venomgrave.hexvg.datacomponents.handlers;

import com.venomgrave.hexvg.datacomponents.utils.ComponentConverter;
import com.venomgrave.hexvg.datacomponents.utils.ComponentLogger;
import net.kyori.adventure.text.Component;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("UnstableApiUsage")
public final class EntityComponentHandler {

    public static final List<String> SUPPORTED_COMPONENTS = List.of(
            "minecraft:custom_name", "minecraft:custom_name_visible", "minecraft:health", "minecraft:max_health",
            "minecraft:is_silent", "minecraft:has_gravity", "minecraft:is_invulnerable", "minecraft:is_glowing",
            "minecraft:freeze_ticks", "minecraft:fire_ticks", "minecraft:movement_speed", "minecraft:follow_range",
            "minecraft:attack_damage", "minecraft:armor", "minecraft:armor_toughness"
    );

    private EntityComponentHandler() {}

    public static Optional<Object> read(Entity entity, String componentName) {
        if (entity == null || componentName == null) return Optional.empty();

        return switch (componentName) {
            case "minecraft:custom_name" -> {
                Component name = entity.customName();
                yield name != null ? Optional.of(ComponentConverter.toColoredString(name)) : Optional.empty();
            }
            case "minecraft:health" -> entity instanceof LivingEntity le ? Optional.of(le.getHealth()) : Optional.empty();
            case "minecraft:is_silent"           -> Optional.of(entity.isSilent());
            case "minecraft:has_gravity"         -> Optional.of(entity.hasGravity());
            case "minecraft:is_invulnerable"     -> Optional.of(entity.isInvulnerable());
            case "minecraft:custom_name_visible" -> Optional.of(entity.isCustomNameVisible());
            case "minecraft:is_glowing"          -> Optional.of(entity.isGlowing());
            case "minecraft:freeze_ticks"        -> Optional.of(entity.getFreezeTicks());
            case "minecraft:fire_ticks"          -> Optional.of(entity.getFireTicks());
            default -> {
                Attribute attribute = attributeFor(componentName);
                if (attribute != null) {
                    AttributeInstance attr = getAttribute(entity, attribute);
                    yield attr != null ? Optional.of(attr.getValue()) : Optional.empty();
                }
                ComponentLogger.warn("Nieznany komponent encji: '" + componentName + "'");
                yield Optional.empty();
            }
        };
    }

    public static void write(Entity entity, String componentName, Object value) {
        if (entity == null || componentName == null || value == null) return;

        switch (componentName) {
            case "minecraft:custom_name" -> {
                Optional<Object> converted = ComponentConverter.toComponentValue(componentName, value);
                if (converted.isEmpty()) return;
                entity.customName(ComponentConverter.parseComponent(converted.get().toString()));
                entity.setCustomNameVisible(true);
            }
            case "minecraft:health" -> {
                if (!(entity instanceof LivingEntity le)) {
                    ComponentLogger.warn("minecraft:health wymaga LivingEntity."); return;
                }
                Double hp = toFiniteDouble(value, componentName);
                if (hp == null) return;
                AttributeInstance maxAttr = le.getAttribute(Attribute.MAX_HEALTH);
                double max = maxAttr != null ? maxAttr.getValue() : 20.0;
                le.setHealth(Math.max(0.0, Math.min(max, hp)));
            }
            case "minecraft:is_silent"           -> entity.setSilent(toBool(value));
            case "minecraft:has_gravity"         -> entity.setGravity(toBool(value));
            case "minecraft:is_invulnerable"     -> entity.setInvulnerable(toBool(value));
            case "minecraft:custom_name_visible" -> entity.setCustomNameVisible(toBool(value));
            case "minecraft:is_glowing"          -> entity.setGlowing(toBool(value));
            case "minecraft:freeze_ticks" -> {
                Double ticks = toFiniteDouble(value, componentName);
                if (ticks != null) entity.setFreezeTicks(clampToInt(ticks, 0));
            }
            case "minecraft:fire_ticks" -> {
                Double ticks = toFiniteDouble(value, componentName);
                if (ticks != null) entity.setFireTicks(clampToInt(ticks, -1));
            }
            default -> {
                Attribute attribute = attributeFor(componentName);
                if (attribute == null) {
                    ComponentLogger.warn("Nieznany komponent encji: '" + componentName + "'");
                    return;
                }
                Double amount = toFiniteDouble(value, componentName);
                if (amount == null) return;
                AttributeInstance attr = getAttribute(entity, attribute);
                if (attr == null) {
                    ComponentLogger.warn("Encja " + entity.getType().key().asString()
                            + " nie posiada atrybutu " + componentName + ".");
                    return;
                }
                if (attribute == Attribute.MAX_HEALTH) amount = Math.max(0.1, amount);
                attr.setBaseValue(amount);
                clampHealth(entity);
            }
        }
    }

    public static void remove(Entity entity, String componentName) {
        if (entity == null || componentName == null) return;
        switch (componentName) {
            case "minecraft:custom_name" -> {
                entity.customName(null);
                entity.setCustomNameVisible(false);
            }
            case "minecraft:is_silent"           -> entity.setSilent(false);
            case "minecraft:has_gravity"         -> entity.setGravity(true);
            case "minecraft:is_invulnerable"     -> entity.setInvulnerable(false);
            case "minecraft:custom_name_visible" -> entity.setCustomNameVisible(false);
            case "minecraft:is_glowing"          -> entity.setGlowing(false);
            case "minecraft:freeze_ticks"        -> entity.setFreezeTicks(0);
            case "minecraft:fire_ticks"          -> entity.setFireTicks(-1);
            case "minecraft:health" -> {
                if (entity instanceof LivingEntity le) {
                    AttributeInstance maxAttr = le.getAttribute(Attribute.MAX_HEALTH);
                    if (maxAttr != null) le.setHealth(maxAttr.getValue());
                }
            }
            default -> {
                Attribute attribute = attributeFor(componentName);
                if (attribute == null) {
                    ComponentLogger.warn("Nie mozna zresetowac komponentu encji: '" + componentName + "'");
                    return;
                }
                AttributeInstance attr = getAttribute(entity, attribute);
                if (attr == null) return;
                // Wartosc domyslna dla danego typu encji (np. 100 HP golema, 0.1 predkosci gracza),
                // a nie stala wspolna dla wszystkich mobow.
                attr.setBaseValue(attr.getDefaultValue());
                clampHealth(entity);
            }
        }
    }

    // ------------------------------------------------------------------

    private static @Nullable Attribute attributeFor(String componentName) {
        return switch (componentName) {
            case "minecraft:max_health"      -> Attribute.MAX_HEALTH;
            case "minecraft:movement_speed"  -> Attribute.MOVEMENT_SPEED;
            case "minecraft:follow_range"    -> Attribute.FOLLOW_RANGE;
            case "minecraft:attack_damage"   -> Attribute.ATTACK_DAMAGE;
            case "minecraft:armor"           -> Attribute.ARMOR;
            case "minecraft:armor_toughness" -> Attribute.ARMOR_TOUGHNESS;
            default -> null;
        };
    }

    private static @Nullable AttributeInstance getAttribute(Entity entity, Attribute attribute) {
        return entity instanceof LivingEntity le ? le.getAttribute(attribute) : null;
    }

    /** Po zmianie max_health aktualne HP nie moze przekraczac nowego maksimum. */
    private static void clampHealth(Entity entity) {
        if (!(entity instanceof LivingEntity le)) return;
        AttributeInstance maxAttr = le.getAttribute(Attribute.MAX_HEALTH);
        if (maxAttr != null && le.getHealth() > maxAttr.getValue()) le.setHealth(maxAttr.getValue());
    }

    /** Odrzuca tekst niebedacy liczba oraz NaN/nieskonczonosc (np. "NaN" jako max_health psuje encje). */
    private static @Nullable Double toFiniteDouble(Object value, String context) {
        double d;
        if (value instanceof Number n) {
            d = n.doubleValue();
        } else {
            try {
                d = Double.parseDouble(value.toString().trim());
            } catch (NumberFormatException e) {
                ComponentLogger.warn("Nieprawidlowa liczba dla " + context + ": " + value);
                return null;
            }
        }
        if (!Double.isFinite(d)) {
            ComponentLogger.warn("Wartosc " + context + " musi byc skonczona liczba: " + value);
            return null;
        }
        return d;
    }

    private static int clampToInt(double value, int min) {
        return (int) Math.max(min, Math.min(Integer.MAX_VALUE, value));
    }

    private static boolean toBool(Object value) {
        if (value instanceof Boolean b) return b;
        if (value instanceof Number n) return n.doubleValue() != 0;
        String str = value.toString().trim();
        return str.equalsIgnoreCase("true") || str.equalsIgnoreCase("yes") || str.equals("1");
    }
}
