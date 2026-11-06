package nerflens;

import java.util.Collections;
import java.util.List;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.Badge;
import codechicken.nei.recipe.IRecipeHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Yellow identifies this recipe's changed inputs; global gold follows products with changed material inputs.
 */
@SideOnly(Side.CLIENT)
public final class NerfBadges {

    private NerfBadges() {}

    public static List<Badge> forRecipe(IRecipeHandler handler, int index, PositionedStack stack, boolean input) {
        if (stack == null || stack.item == null) return Collections.emptyList();
        for (NerfDB.RecipeEntry recipe : NerfRecipes.match(handler, index)) {
            if (!input || NerfRecipes.changedInput(recipe, stack)) {
                return Collections.singletonList(
                    new FrameBadge(
                        input ? 0xFFFFDE59 : 0xFFFFB82E,
                        "ExtraNerf · " + NerfText.tr(input ? "frame.input" : "frame.output")));
            }
        }
        if (NerfItems.affected(stack.item)) {
            return Collections
                .singletonList(new FrameBadge(0xFFFFB82E, "ExtraNerf · " + NerfText.tr("frame.affected")));
        }
        return Collections.emptyList();
    }
}
