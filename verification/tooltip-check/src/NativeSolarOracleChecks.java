import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import com.google.gson.*;
import net.minecraft.item.*;
import net.minecraftforge.fluids.*;
import net.minecraftforge.oredict.OreDictionary;
import codechicken.nei.PositionedStack;
import codechicken.nei.item.ItemFluidDisplay;
import codechicken.nei.recipe.*;
import nerflens.*;

/** The fixture identities, quantities and container layouts were captured from the real running GTNH client. */
public final class NativeSolarOracleChecks {
    private static int assertions,nextId=31000;
    private static final Map<String,Item> items=new HashMap<>();
    private static final Set<String> registered=new HashSet<>();
    private static void require(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
    private static Item item(String id) throws Exception {
        if(items.containsKey(id))return items.get(id);
        Item value=(Item)Item.itemRegistry.getObject(id);
        if(value==null){
            value=new Item().setUnlocalizedName("native_solar_oracle").setHasSubtypes(true);
            Method raw=null;
            for(Method m:Item.itemRegistry.getClass().getDeclaredMethods())if(m.getName().equals("addObjectRaw"))raw=m;
            raw.setAccessible(true);raw.invoke(Item.itemRegistry,nextId++,id,value);
            Method name=value.delegate.getClass().getDeclaredMethod("setName",String.class);name.setAccessible(true);name.invoke(value.delegate,id);
        }
        items.put(id,value);return value;
    }
    private static Fluid fluid(String name){
        Fluid value=FluidRegistry.getFluid(name);
        if(value==null){value=new Fluid(name);FluidRegistry.registerFluid(value);}return value;
    }
    public static final class NativeCountStack extends PositionedStack {
        public final int realStackSize;
        NativeCountStack(ItemStack item,int count){super(item,0,0,false);realStackSize=count;for(ItemStack stack:items)stack.stackSize=1;}
    }
    private static PositionedStack position(JsonObject value,boolean normalized,Map<String,ItemStack> gtItems) throws Exception {
        if(value.get("fluidDisplay").getAsBoolean())return new PositionedStack(ItemFluidDisplay.createStack(fluid(value.get("fluid").getAsString()),value.get("mB").getAsLong()),0,0,false);
        String id=value.get("item").getAsString();int meta=value.get("meta").getAsInt(),count=value.get("n").getAsInt();
        ItemStack stack=new ItemStack(item(id),count,meta);
        if(id.equals("bartworks:gt.bwMetaGenerateddust"))gtItems.put("bw-meta:"+meta+":dust",stack.copy());
        for(JsonElement element:value.getAsJsonArray("ores")){
            String ore=element.getAsString();if(registered.add(ore+":"+id+":"+meta))OreDictionary.registerOre(ore,new ItemStack(stack.getItem(),1,meta));
        }
        return normalized?new NativeCountStack(stack,count):new PositionedStack(stack,0,0,false);
    }
    private static final class Handler extends TemplateRecipeHandler {
        private final String map;
        Handler(String map,List<PositionedStack> inputs,List<PositionedStack> outputs){
            this.map=map;arecipes.add(new CachedRecipe(){
                public List<PositionedStack> getIngredients(){return inputs;}
                public PositionedStack getResult(){return null;}
                public List<PositionedStack> getOtherStacks(){return outputs;}
            });
        }
        public String getRecipeName(){return map;}
        public String getGuiTexture(){return "";}
        public String getOverlayIdentifier(){return map;}
    }
    public static int run(File config,Map<String,Fluid> gtFluids,Map<String,ItemStack> gtItems) throws Exception {
        Map<String,Fluid> original=new HashMap<>(gtFluids);
        Map<String,ItemStack> originalItems=new HashMap<>(gtItems);
        JsonObject snapshot;
        try(Reader reader=new InputStreamReader(new FileInputStream("official/native_solar_oracle.json"),"UTF-8")){snapshot=new JsonParser().parse(reader).getAsJsonObject();}
        NerfDB.load(config,"1.2.0");
        gtFluids.put("Hydrogen:gas",fluid("hydrogen"));gtFluids.put("Trichlorosilane:fluid",fluid("trichlorosilane"));
        gtFluids.put("HydrochloricAcid:fluid",fluid("hydrochloricacid_gt5u"));gtFluids.put("SiliconTetrachloride:fluid",fluid("silicontetrachloride"));
        JsonArray rows=snapshot.getAsJsonArray("solarRecipes");require(rows.size()==11,"Captured all eleven native solar-grade silicon routes");
        int changed=0,unchanged=0;
        for(boolean normalized:new boolean[]{false,true})for(JsonElement element:rows){
            JsonObject row=element.getAsJsonObject();List<PositionedStack> inputs=new ArrayList<>(),outputs=new ArrayList<>();
            for(JsonElement input:row.getAsJsonArray("inputs"))inputs.add(position(input.getAsJsonObject(),normalized,gtItems));
            for(JsonElement output:row.getAsJsonArray("outputs"))outputs.add(position(output.getAsJsonObject(),normalized,gtItems));
            Handler handler=new Handler(row.get("map").getAsString(),inputs,outputs);
            String expected=row.get("expectedRecord").getAsString();List<NerfDB.RecipeEntry> matches=NerfRecipes.match(handler,0);
            if(expected.isEmpty()){
                unchanged++;require(matches.isEmpty(),"Unchanged native Silane route does not inherit a silicon reduction");
                for(PositionedStack output:outputs)require(NerfBadges.forRecipe(handler,0,output,false).isEmpty(),"Unchanged native output has no local/global frame");
            }else{
                changed++;require(matches.stream().anyMatch(r->r.id.equals(expected)),"Native IDs/counts match "+expected+" normalized="+normalized);
                require(!NerfBadges.forRecipe(handler,0,outputs.get(0),false).isEmpty(),"Native solar output frame appears");
                if(expected.equals("silicon-hydrogen")){
                    PositionedStack acid=outputs.get(outputs.size()-1);
                    require(!NerfBadges.forRecipe(handler,0,acid,false).isEmpty(),"Native hydrogen/chlorosilane acid slot has local gold frame");
                    List<String> tip=new ArrayList<>();TooltipHandler.appendRecipeChanges(tip,matches,false);
                    String text=String.join("\n",tip);require(text.contains("3,000")&&text.contains("2,000")&&text.contains("33.3%"),"Native route tooltip contains both quantities and reduction");
                    for(PositionedStack output:outputs)if(Arrays.stream(OreDictionary.getOreIDs(output.item)).anyMatch(id->OreDictionary.getOreName(id).equals("cellEmpty")))
                        require(NerfBadges.forRecipe(handler,0,output,false).isEmpty(),"Native returned empty cell is not falsely framed");
                }
            }
        }
        require(changed==14&&unchanged==8,"Seven changed and four unchanged native routes checked in both count modes");
        require(OreDictionary.getOres("dustSiliconSG",false).isEmpty(),"Tests do not fabricate the incorrect ore name");
        require(!OreDictionary.getOres("dustSiliconSolarGrade",false).isEmpty(),"Captured real ore name exists in the scenario registry");
        NerfDB.load(config,"1.1.1");gtFluids.putAll(original);gtItems.putAll(originalItems);
        System.out.println("PASS: live-captured native solar oracle "+assertions+" assertions");return assertions;
    }
}
