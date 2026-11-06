package nerflens;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;

import codechicken.nei.PositionedStack;
import codechicken.nei.item.ItemFluidDisplay;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.StackInfo;
import codechicken.nei.recipe.TemplateRecipeHandler;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Read-only recipe snapshots. No recipe tables or ItemStacks are modified. */
@SideOnly(Side.CLIENT)
public final class NerfRecipes {

    private static final Map<IRecipeHandler, Map<Integer, List<NerfDB.RecipeEntry>>> CACHE = new WeakHashMap<>();
    private static final Map<String, ItemStack> GT_ITEMS = new HashMap<>();
    private static final Map<String, Fluid> GT_FLUIDS = new HashMap<>();
    private static boolean warned;
    private static long matchingGeneration = -1;
    private static List<NerfDB.RecipeEntry> matchingRecipes;

    private NerfRecipes() {}

    public static void clearCache() {
        CACHE.clear();
        GT_ITEMS.clear();
        GT_FLUIDS.clear();
        NerfItems.clearCache();
        warned = false;
    }

    public static String mapId(IRecipeHandler handler) {
        if (handler instanceof TemplateRecipeHandler) {
            String overlay = ((TemplateRecipeHandler) handler).getOverlayIdentifier();
            if (overlay != null && !overlay.isEmpty()) return overlay;
        }
        return handler.getHandlerId();
    }

    public static boolean supports(IRecipeHandler handler) {
        if (handler == null) return false;
        try {
            String map = mapId(handler);
            for (NerfDB.RecipeEntry recipe : NerfDB.recipes()) {
                if (!recipe.removed && !recipe.noticeOnly
                    && recipe.maps != null
                    && !recipe.outputs.isEmpty()
                    && recipe.maps.contains(map)) return true;
            }
        } catch (RuntimeException | LinkageError failure) {
            warn(failure);
        }
        return false;
    }

    public static List<NerfDB.RecipeEntry> match(IRecipeHandler handler, int index) {
        if (handler == null || index < 0) return Collections.emptyList();
        try {
            long generation = NerfTranslations.generation();
            if (matchingGeneration != generation || matchingRecipes != NerfDB.recipes()) {
                CACHE.clear();
                matchingGeneration = generation;
                matchingRecipes = NerfDB.recipes();
            }
            Map<Integer, List<NerfDB.RecipeEntry>> cache = CACHE.computeIfAbsent(handler, h -> new HashMap<>());
            if (cache.containsKey(index)) return cache.get(index);
            String map = mapId(handler);
            List<PositionedStack> inputs = handler.getIngredientStacks(index);
            List<PositionedStack> outputs = new ArrayList<>();
            PositionedStack result = handler.getResultStack(index);
            if (result != null) outputs.add(result);
            List<PositionedStack> other = handler.getOtherStacks(index);
            if (other != null) outputs.addAll(other);
            List<NerfDB.RecipeEntry> matches = new ArrayList<>();
            for (NerfDB.RecipeEntry recipe : NerfDB.recipes()) {
                if (matchesRecipe(recipe, map, inputs, outputs)) matches.add(recipe);
            }
            List<NerfDB.RecipeEntry> immutable = Collections.unmodifiableList(matches);
            if (cache.size() >= 256) cache.clear();
            cache.put(index, immutable);
            return immutable;
        } catch (RuntimeException | LinkageError failure) {
            if (!warned) {
                warned = true;
                FMLLog.warning("[NerfLens] recipe display disabled for an unreadable handler: %s", failure);
            }
            return Collections.emptyList();
        }
    }

    /** Shared by NEI pages and the read-only index of affected recipe products. */
    public static boolean matchesRecipe(NerfDB.RecipeEntry recipe, String map, List<PositionedStack> inputs,
        List<PositionedStack> outputs) {
        if (recipe.removed || recipe.noticeOnly
            || recipe.maps == null
            || !recipe.maps.contains(map)
            || recipe.outputs == null
            || recipe.outputs.isEmpty()) return false;
        if (!containsAll(outputs, recipe.outputs) || !containsAll(inputs, recipe.inputs)) return false;
        if (recipe.absentInputs != null) for (NerfDB.StackSpec spec : recipe.absentInputs) {
            if (present(inputs, spec)) return false;
        }
        if ("metal-duplication".equals(recipe.rule) && !sameMetal(inputs, outputs)) return false;
        return true;
    }

