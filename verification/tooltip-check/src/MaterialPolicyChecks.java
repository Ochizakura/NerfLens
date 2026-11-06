import java.lang.reflect.*;
import java.util.*;
import codechicken.nei.PositionedStack;
import codechicken.nei.item.ItemFluidDisplay;
import codechicken.nei.recipe.*;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.*;
import net.minecraftforge.oredict.OreDictionary;
import nerflens.*;

/** Distinguish production material changes from yield, behavior, removed usage and current-recipe-only changes. */
public final class MaterialPolicyChecks {
    private static int assertions, nextId = 9200;
    private static void require(boolean result, String message) {
        assertions++;
        if (!result) throw new AssertionError(message);
    }
    private static Item fixture(String id) throws Exception {
        Item item = new Item().setUnlocalizedName("material_policy_fixture").setHasSubtypes(true);
        Method raw = null;
        for (Method method : Item.itemRegistry.getClass().getDeclaredMethods())
            if (method.getName().equals("addObjectRaw")) raw = method;
        raw.setAccessible(true); raw.invoke(Item.itemRegistry, nextId++, id, item);
        Method name = item.delegate.getClass().getDeclaredMethod("setName", String.class);
        name.setAccessible(true); name.invoke(item.delegate, id);
        return item;
    }
    private static PositionedStack p(ItemStack stack) { return new PositionedStack(stack.copy(), 0, 0, false); }
    private static IRecipeHandler handler(String map, List<PositionedStack> inputs, List<PositionedStack> outputs) {
        return (IRecipeHandler) Proxy.newProxyInstance(IRecipeHandler.class.getClassLoader(),
            new Class<?>[]{IRecipeHandler.class}, (proxy, method, arguments) -> {
                switch (method.getName()) {
                    case "getHandlerId": case "getRecipeName": return map;
                    case "getIngredientStacks": return inputs;
                    case "getResultStack": return outputs.isEmpty() ? null : outputs.get(0);
                    case "getOtherStacks": return outputs.size() < 2 ? Collections.emptyList() : outputs.subList(1, outputs.size());
                    case "hashCode": return System.identityHashCode(proxy);
                    case "equals": return proxy == arguments[0];
                    case "numRecipes": return 1;
                    default: return null;
                }
            });
    }
    private static boolean framed(IRecipeHandler handler, PositionedStack stack, boolean input, int color) {
        List<Badge> badges = NerfBadges.forRecipe(handler, 0, stack, input);
        return badges.size() == 1 && badges.get(0).getColor() == color;
    }
    private static String description(ItemStack stack) {
        List<String> lines = new ArrayList<>(); TooltipHandler.appendChanges(stack, lines, true);
        return String.join("\n", lines).replace(",", "");
    }
    public static int run(Item unrelated, Map<String, ItemStack> items, Map<String, Fluid> fluids, String mode) throws Exception {
        Item bronze = fixture("fixture:policy_bronze"), tpv = fixture("fixture:policy_tpv");
        OreDictionary.registerOre("dustBronze", new ItemStack(bronze));
        OreDictionary.registerOre("dustTPV", new ItemStack(tpv));
        Fluid helium = new Fluid("fixture.policy.helium"), plasma = new Fluid("fixture.policy.helium_plasma");
        FluidRegistry.registerFluid(helium); FluidRegistry.registerFluid(plasma);
        fluids.put("Helium:gas", helium); fluids.put("Helium:plasma", plasma);
        ItemStack gasIcon = ItemFluidDisplay.createStack(helium, 1500L);
        ItemStack plasmaIcon = ItemFluidDisplay.createStack(plasma, 1440L);
        ItemStack fuel = ItemFluidDisplay.createStack(fluids.get("Benzene:fluid"), 1000L);
        Item endstone = fixture("fixture:policy_endstone"), tungstate = fixture("fixture:policy_tungstate");
        OreDictionary.registerOre("dustEndstone", new ItemStack(endstone));
        OreDictionary.registerOre("dustTungstate", new ItemStack(tungstate));
        List<PositionedStack> out = Arrays.asList(p(new ItemStack(tungstate)), p(gasIcon));
        IRecipeHandler centrifuge = handler("gt.recipe.centrifuge", Arrays.asList(p(new ItemStack(endstone))), out);
        IRecipeHandler usage = handler("fixture.unmodified", Collections.emptyList(), Arrays.asList(p(new ItemStack(unrelated))));

        Item raw = fixture("fixture:policy_raw_neutronium"), neutronium = fixture("fixture:policy_neutronium");
        Item circuit = fixture("gregtech:gt.integrated_circuit");
        items.put("nh:RawNeutronium", new ItemStack(raw));
        OreDictionary.registerOre("dustNeutronium", new ItemStack(neutronium));
        List<PositionedStack> mixingInputs = Arrays.asList(p(new ItemStack(neutronium)), p(new ItemStack(circuit, 1, 1)), p(plasmaIcon));
        IRecipeHandler mixer = handler("gt.recipe.mixer", mixingInputs, Arrays.asList(p(new ItemStack(raw))));
        Item dense = fixture("fixture:policy_dense_plate");
        OreDictionary.registerOre("plateDenseNetherite", new ItemStack(dense));
        ItemStack fusion = items.get("FusionComputer_LuV");
        PositionedStack denseInput = p(new ItemStack(dense, 64, 0));
        IRecipeHandler fusionRecipe = handler("gt.recipe.fakeAssemblylineProcess", Arrays.asList(denseInput), Arrays.asList(p(fusion)));

        for (boolean zh : new boolean[]{true, false}) {
            NerfText.useChinese = zh;
            require(!NerfItems.affected(new ItemStack(bronze)), "Bronze output 3 to 1 has no global frame");
            require(!NerfItems.affected(new ItemStack(tpv)), "TPV output 7 to 3 has no global frame");
            require(!NerfItems.affected(gasIcon), "Helium output reduction has no global frame");
            require(!NerfItems.affected(fuel), "Benzene fuel-value change has no global frame");
            require(!NerfItems.affected(plasmaIcon), "Helium Plasma's changed use is not its own material recipe");
            require(!NerfItems.affected(new ItemStack(dense)), "Changed Fusion input does not globally frame Dense Plate");
            require(NerfItems.affected(new ItemStack(raw)), "Increased plasma input globally frames Raw Neutronium");
            require(NerfItems.affected(fusion), "Increased Dense Plate input globally frames the Fusion Computer");
            require(description(gasIcon).contains("4320") && description(gasIcon).contains("1500"), "Helium retains both quantities in its detailed description");
            require(description(fuel).contains("360→90"), "Fuel description remains available without a global frame");
            require(framed(centrifuge, out.get(1), false, 0xFFFFB82E), "Changed Helium output still has a local recipe frame");
            require(NerfBadges.forRecipe(usage, 0, p(gasIcon), true).isEmpty(), "Helium does not light up in an unrelated usage recipe");
            require(framed(mixer, mixingInputs.get(2), true, 0xFFFFDE59), "Changed Helium Plasma input remains yellow in its recipe");
            require(framed(fusionRecipe, denseInput, true, 0xFFFFDE59), "64 Dense Plates remain yellow in the Fusion recipe");
            require(NerfBadges.forRecipe(usage, 0, p(new ItemStack(dense)), true).isEmpty(), "Dense Plates do not light up in unrelated recipes");
            require(framed(usage, p(fusion), true, 0xFFFFB82E), "Changed-material product remains gold when used in a different recipe");
            List<String> tip = new ArrayList<>();
            TooltipHandler.appendRecipeChanges(tip, NerfRecipes.match(mixer, 0), true);
            require(String.join("\n", tip).replace(",", "").contains("1440"), "Current plasma input tooltip explains the increased amount");
        }
        if (mode.equals("with-chromatic")) {
            assertions += NativeChromaticChecks.checkHelium(helium);
        }
        NerfText.useChinese = true;
        System.out.println("PASS: material-frame policy " + assertions + " assertions");
        return assertions;
    }
}
