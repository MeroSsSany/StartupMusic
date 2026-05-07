package dev.merosssany.musicaltone.mixin;

import dev.merosssany.musicaltone.StartupMusicalTone;
import net.minecraft.client.gui.screens.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {
    @Unique private static boolean startupmusictone$ticked = false;
    
    @Inject(
            method = "init",
            at = @At(value = "HEAD")
    )
    private static void stopMusic(CallbackInfo ci) {
        if (!startupmusictone$ticked) {
            StartupMusicalTone.stopMusic();
            startupmusictone$ticked = true;
        }
    }
}
