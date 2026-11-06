import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import net.minecraft.client.resources.*;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.*;
import net.minecraftforge.oredict.OreDictionary;
import codechicken.nei.PositionedStack;
import codechicken.nei.item.ItemFluidDisplay;
import codechicken.nei.recipe.*;
import gregtech.api.GregTechAPI;
import nerflens.*;

/** Production matching and translation precedence, exercised with real MC/NEI primitives and isolated registries. */
public final class ExtraNerf111Checks {
    private static int assertions, nextId = 16000;
    private static void require(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
    private static Item item(String name) throws Exception {
        Item value = new Item().setUnlocalizedName("nerflens_111_fixture").setHasSubtypes(true);
        Method raw = null;
        for (Method method : Item.itemRegistry.getClass().getDeclaredMethods())
            if (method.getName().equals("addObjectRaw")) raw = method;
        raw.setAccessible(true); raw.invoke(Item.itemRegistry, nextId++, name, value);
        Method setName = value.delegate.getClass().getDeclaredMethod("setName", String.class);
        setName.setAccessible(true); setName.invoke(value.delegate, name);
        return value;
    }
    private static ItemStack stack(Item item, int count) { return new ItemStack(item, count, 0); }
    private static Item ore(String key) throws Exception {
        Item value = item("fixture111:" + key.toLowerCase());
        OreDictionary.registerOre(key, stack(value, 1));
        return value;
    }
    private static PositionedStack p(ItemStack stack) { return new PositionedStack(stack.copy(), 0, 0, false); }
    private static List<PositionedStack> positions(ItemStack... stacks) {
        List<PositionedStack> values = new ArrayList<>();
        for (ItemStack stack : stacks) values.add(p(stack));
        return values;
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
    private static boolean badge(IRecipeHandler handler, ItemStack stack, boolean input, int color) {
        List<Badge> values = NerfBadges.forRecipe(handler, 0, p(stack), input);
        return values.size() == 1 && values.get(0).getColor() == color;
    }
    private static ItemStack flower(Item item, int count, String type) {
        ItemStack value = stack(item, count); NBTTagCompound tag = new NBTTagCompound(); tag.setString("type", type);
        value.setTagCompound(tag); return value;
    }
    private static final class Resources implements IResourceManager {
        final Map<String,String> files = new LinkedHashMap<>();
        public Set<String> getResourceDomains() { return new LinkedHashSet<>(Arrays.asList("gtnhextranerf","othermod")); }
        public IResource getResource(ResourceLocation location) throws IOException { return getAllResources(location).get(0); }
        public List<IResource> getAllResources(ResourceLocation location) throws IOException {
            String text = files.get(location.getResourceDomain() + ":" + location.getResourcePath());
            if (text == null) throw new FileNotFoundException();
            return Collections.singletonList(new IResource() {
                public InputStream getInputStream() { return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)); }
                public boolean hasMetadata() { return false; }
                public IMetadataSection getMetadata(String name) { return null; }
                public String getResourcePackName() { return "precedence fixture"; }
            });
        }
    }
    private static String english(String key) throws Exception {
        for (String line : Files.readAllLines(Paths.get("official/en_US.lang"), StandardCharsets.UTF_8))
            if (line.startsWith(key + "=")) return line.substring(key.length() + 1);
        throw new AssertionError("Missing official English key " + key);
    }
    private static void translationChecks() throws Exception {
        Resources resources = new Resources();
        resources.files.put("gtnhextranerf:lang/en_US.lang", new String(Files.readAllBytes(Paths.get("official/en_US.lang")), StandardCharsets.UTF_8));
        resources.files.put("gtnhextranerf:lang/zh_CN.lang", new String(Files.readAllBytes(Paths.get("official/zh_CN.lang")), StandardCharsets.UTF_8));
        List<String> zh = Arrays.asList("en_US", "zh_CN");
        String key = "item.gtnhextranerf.magicnetized_manasteel_rod.name", en = english(key);
        NerfTranslations.reloaded(resources, zh);
        require(NerfTranslations.fallback(key, en).equals("魔法磁化魔力钢棒"), "Missing native Chinese key receives fallback");
        require(NerfTranslations.fallback("foreign.untranslated.key", "foreign.untranslated.key").equals("foreign.untranslated.key"), "Unowned keys remain untouched");
        require(NerfTranslations.fallback(key, "动态外部译名").equals("动态外部译名"), "Directly injected translations take precedence");
        require(NerfWikiNames.display("机器外壳").equals("机器外壳"), "Existing Chinese names are preserved");
        long version = NerfTranslations.generation();
        resources.files.put("othermod:lang/zh_CN.lang", key + "=新版官方棒材\nothermod.machine.name=Mass Fabricator\n");
        NerfTranslations.reloaded(resources, zh);
        require(NerfTranslations.generation() > version, "Same-language resource reload advances cache generation");
        require(NerfTranslations.fallback(key, "新版官方棒材").equals("新版官方棒材"), "Resource-pack translation wins after reload");
        require(NerfTranslations.fallback(key, en).equals(en), "Explicit language key prevents the fallback even before caller value changes");
        require(NerfWikiNames.display("Mass Fabricator").equals("Mass Fabricator"), "Another translator's intentional English name is retained");
        resources.files.put("othermod:lang/zh_CN.lang", key + "=" + en + "\n");
        NerfTranslations.reloaded(resources, zh);
        require(NerfTranslations.fallback(key, en).equals(en), "An explicit translation keeping English also wins");
        resources.files.remove("othermod:lang/zh_CN.lang");
        NerfTranslations.reloaded(resources, zh);
        require(NerfTranslations.fallback(key, en).equals("魔法磁化魔力钢棒"), "Removing the translator restores fallback");
        String originalEnglish = resources.files.get("gtnhextranerf:lang/en_US.lang");
        resources.files.put("gtnhextranerf:lang/en_US.lang", originalEnglish.replace(key + "=" + en, key + "=Changed meaning in a future release"));
        NerfTranslations.reloaded(resources, zh);
        require(NerfTranslations.fallback(key, "Changed meaning in a future release").equals("Changed meaning in a future release"), "Changed future English meaning does not receive stale fallback");
        resources.files.remove("gtnhextranerf:lang/en_US.lang");NerfTranslations.reloaded(resources,zh);
        require(NerfTranslations.fallback(key,key).equals(key), "Absent owner text does not receive an invented fallback");
        resources.files.put("gtnhextranerf:lang/en_US.lang",originalEnglish);
        NerfTranslations.reloaded(resources, Collections.singletonList("en_US"));
        require(NerfTranslations.fallback(key, en).equals(en), "English language disables Chinese fallback");
        NerfTranslations.reloaded(resources, zh);
    }
    public static int run(Map<String,ItemStack> items, Map<String,Fluid> fluids) throws Exception {
        translationChecks();
        Item mar = item("gtnhextranerf:magicnetized_manasteel_rod");
        Item vrod = item("gtnhextranerf:magicnetized_void_metal_rod");
        Item flower = item("Botania:specialFlower"), salis = item("Thaumcraft:ItemResource");
        Item mana = ore("stickManasteel"), magnet = ore("stickSteelMagnetic"), steel = ore("stickSteel"), foil = ore("foilSteelMagnetic"), voidRod = ore("stickVoid");
        Item mv = item("fixture111:mv_motor"), hv = item("fixture111:hv_motor");
        items.put("Electric_Motor_MV", stack(mv, 1)); items.put("Electric_Motor_HV", stack(hv, 1));
        Fluid mercury = new Fluid("fixture111.mercury"); FluidRegistry.registerFluid(mercury); fluids.put("Mercury:fluid", mercury);
        ItemStack gas = ItemFluidDisplay.createStack(mercury, 144L), catalyst = flower(flower, 0, "solegnolia");
        ItemStack salt = new ItemStack(salis, 2, 14);
        IRecipeHandler auto = handler("gt.recipe.assembler", positions(stack(mana,1),stack(magnet,2),salt,catalyst,gas), positions(stack(mar,1),stack(steel,2)));
        IRecipeHandler arcane = handler("thaumcraft.arcane.shaped", positions(stack(mana,1),stack(magnet,2),flower(flower,1,"solegnolia")), positions(stack(mar,1)));
        IRecipeHandler wrongFlower = handler("gt.recipe.assembler", positions(stack(mana,1),stack(magnet,2),salt,flower(flower,0,"hopperhock"),gas), positions(stack(mar,1)));
        require(has(auto,"magicnetized-manasteel-assembler"), "HV automation matches native quantities and reusable flower");
        require(has(arcane,"magicnetized-manasteel-arcane"), "Arcane recipe matches its one-item returned flower");
        require(!has(wrongFlower,"magicnetized-manasteel-assembler"), "Another special flower with the same item ID does not match");
        require(badge(auto, catalyst, true, 0xFFFFDE59), "New reusable catalyst receives a yellow recipe frame");
        require(NerfVisuals.cards(NerfRecipes.match(auto,0).get(0),auto,0).stream().anyMatch(c -> c.note.equals(NerfText.tr("tool.note")) && c.after.equals(NerfText.tr("tool.present"))), "Zero-sized catalyst card states that it is not consumed");
        IRecipeHandler voidRecipe = handler("gt.recipe.assembler", positions(stack(voidRod,1),stack(mar,2),stack(foil,4),salt,catalyst,gas), positions(stack(vrod,1),stack(mana,2)));
        require(has(voidRecipe,"magicnetized-void-assembler"), "Void rod matches all new native inputs");
        IRecipeHandler mvRecipe = handler("gt.recipe.assembler", positions(stack(mar,1)), positions(stack(mv,1)));
        IRecipeHandler hvRecipe = handler("crafting", positions(stack(vrod,1)), positions(stack(hv,1)));
        require(has(mvRecipe,"mv-motor-magicnetized") && has(hvRecipe,"hv-motor-magicnetized"), "Both motor recipes recognize the exact new rods");
        require(!has(handler("gt.recipe.componentassemblyline",positions(stack(magnet,1)),positions(stack(mv,1))),"mv-motor-magicnetized"), "Unchanged Component Assembly Line is outside the motor record");
        require(NerfItems.affected(stack(mv,1)) && NerfItems.affected(stack(hv,1)), "Changed motor products retain global gold frames");
        require(NerfItems.affected(stack(mar,1)) && NerfItems.affected(stack(vrod,1)), "New rods retain global gold frames");
        require(!NerfItems.affected(stack(steel,1)) && !NerfItems.affected(stack(mana,1)), "Returned ordinary rods do not gain global frames");
        NerfDB.StackSpec exactFlower = NerfDB.recipes().stream().filter(r -> r.id.equals("magicnetized-manasteel-assembler")).findFirst().get().inputs.stream().filter(s -> s.item != null && s.item.equals("Botania:specialFlower")).findFirst().get();
        ItemStack display = NerfRecipes.displayStack(exactFlower);
        require(display.hasTagCompound() && display.getTagCompound().getString("type").equals("solegnolia"), "Display copy recreates exact flower NBT");
        require(!NerfRecipes.matchesItem(exactFlower, stack(flower,1)), "Missing NBT does not match the catalyst");

        Item mica = item("fixture111:mica"); items.put("nh:MicaInsulatorFoil",stack(mica,1));
        Item kanthalWire = ore("wireGt08Kanthal"), kanthal = item("fixture111:kanthal_coil"); items.put("Casing_Coil_Kanthal",stack(kanthal,1));
        IRecipeHandler coil = handler("gt.recipe.assembler", positions(stack(kanthalWire,8),stack(mica,24)), positions(stack(kanthal,1)));
        require(has(coil,"heating-coil-kanthal"), "Kanthal wire and doubled insulation match");
        require(!has(handler("gt.recipe.assembler",positions(stack(kanthalWire,8),stack(mica,12)),positions(stack(kanthal,1))),"heating-coil-kanthal"), "Old insulation quantity does not match new coil record");
        require(NerfItems.affected(stack(kanthal,1)), "Changed coil remains globally framed");

        Item rock = ore("blockLivingrock"), plate = ore("plateLivingrock");
        Fluid lubricant = new Fluid("fixture111.lubricant"); FluidRegistry.registerFluid(lubricant); fluids.put("Lubricant:fluid",lubricant);
        IRecipeHandler hammer = handler("gt.recipe.hammer",positions(stack(rock,1),ItemFluidDisplay.createStack(lubricant,22L)),positions(stack(plate,1)));
        require(has(hammer,"livingrock-plate-hammer"), "New hammer route matches exact 22 L coolant");
        require(!NerfItems.affected(stack(plate,1)), "Additional plate route does not mark unchanged original production globally");

        Item alu = ore("dustAluminium"), tc = item("TConstruct:materials"); OreDictionary.registerOre("dustAluminium",new ItemStack(tc,1,12));
        Item circuit = (Item) Item.itemRegistry.getObject("gregtech:gt.integrated_circuit");
        Fluid moltenAlu = new Fluid("fixture111.aluminium"); FluidRegistry.registerFluid(moltenAlu); fluids.put("Aluminium:molten",moltenAlu);
        IRecipeHandler ordinaryAlu = handler("gt.recipe.alloyblastsmelter",positions(stack(alu,1),new ItemStack(circuit,1,1)),positions(ItemFluidDisplay.createStack(moltenAlu,144L)));
        IRecipeHandler tinkersAlu = handler("gt.recipe.alloyblastsmelter",positions(new ItemStack(tc,1,12),new ItemStack(circuit,1,1)),positions(ItemFluidDisplay.createStack(moltenAlu,144L)));
        require(has(ordinaryAlu,"aluminium-no-gas-alloy-smelter") && !has(ordinaryAlu,"tconstruct-aluminium-no-gas-alloy-smelter"), "Ordinary Aluminium route excludes TConstruct metadata");
        require(has(tinkersAlu,"tconstruct-aluminium-no-gas-alloy-smelter") && !has(tinkersAlu,"aluminium-no-gas-alloy-smelter"), "TConstruct route receives only its own duration record");

        Item nugget = ore("nuggetAluminium"), tiny = ore("dustTinyAluminium"), sulfurNugget = ore("nuggetSulfur"), sulfurTiny = ore("dustTinySulfur");
        thaumcraft.api.ThaumcraftApi.recipes.add(new thaumcraft.api.crafting.CrucibleRecipe(stack(nugget,1),stack(tiny,2)));
        thaumcraft.api.ThaumcraftApi.recipes.add(new thaumcraft.api.crafting.CrucibleRecipe(stack(sulfurNugget,1),stack(sulfurTiny,2)));
        NerfItems.clearCache();
        IRecipeHandler metal = handler("thaumcraft.alchemy",positions(stack(nugget,1)),positions(stack(tiny,2)));
        IRecipeHandler nonmetal = handler("thaumcraft.alchemy",positions(stack(sulfurNugget,1)),positions(stack(sulfurTiny,2)));
        require(has(metal,"metal-duplication"), "Same GT metal nugget/dust pair matches");
        require(!has(nonmetal,"metal-duplication"), "Non-metal duplication does not match");
        require(!has(handler("thaumcraft.alchemy",positions(stack(nugget,1)),positions(stack(sulfurTiny,2))),"metal-duplication"), "Mismatched material pairs do not match");
        require(NerfItems.recipesFor(stack(tiny,1)).stream().anyMatch(r -> r.id.equals("metal-duplication")), "Final alchemy registry produces an exact tooltip index");
        require(NerfItems.recipesFor(stack(sulfurTiny,1)).stream().noneMatch(r -> r.id.equals("metal-duplication")), "Alchemy index excludes non-metals");
        require(!NerfItems.affected(stack(tiny,1)), "Aspect/output-form change alone does not produce a global frame");
        require(NerfDB.recipes().stream().noneMatch(r -> r.id.equals("sphalerite-electrolyzer")), "Cancelled Gallium chance record is absent");
        require(NerfDB.recipes().stream().filter(r -> r.id.equals("ilmenite-slag")).findFirst().get().outputs.stream().noneMatch(s -> "dustGallium".equals(s.od)), "Ilmenite no longer depends on a nonexistent Gallium output");

        GregTechAPI.METATILEENTITIES = Arrays.copyOf(GregTechAPI.METATILEENTITIES,32);
        GregTechAPI.METATILEENTITIES[16] = new gregtech.api.metatileentity.implementations.MTEBasicMachine(true);
        GregTechAPI.METATILEENTITIES[17] = new gregtech.api.metatileentity.implementations.MTEBasicMachine(false);
        GregTechAPI.METATILEENTITIES[18] = new gregtech.common.tileentities.machines.basic.MTEIceCreamMachine();
        Item machine = Item.getItemFromBlock(GregTechAPI.sBlockMachines);
        ItemStack steam = new ItemStack(machine,1,16), electric = new ItemStack(machine,1,17), ice = new ItemStack(machine,1,18);
        require(NerfRecipes.entriesFor(steam).stream().anyMatch(e -> e.id.equals("tooltip-steam-basic")), "Steam category uses native machine type and steam flag");
        require(NerfRecipes.entriesFor(electric).stream().noneMatch(e -> e.id.equals("tooltip-steam-basic")), "Electric machines do not receive steam consumption hints");
        require(NerfRecipes.entriesFor(electric).stream().anyMatch(e -> e.id.equals("tooltip-power-failure")), "Standard basic machine receives power-failure warning");
        require(NerfRecipes.entriesFor(ice).stream().noneMatch(e -> e.id.equals("tooltip-power-failure")), "Unpowered Ice Cream Machine is excluded");
        require(!NerfItems.affected(steam) && !NerfItems.affected(electric), "Behavior-only machine changes do not create global material frames");
        System.out.println("PASS: ExtraNerf 1.1.1 integration " + assertions + " assertions");
        return assertions;
    }
}
