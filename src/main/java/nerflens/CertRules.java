package nerflens;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.FMLLog;

/** Reads the installed ExtraNerf machine classifier without attaching tiles or changing world data. */
public final class CertRules {

    public static final String[] CODES = { "steam", "lv", "mv", "hv", "ev", "iv", "luv", "zpm", "uv", "uhv", "uev",
        "uiv", "umv", "uxv", "max" };
    private static final String[] TIERS = { "LV", "MV", "HV", "EV", "IV", "LuV", "ZPM", "UV", "UHV", "UEV", "UIV",
        "UMV", "UXV", "MAX" };
    private static String lookupVersion;
    private static Lookup lookup;

    public static final class Need {

        public final List<String> required;
        public final boolean certificateItem;

        private Need(List<String> required, boolean certificateItem) {
            this.required = Collections.unmodifiableList(required);
            this.certificateItem = certificateItem;
        }
    }

    private static final class Lookup {

        final Field machines;
        final Field block;
        final Method classifier;
        final int apiaryId;

        Lookup(String version) throws ReflectiveOperationException {
            Class<?> api = Class.forName("gregtech.api.GregTechAPI");
            machines = api.getField("METATILEENTITIES");
            block = api.getField("sBlockMachines");
            Class<?> machine = Class.forName("gregtech.api.metatileentity.MetaTileEntity");
            classifier = "1.0.1".equals(version) ? Class.forName("pl.kuba6000.gtnhextranerf.internal.r.g")
                .getMethod("a", machine)
                : Class.forName("pl.kuba6000.gtnhextranerf.progression.MachineProgression")
                    .getMethod("requiredTier", machine);
            Object apiary = Class.forName("gregtech.api.enums.MetaTileEntityIDs")
                .getField("INDUSTRIAL_APIARY")
                .get(null);
            apiaryId = apiary.getClass()
                .getField("ID")
                .getInt(apiary);
        }
    }

    private CertRules() {}

    private static synchronized Lookup lookup(String version) {
        if (!version.equals(lookupVersion)) {
            lookupVersion = version;
            lookup = null;
            try {
                lookup = new Lookup(version);
            } catch (ReflectiveOperationException | LinkageError failure) {
                FMLLog.warning("[NerfLens] certificate classifier unavailable for %s: %s", version, failure);
            }
        }
        return lookup;
    }

    public static Need forStack(ItemStack stack) {
        String version = NerfDB.profileVersion();
        if (version.isEmpty() || stack == null || stack.getItem() == null) return null;
        String name = Item.itemRegistry.getNameForObject(stack.getItem());
        if (stack.getItemDamage() == 0)
            for (String code : CODES) if (("gtnhextranerf:" + code + "_certificate").equals(name))
                return new Need(new ArrayList<>(Collections.singletonList(code)), true);
        Lookup current = lookup(version);
        if (current == null) return null;
        try {
            Block block = (Block) current.block.get(null);
            if (block == null || stack.getItem() != Item.getItemFromBlock(block)) return null;
            Object[] machines = (Object[]) current.machines.get(null);
            int meta = stack.getItemDamage();
            if (meta < 0 || meta >= machines.length
                || machines[meta] == null
                || !current.classifier.getParameterTypes()[0].isInstance(machines[meta])) return null;
            // Registered prototypes need not have a tile; the original apiary rule identifies the installed block ID.
            if (meta == current.apiaryId) return forCertificate("1.0.1".equals(version) ? "uv" : "hv");
            Object result = current.classifier.invoke(null, machines[meta]);
            return result instanceof Enum<?> ? forCertificate(
                ((Enum<?>) result).name()
                    .toLowerCase(Locale.ROOT))
                : null;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unreadable) {
            return null;
        }
    }

    static Need forCertificate(String code) {
        List<String> required = new ArrayList<>();
        if ("steam".equals(code)) required.add("steam");
        else {
            int highest = -1;
            for (int i = 1; i < CODES.length; i++) if (CODES[i].equals(code)) highest = i;
            if (highest < 0) return null;
            for (int i = 0; i <= highest; i++) required.add(CODES[i]);
        }
        return new Need(required, false);
    }

    public static String labels(List<String> codes) {
        StringBuilder result = new StringBuilder();
        for (String code : codes) {
            if (result.length() > 0) result.append(" + ");
            result.append(shortLabel(code));
        }
        return result.toString();
    }

    public static String shortLabel(String code) {
        if ("steam".equals(code)) return NerfText.tr("cert.steam.short");
        for (int i = 1; i < CODES.length; i++) if (CODES[i].equals(code)) return TIERS[i - 1];
        return code;
    }
}
