import java.io.File;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

import codechicken.nei.PositionedStack;
import codechicken.nei.item.ItemFluidDisplay;
import codechicken.nei.recipe.Badge;
import codechicken.nei.recipe.IRecipeHandler;
import gregtech.api.GregTechAPI;
import gregtech.api.metatileentity.implementations.MTECable;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTRecipe;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import nerflens.*;

public final class FrameChecks {
    private static int assertions, nextId = 6000;
    private static Map<String, ItemStack> gtItems;
    private static Map<String, Fluid> gtFluids;
    private static final List<String> scenarios = new ArrayList<>();

    private static final class NamedItem extends Item {
        final String zh, en;
        NamedItem(String zh, String en) { this.zh = zh; this.en = en; setUnlocalizedName("fixture"); setHasSubtypes(true); }
        @Override public String getItemStackDisplayName(ItemStack stack) { return NerfText.useChinese ? zh : en; }
    }
    private static final class FluidContainer extends Item implements IFluidContainerItem {
        public FluidStack getFluid(ItemStack stack) {
            if (!stack.hasTagCompound()) return null;
            Fluid fluid = FluidRegistry.getFluid(stack.getTagCompound().getString("fluid"));
            return fluid == null ? null : new FluidStack(fluid, 1000);
        }
        public int getCapacity(ItemStack stack) { return 1000; }
        public int fill(ItemStack stack, FluidStack fluid, boolean doFill) { return 0; }
        public FluidStack drain(ItemStack stack, int maxDrain, boolean doDrain) { return null; }
    }
    private static Item named(String id, String zh, String en) {
        Item item = new NamedItem(zh, en);
        registerFixture(id, item);
        return item;
    }
    private static void registerFixture(String id, Item item) {
        // Raw registration gives these isolated fixtures the exact namespace IDs found in installed bytecode.
        try {
            Method raw = null;
            for (Method method : Item.itemRegistry.getClass().getDeclaredMethods())
                if (method.getName().equals("addObjectRaw")) raw = method;
            raw.setAccessible(true);
            raw.invoke(Item.itemRegistry, nextId++, id, item);
            Method setName = item.delegate.getClass().getDeclaredMethod("setName", String.class);
            setName.setAccessible(true); setName.invoke(item.delegate, id);
        } catch (Exception failure) { throw new RuntimeException(failure); }
    }
    private static ItemStack s(Item item, int n, int meta) { return new ItemStack(item, n, meta); }
    private static PositionedStack p(ItemStack stack) { return new PositionedStack(stack.copy(), 0, 0, false); }
    private static List<PositionedStack> ps(ItemStack... stacks) {
        List<PositionedStack> result = new ArrayList<>();
        for (ItemStack stack : stacks) result.add(p(stack));
        return result;
    }
    private static IRecipeHandler handler(final String map, final List<PositionedStack> inputs, final PositionedStack output) {
        return new IRecipeHandler() {
            public String getHandlerId() { return map; }
            public String getRecipeName() { return map; }
            public int numRecipes() { return 1; }
            public void drawBackground(int index) {}
            public void drawForeground(int index) {}
            public List<PositionedStack> getIngredientStacks(int index) { return inputs; }
            public List<PositionedStack> getOtherStacks(int index) { return Collections.emptyList(); }
            public PositionedStack getResultStack(int index) { return output; }
            public void onUpdate() {}
            public boolean hasOverlay(net.minecraft.client.gui.inventory.GuiContainer gui, net.minecraft.inventory.Container container, int index) { return false; }
            public codechicken.nei.api.IRecipeOverlayRenderer getOverlayRenderer(net.minecraft.client.gui.inventory.GuiContainer gui, int index) { return null; }
            public codechicken.nei.api.IOverlayHandler getOverlayHandler(net.minecraft.client.gui.inventory.GuiContainer gui, int index) { return null; }
            public List<String> handleTooltip(codechicken.nei.recipe.GuiRecipe<?> gui, List<String> tip, int index) { return tip; }
            public List<String> handleItemTooltip(codechicken.nei.recipe.GuiRecipe<?> gui, ItemStack stack, List<String> tip, int index) { return tip; }
            public boolean keyTyped(codechicken.nei.recipe.GuiRecipe<?> gui, char key, int code, int index) { return false; }
            public boolean mouseClicked(codechicken.nei.recipe.GuiRecipe<?> gui, int button, int index) { return false; }
        };
    }
    private static NerfDB.RecipeEntry recipe(String id) {
        for (NerfDB.RecipeEntry record : NerfDB.recipes()) if (id.equals(record.id)) return record;
        throw new AssertionError("Missing record: " + id);
    }
    private static void require(boolean value, String message) {
        assertions++;
        if (!value) throw new AssertionError(message);
    }
    private static boolean matched(IRecipeHandler handler, String id) {
        for (NerfDB.RecipeEntry record : NerfRecipes.match(handler, 0)) if (record.id.equals(id)) return true;
        return false;
    }
    private static boolean yellow(IRecipeHandler handler, PositionedStack stack) {
        List<Badge> badges = NerfBadges.forRecipe(handler, 0, stack, true);
        return badges.size() == 1 && badges.get(0).getColor() == 0xFFFFDE59;
    }
    private static boolean gold(IRecipeHandler handler, PositionedStack stack, boolean input) {
        List<Badge> badges = NerfBadges.forRecipe(handler, 0, stack, input);
        return badges.size() == 1 && badges.get(0).getColor() == 0xFFFFB82E;
    }
    private static String nbt(ItemStack stack) { return stack.writeToNBT(new NBTTagCompound()).toString(); }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        Bootstrap.func_151354_b();
        registerFixture("nei:fluid_display", new ItemFluidDisplay());
        NerfDB.load(new File(args[0]), "1.1.1");
        Field items = NerfRecipes.class.getDeclaredField("GT_ITEMS"); items.setAccessible(true);
        Field fluids = NerfRecipes.class.getDeclaredField("GT_FLUIDS"); fluids.setAccessible(true);
        gtItems = (Map<String, ItemStack>) items.get(null);
        gtFluids = (Map<String, Fluid>) fluids.get(null);
        // All non-scenario enum/material identities are unavailable in this isolated JVM.
        for (String line : Files.readAllLines(new File(args[1]).toPath(), StandardCharsets.UTF_8)) {
            if (line.startsWith("fluid:")) gtFluids.put(line.substring(6), null);
            else gtItems.put(line, null);
        }
        Item slab = named("TConstruct:CraftingSlab", "工具组装台", "Tool Station");
        Item station = named("TConstruct:ToolStationBlock", "工具组装台", "Tool Station");
        Item basic = named("fixture:lv_circuit", "基础电路", "Basic Circuit");
        Item saw = named("fixture:saw", "锯子", "Crafting Saw");
        Item unrelated = named("fixture:unrelated", "普通成品", "Unrelated product");
        OreDictionary.registerOre("circuitBasic", s(basic, 1, 0));
        OreDictionary.registerOre("craftingToolSaw", s(saw, 1, 0));
        Item ae = named("appliedenergistics2:item.ItemMultiMaterial", "空白样板", "Blank Pattern");
        Item part = named("appliedenergistics2:item.ItemMultiPart", "P2P 通道", "P2P Tunnel");
        Item wire = named("fixture:zpm_wire", "超导线", "Superconducting Wire");
        Item foil = named("fixture:naquarite_foil", "硅岩绝缘箔", "Translated insulator foil");
        Item coil = named("fixture:coil", "超导线圈", "Superconducting Coil");
        OreDictionary.registerOre("wireGt04SuperconductorZPM", s(wire, 1, 4));
        gtItems.put("Naquarite_Universal_Insulator_Foil", s(foil, 1, 0));
        gtItems.put("Casing_Coil_Superconductor", s(coil, 1, 0));
        Item blade = named("fixture:component_blade", "钻石锯片", "Diamond Sawblade");
        Item buzz = named("fixture:buzz_head", "钻石锯片", "Diamond Sawblade");
        Item bloodGear = named("fixture:blood_gear", "血钢齿轮", "Blood Steel Gear");
        gtItems.put("Component_Sawblade_Diamond", s(blade, 1, 0));
        gtItems.put("alloy:BLOODSTEEL:gear", s(bloodGear, 1, 0));
        OreDictionary.registerOre("toolHeadBuzzSawDiamond", s(buzz, 1, 0));
        Item dense = named("fixture:dense_plate", "致密下界合金板", "Dense Netherite Plate");
        Item fusion = named("fixture:fusion", "首级控制电脑", "First Fusion Computer");
        OreDictionary.registerOre("plateDenseNetherite", s(dense, 1, 0));
        gtItems.put("FusionComputer_LuV", s(fusion, 1, 0));
        Item proxy = named("fixture:proxy", "合成输入代理", "Crafting Input Proxy");
        Item uqv = named("gtnhextranerf:uqv_sensor", "传感器", "Sensor");
        Item uev = named("fixture:uev_sensor", "传感器", "Sensor");
        gtItems.put("Hatch_CraftingInput_Bus_Slave", s(proxy, 1, 0));
        gtItems.put("Sensor_UEV", s(uev, 1, 0));

