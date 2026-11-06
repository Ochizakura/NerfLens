package nerflens.mixins;

import java.util.List;

import net.minecraft.client.gui.inventory.GuiContainer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.scroll.ScrollContainer;
import nerflens.NerfCompareOverlay;

@Mixin(value = GuiRecipe.class, remap = false)
public abstract class MixinGuiRecipe extends GuiContainer {

    @Shadow
    private ScrollContainer container;

    protected MixinGuiRecipe() {
        super(null);
    }

    @Inject(method = "updateContainerSize", at = @At("RETURN"))
    private void nerflens$reserveFooter(CallbackInfo ci) {
        NerfCompareOverlay.reserve((GuiRecipe<?>) (Object) this, container);
    }

    @Inject(method = "drawGuiContainerForegroundLayer(II)V", at = @At("RETURN"), remap = true)
    private void nerflens$drawComparison(int mouseX, int mouseY, CallbackInfo ci) {
        NerfCompareOverlay.draw((GuiRecipe<?>) (Object) this, container, guiLeft, guiTop, xSize, ySize, mouseX, mouseY);
    }

    @Inject(method = "handleTooltip", at = @At("RETURN"), cancellable = true)
    private void nerflens$footerTooltip(GuiContainer gui, int mouseX, int mouseY, List<String> tooltip,
        CallbackInfoReturnable<List<String>> cir) {
        cir.setReturnValue(
            NerfCompareOverlay.tooltip(
                (GuiRecipe<?>) (Object) this,
                container,
                guiLeft,
                guiTop,
                xSize,
                ySize,
                mouseX,
                mouseY,
                cir.getReturnValue()));
    }

    @Inject(method = "mouseClicked(III)V", at = @At("HEAD"), remap = true, cancellable = true)
    private void nerflens$changeVisualPage(int mouseX, int mouseY, int button, CallbackInfo ci) {
        if (NerfCompareOverlay
            .click((GuiRecipe<?>) (Object) this, container, guiLeft, guiTop, xSize, ySize, mouseX, mouseY, button))
            ci.cancel();
    }
}
