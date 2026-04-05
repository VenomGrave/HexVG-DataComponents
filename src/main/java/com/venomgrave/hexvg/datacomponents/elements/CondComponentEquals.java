package com.venomgrave.hexvg.datacomponents.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import com.venomgrave.hexvg.datacomponents.handlers.ItemComponentHandler;
import com.venomgrave.hexvg.datacomponents.utils.ComponentConverter;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class CondComponentEquals extends Condition {

    static {
        Skript.registerCondition(
                CondComponentEquals.class,
                "data component %string% of %itemstack% (is|equals) %object%",
                "data component %string% of %itemstack% (isn't|is not|does not equal) %object%"
        );
    }

    private Expression<String> componentName;
    private Expression<ItemStack> item;
    private Expression<Object> expected;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        componentName = (Expression<String>) exprs[0];
        item = (Expression<ItemStack>) exprs[1];
        expected = (Expression<Object>) exprs[2];
        setNegated(matchedPattern == 1);
        return true;
    }

    @Override
    public boolean check(Event event) {
        String name = componentName.getSingle(event);
        ItemStack stack = item.getSingle(event);
        Object expectedVal = expected.getSingle(event);
        if (name == null || stack == null || expectedVal == null) return isNegated();
        Optional<Object> actual = ItemComponentHandler.read(stack, name);
        if (actual.isEmpty()) return isNegated();
        boolean equals = ComponentConverter.toDisplayString(actual.get())
                .equals(ComponentConverter.toDisplayString(expectedVal));
        return isNegated() != equals;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "data component " + componentName.toString(event, debug)
                + " of " + item.toString(event, debug)
                + (isNegated() ? " is not " : " is ")
                + expected.toString(event, debug);
    }
}
