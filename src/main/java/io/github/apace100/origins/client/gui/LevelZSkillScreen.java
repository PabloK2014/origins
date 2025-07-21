package io.github.apace100.origins.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.apace100.origins.Origins;
import io.github.apace100.origins.networking.ModPackets;
import io.github.apace100.origins.profession.ProfessionComponent;
import io.github.apace100.origins.profession.ProfessionProgress;
import io.github.apace100.origins.profession.ProfessionSkills;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class LevelZSkillScreen extends Screen {
    private static final Identifier BACKGROUND_TEXTURE = new Identifier(Origins.MODID, "textures/gui/skill_background.png");
    private static final Identifier ICON_TEXTURES = new Identifier(Origins.MODID, "textures/gui/icons.png");

    private final int backgroundWidth = 200;
    private final int backgroundHeight = 215;
    private int x;
    private int y;

    private final List<SkillButton> skillButtons = new ArrayList<>();
    private ProfessionProgress progress;

    public LevelZSkillScreen() {
        super(Text.translatable("screen.origins.levelz_skill_screen"));
    }

    private class SkillButton {
        private final WidgetButtonPage iconButton;
        private final WidgetButtonPage levelButton;
        private final String skillId;

        public SkillButton(int x, int y, String skillId, int iconU, int iconV, Text tooltip) {
            this.skillId = skillId;

            // Кнопка иконки навыка
            this.iconButton = addDrawableChild(new WidgetButtonPage(
                x, y, 16, 16, iconU, iconV, false, true, tooltip, button -> {
                    // Показываем информацию о навыке
                }));

            // Добавляем подсказки
            if (progress != null) {
                for (Text tooltipLine : progress.getSkills().getSkillTooltip(skillId)) {
                    this.iconButton.addTooltip(tooltipLine);
                }
            }

            // Кнопка повышения уровня
            this.levelButton = addDrawableChild(new WidgetButtonPage(
                x + 68, y + 2, 13, 13, 33, 42, true, true, null, button -> {
                    if (progress != null && progress.getSkillPoints() > 0) {
                        ProfessionSkills skills = progress.getSkills();
                        if (skills.canIncreaseSkill(skillId)) {
                            // Отправляем пакет на сервер
                            PacketByteBuf buf = PacketByteBufs.create();
                            buf.writeString(skillId);
                            ClientPlayNetworking.send(ModPackets.SYNC_SKILLS, buf);

                            // Обновляем локальное состояние
                            skills.increaseSkill(skillId);
                            progress.spendSkillPoint();

                            // Обновляем подсказки
                            iconButton.tooltip.clear();
                            for (Text tooltipLine : skills.getSkillTooltip(skillId)) {
                                iconButton.addTooltip(tooltipLine);
                            }
                        }
                    }
                }));
        }

        public void update() {
            if (progress != null) {
                ProfessionSkills skills = progress.getSkills();
                levelButton.active = progress.getSkillPoints() > 0 && skills.canIncreaseSkill(skillId);
            }
        }
    }

    public static class WidgetButtonPage extends ButtonWidget {
        private final boolean hoverOutline;
        private final boolean clickable;
        private final int textureX;
        private final int textureY;
        List<Text> tooltip = new ArrayList<>();
        private final MinecraftClient client;

        public WidgetButtonPage(int x, int y, int sizeX, int sizeY, int textureX, int textureY,
                              boolean hoverOutline, boolean clickable, Text tooltip, PressAction onPress) {
            super(x, y, sizeX, sizeY, ScreenTexts.EMPTY, onPress, DEFAULT_NARRATION_SUPPLIER);
            this.hoverOutline = hoverOutline;
            this.clickable = clickable;
            this.textureX = textureX;
            this.textureY = textureY;
            this.client = MinecraftClient.getInstance();
            if (tooltip != null) {
                this.tooltip.add(tooltip);
            }
        }

        public void addTooltip(Text text) {
            this.tooltip.add(text);
        }

        @Override
        public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
            if (this.client != null) {
                context.setShaderColor(1.0f, 1.0f, 1.0f, this.alpha);
                RenderSystem.enableBlend();
                RenderSystem.enableDepthTest();
                int i = hoverOutline ? this.getTextureY() : 0;
                context.drawTexture(ICON_TEXTURES, this.getX(), this.getY(), 
                    this.textureX + i * this.width, this.textureY, this.width, this.height);
                context.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                if (this.isHovered()) {
                    context.drawTooltip(this.client.textRenderer, this.tooltip, mouseX, mouseY);
                }
            }
        }

        private int getTextureY() {
            int i = 1;
            if (!this.active) {
                i = 0;
            } else if (this.isSelected()) {
                i = 2;
            }
            return i;
        }
    }

    @Override
    protected void init() {
        super.init();
        this.x = (this.width - this.backgroundWidth) / 2;
        this.y = (this.height - this.backgroundHeight) / 2;

        // Получаем прогресс профессии
        if (this.client != null && this.client.player != null) {
            ProfessionComponent component = ProfessionComponent.KEY.get(this.client.player);
            if (component != null) {
                this.progress = component.getCurrentProgress();
            }
        }

        // Кнопка "Назад"
        this.addDrawableChild(new ButtonWidget.Builder(Text.translatable("gui.back"), (button) -> {
            if (this.client != null) {
                this.client.setScreen(null);
            }
        }).dimensions(this.x + 5, this.y + backgroundHeight - 25, 60, 20).build());

        // Кнопка "Дерево навыков"
        this.addDrawableChild(new ButtonWidget.Builder(Text.translatable("gui.origins.skill_tree"), (button) -> {
            if (this.client != null) {
                this.client.setScreen(new SkillTreeScreen());
            }
        }).dimensions(this.x + backgroundWidth - 105, this.y + backgroundHeight - 25, 100, 20).build());

        // Создаем кнопки навыков только если есть прогресс
        if (this.progress != null) {
            skillButtons.clear();
            int baseY = this.y + 90;

            // Создаем кнопки навыков
            String[] skills = {"health", "strength", "agility", "defense"};
            for (int i = 0; i < skills.length; i++) {
                final String skillId = skills[i];
                skillButtons.add(new SkillButton(
                    this.x + 15,
                    baseY + i * 20,
                    skillId,
                    i * 16, // Каждая следующая иконка смещается на 16 пикселей вправо
                    16, // Все иконки находятся на Y=16
                    Text.translatable("skill.origins." + skillId + ".name")
                ));
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        
        // Отрисовка фона
        context.drawTexture(BACKGROUND_TEXTURE, x, y, 0, 0, backgroundWidth, backgroundHeight);

        // Отрисовка персонажа
        if (this.client != null && this.client.player != null) {
            int scaledWidth = this.client.getWindow().getScaledWidth();
            int scaledHeight = this.client.getWindow().getScaledHeight();
            InventoryScreen.drawEntity(context, scaledWidth / 2 - 75, scaledHeight / 2 - 40, 30, -28, 0, this.client.player);
        }

        // Отрисовка заголовка
        Text title = this.getTitle();
        context.drawText(this.textRenderer, title, x + (backgroundWidth - textRenderer.getWidth(title)) / 2, y + 7, 0x3F3F3F, false);

        // Отрисовка информации о прогрессии
        if (progress != null) {
            int rightColumnX = x + backgroundWidth - 110; // Отступ справа для текста

            // Название профессии
            Text professionName = Text.translatable("gui.origins.profession." + progress.getProfessionId().getPath());
            context.drawText(this.textRenderer, professionName, rightColumnX, y + 30, 0xFFFFFF, false);

            // Уровень
            Text levelText = Text.translatable("gui.origins.level", progress.getLevel());
            context.drawText(this.textRenderer, levelText, rightColumnX, y + 45, 0xFFFFFF, false);

            // Очки навыков
            Text skillPointsText = Text.translatable("gui.origins.skill_points", progress.getSkillPoints());
            context.drawText(this.textRenderer, skillPointsText, rightColumnX, y + 60, 0xFFFFFF, false);

            // Полоса опыта (под текстом очков навыка)
            int expBarWidth = 150;
            int expBarHeight = 5;
            int expBarX = x + (backgroundWidth - expBarWidth) / 2;
            int expBarY = y + 75; // Сразу под текстом очков навыка

            // Фон полосы
            context.fill(expBarX, expBarY, expBarX + expBarWidth, expBarY + expBarHeight, 0xFF555555);

            // Заполнение полосы
            float progressValue = (float)this.progress.getExperience() / this.progress.getExperienceForNextLevel();
            int filledWidth = (int)(expBarWidth * progressValue);
            context.fill(expBarX, expBarY, expBarX + filledWidth, expBarY + expBarHeight, 0xFF00FF00);

            // Текст опыта (центрируем под полосой)
            Text expText = Text.translatable("gui.origins.experience", this.progress.getExperience(), this.progress.getExperienceForNextLevel());
            int expTextX = expBarX + (expBarWidth - textRenderer.getWidth(expText)) / 2;
            context.drawText(this.textRenderer, expText, expTextX, expBarY + expBarHeight + 2, 0xFFFFFF, false);

            // Обновляем состояние кнопок навыков
            for (SkillButton button : skillButtons) {
                button.update();
            }

            // Отображаем текущие уровни навыков
            ProfessionSkills skills = progress.getSkills();
            for (int i = 0; i < skillButtons.size(); i++) {
                SkillButton button = skillButtons.get(i);
                Text currentLevelText = Text.literal(skills.getSkillLevel(button.skillId) + "/" + ProfessionSkills.MAX_SKILL_LEVEL);
                int textX = button.iconButton.getX() + 20;
                int textY = button.iconButton.getY() + 4;
                context.drawText(this.textRenderer, currentLevelText, textX, textY, 0x3F3F3F, false);
            }
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
} 