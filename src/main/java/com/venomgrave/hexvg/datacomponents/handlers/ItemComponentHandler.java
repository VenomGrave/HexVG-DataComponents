package com.venomgrave.hexvg.datacomponents.handlers;

import com.venomgrave.hexvg.datacomponents.events.DataComponentChangeEvent;
import com.venomgrave.hexvg.datacomponents.events.DataComponentRemoveEvent;
import com.venomgrave.hexvg.datacomponents.utils.ComponentConverter;
import com.venomgrave.hexvg.datacomponents.utils.ComponentTypeRegistry;
import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import io.papermc.paper.datacomponent.item.ItemEnchantments;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers;
import io.papermc.paper.datacomponent.item.DyedItemColor;
import io.papermc.paper.datacomponent.item.ItemArmorTrim;
import io.papermc.paper.datacomponent.item.FoodProperties;
import io.papermc.paper.datacomponent.item.WrittenBookContent;
import io.papermc.paper.datacomponent.item.BannerPatternLayers;
import io.papermc.paper.datacomponent.item.SuspiciousStewEffects;
import io.papermc.paper.datacomponent.item.Tool;
import io.papermc.paper.datacomponent.item.Equippable;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.UseCooldown;
import org.bukkit.inventory.EquipmentSlot;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.block.banner.PatternType;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import io.papermc.paper.datacomponent.item.PotionContents;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

@SuppressWarnings({"UnstableApiUsage", "unchecked", "rawtypes"})
public final class ItemComponentHandler {

    private static final Logger LOGGER = Logger.getLogger("HexVG-DataComponents");

    private static final Object UNBREAKABLE_TYPE;
    private static final boolean UNBREAKABLE_IS_NON_VALUED;

    private static final java.lang.reflect.Method OMINOUS_AMPLIFIER_METHOD;

    private static final Object FIRE_RESISTANT_TYPE;
    private static final Object GLIDER_TYPE;
    private static final Object HIDE_TOOLTIP_TYPE;

    static {
        Object type = null;
        boolean isNonValued = false;
        try {
            Field f = DataComponentTypes.class.getField("UNBREAKABLE");
            type = f.get(null);
            isNonValued = type instanceof DataComponentType.NonValued;
            LOGGER.info("[HexVG-DC] UNBREAKABLE type: " + type.getClass().getName() + ", NonValued=" + isNonValued);
        } catch (Exception e) {
            LOGGER.warning("[HexVG-DC] Nie mozna zaladowac UNBREAKABLE: " + e.getMessage());
        }
        UNBREAKABLE_TYPE = type;
        UNBREAKABLE_IS_NON_VALUED = isNonValued;

        java.lang.reflect.Method ampMethod = null;
        try {
            ampMethod = io.papermc.paper.datacomponent.item.OminousBottleAmplifier.class.getMethod("amplifier", int.class);
        } catch (Exception e) {
            LOGGER.warning("[HexVG-DC] OminousBottleAmplifier.amplifier(int) niedostepny: " + e.getMessage());
        }
        OMINOUS_AMPLIFIER_METHOD = ampMethod;

        FIRE_RESISTANT_TYPE = loadComponentField("FIRE_RESISTANT");
        GLIDER_TYPE         = loadComponentField("GLIDER");
        HIDE_TOOLTIP_TYPE   = loadComponentField("HIDE_TOOLTIP");
    }

    private static Object loadComponentField(String fieldName) {
        try {
            Field f = DataComponentTypes.class.getField(fieldName);
            Object val = f.get(null);
            LOGGER.fine("[HexVG-DC] Zaladowano " + fieldName);
            return val;
        } catch (Exception e) {
            LOGGER.fine("[HexVG-DC] " + fieldName + " niedostepny w tej wersji Paper (pomijam).");
            return null;
        }
    }

    private ItemComponentHandler() {}

    public static Optional<Object> read(ItemStack item, String componentName) {
        if (item == null || item.getType().isAir()) return Optional.empty();
        if (!ComponentConverter.isValidComponentName(componentName)) return Optional.empty();

        Optional<DataComponentType.Valued<?>> valuedOpt = ComponentTypeRegistry.getValuedType(componentName);
        if (valuedOpt.isEmpty()) return Optional.empty();

        try {
            Object raw = item.getData(valuedOpt.get());
            if (raw == null) return Optional.empty();
            Object converted = ComponentConverter.toSkriptValue(raw);
            return Optional.ofNullable(converted);
        } catch (Exception e) {
            LOGGER.warning("[HexVG-DC] Blad odczytu '" + componentName + "': " + e.getMessage());
            return Optional.empty();
        }
    }

    public static void write(ItemStack item, String componentName, Object value, @Nullable Player player) {
        if (item == null || item.getType().isAir()) return;
        if (!ComponentConverter.isValidComponentName(componentName)) return;

        Optional<DataComponentType> typeOpt = ComponentTypeRegistry.getType(componentName);
        if (typeOpt.isEmpty()) {
            LOGGER.warning("[HexVG-DC] Nieznany komponent: " + componentName);
            return;
        }

        Optional<Object> converted = ComponentConverter.toComponentValue(componentName, value);
        if (converted.isEmpty()) return;

        Object oldValue = null;
        Optional<DataComponentType.Valued<?>> valuedOpt = ComponentTypeRegistry.getValuedType(componentName);
        if (valuedOpt.isPresent()) {
            try {
                Object raw = item.getData(valuedOpt.get());
                if (raw != null) oldValue = ComponentConverter.toSkriptValue(raw);
            } catch (Exception ignored) {}
        }

        DataComponentChangeEvent changeEvent = new DataComponentChangeEvent(player, item, componentName, oldValue, converted.get());
        Bukkit.getPluginManager().callEvent(changeEvent);
        if (changeEvent.isCancelled()) return;

        try {
            applyComponent(item, componentName, typeOpt.get(), changeEvent.getNewValue());
        } catch (Exception e) {
            LOGGER.warning("[HexVG-DC] Blad zapisu '" + componentName + "': " + e.getMessage());
        }
    }

