import java.util.*;
import java.lang.reflect.Method;
import java.lang.reflect.Field;

import codechicken.nei.PositionedStack;
import codechicken.nei.item.ItemFluidDisplay;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.Badge;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.Locale;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import nerflens.NerfDB;
import nerflens.NerfRecipes;
import nerflens.NerfVisuals;

public final class VisualBoundaryChecks {
    private static int checks;
    private static int failures;

    private static void require(boolean pass, String description) {
        checks++;
        if (!pass) { failures++; System.out.println("FAIL: " + description); }
    }

    private static IRecipeHandler handler(final List<PositionedStack> inputs, final PositionedStack result) {
        return new IRecipeHandler() {
            public String getHandlerId() { return "boundary.input"; }
            public String getRecipeName() { return "Boundary"; }
            public int numRecipes() { return 1; }
            public void drawBackground(int index) {}
            public void drawForeground(int index) {}
            public List<PositionedStack> getIngredientStacks(int index) { return inputs; }
            public List<PositionedStack> getOtherStacks(int index) { return Collections.emptyList(); }
            public PositionedStack getResultStack(int index) { return result; }
            public void onUpdate() {}
            public boolean hasOverlay(net.minecraft.client.gui.inventory.GuiContainer gui, net.minecraft.inventory.Container container, int index) { return false; }
            public codechicken.nei.api.IRecipeOverlayRenderer getOverlayRenderer(net.minecraft.client.gui.inventory.GuiContainer gui, int index) { return null; }
            public codechicken.nei.api.IOverlayHandler getOverlayHandler(net.minecraft.client.gui.inventory.GuiContainer gui, int index) { return null; }
            public List<String> handleTooltip(codechicken.nei.recipe.GuiRecipe<?> gui, List<String> tip, int index) { return tip; }
            public List<String> handleItemTooltip(codechicken.nei.recipe.GuiRecipe<?> gui, ItemStack stack, List<String> tip, int index) { return tip; }
            public boolean keyTyped(codechicken.nei.recipe.GuiRecipe<?> gui, char key, int code, int index) { return false; }
            public boolean mouseClicked(codechicken.nei.recipe.GuiRecipe<?> gui, int button, int index) { return false; }
        };
    }

    public static int run() throws Exception {
        checks = 0;
        failures = 0;
        Bootstrap.func_151354_b();
        for (Field field : I18n.class.getDeclaredFields()) if (field.getType() == Locale.class) {
            field.setAccessible(true);
            if (field.get(null) == null) field.set(null, new Locale());
        }
        if (Item.itemRegistry.getObject("nei:fluid_display") == null) {
            Item fluidItem = new ItemFluidDisplay();
            Method raw = null;
            for (Method method : Item.itemRegistry.getClass().getDeclaredMethods())
                if (method.getName().equals("addObjectRaw")) raw = method;
            raw.setAccessible(true);
            raw.invoke(Item.itemRegistry, 18000, "nei:fluid_display", fluidItem);
            Method setName = fluidItem.delegate.getClass().getDeclaredMethod("setName", String.class);
            setName.setAccessible(true); setName.invoke(fluidItem.delegate, "nei:fluid_display");
        }

        NerfDB.RecipeEntry recipe = new NerfDB.RecipeEntry();
        recipe.id = "boundary.all-inputs";
        recipe.allInputsChanged = true;
        recipe.maps = Collections.singletonList("boundary.input");
        NerfDB.StackSpec output = new NerfDB.StackSpec();
        output.item = "minecraft:stone";
        recipe.outputs = Collections.singletonList(output);
        PositionedStack result = new PositionedStack(new ItemStack(Blocks.stone, 1), 0, 0, false);

        List<List<PositionedStack>> scenarios = Arrays.asList(
            Collections.<PositionedStack>emptyList(),
            Collections.singletonList(new PositionedStack(ItemFluidDisplay.createStack(new FluidStack(FluidRegistry.WATER, 1000)), 0, 0, false)),
            Collections.singletonList(new PositionedStack(new ItemStack(Blocks.stone, 0), 0, 0, false)),
            Collections.<PositionedStack>singletonList(null));
        String[] labels = {"empty input list", "fluid-only inputs", "zero-count tool", "null input slot"};
        for (int i = 0; i < scenarios.size(); i++) {
            List<PositionedStack> inputs = scenarios.get(i);
            require(NerfRecipes.matchesRecipe(recipe, "boundary.input", inputs, Collections.singletonList(result)), labels[i] + " remains a valid scoped display record");
            try {
                List<NerfVisuals.Card> cards = NerfVisuals.cards(recipe, handler(inputs, result), 0);
                require(!cards.isEmpty(), labels[i] + " has a safe comparison card for rendering");
            } catch (RuntimeException error) {
                require(false, labels[i] + " must not throw: " + error.getClass().getSimpleName());
            }
        }
        ItemStack actual = new ItemStack(Blocks.stone, 12);
        PositionedStack input = new PositionedStack(actual, 0, 0, false);
        List<NerfVisuals.Card> normal = NerfVisuals.cards(recipe, handler(Collections.singletonList(input), result), 0);
        require(normal.size() == 1 && "2".equals(normal.get(0).before) && "12".equals(normal.get(0).after), "normal six-times item quantities remain correct");
        require(input.item.stackSize == 12 && normal.get(0).current != input.item, "comparison generation leaves the native ingredient unchanged");
        System.out.println("Visual boundary checks: " + checks + " checked; " + failures + " failures");
        if (failures > 0) throw new AssertionError("Unsafe comparison card generation");
        return checks;
    }

    public static void main(String[] args) throws Exception {
        run();
    }
}
