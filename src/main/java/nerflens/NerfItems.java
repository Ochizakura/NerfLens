package nerflens;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import codechicken.nei.PositionedStack;
import codechicken.nei.item.ItemFluidDisplay;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Product records and global frames for changed material inputs, independent of the current usage recipe. */
@SideOnly(Side.CLIENT)
public final class NerfItems {

    private static final Map<Item, Map<Integer, Boolean>> CACHE = new HashMap<>();
    private static final Map<ItemKey, List<NerfDB.RecipeEntry>> DISCOVERED = new HashMap<>();
    private static List<NerfDB.Entry> indexedEntries;
    private static List<NerfDB.RecipeEntry> indexedRecipes;
    private static int cachedItems;
    private static String cachedLanguage = "";
    private static long cachedResourceGeneration = -1;

    private NerfItems() {}

    private static final class ItemKey {

        final Item item;
        final int damage;

        ItemKey(ItemStack stack) {
            item = stack.getItem();
            damage = stack.getItemDamage();
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(item) * 31 + damage;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof ItemKey)) return false;
            ItemKey key = (ItemKey) other;
            return item == key.item && damage == key.damage;
        }
    }

    public static void clearCache() {
        CACHE.clear();
        DISCOVERED.clear();
        indexedEntries = null;
        indexedRecipes = null;
        cachedItems = 0;
        cachedLanguage = "";
        cachedResourceGeneration = -1;
    }

    /** Run after ExtraNerf's load-complete edits, once per display database. Native recipes are only read. */
    public static void initialize() {
        if (indexedEntries == NerfDB.entries() && indexedRecipes == NerfDB.recipes()) return;
        clearCache();
        indexedEntries = NerfDB.entries();
        indexedRecipes = NerfDB.recipes();
        for (NerfDB.RecipeEntry recipe : indexedRecipes) {
            if (recipe.removed || recipe.noticeOnly || recipe.outputs.isEmpty()) continue;
            boolean broad = false;
            for (NerfDB.StackSpec output : recipe.outputs) broad |= broadSelector(output);
            if (!broad) continue;
            if (recipe.maps.contains("crafting")) scanCrafting(recipe);
            scanGregTech(recipe);
            if (recipe.maps.contains("thaumcraft.alchemy")) scanAlchemy(recipe);
        }
        FMLLog.info("[NerfLens] indexed %d exact products of broad recipe rules", DISCOVERED.size());
    }

    public static boolean affected(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return false;
        initialize();
        String language = NerfText.language();
        long generation = NerfTranslations.generation();
        if (!language.equals(cachedLanguage) || cachedResourceGeneration != generation) {
            CACHE.clear();
            cachedItems = 0;
            cachedLanguage = language;
            cachedResourceGeneration = generation;
        }
        // Fluid display items and custom-named stacks share item IDs; their NBT must be checked each time.
        if (stack.hasTagCompound()) return affectedUncached(stack);
        Map<Integer, Boolean> variants = CACHE.computeIfAbsent(stack.getItem(), item -> new HashMap<>());
        int damage = stack.getItemDamage();
        Boolean cached = variants.get(damage);
        if (cached != null) return cached;
        boolean affected = affectedUncached(stack);
        if (cachedItems >= 4096) {
            CACHE.clear();
            cachedItems = 0;
            variants = CACHE.computeIfAbsent(stack.getItem(), item -> new HashMap<>());
        }
        variants.put(damage, affected);
        cachedItems++;
        return affected;
    }

    private static boolean affectedUncached(ItemStack stack) {
        for (NerfDB.Entry entry : indexedEntries) {
            if (!entry.materialInputsChanged) continue;
            for (NerfDB.StackSpec product : entry.matchStacks) if (explicitProduct(product, stack)) return true;
        }
        List<NerfDB.RecipeEntry> discovered = DISCOVERED.get(new ItemKey(stack));
        for (NerfDB.RecipeEntry recipe : indexedRecipes) {
            if (!materialInputsChanged(recipe)) continue;
            if (discovered != null && discovered.contains(recipe)) return true;
            for (NerfDB.StackSpec output : recipe.outputs) if (explicitProduct(output, stack)) return true;
        }
        return false;
    }

    /** Output quantities, chances, processing conditions and removed routes do not change material inputs. */
    public static boolean materialInputsChanged(NerfDB.RecipeEntry recipe) {
        if (recipe == null || recipe.removed || recipe.noticeOnly) return false;
        if (recipe.materialInputsChanged != null) return recipe.materialInputsChanged;
        if (recipe.allInputsChanged || !recipe.changedInputs.isEmpty()) return true;
        for (NerfDB.VisualChange change : recipe.visualChanges) {
            if (change != null && !change.output && "item".equals(change.kind) && change.stack != null) return true;
        }
        return false;
    }

    /** Production explanations remain available whether or not the product qualifies for a global frame. */
    public static List<NerfDB.RecipeEntry> recipesFor(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return Collections.emptyList();
        initialize();
        List<NerfDB.RecipeEntry> discovered = DISCOVERED.get(new ItemKey(stack));
        List<NerfDB.RecipeEntry> result = new ArrayList<>();
        for (NerfDB.RecipeEntry recipe : indexedRecipes) {
            if (recipe.noticeOnly) continue;
            if (discovered != null && discovered.contains(recipe)) {
                result.add(recipe);
                continue;
            }
            for (NerfDB.StackSpec output : recipe.outputs) if (explicitProduct(output, stack)) {
                result.add(recipe);
                break;
            }
        }
        return result;
    }

    private static boolean broadSelector(NerfDB.StackSpec spec) {
        if (spec == null) return false;
        if (spec.mod != null || spec.odPrefix != null || spec.odSuffix != null) return true;
        if (spec.any != null) for (NerfDB.StackSpec alternative : spec.any) {
            if (broadSelector(alternative)) return true;
        }
        return false;
    }

    private static boolean explicitProduct(NerfDB.StackSpec spec, ItemStack stack) {
        if (spec == null || spec.mod != null || spec.odPrefix != null || spec.odSuffix != null) return false;
        if (spec.any != null) {
            for (NerfDB.StackSpec alternative : spec.any) if (explicitProduct(alternative, stack)) return true;
            return false;
        }
        // Recipe quantities constrain recipe matches, never the identity of a material in a different recipe.
        return NerfRecipes.matchesItem(spec, stack);
    }

    private static boolean candidate(NerfDB.RecipeEntry recipe, ItemStack[] outputs) {
        if (outputs == null) return false;
        for (ItemStack stack : outputs) for (NerfDB.StackSpec output : recipe.outputs) {
            if (NerfRecipes.matchesItem(output, stack)) return true;
        }
        return false;
    }

    private static void remember(NerfDB.RecipeEntry recipe, String map, List<PositionedStack> inputs,
        ItemStack[] outputs, FluidStack[] fluidOutputs) {
        List<PositionedStack> out = positions(outputs);
        addFluids(out, fluidOutputs);
        if (!NerfRecipes.matchesRecipe(recipe, map, inputs, out)) return;
        if (outputs != null) for (ItemStack output : outputs) {
            if (output == null || output.getItem() == null) continue;
            for (NerfDB.StackSpec selector : recipe.outputs) if (NerfRecipes.matchesItem(selector, output)) {
                List<NerfDB.RecipeEntry> changes = DISCOVERED
                    .computeIfAbsent(new ItemKey(output), key -> new ArrayList<>());
                if (!changes.contains(recipe)) changes.add(recipe);
                break;
            }
        }
    }

    private static void scanCrafting(NerfDB.RecipeEntry recipe) {
        for (Object value : CraftingManager.getInstance()
            .getRecipeList()) {
            if (!(value instanceof IRecipe)) continue;
            IRecipe nativeRecipe = (IRecipe) value;
            try {
                ItemStack[] outputs = { nativeRecipe.getRecipeOutput() };
                if (!candidate(recipe, outputs)) continue;
                Object[] inputs;
                if (nativeRecipe instanceof ShapedOreRecipe) inputs = ((ShapedOreRecipe) nativeRecipe).getInput();
                else if (nativeRecipe instanceof ShapelessOreRecipe)
                    inputs = ((ShapelessOreRecipe) nativeRecipe).getInput()
                        .toArray();
                else if (nativeRecipe instanceof ShapedRecipes) inputs = ((ShapedRecipes) nativeRecipe).recipeItems;
                else if (nativeRecipe instanceof ShapelessRecipes)
                    inputs = ((ShapelessRecipes) nativeRecipe).recipeItems.toArray();
                else continue;
                remember(recipe, "crafting", positions(inputs), outputs, null);
            } catch (RuntimeException | LinkageError failure) {
                FMLLog.warning("[NerfLens] unable to index a crafting display: %s", failure);
            }
        }
    }

    private static void scanGregTech(NerfDB.RecipeEntry recipe) {
        try {
            Class<?> mapType = Class.forName("gregtech.api.recipe.RecipeMap");
            Map<?, ?> maps = (Map<?, ?>) mapType.getField("ALL_RECIPE_MAPS")
                .get(null);
            Class<?> recipeType = Class.forName("gregtech.api.util.GTRecipe");
            Field inputItems = recipeType.getField("mInputs"), outputItems = recipeType.getField("mOutputs");
            Field inputFluids = recipeType.getField("mFluidInputs"),
                outputFluids = recipeType.getField("mFluidOutputs");
            for (String map : recipe.maps) {
                Object nativeMap = maps.get(map);
                if (nativeMap == null) continue;
                Collection<?> recipes = (Collection<?>) mapType.getMethod("getAllRecipes")
                    .invoke(nativeMap);
                for (Object nativeRecipe : recipes) {
                    ItemStack[] outputs = (ItemStack[]) outputItems.get(nativeRecipe);
                    if (!candidate(recipe, outputs)) continue;
                    List<PositionedStack> inputs = positions((ItemStack[]) inputItems.get(nativeRecipe));
                    addFluids(inputs, (FluidStack[]) inputFluids.get(nativeRecipe));
                    remember(recipe, map, inputs, outputs, (FluidStack[]) outputFluids.get(nativeRecipe));
                }
            }
            if (!recipe.maps.contains("gt.recipe.fakeAssemblylineProcess")) return;
            Class<?> assembly = Class.forName("gregtech.api.util.GTRecipe$RecipeAssemblyLine");
            for (Object nativeRecipe : (List<?>) assembly.getField("sAssemblylineRecipes")
                .get(null)) {
                ItemStack[] outputs = { (ItemStack) assembly.getField("mOutput")
                    .get(nativeRecipe) };
                if (!candidate(recipe, outputs)) continue;
                List<PositionedStack> inputs = positions(
                    (ItemStack[]) assembly.getField("mInputs")
                        .get(nativeRecipe));
                addFluids(
                    inputs,
                    (FluidStack[]) assembly.getField("mFluidInputs")
                        .get(nativeRecipe));
                remember(recipe, "gt.recipe.fakeAssemblylineProcess", inputs, outputs, null);
            }
        } catch (ClassNotFoundException absent) {
            // GregTech is optional at class-loading time; exact display records still work.
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            FMLLog.warning("[NerfLens] unable to index broad GregTech recipe displays: %s", failure);
        }
    }

    /** Read Thaumcraft's final registry so only real metal transmutations receive product explanations. */
    private static void scanAlchemy(NerfDB.RecipeEntry recipe) {
        try {
            Class<?> api = Class.forName("thaumcraft.api.ThaumcraftApi");
            Class<?> crucible = Class.forName("thaumcraft.api.crafting.CrucibleRecipe");
            for (Object value : (List<?>) api.getMethod("getCraftingRecipes")
                .invoke(null)) {
                if (!crucible.isInstance(value)) continue;
                ItemStack output = (ItemStack) crucible.getMethod("getRecipeOutput")
                    .invoke(value);
                ItemStack[] outputs = { output };
                if (!candidate(recipe, outputs)) continue;
                Object catalyst = crucible.getField("catalyst")
                    .get(value);
                if (catalyst instanceof String)
                    catalyst = net.minecraftforge.oredict.OreDictionary.getOres((String) catalyst);
                remember(recipe, "thaumcraft.alchemy", positions(new Object[] { catalyst }), outputs, null);
            }
        } catch (ClassNotFoundException absent) {
            // Thaumcraft may be absent in reduced development environments.
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            FMLLog.warning("[NerfLens] unable to index alchemy recipe displays: %s", failure);
        }
    }

    private static List<PositionedStack> positions(Object[] ingredients) {
        List<PositionedStack> result = new ArrayList<>();
        if (ingredients == null) return result;
        for (Object ingredient : ingredients) {
            Collection<?> alternatives = ingredient instanceof Collection ? (Collection<?>) ingredient
                : ingredient instanceof ItemStack[] ? Arrays.asList((ItemStack[]) ingredient)
                    : Collections.singletonList(ingredient);
            List<ItemStack> copies = new ArrayList<>();
            for (Object alternative : alternatives)
                if (alternative instanceof ItemStack) copies.add(((ItemStack) alternative).copy());
            if (!copies.isEmpty()) result.add(new PositionedStack(copies, 0, 0, false));
        }
        return result;
    }

    private static void addFluids(List<PositionedStack> stacks, FluidStack[] fluids) {
        if (fluids == null) return;
        for (FluidStack fluid : fluids) {
            if (fluid == null || fluid.getFluid() == null) continue;
            ItemStack display = ItemFluidDisplay.createStack(fluid.getFluid(), (long) fluid.amount);
            if (display != null) stacks.add(new PositionedStack(display, 0, 0, false));
        }
    }
}
