package com.venomgrave.hexvg.datacomponents.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UnstableApiUsage")
public class ExprComponentCount extends SimpleExpression<Number> {

    static {
        Skript.registerExpression(
                ExprComponentCount.class,
                Number.class,
                ExpressionType.COMBINED,
                "[the] data component count of %itemstack%",
                "[the] number of data components of %itemstack%"
        );
    }

    private Expression<ItemStack> item;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        item = (Expression<ItemStack>) exprs[0];
        return true;
    }

    @Override
    @Nullable
    protected Number[] get(Event event) {
        ItemStack stack = item.getSingle(event);
        if (stack == null || stack.getType().isAir()) return new Number[]{0};
        try {
            return new Number[]{stack.getDataTypes().size()};
        } catch (Exception e) {
            return new Number[]{0};
        }
    }

    @Override
    public boolean isSingle() { return true; }

    @Override
    public Class<Number> getReturnType() { return Number.class; }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "data component count of " + item.toString(event, debug);
    }
}
