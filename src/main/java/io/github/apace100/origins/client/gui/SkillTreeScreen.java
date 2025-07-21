package io.github.apace100.origins.client.gui;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginLayers;
import io.github.apace100.origins.registry.ModComponents;
import io.github.apace100.origins.skill.PlayerSkillComponent;
import io.github.apace100.origins.skill.SkillTreeHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Экран дерева навыков
 */
public class SkillTreeScreen extends Screen {
    
    private static final int BACKGROUND_WIDTH = 320;
    private static final int BACKGROUND_HEIGHT = 240;
    private static final int SKILL_BUTTON_SIZE = 24;
    private static final int BRANCH_SPACING = 100;
    private static final int SKILL_SPACING = 40;

    private int backgroundX;
    private int backgroundY;
    private int scrollX = 0;
    private int scrollY = 0;

    private SkillTreeHandler.SkillTree skillTree;
    private String currentClass;
    private int availableSkillPoints;
    private PlayerSkillComponent skillComponent;
    private SkillTreeHandler.Skill selectedSkill;
    private List<SkillButton> skillButtons = new ArrayList<>();

    public SkillTreeScreen() {
        super(Text.translatable("screen.origins.skill_tree"));
    }

    @Override
    protected void init() {
        super.init();
        this.backgroundX = (this.width - BACKGROUND_WIDTH) / 2;
        this.backgroundY = (this.height - BACKGROUND_HEIGHT) / 2;

        // Получаем текущий класс игрока
        if (this.client != null && this.client.player != null) {
            OriginComponent originComponent = ModComponents.ORIGIN.get(this.client.player);
            Origin origin = originComponent.getOrigin(OriginLayers.getLayer(Origins.identifier("origin")));
            if (origin != null) {
                this.currentClass = origin.getIdentifier().toString();
                this.skillTree = SkillTreeHandler.getSkillTree(this.currentClass);
            }

            // Получаем компонент навыков
            this.skillComponent = PlayerSkillComponent.KEY.get(this.client.player);
            this.availableSkillPoints = skillComponent.getAvailableSkillPoints();
        }

        // Кнопка закрытия
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.close"), button -> {
            this.close();
        }).dimensions(this.backgroundX + BACKGROUND_WIDTH - 60, this.backgroundY + BACKGROUND_HEIGHT - 30, 50, 20).build());

        // Создаем кнопки навыков
        createSkillButtons();
    }

    private void createSkillButtons() {
        skillButtons.clear();
        if (skillTree == null) return;

        int branchIndex = 0;
        for (List<SkillTreeHandler.Skill> branch : skillTree.getBranches()) {
            int skillIndex = 0;
            for (SkillTreeHandler.Skill skill : branch) {
                int x = backgroundX + 50 + branchIndex * BRANCH_SPACING + scrollX;
                int y = backgroundY + 50 + skillIndex * SKILL_SPACING + scrollY;

                SkillButton button = new SkillButton(x, y, SKILL_BUTTON_SIZE, SKILL_BUTTON_SIZE, skill);
                skillButtons.add(button);
                skillIndex++;
            }
            branchIndex++;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);

        // Рисуем фон
        context.fill(backgroundX, backgroundY, backgroundX + BACKGROUND_WIDTH, backgroundY + BACKGROUND_HEIGHT, 0x80000000);
        context.drawBorder(backgroundX, backgroundY, BACKGROUND_WIDTH, BACKGROUND_HEIGHT, 0xFFAAAAAA);

        // Рисуем заголовок
        String title = getProfessionDisplayName(currentClass);
        context.drawCenteredTextWithShadow(this.textRenderer, title, this.width / 2, backgroundY + 15, 0xFFFFFF);

        // Рисуем доступные очки навыков
        String pointsText = "Доступные очки: " + availableSkillPoints;
        context.drawTextWithShadow(this.textRenderer, pointsText, backgroundX + 15, backgroundY + 30, 0xFFFF55);

        // Рисуем линии между связанными навыками
        drawSkillConnections(context);

        // Рисуем кнопки навыков
        for (SkillButton button : skillButtons) {
            button.render(context, mouseX, mouseY);
        }

        // Рисуем информацию о выбранном навыке
        if (selectedSkill != null) {
            renderSkillInfo(context, selectedSkill, mouseX, mouseY);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawSkillConnections(DrawContext context) {
        if (skillTree == null) return;

        for (SkillButton button : skillButtons) {
            SkillTreeHandler.Skill skill = button.skill;
            if (skill.getParentId() != null) {
                // Находим родительскую кнопку
                SkillButton parentButton = null;
                for (SkillButton b : skillButtons) {
                    if (b.skill.getId().equals(skill.getParentId())) {
                        parentButton = b;
                        break;
                    }
                }

                if (parentButton != null) {
                    // Определяем цвет линии в зависимости от изученности навыка
                    int lineColor;
                    int skillLevel = skillComponent.getSkillLevel(skill.getId());
                    int parentLevel = skillComponent.getSkillLevel(skill.getParentId());

                    if (skillLevel > 0) {
                        lineColor = 0xFF00FF00; // Зеленый для изученных навыков
                    } else if (parentLevel > 0 && skillComponent.canLearnSkill(skill)) {
                        lineColor = 0xFFFFFF00; // Желтый для доступных навыков
                    } else {
                        lineColor = 0xFF555555; // Серый для недоступных навыков
                    }

                    // Рисуем линию
                    int x1 = parentButton.x + SKILL_BUTTON_SIZE / 2;
                    int y1 = parentButton.y + SKILL_BUTTON_SIZE / 2;
                    int x2 = button.x + SKILL_BUTTON_SIZE / 2;
                    int y2 = button.y + SKILL_BUTTON_SIZE / 2;

                    drawLine(context, x1, y1, x2, y2, lineColor);
                }
            }
        }
    }

    private void drawLine(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        // Рисуем линию как серию маленьких прямоугольников
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;
        int e2;

        while (true) {
            context.fill(x1, y1, x1 + 1, y1 + 1, color);
            if (x1 == x2 && y1 == y2) break;
            e2 = 2 * err;
            if (e2 > -dy) {
                err = err - dy;
                x1 = x1 + sx;
            }
            if (e2 < dx) {
                err = err + dx;
                y1 = y1 + sy;
            }
        }
    }

    private void renderSkillInfo(DrawContext context, SkillTreeHandler.Skill skill, int mouseX, int mouseY) {
        int infoX = backgroundX + BACKGROUND_WIDTH - 150;
        int infoY = backgroundY + 50;
        int infoWidth = 130;
        int infoHeight = 150;

        // Рисуем фон для информации
        context.fill(infoX, infoY, infoX + infoWidth, infoY + infoHeight, 0x80000000);
        context.drawBorder(infoX, infoY, infoWidth, infoHeight, 0xFFAAAAAA);

        // Название навыка
        context.drawTextWithShadow(this.textRenderer, skill.getName(), infoX + 5, infoY + 5, 0xFFFFFF);

        // Тип навыка
        String typeText = "Тип: " + getSkillTypeText(skill.getType());
        context.drawTextWithShadow(this.textRenderer, typeText, infoX + 5, infoY + 20, 0xAAAAAA);

        // Требуемый уровень
        String levelText = "Требуемый уровень: " + skill.getRequiredLevel();
        context.drawTextWithShadow(this.textRenderer, levelText, infoX + 5, infoY + 35, 0xAAAAAA);

        // Текущий уровень навыка
        int currentLevel = skillComponent.getSkillLevel(skill.getId());
        String currentLevelText = "Уровень: " + currentLevel + "/" + skill.getMaxLevel();
        context.drawTextWithShadow(this.textRenderer, currentLevelText, infoX + 5, infoY + 50, 0xFFFF55);

        // Описание навыка
        List<String> descriptionLines = new ArrayList<>();
        String[] descriptionParts = skill.getDescription().split("\\n");
        for (String part : descriptionParts) {
            // Разбиваем текст на строки вручную
            String[] words = part.split(" ");
            StringBuilder currentLine = new StringBuilder();
            
            for (String word : words) {
                String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
                if (this.textRenderer.getWidth(testLine) <= infoWidth - 10) {
                    currentLine = new StringBuilder(testLine);
                } else {
                    if (currentLine.length() > 0) {
                        descriptionLines.add(currentLine.toString());
                        currentLine = new StringBuilder(word);
                    } else {
                        descriptionLines.add(word);
                    }
                }
            }
            if (currentLine.length() > 0) {
                descriptionLines.add(currentLine.toString());
            }
        }

        int lineY = infoY + 65;
        for (String line : descriptionLines) {
            context.drawTextWithShadow(this.textRenderer, line, infoX + 5, lineY, 0xFFFFFF);
            lineY += this.textRenderer.fontHeight + 2;
        }

        // Кнопка изучения навыка
        if (skillComponent.canLearnSkill(skill) && currentLevel < skill.getMaxLevel() && availableSkillPoints > 0) {
            int buttonX = infoX + 5;
            int buttonY = infoY + infoHeight - 30;
            int buttonWidth = infoWidth - 10;
            int buttonHeight = 20;

            boolean isHovered = mouseX >= buttonX && mouseX <= buttonX + buttonWidth &&
                               mouseY >= buttonY && mouseY <= buttonY + buttonHeight;

            int buttonColor = isHovered ? 0xFF00AA00 : 0xFF008800;
            context.fill(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight, buttonColor);
            context.drawCenteredTextWithShadow(this.textRenderer, "Изучить навык", buttonX + buttonWidth / 2, buttonY + 6, 0xFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Проверяем клик по кнопкам навыков
        for (SkillButton skillButton : skillButtons) {
            if (skillButton.isMouseOver(mouseX, mouseY)) {
                selectedSkill = skillButton.skill;
                return true;
            }
        }

        // Проверяем клик по кнопке изучения навыка
        if (selectedSkill != null) {
            int infoX = backgroundX + BACKGROUND_WIDTH - 150;
            int infoY = backgroundY + 50;
            int infoWidth = 130;
            int infoHeight = 150;
            int buttonX = infoX + 5;
            int buttonY = infoY + infoHeight - 30;
            int buttonWidth = infoWidth - 10;
            int buttonHeight = 20;

            if (mouseX >= buttonX && mouseX <= buttonX + buttonWidth &&
                mouseY >= buttonY && mouseY <= buttonY + buttonHeight) {
                if (skillComponent.canLearnSkill(selectedSkill) && 
                    skillComponent.getSkillLevel(selectedSkill.getId()) < selectedSkill.getMaxLevel() && 
                    availableSkillPoints > 0) {
                    learnSkill(selectedSkill);
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void learnSkill(SkillTreeHandler.Skill skill) {
        // Изучаем навык
        skillComponent.learnSkill(skill.getId());
        availableSkillPoints = skillComponent.getAvailableSkillPoints();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (super.mouseScrolled(mouseX, mouseY, amount)) {
            return true;
        }

        // Прокрутка дерева навыков
        if (hasShiftDown()) {
            scrollX += (int) (amount * 10);
        } else {
            scrollY += (int) (amount * 10);
        }

        // Пересоздаем кнопки с новыми позициями
        createSkillButtons();
        return true;
    }

    private String getSkillTypeText(SkillTreeHandler.SkillType type) {
        return switch (type) {
            case ACTIVE -> "Активный";
            case PASSIVE -> "Пассивный";
            case GLOBAL -> "Глобальный";
        };
    }

    private String getProfessionDisplayName(String professionId) {
        if (professionId == null) return "Неизвестно";
        
        return switch (professionId) {
            case "origins:blacksmith" -> "🔨 Кузнец";
            case "origins:brewer" -> "🍺 Пивовар";
            case "origins:cook" -> "👨‍🍳 Повар";
            case "origins:courier" -> "📦 Курьер";
            case "origins:warrior" -> "⚔️ Воин";
            case "origins:miner" -> "⛏️ Шахтер";
            case "origins:human" -> "👤 Человек";
            default -> professionId.replace("origins:", "").replace("_", " ");
        };
    }

    private class SkillButton {
        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private final SkillTreeHandler.Skill skill;

        public SkillButton(int x, int y, int width, int height, SkillTreeHandler.Skill skill) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.skill = skill;
        }

        public void render(DrawContext context, int mouseX, int mouseY) {
            // Определяем цвет кнопки в зависимости от состояния
            int backgroundColor;
            int borderColor;

            int skillLevel = skillComponent.getSkillLevel(skill.getId());
            boolean isSelected = skill == selectedSkill;
            boolean isHovered = isMouseOver(mouseX, mouseY);

            if (skillLevel > 0) {
                // Изученный навык
                backgroundColor = isSelected ? 0xFF00AA00 : 0xFF008800;
                borderColor = 0xFF00FF00;
            } else if (skillComponent.canLearnSkill(skill)) {
                // Доступный для изучения навык
                backgroundColor = isSelected ? 0xFFAAAA00 : 0xFF888800;
                borderColor = 0xFFFFFF00;
            } else {
                // Недоступный навык
                backgroundColor = isSelected ? 0xFF555555 : 0xFF333333;
                borderColor = 0xFF777777;
            }

            // Рисуем фон кнопки
            context.fill(x, y, x + width, y + height, backgroundColor);
            context.drawBorder(x, y, width, height, borderColor);

            // Рисуем иконку навыка (заглушка)
            context.fill(x + 4, y + 4, x + width - 4, y + height - 4, 0xFFFFFFFF);

            // Если навык изучен и имеет уровни, показываем текущий уровень
            if (skillLevel > 0 && skill.getMaxLevel() > 1) {
                String levelText = skillLevel + "/" + skill.getMaxLevel();
                context.drawTextWithShadow(client.textRenderer, levelText, x + width - 4 - client.textRenderer.getWidth(levelText), y + height - 10, 0xFFFFFF);
            }

            // Если навык выбран или под курсором, показываем название
            if (isSelected || isHovered) {
                int textWidth = client.textRenderer.getWidth(skill.getName());
                int textX = x + width / 2 - textWidth / 2;
                int textY = y - 15;
                context.fill(textX - 2, textY - 2, textX + textWidth + 2, textY + 10, 0x80000000);
                context.drawTextWithShadow(client.textRenderer, skill.getName(), textX, textY, 0xFFFFFF);
            }
        }

        public boolean isMouseOver(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }
}