        Item tpv = named("fixture:tpv", "钛铂钒合金板", "TPV Plate");
        Item pps = named("fixture:pps", "聚苯硫醚箔", "PPS Foil");
        OreDictionary.registerOre("plateTPV", s(tpv, 1, 0));
        OreDictionary.registerOre("foilPolyphenyleneSulfide", s(pps, 1, 0));
        ShapelessOreRecipe modifiedAe = new ShapelessOreRecipe(s(ae, 1, 990), s(tpv, 1, 0));
        ShapelessOreRecipe untouchedAe = new ShapelessOreRecipe(s(ae, 1, 991), s(unrelated, 1, 0));
        CraftingManager.getInstance().getRecipeList().add(modifiedAe);
        CraftingManager.getInstance().getRecipeList().add(untouchedAe);
        GregTechAPI.sBlockMachines = Blocks.stone;
        GregTechAPI.METATILEENTITIES[0] = new MTECable(2048, true);
        GregTechAPI.METATILEENTITIES[1] = new MTECable(512, true);
        GregTechAPI.METATILEENTITIES[2] = new MTECable(2048, false);
        GregTechAPI.METATILEENTITIES[3] = new MTECable(32, true);
        GregTechAPI.METATILEENTITIES[4] = new MTECable(32, true);
        ItemStack changedCable = new ItemStack(Blocks.stone, 1, 4);
        OreDictionary.registerOre("cableGt01Fixture", changedCable);
        OreDictionary.registerOre("cableGt01FixtureUnchanged", new ItemStack(Blocks.stone, 1, 3));
        RecipeMap assembler = new RecipeMap();
        GTRecipe cableRecipe = new GTRecipe(new ItemStack[]{s(pps, 1, 0)}, new ItemStack[]{changedCable});
        assembler.recipes.add(cableRecipe);
        RecipeMap.ALL_RECIPE_MAPS.put("gt.recipe.assembler", assembler);
        GTRecipe.RecipeAssemblyLine assembly = new GTRecipe.RecipeAssemblyLine();
        assembly.mInputs = new ItemStack[]{s(tpv, 2, 0)}; assembly.mOutput = s(ae, 1, 992);
        GTRecipe.RecipeAssemblyLine.sAssemblylineRecipes.add(assembly);
        String originalRecipeInput = nbt((ItemStack) modifiedAe.getInput().get(0));
        String originalCableInput = nbt(cableRecipe.mInputs[0]);
        String originalCableOutput = nbt(changedCable);
        NerfItems.initialize();
        require(nbt((ItemStack) modifiedAe.getInput().get(0)).equals(originalRecipeInput), "Native crafting input unchanged");
        require(nbt(cableRecipe.mInputs[0]).equals(originalCableInput), "Native GT input unchanged");
        require(nbt(changedCable).equals(originalCableOutput), "Native GT output unchanged");
        require(NerfItems.affected(s(ae, 1, 990)), "Actual AE2 TPV crafting product discovered before opening its recipe");
        require(NerfItems.affected(s(ae, 1, 992)), "Actual AE2 TPV Assembly Line product discovered");
        require(!NerfItems.affected(s(ae, 1, 991)), "Unchanged AE2 item not blanket-marked by mod selector");
        require(!NerfItems.affected(new ItemStack(Blocks.stone, 1, 0)), "Route-only cable change does not globally mark a product");
        require(!NerfItems.affected(new ItemStack(Blocks.stone, 1, 1)), "Unchanged HV cable not marked");
        require(!NerfItems.affected(new ItemStack(Blocks.stone, 1, 2)), "EV bare wire not marked");
        require(!NerfItems.affected(new ItemStack(Blocks.stone, 1, 3)), "Unchanged LV cable not blanket-marked by ore prefix");
        require(NerfItems.affected(new ItemStack(Blocks.stone, 1, 4)), "Actual PPS cable product marked");
        scenarios.add("read-only native recipe index and broad-rule false-positive controls");