    public static void remove(ItemStack item, String componentName, @Nullable Player player) {
        if (item == null || item.getType().isAir()) return;
        if (!ComponentConverter.isValidComponentName(componentName)) return;

        Optional<DataComponentType> typeOpt = ComponentTypeRegistry.getType(componentName);
        if (typeOpt.isEmpty()) return;

        Object oldValue = null;
        Optional<DataComponentType.Valued<?>> valuedOpt = ComponentTypeRegistry.getValuedType(componentName);
        if (valuedOpt.isPresent()) {
            try {
                Object raw = item.getData(valuedOpt.get());
                if (raw != null) oldValue = ComponentConverter.toSkriptValue(raw);
            } catch (Exception ignored) {}
        }

        DataComponentRemoveEvent removeEvent = new DataComponentRemoveEvent(player, item, componentName, oldValue);
        Bukkit.getPluginManager().callEvent(removeEvent);
        if (removeEvent.isCancelled()) return;

        try {
            item.resetData(typeOpt.get());
        } catch (Exception e) {
            LOGGER.warning("[HexVG-DC] Blad usuwania '" + componentName + "': " + e.getMessage());
        }
    }

    public static boolean hasComponent(ItemStack item, String componentName) {
        if (item == null || item.getType().isAir()) return false;
        if (!ComponentConverter.isValidComponentName(componentName)) return false;
        
        try {
            NamespacedKey key = NamespacedKey.fromString(componentName);
            if (key == null) return false;
            return item.getDataTypes().stream().anyMatch(t -> t.key().equals(key));
        } catch (Exception e) {
            return false;
        }
    }

