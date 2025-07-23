package io.github.apace100.origins.client.gui;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginLayers;
import io.github.apace100.origins.profession.ProfessionComponent;
import io.github.apace100.origins.profession.ProfessionProgress;
import io.github.apace100.origins.registry.ModComponents;
import io.github.apace100.origins.skill.PlayerSkillComponent;
import io.github.apace100.origins.util.TextureValidator;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public class OriginHudOverlay {
    
    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static final Identifier RESOURCE_BAR = new Identifier(Origins.MODID, "textures/gui/resource_bar2.png");
    
    // Размеры полосок
    private static final int ENERGY_WIDTH = 80;  // Ширина полоски энергии
    private static final int ENERGY_HEIGHT = 8;  // Высота полоски энергии
    private static final int EXP_WIDTH = 131;    // Ширина полоски опыта
    private static final int EXP_HEIGHT = 5;     // Высота полоски опыта
    
    // Координаты в текстуре (256x256)
    private static final int ENERGY_EMPTY_V = 0;  // Пустая полоска энергии
    private static final int ENERGY_FILLED_V = 8; // Заполненная полоска энергии
    private static final int EXP_EMPTY_V = 16;    // Пустая полоска опыта
    private static final int EXP_FILLED_V = 21;   // Заполненная полоска опыта
    
    private static boolean hasLoggedDebug = false;
    private static boolean textureValidated = false;
    
    public static void render(DrawContext context, float tickDelta) {
        if (client.player == null || client.options.debugEnabled) {
            return;
        }
        
        // Validate texture on first render
        if (!textureValidated) {
            validateTexture();
            textureValidated = true;
        }
        
        if (!hasLoggedDebug) {
            Origins.LOGGER.info("Rendering HUD bars with texture: " + RESOURCE_BAR);
            hasLoggedDebug = true;
        }
        
        // Получаем компоненты
        ProfessionComponent professionComponent = ProfessionComponent.KEY.get(client.player);
        ProfessionProgress progress = professionComponent.getCurrentProgress();
        if (progress == null) {
            return;
        }
        
        // Позиция HUD (слева от скролл-бара)
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        
        // Координаты (отступ слева от скролл-бара)
        int baseX = 5;
        int baseY = screenHeight / 2 - 30;
        
        // Рисуем полоску энергии
        renderEnergyBar(context, baseX, baseY, 0.75f);
        
        // Рисуем полоску опыта
        renderExpBar(context, baseX, baseY + 12, progress);
    }
    
    private static void renderEnergyBar(DrawContext context, int x, int y, float fillPercent) {
        try {
            // Рисуем пустую полоску энергии
            context.drawTexture(
                RESOURCE_BAR,         // текстура
                x, y,                // позиция на экране
                0, ENERGY_EMPTY_V,   // позиция в текстуре (U,V)
                ENERGY_WIDTH, ENERGY_HEIGHT,  // размер выводимой части
                256, 256             // общий размер текстуры (256x256)
            );
            
            // Рисуем заполненную часть
            int filledWidth = (int)(ENERGY_WIDTH * fillPercent);
            if (filledWidth > 0) {
                context.drawTexture(
                    RESOURCE_BAR,         // текстура
                    x, y,                // позиция на экране
                    0, ENERGY_FILLED_V,  // позиция в текстуре (U,V)
                    filledWidth, ENERGY_HEIGHT,  // размер выводимой части
                    256, 256             // общий размер текстуры
                );
            }
        } catch (Exception e) {
            Origins.LOGGER.error("Failed to render energy bar: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void renderExpBar(DrawContext context, int x, int y, ProfessionProgress progress) {
        try {
            // Рисуем пустую полоску опыта
            context.drawTexture(
                RESOURCE_BAR,       // текстура
                x, y,              // позиция на экране
                0, EXP_EMPTY_V,    // позиция в текстуре (U,V)
                EXP_WIDTH, EXP_HEIGHT,  // размер выводимой части
                256, 256           // общий размер текстуры
            );
            
            // Рисуем заполненную часть
            double expPercent = progress.getProgressToNextLevel();
            int filledWidth = (int)(EXP_WIDTH * expPercent);
            if (filledWidth > 0) {
                context.drawTexture(
                    RESOURCE_BAR,       // текстура
                    x, y,              // позиция на экране
                    0, EXP_FILLED_V,   // позиция в текстуре (U,V)
                    filledWidth, EXP_HEIGHT,  // размер выводимой части
                    256, 256           // общий размер текстуры
                );
            }
            
            // Текст уровня
            String levelText = String.valueOf(progress.getLevel());
            context.drawTextWithShadow(client.textRenderer, levelText, x + EXP_WIDTH + 4, y, 0xFFFF55);
        } catch (Exception e) {
            Origins.LOGGER.error("Failed to render exp bar: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Validates that the required texture exists and can be loaded
     */
    private static void validateTexture() {
        try {
            if (TextureValidator.validateTexture(RESOURCE_BAR)) {
                Origins.LOGGER.info("Successfully validated texture: " + RESOURCE_BAR);
            } else {
                Origins.LOGGER.warn("Texture validation failed: " + RESOURCE_BAR);
                
                // Try to get a valid fallback texture
                Identifier validTexture = TextureValidator.getValidTexture(RESOURCE_BAR);
                if (!validTexture.equals(RESOURCE_BAR)) {
                    Origins.LOGGER.info("Using fallback texture: " + validTexture);
                } else {
                    Origins.LOGGER.error("No valid texture or fallback found for: " + RESOURCE_BAR);
                    Origins.LOGGER.error("Expected location: src/main/resources/assets/origins/textures/gui/resource_bar2.png");
                }
            }
        } catch (Exception e) {
            Origins.LOGGER.error("Failed to validate texture " + RESOURCE_BAR + ": " + e.getMessage());
            Origins.LOGGER.error("This may cause GUI rendering issues");
        }
    }
}