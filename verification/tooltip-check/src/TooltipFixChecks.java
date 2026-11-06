import java.lang.reflect.*;
import java.util.*;
import codechicken.nei.item.ItemFluidDisplay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fluids.*;
import nerflens.*;

/** Exercises the built NerfLens tooltip code with isolated game identities and a font without OpenGL. */
public final class TooltipFixChecks {
    private static int assertions, nextId = 7200;
    private static sun.misc.Unsafe unsafe;
    private static final List<String> samples = new ArrayList<>();

    public static final class TestFont extends FontRenderer {
        public TestFont() { super(null, null, null, false); }
        @Override public int getStringWidth(String text) {
            return EnumChatFormatting.getTextWithoutFormattingCodes(text).length() * 6;
        }
        @Override public List<String> listFormattedStringToWidth(String text, int width) {
            return Collections.singletonList(text);
        }
    }

    private static void require(boolean result, String message) {
        assertions++;
        if (!result) throw new AssertionError(message);
    }

    private static Item fixture(String id) throws Exception {
        Item item = new Item().setUnlocalizedName("tooltip_fixture");
        Method raw = null;
        for (Method method : Item.itemRegistry.getClass().getDeclaredMethods())
            if (method.getName().equals("addObjectRaw")) raw = method;
        raw.setAccessible(true);
        raw.invoke(Item.itemRegistry, nextId++, id, item);
        Method name = item.delegate.getClass().getDeclaredMethod("setName", String.class);
        name.setAccessible(true); name.invoke(item.delegate, id);
        return item;
    }

    private static String describe(ItemStack stack, boolean expanded) {
        List<String> lines = new ArrayList<>();
        TooltipHandler.appendChanges(stack, lines, expanded);
        return EnumChatFormatting.getTextWithoutFormattingCodes(String.join("\n", lines));
    }

    private static boolean hasRecipe(ItemStack stack, String id) {
        return NerfItems.recipesFor(stack).stream().anyMatch(r -> r.id.equals(id));
    }

    public static int run(Item ae, Item unrelated, Map<String, ItemStack> gtItems, Map<String, Fluid> gtFluids,
                          String mode) throws Exception {
        Field access = sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); access.setAccessible(true);
        unsafe = (sun.misc.Unsafe) access.get(null);
        Minecraft mc = (Minecraft) unsafe.allocateInstance(Minecraft.class);
        mc.fontRenderer = (FontRenderer) unsafe.allocateInstance(TestFont.class);
        Field singleton = Minecraft.class.getDeclaredField("theMinecraft"); singleton.setAccessible(true);
        singleton.set(null, mc);

        Fluid benzene = FluidRegistry.getFluid("fixture.benzene");
        // Reuse the fluid previously registered by the frame regression scenarios.
        Field cache = NerfRecipes.class.getDeclaredField("GT_FLUIDS"); cache.setAccessible(true);
        benzene = ((Map<String, Fluid>) cache.get(null)).get("Benzene:fluid");
        require(benzene != null, "Benzene fixture registered by frame checks");
        Fluid phenol = new Fluid("fixture.tooltip.phenol"); FluidRegistry.registerFluid(phenol);
        Fluid water = new Fluid("fixture.tooltip.water"); FluidRegistry.registerFluid(water);
        Fluid medium = new Fluid("fixture.tooltip.medium"); FluidRegistry.registerFluid(medium);
        Fluid solder = new Fluid("fixture.tooltip.solder"); FluidRegistry.registerFluid(solder);
        gtFluids.put("Phenol:fluid", phenol);
        gtFluids.put("BioMediumRaw:fluid", medium);
        gtFluids.put("misc:MUTATED_LIVING_SOLDER:fluid", solder);
        ItemStack benzeneIcon = ItemFluidDisplay.createStack(benzene, 1000L);
        ItemStack phenolIcon = ItemFluidDisplay.createStack(phenol, 1000L);
        ItemStack waterIcon = ItemFluidDisplay.createStack(water, 1000L);
        ItemStack mediumIcon = ItemFluidDisplay.createStack(medium, 1L);
        ItemStack solderIcon = ItemFluidDisplay.createStack(solder, 1L);
        Item pcb = fixture("fixture:tooltip_pcb");
        gtItems.put("Circuit_Board_Bio_Ultra", new ItemStack(pcb, 1, 0));
        require(NerfRecipes.entriesFor(new ItemStack(pcb)).isEmpty(), "PCB product has no standalone item entry");
        require(NerfRecipes.entriesFor(mediumIcon).isEmpty(), "Culture medium has no standalone item entry");
        require(hasRecipe(solderIcon, "mutated-solder"), "Product identity is independent of recipe output quantity");
        require(hasRecipe(new ItemStack(ae, 1, 990), "ae2-tpv-materials"), "AE2 discovery retains its specific recipe source");
        require(!hasRecipe(new ItemStack(ae, 1, 991), "ae2-tpv-materials"), "Unchanged AE2 variant inherits no recipe description");
        require(hasRecipe(new ItemStack(Blocks.stone, 1, 4), "cable-pps"), "Changed PPS cable retains its recipe source");
        require(!hasRecipe(new ItemStack(Blocks.stone, 1, 3), "cable-pps"), "Unchanged cable inherits no broad-rule explanation");

