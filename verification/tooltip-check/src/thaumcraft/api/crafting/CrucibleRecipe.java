package thaumcraft.api.crafting;
public class CrucibleRecipe {
    public final Object catalyst;
    private final net.minecraft.item.ItemStack output;
    public CrucibleRecipe(Object catalyst, net.minecraft.item.ItemStack output) { this.catalyst = catalyst; this.output = output; }
    public net.minecraft.item.ItemStack getRecipeOutput() { return output.copy(); }
}
