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

/** Native 1.2.1 selectors and language references; tests changed routes against unchanged controls. */
public final class ExtraNerf121Checks {
    private static int assertions, nextId = 33000;
    private static void require(boolean result, String message) {
        assertions++; if (!result) throw new AssertionError(message);
    }
    private static Item item(String id) throws Exception {
        Item existing = (Item) Item.itemRegistry.getObject(id);
        if (existing != null) return existing;
        Item value = new Item().setUnlocalizedName("extranerf121_fixture").setHasSubtypes(true);
        Method raw = null;
        for (Method method : Item.itemRegistry.getClass().getDeclaredMethods())
            if (method.getName().equals("addObjectRaw")) raw = method;
        raw.setAccessible(true); raw.invoke(Item.itemRegistry, nextId++, id, value);
        Method name = value.delegate.getClass().getDeclaredMethod("setName", String.class);
        name.setAccessible(true); name.invoke(value.delegate, id); return value;
    }
    private static Item ore(String name) throws Exception {
        Item value = item("fixture121:" + name.toLowerCase());
        OreDictionary.registerOre(name, new ItemStack(value)); return value;
    }
    private static PositionedStack p(ItemStack stack) { return new PositionedStack(stack.copy(), 0, 0, false); }
    private static PositionedStack liquid(Fluid fluid, long amount) { return p(ItemFluidDisplay.createStack(fluid, amount)); }
    private static Fluid fluid(String key, Map<String, Fluid> fluids) {
        Fluid value = new Fluid("fixture121." + key.replace(':', '.').toLowerCase());
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
    private static boolean color(IRecipeHandler handler, PositionedStack stack, boolean input, int color) {
        return NerfBadges.forRecipe(handler, 0, stack, input).stream().anyMatch(b -> b.getColor() == color);
    }
    private static String description(ItemStack stack) {
        List<String> tip = new ArrayList<>(); TooltipHandler.appendChanges(stack, tip, true); return String.join("\n", tip);
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
        String enText = new String(Files.readAllBytes(Paths.get("official/en_US-1.2.1.lang")), StandardCharsets.UTF_8);
        String zhText = new String(Files.readAllBytes(Paths.get("official/zh_CN-1.2.1.lang")), StandardCharsets.UTF_8);
        Map<String, String> english = language(enText), supplied = language(zhText);
        resources.files.put("gtnhextranerf:lang/en_US.lang", enText);
        resources.files.put("gtnhextranerf:lang/zh_CN.lang", zhText);
        List<String> chinese = Arrays.asList("en_US", "zh_CN");
        NerfTranslations.reloaded(resources, chinese);
        JsonObject fallbacks;
        try (Reader reader = new InputStreamReader(ExtraNerf121Checks.class.getResourceAsStream("/assets/nerflens/translation_fallbacks_zh_CN.json"), StandardCharsets.UTF_8)) {
            fallbacks = new JsonParser().parse(reader).getAsJsonObject();
        }
        int variants = 0, matched = 0;
        for (Map.Entry<String, JsonElement> row : fallbacks.entrySet()) {
            String key = row.getKey(), reference = english.get(key);
            JsonObject value = row.getValue().getAsJsonObject();
            if (supplied.containsKey(key)) {
                require(NerfTranslations.fallback(key, supplied.get(key)).equals(supplied.get(key)), "Actual 1.2.1 supplied Chinese wins: " + key);
                continue;
            }
            if (reference == null) {
                require(NerfTranslations.fallback(key, key).equals(key), "Removed/unresolved native key is not invented: " + key);
                continue;
            }
            String expected = reference;
            if (reference.equals(value.get("english").getAsString())) expected = value.get("chinese").getAsString();
            else if (value.has("variants") && value.getAsJsonObject("variants").has(reference)) {
                expected = value.getAsJsonObject("variants").get(reference).getAsString(); variants++;
            }
            if (!expected.equals(reference)) matched++;
            require(NerfTranslations.fallback(key, reference).equals(expected), "Actual 1.2.1 English selects its verified meaning: " + key);
            require(NerfTranslations.fallback(key, "External dynamic translation").equals("External dynamic translation"), "Dynamic injection wins for 1.2.1: " + key);
        }
        require(variants == 8, "All eight changed 1.2.1 meanings select the new translation");
        require(matched == 95, "All 95 missing 1.2.1 labels translate; six language-independent formulas retain their text");
        String key = "gtnhextranerf.iron_bath.measurement.connection", reference = english.get(key);
        require(NerfTranslations.fallback(key, reference).contains("烟囱"), "1.2.1 measurement placement says chimney");
        resources.files.put("othermod:lang/zh_CN.lang", key + "=资源包指定位置\n");
        NerfTranslations.reloaded(resources, chinese);
        require(NerfTranslations.fallback(key, reference).equals(reference), "External supplied key prevents fallback regardless of caller lag");
        resources.files.remove("othermod:lang/zh_CN.lang");
        resources.files.put("gtnhextranerf:lang/en_US.lang", key + "=Unknown future placement\n");
        NerfTranslations.reloaded(resources, chinese);
        require(NerfTranslations.fallback(key, "Unknown future placement").equals("Unknown future placement"), "Future English does not inherit a verified old/new meaning");
        resources.files.clear(); NerfTranslations.reloaded(resources, chinese);
        require(NerfTranslations.fallback("Material.overcarburizediron", "Overcarburized Iron").equals("过渗碳铁"), "Generated names recognize verified owner 1.2.1");
        NerfDB.load(config, "1.3.0");
        require(NerfTranslations.fallback("Material.overcarburizediron", "Overcarburized Iron").equals("Overcarburized Iron"), "Generated name rejects future owner version");
        NerfDB.load(config, "1.2.1");
        resources.files.put("gtnhextranerf:lang/en_US.lang", enText); NerfTranslations.reloaded(resources, Collections.singletonList("en_US"));
        require(NerfTranslations.fallback(key, reference).equals(reference), "English UI remains English for new variants");
    }
    public static int run(File config, Map<String, ItemStack> items, Map<String, Fluid> fluids) throws Exception {
        Map<String, ItemStack> oldItems = new HashMap<>(items);
        Map<String, Fluid> oldFluids = new HashMap<>(fluids);
        NerfDB.load(config, "1.2.1");
        require(NerfDB.entries().size() == 102 && NerfDB.recipes().size() == 177, "Independent 1.2.1 catalog loads completely");
        Item powder = item("fixture121:mineralmix");
        items.put("ore:DEEP_EARTH_REACTOR_FUEL_DEPOSIT:dust", new ItemStack(powder, 1, 0));
        items.put("ore:DEEP_EARTH_REACTOR_FUEL_DEPOSIT:dustSmall", new ItemStack(powder, 1, 1));
        Fluid medium = fluid("BioMediumRaw:fluid", fluids);
        List<PositionedStack> bioInputs = new ArrayList<>(Collections.singletonList(p(new ItemStack(powder, 1, 1))));
        List<PositionedStack> bioOutputs = new ArrayList<>(Collections.singletonList(liquid(medium, 1000)));
        IRecipeHandler bio = handler("bw.recipe.BacteriaVat", bioInputs, bioOutputs);
        require(has(bio, "bio-medium"), "1.2.1 native Small Pile selector matches");
        require(color(bio, bioInputs.get(0), true, 0xFFFFDE59), "Only added Small Pile input is yellow");
        bioOutputs.set(0, liquid(medium, 8000));
        require(has(bio, "bio-medium"), "Native fluid amplification keeps one Small Pile cost");
        bioInputs.set(0, p(new ItemStack(powder, 1, 0)));
        require(!has(bio, "bio-medium"), "1.2.1 rejects stale full-dust recipe on reused page");
        NerfDB.load(config, "1.2.0");
        require(has(bio, "bio-medium"), "Frozen 1.2.0 full-dust recipe still matches");
        bioInputs.set(0, p(new ItemStack(powder, 1, 1)));
        require(!has(bio, "bio-medium"), "1.2.0 does not inherit Small Pile change");
        NerfDB.load(config, "1.2.1");
        require(has(bio, "bio-medium"), "Profile switch invalidates old match and restores Small Pile");

        Item carbon = ore("dustCarbon"), osmium = ore("dustOsmium"), comb = item("gregtech:gt.comb");
        Fluid hydrogen = fluid("Hydrogen:gas", fluids), nitrogen = fluid("Nitrogen:gas", fluids);
        Fluid xenoxene = fluid("Xenoxene:fluid", fluids), radox = fluid("RadoxPolymer:molten", fluids);
        Fluid heavy = fluid("RadoxHeavy:fluid", fluids), kevlar = fluid("Kevlar:molten", fluids);
        List<PositionedStack> inputs = new ArrayList<>(Arrays.asList(p(new ItemStack(carbon, 64, 0)), p(new ItemStack(osmium, 24, 0)), liquid(hydrogen, 16000), liquid(nitrogen, 16000), p(new ItemStack(comb, 64, 161))));
        List<PositionedStack> outputs = new ArrayList<>(Arrays.asList(liquid(xenoxene, 16000), liquid(radox, 9216), liquid(heavy, 16000), liquid(kevlar, 9216)));
        IRecipeHandler qft = handler("gt.recipe.quantumforcesmelter", inputs, outputs);
        require(has(qft, "qft-kevlar-combs"), "Exact native QFT map, comb metadata, materials and four outputs match");
        require(color(qft, inputs.get(4), true, 0xFFFFDE59), "64 added Kevlar Combs have local yellow frame");
        require(!color(qft, inputs.get(0), true, 0xFFFFDE59), "Unchanged Carbon input does not inherit added-input frame");
        require(color(qft, outputs.get(1), false, 0xFFFFB82E) && color(qft, outputs.get(3), false, 0xFFFFB82E), "Both intended plastics have local gold frames");
        require(!color(qft, outputs.get(0), false, 0xFFFFB82E) && !color(qft, outputs.get(2), false, 0xFFFFB82E), "Unchanged Xenoxene and Heavy Radox do not inherit plastics gold frames");
        require(!NerfItems.affected(outputs.get(1).item) && !NerfItems.affected(outputs.get(3).item), "QFT change does not globally frame other plastics routes");
        require(!NerfItems.affected(new ItemStack(comb, 64, 161)), "Changed usage does not globally frame the comb ingredient");
        NerfText.useChinese = true;
        require(description(new ItemStack(comb, 1, 161)).contains("64") && description(new ItemStack(comb, 1, 161)).contains("7.5%"), "Verified native Kevlar Comb receives readable QFT and specialty details");
        require(!description(new ItemStack(comb, 1, 160)).contains("QFT"), "Neighboring Blizz Comb metadata has no Kevlar text");
        for (boolean chinese : new boolean[]{true, false}) {
            NerfText.useChinese = chinese;
            List<String> tip = new ArrayList<>(); TooltipHandler.appendRecipeChanges(tip, NerfRecipes.match(qft, 0), false);
            require(String.join("\n", tip).contains("64"), "QFT compact comparison explains comb count in both languages");
        }
        inputs.set(4, p(new ItemStack(comb, 63, 161)));
        require(!has(qft, "qft-kevlar-combs"), "Wrong comb count invalidates reused QFT match");
        inputs.set(4, p(new ItemStack(comb, 64, 160)));
        require(!has(qft, "qft-kevlar-combs"), "Neighboring native comb metadata is excluded");
        inputs.set(4, p(new ItemStack(comb, 64, 161)));
        outputs.set(1, liquid(radox, 144));
        require(!has(qft, "qft-kevlar-combs"), "Other Radox output quantity cannot inherit exact plastics route");
        outputs.set(1, liquid(radox, 9216));
        require(has(qft, "qft-kevlar-combs"), "Correct contents restore match without handler recreation");
        NerfDB.load(config, "1.2.0");
        require(!has(qft, "qft-kevlar-combs"), "New QFT rule cannot leak into frozen 1.2.0");
        NerfDB.load(config, "1.2.1");
        require(has(qft, "qft-kevlar-combs"), "Switching back to 1.2.1 restores QFT rule");
        Item wireless = item("fixture121:wirelesscover");
        items.put("Cover_Wireless_Energy_LV", new ItemStack(wireless, 1, 1));
        NerfText.useChinese = true;
        require(NerfRecipes.entriesFor(new ItemStack(wireless, 1, 1)).stream().anyMatch(e -> e.id.equals("tooltip-wireless-cover-certification")), "Native wireless cover ID selects its exact 1.2.1 record");
        require(description(new ItemStack(wireless, 1, 1)).contains("认证等级"), "Expanded wireless cover tooltip explains native hatch certification");
        Item qftController = item("gregtech:qftcontrollerfixture").setUnlocalizedName("gt.blockmachines.quantumforcetransformer.controller.tier.single");
        require(NerfRecipes.entriesFor(new ItemStack(qftController)).stream().anyMatch(e -> e.id.equals("tooltip-qft-kevlar-combs")), "Bytecode-verified native QFT controller name receives its machine explanation");
        require(!NerfItems.affected(new ItemStack(wireless, 1, 1)), "Certification behavior does not create a changed-material global frame");
        translations(config);
        items.clear(); items.putAll(oldItems); fluids.clear(); fluids.putAll(oldFluids);
        NerfDB.load(config, "1.1.1"); NerfText.useChinese = true;
        System.out.println("PASS: ExtraNerf 1.2.1 routes and native translations " + assertions + " assertions");
        return assertions;
    }
}
