package gregtech.api.util;
import java.util.*;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
public final class GTRecipe {
    public ItemStack[] mInputs, mOutputs;
    public FluidStack[] mFluidInputs, mFluidOutputs;
    public GTRecipe(ItemStack[] inputs, ItemStack[] outputs) { mInputs = inputs; mOutputs = outputs; }
    public static final class RecipeAssemblyLine {
        public static final ArrayList<RecipeAssemblyLine> sAssemblylineRecipes = new ArrayList<>();
        public ItemStack[] mInputs;
        public FluidStack[] mFluidInputs;
        public ItemStack mOutput;
    }
}
