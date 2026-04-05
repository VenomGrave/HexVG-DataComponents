package com.venomgrave.hexvg.datacomponents.handlers;

import com.venomgrave.hexvg.datacomponents.utils.ComponentConverter;
import net.kyori.adventure.text.Component;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

import java.util.Optional;
import java.util.logging.Logger;

@SuppressWarnings("UnstableApiUsage")
public final class EntityComponentHandler {

    private static final Logger LOGGER = Logger.getLogger("HexVG-DataComponents");

    private EntityComponentHandler() {}

    private static Optional<Object> getAttrValue(Entity entity, Attribute attribute) {
        if (!(entity instanceof LivingEntity le)) return Optional.empty();
        AttributeInstance attr = le.getAttribute(attribute);
        return attr != null ? Optional.of(attr.getValue()) : Optional.empty();
    }

    public static Optional<Object> read(Entity entity, String componentName) {
        if (entity == null) return Optional.empty();

        return switch (componentName) {
            case "minecraft:custom_name" -> {
                Component name = entity.customName();
                yield name != null ? Optional.of(ComponentConverter.toColoredString(name)) : Optional.empty();
            }
            case "minecraft:max_health" -> getAttrValue(entity, Attribute.MAX_HEALTH);
            case "minecraft:health" -> {
                if (entity instanceof LivingEntity le) yield Optional.of(le.getHealth());
                yield Optional.empty();
            }
            case "minecraft:is_silent"           -> Optional.of(entity.isSilent());
            case "minecraft:has_gravity"         -> Optional.of(entity.hasGravity());
            case "minecraft:is_invulnerable"     -> Optional.of(entity.isInvulnerable());
            case "minecraft:custom_name_visible" -> Optional.of(entity.isCustomNameVisible());
            case "minecraft:is_glowing"          -> Optional.of(entity.isGlowing());
            case "minecraft:freeze_ticks"        -> Optional.of(entity.getFreezeTicks());
            case "minecraft:fire_ticks"          -> Optional.of(entity.getFireTicks());
            case "minecraft:movement_speed" -> getAttrValue(entity, Attribute.MOVEMENT_SPEED);
            case "minecraft:follow_range" -> getAttrValue(entity, Attribute.FOLLOW_RANGE);
            case "minecraft:attack_damage" -> getAttrValue(entity, Attribute.ATTACK_DAMAGE);
            case "minecraft:armor" -> getAttrValue(entity, Attribute.ARMOR);
            case "minecraft:armor_toughness" -> getAttrValue(entity, Attribute.ARMOR_TOUGHNESS);
            default -> {
                LOGGER.warning("[HexVG-DC] Nieznany komponent encji: '" + componentName + "'");
                yield Optional.empty();
            }
        };
    }

