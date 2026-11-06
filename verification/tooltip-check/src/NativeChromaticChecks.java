import java.lang.reflect.*;
import java.util.*;
import com.slprime.chromatictooltips.TooltipRegistry;
import com.slprime.chromatictooltips.api.*;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.*;
import nerflens.*;

/** Uses the actual installed Chromatic API, with a context that does not create an OpenGL renderer. */
public final class NativeChromaticChecks {
    private static int assertions;
    public static final class TestContext extends TooltipContext {
        ItemStack item;
        FluidStack fluid;
        TooltipModifier modifier;
        public TestContext() { super((TooltipRequest) null, (ITooltipRenderer) null); }
        @Override public ItemStack getItem() { return item; }
        @Override public FluidStack getFluid() { return fluid; }
        @Override public TooltipModifier getActiveModifier() { return modifier; }
    }
    private static void require(boolean result, String message) {
        assertions++;
        if (!result) throw new AssertionError(message);
    }
    private static String text(TooltipLines lines) throws Exception {
        if (lines == null) return "";
        Field field = TooltipLines.class.getDeclaredField("textLines"); field.setAccessible(true);
        return field.get(lines).toString();
    }
    public static int run(sun.misc.Unsafe unsafe, Fluid benzene, Fluid water, Fluid medium, ItemStack item) throws Exception {
        NerfChromaticTooltips.register();
        ITooltipEnricher bridge = TooltipRegistry.getEnrichers().stream()
            .filter(e -> e.sectionId().equals("nerflensChanges")).findFirst().orElse(null);
        require(bridge != null, "Fluid bridge registers with the installed Chromatic API");
        NerfChromaticTooltips.register();
        require(TooltipRegistry.getEnrichers().stream().filter(e -> e.sectionId().equals("nerflensChanges")).count() == 1,
                "Optional bridge registration is idempotent");
        require(bridge.place() == EnricherPlace.BODY && bridge.mode().contains(TooltipModifier.SHIFT),
                "Native section appears in the body and is available with Shift");
        TestContext context = (TestContext) unsafe.allocateInstance(TestContext.class);
        context.fluid = new FluidStack(benzene, 1000);
        for (boolean zh : new boolean[]{true, false}) {
            NerfText.useChinese = zh;
            context.modifier = TooltipModifier.NONE;
            require(text(bridge.build(context)).contains(zh ? "1/4" : "one quarter"),
                    "Pure fluid context without ItemTooltipEvent receives the fuel summary");
            context.modifier = TooltipModifier.SHIFT;
            TooltipLines full = bridge.build(context);
            require(text(full).contains("360→90"), "Native Shift context receives the detailed fuel change");
            Field modifiers = TooltipLines.class.getDeclaredField("supportedModifiers"); modifiers.setAccessible(true);
            require(((Set<?>) modifiers.get(full)).contains(TooltipModifier.SHIFT), "Native renderer recognizes supported Shift details");
            context.modifier = TooltipModifier.CTRL;
            require(!text(bridge.build(context)).isEmpty(), "Other modifier modes retain a useful default summary");
        }
        context.fluid = new FluidStack(water, 1000);
        require(bridge.build(context) == null, "Other fluid context cannot inherit Benzene's description");
        context.fluid = new FluidStack(medium, 1);
        require(!text(bridge.build(context)).isEmpty(), "Native pure fluid context also receives recipe-only product explanations");
        context.item = item;
        require(bridge.build(context) == null, "Item targets do not duplicate descriptions already supplied by the Forge event");
        context.item = null;
        context.fluid = null;
        require(bridge.build(context) == null, "Text-only contexts receive no fluid section");
        System.out.println("PASS: installed Chromatic API " + assertions + " assertions");
        return assertions;
    }

    public static int checkHelium(Fluid helium) throws Exception {
        Field access = sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); access.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) access.get(null);
        ITooltipEnricher bridge = TooltipRegistry.getEnrichers().stream()
            .filter(e -> e.sectionId().equals("nerflensChanges")).findFirst().get();
        TestContext context = (TestContext) unsafe.allocateInstance(TestContext.class);
        context.fluid = new FluidStack(helium, 1500); context.modifier = TooltipModifier.SHIFT;
        int checks = 0;
        for (boolean zh : new boolean[]{true, false}) {
            NerfText.useChinese = zh;
            String description = text(bridge.build(context)).replace(",", "");
            if (!description.contains("4320") || !description.contains("1500")) throw new AssertionError("Native Helium description missing its original/current quantities");
            checks++;
        }
        return checks;
    }
}
