package com.venomgrave.hexvg.datacomponents.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.venomgrave.hexvg.datacomponents.handlers.ItemComponentHandler;
import com.venomgrave.hexvg.datacomponents.utils.ComponentConverter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class ExprDataComponent extends SimpleExpression<Object> {

    static {
        Skript.registerExpression(
                ExprDataComponent.class,
                Object.class,
                ExpressionType.COMBINED,
                "data component %string% of %itemstack%",
                "[all] data component values %string% of %itemstack%"
        );
    }

    private Expression<String> componentName;
    private Expression<ItemStack> item;
    private boolean plural;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        componentName = (Expression<String>) exprs[0];
        item = (Expression<ItemStack>) exprs[1];
        plural = matchedPattern == 1;
        return true;
    }

    @Override
    protected Object[] get(Event event) {
        String name = componentName.getSingle(event);
        ItemStack stack = item.getSingle(event);
        if (name == null || stack == null) return new Object[0];
        return ItemComponentHandler.read(stack, name)
                .map(val -> plural ? ComponentConverter.toValueArray(val) : new Object[]{ComponentConverter.toSingleValue(val)})
                .orElse(new Object[0]);
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        if (mode != Changer.ChangeMode.SET && mode != Changer.ChangeMode.DELETE) return null;
        // Zmiana komponentu wymaga zapisania zmodyfikowanej kopii przedmiotu z powrotem.
        if (!Changer.ChangerUtils.acceptsChange(item, Changer.ChangeMode.SET, ItemStack.class)) return null;
        return new Class<?>[]{Object[].class};
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        String name = componentName.getSingle(event);
        ItemStack stack = item.getSingle(event);
        if (name == null || stack == null) return;

        Player player = ItemComponentHandler.extractPlayer(event);
        ItemStack modified = stack.clone();
        boolean changed = false;

        if (mode == Changer.ChangeMode.SET) {
            if (delta == null || delta.length == 0) return;
            Object finalValue = delta.length == 1 ? delta[0] : Arrays.asList(delta);
            changed = ItemComponentHandler.write(modified, name, finalValue, player);
        } else if (mode == Changer.ChangeMode.DELETE) {
            changed = ItemComponentHandler.remove(modified, name, player);
        }

        if (changed) item.change(event, new ItemStack[]{modified}, Changer.ChangeMode.SET);
    }

    @Override
    public boolean isSingle() { return !plural; }

    @Override
    public Class<Object> getReturnType() { return Object.class; }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return (plural ? "data component values " : "data component ")
                + componentName.toString(event, debug) + " of " + item.toString(event, debug);
    }
}