    public static void write(Entity entity, String componentName, Object value) {
        if (entity == null || value == null) return;

        switch (componentName) {
            case "minecraft:custom_name" -> {
                String str = value.toString();
                Component comp = ComponentConverter.parseComponent(str);
                entity.customName(comp);
                entity.setCustomNameVisible(true);
            }
            case "minecraft:max_health" -> {
                if (!(entity instanceof LivingEntity le)) {
                    LOGGER.warning("[HexVG-DC] minecraft:max_health wymaga LivingEntity."); return;
                }
                double hp = toDouble(value);
                AttributeInstance attr = le.getAttribute(Attribute.MAX_HEALTH);
                if (attr != null) {
                    attr.setBaseValue(Math.max(0.1, hp));
                    if (le.getHealth() > attr.getValue()) le.setHealth(attr.getValue());
                }
            }
            case "minecraft:health" -> {
                if (!(entity instanceof LivingEntity le)) {
                    LOGGER.warning("[HexVG-DC] minecraft:health wymaga LivingEntity."); return;
                }
                double hp = toDouble(value);
                AttributeInstance maxAttr = le.getAttribute(Attribute.MAX_HEALTH);
                double max = maxAttr != null ? maxAttr.getValue() : 20.0;
                le.setHealth(Math.max(0.0, Math.min(max, hp)));
            }
            case "minecraft:is_silent"           -> entity.setSilent(toBool(value));
            case "minecraft:has_gravity"         -> entity.setGravity(toBool(value));
            case "minecraft:is_invulnerable"     -> entity.setInvulnerable(toBool(value));
            case "minecraft:custom_name_visible" -> entity.setCustomNameVisible(toBool(value));
            case "minecraft:is_glowing"          -> entity.setGlowing(toBool(value));
            case "minecraft:freeze_ticks"        -> entity.setFreezeTicks(Math.max(0, toInt(value)));
            case "minecraft:fire_ticks"          -> entity.setFireTicks(Math.max(-1, toInt(value)));
            case "minecraft:movement_speed" -> {
                if (!(entity instanceof LivingEntity le)) {
                    LOGGER.warning("[HexVG-DC] minecraft:movement_speed wymaga LivingEntity."); return;
                }
                setAttr(le, Attribute.MOVEMENT_SPEED, toDouble(value));
            }
            case "minecraft:follow_range" -> {
                if (!(entity instanceof Mob mob)) {
                    LOGGER.warning("[HexVG-DC] minecraft:follow_range wymaga Mob."); return;
                }
                setAttr(mob, Attribute.FOLLOW_RANGE, toDouble(value));
            }
            case "minecraft:attack_damage" -> {
                if (!(entity instanceof LivingEntity le)) {
                    LOGGER.warning("[HexVG-DC] minecraft:attack_damage wymaga LivingEntity."); return;
                }
                setAttr(le, Attribute.ATTACK_DAMAGE, toDouble(value));
            }
            case "minecraft:armor" -> {
                if (!(entity instanceof LivingEntity le)) {
                    LOGGER.warning("[HexVG-DC] minecraft:armor wymaga LivingEntity."); return;
                }
                setAttr(le, Attribute.ARMOR, toDouble(value));
            }
            case "minecraft:armor_toughness" -> {
                if (!(entity instanceof LivingEntity le)) {
                    LOGGER.warning("[HexVG-DC] minecraft:armor_toughness wymaga LivingEntity."); return;
                }
                setAttr(le, Attribute.ARMOR_TOUGHNESS, toDouble(value));
            }
            default -> LOGGER.warning("[HexVG-DC] Nieznany komponent encji: '" + componentName + "'");
        }
    }

    public static void remove(Entity entity, String componentName) {
        if (entity == null) return;
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
            case "minecraft:max_health" -> {
                if (entity instanceof LivingEntity le) {
                    AttributeInstance attr = le.getAttribute(Attribute.MAX_HEALTH);
                    if (attr != null) attr.setBaseValue(20.0);
                }
            }
            case "minecraft:movement_speed" -> {
                if (entity instanceof LivingEntity le) {
                    AttributeInstance attr = le.getAttribute(Attribute.MOVEMENT_SPEED);
                    if (attr != null) attr.setBaseValue(0.2);
                }
            }
            default -> LOGGER.warning("[HexVG-DC] Nie można zresetować komponentu encji: '" + componentName + "'");
        }
    }

    private static void setAttr(LivingEntity entity, Attribute attribute, double value) {
        AttributeInstance attr = entity.getAttribute(attribute);
        if (attr != null) {
            attr.setBaseValue(value);
        } else {
            LOGGER.warning("[HexVG-DC] Encja nie posiada atrybutu: " + attribute.key().asString());
        }
    }

    private static double toDouble(Object value) {
        if (value instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(value.toString()); }
        catch (NumberFormatException e) { return 0.0; }
    }

    private static int toInt(Object value) {
        return (int) toDouble(value);
    }

    private static boolean toBool(Object value) {
        if (value instanceof Boolean b) return b;
        return Boolean.parseBoolean(value.toString());
    }
}
