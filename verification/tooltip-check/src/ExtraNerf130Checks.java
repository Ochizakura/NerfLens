import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import net.minecraft.item.*;
import net.minecraftforge.fluids.*;
import net.minecraftforge.oredict.OreDictionary;
import codechicken.nei.PositionedStack;
import codechicken.nei.item.ItemFluidDisplay;
import codechicken.nei.recipe.*;
import nerflens.*;

/** Native 1.3.0 selectors: QFT zero-count catalysts, iron-family parts, laser certification and party rewards. */
public final class ExtraNerf130Checks {
    private static int assertions, nextId = 34000;
    private static void require(boolean result, String message) {
        assertions++; if (!result) throw new AssertionError(message);
    }
    private static Item item(String id) throws Exception {
        Item existing = (Item) Item.itemRegistry.getObject(id);
        if (existing != null) return existing;
        Item value = new Item().setUnlocalizedName("extranerf130_fixture").setHasSubtypes(true);
        Method raw = null;
        for (Method method : Item.itemRegistry.getClass().getDeclaredMethods())
            if (method.getName().equals("addObjectRaw")) raw = method;
        raw.setAccessible(true); raw.invoke(Item.itemRegistry, nextId++, id, value);
        Method name = value.delegate.getClass().getDeclaredMethod("setName", String.class);
        name.setAccessible(true); name.invoke(value.delegate, id); return value;
    }
    private static Item ore(String name) throws Exception {
        Item value = item("fixture130:" + name.toLowerCase());
        OreDictionary.registerOre(name, new ItemStack(value)); return value;
    }
    private static PositionedStack p(ItemStack stack) { return new PositionedStack(stack.copy(), 0, 0, false); }
    private static PositionedStack liquid(Fluid fluid, long amount) { return p(ItemFluidDisplay.createStack(fluid, amount)); }
    private static Fluid fluid(String key, Map<String, Fluid> fluids) {
        Fluid value = new Fluid("fixture130." + key.replace(':', '.').toLowerCase());
        FluidRegistry.registerFluid(value); fluids.put(key, value); return value;
    }
    private static IRecipeHandler handler(String map, List<PositionedStack> inputs, List<PositionedStack> outputs) {
        return (IRecipeHandler) Proxy.newProxyInstance(IRecipeHandler.class.getClassLoader(), new Class<?>[]{IRecipeHandler.class}, (proxy, method, args) -> {
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
    private static boolean hasRecipe(String id) {
        return NerfDB.recipes().stream().anyMatch(r -> r.id.equals(id));
    }
    private static boolean color(IRecipeHandler handler, PositionedStack stack, boolean input, int color) {
        return NerfBadges.forRecipe(handler, 0, stack, input).stream().anyMatch(b -> b.getColor() == color);
    }
    private static String description(ItemStack stack) {
        List<String> tip = new ArrayList<>(); TooltipHandler.appendChanges(stack, tip, true); return String.join("\n", tip);
    }
    private static boolean entry(ItemStack stack, String id) {
        return NerfRecipes.entriesFor(stack).stream().anyMatch(e -> e.id.equals(id));
    }
    private static Map<String, String> language(String text) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String line : text.replace("\r", "").split("\n")) if (!line.startsWith("#") && line.contains("=")) {
            int split = line.indexOf('='); values.put(line.substring(0, split), line.substring(split + 1));
        }
        return values;
    }
    private static void translations(File config) throws Exception {
        ExtraNerf111Checks.Resources resources = new ExtraNerf111Checks.Resources();
        String enText = new String(Files.readAllBytes(Paths.get("official/en_US-1.3.0.lang")), StandardCharsets.UTF_8);
        String zhText = new String(Files.readAllBytes(Paths.get("official/zh_CN-1.3.0.lang")), StandardCharsets.UTF_8);
        Map<String, String> english = language(enText);
        resources.files.put("gtnhextranerf:lang/en_US.lang", enText);
        resources.files.put("gtnhextranerf:lang/zh_CN.lang", zhText);
        List<String> chinese = Arrays.asList("en_US", "zh_CN");
        NerfTranslations.reloaded(resources, chinese);
        JsonObject fallbacks;
        try (Reader reader = new InputStreamReader(ExtraNerf130Checks.class.getResourceAsStream("/assets/nerflens/translation_fallbacks_zh_CN.json"), StandardCharsets.UTF_8)) {
            fallbacks = new JsonParser().parse(reader).getAsJsonObject();
        }
        int variants = 0;
        for (Map.Entry<String, JsonElement> row : fallbacks.entrySet())
            if (row.getValue().isJsonObject() && row.getValue().getAsJsonObject().has("variants"))
                variants += row.getValue().getAsJsonObject().getAsJsonObject("variants").entrySet().size();
        require(variants == 11, "All eleven changed meanings keep an independent translation");
        String key = "gtnhextranerf.iron_bath.tap.mode.description.0";
        require(NerfTranslations.fallback(key, english.get(key)).equals("红石信号开启时开始一整批"), "1.3.0 batch wording selects its own Chinese");
        require(NerfTranslations.fallback(key, "Discharge once when the redstone signal turns on").equals("Discharge once when the redstone signal turns on"), "Older English is not guessed from the 1.3.0 reference");
        require(NerfTranslations.fallback("gtnhextranerf.iron_bath.tap.mode.description.1", english.get("gtnhextranerf.iron_bath.tap.mode.description.1")).equals("有红石信号时持续浇铸"), "1.3.0 powered pour wording translates");
        require(NerfTranslations.fallback("gtnhextranerf.iron_bath.tap.mode.description.2", english.get("gtnhextranerf.iron_bath.tap.mode.description.2")).equals("持续按批浇铸"), "1.3.0 continuous batches wording translates");
        require(NerfTranslations.fallback("gtnhextranerf.iron_bath.tap.mode.description.2", "Discharge every tick").equals("Discharge every tick"), "Older continuous wording stays original");
        require(NerfTranslations.fallback(key, "Start one batch when the redstone signal turns on (future)").equals("Start one batch when the redstone signal turns on (future)"), "Unknown English never inherits a verified Chinese meaning");
        resources.files.put("othermod:lang/zh_CN.lang", key + "=资源包指定文字\n");
        NerfTranslations.reloaded(resources, chinese);
        require(NerfTranslations.fallback(key, english.get(key)).equals(english.get(key)), "External supplied Chinese keeps priority");
        resources.files.remove("othermod:lang/zh_CN.lang");
        NerfDB.load(config, "1.3.0");
        NerfTranslations.reloaded(resources, chinese);
        require(NerfTranslations.fallback("Material.overcarburizediron", "Overcarburized Iron").equals("过渗碳铁"), "Generated names recognize verified owner 1.3.0");
        NerfDB.load(config, "1.2.1");
        require(NerfTranslations.fallback("Material.overcarburizediron", "Overcarburized Iron").equals("过渗碳铁"), "Frozen 1.2.1 keeps its verified generated name");
        NerfDB.load(config, "1.4.0");
        require(NerfTranslations.fallback("Material.overcarburizediron", "Overcarburized Iron").equals("Overcarburized Iron"), "Future owner version rejects the generated name");
        NerfDB.load(config, "1.3.0");
        resources.files.put("gtnhextranerf:lang/en_US.lang", enText);
        NerfTranslations.reloaded(resources, Collections.singletonList("en_US"));
        require(NerfTranslations.fallback(key, english.get(key)).equals(english.get(key)), "English UI remains English");
    }
    public static int run(File config, Map<String, ItemStack> items, Map<String, Fluid> fluids) throws Exception {
        Map<String, ItemStack> oldItems = new HashMap<>(items);
        Map<String, Fluid> oldFluids = new HashMap<>(fluids);
        NerfDB.load(config, "1.3.0");
        require(NerfDB.entries().size() == 104 && NerfDB.recipes().size() == 178, "Independent 1.3.0 catalog loads completely");
        require(hasRecipe("qft-kevlar-dust") && hasRecipe("qft-kevlar-combs"), "Both QFT catalyst routes are scoped to 1.3.0");
        NerfDB.load(config, "1.2.1");
        require(NerfDB.entries().size() == 102 && NerfDB.recipes().size() == 177, "Frozen 1.2.1 catalog keeps its released size");
        require(!hasRecipe("qft-kevlar-dust"), "1.2.1 does not inherit the 24-comb Kevlar Dust route");
        NerfDB.load(config, "1.3.0");

        Item carbon = ore("dustCarbon"), osmium = ore("dustOsmium"), comb = item("gregtech:gt.comb");
        Item pulp = ore("dustRadoxPolymer"), kevlarDust = ore("dustKevlar");
        Fluid hydrogen = fluid("Hydrogen:gas", fluids), nitrogen = fluid("Nitrogen:gas", fluids);
        Fluid xenoxene = fluid("Xenoxene:fluid", fluids), radox = fluid("RadoxPolymer:molten", fluids);
        Fluid heavy = fluid("RadoxHeavy:fluid", fluids), kevlar = fluid("Kevlar:molten", fluids);
        Fluid resin = fluid("PolyurethaneResin:fluid", fluids), liquidKevlar = fluid("LiquidCrystalKevlar:fluid", fluids);

        List<PositionedStack> inputs = new ArrayList<>(Arrays.asList(p(new ItemStack(carbon, 64, 0)), p(new ItemStack(osmium, 24, 0)),
            liquid(hydrogen, 16000), liquid(nitrogen, 16000), p(new ItemStack(comb, 64, 161)), p(new ItemStack(pulp, 0, 0))));
        List<PositionedStack> outputs = new ArrayList<>(Arrays.asList(liquid(xenoxene, 16000), liquid(radox, 9216), liquid(heavy, 16000), liquid(kevlar, 9216)));
        IRecipeHandler qft = handler("gt.recipe.quantumforcesmelter", inputs, outputs);
        require(has(qft, "qft-kevlar-combs"), "1.3.0 Radox route matches with its zero-count Radox Polymer Pulp");
        require(color(qft, inputs.get(5), true, 0xFFFFDE59), "Added non-consumed Pulp has a local yellow frame");
        require(color(qft, inputs.get(4), true, 0xFFFFDE59), "Consumed 64 Combs keep their own yellow frame");
        require(!color(qft, inputs.get(0), true, 0xFFFFDE59), "Unchanged Carbon does not inherit added-input frames");
        require(color(qft, outputs.get(1), false, 0xFFFFB82E) && color(qft, outputs.get(3), false, 0xFFFFB82E), "Intended plastics keep local gold frames");
        require(!color(qft, outputs.get(0), false, 0xFFFFB82E) && !color(qft, outputs.get(2), false, 0xFFFFB82E), "Unchanged Xenoxene and Heavy Radox stay unframed");
        inputs.remove(5);
        require(!has(qft, "qft-kevlar-combs"), "1.3.0 rejects the 1.2.1 Radox route without the Pulp");
        NerfDB.load(config, "1.2.1");
        require(has(qft, "qft-kevlar-combs"), "Frozen 1.2.1 still matches the route without the Pulp");
        inputs.add(p(new ItemStack(pulp, 0, 0)));
        require(has(qft, "qft-kevlar-combs"), "A zero-count stack does not break the 1.2.1 route");
        NerfDB.load(config, "1.3.0");
        require(has(qft, "qft-kevlar-combs"), "Switching back to 1.3.0 restores the Pulp rule");

        List<PositionedStack> comb24 = new ArrayList<>(Arrays.asList(p(new ItemStack(comb, 24, 161)), p(new ItemStack(carbon, 64, 0)),
            liquid(nitrogen, 16000), liquid(hydrogen, 16000), p(new ItemStack(kevlarDust, 0, 0))));
        List<PositionedStack> comb24Outputs = new ArrayList<>(Arrays.asList(liquid(resin, 32000), liquid(liquidKevlar, 4608), liquid(kevlar, 9216)));
        IRecipeHandler small = handler("gt.recipe.quantumforcesmelter", comb24, comb24Outputs);
        require(has(small, "qft-kevlar-dust"), "1.3.0 24-comb route matches its zero-count Kevlar Dust");
        require(color(small, comb24.get(4), true, 0xFFFFDE59), "Added non-consumed Kevlar Dust has a local yellow frame");
        require(!color(small, comb24Outputs.get(0), false, 0xFFFFB82E), "Unchanged Polyurethane Resin gains no gold frame");
        comb24.set(0, p(new ItemStack(comb, 64, 161)));
        require(!has(small, "qft-kevlar-dust"), "Wrong comb count cannot inherit the 24-comb route");
        comb24.set(0, p(new ItemStack(comb, 24, 161)));
        comb24.set(4, p(new ItemStack(kevlarDust, 0, 0)));
        comb24Outputs.set(0, liquid(resin, 16000));
        require(!has(small, "qft-kevlar-dust"), "Other Polyurethane quantity cannot inherit the 24-comb route");
        NerfDB.load(config, "1.2.1");
        require(!has(small, "qft-kevlar-dust"), "Frozen 1.2.1 has no 24-comb dust claim");
        NerfDB.load(config, "1.3.0");

        Item plate = ore("plateWroughtIron"), tiny = ore("dustTinyCastIron"), block = ore("blockOvercarburizedIron");
        Item pigGear = ore("gearPigIron"), ironPlate = ore("plateIron");
        require(entry(new ItemStack(plate), "tooltip-iron-bath-products"), "Wrought Iron plates are recognized as bath products");
        require(entry(new ItemStack(tiny), "tooltip-iron-bath-products"), "Cast Iron tiny dusts are recognized as bath products");
        require(entry(new ItemStack(block), "tooltip-iron-bath-products"), "Overcarburized Iron blocks are recognized as bath products");
        require(entry(new ItemStack(pigGear), "tooltip-iron-bath-products"), "Pig Iron gears are recognized as bath products");
        require(!entry(new ItemStack(ironPlate), "tooltip-iron-bath-products"), "Plain Iron parts do not inherit bath product text");
        String parts = description(new ItemStack(plate));
        NerfText.useChinese = true;
        require(parts.contains("锻铁") && parts.contains("板"), "Bath product tooltip names the real material and part");
        require(parts.contains("过渗碳铁"), "Bath product tooltip lists all four metals");
        NerfText.useChinese = false;
        require(description(new ItemStack(plate)).contains("Wrought Iron"), "Bath product tooltip switches to English");

        Item lsc = item("fixture130:lsc").setUnlocalizedName("gt.blockmachines.multimachine.supercapacitor");
        NerfText.useChinese = true;
        require(entry(new ItemStack(lsc), "tooltip-lapotronic-laser-certification"), "Native Supercapacitor name selects its laser certification record");
        require(description(new ItemStack(lsc)).contains("激光仓"), "Laser certification tooltip explains laser hatches");
        require(!NerfItems.affected(new ItemStack(lsc)), "Certification behavior creates no changed-material frame");
        Item book = item("questbook:ItemQuestBook");
        require(entry(new ItemStack(book), "tooltip-quest-reward-party"), "Native Quest Book identity selects the party-reward record");
        require(description(new ItemStack(book)).contains("队伍"), "Party reward tooltip mentions the party");
        NerfDB.load(config, "1.2.1");
        require(!entry(new ItemStack(lsc), "tooltip-lapotronic-laser-certification"), "Frozen 1.2.1 has no laser certification claim");
        require(!entry(new ItemStack(book), "tooltip-quest-reward-party"), "Frozen 1.2.1 has no party reward claim");
        NerfDB.load(config, "1.3.0");
        Item brainrotter = item("fixture130:brainrotter").setUnlocalizedName("gt.blockmachines.multimachine.gtnhextranerf.brainrotter");
        NerfText.useChinese = true;
        require(description(new ItemStack(brainrotter)).contains("并行"), "Brainrotter tooltip records sub-tick parallel");
        require(description(new ItemStack(brainrotter)).contains("1.3.0"), "Brainrotter tooltip marks the 1.3.0 change");
        translations(config);
        items.clear(); items.putAll(oldItems); fluids.clear(); fluids.putAll(oldFluids);
        NerfDB.load(config, "1.2.1"); NerfText.useChinese = true;
        System.out.println("PASS: ExtraNerf 1.3.0 routes and native translations " + assertions + " assertions");
        return assertions;
    }
}
