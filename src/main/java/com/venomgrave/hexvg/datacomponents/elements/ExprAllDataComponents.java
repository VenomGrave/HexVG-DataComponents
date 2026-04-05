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

@SuppressWarnings("UnstableApiUsage")
public class ExprAllDataComponents extends SimpleExpression<String> {

    static {
        Skript.registerExpression(
                ExprAllDataComponents.class,
                String.class,
                ExpressionType.COMBINED,
                "all data components of %itemstack%",
                "data components of %itemstack%"
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
    protected String[] get(Event event) {
        ItemStack stack = item.getSingle(event);
        if (stack == null || stack.getType().isAir()) return new String[0];
        try {
            return stack.getDataTypes().stream()
                    .map(t -> t.key().asString())
                    .toArray(String[]::new);
        } catch (Exception e) {
            return new String[0];
        }
    }

    @Override
    public boolean isSingle() { return false; }

    @Override
    public Class<String> getReturnType() { return String.class; }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "all data components of " + item.toString(event, debug);
    }
}