        for (boolean zh : new boolean[]{true, false}) {
            NerfText.useChinese = zh;
            List<PositionedStack> ingredients = ps(s(station, 1, 0), s(saw, 1, 0), s(basic, 1, 0));
            PositionedStack result = p(s(slab, 1, 1));
            IRecipeHandler craft = handler("crafting", ingredients, result);
            require(matched(craft, "tool-station-slab"), "Tool Station slab matches in both display languages");
            require(yellow(craft, ingredients.get(2)), "Added LV circuit is yellow");
            require(NerfBadges.forRecipe(craft, 0, ingredients.get(1), true).isEmpty(), "Original saw not described as added");
            require(gold(craft, result, false), "Slab product is gold");
            require(!matched(handler("crafting", ps(s(station, 1, 0), s(saw, 1, 0)), result), "tool-station-slab"), "Missing LV circuit cannot match");
            require(!matched(handler("crafting", ps(s(station, 1, 0), s(basic, 1, 0)), result), "tool-station-slab"), "Missing original saw cannot match");
            require(!matched(handler("crafting", ps(s(station, 1, 0), s(saw, 1, 0), s(basic, 2, 0)), result), "tool-station-slab"), "Wrong circuit quantity cannot match");
            require(!NerfItems.affected(s(station, 1, 0)), "Full Tool Station not flagged despite identical translated name");
            require(!NerfItems.affected(s(slab, 1, 0)), "Crafting Station slab metadata not flagged");
            require(NerfRecipes.entriesFor(s(slab, 1, 1)).stream().anyMatch(e -> e.id.equals("tooltip-060")), "Slab tooltip follows exact item identity");
            PositionedStack usedSlab = p(s(slab, 64, 1));
            IRecipeHandler unrelatedRecipe = handler("fixture.unmodified", Arrays.asList(usedSlab), p(s(unrelated, 1, 0)));
            require(NerfRecipes.match(unrelatedRecipe, 0).isEmpty(), "Usage recipe itself remains unmodified");
            require(gold(unrelatedRecipe, usedSlab, true), "Nerfed slab remains gold as a material at a different quantity");
            require(NerfBadges.forRecipe(unrelatedRecipe, 0, p(s(unrelated, 1, 0)), false).isEmpty(), "Unmodified product not framed from affected input");

            List<PositionedStack> coilInputs = ps(s(wire, 32, 4), s(foil, 8, 0));
            IRecipeHandler coilRecipe = handler("gt.recipe.assembler", coilInputs, p(s(coil, 1, 0)));
            require(matched(coilRecipe, "superconductor-coil-zpm"), "ZPM coil accepts translated foil display name");
            require(yellow(coilRecipe, coilInputs.get(1)), "ZPM insulator foil is yellow");
            require(!matched(handler("gt.recipe.assembler", ps(s(wire, 32, 4), s(foil, 4, 0)), p(s(coil, 1, 0))), "superconductor-coil-zpm"), "Old foil quantity does not match changed route");
            require(matched(handler("crafting", ps(s(bloodGear, 1, 0)), p(s(blade, 1, 0))), "diamond-sawblade"), "GT component sawblade matches");
            require(!NerfItems.affected(s(buzz, 1, 0)), "Unrelated buzzsaw tool head is not the component sawblade");
            require(NerfItems.affected(s(ae, 8, 52)), "Blank Pattern metadata 52 matches at any material quantity");
            require(!NerfItems.affected(s(ae, 1, 54)), "Other AE2 material sharing display name does not match Blank Pattern");
            require(!NerfItems.affected(s(part, 1, 460)), "Channel capacity does not cause a global material frame");
            require(!NerfItems.affected(s(part, 1, 461)), "Redstone P2P not flagged with ME channel rule");
            require(!NerfItems.affected(s(part, 1, 470)), "GT power P2P not flagged with ME channel rule");

            List<PositionedStack> denseInputs = ps(s(dense, 64, 0));
            IRecipeHandler fusionRecipe = handler("gt.recipe.fakeAssemblylineProcess", denseInputs, p(s(fusion, 1, 0)));
            require(matched(fusionRecipe, "dense-netherite"), "Exact first Fusion Computer recipe is now live");
            require(yellow(fusionRecipe, denseInputs.get(0)), "64 Dense Netherite Plates framed as changed input");
            require(!matched(handler("gt.recipe.fakeAssemblylineProcess", ps(s(dense, 1, 0)), p(s(fusion, 1, 0))), "dense-netherite"), "Original one-plate recipe not misidentified as current");
            List<PositionedStack> proxyInputs = ps(s(uqv, 1, 0));
            IRecipeHandler proxyRecipe = handler("gt.recipe.spaceAssembler", proxyInputs, p(s(proxy, 1, 0)));
            require(matched(proxyRecipe, "crafting-input-proxy"), "Crafting Input Proxy recipe now identified");
            require(yellow(proxyRecipe, proxyInputs.get(0)), "Hidden UQV Sensor framed as changed ingredient");
            require(!matched(handler("gt.recipe.spaceAssembler", ps(s(uev, 1, 0)), p(s(proxy, 1, 0))), "crafting-input-proxy"), "Visually identical UEV Sensor cannot substitute");
            require(NerfItems.affected(s(proxy, 8, 0)), "Proxy follows into material slots");
        }
        scenarios.add("bilingual Tool Station, ZPM coil, sawblade, Blank Pattern, P2P, Fusion Computer and Proxy regressions");

