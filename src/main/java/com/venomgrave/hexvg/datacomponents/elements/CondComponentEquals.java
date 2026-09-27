package com.venomgrave.hexvg.datacomponents.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.LiteralUtils;
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
        // %object% zostawia literaly (np. 1, true) nieprzetworzone - trzeba je rozwiazac przed uzyciem.
        expected = LiteralUtils.defendExpression(exprs[2]);
        setNegated(matchedPattern == 1);
        return LiteralUtils.canInitSafely(expected);
    }

    @Override
    public boolean check(Event event) {
        String name = componentName.getSingle(event);
        ItemStack stack = item.getSingle(event);
        Object expectedVal = ComponentConverter.toSingleValue(expected.getSingle(event));
        if (name == null || stack == null || expectedVal == null) return isNegated();
        Optional<Object> actual = ItemComponentHandler.read(stack, name);
        if (actual.isEmpty()) return isNegated();
        Object actualVal = ComponentConverter.toSingleValue(actual.get());
        // Liczby porownujemy numerycznie (5 == 5.0), reszte jako tekst.
        boolean equals = actualVal instanceof Number a && expectedVal instanceof Number b
                ? Double.compare(a.doubleValue(), b.doubleValue()) == 0
                : ComponentConverter.toDisplayString(actualVal).equals(ComponentConverter.toDisplayString(expectedVal));
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
