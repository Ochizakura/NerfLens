import java.io.File;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;
import cpw.mods.fml.common.DummyModContainer;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.ModMetadata;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;
import nerflens.*;

public final class VersionMatrixChecks {
    private static int assertions, nextId = 22000;
    private static void require(boolean result, String description) {
        assertions++;
        if (!result) throw new AssertionError(description);
    }
    private static Item item(String id) throws Exception {
        Item previous = (Item) Item.itemRegistry.getObject(id);
        if (previous != null) return previous;
        Item value = new Item().setUnlocalizedName("version_fixture").setHasSubtypes(true);
        Method raw = null;
        for (Method method : Item.itemRegistry.getClass().getDeclaredMethods())
            if (method.getName().equals("addObjectRaw")) raw = method;
        raw.setAccessible(true); raw.invoke(Item.itemRegistry,nextId++,id,value);
        Method name = value.delegate.getClass().getDeclaredMethod("setName",String.class);
        name.setAccessible(true);name.invoke(value.delegate,id);
        return value;
    }
    private static IRecipeHandler handler(String map, ItemStack input, ItemStack output) {
        return (IRecipeHandler) Proxy.newProxyInstance(IRecipeHandler.class.getClassLoader(),new Class<?>[]{IRecipeHandler.class},
            (proxy,method,args) -> {
                switch (method.getName()) {
                    case "getHandlerId":case "getRecipeName":return map;
                    case "getIngredientStacks":return Collections.singletonList(new PositionedStack(input.copy(),0,0,false));
                    case "getResultStack":return new PositionedStack(output.copy(),0,0,false);
                    case "getOtherStacks":return Collections.emptyList();
                    case "hashCode":return System.identityHashCode(proxy);
                    case "equals":return proxy==args[0];
                    default:return null;
                }
            });
    }
    private static boolean matched(IRecipeHandler handler,String id) {
        return NerfRecipes.match(handler,0).stream().anyMatch(r -> r.id.equals(id));
    }
    private static boolean recipe(String id) {return NerfDB.recipes().stream().anyMatch(r -> r.id.equals(id));}
    public static int run(File normalConfig, Map<String,ItemStack> items) throws Exception {
        File directory = new File("version-matrix");directory.mkdirs();
        Path override = directory.toPath().resolve("nerflens/user_nerfs.json");Files.createDirectories(override.getParent());Files.deleteIfExists(override);
        Item sphalerite = item("matrix:sphalerite"), gallium = item("matrix:gallium");
        OreDictionary.registerOre("dustSphalerite",new ItemStack(sphalerite));
        OreDictionary.registerOre("dustGallium",new ItemStack(gallium));
        IRecipeHandler galliumRecipe = handler("gt.recipe.electrolyzer",new ItemStack(sphalerite,2,0),new ItemStack(gallium));
        Item raw = item("gtnhextranerf:raw_machine_casing");
        ItemStack mv = items.get("Electric_Motor_MV");
        for (String version : new String[]{"1.0.1","1.1.0","1.1.1"}) {
            NerfDB.load(directory,version);
            require(NerfDB.profileVersion().equals(version),"Correct loaded version " + version);
            require(matched(galliumRecipe,"sphalerite-electrolyzer") == version.equals("1.0.1"),"Same handler cache follows Gallium version changes");
            require(recipe("metal-duplication") == version.equals("1.1.1"),"Metal transmutation is 1.1.1 only");
            require(recipe("livingrock-plate-hammer") == version.equals("1.1.1"),"Hammer route is 1.1.1 only");
            require(NerfItems.affected(mv) == !version.equals("1.0.1"),"Motor global frame follows version");
            require(NerfItems.affected(new ItemStack(raw,1,9)) == !version.equals("1.0.1"),"UEV raw casing material frame follows version");
            require(NerfItems.affected(new ItemStack(raw,1,8)),"UHV raw casing retains changed material frame");
            require(!NerfItems.affected(new ItemStack(raw,1,13)),"MAX placeholder has no invented material frame");
        }
        NerfDB.load(directory,"1.2.0");
        require(NerfDB.entries().isEmpty() && NerfDB.recipes().isEmpty(),"Unknown future version does not claim known rules");
        require(NerfRecipes.match(galliumRecipe,0).isEmpty(),"Unknown-version cache drops old matches");
        require(!NerfItems.affected(mv),"Unknown version does not retain a cached global frame");

        Loader loader = Loader.instance();Field index = Loader.class.getDeclaredField("namedMods");index.setAccessible(true);
        Object original = index.get(loader);
        try {
            for (String version : new String[]{"1.0.1","1.1.0","1.1.1"}) {
                ModMetadata metadata = new ModMetadata();metadata.modId="gtnhextranerf";metadata.version=version;
                Map<String,ModContainer> mods = new HashMap<>();mods.put("gtnhextranerf",new DummyModContainer(metadata));index.set(loader,mods);
                NerfDB.load(directory);
                require(NerfDB.profileVersion().equals(version),"Production loader reads actual FML metadata " + version);
            }
            index.set(loader,new HashMap<String,ModContainer>());NerfDB.load(directory);
            require(NerfDB.entries().isEmpty() && NerfDB.recipes().isEmpty(),"Production loader handles absent ExtraNerf");
        } finally {index.set(loader,original);}

        String custom="{\"extraNerfVersion\":\"1.1.1\",\"entries\":[{\"id\":\"matrix-custom\",\"summary\":[\"fixture\"]}]}";
        Files.write(override,custom.getBytes(StandardCharsets.UTF_8));NerfDB.load(directory,"1.0.1");
        require(NerfDB.entries().stream().noneMatch(e -> e.id.equals("matrix-custom")),"Versioned override does not leak into old version");
        NerfDB.load(directory,"1.1.1");
        require(NerfDB.entries().stream().anyMatch(e -> e.id.equals("matrix-custom")),"Versioned override applies to matching version");
        Files.write(override,custom.replace("\"extraNerfVersion\":\"1.1.1\",","").getBytes(StandardCharsets.UTF_8));NerfDB.load(directory,"1.2.0");
        require(NerfDB.entries().stream().anyMatch(e -> e.id.equals("matrix-custom")),"Unversioned custom data remains available in unknown version");
        Files.delete(override);NerfDB.load(normalConfig,"1.1.1");
        System.out.println("PASS: version matrix " + assertions + " assertions");return assertions;
    }
}