        Item food = named("TConstruct:strangeFood", "凝胶状黏液", "Gelatinous Slime");
        Item gel = named("TConstruct:slime.gel", "黏液方块", "Slime Block");
        Item leaves = named("TConstruct:slime.leaves", "黏液树叶", "Slimy Leaves");
        Item soil = named("TConstruct:CraftedSoil", "黏液土", "Slimy Soil");
        ItemStack[] slimes = {s(food, 1, 0), s(gel, 1, 0), s(gel, 1, 1), s(gel, 1, 2), s(leaves, 1, 0), s(soil, 1, 0)};
        for (ItemStack slime : slimes) require(!NerfItems.affected(slime), "Removed-use material has no global material frame: " + nbt(slime));
        require(!NerfItems.affected(s(soil, 1, 1)), "Unchanged Crafted Soil metadata not framed");
        scenarios.add("TConstruct slime-family omissions and metadata controls");

        Fluid benzene = new Fluid("fixture_benzene"), water = new Fluid("fixture_water");
        FluidRegistry.registerFluid(benzene); FluidRegistry.registerFluid(water);
        gtFluids.put("Benzene:fluid", benzene);
        ItemStack benzeneIcon = ItemFluidDisplay.createStack(benzene, 1000L);
        ItemStack waterIcon = ItemFluidDisplay.createStack(water, 1000L);
        require(benzeneIcon.getItem() == waterIcon.getItem(), "Fluid icons really share one item identity");
        require(!NerfRecipes.entriesFor(benzeneIcon).isEmpty(), "Affected fuel fluid icon framed");
        require(NerfRecipes.entriesFor(waterIcon).isEmpty(), "Different fluid NBT cannot inherit cached frame");
        require(!NerfRecipes.entriesFor(benzeneIcon).isEmpty(), "Fluid marker stays correct after other fluid");
        Item container = new FluidContainer(); registerFixture("fixture:fluid_container", container);
        ItemStack benzeneCell = s(container, 1, 0), waterCell = s(container, 1, 0);
        NBTTagCompound benzeneTag = new NBTTagCompound(), waterTag = new NBTTagCompound();
        benzeneTag.setString("fluid", benzene.getName()); waterTag.setString("fluid", water.getName());
        benzeneCell.setTagCompound(benzeneTag); waterCell.setTagCompound(waterTag);
        require(!NerfRecipes.entriesFor(benzeneCell).isEmpty(), "Same-metadata fluid container identifies affected contents");
        require(NerfRecipes.entriesFor(waterCell).isEmpty(), "Same item and metadata with different fluid NBT is not framed");
        waterTag.setString("fluid", benzene.getName());
        require(!NerfRecipes.entriesFor(waterCell).isEmpty(), "Changed fluid NBT is re-evaluated instead of using a stale item cache");
        scenarios.add("fluid NBT cache isolation");

