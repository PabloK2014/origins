package io.github.apace100.origins.client.gui;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.networking.ModPackets;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginLayers;
import io.github.apace100.origins.registry.ModComponents;
import io.github.apace100.origins.skill.PlayerSkillComponent;
import io.github.apace100.origins.skill.SkillTreeHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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
    private static final int INFO_HEIGHT = 100; // Увеличиваем высоту панели информации

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
        
        // Проверяем, удалось ли получить дерево навыков
        if (this.skillTree == null) {
            System.out.println("Ошибка: дерево навыков не найдено для класса " + this.currentClass);
        }

        // Кнопка закрытия
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.close"), button -> {
            this.close();
        }).dimensions(this.backgroundX + BACKGROUND_WIDTH - 60, this.backgroundY + BACKGROUND_HEIGHT - 30, 50, 20).build());

        // Кнопки для прокачки энергии
        createEnergyButtons();

        // Создаем кнопки навыков
        createSkillButtons();
    }

    private void createEnergyButtons() {
        if (skillComponent == null) return;

        int energyPanelX = backgroundX + BACKGROUND_WIDTH + 10;
        int energyPanelY = backgroundY + 90; // Размещаем кнопки ниже информации об энергии
        int buttonWidth = 120;
        int buttonHeight = 20;

        // Проверяем, есть ли доступные очки навыков
        int availablePoints = skillComponent.getAvailableSkillPoints();
        boolean canUpgrade = availablePoints > 0;

        // Кнопка увеличения максимальной энергии
        String energyButtonText = canUpgrade ? 
            "Увеличить энергию (+5)" : 
            "Увеличить энергию (нет очков)";
        
        ButtonWidget energyButton = ButtonWidget.builder(
            Text.literal(energyButtonText),
            button -> upgradeMaxEnergy()
        ).dimensions(energyPanelX, energyPanelY, buttonWidth, buttonHeight).build();
        
        energyButton.active = canUpgrade;
        this.addDrawableChild(energyButton);

        // Кнопка увеличения скорости восстановления энергии
        String regenButtonText = canUpgrade ? 
            "Скорость восст. (+1)" : 
            "Скорость восст. (нет очков)";
            
        ButtonWidget regenButton = ButtonWidget.builder(
            Text.literal(regenButtonText),
            button -> upgradeEnergyRegen()
        ).dimensions(energyPanelX, energyPanelY + 25, buttonWidth, buttonHeight).build();
        
        regenButton.active = canUpgrade;
        this.addDrawableChild(regenButton);
    }

    private void upgradeMaxEnergy() {
        if (skillComponent == null || skillComponent.getAvailableSkillPoints() < 1) {
            return;
        }

        // Отправляем пакет на сервер для увеличения максимальной энергии
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString("upgrade_max_energy");
        ClientPlayNetworking.send(new Identifier("origins", "upgrade_energy"), buf);

        // Показываем сообщение игроку
        if (client != null && client.player != null) {
            client.player.sendMessage(
                Text.literal("Максимальная энергия увеличена на 5!")
                    .formatted(net.minecraft.util.Formatting.GREEN), 
                true
            );
        }
    }

    private void upgradeEnergyRegen() {
        if (skillComponent == null || skillComponent.getAvailableSkillPoints() < 1) {
            return;
        }

        // Отправляем пакет на сервер для увеличения скорости восстановления энергии
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString("upgrade_energy_regen");
        ClientPlayNetworking.send(new Identifier("origins", "upgrade_energy"), buf);

        // Показываем сообщение игроку
        if (client != null && client.player != null) {
            client.player.sendMessage(
                Text.literal("Скорость восстановления энергии увеличена!")
                    .formatted(net.minecraft.util.Formatting.GREEN), 
                true
            );
        }
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

        // Рисуем доступные очки навыков (теперь всегда актуально)
        int actualPoints = skillComponent != null ? skillComponent.getAvailableSkillPoints() : 0;
        String pointsText = "Доступные очки: " + actualPoints;
        context.drawTextWithShadow(this.textRenderer, pointsText, backgroundX + 15, backgroundY + 30, 0xFFFF55);

        // Рисуем информацию об энергии
        if (skillComponent != null) {
            int energyPanelX = backgroundX + BACKGROUND_WIDTH + 10;
            int energyPanelY = backgroundY;
            
            // Фон для панели энергии
            context.fill(energyPanelX, energyPanelY, energyPanelX + 140, energyPanelY + 80, 0x80000000);
            context.drawBorder(energyPanelX, energyPanelY, 140, 80, 0xFFAAAAAA);
            
            // Заголовок
            context.drawTextWithShadow(this.textRenderer, "Энергия", energyPanelX + 5, energyPanelY + 5, 0xFFFFFF);
            
            // Текущая/максимальная энергия
            String energyText = skillComponent.getCurrentEnergy() + "/" + skillComponent.getMaxEnergy();
            context.drawTextWithShadow(this.textRenderer, energyText, energyPanelX + 5, energyPanelY + 55, 0x55FFFF);
            
            // Скорость восстановления (предполагаем, что есть геттер)
            String regenText = "Восст: " + skillComponent.getEnergyRegenRate() + "/сек";
            context.drawTextWithShadow(this.textRenderer, regenText, energyPanelX + 5, energyPanelY + 65, 0xAAFFAA);
        }

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
                    } else if (parentLevel >= parentButton.skill.getMaxLevel() && skillComponent.canLearnSkill(skill)) {
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
        int infoX = backgroundX;
        int infoY = backgroundY + BACKGROUND_HEIGHT + 5;
        int infoWidth = BACKGROUND_WIDTH;
        
        // Рисуем фон для информации
        context.fill(infoX, infoY, infoX + infoWidth, infoY + INFO_HEIGHT, 0x80000000);
        context.drawBorder(infoX, infoY, infoWidth, INFO_HEIGHT, 0xFFAAAAAA);

        // Название навыка
        context.drawTextWithShadow(this.textRenderer, skill.getName(), infoX + 10, infoY + 10, 0xFFFFFF);

        // Тип навыка и требуемый уровень
        String typeText = "Тип: " + getSkillTypeText(skill.getType());
        String levelText = "Требуемый уровень: " + skill.getRequiredLevel();
        context.drawTextWithShadow(this.textRenderer, typeText, infoX + 10, infoY + 25, 0xAAAAAA);
        context.drawTextWithShadow(this.textRenderer, levelText, infoX + 200, infoY + 25, 0xAAAAAA);

        // Текущий уровень навыка
        int currentLevel = skillComponent.getSkillLevel(skill.getId());
        String currentLevelText = "Уровень: " + currentLevel + "/" + skill.getMaxLevel();
        context.drawTextWithShadow(this.textRenderer, currentLevelText, infoX + 10, infoY + 40, 0xFFFF55);

        // Описание навыка (ограничиваем ширину для текста)
        String[] descriptionLines = skill.getDescription().split("\\n");
        int descY = infoY + 55;
        int maxDescWidth = infoWidth / 2 - 20; // Половина ширины минус отступы
        for (String line : descriptionLines) {
            context.drawTextWithShadow(this.textRenderer, line, infoX + 10, descY, 0xFFFFFF);
            descY += this.textRenderer.fontHeight + 2;
        }

        // Проверяем условия для изучения навыка
        boolean hasPoints = skillComponent.getAvailableSkillPoints() > 0;
        boolean notMaxLevel = currentLevel < skill.getMaxLevel();
        boolean hasRequiredLevel = skillComponent.getPlayerLevel() >= skill.getRequiredLevel();
        boolean parentOk = true;
        
        if (skill.getParentId() != null) {
            String parentId = skill.getParentId();
            int parentLevel = skillComponent.getSkillLevel(parentId);
            SkillTreeHandler.SkillTree skillTree = SkillTreeHandler.getSkillTree(currentClass);
            if (skillTree != null) {
                SkillTreeHandler.Skill parentSkill = null;
                for (SkillTreeHandler.Skill s : skillTree.getAllSkills()) {
                    if (s.getId().equals(parentId)) {
                        parentSkill = s;
                        break;
                    }
                }
                if (parentSkill != null) {
                    parentOk = parentLevel >= parentSkill.getMaxLevel();
                }
            }
        }

        // Кнопка изучения навыка или сообщение о недоступности
        boolean canLearn = skillComponent.canLearnSkill(skill) && hasRequiredLevel && parentOk;
        if (canLearn && hasPoints && notMaxLevel) {
            int buttonX = infoX + (infoWidth - 100) / 2;
            int buttonY = infoY + INFO_HEIGHT - 25; // Опускаем кнопку ниже
            int buttonWidth = 100;
            int buttonHeight = 20;

            boolean isHovered = mouseX >= buttonX && mouseX <= buttonX + buttonWidth &&
                               mouseY >= buttonY && mouseY <= buttonY + buttonHeight;

            int buttonColor = isHovered ? 0xFF00AA00 : 0xFF008800;
            context.fill(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight, buttonColor);
            context.drawCenteredTextWithShadow(this.textRenderer, "Изучить навык", buttonX + buttonWidth / 2, buttonY + 6, 0xFFFFFF);

            if (isHovered) {
                List<Text> tooltip = new ArrayList<>();
                tooltip.add(Text.literal("Стоимость: 1 очко навыка"));
                tooltip.add(Text.literal("У вас есть: " + skillComponent.getAvailableSkillPoints()));
                context.drawTooltip(this.textRenderer, tooltip, mouseX, mouseY);
            }
        } else {
            // Перемещаем сообщения о требованиях в правую часть
            int textX = infoX + infoWidth / 2 + 10; // Правая половина панели
            int textY = infoY + 10; // Выравниваем по верху с основным текстом
            if (currentLevel >= skill.getMaxLevel()) {
                context.drawTextWithShadow(this.textRenderer, "Максимальный уровень!", textX, textY, 0xFFAA00);
            } else if (!hasPoints) {
                context.drawTextWithShadow(this.textRenderer, "Нет очков навыков!", textX, textY, 0xFFAA00);
            } else if (!hasRequiredLevel) {
                context.drawTextWithShadow(this.textRenderer, 
                    "Требуется уровень " + skill.getRequiredLevel() + "!", textX, textY, 0xFFAA00);
            } else if (!canLearn) {
                if (skill.getParentId() != null && !parentOk) {
                    SkillTreeHandler.SkillTree skillTree = SkillTreeHandler.getSkillTree(currentClass);
                    if (skillTree != null) {
                        SkillTreeHandler.Skill parentSkill = null;
                        for (SkillTreeHandler.Skill s : skillTree.getAllSkills()) {
                            if (s.getId().equals(skill.getParentId())) {
                                parentSkill = s;
                                break;
                            }
                        }
                        if (parentSkill != null) {
                            int parentLevel = skillComponent.getSkillLevel(skill.getParentId());
                            context.drawTextWithShadow(this.textRenderer, 
                                "Требуется " + parentSkill.getName() + " (" + parentLevel + "/" + parentSkill.getMaxLevel() + ")", 
                                textX, textY, 0xFFAA00);
                        }
                    }
                } else {
                    context.drawTextWithShadow(this.textRenderer, "Навык недоступен!", textX, textY, 0xFFAA00);
                }
            }
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
            int infoX = backgroundX;
            int infoY = backgroundY + BACKGROUND_HEIGHT + 5;
            int buttonX = infoX + (BACKGROUND_WIDTH - 100) / 2;
            int buttonY = infoY + INFO_HEIGHT - 25;
            int buttonWidth = 100;
            int buttonHeight = 20;

            if (mouseX >= buttonX && mouseX <= buttonX + buttonWidth &&
                mouseY >= buttonY && mouseY <= buttonY + buttonHeight) {
                if (skillComponent.canLearnSkill(selectedSkill) && 
                    skillComponent.getSkillLevel(selectedSkill.getId()) < selectedSkill.getMaxLevel() && 
                    skillComponent.getAvailableSkillPoints() > 0 &&
                    skillComponent.getPlayerLevel() >= selectedSkill.getRequiredLevel()) {
                    learnSkill(selectedSkill);
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void learnSkill(SkillTreeHandler.Skill skill) {
        if (skillComponent.canLearnSkill(skill)) {
            // Отправляем пакет на сервер
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeString(skill.getId());
            ClientPlayNetworking.send(ModPackets.LEARN_TREE_SKILL, buf);

            // Обновляем локальное состояние
            skillComponent.learnSkill(skill.getId());
            availableSkillPoints = skillComponent.getAvailableSkillPoints();

            // Навык изучен
        }
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
            } else if (skill.getParentId() != null) {
                // Проверяем, прокачан ли родительский навык до максимума
                int parentLevel = skillComponent.getSkillLevel(skill.getParentId());
                SkillTreeHandler.Skill parentSkill = null;
                for (SkillTreeHandler.Skill s : skillTree.getAllSkills()) {
                    if (s.getId().equals(skill.getParentId())) {
                        parentSkill = s;
                        break;
                    }
                }
                if (parentSkill != null && parentLevel < parentSkill.getMaxLevel()) {
                    // Родительский навык не прокачан до максимума
                    backgroundColor = isSelected ? 0xFF550000 : 0xFF330000;
                    borderColor = 0xFF770000;
                } else {
                    // Недоступный навык по другим причинам
                    backgroundColor = isSelected ? 0xFF555555 : 0xFF333333;
                    borderColor = 0xFF777777;
                }
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

            // Показываем подсказку при наведении
            if (isHovered) {
                List<Text> tooltip = new ArrayList<>();
                tooltip.add(Text.literal(skill.getName()));

                // Добавляем информацию о требованиях
                if (skill.getParentId() != null) {
                    SkillTreeHandler.Skill parentSkill = null;
                    for (SkillTreeHandler.Skill s : skillTree.getAllSkills()) {
                        if (s.getId().equals(skill.getParentId())) {
                            parentSkill = s;
                            break;
                        }
                    }
                    if (parentSkill != null) {
                        int parentLevel = skillComponent.getSkillLevel(skill.getParentId());
                        tooltip.add(Text.literal("Требуется: " + parentSkill.getName() + " (" + parentLevel + "/" + parentSkill.getMaxLevel() + ")")
                            .formatted(parentLevel >= parentSkill.getMaxLevel() ? net.minecraft.util.Formatting.GREEN : net.minecraft.util.Formatting.RED));
                    }
                }

                // Отображаем подсказку
                context.drawTooltip(client.textRenderer, tooltip, mouseX, mouseY);
            }
        }

        public boolean isMouseOver(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }
}