    public static List<String> getAllComponents(ItemStack item) {
        if (item == null || item.getType().isAir()) return List.of();
        try {
            return item.getDataTypes().stream().map(t -> t.key().asString()).toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private static void applyComponent(ItemStack item, String componentName, DataComponentType type, Object value) {
        
        if (type == DataComponentTypes.CUSTOM_NAME || type == DataComponentTypes.ITEM_NAME) {
            String str = value instanceof String s ? s : value.toString();
            Component comp = ComponentConverter.parseComponent(str);
            item.setData((DataComponentType.Valued<Component>) type, comp);
            return;
        }

        if (type == DataComponentTypes.LORE) {
            List<String> lines = value instanceof List<?> list
                    ? list.stream().map(Object::toString).toList()
                    : List.of(value.toString());
            List<Component> components = ComponentConverter.parseComponentList(lines);
            item.setData(DataComponentTypes.LORE, ItemLore.lore(components));
            return;
        }

        if (type == DataComponentTypes.CUSTOM_MODEL_DATA) {
            int modelData = value instanceof Number n ? n.intValue() : Integer.parseInt(value.toString());
            item.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData()
                    .addFloat((float) modelData).build());
            return;
        }

        if (type == DataComponentTypes.MAX_STACK_SIZE) {
            int size = value instanceof Number n ? n.intValue() : Integer.parseInt(value.toString());
            item.setData(DataComponentTypes.MAX_STACK_SIZE, Math.max(1, Math.min(99, size)));
            return;
        }

        if (componentName.equals("minecraft:unbreakable")) {
            boolean unbreakable = value instanceof Boolean b ? b : Boolean.parseBoolean(value.toString());
            setUnbreakable(item, unbreakable);
            return;
        }

        if (type == DataComponentTypes.MAX_DAMAGE) {
            int damage = value instanceof Number n ? n.intValue() : Integer.parseInt(value.toString());
            item.setData(DataComponentTypes.MAX_DAMAGE, Math.max(1, damage));
            return;
        }

        if (type == DataComponentTypes.DAMAGE) {
            int damage = value instanceof Number n ? n.intValue() : Integer.parseInt(value.toString());
            item.setData(DataComponentTypes.DAMAGE, Math.max(0, damage));
            return;
        }

        if (type == DataComponentTypes.FOOD) {
            applyFood(item, value);
            return;
        }

        if (type == DataComponentTypes.DYED_COLOR) {
            applyDyedColor(item, value);
            return;
        }

        if (type == DataComponentTypes.TRIM) {
            applyTrim(item, value);
            return;
        }

        if (type == DataComponentTypes.ENCHANTMENTS) {
            applyEnchantments(item, value);
            return;
        }

        if (type == DataComponentTypes.ATTRIBUTE_MODIFIERS) {
            applyAttributeModifiers(item, value);
            return;
        }

        if (type == DataComponentTypes.POTION_CONTENTS) {
            applyPotionContents(item, value);
            return;
        }

        if (componentName.equals("minecraft:hide_tooltip")) {
            boolean hide = value instanceof Boolean b ? b : Boolean.parseBoolean(value.toString());
            applyNonValuedFromCache(item, "HIDE_TOOLTIP", HIDE_TOOLTIP_TYPE, hide);
            return;
        }

        if (componentName.equals("minecraft:fire_resistant")) {
            boolean resistant = value instanceof Boolean b ? b : Boolean.parseBoolean(value.toString());
            applyNonValuedFromCache(item, "FIRE_RESISTANT", FIRE_RESISTANT_TYPE, resistant);
            return;
        }

        if (componentName.equals("minecraft:glider")) {
            boolean glider = value instanceof Boolean b ? b : Boolean.parseBoolean(value.toString());
            applyNonValuedFromCache(item, "GLIDER", GLIDER_TYPE, glider);
            return;
        }

        if (type == DataComponentTypes.WRITTEN_BOOK_CONTENT) {
            applyWrittenBookContent(item, value);
            return;
        }

        if (type == DataComponentTypes.BANNER_PATTERNS) {
            applyBannerPatterns(item, value);
            return;
        }

        if (type == DataComponentTypes.SUSPICIOUS_STEW_EFFECTS) {
            applySuspiciousStewEffects(item, value);
            return;
        }

        if (type == DataComponentTypes.TOOL) {
            applyTool(item, value);
            return;
        }

        if (type == DataComponentTypes.STORED_ENCHANTMENTS) {
            applyStoredEnchantments(item, value);
            return;
        }

        if (type == DataComponentTypes.REPAIR_COST) {
            int cost = value instanceof Number n ? n.intValue() : Integer.parseInt(value.toString());
            item.setData(DataComponentTypes.REPAIR_COST, Math.max(0, cost));
            return;
        }

        if (type == DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE) {
            boolean glint = value instanceof Boolean b ? b : Boolean.parseBoolean(value.toString());
            item.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, glint);
            return;
        }

        if (type == DataComponentTypes.RARITY) {
            applyRarity(item, value);
            return;
        }

        if (type == DataComponentTypes.ITEM_MODEL) {
            String modelStr = value.toString().trim();
            NamespacedKey modelKey = modelStr.contains(":") ? NamespacedKey.fromString(modelStr) : NamespacedKey.minecraft(modelStr);
            if (modelKey != null) item.setData(DataComponentTypes.ITEM_MODEL, modelKey);
            else LOGGER.warning("[HexVG-DC] Nieprawidlowy klucz item_model: " + modelStr);
            return;
        }

        if (type == DataComponentTypes.EQUIPPABLE) {
            applyEquippable(item, value);
            return;
        }

        if (type == DataComponentTypes.CONSUMABLE) {
            applyConsumable(item, value);
            return;
        }

        if (type == DataComponentTypes.USE_COOLDOWN) {
            applyUseCooldown(item, value);
            return;
        }

        if (type == DataComponentTypes.OMINOUS_BOTTLE_AMPLIFIER) {
            if (OMINOUS_AMPLIFIER_METHOD == null) { LOGGER.warning("[HexVG-DC] ominous_bottle_amplifier niedostepny."); return; }
            int clamped = Math.max(0, Math.min(4, value instanceof Number n ? n.intValue() : parseInt(value.toString(), "ominous_bottle_amplifier")));
            try {
                item.setData(DataComponentTypes.OMINOUS_BOTTLE_AMPLIFIER,
                    (io.papermc.paper.datacomponent.item.OminousBottleAmplifier) OMINOUS_AMPLIFIER_METHOD.invoke(null, clamped));
            } catch (Exception ex) {
                LOGGER.warning("[HexVG-DC] Blad ominous_bottle_amplifier: " + ex.getMessage());
            }
            return;
        }

        if (type == DataComponentTypes.BASE_COLOR) {
            String colorName = value.toString().trim().toUpperCase();
            try {
                DyeColor dyeColor = DyeColor.valueOf(colorName);
                item.setData(DataComponentTypes.BASE_COLOR, dyeColor);
            } catch (IllegalArgumentException e) {
                LOGGER.warning("[HexVG-DC] Nieznany kolor base_color: " + colorName);
            }
            return;
        }

        if (type == DataComponentTypes.INSTRUMENT) {
            applyInstrument(item, value);
            return;
        }

        if (type == DataComponentTypes.JUKEBOX_PLAYABLE) {
            applyJukeboxPlayable(item, value);
            return;
        }

        LOGGER.warning("[HexVG-DC] Komponent '" + componentName + "' nie jest obslugiwany.");
    }

    private static ItemEnchantments buildEnchantments(List<String> entries) {
        ItemEnchantments.Builder builder = ItemEnchantments.itemEnchantments();
        for (String entry : entries) {
            
            int lastColon = entry.lastIndexOf(':');
            if (lastColon < 1) { LOGGER.warning("[HexVG-DC] Nieprawidlowy format enchant '" + entry + "'. Uzyj 'nazwa:poziom'"); continue; }
            String enchantName = entry.substring(0, lastColon).trim().toLowerCase();
            int level = parseInt(entry.substring(lastColon + 1), "enchant level");
            NamespacedKey key = parseKey(enchantName);
            if (key == null) { LOGGER.warning("[HexVG-DC] Nieprawidlowy klucz enchantu: " + enchantName); continue; }
            Enchantment enchant = Registry.ENCHANTMENT.get(key);
            if (enchant == null) { LOGGER.warning("[HexVG-DC] Nieznany enchant: " + enchantName); continue; }
            builder.add(enchant, level);
        }
        return builder.build();
    }

    public static @Nullable Player extractPlayer(org.bukkit.event.Event event) {
        return event instanceof org.bukkit.event.player.PlayerEvent pe ? pe.getPlayer() : null;
    }

    private static List<String> toEntries(Object value) {
        if (value instanceof List<?> list) {
            List<String> result = new ArrayList<>(list.size());
            for (Object o : list) result.add(o.toString());
            return result;
        }
        return List.of(value.toString());
    }

    private static NamespacedKey parseKey(String name) {
        return name.contains(":") ? NamespacedKey.fromString(name) : NamespacedKey.minecraft(name);
    }

