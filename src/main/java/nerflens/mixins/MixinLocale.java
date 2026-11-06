package nerflens.mixins;

import java.util.List;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.Locale;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import nerflens.NerfTranslations;

@Mixin(value = Locale.class, priority = 500)
public abstract class MixinLocale {

    @Inject(method = "loadLocaleDataFiles", at = @At("RETURN"))
    private void nerflens$trackTranslations(IResourceManager resources, List<String> languages, CallbackInfo callback) {
        NerfTranslations.reloaded(resources, languages);
    }

    @ModifyReturnValue(method = "translateKeyPrivate", at = @At("RETURN"))
    private String nerflens$fillMissingTranslation(String original, String key) {
        return NerfTranslations.fallback(key, original);
    }
}
