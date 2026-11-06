package gregtech.api.recipe;
import java.util.*;
import gregtech.api.util.GTRecipe;
/** Isolated recipe-table fixture with the installed GT API's verified public signatures. */
public final class RecipeMap {
    public static final Map<String, RecipeMap> ALL_RECIPE_MAPS = new HashMap<>();
    public final List<GTRecipe> recipes = new ArrayList<>();
    public Collection<GTRecipe> getAllRecipes() { return recipes; }
}