    /** Exact stack selectors take precedence over translated name fallbacks, as in item tooltips. */
    public static List<NerfDB.Entry> entriesFor(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return Collections.emptyList();
        try {
            return entriesFor(stack, stack.getUnlocalizedName(), stack.getDisplayName());
        } catch (RuntimeException | LinkageError failure) {
            warn(failure);
            return Collections.emptyList();
        }
    }

    public static List<NerfDB.Entry> entriesFor(ItemStack stack, String unloc, String display) {
        List<NerfDB.Entry> hits = new ArrayList<>(NerfDB.match(unloc, display));
        for (NerfDB.Entry entry : NerfDB.entries()) {
            if (entry.matchStacks == null || entry.matchStacks.isEmpty()) continue;
            hits.remove(entry);
            for (NerfDB.StackSpec selector : entry.matchStacks) {
                if (matchesItem(selector, stack)) {
                    hits.add(entry);
                    break;
                }
            }
        }
        return hits;
    }

    private static boolean containsAll(List<PositionedStack> stacks, List<NerfDB.StackSpec> specs) {
        if (specs == null) return true;
        for (NerfDB.StackSpec spec : specs) if (!present(stacks, spec)) return false;
        return true;
    }

    private static boolean present(List<PositionedStack> stacks, NerfDB.StackSpec spec) {
        if (stacks == null || spec == null) return false;
        long total = amount(stacks, spec);
        Integer wanted = isFluid(spec) ? spec.mB : spec.n;
        return total >= 0 && (wanted == null || total == wanted.longValue());
    }

    public static long amount(List<PositionedStack> stacks, NerfDB.StackSpec spec) {
        if (stacks == null || spec == null) return -1;
        long total = 0;
        boolean found = false;
        for (PositionedStack stack : stacks) {
            if (stack == null) continue;
            int amount = -1;
            ItemStack[] alternatives = stack.items == null || stack.items.length == 0 ? new ItemStack[] { stack.item }
                : stack.items;
            for (ItemStack item : alternatives) {
                if (matchesItem(spec, item)) {
                    FluidStack fluid = fluid(item);
                    amount = Math
                        .max(amount, isFluid(spec) ? (fluid == null ? 0 : fluid.amount) : StackInfo.getAmount(item));
                }
            }
            if (amount >= 0) {
                found = true;
                total += amount;
            }
        }
        return found ? total : -1;
    }

    public static List<PositionedStack> recipeStacks(IRecipeHandler handler, int index, boolean output) {
        if (!output) {
            List<PositionedStack> inputs = handler.getIngredientStacks(index);
            return inputs == null ? Collections.emptyList() : inputs;
        }
        List<PositionedStack> result = new ArrayList<>();
        PositionedStack primary = handler.getResultStack(index);
        if (primary != null) result.add(primary);
        List<PositionedStack> others = handler.getOtherStacks(index);
        if (others != null) result.addAll(others);
        return result;
    }

    public static ItemStack sampleStack(List<PositionedStack> stacks, NerfDB.StackSpec spec) {
        for (PositionedStack position : stacks) {
            if (position == null) continue;
            if (matchesItem(spec, position.item)) return position.item.copy();
            if (position.items != null) for (ItemStack stack : position.items) {
                if (matchesItem(spec, stack)) return stack.copy();
            }
        }
        return displayStack(spec);
    }

