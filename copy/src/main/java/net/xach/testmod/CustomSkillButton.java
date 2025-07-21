package net.xach.testmod;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;



@OnlyIn(Dist.CLIENT)
public class CustomSkillButton extends AbstractButton {

    private static final ResourceLocation BUTTON_TEXTURE = new ResourceLocation(TestMod.MOD_ID, "textures/gui/custom_button.png");
    private final Runnable onPress;
    private final boolean isLocked;

    public CustomSkillButton(int x, int y, int width, int height, Component title, Runnable onPress, boolean isLocked) {
        super(x, y, 20, 20, title); // Кнопка 20x20
        this.onPress = onPress;
        this.isLocked = isLocked;

    }

    // Added getter for isLocked
    public boolean isLocked() {
        return isLocked;
    }

    @Override
    public void onPress() {
        if (!isLocked) {
            this.onPress.run();
        }
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        try {
            RenderSystem.setShaderTexture(0, BUTTON_TEXTURE);
            int textureY = isLocked ? 40 : (this.isHoveredOrFocused() ? 20 : 0); // Смещения: 0, 20, 40
            guiGraphics.blit(BUTTON_TEXTURE, this.getX(), this.getY(), 0, textureY, this.getWidth(), this.getHeight(), 20, 60); // Текстура 20x60
            guiGraphics.drawCenteredString(Minecraft.getInstance().font, this.getMessage(), this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, isLocked ? 0x888888 : 0xFFFFFF);

        } catch (Exception e) {
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
        this.defaultButtonNarrationText(narration);
    }
}