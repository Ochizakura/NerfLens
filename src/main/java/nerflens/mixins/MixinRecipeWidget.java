package nerflens.mixins;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import codechicken.lib.vec.Rectangle4i;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.Badge;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.widget.RecipeWidget;
import nerflens.NerfBadges;
import nerflens.NerfDB;
import nerflens.NerfItems;
import nerflens.NerfRecipes;
import nerflens.NerfTooltipLayout;
import nerflens.TooltipHandler;

/**
 * Draw frames independently of NEI's optional native recipe badges.
 */
@Mixin(value = RecipeWidget.class, remap = false)
public abstract class MixinRecipeWidget {

    @Shadow
    @Final
    protected IRecipeHandler handler;
    @Shadow
    protected boolean showAsWidget;

    @Shadow
    protected abstract int getRecipeIndex();

    @Shadow
    public abstract PositionedStack getPositionedStackMouseOver(int mouseX, int mouseY);

    @Shadow
    protected abstract List<PositionedStack> getCyclingStacks();

    @Shadow
    protected abstract List<PositionedStack> getOutputs();

    @Inject(
        method = "draw",
        at = @At(
            value = "INVOKE",
            target = "Lcodechicken/nei/recipe/IRecipeHandler;drawForeground(I)V",
            shift = At.Shift.AFTER),
        require = 1)
    private void nerflens$drawFrame(int mouseX, int mouseY, CallbackInfo ci) {
        try {
            if (showAsWidget || !(Minecraft.getMinecraft().currentScreen instanceof GuiRecipe)) return;
            nerflens$drawFrames(getCyclingStacks(), true);
            nerflens$drawFrames(getOutputs(), false);
        } catch (Throwable ignored) {
            // 显示类功能绝不引发崩溃
        }
    }

    @Unique
    private void nerflens$drawFrames(List<PositionedStack> positions, boolean input) {
        if (positions == null) return;
        for (PositionedStack pStack : positions) {
            if (pStack == null) continue;
            for (Badge frame : NerfBadges.forRecipe(handler, getRecipeIndex(), pStack, input))
                frame.draw(new Rectangle4i(pStack.relx, pStack.rely, pStack.width, pStack.height));
        }
    }

    @Inject(method = "handleItemTooltip", at = @At("RETURN"), cancellable = true, require = 1)
    private void nerflens$frameTooltip(ItemStack stack, int mouseX, int mouseY, List<String> tooltip,
        CallbackInfoReturnable<List<String>> cir) {
        try {
            if (showAsWidget || !(Minecraft.getMinecraft().currentScreen instanceof GuiRecipe)) return;
            PositionedStack position = getPositionedStackMouseOver(mouseX, mouseY);
            if (position == null) return;
            boolean input = false;
            for (PositionedStack candidate : getCyclingStacks()) {
                if (candidate == position || candidate != null && candidate.relx == position.relx
                    && candidate.rely == position.rely
                    && candidate.width == position.width
                    && candidate.height == position.height) {
                    input = true;
                    break;
                }
            }
            List<Badge> frames = NerfBadges.forRecipe(handler, getRecipeIndex(), position, input);
            if (frames.isEmpty()) return;
            List<String> result = cir.getReturnValue() == null ? new ArrayList<>()
                : new ArrayList<>(cir.getReturnValue());
            for (Badge frame : frames) for (String line : frame.getTooltip()) {
                result.addAll(
                    NerfTooltipLayout
                        .wrap(Minecraft.getMinecraft().fontRenderer, EnumChatFormatting.GOLD + line, "", "", 260));
            }
            if (input || NerfItems.recipesFor(stack)
                .isEmpty()) {
                List<NerfDB.RecipeEntry> changes = new ArrayList<>();
                for (NerfDB.RecipeEntry recipe : NerfRecipes.match(handler, getRecipeIndex())) {
                    if (input ? NerfRecipes.changedInput(recipe, position)
                        : NerfRecipes.changedOutput(recipe, position)) changes.add(recipe);
                }
                TooltipHandler.appendRecipeChanges(result, changes, GuiScreen.isShiftKeyDown());
            }
            cir.setReturnValue(result);
        } catch (Throwable ignored) {
            // Display-only information must never interrupt a native recipe tooltip.
        }
    }
}
