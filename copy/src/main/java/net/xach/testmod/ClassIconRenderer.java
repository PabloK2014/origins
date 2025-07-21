package net.xach.testmod;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.charset.StandardCharsets;


@Mod.EventBusSubscriber(modid = TestMod.MOD_ID, value = Dist.CLIENT)
@OnlyIn(Dist.CLIENT)
public class ClassIconRenderer {
    private static float currentExpWidth = 0;
    private static float currentEnergyWidth = 0;
    private static float currentHealthWidth = 0;
    private static float lastHealth = 20.0f;
    private static float damageFlashTime = 0;
    private static final ResourceLocation CUSTOM_HP_TEXTURE = new ResourceLocation(TestMod.MOD_ID, "textures/gui/customhp.png");
    private static final int CUSTOM_HP_COLOR = 0xFF6B03FC;

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof InventoryScreen screen) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null) {
                minecraft.player.getCapability(TestMod.PlayerClassCapability.CAPABILITY).ifPresent(cap -> {
                    String playerClass = cap.getPlayerClass();
                    if (!playerClass.isEmpty()) {
                        int iconX = screen.getGuiLeft() - 26;
                        int iconY = screen.getGuiTop() + 20;
                        int iconWidth = 16;
                        int iconHeight = 16;

                        int buttonX = screen.getGuiLeft() + 130;
                        int buttonY = iconY;
                        int buttonWidth = 60;
                        int buttonHeight = 16;

                        int offsetX = screen.getRecipeBookComponent().isVisible() ? 70 : 0;

                        event.addListener(Button.builder(
                                        Component.literal("Навыки"),
                                        button -> {
                                            TestMod.NETWORK.sendToServer(new OpenSkillTreePacket());
                                        }
                                )
                                .pos(buttonX + offsetX, buttonY)
                                .size(buttonWidth, buttonHeight)
                                .build());
                    } else {
                        // Тестовая кнопка для открытия выбора класса
                        int buttonX = screen.getGuiLeft() + 130;
                        int buttonY = screen.getGuiTop() + 20;
                        int buttonWidth = 60;
                        int buttonHeight = 16;
                        int offsetX = screen.getRecipeBookComponent().isVisible() ? 70 : 0;
                        event.addListener(Button.builder(
                                        Component.literal("Выбрать класс"),
                                        button -> TestMod.NETWORK.sendToServer(new OpenClassSelectionPacket())
                                )
                                .pos(buttonX + offsetX, buttonY + 20)
                                .size(buttonWidth, buttonHeight)
                                .build());
                    }
                });
            } else {
            }
        }
    }

    @SubscribeEvent
    public static void onRenderGameOverlayPre(RenderGuiOverlayEvent.Pre event) {
        // Отменяем рендеринг ванильного бара здоровья
        if (event.getOverlay() == VanillaGuiOverlay.PLAYER_HEALTH.type()) {
            event.setCanceled(true);
            Minecraft minecraft = Minecraft.getInstance();
            GuiGraphics guiGraphics = event.getGuiGraphics();
            renderCustomHealthBar(guiGraphics, minecraft);
        }
    }

    @SubscribeEvent
    public static void onRenderGameOverlayPost(RenderGuiOverlayEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        GuiGraphics guiGraphics = event.getGuiGraphics();

        // Рендеринг иконки класса и кнопки в инвентаре
        if (minecraft.screen instanceof InventoryScreen screen) {
            if (minecraft.player != null) {
                minecraft.player.getCapability(TestMod.PlayerClassCapability.CAPABILITY).ifPresent(cap -> {
                    String playerClass = cap.getPlayerClass();
                    if (!playerClass.isEmpty()) {
                        int iconX = screen.getGuiLeft() - 26;
                        int iconY = screen.getGuiTop() + 20;
                        int iconWidth = 16;
                        int iconHeight = 16;

                        ResourceLocation iconTexture = getIconTexture(playerClass);
                        RenderSystem.setShaderTexture(0, iconTexture);
                        guiGraphics.blit(iconTexture, iconX, iconY, 0, 0, iconWidth, iconHeight, iconWidth, iconHeight);
                    } else {
                    }
                });
            } else {
            }
        }

        // Рендеринг уровня, прогресс-бара опыта и энергии в верхнем левом углу
        if (minecraft.player != null) {
            minecraft.player.getCapability(TestMod.PlayerClassCapability.CAPABILITY).ifPresent(cap -> {
                String playerClass = cap.getPlayerClass();
                if (!playerClass.isEmpty()) {
                    int level = cap.getLevel();
                    int experience = cap.getExperience();
                    int surgeEnergy = cap.getSurgeEnergy();
                    int expForNextLevel = 100 * level;

                    // Координаты для верхнего левого угла
                    int levelX = 10;
                    int levelY = 10;
                    int expBarY = levelY + 10;
                    int energyBarY = expBarY + 10;

                    // Отображение уровня (уменьшенный шрифт)
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().scale(0.75f, 0.75f, 1.0f);
                    guiGraphics.drawString(minecraft.font, String.valueOf(level), (int) (levelX / 0.75f), (int) (levelY / 0.75f), 0xFFFFFF);
                    guiGraphics.pose().popPose();

                    // Прогресс-бар опыта (зелёный с пульсацией и плавным заполнением)
                    int barWidth = 50;
                    int barHeight = 3;
                    int barX = levelX;
                    float expProgress = (float) experience / expForNextLevel;
                    float targetExpWidth = barWidth * expProgress;
                    currentExpWidth += (targetExpWidth - currentExpWidth) * 0.1f;

                    float pulse = (float) (0.8 + 0.2 * Math.sin(System.currentTimeMillis() / 500.0));
                    int alpha = (int) (pulse * 255);
                    int greenColor = (alpha << 24) | (0x00FF00);

                    guiGraphics.fill(barX, expBarY, barX + barWidth, expBarY + barHeight, 0xFF555555);
                    guiGraphics.fill(barX, expBarY, barX + (int) currentExpWidth, expBarY + barHeight, greenColor);

                    // Прогресс-бар энергии (красный с пульсацией и плавным заполнением)
                    float energyProgress = (float) surgeEnergy / 100;
                    float targetEnergyWidth = barWidth * energyProgress;
                    currentEnergyWidth += (targetEnergyWidth - currentEnergyWidth) * 0.1f;

                    int redColor = (alpha << 24) | (0xFF0000);
                    guiGraphics.fill(barX, energyBarY, barX + barWidth, energyBarY + barHeight, 0xFF555555);
                    guiGraphics.fill(barX, energyBarY, barX + (int) currentEnergyWidth, energyBarY + barHeight, redColor);


                }
            });
        }
    }

    private static void renderCustomHealthBar(GuiGraphics guiGraphics, Minecraft minecraft) {
        Player player = minecraft.player;
        if (player == null) return;

        float health = player.getHealth();
        float maxHealth = player.getMaxHealth();
        float healthPercent = health / maxHealth;

        // Позиция ванильного бара здоровья
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int barWidth = 81; // Ванильный бар здоровья имеет ширину 81 пиксель
        int barHeight = 9; // Высота ванильного бара
        int x = screenWidth / 2 - 91; // Левый край, как у ванильного
        int y = screenHeight - 39; // Чуть выше бара опыта

        // Плавное заполнение
        float targetWidth = barWidth * healthPercent;
        currentHealthWidth += (targetWidth - currentHealthWidth) * 0.1f;

        // Вспышка при уроне (рендерится первой, чтобы быть под основным баром)
        if (health < lastHealth) {
            damageFlashTime = System.currentTimeMillis() + 600; // Вспышка длится 0.6 секунды
        }
        if (System.currentTimeMillis() < damageFlashTime) {
            float flash = (float) (0.4 + 0.6 * Math.sin(System.currentTimeMillis() / 70.0)); // Увеличенная амплитуда и скорость
            int flashColor = (int) (flash * 255) << 24 | 0xFF0000; // Красная вспышка
            guiGraphics.fill(x, y, x + barWidth, y + barHeight, flashColor);
        }

        guiGraphics.fill(x, y, x + barWidth, y + barHeight, 0x80000000);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        int healthColor = CUSTOM_HP_COLOR;
        if (health >= maxHealth) {
            float pulse = (float) (0.7 + 0.8 * Math.sin(System.currentTimeMillis() / 400.0));
            int alpha = (int) (pulse * 255);
            // Изменение яркости для мерцания
            int r = (CUSTOM_HP_COLOR >> 16) & 0xFF;
            int g = (CUSTOM_HP_COLOR >> 8) & 0xFF;
            int b = CUSTOM_HP_COLOR & 0xFF;
            int brightR = (int) (r * (0.6 + 0.4 * pulse));
            int brightG = (int) (g * (0.6 + 0.4 * pulse));
            int brightB = (int) (b * (0.6 + 0.4 * pulse));
            healthColor = (alpha << 24) | (brightR << 16) | (brightG << 8) | brightB;
        }

        // Рендеринг текстуры или запасного цвета
        if (minecraft.getResourceManager().getResource(CUSTOM_HP_TEXTURE).isPresent()) {
            RenderSystem.setShaderTexture(0, CUSTOM_HP_TEXTURE);
            guiGraphics.blit(
                    CUSTOM_HP_TEXTURE,
                    x, y,
                    0, 0,
                    (int) currentHealthWidth, barHeight,
                    barWidth, barHeight
            );
        } else {
            guiGraphics.fill(
                    x, y,
                    x + (int) currentHealthWidth, y + barHeight,
                    healthColor
            );
        }
        RenderSystem.disableBlend();

        // Частицы при получении урона
        if (health < lastHealth && minecraft.level != null) {
            for (int i = 0; i < 3; i++) {
                double particleX = player.getX() + (minecraft.level.random.nextDouble() - 0.5) * 1.0;
                double particleY = player.getY() + 1.0;
                double particleZ = player.getZ() + (minecraft.level.random.nextDouble() - 0.5) * 1.0;
                minecraft.level.addParticle(
                        ParticleTypes.DAMAGE_INDICATOR,
                        particleX, particleY, particleZ,
                        (minecraft.level.random.nextDouble() - 0.5) * 0.5,
                        0.5,
                        (minecraft.level.random.nextDouble() - 0.5) * 0.5
                );
            }
        }

        lastHealth = health;

    }

    private static ResourceLocation getIconTexture(String playerClass) {
        String texturePath = switch (playerClass) {
            case "war" -> "textures/gui/war.png";
            case "cook" -> "textures/gui/chef.png";
            case "yandex.go" -> "textures/gui/yandex.png";
            case "pivo" -> "textures/gui/brewer.png";
            case "smith" -> "textures/gui/smith.png";
            case "miner" -> "textures/gui/miner.png";
            default -> "textures/gui/unknown.png";
        };
        ResourceLocation texture = new ResourceLocation(TestMod.MOD_ID, texturePath);

        try {
            Minecraft.getInstance().getResourceManager().getResource(texture).orElseThrow();

        } catch (Exception e) {

        }
        return texture;
    }
}