    private static @Nullable Attribute resolveAttribute(String input) {
        String name = input.trim().toLowerCase();

        java.util.LinkedHashSet<String> candidates = new java.util.LinkedHashSet<>();

        if (name.contains(":")) {
            
            candidates.add(name);
            
            if (name.contains(":generic.")) {
                candidates.add(name.replace(":generic.", ":"));
            } else {
                int colon = name.indexOf(':');
                candidates.add(name.substring(0, colon + 1) + "generic." + name.substring(colon + 1));
            }
        } else {
            
            String bare = name.startsWith("generic.") ? name.substring("generic.".length()) : name;
            
            candidates.add("minecraft:" + bare);
            candidates.add("minecraft:generic." + bare);
            
            if (!name.equals(bare)) {
                candidates.add("minecraft:" + name);
            }
        }

        for (String candidate : candidates) {
            NamespacedKey key = NamespacedKey.fromString(candidate);
            if (key == null) continue;
            Attribute attr = Registry.ATTRIBUTE.get(key);
            if (attr != null) return attr;
        }
        return null;
    }

    private static int parseInt(String val, String context) {
        try { return Integer.parseInt(val.trim()); }
        catch (NumberFormatException e) { LOGGER.warning("[HexVG-DC] Nieprawidlowa wartosc int dla " + context + ": " + val); return 0; }
    }

    private static float parseFloat(String val, String context) {
        try { return Float.parseFloat(val.trim()); }
        catch (NumberFormatException e) { LOGGER.warning("[HexVG-DC] Nieprawidlowa wartosc float dla " + context + ": " + val); return 0f; }
    }

    private static void applyEnchantments(ItemStack item, Object value) {
        item.setData(DataComponentTypes.ENCHANTMENTS, buildEnchantments(toEntries(value)));
    }

    private static void applyAttributeModifiers(ItemStack item, Object value) {
        List<String> entries = toEntries(value);

        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.itemAttributes();

        for (String entry : entries) {
            String[] parts = entry.split(":", 4);
            if (parts.length < 3) {
                LOGGER.warning("[HexVG-DC] Nieprawidlowy format atrybutu '" + entry + "'. Uzyj 'atrybut:operacja:wartosc[:slot]'");
                continue;
            }

            String attrName   = parts[0].trim().toLowerCase();
            String opName     = parts[1].trim().toLowerCase();
            double attrValue;
            try {
                attrValue = Double.parseDouble(parts[2].trim());
            } catch (NumberFormatException e) {
                LOGGER.warning("[HexVG-DC] Nieprawidlowa wartosc atrybutu: " + parts[2]);
                continue;
            }
            String slotName = parts.length >= 4 ? parts[3].trim().toLowerCase() : "any";

            Attribute attribute = resolveAttribute(attrName);
            if (attribute == null) {
                LOGGER.warning("[HexVG-DC] Nieznany atrybut: '" + attrName + "'. "
                    + "Dostepne formaty: 'attack_damage', 'generic.attack_damage', 'minecraft:attack_damage'");
                continue;
            }

            AttributeModifier.Operation operation = switch (opName) {
                case "add_value",           "add",      "0" -> AttributeModifier.Operation.ADD_NUMBER;
                case "add_multiplied_base", "multiply", "1" -> AttributeModifier.Operation.ADD_SCALAR;
                case "add_multiplied_total",             "2" -> AttributeModifier.Operation.MULTIPLY_SCALAR_1;
                default -> {
                    LOGGER.warning("[HexVG-DC] Nieznana operacja: " + opName);
                    yield null;
                }
            };
            if (operation == null) continue;

            EquipmentSlotGroup slotGroup = switch (slotName) {
                case "mainhand"  -> EquipmentSlotGroup.MAINHAND;
                case "offhand"   -> EquipmentSlotGroup.OFFHAND;
                case "hand"      -> EquipmentSlotGroup.HAND;
                case "head"      -> EquipmentSlotGroup.HEAD;
                case "chest"     -> EquipmentSlotGroup.CHEST;
                case "legs"      -> EquipmentSlotGroup.LEGS;
                case "feet"      -> EquipmentSlotGroup.FEET;
                case "armor"     -> EquipmentSlotGroup.ARMOR;
                case "body"      -> EquipmentSlotGroup.BODY;
                default          -> EquipmentSlotGroup.ANY;
            };

            NamespacedKey modKey = new NamespacedKey("hexvg", attrName.replace(":", "_").replace(".", "_") + "_" + UUID.randomUUID().toString().substring(0, 8));
            AttributeModifier modifier = new AttributeModifier(modKey, attrValue, operation, slotGroup);
            builder.addModifier(attribute, modifier);
        }

        item.setData(DataComponentTypes.ATTRIBUTE_MODIFIERS, builder.build());
    }