        PositionedStack rotating = p(s(slab, 1, 1));
        rotating.items = new ItemStack[]{s(slab, 1, 1), s(slab, 1, 0)};
        rotating.item = rotating.items[1];
        IRecipeHandler usage = handler("fixture.unmodified", Arrays.asList(rotating), p(s(unrelated, 1, 0)));
        require(NerfBadges.forRecipe(usage, 0, rotating, true).isEmpty(), "Unchanged visible alternative does not inherit another alternative's frame");
        rotating.item = rotating.items[0];
        require(gold(usage, rotating, true), "Affected visible alternative gets frame when cycling");
        scenarios.add("cycling ingredient alternatives");

        assertions += TooltipFixChecks.run(ae, unrelated, gtItems, gtFluids, args[2]);
        assertions += MaterialPolicyChecks.run(unrelated, gtItems, gtFluids, args[2]);
        assertions += ExtraNerf111Checks.run(gtItems, gtFluids);
        assertions += VersionMatrixChecks.run(new File(args[0]), gtItems);
        assertions += nerflens.CertificateTooltipChecks.run(new File(args[0]));
        scenarios.add("fluid descriptions, recipe-only products, scoped provenance, bilingual summaries and Shift");

        File override = new File(args[0], "nerflens/user_nerfs.json"); override.getParentFile().mkdirs();
        Files.write(override.toPath(), Collections.singletonList("{\"entries\":[{\"id\":\"language-fixture\",\"materialInputsChanged\":true,\"matchStacks\":[{\"dn\":\"=Language Sensitive Fixture\"}],\"summary\":[\"fixture\"]}]}"), StandardCharsets.UTF_8);
        NerfDB.load(new File(args[0]), "1.1.1");
        Item languageItem = named("fixture:language_item", "独立中文名称", "Language Sensitive Fixture");
        NerfText.useChinese = true;
        require(!NerfItems.affected(s(languageItem, 1, 0)), "Language-dependent fixture initially has no Chinese name match");
        NerfText.useChinese = false;
        require(NerfItems.affected(s(languageItem, 1, 0)), "Switching to English clears a cached negative name match");
        NerfText.useChinese = true;
        require(!NerfItems.affected(s(languageItem, 1, 0)), "Switching back clears a cached positive name match");
        scenarios.add("user overrides and language-switch cache invalidation");

        long start = System.nanoTime();
        for (int i = 0; i < 100000; i++) NerfItems.affected(s(slab, 64, 1));
        double milliseconds = (System.nanoTime() - start) / 1000000.0;
        require(milliseconds < 3000, "Cached classification suitable for recipe drawing: " + milliseconds + " ms for 100000 calls");
        System.out.println("PASS: " + assertions + " assertions; scenarios=" + scenarios);
        System.out.println("Cached classification: " + milliseconds + " ms / 100000 calls");
        Files.write(new File(args[0], "frame-check-summary.txt").toPath(),
            Arrays.asList("assertions=" + assertions, "scenarios=" + scenarios, "cached_100000_calls_ms=" + milliseconds), StandardCharsets.UTF_8);
    }
}