    public static ItemStack displayStack(NerfDB.StackSpec spec) {
        if (spec == null) return null;
        try {
            if (spec.any != null) {
                for (NerfDB.StackSpec alternative : spec.any) {
                    ItemStack stack = displayStack(alternative);
                    if (stack != null) return stack;
                }
                return null;
            }
            ItemStack target = null;
            if (isFluid(spec)) {
                Fluid targetFluid = spec.fluidGt != null ? gtFluid(spec.fluidGt, spec.phase) : null;
                if (targetFluid == null && spec.f != null) targetFluid = FluidRegistry.getFluid(spec.f);
                if (targetFluid == null && spec.fany != null) for (String name : spec.fany) {
                    targetFluid = FluidRegistry.getFluid(name);
                    if (targetFluid != null) break;
                }
                return targetFluid == null ? null : ItemFluidDisplay.createStack(targetFluid, 1L);
            }
            if (spec.gt != null) target = gtItem(spec.gt);
            else if (spec.nh != null || spec.ic2 != null || spec.material != null || spec.bwMeta != null)
                target = foreignItem(spec);
            else if (spec.od != null) target = oreStack(spec.od);
            else if (spec.item != null) {
                Item item = (Item) Item.itemRegistry.getObject(spec.item);
                if (item != null) target = new ItemStack(item, 1, spec.meta == null ? 0 : spec.meta);
            } else if (spec.circuit != null) {
                Item item = (Item) Item.itemRegistry.getObject("gregtech:gt.integrated_circuit");
                if (item != null) target = new ItemStack(item, 1, Integer.parseInt(spec.circuit));
            }
            if (target == null) return null;
            ItemStack copy = target.copy();
            copy.stackSize = 1;
            if (spec.nbtStrings != null && !spec.nbtStrings.isEmpty()) {
                NBTTagCompound tag = copy.hasTagCompound() ? copy.getTagCompound() : new NBTTagCompound();
                for (Map.Entry<String, String> value : spec.nbtStrings.entrySet())
                    tag.setString(value.getKey(), value.getValue());
                copy.setTagCompound(tag);
            }
            return copy;
        } catch (RuntimeException | LinkageError failure) {
            warn(failure);
            return null;
        }
    }

    private static ItemStack oreStack(String name) {
        for (ItemStack stack : OreDictionary.getOres(name)) {
            if (stack != null && stack.getItemDamage() != OreDictionary.WILDCARD_VALUE) return stack.copy();
        }
        return null;
    }

    /** Resolve the old form of the same material for generic plate/rod replacements. */
    public static ItemStack previousShape(ItemStack current, NerfDB.StackSpec selector, String prefix, String suffix) {
        if (current == null || selector == null) return null;
        for (int id : OreDictionary.getOreIDs(current)) {
            String name = OreDictionary.getOreName(id);
            if (!prefix.isEmpty() && selector.odPrefix != null && name.startsWith(selector.odPrefix)) {
                ItemStack stack = oreStack(prefix + name.substring(selector.odPrefix.length()));
                if (stack != null) return stack;
            }
            if (!suffix.isEmpty() && name.toLowerCase(Locale.ROOT)
                .endsWith("tpv")) {
                ItemStack stack = oreStack(name.substring(0, name.length() - 3) + suffix);
                if (stack != null) return stack;
            }
        }
        return null;
    }

    public static boolean changedInput(NerfDB.RecipeEntry recipe, PositionedStack stack) {
        if (recipe.allInputsChanged)
            return stack.item != null && StackInfo.getAmount(stack.item) > 0 && fluid(stack.item) == null;
        if (recipe.changedInputs == null) return false;
        for (NerfDB.StackSpec spec : recipe.changedInputs) {
            if (matchesItem(spec, stack.item)) return true;
        }
        return false;
    }

    public static boolean matchesItem(NerfDB.StackSpec spec, ItemStack stack) {
        try {
            return matchesItemInner(spec, stack);
        } catch (RuntimeException | LinkageError failure) {
            warn(failure);
            return false;
        }
    }

    private static void warn(Throwable failure) {
        if (!warned) {
            warned = true;
            FMLLog.warning("[NerfLens] unreadable recipe display data: %s", failure);
        }
    }