    private static void setUnbreakable(ItemStack item, boolean unbreakable) {
        if (UNBREAKABLE_TYPE == null) {
            LOGGER.warning("[HexVG-DC] UNBREAKABLE_TYPE niedostepny.");
            return;
        }
        try {
            if (!unbreakable) {
                item.resetData((DataComponentType) UNBREAKABLE_TYPE);
                return;
            }
            if (UNBREAKABLE_IS_NON_VALUED) {
                
                item.setData((DataComponentType.NonValued) UNBREAKABLE_TYPE);
            } else {
                
                DataComponentType.Valued valued = (DataComponentType.Valued) UNBREAKABLE_TYPE;
                
                Class<?> valueClass = getValuedTypeClass(valued);
                if (valueClass != null) {
                    Object obj;
                    try {
                        obj = valueClass.getMethod("unbreakable", boolean.class).invoke(null, true);
                    } catch (NoSuchMethodException e) {
                        obj = valueClass.getMethod("unbreakable").invoke(null);
                    }
                    Method setData = ItemStack.class.getMethod("setData", DataComponentType.Valued.class, Object.class);
                    setData.invoke(item, valued, obj);
                }
            }
        } catch (Exception e) {
            LOGGER.warning("[HexVG-DC] Blad setUnbreakable: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private static Class<?> getValuedTypeClass(DataComponentType.Valued<?> valued) {
        
        try {
            java.lang.reflect.Type[] ifaces = valued.getClass().getGenericInterfaces();
            for (java.lang.reflect.Type iface : ifaces) {
                if (iface instanceof java.lang.reflect.ParameterizedType pt) {
                    if (pt.getRawType().equals(DataComponentType.Valued.class)) {
                        java.lang.reflect.Type arg = pt.getActualTypeArguments()[0];
                        if (arg instanceof Class<?> cls) return cls;
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
    
    private static void applyFood(ItemStack item, Object value) {
        String str = value.toString();
        String[] parts = str.split(":");
        if (parts.length < 2) {
            LOGGER.warning("[HexVG-DC] Nieprawidlowy format food '" + str + "'. Uzyj 'nutrition:saturation[:canAlwaysEat]'");
            return;
        }
        int nutrition = parseInt(parts[0], "food.nutrition");
        float saturation = parseFloat(parts[1], "food.saturation");
        boolean canAlwaysEat = parts.length >= 3 && Boolean.parseBoolean(parts[2].trim());
        item.setData(DataComponentTypes.FOOD, FoodProperties.food()
            .nutrition(nutrition).saturation(saturation).canAlwaysEat(canAlwaysEat).build());
    }

    private static void applyDyedColor(ItemStack item, Object value) {
        String str = value.toString().trim();
        try {
            Color color;
            if (str.startsWith("#")) {
                color = Color.fromRGB(Integer.parseInt(str.substring(1), 16));
            } else if (str.contains(",")) {
                String[] p = str.split(",");
                color = Color.fromRGB(Integer.parseInt(p[0].trim()), Integer.parseInt(p[1].trim()), Integer.parseInt(p[2].trim()));
            } else {
                color = Color.fromRGB(Integer.parseInt(str));
            }
            item.setData(DataComponentTypes.DYED_COLOR, DyedItemColor.dyedItemColor(color, true));
        } catch (Exception e) {
            LOGGER.warning("[HexVG-DC] Nieprawidlowy kolor '" + str + "': " + e.getMessage());
        }
    }

    private static void applyTrim(ItemStack item, Object value) {
        String str = value.toString().trim();
        String[] parts = str.split(":", 2);
        if (parts.length < 2) {
            LOGGER.warning("[HexVG-DC] Nieprawidlowy format trim '" + str + "'. Uzyj 'material:pattern'");
            return;
        }
        try {
            String matName = parts[0].trim().toLowerCase();
            String patName = parts[1].trim().toLowerCase();

            NamespacedKey matKey = parseKey(matName);
            NamespacedKey patKey = parseKey(patName);

            if (matKey == null || patKey == null) {
                LOGGER.warning("[HexVG-DC] Nieprawidlowy klucz trim: " + str);
                return;
            }

            TrimMaterial material = Registry.TRIM_MATERIAL.get(matKey);
            TrimPattern pattern = Registry.TRIM_PATTERN.get(patKey);

            if (material == null) { LOGGER.warning("[HexVG-DC] Nieznany material trim: " + matName); return; }
            if (pattern == null)  { LOGGER.warning("[HexVG-DC] Nieznany wzor trim: " + patName); return; }

            item.setData(DataComponentTypes.TRIM, ItemArmorTrim.itemArmorTrim(new ArmorTrim(material, pattern)).build());
        } catch (Exception e) {
            LOGGER.warning("[HexVG-DC] Blad trim '" + str + "': " + e.getMessage());
        }
    }
    
    private static void applyPotionContents(ItemStack item, Object value) {
        List<String> entries = toEntries(value);

        PotionContents.Builder builder = PotionContents.potionContents();

        for (String entry : entries) {
            String[] parts = entry.split(":", -1);
            if (parts.length < 2) {
                LOGGER.warning("[HexVG-DC] Nieprawidlowy format potion '" + entry + "'");
                continue;
            }

            String type = parts[0].trim().toLowerCase();

            if (type.equals("base")) {
                
                String potionName = parts[1].trim()
                    .toUpperCase()
                    .replace("MINECRAFT:", "")
                    .replace("-", "_");
                try {
                    PotionType potionType = PotionType.valueOf(potionName);
                    builder.potion(potionType);
                } catch (IllegalArgumentException e) {
                    LOGGER.warning("[HexVG-DC] Nieznany PotionType: '" + potionName
                        + "'. Uzyj np. speed, strong_strength, long_fire_resistance (wielkosc liter nie ma znaczenia)");
                }

            } else if (type.equals("effect")) {
                
                if (parts.length < 4) {
                    LOGGER.warning("[HexVG-DC] Nieprawidlowy format effect '" + entry + "'. Uzyj 'effect:nazwa:ticki:amplifier'");
                    continue;
                }
                String effectName = parts[1].trim().toLowerCase();
                int duration, amplifier;
                try {
                    duration  = Integer.parseInt(parts[2].trim());
                    amplifier = Integer.parseInt(parts[3].trim());
                } catch (NumberFormatException e) {
                    LOGGER.warning("[HexVG-DC] Nieprawidlowe wartosci efektu: " + entry);
                    continue;
                }
                boolean ambient   = parts.length >= 5 && Boolean.parseBoolean(parts[4].trim());
                boolean particles = parts.length < 6 || Boolean.parseBoolean(parts[5].trim());

                NamespacedKey effectKey = parseKey(effectName);
                if (effectKey == null) { LOGGER.warning("[HexVG-DC] Nieprawidlowy klucz efektu: " + effectName); continue; }

                PotionEffectType effectType = Registry.EFFECT.get(effectKey);
                if (effectType == null) { LOGGER.warning("[HexVG-DC] Nieznany efekt: " + effectName); continue; }

                builder.addCustomEffect(new PotionEffect(effectType, duration, amplifier, ambient, particles));

            } else {
                LOGGER.warning("[HexVG-DC] Nieznany typ potion entry '" + type + "'. Uzyj 'base' lub 'effect'");
            }
        }

        item.setData(DataComponentTypes.POTION_CONTENTS, builder.build());
    }
    
    private static void applyNonValuedFromCache(ItemStack item, String fieldName, Object cachedType, boolean enable) {
        if (cachedType == null) {
            LOGGER.warning("[HexVG-DC] " + fieldName + " niedostepny w tej wersji Paper - pomijam.");
            return;
        }
        try {
            if (enable) {
                if (cachedType instanceof DataComponentType.NonValued nonValued) {
                    item.setData(nonValued);
                } else {
                    LOGGER.warning("[HexVG-DC] " + fieldName + " nie jest NonValued.");
                }
            } else {
                if (cachedType instanceof DataComponentType dt) {
                    item.resetData(dt);
                }
            }
        } catch (Exception e) {
            LOGGER.warning("[HexVG-DC] Blad " + fieldName + ": " + e.getMessage());
        }
    }
    
    private static void applyWrittenBookContent(ItemStack item, Object value) {
        List<String> entries = toEntries(value);

        String title = "Bez tytulu";
        String author = "Nieznany";
        List<Component> pages = new ArrayList<>();

        for (String entry : entries) {
            int colon = entry.indexOf(':');
            if (colon < 0) { pages.add(ComponentConverter.parseComponent(entry)); continue; }
            String key = entry.substring(0, colon).trim().toLowerCase();
            String val = entry.substring(colon + 1).trim();
            switch (key) {
                case "title"  -> title = val;
                case "author" -> author = val;
                case "page"   -> pages.add(ComponentConverter.parseComponent(val));
                default       -> pages.add(ComponentConverter.parseComponent(entry));
            }
        }

        if (pages.isEmpty()) pages.add(Component.empty());

        try {
            WrittenBookContent.Builder builder = WrittenBookContent.writtenBookContent(title, author);
            for (Component page : pages) {
                builder.addPage(page);
            }
            item.setData(DataComponentTypes.WRITTEN_BOOK_CONTENT, builder.build());
        } catch (Exception e) {
            LOGGER.warning("[HexVG-DC] Blad written_book_content: " + e.getMessage());
        }
    }

    private static void applyBannerPatterns(ItemStack item, Object value) {
        List<String> entries = toEntries(value);

        BannerPatternLayers.Builder builder = BannerPatternLayers.bannerPatternLayers();

        for (String entry : entries) {
            String[] parts = entry.split(":", 2);
            if (parts.length < 2) {
                LOGGER.warning("[HexVG-DC] Nieprawidlowy format banner_pattern '" + entry + "'. Uzyj 'wzor:kolor'");
                continue;
            }
            String patName   = parts[0].trim().toUpperCase();
            String colorName = parts[1].trim().toUpperCase();

            PatternType patternType;
            try {
                NamespacedKey patKey = parseKey(patName.toLowerCase());
                patternType = Registry.BANNER_PATTERN.get(patKey);
                if (patternType == null) {
                    LOGGER.warning("[HexVG-DC] Nieznany wzor banneru: " + patName);
                    continue;
                }
            } catch (Exception e) {
                LOGGER.warning("[HexVG-DC] Blad wzoru banneru '" + patName + "': " + e.getMessage());
                continue;
            }

            DyeColor dyeColor;
            try {
                dyeColor = DyeColor.valueOf(colorName);
            } catch (IllegalArgumentException e) {
                LOGGER.warning("[HexVG-DC] Nieznany kolor banneru: " + colorName);
                continue;
            }

            builder.add(new org.bukkit.block.banner.Pattern(dyeColor, patternType));
        }

        item.setData(DataComponentTypes.BANNER_PATTERNS, builder.build());
    }

    private static void applySuspiciousStewEffects(ItemStack item, Object value) {
        List<String> entries = toEntries(value);

        SuspiciousStewEffects.Builder builder = SuspiciousStewEffects.suspiciousStewEffects();

        for (String entry : entries) {
            String[] parts = entry.split(":", 2);
            if (parts.length < 2) {
                LOGGER.warning("[HexVG-DC] Nieprawidlowy format stew_effect '" + entry + "'. Uzyj 'efekt:ticki'");
                continue;
            }
            String effectName = parts[0].trim().toLowerCase();
            int duration;
            try {
                duration = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException e) {
                LOGGER.warning("[HexVG-DC] Nieprawidlowy czas efektu: " + parts[1]);
                continue;
            }

            NamespacedKey effectKey = parseKey(effectName);
            if (effectKey == null) { LOGGER.warning("[HexVG-DC] Nieprawidlowy klucz efektu: " + effectName); continue; }

            PotionEffectType effectType = Registry.EFFECT.get(effectKey);
            if (effectType == null) { LOGGER.warning("[HexVG-DC] Nieznany efekt: " + effectName); continue; }

            builder.add(io.papermc.paper.potion.SuspiciousEffectEntry.create(effectType, duration));
        }

        item.setData(DataComponentTypes.SUSPICIOUS_STEW_EFFECTS, builder.build());
    }

    private static void applyTool(ItemStack item, Object value) {
        List<String> entries = toEntries(value);

        Tool.Builder builder = Tool.tool();

        for (String entry : entries) {
            int colon = entry.indexOf(':');
            if (colon < 0) continue;
            String key = entry.substring(0, colon).trim().toLowerCase();
            String rest = entry.substring(colon + 1).trim();

            switch (key) {
                case "speed" -> {
                    try {
                        builder.defaultMiningSpeed(Float.parseFloat(rest));
                    } catch (NumberFormatException e) {
                        LOGGER.warning("[HexVG-DC] Nieprawidlowa predkosc tool: " + rest);
                    }
                }
                case "damage" -> {
                    try {
                        builder.damagePerBlock(Integer.parseInt(rest));
                    } catch (NumberFormatException e) {
                        LOGGER.warning("[HexVG-DC] Nieprawidlowe damage tool: " + rest);
                    }
                }
                case "rule" -> {
                    
                    String[] ruleParts = rest.split(":", 3);
                    if (ruleParts.length < 3) {
                        LOGGER.warning("[HexVG-DC] Nieprawidlowy format rule '" + entry + "'. Uzyj 'rule:bloki:predkosc:true/false'");
                        continue;
                    }
                    float speed;
                    boolean correct;
                    try {
                        speed   = Float.parseFloat(ruleParts[1].trim());
                        correct = Boolean.parseBoolean(ruleParts[2].trim());
                    } catch (Exception e) {
                        LOGGER.warning("[HexVG-DC] Nieprawidlowe wartosci rule: " + entry);
                        continue;
                    }

                    String[] blockNames = ruleParts[0].split(",");
                    List<io.papermc.paper.registry.keys.BlockTypeKeys> blockKeys = new ArrayList<>();
                    List<Material> materials = new ArrayList<>();
                    for (String blockName : blockNames) {
                        String bn = blockName.trim().toLowerCase();
                        try {
                            Material mat = Material.valueOf(bn.toUpperCase());
                            if (mat.isBlock()) materials.add(mat);
                            else LOGGER.warning("[HexVG-DC] Material nie jest blokiem: " + bn);
                        } catch (IllegalArgumentException e) {
                            LOGGER.warning("[HexVG-DC] Nieznany material: " + bn);
                        }
                    }
                    if (!materials.isEmpty()) {
                        
                        io.papermc.paper.registry.RegistryAccess ra = io.papermc.paper.registry.RegistryAccess.registryAccess();
                        io.papermc.paper.registry.TypedKey<org.bukkit.block.BlockType>[] keys =
                            materials.stream()
                                .map(mat -> io.papermc.paper.registry.TypedKey.<org.bukkit.block.BlockType>create(
                                    io.papermc.paper.registry.RegistryKey.BLOCK, mat.getKey()))
                                .toArray(io.papermc.paper.registry.TypedKey[]::new);
                        io.papermc.paper.registry.set.RegistryKeySet<org.bukkit.block.BlockType> keySet =
                            io.papermc.paper.registry.set.RegistrySet.keySet(
                                io.papermc.paper.registry.RegistryKey.BLOCK, keys);
                        builder.addRule(Tool.rule(keySet, speed, correct ? net.kyori.adventure.util.TriState.TRUE : net.kyori.adventure.util.TriState.FALSE));
                    }
                }
                default -> LOGGER.warning("[HexVG-DC] Nieznany klucz tool: " + key);
            }
        }

        item.setData(DataComponentTypes.TOOL, builder.build());
    }
    
    private static void applyStoredEnchantments(ItemStack item, Object value) {
        item.setData(DataComponentTypes.STORED_ENCHANTMENTS, buildEnchantments(toEntries(value)));
    }

    private static void applyRarity(ItemStack item, Object value) {
        String rarityName = value.toString().trim().toUpperCase();
        try {
            org.bukkit.inventory.ItemRarity rarity = org.bukkit.inventory.ItemRarity.valueOf(rarityName);
            item.setData(DataComponentTypes.RARITY, rarity);
        } catch (IllegalArgumentException e) {
            LOGGER.warning("[HexVG-DC] Nieznana rzadkosc: " + rarityName + ". Dostepne: COMMON, UNCOMMON, RARE, EPIC");
        }
    }

    private static void applyEquippable(ItemStack item, Object value) {
        List<String> entries = toEntries(value);

        EquipmentSlot slot = EquipmentSlot.HEAD;
        NamespacedKey sound = null;
        boolean swappable = true;
        boolean dispensable = true;
        NamespacedKey cameraOverlay = null;

        for (String entry : entries) {
            int colon = entry.indexOf(':');
            if (colon < 0) continue;
            String key = entry.substring(0, colon).trim().toLowerCase();
            String val = entry.substring(colon + 1).trim();

            switch (key) {
                case "slot" -> {
                    slot = switch (val.toLowerCase()) {
                        case "head"      -> EquipmentSlot.HEAD;
                        case "chest"     -> EquipmentSlot.CHEST;
                        case "legs"      -> EquipmentSlot.LEGS;
                        case "feet"      -> EquipmentSlot.FEET;
                        case "mainhand"  -> EquipmentSlot.HAND;
                        case "offhand"   -> EquipmentSlot.OFF_HAND;
                        case "body"      -> EquipmentSlot.BODY;
                        default -> { LOGGER.warning("[HexVG-DC] Nieznany slot equippable: " + val); yield EquipmentSlot.HEAD; }
                    };
                }
                case "sound"           -> sound = NamespacedKey.fromString(val);
                case "swappable"       -> swappable = Boolean.parseBoolean(val);
                case "dispensable"     -> dispensable = Boolean.parseBoolean(val);
                case "camera_overlay"  -> cameraOverlay = NamespacedKey.fromString(val);
            }
        }

        try {
            Equippable.Builder builder = Equippable.equippable(slot)
                .swappable(swappable)
                .dispensable(dispensable);
            if (sound != null) builder.equipSound(sound);
            if (cameraOverlay != null) builder.cameraOverlay(cameraOverlay);
            item.setData(DataComponentTypes.EQUIPPABLE, builder.build());
        } catch (Exception e) {
            LOGGER.warning("[HexVG-DC] Blad equippable: " + e.getMessage());
        }
    }

    private static void applyConsumable(ItemStack item, Object value) {
        List<String> entries = toEntries(value);

        float consumeTime = 1.6f;
        String animation = "eat";
        NamespacedKey sound = null;
        boolean hasParticles = true;

        for (String entry : entries) {
            int colon = entry.indexOf(':');
            if (colon < 0) continue;
            String key = entry.substring(0, colon).trim().toLowerCase();
            String val = entry.substring(colon + 1).trim();

            switch (key) {
                case "time"       -> { try { consumeTime = Float.parseFloat(val); } catch (NumberFormatException e) { LOGGER.warning("[HexVG-DC] Nieprawidlowy czas consumable: " + val); } }
                case "animation"  -> animation = val.toLowerCase();
                case "sound"      -> sound = NamespacedKey.fromString(val);
                case "particles"  -> hasParticles = Boolean.parseBoolean(val);
            }
        }

        try {
            io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation anim =
                switch (animation) {
                    case "none"      -> io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation.NONE;
                    case "drink"     -> io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation.DRINK;
                    case "block"     -> io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation.BLOCK;
                    case "bow"       -> io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation.BOW;
                    case "spear"     -> io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation.SPEAR;
                    case "crossbow"  -> io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation.CROSSBOW;
                    case "spyglass"  -> io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation.SPYGLASS;
                    case "toot_horn" -> io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation.TOOT_HORN;
                    case "brush"     -> io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation.BRUSH;
                    default          -> io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation.EAT;
                };

            Consumable.Builder builder = Consumable.consumable()
                .consumeSeconds(consumeTime)
                .animation(anim);
            if (sound != null) builder.sound(sound);
            item.setData(DataComponentTypes.CONSUMABLE, builder.build());
        } catch (Exception e) {
            LOGGER.warning("[HexVG-DC] Blad consumable: " + e.getMessage());
        }
    }

    private static void applyUseCooldown(ItemStack item, Object value) {
        List<String> entries = toEntries(value);

        float cooldownTime = 1.0f;
        NamespacedKey group = null;

        for (String entry : entries) {
            int colon = entry.indexOf(':');
            if (colon < 0) continue;
            String key = entry.substring(0, colon).trim().toLowerCase();
            String val = entry.substring(colon + 1).trim();

            switch (key) {
                case "time"  -> { try { cooldownTime = Float.parseFloat(val); } catch (NumberFormatException e) { LOGGER.warning("[HexVG-DC] Nieprawidlowy czas cooldown: " + val); } }
                case "group" -> group = NamespacedKey.fromString(val);
            }
        }

        try {
            UseCooldown.Builder builder = UseCooldown.useCooldown(cooldownTime);
            if (group != null) builder.cooldownGroup(group);
            item.setData(DataComponentTypes.USE_COOLDOWN, builder.build());
        } catch (Exception e) {
            LOGGER.warning("[HexVG-DC] Blad use_cooldown: " + e.getMessage());
        }
    }

    private static void applyInstrument(ItemStack item, Object value) {
        String instrName = value.toString().trim().toLowerCase();
        NamespacedKey instrKey = parseKey(instrName);
        if (instrKey == null) { LOGGER.warning("[HexVG-DC] Nieprawidlowy klucz instrument: " + instrName); return; }
        try {
            org.bukkit.MusicInstrument instrument = Registry.INSTRUMENT.get(instrKey);
            if (instrument == null) { LOGGER.warning("[HexVG-DC] Nieznany instrument: " + instrName); return; }
            item.setData(DataComponentTypes.INSTRUMENT, instrument);
        } catch (Exception e) {
            LOGGER.warning("[HexVG-DC] Blad instrument: " + e.getMessage());
        }
    }

    private static void applyJukeboxPlayable(ItemStack item, Object value) {
        String songName = value.toString().trim().toLowerCase();
        NamespacedKey songKey = parseKey(songName);
        if (songKey == null) { LOGGER.warning("[HexVG-DC] Nieprawidlowy klucz jukebox: " + songName); return; }
        try {
            org.bukkit.JukeboxSong song = Registry.JUKEBOX_SONG.get(songKey);
            if (song == null) { LOGGER.warning("[HexVG-DC] Nieznany utwr jukebox: " + songName); return; }
            io.papermc.paper.datacomponent.item.JukeboxPlayable.Builder builder =
                io.papermc.paper.datacomponent.item.JukeboxPlayable.jukeboxPlayable(song);
            item.setData(DataComponentTypes.JUKEBOX_PLAYABLE, builder.build());
        } catch (Exception e) {
            LOGGER.warning("[HexVG-DC] Blad jukebox_playable: " + e.getMessage());
        }
    }
}
