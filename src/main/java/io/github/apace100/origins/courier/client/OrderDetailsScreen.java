package io.github.apace100.origins.courier.client;

import io.github.apace100.origins.courier.ClientOrder;
import io.github.apace100.origins.courier.CourierNetworking;
import io.github.apace100.origins.courier.Order;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/**
 * Экран детального просмотра заказа
 */
public class OrderDetailsScreen extends Screen {
    
    private final ClientOrder order;
    private ButtonWidget acceptButton;
    private ButtonWidget declineButton;
    private ButtonWidget completeButton;
    private ButtonWidget cancelButton;
    private ButtonWidget deleteButton;
    private ButtonWidget backButton;
    
    private int animationTicks = 0;
    private boolean isProcessing = false;
    
    public OrderDetailsScreen(ClientOrder order) {
        super(Text.literal("Заказ — " + order.ownerName));
        this.order = order;
    }
    
    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int screenWidth = CourierNetworking.ORDER_DETAILS_WIDTH;
        int screenHeight = CourierNetworking.ORDER_DETAILS_HEIGHT;
        
        int left = centerX - screenWidth / 2;
        int top = centerY - screenHeight / 2;
        
        // Кнопки действий (показываем в зависимости от статуса заказа)
        setupActionButtons(left, top, screenWidth, screenHeight);
        
