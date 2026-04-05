package com.venomgrave.hexvg.datacomponents.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import com.venomgrave.hexvg.datacomponents.handlers.ItemComponentHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class EffSetDataComponent extends Effect {

    static {
        Skript.registerEffect(
                EffSetDataComponent.class,
                "set data component %string% of %itemstack% to %strings%",
                "set data component %string% of %itemstack% to %number%",
                "set data component %string% of %itemstack% to %boolean%"
        );
    }

    private Expression<String> componentName;
    private Expression<ItemStack> item;
    private Expression<?> value;
    private int pattern;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        componentName = (Expression<String>) exprs[0];
        item = (Expression<ItemStack>) exprs[1];
        value = exprs[2];
        pattern = matchedPattern;
        return true;
    }

    @Override
    protected void execute(Event event) {
        String name = componentName.getSingle(event);
        ItemStack stack = item.getSingle(event);
        if (name == null || stack == null) return;

        Player player = ItemComponentHandler.extractPlayer(event);

        Object finalValue = resolveValue(event);
        if (finalValue == null) return;

        ItemStack modified = stack.clone();
        ItemComponentHandler.write(modified, name, finalValue, player);
        item.change(event, new ItemStack[]{modified}, Changer.ChangeMode.SET);
    }

    @SuppressWarnings("unchecked")
    private Object resolveValue(Event event) {
        return switch (pattern) {
            case 0 -> {
                String[] strings = ((Expression<String>) value).getAll(event);
                yield (strings == null || strings.length == 0) ? null
                        : strings.length == 1 ? strings[0] : Arrays.asList(strings);
            }
            case 1 -> (Number) value.getSingle(event);
            case 2 -> (Boolean) value.getSingle(event);
            default -> null;
        };
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "set data component " + componentName.toString(event, debug)
                + " of " + item.toString(event, debug)
                + " to " + value.toString(event, debug);
    }
}
