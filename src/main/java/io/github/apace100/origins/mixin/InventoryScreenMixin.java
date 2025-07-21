package io.github.apace100.origins.mixin;

import io.github.apace100.origins.client.gui.LevelZSkillScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public class InventoryScreenMixin {
    
    @Inject(method = "init", at = @At("TAIL"))
    private void addSkillButton(CallbackInfo ci) {
        InventoryScreen screen = (InventoryScreen) (Object) this;
        int x = (screen.width - 176) / 2;
        int y = (screen.height - 166) / 2;
        
        ((ScreenAccessor) screen).origins$addDrawableChild(
            ButtonWidget.builder(Text.translatable("button.origins.skills"), button -> {
                MinecraftClient.getInstance().setScreen(new LevelZSkillScreen());
            }).dimensions(x + 125, y - 18, 40, 18).build()
        );
    }
}