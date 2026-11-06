package nerflens;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.item.ItemStack;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.StackInfo;

/** Display-only cards built from copies of the current recipe's real stacks. */
public final class NerfVisuals {

    public static final class Card {

        public String label, kind, before, after, note;
        public ItemStack previous, current;
    }

    private NerfVisuals() {}

    public static List<Card> cards(NerfDB.RecipeEntry recipe, IRecipeHandler handler, int index) {
        List<Card> out = new ArrayList<>();
        if (recipe.allInputsChanged) {
            for (PositionedStack position : NerfRecipes.recipeStacks(handler, index, false)) {
                if (position.item == null || StackInfo.getAmount(position.item) <= 0
                    || StackInfo.getFluid(position.item) != null) continue;
                Card card = new Card();
                card.label = NerfText.tr("scale");
                card.kind = "item";
                card.current = position.item.copy();
                card.previous = position.item.copy();
                long amount = StackInfo.getAmount(position.item);
                card.before = amount < 64 && amount % 6 == 0 ? String.valueOf(amount / 6) : "?";
                card.after = String.valueOf(amount);
                card.note = NerfText.tr(amount >= 64 ? "scale.capped" : "scale.note");
                out.add(card);
            }
            return out;
        }
        for (NerfDB.VisualChange change : recipe.visualChanges) {
            if (change == null) continue;
            Card card = new Card();
            card.label = NerfText.pick(change.label, change.labelEn);
            card.kind = change.kind;
            card.before = NerfText.pick(change.before, change.beforeEn);
            card.after = NerfText.pick(change.after, change.afterEn);
            card.note = "";
            if (change.stack != null) {
                List<PositionedStack> stacks = NerfRecipes.recipeStacks(handler, index, change.output);
                card.current = NerfRecipes.sampleStack(stacks, change.stack);
                if (card.after.isEmpty()) {
                    long amount = NerfRecipes.amount(stacks, change.stack);
                    card.after = amount < 0 ? "?" : String.valueOf(amount);
                    if (amount == 0 && !change.output) {
                        card.after = NerfText.tr("tool.present");
                        card.note = NerfText.tr("tool.note");
                    }
                }
                if (card.before.equals("=")) card.before = card.after;
                if (change.oldStack != null) card.previous = NerfRecipes.displayStack(change.oldStack);
                else if (!change.oldPrefix.isEmpty() || !change.oldSuffix.isEmpty()) card.previous = NerfRecipes
                    .previousShape(card.current, change.stack, change.oldPrefix, change.oldSuffix);
                else if (!card.before.equals("0") && card.current != null) card.previous = card.current.copy();
                if (change.stack.fluidGt != null || change.stack.f != null || change.stack.fany != null) {
                    if (card.before.matches("[0-9]+")) card.before += " mB";
                    if (card.after.matches("[0-9]+")) card.after += " mB";
                }
            }
            out.add(card);
        }
        if (out.isEmpty()) for (NerfDB.StackSpec selector : recipe.changedInputs) {
            Card card = new Card();
            List<PositionedStack> inputs = NerfRecipes.recipeStacks(handler, index, false);
            card.label = NerfText.tr("input");
            card.kind = "item";
            card.before = "?";
            card.note = "";
            card.current = NerfRecipes.sampleStack(inputs, selector);
            card.after = String.valueOf(NerfRecipes.amount(inputs, selector));
            out.add(card);
        }
        if (out.isEmpty()) {
            Card card = new Card();
            card.label = NerfText.tr("route");
            card.kind = "route";
            card.before = NerfText.tr("old");
            card.after = NerfText.tr("new");
            card.note = "";
            out.add(card);
        }
        return out;
    }

    public static String compact(String value) {
        if (value == null) return "?";
        String number = value.replace(" mB", "");
        try {
            long amount = Long.parseLong(number);
            if (amount >= 1000000) return String.format(Locale.ROOT, "%.1fM", amount / 1000000.0);
            if (amount >= 10000) return String.format(Locale.ROOT, "%.1fk", amount / 1000.0);
        } catch (NumberFormatException ignored) {}
        return number;
    }
}
