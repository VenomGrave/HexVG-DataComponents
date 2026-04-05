package com.venomgrave.hexvg.datacomponents.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.venomgrave.hexvg.datacomponents.handlers.ItemComponentHandler;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class ExprItemWithComponent extends SimpleExpression<ItemStack> {

    static {
        Skript.registerExpression(
                ExprItemWithComponent.class,
                ItemStack.class,
                ExpressionType.COMBINED,
                "%itemstack% with data component %string% set to %object%"
        );
    }

    private Expression<ItemStack> item;
    private Expression<String> componentName;
    private Expression<Object> value;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        item = (Expression<ItemStack>) exprs[0];
        componentName = (Expression<String>) exprs[1];
        value = (Expression<Object>) exprs[2];
        return true;
    }

    @Override
    @Nullable
    protected ItemStack[] get(Event event) {
        ItemStack stack = item.getSingle(event);
        String name = componentName.getSingle(event);
        Object val = value.getSingle(event);
        if (stack == null || name == null || val == null) return new ItemStack[0];
        ItemStack copy = stack.clone();
        ItemComponentHandler.write(copy, name, val, null);
        return new ItemStack[]{copy};
    }

    @Override
    public boolean isSingle() { return true; }

    @Override
    public Class<ItemStack> getReturnType() { return ItemStack.class; }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return item.toString(event, debug)
                + " with data component " + componentName.toString(event, debug)
                + " set to " + value.toString(event, debug);
    }
}
