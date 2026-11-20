import java.io.File;
import java.lang.reflect.*;
import java.util.*;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.*;
import nerflens.*;

/** Cross-profile checks for the analogous route-only changes found during the complete catalog audit. */
public final class CatalogScopeChecks {
    private static int assertions, nextId=29000;
    private static void require(boolean result,String message) {
        assertions++; if (!result) throw new AssertionError(message);
    }
    private static Item item(String name) throws Exception {
        Item value=new Item().setUnlocalizedName("catalog_scope");
        Method raw=null;
        for (Method method:Item.itemRegistry.getClass().getDeclaredMethods()) if (method.getName().equals("addObjectRaw")) raw=method;
        raw.setAccessible(true);raw.invoke(Item.itemRegistry,nextId++,name,value);
        Method setName=value.delegate.getClass().getDeclaredMethod("setName",String.class);
        setName.setAccessible(true);setName.invoke(value.delegate,name);return value;
    }
    private static IRecipeHandler handler(List<PositionedStack> inputs,PositionedStack output) {
        return (IRecipeHandler)Proxy.newProxyInstance(IRecipeHandler.class.getClassLoader(),new Class<?>[]{IRecipeHandler.class},(proxy,method,args)->{
            switch(method.getName()) {
                case "getHandlerId":case "getRecipeName":return "gt.recipe.circuitassembler";
                case "getIngredientStacks":return inputs;
                case "getResultStack":return output;
                case "getOtherStacks":return Collections.emptyList();
                case "hashCode":return System.identityHashCode(proxy);
                case "equals":return proxy==args[0];
                default:return null;
            }
        });
    }
    public static int run(File config,Map<String,ItemStack> items) throws Exception {
        Map<String,ItemStack> original=new HashMap<>(items);
        String[] localIds={"mutated-solder","pcb-wetware","pcb-bioware","purified-water-naquadah","purified-water-enriched-naquadah",
            "purified-water-naquadria","sticky-piston","coated-board-1","coated-board-3","ilmenite-ebf","indium-concentrate",
            "lubricant-lightoil","lubricant-oil","lubricant-heavyoil","europium-base","europium-promethium","americium-base",
            "americium-promethium","nanoprocessor","quantumprocessor","iron-steel-transmutation",
            "iron-bath-stainless-nickel","iron-bath-stainless-invar","iron-bath-electrical-steel","iron-bath-dark-steel-small",
            "iron-bath-dark-steel-large","iron-bath-electrical-steel-crafting","iron-bath-dark-steel-crafting"};
        Item nano=item("catalog:nanoprocessor"),quantum=item("catalog:quantumprocessor"),cpu=item("catalog:nanocpu"),soc=item("catalog:soc");
        int audited=0;
        for(String version:new String[]{"1.0.1","1.1.0","1.1.1","1.2.0"}) {
            NerfDB.load(config,version);
            items.put("Circuit_Nanoprocessor",new ItemStack(nano));items.put("Circuit_Quantumprocessor",new ItemStack(quantum));
            items.put("Circuit_Chip_NanoCPU",new ItemStack(cpu));items.put("Circuit_Chip_SoC",new ItemStack(soc));
            for(NerfDB.RecipeEntry recipe:NerfDB.recipes()) {
                audited++;
                if(Arrays.asList(localIds).contains(recipe.id)) require(!NerfItems.materialInputsChanged(recipe),"Route-only material frame disabled: "+version+" / "+recipe.id);
                if(recipe.removed||recipe.noticeOnly) require(!NerfItems.materialInputsChanged(recipe),"Removed/notice route cannot create global frame: "+recipe.id);
            }
            require(!NerfItems.affected(new ItemStack(nano)),"Handwritten Nano entry cannot restore the rejected global frame: "+version);
            require(!NerfItems.affected(new ItemStack(quantum)),"Handwritten Quantum entry cannot restore the rejected global frame: "+version);
            List<PositionedStack> inputs=new ArrayList<>(Collections.singletonList(new PositionedStack(new ItemStack(cpu,6,0),0,0,false)));
            PositionedStack output=new PositionedStack(new ItemStack(nano),0,0,false);
            IRecipeHandler h=handler(inputs,output);
            require(NerfRecipes.match(h,0).stream().anyMatch(r->r.id.equals("nanoprocessor")),"Changed standard Nano route still matches: "+version);
            require(!NerfBadges.forRecipe(h,0,output,false).isEmpty(),"Changed standard route retains a local output frame: "+version);
            inputs.add(new PositionedStack(new ItemStack(soc),0,0,false));
            require(NerfRecipes.match(h,0).stream().noneMatch(r->r.id.equals("nanoprocessor")),"SoC route is excluded after handler contents change: "+version);
            require(NerfBadges.forRecipe(h,0,output,false).isEmpty(),"Unchanged SoC recipe does not inherit global/old local frames: "+version);
        }
        require(audited==582,"All 582 version-scoped recipe records were inspected");
        Item tagged=item("appliedenergistics2:catalog_tagged"),tpv=item("catalog:tpv"),other=item("catalog:other");
        OreDictionary.registerOre("dustCatalogTPVAlloy",new ItemStack(tpv));
        ItemStack changed=new ItemStack(tagged),unchanged=new ItemStack(tagged);
        NBTTagCompound changedTag=new NBTTagCompound(),unchangedTag=new NBTTagCompound();
        changedTag.setString("Variant","changed");unchangedTag.setString("Variant","unchanged");
        changed.setTagCompound(changedTag);unchanged.setTagCompound(unchangedTag);
        CraftingManager.getInstance().getRecipeList().add(new ShapelessOreRecipe(changed,new ItemStack(tpv)));
        CraftingManager.getInstance().getRecipeList().add(new ShapelessOreRecipe(unchanged,new ItemStack(other)));
        NerfItems.clearCache();
        require(NerfItems.affected(changed),"Actual tagged TPV product is discovered");
        require(!NerfItems.affected(unchanged),"Same item/meta with another NBT variant does not inherit the discovered global frame");
        require(NerfItems.recipesFor(unchanged).stream().noneMatch(r->r.id.equals("ae2-tpv-materials")),"Different NBT product does not inherit production provenance");
        require(changed.getTagCompound().equals(changedTag),"Indexing preserves the native output NBT");
        NerfDB.load(config,"1.1.1");items.putAll(original);
        System.out.println("PASS: complete catalog scope audit "+audited+" records; "+assertions+" assertions");
        return assertions;
    }
}
