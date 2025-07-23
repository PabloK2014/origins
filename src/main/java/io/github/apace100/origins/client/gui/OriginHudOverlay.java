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
    private static final Identifier ALL_TEXTURE = new Identifier(Origins.MODID, "textures/gui/all.png");
    
    // Размеры полосок
    private static final int BAR_WIDTH = 81;     // Ширина полосок
    private static final int BAR_HEIGHT = 9;     // Высота полосок
    
    // Координаты в текстуре all.png
    // 1. Опыт (первая текстура)
    private static final int EXP_V = 0;          // Y координата полоски опыта
    private static final int EXP_OVERLAY_V = 29;  // Y координата оверлея опыта
    private static final int EXP_TEXT_COLOR = 0xFFFF55; // Желтый цвет для текста опыта
    
    // 2. Энергия (вторая текстура)
    private static final int ENERGY_V = 9;      // Y координата полоски энергии
    private static final int ENERGY_TEXT_COLOR = 0x55FFFF; // Голубой цвет для текста энергии
    
    // 3. Кулдаун (третья текстура)
    private static final int COOLDOWN_V = 19;    // Y координата полоски кулдауна
    private static final int COOLDOWN_TEXT_COLOR = 0xFF5555; // Красный цвет для текста кулдауна
    
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
            Origins.LOGGER.info("Rendering HUD bars with texture: " + ALL_TEXTURE);
            hasLoggedDebug = true;
        }
        
        // Получаем компоненты
        ProfessionComponent professionComponent = ProfessionComponent.KEY.get(client.player);
        ProfessionProgress progress = professionComponent.getCurrentProgress();
        if (progress == null) {
            return;
        }
        
        PlayerSkillComponent skillComponent = PlayerSkillComponent.KEY.get(client.player);
        if (skillComponent == null) {
            return;
        }
        
        // Позиция HUD (слева от скролл-бара)
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        
        // Координаты (отступ слева от скролл-бара)
        int baseX = 5;
        int baseY = screenHeight / 2 - 40;
        
        // 1. Рисуем полоску опыта
        renderExpBar(context, baseX, baseY, progress);
        
        // 2. Рисуем полоску энергии
        float energyPercent = skillComponent.getEnergyPercentage();
        renderEnergyBar(context, baseX, baseY + 15, energyPercent);
        
        // 3. Рисуем полоску кулдауна активного навыка
        String activeSkill = skillComponent.getActiveSkill();
        if (activeSkill != null && !activeSkill.isEmpty()) {
            float cooldownPercent = getCooldownPercentage(skillComponent, activeSkill);
            renderCooldownBar(context, baseX, baseY + 30, cooldownPercent, activeSkill);
        }
    }
    
    private static void renderExpBar(DrawContext context, int x, int y, ProfessionProgress progress) {
        try {
            double expPercent = progress.getProgressToNextLevel();
            int filledWidth = (int)(BAR_WIDTH * expPercent);
            
            // Сначала рисуем базовую полоску опыта
            context.drawTexture(
                ALL_TEXTURE,       // текстура
                x, y,              // позиция на экране
                0, EXP_V,         // позиция в текстуре (U,V)
                BAR_WIDTH, BAR_HEIGHT,  // размер выводимой части
                256, 256           // общий размер текстуры
            );
            
            // Затем рисуем оверлей заполнения
            if (filledWidth > 0) {
                context.drawTexture(
                    ALL_TEXTURE,       // текстура
                    x, y,              // позиция на экране
                    0, EXP_OVERLAY_V,  // позиция в текстуре (U,V)
                    filledWidth, BAR_HEIGHT,  // размер выводимой части
                    256, 256           // общий размер текстуры
                );
            }
            
            // Текст уровня
            String levelText = String.valueOf(progress.getLevel());
            context.drawTextWithShadow(client.textRenderer, levelText, x + BAR_WIDTH + 4, y + 1, EXP_TEXT_COLOR);
        } catch (Exception e) {
            Origins.LOGGER.error("Failed to render exp bar: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void renderEnergyBar(DrawContext context, int x, int y, float fillPercent) {
        try {
            int filledWidth = (int)(BAR_WIDTH * fillPercent);
            
            // Рисуем полоску энергии
            context.drawTexture(
                ALL_TEXTURE,         // текстура
                x, y,                // позиция на экране
                0, ENERGY_V,        // позиция в текстуре (U,V)
                filledWidth, BAR_HEIGHT,  // размер выводимой части
                256, 256             // общий размер текстуры
            );
            
            // Текст энергии
            PlayerSkillComponent skillComponent = PlayerSkillComponent.KEY.get(client.player);
            if (skillComponent != null) {
                String energyText = skillComponent.getCurrentEnergy() + "/" + skillComponent.getMaxEnergy();
                context.drawTextWithShadow(client.textRenderer, energyText, x + BAR_WIDTH + 4, y + 1, ENERGY_TEXT_COLOR);
            }
        } catch (Exception e) {
            Origins.LOGGER.error("Failed to render energy bar: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void renderCooldownBar(DrawContext context, int x, int y, float fillPercent, String skillName) {
        try {
            PlayerSkillComponent skillComponent = PlayerSkillComponent.KEY.get(client.player);
            if (skillComponent == null) return;
            
            long cooldownRemaining = skillComponent.getSkillCooldownRemaining(skillName);
            int maxCooldown = getMaxCooldownForSkill(skillName);
            
            // Рассчитываем точный процент заполнения
            float exactFillPercent = 1.0f - ((float) cooldownRemaining / maxCooldown);
            int filledWidth = (int)(BAR_WIDTH * exactFillPercent);
            
            // Рисуем полоску кулдауна
            if (filledWidth > 0) {
                context.drawTexture(
                    ALL_TEXTURE,       // текстура
                    x, y,              // позиция на экране
                    0, COOLDOWN_V,    // позиция в текстуре (U,V)
                    filledWidth, BAR_HEIGHT,  // размер выводимой части
                    256, 256           // общий размер текстуры
                );
            }
            
            // Текст кулдауна
            if (cooldownRemaining > 0) {
                String cooldownText = String.format("%.1fs", cooldownRemaining / 20.0f);
                context.drawTextWithShadow(client.textRenderer, cooldownText, x + BAR_WIDTH + 4, y + 1, COOLDOWN_TEXT_COLOR);
            }
        } catch (Exception e) {
            Origins.LOGGER.error("Failed to render cooldown bar: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Получает процент кулдауна навыка (0.0 = готов, 1.0 = полный кулдаун)
     */
    private static float getCooldownPercentage(PlayerSkillComponent skillComponent, String skillId) {
        if (!skillComponent.isSkillOnCooldown(skillId)) {
            return 0.0f;
        }
        
        long remainingTicks = skillComponent.getSkillCooldownRemaining(skillId);
        
        // Получаем максимальный кулдаун для навыка (в тиках)
        int maxCooldown = getMaxCooldownForSkill(skillId);
        
        if (maxCooldown <= 0) {
            return 0.0f;
        }
        
        return Math.min(1.0f, (float) remainingTicks / maxCooldown);
    }
    
    /**
     * Получает максимальный кулдаун для навыка в тиках
     */
    private static int getMaxCooldownForSkill(String skillId) {
        return switch (skillId) {
            case "mad_boost" -> 100; // 5 секунд
            case "last_chance" -> 1200; // 60 секунд
            case "indestructibility" -> 1800; // 90 секунд
            case "dagestan" -> 2400; // 120 секунд
            case "sprint_boost" -> 600; // 30 секунд
            case "speed_surge" -> 1200; // 60 секунд
            case "carry_capacity_basic" -> 600; // 30 секунд
            case "hot_strike" -> 600; // 30 секунд
            case "instant_repair" -> 6000; // 300 секунд (5 минут)
            default -> 1200; // 60 секунд по умолчанию
        };
    }
    
    /**
     * Validates that the required texture exists and can be loaded
     */
    private static void validateTexture() {
        try {
            if (TextureValidator.validateTexture(ALL_TEXTURE)) {
                Origins.LOGGER.info("Successfully validated texture: " + ALL_TEXTURE);
            } else {
                Origins.LOGGER.warn("Texture validation failed: " + ALL_TEXTURE);
                
                // Try to get a valid fallback texture
                Identifier validTexture = TextureValidator.getValidTexture(ALL_TEXTURE);
                if (!validTexture.equals(ALL_TEXTURE)) {
                    Origins.LOGGER.info("Using fallback texture: " + validTexture);
                } else {
                    Origins.LOGGER.error("No valid texture or fallback found for: " + ALL_TEXTURE);
                    Origins.LOGGER.error("Expected location: src/main/resources/assets/origins/textures/gui/all.png");
                }
            }
        } catch (Exception e) {
            Origins.LOGGER.error("Failed to validate texture " + ALL_TEXTURE + ": " + e.getMessage());
            Origins.LOGGER.error("This may cause GUI rendering issues");
        }
    }
}