        for (boolean zh : new boolean[]{true, false}) {
            NerfText.useChinese = zh;
            String fuelShort = describe(benzeneIcon, false), fuelFull = describe(benzeneIcon, true);
            require(fuelShort.contains(zh ? "1/4" : "one quarter"), "Benzene has a default fuel reduction summary");
            require(fuelFull.contains("360→90") && fuelFull.contains("288→72"), "Shift exposes original and current fuel values");
            require(describe(phenolIcon, false).contains(zh ? "1/4" : "one quarter"), "Phenol shares the fuel description");
            require(describe(waterIcon, false).isEmpty(), "Unchanged water has no ExtraNerf description");
            require(describe(new ItemStack(unrelated), true).isEmpty(), "Unchanged item has no ExtraNerf description");
            String aeText = describe(new ItemStack(ae, 64, 990), false);
            require(aeText.contains(zh ? "钛铂钒" : "TPV"), "Recipe-only AE2 product receives the actual change summary");
            require(describe(new ItemStack(ae, 64, 991), true).isEmpty(), "Other AE2 metadata stays unannotated");
            require(describe(new ItemStack(Blocks.stone, 1, 4), true).contains(zh ? "聚苯硫醚" : "PPS"),
                    "PPS cable description comes from the discovered matching recipe");
            String pcbText = describe(new ItemStack(pcb, 8, 0), false);
            require(pcbText.contains(zh ? "铜" : "Copper"), "Recipe-only circuit board receives its changed material description");
            String mediumText = describe(mediumIcon, false);
            require(mediumText.contains(zh ? "奇异" : "DEEP EARTH"), "Recipe-only culture fluid receives its added material description");
            require(!mediumText.contains("item card"), "Standalone item description does not refer to an absent comparison card");
            String solderText = describe(solderIcon, true);
            require(solderText.contains(zh ? "核能之星" : "NuclearStar"), "Small material quantities retain the solder recipe explanation");
            NerfDB.RecipeEntry slab = NerfDB.recipes().stream().filter(r -> r.id.equals("tool-station-slab")).findFirst().get();
            List<String> inputTip = new ArrayList<>();
            TooltipHandler.appendRecipeChanges(inputTip, Collections.singletonList(slab), false);
            require(String.join("\n", inputTip).contains("LV"), "Changed input tooltip explains the circuit addition in its current recipe");
            require(inputTip.stream().filter(s -> s.contains("more")).count() == 1, "Only one Shift prompt for a recipe summary");
            samples.add((zh ? "zh_CN" : "en_US") + " benzene summary:\n" + fuelShort);
            samples.add((zh ? "zh_CN" : "en_US") + " benzene Shift:\n" + fuelFull);
            samples.add((zh ? "zh_CN" : "en_US") + " AE2 product:\n" + aeText);
            samples.add((zh ? "zh_CN" : "en_US") + " culture medium:\n" + mediumText);
        }

        String original = benzeneIcon.writeToNBT(new NBTTagCompound()).toString();
        describe(benzeneIcon, true);
        require(original.equals(benzeneIcon.writeToNBT(new NBTTagCompound()).toString()), "Description building does not mutate fluid NBT");
        if (mode.equals("with-chromatic")) {
            assertions += NativeChromaticChecks.run(unsafe, benzene, water, medium, new ItemStack(pcb));
        } else {
            NerfChromaticTooltips.register();
            Field field = NerfChromaticTooltips.class.getDeclaredField("registered"); field.setAccessible(true);
            require(!field.getBoolean(null), "Chromatic absence is safe and does not register a bridge");
        }
        NerfText.useChinese = true;
        java.nio.file.Files.write(java.nio.file.Paths.get("tooltip-samples-" + mode + ".txt"), samples,
                                 java.nio.charset.StandardCharsets.UTF_8);
        System.out.println("PASS: tooltip fix " + assertions + " assertions (" + mode + ")");
        return assertions;
    }
}
