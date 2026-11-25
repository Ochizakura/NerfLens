import java.io.File;
import java.lang.reflect.*;
import java.util.*;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraftforge.fluids.*;
import net.minecraftforge.oredict.OreDictionary;
import codechicken.nei.PositionedStack;
import codechicken.nei.item.ItemFluidDisplay;
import codechicken.nei.recipe.*;
import nerflens.*;

/** Reproduces unrelated-acid provenance, reused NEI pages, all silicon container routes and species-specific bees. */
public final class FixRegressionChecks {
    private static int assertions, nextId = 27000;
    private static void require(boolean result, String message) {
        assertions++;
        if (!result) throw new AssertionError(message);
    }
    private static Item item(String id) throws Exception {
        Item previous = (Item) Item.itemRegistry.getObject(id);
        if (previous != null) return previous;
        Item value = new Item().setUnlocalizedName("rc5_fixture").setHasSubtypes(true);
        Method raw = null;
        for (Method method : Item.itemRegistry.getClass().getDeclaredMethods())
            if (method.getName().equals("addObjectRaw")) raw = method;
        raw.setAccessible(true); raw.invoke(Item.itemRegistry, nextId++, id, value);
        Method name = value.delegate.getClass().getDeclaredMethod("setName", String.class);
        name.setAccessible(true); name.invoke(value.delegate, id);
        return value;
    }
    private static Item ore(String od) throws Exception {
        Item value = item("rc5:" + od.toLowerCase());
        OreDictionary.registerOre(od, new ItemStack(value)); return value;
    }
    private static Fluid fluid(String selector, Map<String, Fluid> fluids) {
        Fluid value = new Fluid("rc5." + selector.toLowerCase().replace(':', '.'));
        FluidRegistry.registerFluid(value); fluids.put(selector, value); return value;
    }
    private static PositionedStack p(ItemStack stack) { return new PositionedStack(stack, 0, 0, false); }
    private static PositionedStack liquid(Fluid fluid, long amount) { return p(ItemFluidDisplay.createStack(fluid, amount)); }
    private static ItemStack bee(Item item, String active, String inactive) {
        ItemStack stack = new ItemStack(item); NBTTagCompound tag = new NBTTagCompound();
        NBTTagCompound genome = new NBTTagCompound(), chromosome = new NBTTagCompound();
        chromosome.setString("UID0", active); chromosome.setString("UID1", inactive);
        NBTTagList chromosomes = new NBTTagList(); chromosomes.appendTag(chromosome);
        genome.setTag("Chromosomes", chromosomes); tag.setTag("Genome", genome); stack.setTagCompound(tag);
        return stack;
    }
    private static IRecipeHandler handler(String map, List<PositionedStack> inputs, List<PositionedStack> outputs) {
        return (IRecipeHandler) Proxy.newProxyInstance(IRecipeHandler.class.getClassLoader(),
            new Class<?>[]{IRecipeHandler.class}, (proxy, method, args) -> {
                switch (method.getName()) {
                    case "getHandlerId": case "getRecipeName": return map;
                    case "getIngredientStacks": return inputs;
                    case "getResultStack": return outputs.isEmpty() ? null : outputs.get(0);
                    case "getOtherStacks": return outputs.size() < 2 ? Collections.emptyList() : outputs.subList(1, outputs.size());
                    case "hashCode": return System.identityHashCode(proxy);
                    case "equals": return proxy == args[0];
                    default: return null;
                }
            });
    }
    private static boolean has(IRecipeHandler handler, String id) {
        return NerfRecipes.match(handler, 0).stream().anyMatch(r -> r.id.equals(id));
    }
    private static boolean gold(IRecipeHandler handler, PositionedStack stack, boolean input) {
        return NerfBadges.forRecipe(handler, 0, stack, input).stream().anyMatch(b -> b.getColor() == 0xFFFFB82E);
    }
    private static String description(ItemStack stack, boolean expanded) {
        List<String> tip = new ArrayList<>(); TooltipHandler.appendChanges(stack, tip, expanded);
        return String.join("\n", tip);
    }
    public static int run(File config, Map<String, Fluid> fluids) throws Exception {
        NerfDB.load(config, "1.2.0");
        ExtraNerf111Checks.Resources languages = new ExtraNerf111Checks.Resources();
        List<String> chinese = Arrays.asList("en_US", "zh_CN");
        String materialKey="Material.overcarburizediron",materialEnglish="Overcarburized Iron";
        NerfTranslations.reloaded(languages,chinese);
        require(NerfTranslations.fallback(materialKey,materialEnglish).equals("过渗碳铁"),"Version-verified native generated material name receives fallback");
        require(NerfTranslations.fallback(materialKey,materialKey).equals(materialKey),"Missing dynamic native English is not guessed");
        require(NerfTranslations.fallback(materialKey,"Future changed material meaning").equals("Future changed material meaning"),"Future native material text is not overwritten");
        languages.files.put("othermod:lang/zh_CN.lang",materialKey+"="+materialEnglish+"\n");
        NerfTranslations.reloaded(languages,chinese);
        require(NerfTranslations.fallback(materialKey,materialEnglish).equals(materialEnglish),"An intentional external English material name still wins");
        languages.files.remove("othermod:lang/zh_CN.lang");
        NerfTranslations.reloaded(languages,chinese);
        NerfDB.load(config,"1.1.1");
        require(NerfTranslations.fallback(materialKey,materialEnglish).equals(materialEnglish),"Generated material fallback is not applied to an older owner version");
        NerfDB.load(config,"1.3.0");
        require(NerfTranslations.fallback(materialKey,materialEnglish).equals("过渗碳铁"),"Current owner 1.3.0 keeps the verified generated material name");
        NerfDB.load(config,"1.4.0");
        require(NerfTranslations.fallback(materialKey,materialEnglish).equals(materialEnglish),"Unknown owner version receives no dynamic material fallback");
        NerfDB.load(config,"1.2.0");
        Fluid diluted = fluid("DilutedHydrochloricAcid:fluid", fluids);
        Fluid liquidKevlar = fluid("LiquidCrystalKevlar:fluid", fluids);
        Fluid acid = fluid("HydrochloricAcid:fluid", fluids);
        Fluid trichlorosilane = fluid("Trichlorosilane:fluid", fluids);
        Fluid hydrogen = fluid("Hydrogen:gas", fluids);
        Fluid nmp = fluid("NMethylIIPyrrolidone:fluid", fluids);
        PositionedStack dilute = liquid(diluted, 2000), kevlarFluid = liquid(liquidKevlar, 9000);
        require(!NerfItems.affected(dilute.item), "Unchanged dilute-acid byproduct has no global frame");
        require(NerfItems.recipesFor(dilute.item).stream().noneMatch(r -> r.id.startsWith("kevlar-drone")), "Dilute acid cannot inherit Kevlar production records");
        require(!description(dilute.item, false).contains("凯芙拉"), "Unrelated dilute-acid tooltip contains no Kevlar claims");
        require(NerfItems.affected(kevlarFluid.item), "Liquid Crystal Kevlar is globally framed");
        require(NerfItems.affected(new ItemStack(ore("plateKevlar"))), "Kevlar plates inherit the affected production chain");
        Item droneItem = item("Forestry:beeDroneGE");
        String kevlarSpecies = "gregtech.bee.speciesKevlar", otherSpecies = "forestry.speciesForest";
        ItemStack drone = bee(droneItem, kevlarSpecies, otherSpecies);
        require(NerfItems.affected(drone), "Active Kevlar drone has global frame");
        ItemStack inactive = bee(droneItem, otherSpecies, kevlarSpecies);
        require(!NerfItems.affected(inactive), "An inactive Kevlar allele alone is insufficient");
        require(!NerfItems.affected(new ItemStack(droneItem)), "Unspecified drone is never globally marked");
        for (String kind : new String[]{"beePrincessGE", "beeQueenGE"})
            require(NerfItems.affected(bee(item("Forestry:" + kind), kevlarSpecies, otherSpecies)), "Kevlar " + kind + " has the mutation warning");
        ItemStack secondary = bee(droneItem, otherSpecies, kevlarSpecies);
        secondary.getTagCompound().getCompoundTag("Genome").getTagList("Chromosomes",10).getCompoundTagAt(0).setBoolean("SecondaryActive",true);
        require(NerfItems.affected(secondary), "Uses the native active-allele result instead of assuming UID0");
        Item calcium = ore("dustCalciumChloride"), para = ore("dustpPhenylenediamine"), tere = ore("dustTerephthaloylChloride");
        Item circuit = item("gregtech:gt.integrated_circuit");
        List<PositionedStack> kevlarInputs = new ArrayList<>(Arrays.asList(p(new ItemStack(calcium)), p(new ItemStack(para,9,0)),
            p(new ItemStack(tere,9,0)), p(new ItemStack(circuit,1,1)), liquid(nmp,1000), p(drone)));
        IRecipeHandler kevlarHandler = handler("gt.recipe.largechemicalreactor", kevlarInputs, Arrays.asList(kevlarFluid,dilute));
        require(has(kevlarHandler,"kevlar-drone-small"), "Exact 9,000 mB Kevlar recipe matches with active-species drone");
        require(gold(kevlarHandler,kevlarFluid,false), "Kevlar main output has local gold frame");
        require(!gold(kevlarHandler,dilute,false), "Unchanged acid byproduct has no local Kevlar gold frame");
        kevlarInputs.set(5,p(inactive));
        require(!has(kevlarHandler,"kevlar-drone-small"), "Same handler page loses its match after changing bee NBT");

        Item silicon = ore("dustSiliconSolarGrade"), cell = ore("cellTrichlorosilane");
        for (int mode=0;mode<3;mode++) {
            List<PositionedStack> inputs = new ArrayList<>();
            inputs.add(mode==2 ? p(new ItemStack(cell)) : liquid(trichlorosilane,1000));
            inputs.add(mode==0 ? p(new ItemStack(ore("cellHydrogen"),2,0)) : liquid(hydrogen,2000));
            inputs.add(p(new ItemStack(circuit,1,1)));
            List<PositionedStack> outputs = new ArrayList<>(Arrays.asList(p(new ItemStack(silicon)),liquid(acid,2000)));
            IRecipeHandler route = handler(mode==1 ? "gt.recipe.largechemicalreactor" : "gt.recipe.chemicalreactor",inputs,outputs);
            require(has(route,"silicon-hydrogen"), "Silicon container/LCR route " + mode + " matches");
            require(gold(route,outputs.get(0),false), "Silicon main output has local frame in route " + mode);
            require(gold(route,outputs.get(1),false), "Reduced acid has local output frame in route " + mode);
            NerfDB.RecipeEntry record = NerfRecipes.match(route,0).stream().filter(r -> r.id.equals("silicon-hydrogen")).findFirst().get();
            List<String> tip = new ArrayList<>(); TooltipHandler.appendRecipeChanges(tip,Collections.singletonList(record),false);
            String text = String.join("\n",tip);
            require(text.contains("3,000") && text.contains("2,000") && text.contains("33.3%"), "Compact silicon tooltip shows before, after and reduction");
            outputs.set(1,liquid(acid,3000));
            require(!has(route,"silicon-hydrogen"), "Same handler/index drops stale match when acid quantity changes");
            outputs.set(1,liquid(acid,2000));
            require(has(route,"silicon-hydrogen"), "Same handler/index restores match when correct output returns");
            inputs.clear();
            require(!has(route,"silicon-hydrogen"), "Unrelated contents at the same handler/index receive no stale frame");
        }
        require(!NerfItems.affected(new ItemStack(silicon)), "Multi-route Silicon SG has no misleading global material frame");
        require(!NerfItems.affected(liquid(acid,2000).item), "Common acid has no global silicon-route frame");
        String ranges = description(new ItemStack(ore("ingotCastIron")),true);
        require(ranges.contains("纯铁") && ranges.contains("锻铁") && ranges.contains("生铁") && ranges.contains("过渗碳铁"), "Six product names are readable Chinese");
        require(!ranges.contains("carbon units") && !ranges.contains("Pig Iron"), "Iron Bath prose contains no leftover mixed-language formula");
        NerfDB.load(config,"1.1.1");
        System.out.println("PASS: ExtraNerf display regressions " + assertions + " assertions");
        return assertions;
    }
}
