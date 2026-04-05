package com.venomgrave.hexvg.datacomponents.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import com.venomgrave.hexvg.datacomponents.handlers.ItemComponentHandler;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class CondHasDataComponent extends Condition {

    static {
        Skript.registerCondition(
                CondHasDataComponent.class,
                "%itemstack% has data component %string%",
                "%itemstack% (doesn't have|does not have) data component %string%"
        );
    }

    private Expression<ItemStack> item;
    private Expression<String> componentName;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        item = (Expression<ItemStack>) exprs[0];
        componentName = (Expression<String>) exprs[1];
        setNegated(matchedPattern == 1);
        return true;
    }

    @Override
    public boolean check(Event event) {
        ItemStack stack = item.getSingle(event);
        String name = componentName.getSingle(event);
        if (stack == null || name == null) return isNegated();
        boolean has = ItemComponentHandler.hasComponent(stack, name);
        return isNegated() != has;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return item.toString(event, debug)
                + (isNegated() ? " does not have" : " has")
                + " data component " + componentName.toString(event, debug);
    }
}
