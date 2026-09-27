package com.venomgrave.hexvg.datacomponents.handlers;

import com.venomgrave.hexvg.datacomponents.events.DataComponentChangeEvent;
import com.venomgrave.hexvg.datacomponents.events.DataComponentRemoveEvent;
import com.venomgrave.hexvg.datacomponents.utils.ComponentConverter;
import com.venomgrave.hexvg.datacomponents.utils.ComponentLogger;
import com.venomgrave.hexvg.datacomponents.utils.ComponentTypeRegistry;
import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
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
import io.papermc.paper.datacomponent.item.WrittenBookContent;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import io.papermc.paper.potion.SuspiciousEffectEntry;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.set.RegistryKeySet;
import io.papermc.paper.registry.set.RegistrySet;
import io.papermc.paper.registry.tag.TagKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.TriState;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.JukeboxSong;
import org.bukkit.Material;
import org.bukkit.MusicInstrument;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.BlockType;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@SuppressWarnings({"UnstableApiUsage", "unchecked", "rawtypes"})
public final class ItemComponentHandler {

    // Limity zgodne z walidacja vanilla - przekroczenie powoduje, ze serwer nie potrafi
    // zapisac przedmiotu (utrata ekwipunku / blad zapisu chunka).
    private static final int MAX_LORE_LINES = 256;
    private static final int MAX_BOOK_PAGES = 100;
    private static final int MAX_BOOK_TITLE_LENGTH = 32;
    private static final int MAX_ENCHANT_LEVEL = 255;
    private static final int MAX_EFFECT_AMPLIFIER = 255;
    private static final int MAX_STACK_SIZE = 99;
    private static final int MAX_OMINOUS_AMPLIFIER = 4;

    private static final String UNBREAKABLE = "minecraft:unbreakable";

    // Paper 1.21.4: minecraft:unbreakable jest Valued<Unbreakable>; od 1.21.5 jest NonValued,
    // a klasa Unbreakable zostala usunieta - stad refleksja.
    private static final @Nullable Object LEGACY_UNBREAKABLE_VALUE = loadLegacyUnbreakableValue();

    private ItemComponentHandler() {}

    private static @Nullable Object loadLegacyUnbreakableValue() {
        try {
            Class<?> cls = Class.forName("io.papermc.paper.datacomponent.item.Unbreakable");
            Method factory = cls.getMethod("unbreakable", boolean.class);
            return factory.invoke(null, true);
        } catch (Exception | LinkageError e) {
            return null; // nowszy Paper - unbreakable jest NonValued
        }
    }

    // ------------------------------------------------------------------
    //  API publiczne
    // ------------------------------------------------------------------