    private static boolean matchesItemInner(NerfDB.StackSpec spec, ItemStack stack) {
        if (spec == null || stack == null || stack.getItem() == null) return false;
        if (spec.nbtStrings != null) for (Map.Entry<String, String> value : spec.nbtStrings.entrySet()) {
            if (!stack.hasTagCompound() || !stack.getTagCompound()
                .hasKey(value.getKey(), 8)
                || !value.getValue()
                    .equals(
                        stack.getTagCompound()
                            .getString(value.getKey())))
                return false;
        }
        if (spec.any != null) {
            for (NerfDB.StackSpec alternative : spec.any) if (matchesItem(alternative, stack)) return true;
            return false;
        }
        if (isFluid(spec)) {
            FluidStack fluid = fluid(stack);
            if (fluid == null) return false;
            if (spec.fluidGt != null) return fluid.getFluid() == gtFluid(spec.fluidGt, spec.phase);
            String name = fluid.getFluid()
                .getName();
            return name.equals(spec.f) || (spec.fany != null && spec.fany.contains(name));
        }
        if (spec.gt != null) {
            ItemStack target = gtItem(spec.gt);
            return target != null && OreDictionary.itemMatches(target, stack, false);
        }
        if (spec.gtCableMinTier != null) return highTierCable(stack, spec.gtCableMinTier);
        if (spec.gtMachineClass != null) {
            Object machine = nativeMachine(stack);
            if (!isMachineClass(machine, spec.gtMachineClass)) return false;
            if (spec.steamPowered != null) {
                try {
                    if (!spec.steamPowered.equals(
                        machine.getClass()
                            .getMethod("isSteampowered")
                            .invoke(machine)))
                        return false;
                } catch (ReflectiveOperationException failure) {
                    warn(failure);
                    return false;
                }
            }
            if (spec.excludeMachineClasses != null)
                for (String excluded : spec.excludeMachineClasses) if (isMachineClass(machine, excluded)) return false;
            return true;
        }
        if (spec.mod != null) {
            String registryName = Item.itemRegistry.getNameForObject(stack.getItem());
            return registryName != null && registryName.startsWith(spec.mod + ":");
        }
        if (spec.nh != null || spec.ic2 != null || spec.material != null || spec.bwMeta != null) {
            ItemStack target = foreignItem(spec);
            return target != null && OreDictionary.itemMatches(target, stack, false);
        }
        if (spec.item != null) {
            if (!spec.item.equals(Item.itemRegistry.getNameForObject(stack.getItem()))) return false;
            return spec.meta == null || spec.meta == OreDictionary.WILDCARD_VALUE || spec.meta == stack.getItemDamage();
        }
        if (spec.circuit != null)
            return "gregtech:gt.integrated_circuit".equals(Item.itemRegistry.getNameForObject(stack.getItem()))
                && stack.getItemDamage() == Integer.parseInt(spec.circuit);
        if (spec.od != null || spec.odPrefix != null || spec.odSuffix != null) {
            for (int id : OreDictionary.getOreIDs(stack)) {
                String name = OreDictionary.getOreName(id);
                if (name.equals(spec.od) || (spec.odPrefix != null && name.startsWith(spec.odPrefix))
                    || (spec.odSuffix != null && name.toLowerCase(Locale.ROOT)
                        .endsWith(spec.odSuffix.toLowerCase(Locale.ROOT))))
                    return true;
            }
            return false;
        }
        if (spec.unloc != null) return nameMatches(spec.unloc, stack.getUnlocalizedName());
        String display = EnumChatFormatting.getTextWithoutFormattingCodes(stack.getDisplayName());
        return nameMatches(spec.dn, display) || nameMatches(spec.dnzh, display);
    }

    /** Read the same native cable voltage and insulation fields used by ExtraNerf's LATEX rule. */
    private static boolean highTierCable(ItemStack stack, int tier) {
        try {
            Class<?> api = Class.forName("gregtech.api.GregTechAPI");
            Block block = (Block) api.getField("sBlockMachines")
                .get(null);
            if (block == null || stack.getItem() != Item.getItemFromBlock(block)) return false;
            Object[] machines = (Object[]) api.getField("METATILEENTITIES")
                .get(null);
            int meta = stack.getItemDamage();
            if (meta < 0 || meta >= machines.length || machines[meta] == null) return false;
            Class<?> cableType = Class.forName("gregtech.api.metatileentity.implementations.MTECable");
            Object cable = machines[meta];
            if (!cableType.isInstance(cable) || !cableType.getField("mInsulated")
                .getBoolean(cable)) return false;
            long[] voltages = (long[]) Class.forName("gregtech.api.enums.GTValues")
                .getField("V")
                .get(null);
            return tier >= 0 && tier < voltages.length
                && cableType.getField("mVoltage")
                    .getLong(cable) >= voltages[tier];
        } catch (ReflectiveOperationException | LinkageError failure) {
            warn(failure);
            return false;
        }
    }

    private static Object nativeMachine(ItemStack stack) {
        try {
            Class<?> api = Class.forName("gregtech.api.GregTechAPI");
            Block block = (Block) api.getField("sBlockMachines")
                .get(null);
            if (block == null || stack.getItem() != Item.getItemFromBlock(block)) return null;
            Object[] machines = (Object[]) api.getField("METATILEENTITIES")
                .get(null);
            int meta = stack.getItemDamage();
            return meta < 0 || meta >= machines.length ? null : machines[meta];
        } catch (ReflectiveOperationException | LinkageError failure) {
            warn(failure);
            return null;
        }
    }

