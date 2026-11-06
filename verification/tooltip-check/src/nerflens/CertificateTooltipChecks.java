package nerflens;
import java.io.File;
import java.lang.reflect.Method;
import java.util.*;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;
import gregtech.api.GregTechAPI;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public final class CertificateTooltipChecks {
    private static int assertions;
    private static void require(boolean value, String message) { assertions++; if (!value) throw new AssertionError(message); }
    private static String text(CertRules.Need need, CertTracker.Status status, boolean expanded) {
        List<String> tip = new ArrayList<>();TooltipHandler.appendCertificateStatus(tip,need,status,expanded);
        return String.join("\n",tip);
    }
    private static Item certificateItem() throws Exception {
        Item value = new Item().setUnlocalizedName("lv_certificate");
        Method raw = null;
        for (Method m : Item.itemRegistry.getClass().getDeclaredMethods()) if (m.getName().equals("addObjectRaw")) raw=m;
        raw.setAccessible(true);raw.invoke(Item.itemRegistry,26000,"gtnhextranerf:lv_certificate",value);
        Method name = value.delegate.getClass().getDeclaredMethod("setName",String.class);name.setAccessible(true);name.invoke(value.delegate,"gtnhextranerf:lv_certificate");
        return value;
    }
    public static int run(File config) throws Exception {
        Item machines = Item.getItemFromBlock(GregTechAPI.sBlockMachines);
        MTEBasicMachine lv = new MTEBasicMachine(false), hv = new MTEBasicMachine(false), ulv = new MTEBasicMachine(false), apiary = new MTEBasicMachine(false);
        hv.mTier=3;ulv.mTier=0;apiary.mTier=8;
        GregTechAPI.METATILEENTITIES=Arrays.copyOf(GregTechAPI.METATILEENTITIES,32);
        GregTechAPI.METATILEENTITIES[20]=lv;GregTechAPI.METATILEENTITIES[21]=hv;GregTechAPI.METATILEENTITIES[22]=ulv;
        GregTechAPI.METATILEENTITIES[23]=new Object();GregTechAPI.METATILEENTITIES[24]=apiary;
        Item certificate = certificateItem();
        for (String version:new String[]{"1.0.1","1.1.0","1.1.1"}) {
            NerfDB.load(config,version);
            CertRules.Need need=CertRules.forStack(new ItemStack(machines,1,20));
            require(need!=null&&need.required.equals(Arrays.asList("steam","lv")),"Native classifier produces LV chain in "+version);
            require(CertRules.forStack(new ItemStack(machines,1,21)).required.equals(Arrays.asList("steam","lv","mv","hv")),"Higher machine uses cumulative native requirement");
            require(CertRules.forStack(new ItemStack(machines,1,22))==null,"ULV does not invent a certificate gate");
            require(CertRules.forStack(new ItemStack(machines,1,23))==null,"Mixed registry entries do not crash the lookup");
            List<String> ap=CertRules.forStack(new ItemStack(machines,1,24)).required;
            require(ap.get(ap.size()-1).equals(version.equals("1.0.1")?"uv":"hv"),"Apiary uses correct registered-ID exception");
            CertRules.Need own=CertRules.forStack(new ItemStack(certificate));
            require(own.certificateItem&&own.required.equals(Collections.singletonList("lv")),"Certificate item reports its own world ownership bit");
            for (boolean zh:new boolean[]{true,false}) {
                NerfText.useChinese=zh;
                CertTracker.Status missing=CertTracker.evaluate(need.required,Collections.singleton("steam"));
                String locked=text(need,missing,false);
                require(locked.contains("cert.locked")&&locked.contains("LV"),"Missing LV is visible in compact tooltip");
                require(!locked.contains("cert.unknown"),"Known missing state is not unknown");
                String ready=text(need,CertTracker.evaluate(need.required,new HashSet<>(need.required)),false);
                require(ready.contains("cert.ready")&&!ready.contains("cert.locked"),"Unlocked prerequisites render ready");
                String unknown=text(need,CertTracker.evaluate(need.required,null),false);
                require(unknown.contains("cert.unknown")&&!unknown.contains("cert.locked"),"Remote or unloaded state is not falsely locked");
                CertTracker.Status revoked=CertTracker.evaluate(need.required,Collections.singleton("lv"));
                require(revoked.missing.equals(Collections.singletonList("steam")),"Revocation keeps the still-owned LV bit");
                String detailed=text(need,revoked,true);
                require(detailed.contains("cert.unlocked")&&detailed.contains("cert.locked"),"Shift shows independent states for prerequisites");
            }
        }
        NerfDB.load(config,"1.2.0");
        require(CertRules.forStack(new ItemStack(machines,1,20))==null,"Unknown profile does not guess machine gates");
        NerfDB.load(config,"1.1.1");NerfText.useChinese=true;
        System.out.println("PASS: certificate tooltip "+assertions+" assertions");return assertions;
    }
}
