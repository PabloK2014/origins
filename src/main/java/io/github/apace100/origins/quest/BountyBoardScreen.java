package io.github.apace100.origins.quest;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.apace100.origins.Origins;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class BountyBoardScreen extends HandledScreen<BountyBoardScreenHandler> {
    // Используем оригинальную текстуру из Bountiful
    private static final Identifier TEXTURE = new Identifier(Origins.MODID, "textures/gui/new_new_board.png");
    private static final int BACKGROUND_WIDTH = 348;
    private static final int BACKGROUND_HEIGHT = 165;
    
    // Константы для позиционирования элементов (как в оригинальном Bountiful)
    private static final int QUEST_LIST_X = 5;
    private static final int QUEST_LIST_Y = 18;
    private static final int QUEST_LIST_WIDTH = 160;
    private static final int QUEST_LIST_HEIGHT = 126;
    private static final int QUEST_ITEM_HEIGHT = 20;
    
    private static final int INVENTORY_X = 179;
    private static final int INVENTORY_Y = 16;

    public BountyBoardScreen(BountyBoardScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = BACKGROUND_WIDTH;
        this.backgroundHeight = BACKGROUND_HEIGHT;
        this.playerInventoryTitleY = INVENTORY_Y + 54;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        
        // Рисуем основной фон доски объявлений (большой интерфейс как в Bountiful)
        context.drawTexture(TEXTURE, x, y, 0, 0, BACKGROUND_WIDTH, BACKGROUND_HEIGHT);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        // Рисуем заголовок (по центру, как в оригинальном Bountiful)
        Text titleText = Text.translatable("gui.origins.bounty_board.title");
        int titleX = (backgroundWidth - textRenderer.getWidth(titleText)) / 2 - 53;
        context.drawText(textRenderer, titleText, titleX, 6, 0xEADAB5, false);
        
        // Рисуем доступные задания в левой части
        var availableQuests = handler.getAvailableQuests();
        if (availableQuests.isEmpty()) {
            Text noQuestsText = Text.translatable("gui.origins.bounty_board.no_quests");
            int noQuestsX = 85 - textRenderer.getWidth(noQuestsText) / 2;
            context.drawText(textRenderer, noQuestsText, noQuestsX, 78, 0xEADAB5, false);
        } else {
            int questY = QUEST_LIST_Y;
            int questIndex = 0;
            for (BountyQuest quest : availableQuests) {
                if (questIndex >= 7) break; // Максимум 7 квестов на экране (как в Bountiful)
                
                // Подсвечиваем выбранный квест
                if (questIndex == handler.getSelectedQuestIndex()) {
                    context.fill(QUEST_LIST_X - 2, questY - 2, QUEST_LIST_X + QUEST_LIST_WIDTH + 2, questY + QUEST_ITEM_HEIGHT - 2, 0x80FFFF00);
                }
                
                // Рисуем информацию о задании
                String professionName = getProfessionDisplayName(quest.getProfession());
                String questText = String.format("%s: %dx %s", 
                    professionName,
                    quest.getRequiredAmount(),
                    quest.getRequiredItem().getName().getString()
                );
                
                // Обрезаем текст если он слишком длинный
                if (textRenderer.getWidth(questText) > QUEST_LIST_WIDTH - 10) {
                    questText = textRenderer.trimToWidth(questText, QUEST_LIST_WIDTH - 10) + "...";
                }
                
                context.drawText(textRenderer, questText, QUEST_LIST_X, questY, 0x404040, false);
                
                // Рисуем награду
                String rewardText = String.format("Награда: %d опыта", quest.getRewardExp());
                context.drawText(textRenderer, rewardText, QUEST_LIST_X, questY + 10, 0x008000, false);
                
                questY += QUEST_ITEM_HEIGHT;
                questIndex++;
            }
        }
        
        // Рисуем заголовок инвентаря игрока (справа)
        context.drawText(textRenderer, playerInventoryTitle, INVENTORY_X, playerInventoryTitleY, 0x404040, false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void init() {
        super.init();
        titleY = 6;
        
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        
        // Добавляем кнопку "Взять заказ"
        this.addDrawableChild(ButtonWidget.builder(
            Text.translatable("gui.origins.bounty_board.take"),
            button -> takeQuest()
        ).dimensions(x + 8, y + 100, 70, 20).build());
        
        // Добавляем кнопку "Завершить заказ"
        this.addDrawableChild(ButtonWidget.builder(
            Text.translatable("gui.origins.bounty_board.complete"),
            button -> completeQuest()
        ).dimensions(x + 88, y + 100, 80, 20).build());
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Проверяем клик по квестам для их выбора
        var availableQuests = handler.getAvailableQuests();
        if (!availableQuests.isEmpty()) {
            int questY = 20;
            int questIndex = 0;
            for (BountyQuest quest : availableQuests) {
                if (questIndex >= 4) break;
                
                int relativeX = (int) mouseX - (width - backgroundWidth) / 2;
                int relativeY = (int) mouseY - (height - backgroundHeight) / 2;
                
                if (relativeX >= 6 && relativeX <= backgroundWidth - 6 && 
                    relativeY >= questY - 2 && relativeY <= questY + 20) {
                    handler.setSelectedQuestIndex(questIndex);
                    return true;
                }
                
                questY += 22;
                questIndex++;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    private void takeQuest() {
        BountyQuest selectedQuest = handler.getSelectedQuest();
        if (selectedQuest != null && client != null) {
            // Показываем сообщение игроку
            client.player.sendMessage(
                Text.translatable("gui.origins.bounty_board.quest_accepted"),
                true
            );
            // TODO: Отправить пакет на сервер для принятия квеста
        }
    }
    
    private void completeQuest() {
        BountyQuest selectedQuest = handler.getSelectedQuest();
        if (selectedQuest != null && client != null && client.player != null) {
            // Проверяем, есть ли у игрока необходимые предметы
            if (client.player.getInventory().getMainHandStack().getItem() == selectedQuest.getRequiredItem() &&
                client.player.getInventory().getMainHandStack().getCount() >= selectedQuest.getRequiredAmount()) {
                
                client.player.sendMessage(
                    Text.translatable("gui.origins.bounty_board.quest_completed"),
                    true
                );
                // TODO: Отправить пакет на сервер для завершения квеста
            } else {
                client.player.sendMessage(
                    Text.translatable("gui.origins.bounty_board.insufficient_items"),
                    true
                );
            }
        }
    }
    
    /**
     * Получает локализованное название профессии
     */
    private String getProfessionDisplayName(String professionId) {
        return switch (professionId) {
            case "blacksmith" -> "🔨 Кузнец";
            case "brewer" -> "🍺 Пивовар";
            case "cook" -> "👨‍🍳 Повар";
            case "courier" -> "📦 Курьер";
            case "warrior" -> "⚔️ Воин";
            case "miner" -> "⛏️ Шахтер";
            default -> professionId;
        };
    }
} 