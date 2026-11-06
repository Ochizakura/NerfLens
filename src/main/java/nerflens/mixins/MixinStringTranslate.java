package nerflens.mixins;

import net.minecraft.util.StringTranslate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import nerflens.NerfTranslations;

/** Item names, examinations and consent windows use StatCollector instead of client I18n. */
@Mixin(value = StringTranslate.class, priority = 500)
public abstract class MixinStringTranslate {

    @Shadow
    private static StringTranslate instance;

    @ModifyReturnValue(method = "tryTranslateKey", at = @At("RETURN"))
    private String nerflens$fillMissingTranslation(String original, String key) {
        return (Object) this == instance ? NerfTranslations.fallback(key, original) : original;
    }
}