        // Кнопка "Назад" - размещаем справа
        backButton = ButtonWidget.builder(Text.literal("Назад"), 
            button -> MinecraftClient.getInstance().setScreen(new OrdersListScreen()))
            .dimensions(left + screenWidth - 90, top + screenHeight - 35, 80, 25)
            .build();
        this.addDrawableChild(backButton);
    }
    
    /**
     * Настраивает кнопки действий в зависимости от статуса заказа
     */
    private void setupActionButtons(int left, int top, int screenWidth, int screenHeight) {
        int buttonY = top + screenHeight - 35;
        int buttonWidth = 100;
        int buttonSpacing = 110;
        
        if (order.status == Order.Status.OPEN) {
            // Заказ открыт - показываем кнопки принять/отклонить
            acceptButton = ButtonWidget.builder(Text.literal("Принять"), button -> acceptOrder())
                .dimensions(left + 20, buttonY, buttonWidth, 25)
                .build();
            this.addDrawableChild(acceptButton);
            
            declineButton = ButtonWidget.builder(Text.literal("Отклонить"), button -> declineOrder())
                .dimensions(left + 20 + buttonSpacing, buttonY, buttonWidth, 25)
                .build();
            this.addDrawableChild(declineButton);
            
        } else if (order.status == Order.Status.ACCEPTED || order.status == Order.Status.IN_PROGRESS) {
            // Заказ принят - показываем кнопку завершить
            completeButton = ButtonWidget.builder(Text.literal("Завершить"), button -> completeOrder())
                .dimensions(left + 20, buttonY, buttonWidth, 25)
                .build();
            this.addDrawableChild(completeButton);
            
            // Кнопка отмены (если это наш заказ)
            if (isOurOrder()) {
                cancelButton = ButtonWidget.builder(Text.literal("Отменить"), button -> cancelOrder())
                    .dimensions(left + 20 + buttonSpacing, buttonY, buttonWidth, 25)
                    .build();
                this.addDrawableChild(cancelButton);
            }
        }
        
        // Кнопка удаления (для владельца заказа или завершенных заказов)
        if (isOwnerOfOrder() || order.status == Order.Status.COMPLETED || 
            order.status == Order.Status.DECLINED || order.status == Order.Status.CANCELLED) {
            
            int deleteButtonX = left + screenWidth - 220; // Размещаем слева от кнопки "Назад"
            deleteButton = ButtonWidget.builder(Text.literal("Удалить"), button -> deleteOrder())
                .dimensions(deleteButtonX, buttonY, buttonWidth, 25)
                .build();
            this.addDrawableChild(deleteButton);
        }
    }
    
    /**
     * Проверяет, является ли заказ нашим (принятым нами)
     */
    private boolean isOurOrder() {
        if (this.client == null || this.client.player == null) return false;
        String playerName = this.client.player.getName().getString();
        return playerName.equals(order.acceptedByName);
    }
    
    /**
     * Проверяет, является ли игрок владельцем заказа
     */
    private boolean isOwnerOfOrder() {
        if (this.client == null || this.client.player == null) return false;
        String playerName = this.client.player.getName().getString();
        return playerName.equals(order.ownerName);
    }
    
    @Override
    public void tick() {
        super.tick();
        animationTicks++;
        
        // Обновляем состояние кнопок
        updateButtonStates();
    }
    
    /**
     * Обновляет состояние кнопок
     */
    private void updateButtonStates() {
        if (acceptButton != null) {
            acceptButton.active = !isProcessing;
        }
        if (declineButton != null) {
            declineButton.active = !isProcessing;
        }
        if (completeButton != null) {
            completeButton.active = !isProcessing;
        }
        if (cancelButton != null) {
            cancelButton.active = !isProcessing;
        }
        if (deleteButton != null) {
            deleteButton.active = !isProcessing;
        }
    }
    
    /**
     * Принимает заказ
     */
    private void acceptOrder() {
        if (isProcessing) return;
        
        isProcessing = true;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(order.id);
        ClientPlayNetworking.send(CourierNetworking.ACCEPT_ORDER, buf);
        
        // Закрываем экран после отправки
        this.close();
    }
    
    /**
     * Отклоняет заказ
     */
    private void declineOrder() {
        if (isProcessing) return;
        
        isProcessing = true;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(order.id);
        ClientPlayNetworking.send(CourierNetworking.DECLINE_ORDER, buf);
        
        // Закрываем экран после отправки
        this.close();
    }
    
    /**
     * Завершает заказ
     */
    private void completeOrder() {
        if (isProcessing) return;
        
        // Проверяем, есть ли у игрока необходимые предметы
        if (!hasRequiredItemsInInventory()) {
            if (this.client != null && this.client.player != null) {
                this.client.player.sendMessage(Text.literal("У вас нет необходимых предметов для выполнения заказа!")
                    .formatted(Formatting.RED), false);
            }
            return;
        }
        
        isProcessing = true;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(order.id);
        ClientPlayNetworking.send(CourierNetworking.COMPLETE_ORDER, buf);
        
        // Закрываем экран после отправки
        this.close();
    }
    
    /**
     * Проверяет, есть ли у игрока необходимые предметы в инвентаре
     */
    private boolean hasRequiredItemsInInventory() {
        if (this.client == null || this.client.player == null) return false;
        
        for (ItemStack requiredItem : order.requestItems) {
            if (requiredItem.isEmpty()) continue;
            
            int requiredCount = requiredItem.getCount();
            int foundCount = 0;
            
            // Проверяем все слоты инвентаря
            for (int i = 0; i < this.client.player.getInventory().size(); i++) {
                ItemStack slotItem = this.client.player.getInventory().getStack(i);
                if (ItemStack.canCombine(requiredItem, slotItem)) {
                    foundCount += slotItem.getCount();
                    if (foundCount >= requiredCount) {
                        break;
                    }
                }
            }
            
            if (foundCount < requiredCount) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Отменяет заказ
     */
    private void cancelOrder() {
        if (isProcessing) return;
        
        isProcessing = true;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(order.id);
        ClientPlayNetworking.send(CourierNetworking.CANCEL_ORDER, buf);
        
        // Закрываем экран после отправки
        this.close();
    }
    
    /**
     * Удаляет заказ
     */
    private void deleteOrder() {
        if (isProcessing) return;
        
        isProcessing = true;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(order.id);
        ClientPlayNetworking.send(CourierNetworking.DELETE_ORDER, buf);
        
        // Закрываем экран после отправки
        this.close();
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Рендерим фон
        context.fill(0, 0, this.width, this.height, 0x80000000);
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int screenWidth = CourierNetworking.ORDER_DETAILS_WIDTH;
        int screenHeight = CourierNetworking.ORDER_DETAILS_HEIGHT;
        
        int left = centerX - screenWidth / 2;
        int top = centerY - screenHeight / 2;
        
        // Рендерим фон окна с анимацией
        float animationProgress = Math.min(1.0f, animationTicks / 10.0f);
        int alpha = (int)(255 * animationProgress);
        int backgroundColor = (alpha << 24) | (CourierNetworking.COLOR_BACKGROUND & 0xFFFFFF);
        
        context.fill(left, top, left + screenWidth, top + screenHeight, backgroundColor);
        context.drawBorder(left, top, screenWidth, screenHeight, CourierNetworking.COLOR_PRIMARY);
        
        // Заголовок
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, top + 35, CourierNetworking.COLOR_TEXT);
        
        // Информация о заказе
        int infoY = top + 55;
        
        // Заказчик
        context.drawText(this.textRenderer, 
            Text.literal("Заказчик: ").formatted(Formatting.YELLOW).append(Text.literal(order.ownerName)), 
            left + 20, infoY, CourierNetworking.COLOR_TEXT, false);
        infoY += 15;
        
        // Статус
        context.drawText(this.textRenderer, 
            Text.literal("Статус: ").formatted(Formatting.YELLOW).append(Text.literal(order.getLocalizedStatus())), 
            left + 20, infoY, CourierNetworking.COLOR_TEXT, false);
        infoY += 15;
        
        // Время создания
        String timeText = CourierNetworking.formatOrderTime(order.createdTime);
        context.drawText(this.textRenderer, 
            Text.literal("Создан: ").formatted(Formatting.YELLOW).append(Text.literal(timeText)), 
            left + 20, infoY, CourierNetworking.COLOR_TEXT, false);
        infoY += 15;
        
        // Принят кем
        if (order.acceptedByName != null) {
            context.drawText(this.textRenderer, 
                Text.literal("Принят: ").formatted(Formatting.YELLOW).append(Text.literal(order.acceptedByName)), 
                left + 20, infoY, CourierNetworking.COLOR_TEXT, false);
            infoY += 15;
        }
        
        // Опыт для курьера
        if (order.experienceReward > 0) {
            context.drawText(this.textRenderer, 
                Text.literal("Опыт для курьера: ").formatted(Formatting.YELLOW).append(Text.literal(String.valueOf(order.experienceReward))), 
                left + 20, infoY, CourierNetworking.COLOR_TEXT, false);
            infoY += 15;
        }
        
        infoY += 10;
        
        // Описание
        context.drawText(this.textRenderer, Text.literal("Описание:"), left + 20, infoY, CourierNetworking.COLOR_ACCENT, false);
        infoY += 15;
        
        // Разбиваем описание на строки
        List<String> descriptionLines = wrapText(order.description, screenWidth - 40);
        for (String line : descriptionLines) {
            context.drawText(this.textRenderer, line, left + 20, infoY, CourierNetworking.COLOR_TEXT, false);
            infoY += 12;
        }
        
        infoY += 10;
        
        // Запрашиваемые предметы
        context.drawText(this.textRenderer, Text.literal("Запрашиваемые предметы:"), 
            left + 20, infoY, CourierNetworking.COLOR_WARNING, false);
        infoY += 20;
        
        // Показываем проверку инвентаря только если заказ принят нами
        boolean showInventoryCheck = isOurOrder() && (order.status == Order.Status.ACCEPTED || order.status == Order.Status.IN_PROGRESS);
        infoY = renderItemList(context, order.requestItems, left + 20, infoY, screenWidth - 40, showInventoryCheck);
        
        infoY += 15;
        
        // Награда
        context.drawText(this.textRenderer, Text.literal("Награда:"), 
            left + 20, infoY, CourierNetworking.COLOR_ACCENT, false);
        infoY += 20;
        
        renderItemList(context, order.rewardItems, left + 20, infoY, screenWidth - 40);
        
        // Рендерим кнопки
        super.render(context, mouseX, mouseY, delta);
        
        // Индикатор обработки
        if (isProcessing) {
            String processingText = "Обработка...";
            int processingX = centerX - this.textRenderer.getWidth(processingText) / 2;
            int processingY = top + screenHeight - 60;
            context.fill(processingX - 5, processingY - 5, 
                        processingX + this.textRenderer.getWidth(processingText) + 5, 
                        processingY + 15, 0x80000000);
            context.drawText(this.textRenderer, processingText, processingX, processingY, 
                           CourierNetworking.COLOR_ACCENT, false);
        }
    }
    
    /**
     * Рендерит список предметов
     */
    private int renderItemList(DrawContext context, List<ItemStack> items, int x, int y, int maxWidth) {
        return renderItemList(context, items, x, y, maxWidth, false);
    }
    
    /**
     * Рендерит список предметов с опциональной проверкой наличия в инвентаре
     */
    private int renderItemList(DrawContext context, List<ItemStack> items, int x, int y, int maxWidth, boolean checkInventory) {
        int currentX = x;
        int currentY = y;
        int itemSize = 20;
        int itemSpacing = 25;
        
        for (ItemStack item : items) {
            if (!item.isEmpty()) {
                // Проверяем, помещается ли предмет в текущую строку
                if (currentX + itemSize > x + maxWidth) {
                    currentX = x;
                    currentY += itemSize + 5;
                }
                
                // Проверяем наличие предмета в инвентаре (только для запрашиваемых предметов)
                boolean hasEnough = true;
                if (checkInventory && this.client != null && this.client.player != null) {
                    int requiredCount = item.getCount();
                    int foundCount = 0;
                    
                    for (int i = 0; i < this.client.player.getInventory().size(); i++) {
                        ItemStack slotItem = this.client.player.getInventory().getStack(i);
                        if (ItemStack.canCombine(item, slotItem)) {
                            foundCount += slotItem.getCount();
                        }
                    }
                    
                    hasEnough = foundCount >= requiredCount;
                }
                
                // Рендерим фон для предмета (красный если не хватает)
                if (checkInventory && !hasEnough) {
                    context.fill(currentX - 1, currentY - 1, currentX + itemSize + 1, currentY + itemSize + 1, 0x80FF0000);
                }
                
                // Рендерим предмет
                context.drawItem(item, currentX, currentY);
                
                // Рендерим количество
                if (item.getCount() > 1) {
                    String countText = String.valueOf(item.getCount());
                    int textColor = (checkInventory && !hasEnough) ? CourierNetworking.COLOR_ERROR : CourierNetworking.COLOR_TEXT;
                    context.drawText(this.textRenderer, countText, 
                        currentX + itemSize - this.textRenderer.getWidth(countText), 
                        currentY + itemSize - 8, textColor, true);
                }
                
                // Добавляем галочку или крестик для запрашиваемых предметов
                if (checkInventory) {
                    String indicator = hasEnough ? "✓" : "✗";
                    int indicatorColor = hasEnough ? 0xFF00FF00 : 0xFFFF0000;
                    context.drawText(this.textRenderer, indicator, 
                        currentX + itemSize + 2, currentY, indicatorColor, false);
                }
                
                currentX += itemSpacing;
            }
        }
        
        return currentY + itemSize + 5;
    }
    
    /**
     * Разбивает текст на строки
     */
    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        
        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            if (this.textRenderer.getWidth(testLine) <= maxWidth) {
                currentLine = new StringBuilder(testLine);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    lines.add(word);
                }
            }
        }
        
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        
        return lines;
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // ESC для возврата к списку
        if (keyCode == 256) { // GLFW_KEY_ESCAPE
            MinecraftClient.getInstance().setScreen(new OrdersListScreen());
            return true;
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}