package io.github.apace100.origins.quest.gui;

import io.github.apace100.origins.quest.BountyBoardScreen;
import io.github.apace100.origins.quest.Quest;
import io.github.apace100.origins.quest.QuestObjective;
import io.github.apace100.origins.quest.QuestReward;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Кнопка для отображения квеста в интерфейсе доски объявлений.
 * Адаптирована из BountyLongButton.kt оригинального мода Bountiful.
 */
public class QuestButton {
    public static final int WIDTH = 160;
    public static final int HEIGHT = 20;

    private final BountyBoardScreen parent;
    private final int questIndex;

    public QuestButton(BountyBoardScreen parent, int questIndex) {
        this.parent = parent;
        this.questIndex = questIndex;
    }

    /**
     * Получает данные квеста для этой кнопки
     */
    public Quest getQuestData() {
        return parent.getHandler().getQuest(questIndex);
    }

    /**
     * Проверяет, выбрана ли эта кнопка
     */
    public boolean isSelected() {
        return parent.getHandler().getSelectedQuestIndex() == questIndex;
    }

    /**
     * Отрисовка кнопки квеста
     */
    public void render(DrawContext context, int x, int y, int mouseX, int mouseY, boolean hovered) {
        Quest quest = getQuestData();
        if (quest == null) return;

        // Отрисовка фона кнопки
        SpriteHelper.drawQuestButton(context, x, y, WIDTH, isSelected(), hovered);

        // Отрисовка названия квеста в центре
        String questTitle = quest.getTitle();
        int textColor = isSelected() ? 0xFFFF00 : 0xFFFFFF; // Желтый если выбран, белый если нет
        int textX = x + 25; // Отступ слева для иконок целей
        int textY = y + (HEIGHT - 8) / 2; // Центрируем по вертикали
        context.drawText(MinecraftClient.getInstance().textRenderer, questTitle, textX, textY, textColor, false);

        // Отрисовка цели квеста (слева, только иконка)
        renderObjective(context, quest.getObjective(), x + 2, y + 2);

        // Отрисовка награды квеста (справа, только иконка)
        renderReward(context, quest.getReward(), x + WIDTH - 18, y + 2);
    }

    /**
     * Отрисовка цели квеста
     */
    private void renderObjective(DrawContext context, QuestObjective objective, int x, int y) {
        ItemStack displayStack = getObjectiveDisplayStack(objective);
        if (!displayStack.isEmpty()) {
            // Отрисовка иконки
            context.drawItem(displayStack, x, y);

            // Отрисовка количества поверх иконки
            if (objective.getAmount() > 1) {
                String amountText = String.valueOf(objective.getAmount());
                // Позиционируем текст в правом нижнем углу иконки
                int textX = x + 12; // Смещение вправо внутри иконки
                int textY = y + 9;  // Смещение вниз внутри иконки
                context.drawText(MinecraftClient.getInstance().textRenderer, amountText, textX, textY, 0xFFFFFF, true);
            }
        }
    }

    /**
     * Отрисовка награды квеста
     */
    private void renderReward(DrawContext context, QuestReward reward, int x, int y) {
        ItemStack displayStack = getRewardDisplayStack(reward);
        if (!displayStack.isEmpty()) {
            context.drawItem(displayStack, x, y);

            // Отрисовка количества с цветом редкости
            String amountText = String.valueOf(reward.getExperience());
            int color = getRewardColor(reward.getTier());
            int textX = x + 17 - MinecraftClient.getInstance().textRenderer.getWidth(amountText);
            context.drawText(MinecraftClient.getInstance().textRenderer, amountText, textX, y + 9, color, true);
        }
    }

    /**
     * Получает ItemStack для отображения цели квеста
     */
    private ItemStack getObjectiveDisplayStack(QuestObjective objective) {
        switch (objective.getType()) {
            case COLLECT:
                Identifier itemId = new Identifier(objective.getTarget());
                return new ItemStack(Registries.ITEM.get(itemId), objective.getAmount());
            case KILL:
                // Для убийства мобов показываем меч
                return new ItemStack(Items.IRON_SWORD);
            case CRAFT:
                Identifier craftItemId = new Identifier(objective.getTarget());
                return new ItemStack(Registries.ITEM.get(craftItemId));
            default:
                return new ItemStack(Items.BARRIER);
        }
    }

    /**
     * Получает ItemStack для отображения награды квеста
     */
    private ItemStack getRewardDisplayStack(QuestReward reward) {
        switch (reward.getType()) {
            case SKILL_POINT_TOKEN:
                switch (reward.getTier()) {
                    case 1:
                        return new ItemStack(Items.IRON_NUGGET);
                    case 2:
                        return new ItemStack(Items.GOLD_NUGGET);
                    case 3:
                        return new ItemStack(Items.DIAMOND);
                    default:
                        return new ItemStack(Items.EXPERIENCE_BOTTLE);
                }
            default:
                return new ItemStack(Items.EXPERIENCE_BOTTLE);
        }
    }

    /**
     * Получает цвет для отображения награды в зависимости от уровня
     */
    private int getRewardColor(int tier) {
        switch (tier) {
            case 1:
                return 0xFFFFFF; // Белый
            case 2:
                return 0xFFD700; // Золотой
            case 3:
                return 0x00FFFF; // Голубой
            default:
                return 0xFFFFFF;
        }
    }

    /**
     * Обработка клика по кнопке
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        parent.getHandler().setSelectedQuestIndex(questIndex);
        return true;
    }

    /**
     * Отрисовка подсказки при наведении
     */
    public void renderTooltip(DrawContext context, int mouseX, int mouseY) {
        Quest quest = getQuestData();
        if (quest != null) {
            QuestTooltipRenderer.renderQuestTooltip(context, quest, mouseX, mouseY);
        }
    }

    /**
     * Отрисовка подсказки для цели квеста при наведении на иконку
     */
    public void renderObjectiveTooltip(DrawContext context, QuestObjective objective, int mouseX, int mouseY) {
        QuestTooltipRenderer.renderObjectiveTooltip(context, objective, mouseX, mouseY);
    }

    /**
     * Отрисовка подсказки для награды квеста при наведении на иконку
     */
    public void renderRewardTooltip(DrawContext context, QuestReward reward, int mouseX, int mouseY) {
        QuestTooltipRenderer.renderRewardTooltip(context, reward, mouseX, mouseY);
    }
}