    public static Optional<Object> read(ItemStack item, String componentName) {
        if (isEmpty(item)) return Optional.empty();
        Optional<DataComponentType> typeOpt = ComponentTypeRegistry.getType(componentName);
        if (typeOpt.isEmpty()) return Optional.empty();
        DataComponentType type = typeOpt.get();

        try {
            // Komponenty-znaczniki (NonValued) oraz unbreakable nie maja wartosci - zwracamy true, gdy sa obecne.
            if (!(type instanceof DataComponentType.Valued<?> valued) || UNBREAKABLE.equals(componentName)) {
                return item.hasData(type) ? Optional.of(Boolean.TRUE) : Optional.empty();
            }
            Object raw = item.getData(valued);
            return raw == null ? Optional.empty() : Optional.ofNullable(ComponentConverter.toSkriptValue(raw));
        } catch (Exception e) {
            ComponentLogger.warn("Blad odczytu '" + componentName + "': " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Zapisuje komponent na przedmiocie (modyfikuje przekazany ItemStack).
     *
     * @return true, jesli komponent zostal zapisany
     */
    public static boolean write(ItemStack item, String componentName, Object value, @Nullable Player player) {
        if (isEmpty(item) || value == null) return false;

        Optional<DataComponentType> typeOpt = ComponentTypeRegistry.getType(componentName);
        if (typeOpt.isEmpty()) {
            ComponentLogger.warn("Nieznany komponent: " + componentName);
            return false;
        }
        DataComponentType type = typeOpt.get();

        Optional<Object> converted = ComponentConverter.toComponentValue(componentName, value);
        if (converted.isEmpty()) return false;
        Object newValue = converted.get();

        // Event (i kosztowny odczyt starej wartosci) tylko, gdy ktos go nasluchuje.
        if (hasListeners(DataComponentChangeEvent.getHandlerList())) {
            Object oldValue = read(item, componentName).orElse(null);
            DataComponentChangeEvent changeEvent =
                    new DataComponentChangeEvent(player, item, componentName, oldValue, newValue);
            Bukkit.getPluginManager().callEvent(changeEvent);
            if (changeEvent.isCancelled()) return false;

            Object eventValue = changeEvent.getNewValue();
            if (eventValue != newValue) {
                // Wartosc zmieniona przez skrypt - ponownie przepuszczamy ja przez walidacje i limity.
                Optional<Object> reconverted = ComponentConverter.toComponentValue(componentName, eventValue);
                if (reconverted.isEmpty()) return false;
                newValue = reconverted.get();
            }
        }

        try {
            return applyComponent(item, componentName, type, newValue);
        } catch (Exception e) {
            ComponentLogger.warn("Blad zapisu '" + componentName + "': " + e.getMessage());
            return false;
        }
    }

    /**
     * Usuwa komponent z przedmiotu (modyfikuje przekazany ItemStack).
     *
     * @return true, jesli komponent zostal usuniety
     */
    public static boolean remove(ItemStack item, String componentName, @Nullable Player player) {
        if (isEmpty(item)) return false;

        Optional<DataComponentType> typeOpt = ComponentTypeRegistry.getType(componentName);
        if (typeOpt.isEmpty()) return false;
        DataComponentType type = typeOpt.get();
        if (!item.hasData(type)) return false;

        if (hasListeners(DataComponentRemoveEvent.getHandlerList())) {
            Object oldValue = read(item, componentName).orElse(null);
            DataComponentRemoveEvent removeEvent = new DataComponentRemoveEvent(player, item, componentName, oldValue);
            Bukkit.getPluginManager().callEvent(removeEvent);
            if (removeEvent.isCancelled()) return false;
        }

        try {
            removeData(item, type);
            return true;
        } catch (Exception e) {
            ComponentLogger.warn("Blad usuwania '" + componentName + "': " + e.getMessage());
            return false;
        }
    }

    public static boolean hasComponent(ItemStack item, String componentName) {
        if (isEmpty(item)) return false;
        Optional<DataComponentType> typeOpt = ComponentTypeRegistry.getType(componentName);
        return typeOpt.isPresent() && item.hasData(typeOpt.get());
    }

    public static List<String> getAllComponents(ItemStack item) {
        if (isEmpty(item)) return List.of();
        try {
            return item.getDataTypes().stream().map(t -> t.key().asString()).sorted().toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    public static @Nullable Player extractPlayer(org.bukkit.event.Event event) {
        return event instanceof org.bukkit.event.player.PlayerEvent pe ? pe.getPlayer() : null;
    }

    // ------------------------------------------------------------------
    //  Zapis komponentow
    // ------------------------------------------------------------------

    private static boolean applyComponent(ItemStack item, String componentName, DataComponentType type, Object value) {
        switch (componentName) {
            case "minecraft:custom_name", "minecraft:item_name" -> {
                item.setData((DataComponentType.Valued<Component>) type, ComponentConverter.parseComponent(value.toString()));
                return true;
            }
            case "minecraft:lore"                     -> { return applyLore(item, value); }
            case "minecraft:custom_model_data"        -> { return applyCustomModelData(item, value); }
            case "minecraft:max_stack_size"           -> { return applyMaxStackSize(item, value); }
            case "minecraft:max_damage"               -> { return applyMaxDamage(item, value); }
            case "minecraft:damage"                   -> { return applyNonNegativeInt(item, DataComponentTypes.DAMAGE, value, componentName); }
            case "minecraft:repair_cost"              -> { return applyNonNegativeInt(item, DataComponentTypes.REPAIR_COST, value, componentName); }
            case UNBREAKABLE                          -> { return applyUnbreakable(item, type, toBool(value)); }
            case "minecraft:enchantment_glint_override" -> {
                item.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, toBool(value));
                return true;
            }
            case "minecraft:food"                     -> { return applyFood(item, value); }
            case "minecraft:dyed_color"               -> { return applyDyedColor(item, value); }
            case "minecraft:trim"                     -> { return applyTrim(item, value); }
            case "minecraft:enchantments"             -> { return applyEnchantments(item, DataComponentTypes.ENCHANTMENTS, value); }
            case "minecraft:stored_enchantments"      -> { return applyEnchantments(item, DataComponentTypes.STORED_ENCHANTMENTS, value); }
            case "minecraft:attribute_modifiers"      -> { return applyAttributeModifiers(item, value); }
            case "minecraft:potion_contents"          -> { return applyPotionContents(item, value); }
            case "minecraft:written_book_content"     -> { return applyWrittenBookContent(item, value); }
            case "minecraft:banner_patterns"          -> { return applyBannerPatterns(item, value); }
            case "minecraft:suspicious_stew_effects"  -> { return applySuspiciousStewEffects(item, value); }
            case "minecraft:tool"                     -> { return applyTool(item, value); }
            case "minecraft:rarity"                   -> { return applyRarity(item, value); }
            case "minecraft:item_model"               -> { return applyItemModel(item, value); }
            case "minecraft:equippable"               -> { return applyEquippable(item, value); }
            case "minecraft:consumable"               -> { return applyConsumable(item, value); }
            case "minecraft:use_cooldown"             -> { return applyUseCooldown(item, value); }
            case "minecraft:ominous_bottle_amplifier" -> { return applyOminousAmplifier(item, value); }
            case "minecraft:base_color"               -> { return applyBaseColor(item, value); }
            case "minecraft:instrument"               -> { return applyInstrument(item, value); }
            case "minecraft:jukebox_playable"         -> { return applyJukeboxPlayable(item, value); }
            default -> {
                // Dowolny komponent-znacznik (np. glider, hide_tooltip, intangible_projectile)
                // obslugujemy generycznie: true = dodaj, false = usun.
                if (type instanceof DataComponentType.NonValued nonValued) {
                    applyNonValued(item, nonValued, toBool(value));
                    return true;
                }
                ComponentLogger.warn("Komponent '" + componentName + "' nie jest obslugiwany przy zapisie.");
                return false;
            }
        }
    }

    private static boolean applyLore(ItemStack item, Object value) {
        List<String> lines = toEntries(value);
        if (lines.size() > MAX_LORE_LINES) {
            ComponentLogger.warn("Lore ma " + lines.size() + " linii - przycinam do " + MAX_LORE_LINES + ".");
            lines = lines.subList(0, MAX_LORE_LINES);
        }
        item.setData(DataComponentTypes.LORE, ItemLore.lore(ComponentConverter.parseComponentList(lines)));
        return true;
    }

    private static boolean applyCustomModelData(ItemStack item, Object value) {
        Integer modelData = toInt(value, "custom_model_data");
        if (modelData == null) return false;
        item.setData(DataComponentTypes.CUSTOM_MODEL_DATA,
                CustomModelData.customModelData().addFloat((float) modelData).build());
        return true;
    }

    private static boolean applyMaxStackSize(ItemStack item, Object value) {
        Integer size = toInt(value, "max_stack_size");
        if (size == null) return false;
        size = clamp(size, 1, MAX_STACK_SIZE);
        // Vanilla odrzuca przedmioty jednoczesnie niszczalne i stackowalne - taki przedmiot nie zapisze sie.
        if (size > 1 && item.hasData(DataComponentTypes.MAX_DAMAGE)) {
            ComponentLogger.warn("Nie mozna ustawic max_stack_size > 1 na przedmiocie z max_damage. "
                    + "Najpierw usun komponent minecraft:max_damage.");
            return false;
        }
        item.setData(DataComponentTypes.MAX_STACK_SIZE, size);
        return true;
    }

    private static boolean applyMaxDamage(ItemStack item, Object value) {
        Integer damage = toInt(value, "max_damage");
        if (damage == null) return false;
        Integer stackSize = item.getData(DataComponentTypes.MAX_STACK_SIZE);
        if (stackSize != null && stackSize > 1) {
            ComponentLogger.warn("Nie mozna ustawic max_damage na przedmiocie stackowalnym (max_stack_size="
                    + stackSize + "). Najpierw ustaw minecraft:max_stack_size na 1.");
            return false;
        }
        item.setData(DataComponentTypes.MAX_DAMAGE, Math.max(1, damage));
        return true;
    }

    private static boolean applyNonNegativeInt(ItemStack item, DataComponentType.Valued<Integer> type, Object value, String context) {
        Integer number = toInt(value, context);
        if (number == null) return false;
        item.setData(type, Math.max(0, number));
        return true;
    }

    private static boolean applyUnbreakable(ItemStack item, DataComponentType type, boolean enable) {
        if (type instanceof DataComponentType.NonValued nonValued) {
            applyNonValued(item, nonValued, enable);
            return true;
        }
        if (!enable) {
            removeData(item, type);
            return true;
        }
        if (LEGACY_UNBREAKABLE_VALUE == null) {
            ComponentLogger.warn("minecraft:unbreakable nie jest obslugiwany w tej wersji Paper.");
            return false;
        }
        item.setData((DataComponentType.Valued) type, LEGACY_UNBREAKABLE_VALUE);
        return true;
    }

    private static void applyNonValued(ItemStack item, DataComponentType.NonValued type, boolean enable) {
        if (enable) item.setData(type);
        else removeData(item, type);
    }

    private static boolean applyFood(ItemStack item, Object value) {
        String str = value.toString();
        String[] parts = str.split(":");
        if (parts.length < 2) {
            ComponentLogger.warn("Nieprawidlowy format food '" + str + "'. Uzyj 'nutrition:saturation[:canAlwaysEat]'");
            return false;
        }
        Integer nutrition = toInt(parts[0], "food.nutrition");
        Float saturation = toFiniteFloat(parts[1], "food.saturation");
        if (nutrition == null || saturation == null) return false;
        boolean canAlwaysEat = parts.length >= 3 && Boolean.parseBoolean(parts[2].trim());
        item.setData(DataComponentTypes.FOOD, FoodProperties.food()
                .nutrition(Math.max(0, nutrition))
                .saturation(saturation)
                .canAlwaysEat(canAlwaysEat)
                .build());
        return true;
    }

    private static boolean applyDyedColor(ItemStack item, Object value) {
        String str = value.toString().trim();
        Color color;
        try {
            if (str.startsWith("#")) {
                color = Color.fromRGB(Integer.parseInt(str.substring(1), 16));
            } else if (str.contains(",")) {
                String[] p = str.split(",");
                if (p.length != 3) throw new IllegalArgumentException("oczekiwano R,G,B");
                color = Color.fromRGB(Integer.parseInt(p[0].trim()), Integer.parseInt(p[1].trim()), Integer.parseInt(p[2].trim()));
            } else {
                color = Color.fromRGB(Integer.parseInt(str));
            }
        } catch (IllegalArgumentException e) {
            ComponentLogger.warn("Nieprawidlowy kolor '" + str + "': " + e.getMessage());
            return false;
        }
        // Builder zamiast dyedItemColor(Color, boolean) - ta metoda nie istnieje od Paper 1.21.5.
        item.setData(DataComponentTypes.DYED_COLOR, DyedItemColor.dyedItemColor().color(color).build());
        return true;
    }

    private static boolean applyTrim(ItemStack item, Object value) {
        String str = value.toString().trim().toLowerCase(Locale.ROOT);
        // Obslugiwane formaty: "material:wzor" oraz "minecraft:material:minecraft:wzor"
        String matName;
        String patName;
        String[] parts = str.split(":");
        if (parts.length == 2) {
            matName = parts[0];
            patName = parts[1];
        } else if (parts.length == 4) {
            matName = parts[0] + ":" + parts[1];
            patName = parts[2] + ":" + parts[3];
        } else {
            ComponentLogger.warn("Nieprawidlowy format trim '" + str + "'. Uzyj 'material:wzor'");
            return false;
        }

        NamespacedKey matKey = parseKey(matName);
        NamespacedKey patKey = parseKey(patName);
        TrimMaterial material = matKey == null ? null : Registry.TRIM_MATERIAL.get(matKey);
        TrimPattern pattern = patKey == null ? null : Registry.TRIM_PATTERN.get(patKey);
        if (material == null) { ComponentLogger.warn("Nieznany material trim: " + matName); return false; }
        if (pattern == null)  { ComponentLogger.warn("Nieznany wzor trim: " + patName); return false; }

        item.setData(DataComponentTypes.TRIM, ItemArmorTrim.itemArmorTrim(new ArmorTrim(material, pattern)).build());
        return true;
    }

    private static boolean applyEnchantments(ItemStack item, DataComponentType.Valued<ItemEnchantments> type, Object value) {
        ItemEnchantments.Builder builder = ItemEnchantments.itemEnchantments();
        int added = 0;
        for (String entry : toEntries(value)) {
            int lastColon = entry.lastIndexOf(':');
            if (lastColon < 1) {
                ComponentLogger.warn("Nieprawidlowy format enchantu '" + entry + "'. Uzyj 'nazwa:poziom'");
                continue;
            }
            String enchantName = entry.substring(0, lastColon).trim();
            Integer level = toInt(entry.substring(lastColon + 1), "poziom enchantu");
            if (level == null) continue;

            NamespacedKey key = parseKey(enchantName);
            Enchantment enchant = key == null ? null : Registry.ENCHANTMENT.get(key);
            if (enchant == null) { ComponentLogger.warn("Nieznany enchant: " + enchantName); continue; }
            builder.add(enchant, clamp(level, 1, MAX_ENCHANT_LEVEL));
            added++;
        }
        if (nothingValid(added, "enchantments")) return false;
        item.setData(type, builder.build());
        return true;
    }

    private static boolean applyAttributeModifiers(ItemStack item, Object value) {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.itemAttributes();
        int index = 0;

        for (String entry : toEntries(value)) {
            String[] parts = entry.split(":");
            // Nazwa atrybutu moze miec namespace ("minecraft:attack_damage:add:5") - wtedy jest o jedna czesc wiecej.
            int offset = parts.length >= 4 && resolveAttribute(parts[0] + ":" + parts[1]) != null ? 1 : 0;
            if (parts.length < 3 + offset) {
                ComponentLogger.warn("Nieprawidlowy format atrybutu '" + entry + "'. Uzyj 'atrybut:operacja:wartosc[:slot]'");
                continue;
            }

            String attrName = offset == 1 ? parts[0] + ":" + parts[1] : parts[0];
            String opName   = parts[1 + offset].trim().toLowerCase(Locale.ROOT);
            Double amount   = toFiniteDouble(parts[2 + offset], "atrybutu");
            String slotName = parts.length > 3 + offset ? parts[3 + offset].trim().toLowerCase(Locale.ROOT) : "any";
            if (amount == null) continue;

            Attribute attribute = resolveAttribute(attrName);
            if (attribute == null) {
                ComponentLogger.warn("Nieznany atrybut: '" + attrName + "'. "
                        + "Dostepne formaty: 'attack_damage', 'generic.attack_damage', 'minecraft:attack_damage'");
                continue;
            }

            AttributeModifier.Operation operation = switch (opName) {
                case "add_value", "add", "0"             -> AttributeModifier.Operation.ADD_NUMBER;
                case "add_multiplied_base", "multiply", "1" -> AttributeModifier.Operation.ADD_SCALAR;
                case "add_multiplied_total", "2"         -> AttributeModifier.Operation.MULTIPLY_SCALAR_1;
                default -> null;
            };
            if (operation == null) {
                ComponentLogger.warn("Nieznana operacja atrybutu: " + opName);
                continue;
            }

            EquipmentSlotGroup slotGroup = switch (slotName) {
                case "any"      -> EquipmentSlotGroup.ANY;
                case "mainhand" -> EquipmentSlotGroup.MAINHAND;
                case "offhand"  -> EquipmentSlotGroup.OFFHAND;
                case "hand"     -> EquipmentSlotGroup.HAND;
                case "head"     -> EquipmentSlotGroup.HEAD;
                case "chest"    -> EquipmentSlotGroup.CHEST;
                case "legs"     -> EquipmentSlotGroup.LEGS;
                case "feet"     -> EquipmentSlotGroup.FEET;
                case "armor"    -> EquipmentSlotGroup.ARMOR;
                case "body"     -> EquipmentSlotGroup.BODY;
                default -> {
                    ComponentLogger.warn("Nieznany slot atrybutu '" + slotName + "' - uzywam 'any'.");
                    yield EquipmentSlotGroup.ANY;
                }
            };

            // Klucz budowany z klucza rejestru (zawsze poprawne znaki), a nie z danych wejsciowych.
            String keyPath = attribute.key().value().replace('.', '_') + "_" + (index++) + "_"
                    + UUID.randomUUID().toString().substring(0, 8);
            AttributeModifier modifier = new AttributeModifier(new NamespacedKey("hexvg", keyPath), amount, operation, slotGroup);
            builder.addModifier(attribute, modifier);
        }

        if (nothingValid(index, "attribute_modifiers")) return false;
        item.setData(DataComponentTypes.ATTRIBUTE_MODIFIERS, builder.build());
        return true;
    }

    private static boolean applyPotionContents(ItemStack item, Object value) {
        PotionContents.Builder builder = PotionContents.potionContents();
        int added = 0;

        for (String entry : toEntries(value)) {
            String[] parts = entry.split(":", -1);
            if (parts.length < 2) {
                ComponentLogger.warn("Nieprawidlowy format potion '" + entry + "'");
                continue;
            }
            String kind = parts[0].trim().toLowerCase(Locale.ROOT);

            if (kind.equals("base")) {
                String potionName = entry.substring(entry.indexOf(':') + 1).trim().replace('-', '_');
                NamespacedKey key = parseKey(potionName);
                PotionType potionType = key == null ? null : Registry.POTION.get(key);
                if (potionType == null) {
                    ComponentLogger.warn("Nieznany typ mikstury: '" + potionName
                            + "'. Uzyj np. swiftness, strong_strength, long_fire_resistance");
                    continue;
                }
                builder.potion(potionType);
                added++;

            } else if (kind.equals("effect")) {
                // effect:<nazwa>:<ticki>:<amplifier>[:ambient][:particles]
                // nazwa moze zawierac namespace -> effect:minecraft:speed:200:1
                boolean namespaced = parts.length >= 5 && toIntQuiet(parts[2]) == null;
                int base = namespaced ? 2 : 1;
                if (parts.length < base + 3) {
                    ComponentLogger.warn("Nieprawidlowy format effect '" + entry + "'. Uzyj 'effect:nazwa:ticki:amplifier'");
                    continue;
                }
                String effectName = namespaced ? parts[1] + ":" + parts[2] : parts[1];
                Integer duration  = toInt(parts[base + 1], "czas efektu");
                Integer amplifier = toInt(parts[base + 2], "amplifier efektu");
                if (duration == null || amplifier == null) continue;
                boolean ambient   = parts.length > base + 3 && Boolean.parseBoolean(parts[base + 3].trim());
                boolean particles = parts.length <= base + 4 || Boolean.parseBoolean(parts[base + 4].trim());

                PotionEffectType effectType = resolveEffect(effectName);
                if (effectType == null) continue;

                builder.addCustomEffect(new PotionEffect(effectType,
                        duration < 0 ? PotionEffect.INFINITE_DURATION : duration,
                        clamp(amplifier, 0, MAX_EFFECT_AMPLIFIER), ambient, particles));
                added++;

            } else {
                ComponentLogger.warn("Nieznany typ wpisu potion '" + kind + "'. Uzyj 'base' lub 'effect'");
            }
        }

        if (nothingValid(added, "potion_contents")) return false;
        item.setData(DataComponentTypes.POTION_CONTENTS, builder.build());
        return true;
    }

    private static boolean applyWrittenBookContent(ItemStack item, Object value) {
        String title = "Bez tytulu";
        String author = "Nieznany";
        List<Component> pages = new ArrayList<>();

        for (String entry : toEntries(value)) {
            int colon = entry.indexOf(':');
            String key = colon < 0 ? "" : entry.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            String val = colon < 0 ? entry : entry.substring(colon + 1).trim();
            switch (key) {
                case "title"  -> title = val;
                case "author" -> author = val;
                case "page"   -> pages.add(ComponentConverter.parseComponent(val));
                default       -> pages.add(ComponentConverter.parseComponent(entry));
            }
        }

        if (title.length() > MAX_BOOK_TITLE_LENGTH) {
            ComponentLogger.warn("Tytul ksiazki dluzszy niz " + MAX_BOOK_TITLE_LENGTH + " znaki - przycinam.");
            title = title.substring(0, MAX_BOOK_TITLE_LENGTH);
        }
        if (pages.size() > MAX_BOOK_PAGES) {
            ComponentLogger.warn("Ksiazka ma " + pages.size() + " stron - przycinam do " + MAX_BOOK_PAGES + ".");
            pages = pages.subList(0, MAX_BOOK_PAGES);
        }
        if (pages.isEmpty()) pages.add(Component.empty());

        WrittenBookContent.Builder builder = WrittenBookContent.writtenBookContent(title, author);
        pages.forEach(builder::addPage);
        item.setData(DataComponentTypes.WRITTEN_BOOK_CONTENT, builder.build());
        return true;
    }

    private static boolean applyBannerPatterns(ItemStack item, Object value) {
        BannerPatternLayers.Builder builder = BannerPatternLayers.bannerPatternLayers();
        int added = 0;

        for (String entry : toEntries(value)) {
            // wzor:kolor lub minecraft:wzor:kolor
            int lastColon = entry.lastIndexOf(':');
            if (lastColon < 1) {
                ComponentLogger.warn("Nieprawidlowy format banner_patterns '" + entry + "'. Uzyj 'wzor:kolor'");
                continue;
            }
            String patName   = entry.substring(0, lastColon).trim();
            String colorName = entry.substring(lastColon + 1).trim();

            NamespacedKey patKey = parseKey(patName);
            PatternType patternType = patKey == null ? null : Registry.BANNER_PATTERN.get(patKey);
            if (patternType == null) { ComponentLogger.warn("Nieznany wzor banneru: " + patName); continue; }

            DyeColor dyeColor = parseEnum(DyeColor.class, colorName);
            if (dyeColor == null) { ComponentLogger.warn("Nieznany kolor banneru: " + colorName); continue; }

            builder.add(new Pattern(dyeColor, patternType));
            added++;
        }

        if (nothingValid(added, "banner_patterns")) return false;
        item.setData(DataComponentTypes.BANNER_PATTERNS, builder.build());
        return true;
    }

    private static boolean applySuspiciousStewEffects(ItemStack item, Object value) {
        SuspiciousStewEffects.Builder builder = SuspiciousStewEffects.suspiciousStewEffects();
        int added = 0;

        for (String entry : toEntries(value)) {
            // efekt:ticki lub minecraft:efekt:ticki
            int lastColon = entry.lastIndexOf(':');
            if (lastColon < 1) {
                ComponentLogger.warn("Nieprawidlowy format suspicious_stew_effects '" + entry + "'. Uzyj 'efekt:ticki'");
                continue;
            }
            Integer duration = toInt(entry.substring(lastColon + 1), "czas efektu");
            PotionEffectType effectType = resolveEffect(entry.substring(0, lastColon));
            if (duration == null || effectType == null) continue;
            builder.add(SuspiciousEffectEntry.create(effectType, Math.max(0, duration)));
            added++;
        }

        if (nothingValid(added, "suspicious_stew_effects")) return false;
        item.setData(DataComponentTypes.SUSPICIOUS_STEW_EFFECTS, builder.build());
        return true;
    }

    private static boolean applyTool(ItemStack item, Object value) {
        Tool.Builder builder = Tool.tool();
        int added = 0;

        for (String entry : toEntries(value)) {
            int colon = entry.indexOf(':');
            if (colon < 0) {
                ComponentLogger.warn("Nieprawidlowy wpis tool '" + entry + "'. Uzyj 'speed:', 'damage:' lub 'rule:'");
                continue;
            }
            String key  = entry.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            String rest = entry.substring(colon + 1).trim();

            switch (key) {
                case "speed" -> {
                    Float speed = toFiniteFloat(rest, "tool.speed");
                    if (speed != null) { builder.defaultMiningSpeed(speed); added++; }
                }
                case "damage" -> {
                    Integer damage = toInt(rest, "tool.damage");
                    if (damage != null) { builder.damagePerBlock(Math.max(0, damage)); added++; }
                }
                case "rule" -> {
                    // rule:<blok1,blok2 | #tag>:<predkosc | default>:<true | false | default>
                    String[] ruleParts = rest.split(":");
                    if (ruleParts.length < 3) {
                        ComponentLogger.warn("Nieprawidlowy format rule '" + entry + "'. Uzyj 'rule:bloki:predkosc:true/false'");
                        continue;
                    }
                    String speedStr = ruleParts[ruleParts.length - 2].trim();
                    Float speed = null;
                    if (!speedStr.equalsIgnoreCase("default")) {
                        speed = toFiniteFloat(speedStr, "tool.rule.speed");
                        if (speed == null) continue;
                        if (speed <= 0) {
                            ComponentLogger.warn("Predkosc w rule musi byc wieksza od 0: " + entry);
                            continue;
                        }
                    }
                    TriState correctForDrops = switch (ruleParts[ruleParts.length - 1].trim().toLowerCase(Locale.ROOT)) {
                        case "true" -> TriState.TRUE;
                        case "false" -> TriState.FALSE;
                        default -> TriState.NOT_SET;
                    };
                    String blocks = String.join(":", java.util.Arrays.copyOf(ruleParts, ruleParts.length - 2)).trim();

                    RegistryKeySet<BlockType> keySet = parseBlockSet(blocks);
                    if (keySet == null) continue;
                    builder.addRule(Tool.rule(keySet, speed, correctForDrops));
                    added++;
                }
                default -> ComponentLogger.warn("Nieznany klucz tool: " + key);
            }
        }

        if (nothingValid(added, "tool")) return false;
        item.setData(DataComponentTypes.TOOL, builder.build());
        return true;
    }

    /** "#minecraft:mineable/pickaxe" -> tag bloków, "stone,minecraft:dirt" -> lista bloków. */
    private static @Nullable RegistryKeySet<BlockType> parseBlockSet(String blocks) {
        if (blocks.startsWith("#")) {
            NamespacedKey tagKey = parseKey(blocks.substring(1));
            if (tagKey == null) { ComponentLogger.warn("Nieprawidlowy klucz tagu: " + blocks); return null; }
            Registry<BlockType> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.BLOCK);
            TagKey<BlockType> key = TagKey.create(RegistryKey.BLOCK, tagKey);
            if (!registry.hasTag(key)) { ComponentLogger.warn("Nieznany tag blokow: " + blocks); return null; }
            return registry.getTag(key);
        }

        List<TypedKey<BlockType>> keys = new ArrayList<>();
        for (String blockName : blocks.split(",")) {
            Material mat = Material.matchMaterial(blockName.trim());
            if (mat == null) { ComponentLogger.warn("Nieznany material: " + blockName.trim()); continue; }
            if (!mat.isBlock()) { ComponentLogger.warn("Material nie jest blokiem: " + blockName.trim()); continue; }
            keys.add(TypedKey.create(RegistryKey.BLOCK, mat.getKey()));
        }
        return keys.isEmpty() ? null : RegistrySet.keySet(RegistryKey.BLOCK, keys);
    }

    private static boolean applyRarity(ItemStack item, Object value) {
        ItemRarity rarity = parseEnum(ItemRarity.class, value.toString());
        if (rarity == null) {
            ComponentLogger.warn("Nieznana rzadkosc: " + value + ". Dostepne: common, uncommon, rare, epic");
            return false;
        }
        item.setData(DataComponentTypes.RARITY, rarity);
        return true;
    }

    private static boolean applyItemModel(ItemStack item, Object value) {
        NamespacedKey modelKey = parseKey(value.toString());
        if (modelKey == null) {
            ComponentLogger.warn("Nieprawidlowy klucz item_model: " + value);
            return false;
        }
        item.setData(DataComponentTypes.ITEM_MODEL, modelKey);
        return true;
    }

    private static boolean applyEquippable(ItemStack item, Object value) {
        EquipmentSlot slot = EquipmentSlot.HEAD;
        NamespacedKey sound = null;
        NamespacedKey cameraOverlay = null;
        NamespacedKey assetId = null;
        boolean swappable = true;
        boolean dispensable = true;
        int errors = 0;

        for (String entry : toEntries(value)) {
            int colon = entry.indexOf(':');
            if (colon < 0) { ComponentLogger.warn("Nieprawidlowy wpis equippable: " + entry); errors++; continue; }
            String key = entry.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            String val = entry.substring(colon + 1).trim();

            switch (key) {
                case "slot" -> {
                    EquipmentSlot parsed = switch (val.toLowerCase(Locale.ROOT)) {
                        case "head"     -> EquipmentSlot.HEAD;
                        case "chest"    -> EquipmentSlot.CHEST;
                        case "legs"     -> EquipmentSlot.LEGS;
                        case "feet"     -> EquipmentSlot.FEET;
                        case "mainhand" -> EquipmentSlot.HAND;
                        case "offhand"  -> EquipmentSlot.OFF_HAND;
                        case "body"     -> EquipmentSlot.BODY;
                        default         -> parseEnum(EquipmentSlot.class, val);
                    };
                    if (parsed == null) { ComponentLogger.warn("Nieznany slot equippable: " + val); errors++; }
                    else slot = parsed;
                }
                case "sound"          -> { sound = parseKey(val); if (sound == null) errors += invalidKey("equippable.sound", val); }
                case "camera_overlay" -> { cameraOverlay = parseKey(val); if (cameraOverlay == null) errors += invalidKey("equippable.camera_overlay", val); }
                case "asset_id"       -> { assetId = parseKey(val); if (assetId == null) errors += invalidKey("equippable.asset_id", val); }
                case "swappable"      -> swappable = Boolean.parseBoolean(val);
                case "dispensable"    -> dispensable = Boolean.parseBoolean(val);
                default -> { ComponentLogger.warn("Nieznany klucz equippable: " + key); errors++; }
            }
        }
        if (hasErrors(errors, "equippable")) return false;

        Equippable.Builder builder = Equippable.equippable(slot).swappable(swappable).dispensable(dispensable);
        if (sound != null) builder.equipSound(sound);
        if (cameraOverlay != null) builder.cameraOverlay(cameraOverlay);
        if (assetId != null) builder.assetId(assetId);
        item.setData(DataComponentTypes.EQUIPPABLE, builder.build());
        return true;
    }

    private static boolean applyConsumable(ItemStack item, Object value) {
        float consumeTime = 1.6f;
        ItemUseAnimation animation = ItemUseAnimation.EAT;
        NamespacedKey sound = null;
        boolean hasParticles = true;
        int errors = 0;

        for (String entry : toEntries(value)) {
            int colon = entry.indexOf(':');
            if (colon < 0) { ComponentLogger.warn("Nieprawidlowy wpis consumable: " + entry); errors++; continue; }
            String key = entry.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            String val = entry.substring(colon + 1).trim();

            switch (key) {
                case "time" -> {
                    Float time = toFiniteFloat(val, "consumable.time");
                    if (time == null) errors++;
                    else consumeTime = Math.max(0f, time);
                }
                case "animation" -> {
                    ItemUseAnimation parsed = parseEnum(ItemUseAnimation.class, val);
                    if (parsed == null) { ComponentLogger.warn("Nieznana animacja consumable: " + val); errors++; }
                    else animation = parsed;
                }
                case "sound"     -> { sound = parseKey(val); if (sound == null) errors += invalidKey("consumable.sound", val); }
                case "particles" -> hasParticles = Boolean.parseBoolean(val);
                default -> { ComponentLogger.warn("Nieznany klucz consumable: " + key); errors++; }
            }
        }
        if (hasErrors(errors, "consumable")) return false;

        Consumable.Builder builder = Consumable.consumable()
                .consumeSeconds(consumeTime)
                .animation(animation)
                .hasConsumeParticles(hasParticles);
        if (sound != null) builder.sound(sound);
        item.setData(DataComponentTypes.CONSUMABLE, builder.build());
        return true;
    }

    private static boolean applyUseCooldown(ItemStack item, Object value) {
        float cooldownTime = 1.0f;
        NamespacedKey group = null;
        int errors = 0;

        for (String entry : toEntries(value)) {
            int colon = entry.indexOf(':');
            if (colon < 0) {
                // Sama liczba = czas cooldownu
                Float time = toFiniteFloat(entry, "use_cooldown.time");
                if (time == null) errors++;
                else cooldownTime = time;
                continue;
            }
            String key = entry.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            String val = entry.substring(colon + 1).trim();

            switch (key) {
                case "time" -> {
                    Float time = toFiniteFloat(val, "use_cooldown.time");
                    if (time == null) errors++;
                    else cooldownTime = time;
                }
                case "group" -> { group = parseKey(val); if (group == null) errors += invalidKey("use_cooldown.group", val); }
                default -> { ComponentLogger.warn("Nieznany klucz use_cooldown: " + key); errors++; }
            }
        }
        if (hasErrors(errors, "use_cooldown")) return false;

        if (cooldownTime <= 0) {
            ComponentLogger.warn("Czas use_cooldown musi byc wiekszy od 0.");
            return false;
        }
        UseCooldown.Builder builder = UseCooldown.useCooldown(cooldownTime);
        if (group != null) builder.cooldownGroup(group);
        item.setData(DataComponentTypes.USE_COOLDOWN, builder.build());
        return true;
    }

    private static boolean applyOminousAmplifier(ItemStack item, Object value) {
        Integer amplifier = toInt(value, "ominous_bottle_amplifier");
        if (amplifier == null) return false;
        item.setData(DataComponentTypes.OMINOUS_BOTTLE_AMPLIFIER,
                OminousBottleAmplifier.amplifier(clamp(amplifier, 0, MAX_OMINOUS_AMPLIFIER)));
        return true;
    }

    private static boolean applyBaseColor(ItemStack item, Object value) {
        DyeColor dyeColor = parseEnum(DyeColor.class, value.toString());
        if (dyeColor == null) {
            ComponentLogger.warn("Nieznany kolor base_color: " + value);
            return false;
        }
        item.setData(DataComponentTypes.BASE_COLOR, dyeColor);
        return true;
    }

    private static boolean applyInstrument(ItemStack item, Object value) {
        NamespacedKey key = parseKey(value.toString());
        MusicInstrument instrument = key == null ? null : Registry.INSTRUMENT.get(key);
        if (instrument == null) {
            ComponentLogger.warn("Nieznany instrument: " + value);
            return false;
        }
        item.setData(DataComponentTypes.INSTRUMENT, instrument);
        return true;
    }

    private static boolean applyJukeboxPlayable(ItemStack item, Object value) {
        NamespacedKey key = parseKey(value.toString());
        JukeboxSong song = key == null ? null : Registry.JUKEBOX_SONG.get(key);
        if (song == null) {
            ComponentLogger.warn("Nieznany utwor jukebox: " + value);
            return false;
        }
        item.setData(DataComponentTypes.JUKEBOX_PLAYABLE, JukeboxPlayable.jukeboxPlayable(song).build());
        return true;
    }

    // ------------------------------------------------------------------
    //  Pomocnicze
    // ------------------------------------------------------------------

    private static boolean isEmpty(@Nullable ItemStack item) {
        return item == null || item.getType().isAir();
    }

    private static boolean hasListeners(HandlerList handlers) {
        return handlers.getRegisteredListeners().length > 0;
    }

    /**
     * Przywraca wartosc domyslna, a jesli domyslny prototyp przedmiotu nadal ma komponent
     * (np. jedzenie jablka) - usuwa go jawnie. Dzieki temu nie dodajemy zbednych wpisow
     * "!komponent" do przedmiotow, ktore go nie mialy (zachowana stackowalnosc).
     */
    private static void removeData(ItemStack item, DataComponentType type) {
        item.resetData(type);
        if (item.hasData(type)) item.unsetData(type);
    }

    /** Gdy zaden wpis nie byl poprawny, nie nadpisujemy komponentu pusta wartoscia (np. miecz nie traci obrazen). */
    private static boolean nothingValid(int added, String componentName) {
        if (added > 0) return false;
        ComponentLogger.warn("Zaden wpis dla '" + componentName + "' nie byl poprawny - komponent nie zostal zmieniony.");
        return true;
    }

    /** Komponenty typu klucz:wartosc - jeden bledny wpis blokuje zapis, zamiast po cichu uzyc wartosci domyslnej. */
    private static boolean hasErrors(int errors, String componentName) {
        if (errors == 0) return false;
        ComponentLogger.warn("'" + componentName + "' zawiera bledne wpisy (" + errors + ") - komponent nie zostal zmieniony.");
        return true;
    }

    private static int invalidKey(String context, String value) {
        ComponentLogger.warn("Nieprawidlowy klucz dla " + context + ": " + value);
        return 1;
    }

    private static List<String> toEntries(Object value) {
        if (value instanceof List<?> list) {
            List<String> result = new ArrayList<>(list.size());
            for (Object o : list) if (o != null) result.add(o.toString());
            return result;
        }
        return List.of(value.toString());
    }

    /** Bezpieczne parsowanie klucza - zwraca null zamiast rzucac wyjatkiem dla niepoprawnych znakow. */
    private static @Nullable NamespacedKey parseKey(String name) {
        if (name == null) return null;
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) return null;
        return NamespacedKey.fromString(normalized);
    }

    private static <E extends Enum<E>> @Nullable E parseEnum(Class<E> enumClass, String name) {
        if (name == null) return null;
        try {
            return Enum.valueOf(enumClass, name.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_'));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static @Nullable PotionEffectType resolveEffect(String name) {
        NamespacedKey key = parseKey(name);
        PotionEffectType type = key == null ? null : Registry.EFFECT.get(key);
        if (type == null) ComponentLogger.warn("Nieznany efekt: " + name);
        return type;
    }

    private static @Nullable Attribute resolveAttribute(String input) {
        String name = input.trim().toLowerCase(Locale.ROOT);
        Set<String> candidates = new LinkedHashSet<>();

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
            if (!name.equals(bare)) candidates.add("minecraft:" + name);
        }

        for (String candidate : candidates) {
            NamespacedKey key = NamespacedKey.fromString(candidate);
            if (key == null) continue;
            Attribute attr = Registry.ATTRIBUTE.get(key);
            if (attr != null) return attr;
        }
        return null;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static @Nullable Integer toIntQuiet(Object value) {
        if (value instanceof Number n) {
            double d = n.doubleValue();
            if (!Double.isFinite(d)) return null;
            return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, d));
        }
        String str = value.toString().trim();
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            try {
                double d = Double.parseDouble(str);
                if (!Double.isFinite(d)) return null;
                return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, d));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
    }

    private static @Nullable Integer toInt(Object value, String context) {
        Integer result = toIntQuiet(value);
        if (result == null) ComponentLogger.warn("Nieprawidlowa liczba calkowita dla " + context + ": " + value);
        return result;
    }

    /** Odrzuca NaN i nieskonczonosc - takie wartosci psuja atrybuty/predkosci i zapis przedmiotu. */
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

    private static @Nullable Float toFiniteFloat(Object value, String context) {
        Double d = toFiniteDouble(value, context);
        if (d == null) return null;
        if (Math.abs(d) > Float.MAX_VALUE) {
            ComponentLogger.warn("Wartosc " + context + " poza zakresem: " + value);
            return null;
        }
        return d.floatValue();
    }

    private static boolean toBool(Object value) {
        if (value instanceof Boolean b) return b;
        if (value instanceof Number n) return n.doubleValue() != 0;
        String str = value.toString().trim();
        return str.equalsIgnoreCase("true") || str.equalsIgnoreCase("yes") || str.equals("1");
    }
}
