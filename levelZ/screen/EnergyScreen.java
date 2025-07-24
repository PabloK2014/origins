package net.levelz.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.levelz.data.EnergyData;

public class EnergyScreen extends Screen {
    private static final ResourceLocation TEXTURE = new ResourceLocation("levelz", "textures/gui/energy_screen.png");
    private static final int TEXTURE_WIDTH = 176;
    private static final int TEXTURE_HEIGHT = 166;
    
    private final EnergyData energyData;
    
    public EnergyScreen(EnergyData energyData) {
        super(Component.translatable("screen.levelz.energy"));
        this.energyData = energyData;
    }
    
    @Override
    protected void init() {
        super.init();
        // Calculate position to center the screen
        int x = (this.width - TEXTURE_WIDTH) / 2;
        int y = (this.height - TEXTURE_HEIGHT) / 2;
    }
    
    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);
        
        // Bind and render the background texture
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        
        int x = (this.width - TEXTURE_WIDTH) / 2;
        int y = (this.height - TEXTURE_HEIGHT) / 2;
        
        // Draw the background
        this.blit(poseStack, x, y, 0, 0, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        
        // Draw energy text
        String energyText = String.format("Energy: %d / %d", 
            energyData.getCurrentEnergy(), 
            energyData.getMaxEnergy());
        drawCenteredString(poseStack, this.font, energyText,
            this.width / 2,
            y + 20,
            0xFFFFFF);
        
        super.render(poseStack, mouseX, mouseY, partialTick);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
} 