    private static boolean isMachineClass(Object machine, String name) {
        if (machine == null) return false;
        for (Class<?> type = machine.getClass(); type != null; type = type.getSuperclass())
            if (name.equals(type.getName())) return true;
        return false;
    }

    private static boolean sameMetal(List<PositionedStack> inputs, List<PositionedStack> outputs) {
        if (inputs == null || outputs == null) return false;
        for (PositionedStack input : inputs) for (ItemStack item : alternatives(input)) {
            if (item == null) continue;
            for (int id : OreDictionary.getOreIDs(item)) {
                String name = OreDictionary.getOreName(id);
                if (!name.startsWith("nugget")) continue;
                String material = name.substring("nugget".length());
                if (!metal(material)) continue;
                for (PositionedStack output : outputs) for (ItemStack product : alternatives(output)) {
                    if (product == null) continue;
                    for (int productId : OreDictionary.getOreIDs(product))
                        if (("dustTiny" + material).equals(OreDictionary.getOreName(productId))) return true;
                }
            }
        }
        return false;
    }

    private static ItemStack[] alternatives(PositionedStack position) {
        if (position == null) return new ItemStack[0];
        return position.items == null || position.items.length == 0 ? new ItemStack[] { position.item }
            : position.items;
    }

    private static boolean metal(String name) {
        try {
            Class<?> materials = Class.forName("gregtech.api.enums.Materials");
            Object material = materials.getMethod("get", String.class)
                .invoke(null, name);
            Class<?> tags = Class.forName("gregtech.api.enums.SubTag");
            return material != null && (Boolean) materials.getMethod("contains", tags)
                .invoke(
                    material,
                    tags.getField("METAL")
                        .get(null));
        } catch (ReflectiveOperationException | LinkageError failure) {
            warn(failure);
            return false;
        }
    }

    private static boolean nameMatches(String query, String value) {
        if (query == null || query.isEmpty() || value == null) return false;
        String text = value.toLowerCase(Locale.ROOT);
        String key = query.toLowerCase(Locale.ROOT);
        return key.startsWith("=") ? text.equals(key.substring(1)) : text.contains(key);
    }

    private static FluidStack fluid(ItemStack stack) {
        return stack == null ? null : StackInfo.getFluid(stack);
    }

    private static boolean isFluid(NerfDB.StackSpec spec) {
        return spec.f != null || spec.fany != null || spec.fluidGt != null;
    }

    private static Fluid gtFluid(String name, String phase) {
        String key = name + ":" + phase;
        if (GT_FLUIDS.containsKey(key)) return GT_FLUIDS.get(key);
        Fluid result = null;
        try {
            if (name.startsWith("ItemList:")) {
                result = (Fluid) Class.forName("gregtech.api.enums.ItemList")
                    .getField(name.substring("ItemList:".length()))
                    .get(null);
                GT_FLUIDS.put(key, result);
                return result;
            }
            if (name.startsWith("GTPPFluids:")) {
                result = (Fluid) Class.forName("gtPlusPlus.core.fluids.GTPPFluids")
                    .getField(name.substring("GTPPFluids:".length()))
                    .get(null);
                GT_FLUIDS.put(key, result);
                return result;
            }
            if (name.contains(":")) {
                String[] parts = name.split(":", 2);
                Object material = Class.forName(materialClass(parts[0]))
                    .getField(parts[1])
                    .get(null);
                String method = isGTPPMaterial(parts[0]) ? "getFluidStack" : "getFluidOrGas";
                FluidStack stack = (FluidStack) material.getClass()
                    .getMethod(method, int.class)
                    .invoke(material, 1);
                if (stack != null) result = stack.getFluid();
                GT_FLUIDS.put(key, result);
                return result;
            }
            Class<?> type = Class.forName("gregtech.api.enums.Materials");
            Object material = type.getField(name)
                .get(null);
            String method = "gas".equals(phase) ? "getGas"
                : "molten".equals(phase) ? "getMolten" : "plasma".equals(phase) ? "getPlasma" : "getFluid";
            FluidStack stack = (FluidStack) type.getMethod(method, long.class)
                .invoke(material, 1L);
            if (stack != null) result = stack.getFluid();
        } catch (ReflectiveOperationException | IllegalArgumentException failure) {
            FMLLog.warning("[NerfLens] unknown fluid selector %s: %s", key, failure);
        }
        GT_FLUIDS.put(key, result);
        return result;
    }

