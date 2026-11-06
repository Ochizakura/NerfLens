package nerflens;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import codechicken.nei.item.ItemFluidDisplay;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Optional native tooltip API bridge: Chromatic's pure fluid targets never fire ItemTooltipEvent. */
@SideOnly(Side.CLIENT)
public final class NerfChromaticTooltips {

    private static boolean registered;
    private static boolean warned;

    private NerfChromaticTooltips() {}

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void register() {
        if (registered) return;
        try {
            String api = "com.slprime.chromatictooltips.api.";
            Class<?> registry = Class.forName("com.slprime.chromatictooltips.TooltipRegistry");
            Class<?> enricher = Class.forName(api + "ITooltipEnricher");
            Class<?> context = Class.forName(api + "TooltipContext");
            Class<?> lines = Class.forName(api + "TooltipLines");
            Class modifier = Class.forName(api + "TooltipModifier");
            Object body = Class.forName(api + "EnricherPlace")
                .getField("BODY")
                .get(null);
            EnumSet<?> modes = EnumSet.allOf(modifier);
            Object shift = modifier.getField("SHIFT")
                .get(null);
            Object supportsShift = Array.newInstance(modifier, 1);
            Array.set(supportsShift, 0, shift);
            Method getItem = context.getMethod("getItem");
            Method getFluid = context.getMethod("getFluid");
            Method getModifier = context.getMethod("getActiveModifier");
            Constructor<?> constructLines = lines.getConstructor(List.class);
            Method supports = lines.getMethod("supports", supportsShift.getClass());
            Object bridge = Proxy
                .newProxyInstance(enricher.getClassLoader(), new Class<?>[] { enricher }, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "sectionId":
                            return "nerflensChanges";
                        case "place":
                            return body;
                        case "mode":
                            return modes.clone();
                        case "hashCode":
                            return System.identityHashCode(proxy);
                        case "equals":
                            return proxy == args[0];
                        case "toString":
                            return "NerfLens fluid change descriptions";
                        case "build":
                            try {
                                // Item targets already receive these descriptions from the Forge event.
                                if (getItem.invoke(args[0]) != null) return null;
                                FluidStack fluid = (FluidStack) getFluid.invoke(args[0]);
                                if (fluid == null) return null;
                                ItemStack stack = ItemFluidDisplay.createStack(fluid);
                                List<String> tip = new ArrayList<>();
                                TooltipHandler.appendChanges(stack, tip, shift.equals(getModifier.invoke(args[0])));
                                if (tip.isEmpty()) return null;
                                Object result = constructLines.newInstance(tip);
                                supports.invoke(result, supportsShift);
                                return result;
                            } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
                                warn(failure);
                                return null;
                            }
                        default:
                            return null;
                    }
                });
            registry.getMethod("addEnricher", enricher)
                .invoke(null, bridge);
            registered = true;
            FMLLog.info("[NerfLens] registered native Chromatic fluid change descriptions");
        } catch (ClassNotFoundException absent) {
            // Chromatic Tooltips is optional; normal Forge tooltips continue to work without it.
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            warn(failure);
        }
    }

    private static void warn(Throwable failure) {
        if (!warned) {
            warned = true;
            FMLLog.warning("[NerfLens] optional Chromatic fluid descriptions unavailable: %s", failure);
        }
    }
}