    private static boolean isGTPPMaterial(String namespace) {
        return "misc".equals(namespace) || "ore".equals(namespace)
            || "alloy".equals(namespace)
            || "fluoride".equals(namespace);
    }

    private static String materialClass(String namespace) {
        switch (namespace) {
            case "bw":
                return "bartworks.system.material.WerkstoffLoader";
            case "gg":
                return "goodgenerator.items.GGMaterial";
            case "lanth":
                return "gtnhlanth.common.register.WerkstoffMaterialPool";
            case "misc":
                return "gtPlusPlus.core.material.MaterialMisc";
            case "ore":
                return "gtPlusPlus.core.material.MaterialsOres";
            case "alloy":
                return "gtPlusPlus.core.material.MaterialsAlloy";
            case "fluoride":
                return "gtPlusPlus.core.material.nuclear.MaterialsFluorides";
            default:
                throw new IllegalArgumentException("unknown material namespace");
        }
    }

    private static ItemStack foreignItem(NerfDB.StackSpec spec) {
        String key = spec.nh != null ? "nh:" + spec.nh
            : spec.ic2 != null ? "ic2:" + spec.ic2
                : spec.bwMeta != null ? "bw-meta:" + spec.bwMeta + ":" + spec.prefix
                    : spec.material + ":" + spec.prefix;
        if (GT_ITEMS.containsKey(key)) return GT_ITEMS.get(key);
        ItemStack result = null;
        try {
            if (spec.nh != null) {
                Class<?> type = Class.forName("com.dreammaster.item.NHItemList");
                Object entry = type.getField(spec.nh)
                    .get(null);
                result = (ItemStack) type.getMethod("get")
                    .invoke(entry);
            } else if (spec.ic2 != null) {
                result = (ItemStack) Class.forName("ic2.api.item.IC2Items")
                    .getMethod("getItem", String.class)
                    .invoke(null, spec.ic2);
            } else if (spec.bwMeta != null) {
                Class<?> prefixType = Class.forName("gregtech.api.enums.OrePrefixes");
                Object prefix = prefixType.getField(spec.prefix)
                    .get(null);
                Map<?, ?> items = (Map<?, ?>) Class.forName("bartworks.system.material.WerkstoffLoader")
                    .getField("items")
                    .get(null);
                Item item = (Item) items.get(prefix);
                if (item != null) result = new ItemStack(item, 1, spec.bwMeta);
            } else {
                String[] parts = spec.material.split(":", 2);
                String className = materialClass(parts[0]);
                Object material = Class.forName(className)
                    .getField(parts[1])
                    .get(null);
                Class<?> prefixType = Class.forName("gregtech.api.enums.OrePrefixes");
                Object prefix = prefixType.getField(spec.prefix)
                    .get(null);
                result = (ItemStack) material.getClass()
                    .getMethod(isGTPPMaterial(parts[0]) ? "getComponentByPrefix" : "get", prefixType, int.class)
                    .invoke(material, prefix, 1);
            }
        } catch (ReflectiveOperationException | IllegalArgumentException failure) {
            FMLLog.warning("[NerfLens] unknown item selector %s: %s", key, failure);
        }
        GT_ITEMS.put(key, result);
        return result;
    }

    private static ItemStack gtItem(String name) {
        if (GT_ITEMS.containsKey(name)) return GT_ITEMS.get(name);
        ItemStack stack = null;
        try {
            Class<?> type = Class.forName("gregtech.api.enums.ItemList");
            Object entry = type.getField(name)
                .get(null);
            Method get = type.getMethod("get", long.class, Object[].class);
            stack = (ItemStack) get.invoke(entry, 1L, new Object[0]);
        } catch (ReflectiveOperationException | IllegalArgumentException failure) {
            FMLLog.warning("[NerfLens] unknown GregTech display selector %s: %s", name, failure);
        }
        GT_ITEMS.put(name, stack);
        return stack;
    }
}
