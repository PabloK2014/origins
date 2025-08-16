package io.github.apace100.origins.courier.client;

import io.github.apace100.origins.courier.CourierNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/**
 * Экран создания заказа для курьера
 */
public class CreateOrderScreen extends Screen {
    
    // Статические поля для сохранения состояния между экранами
    private static String savedDescription = "";
    private static final ItemStack[] savedRequestItems = new ItemStack[CourierNetworking.MAX_ITEMS_PER_CATEGORY];
    private static final int[] savedRequestCounts = new int[CourierNetworking.MAX_ITEMS_PER_CATEGORY];
    private static final ItemStack[] savedRewardItems = new ItemStack[CourierNetworking.MAX_ITEMS_PER_CATEGORY];
    private static final int[] savedRewardCounts = new int[CourierNetworking.MAX_ITEMS_PER_CATEGORY];
    private static int savedExperienceReward = 0;
    
    static {
        // Инициализируем массивы
        for (int i = 0; i < CourierNetworking.MAX_ITEMS_PER_CATEGORY; i++) {
            savedRequestItems[i] = ItemStack.EMPTY;
            savedRequestCounts[i] = 1;
            savedRewardItems[i] = ItemStack.EMPTY;
            savedRewardCounts[i] = 1;
        }
    }
    
    private TextFieldWidget descriptionField;
    private TextFieldWidget experienceField;
    private final List<ItemSlot> requestSlots = new ArrayList<>();
    private final List<ItemSlot> rewardSlots = new ArrayList<>();
    private ButtonWidget createButton;
    private ButtonWidget cancelButton;
    
    private int animationTicks = 0;
    
    public CreateOrderScreen() {
        super(Text.literal("Создать заказ"));
    }
    
    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int screenWidth = CourierNetworking.CREATE_ORDER_WIDTH;
        int screenHeight = CourierNetworking.CREATE_ORDER_HEIGHT + 30; // Увеличиваем высоту
        
        int left = centerX - screenWidth / 2;
        int top = centerY - screenHeight / 2;
        
        // Поле описания
        descriptionField = new TextFieldWidget(this.textRenderer, left + 20, top + 30, screenWidth - 40, 20, Text.literal("Описание"));
        descriptionField.setPlaceholder(Text.literal("Опишите ваш заказ...").formatted(Formatting.GRAY));
        descriptionField.setMaxLength(CourierNetworking.MAX_DESCRIPTION_LENGTH);
        descriptionField.setText(savedDescription);
        this.addSelectableChild(descriptionField);
        
        // Поле опыта\n        experienceField = new TextFieldWidget(this.textRenderer, left + 20, top + 55, 100, 20, Text.literal(\"Опыт для курьера\"));\n        experienceField.setPlaceholder(Text.literal(\"0\").formatted(Formatting.GRAY));\n        experienceField.setMaxLength(5); // Максимум 5 символов (до 99999)\n        experienceField.setText(String.valueOf(savedExperienceReward));\n        // Разрешаем только цифры\n        experienceField.setTextPredicate(text -> text.matches(\"\\\\d*\"));\n        this.addSelectableChild(experienceField);\n        \n        // Добавляем визуальный индикатор для поля опыта\n        // Это будет сделано в методе render
        
        // Создаем слоты для предметов запроса
        requestSlots.clear();
        for (int i = 0; i < CourierNetworking.MAX_ITEMS_PER_CATEGORY; i++) {
            int row = i / 5;
            int col = i % 5;
            int x = left + 20 + col * 45;
            int y = top + 95 + row * 45;
            
            ItemSlot slot = new ItemSlot(x, y, i, true);
            // Восстанавливаем сохраненные предметы
            slot.itemStack = savedRequestItems[i].copy();
            slot.count = savedRequestCounts[i];
            requestSlots.add(slot);
        }
        
        // Создаем слоты для предметов награды
        rewardSlots.clear();
        for (int i = 0; i < CourierNetworking.MAX_ITEMS_PER_CATEGORY; i++) {
            int row = i / 5;
            int col = i % 5;
            int x = left + 270 + col * 45;
            int y = top + 95 + row * 45;
            
            ItemSlot slot = new ItemSlot(x, y, i, false);
            // Восстанавливаем сохраненные предметы
            slot.itemStack = savedRewardItems[i].copy();
            slot.count = savedRewardCounts[i];
            rewardSlots.add(slot);
        }
        
        // Кнопки
        createButton = ButtonWidget.builder(Text.literal("Создать заказ"), button -> createOrder())
            .dimensions(left + 20, top + screenHeight - 40, 150, 25)
            .build();
        this.addDrawableChild(createButton);
        
        cancelButton = ButtonWidget.builder(Text.literal("Отмена"), button -> this.close())
            .dimensions(left + screenWidth - 120, top + screenHeight - 40, 100, 25)
            .build();
        this.addDrawableChild(cancelButton);
        
        // Устанавливаем фокус на поле описания
        this.setInitialFocus(descriptionField);
    }
    
    @Override
    public void tick() {
        super.tick();
        animationTicks++;
        
        // Сохраняем описание
        if (descriptionField != null) {
            savedDescription = descriptionField.getText();
        }
        
        // Сохраняем опыт
        if (experienceField != null) {
            try {
                int exp = Integer.parseInt(experienceField.getText());
                // Ограничиваем значение
                exp = Math.max(0, Math.min(exp, 10000));
                savedExperienceReward = exp;
                // Обновляем текст в поле, если значение было изменено
                if (exp != Integer.parseInt(experienceField.getText())) {
                    experienceField.setText(String.valueOf(exp));
                }
            } catch (NumberFormatException e) {
                savedExperienceReward = 0;
            }
        }
        
        // Обновляем состояние кнопки создания
        updateCreateButtonState();
    }
    
    /**
     * Обновляет состояние кнопки создания заказа
     */
    private void updateCreateButtonState() {
        boolean hasDescription = descriptionField.getText().trim().length() > 0;
        boolean hasRequestItems = requestSlots.stream().anyMatch(slot -> !slot.itemStack.isEmpty());
        boolean hasRewardItems = rewardSlots.stream().anyMatch(slot -> !slot.itemStack.isEmpty());
        boolean hasValidExperience = true;
        
        // Проверяем корректность ввода опыта
        try {
            int exp = Integer.parseInt(experienceField.getText());
            hasValidExperience = exp >= 0 && exp <= 10000;
        } catch (NumberFormatException e) {
            hasValidExperience = false;
        }
        
        createButton.active = hasDescription && hasRequestItems && hasRewardItems && hasValidExperience;
    }
    
    /**
     * Очищает сохраненное состояние
     */
    private static void clearSavedState() {
        savedDescription = "";
        savedExperienceReward = 0;
        for (int i = 0; i < CourierNetworking.MAX_ITEMS_PER_CATEGORY; i++) {
            savedRequestItems[i] = ItemStack.EMPTY;
            savedRequestCounts[i] = 1;
            savedRewardItems[i] = ItemStack.EMPTY;
            savedRewardCounts[i] = 1;
        }
    }
    
    /**
     * Создает заказ и отправляет на сервер
     */
    private void createOrder() {
        try {
            PacketByteBuf buf = PacketByteBufs.create();
            
            // Описание
            buf.writeString(descriptionField.getText().trim());
            
            // Опыт для курьера
            int experienceReward = 0;
            try {
                experienceReward = Integer.parseInt(experienceField.getText());
            } catch (NumberFormatException e) {
                experienceReward = 0;
            }
            // Ограничиваем значение
            experienceReward = Math.max(0, Math.min(experienceReward, 10000));
            buf.writeInt(experienceReward);
            
            // Предметы запроса
            List<ItemStack> requestItems = new ArrayList<>();
            for (ItemSlot slot : requestSlots) {
                if (!slot.itemStack.isEmpty()) {
                    ItemStack item = slot.itemStack.copy();
                    item.setCount(slot.count);
                    requestItems.add(item);
                }
            }
            buf.writeInt(requestItems.size());
            for (ItemStack item : requestItems) {
                buf.writeItemStack(item);
            }
            
            // Предметы награды
            List<ItemStack> rewardItems = new ArrayList<>();
            for (ItemSlot slot : rewardSlots) {
                if (!slot.itemStack.isEmpty()) {
                    ItemStack item = slot.itemStack.copy();
                    item.setCount(slot.count);
                    rewardItems.add(item);
                }
            }
            buf.writeInt(rewardItems.size());
            for (ItemStack item : rewardItems) {
                buf.writeItemStack(item);
            }
            
            // Отправляем пакет
            ClientPlayNetworking.send(CourierNetworking.CREATE_ORDER, buf);
            
            // Очищаем сохраненное состояние
            clearSavedState();
            
            // Закрываем экран
            this.close();
            
        } catch (Exception e) {
            // Показываем ошибку пользователю
            if (this.client != null && this.client.player != null) {
                this.client.player.sendMessage(Text.literal("Ошибка при создании заказа: " + e.getMessage())
                    .formatted(Formatting.RED), false);
            }
        }
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Рендерим фон
        context.fill(0, 0, this.width, this.height, 0x80000000);
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int screenWidth = CourierNetworking.CREATE_ORDER_WIDTH;
        int screenHeight = CourierNetworking.CREATE_ORDER_HEIGHT + 30; // Увеличиваем высоту
        
        int left = centerX - screenWidth / 2;
        int top = centerY - screenHeight / 2;
        
        // Рендерим фон окна с анимацией
        float animationProgress = Math.min(1.0f, animationTicks / 10.0f);
        int alpha = (int)(255 * animationProgress);
        int backgroundColor = (alpha << 24) | (CourierNetworking.COLOR_BACKGROUND & 0xFFFFFF);
        
        context.fill(left, top, left + screenWidth, top + screenHeight, backgroundColor);
        context.drawBorder(left, top, screenWidth, screenHeight, CourierNetworking.COLOR_PRIMARY);
        
        // Заголовок
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, top + 8, CourierNetworking.COLOR_TEXT);
        
        // Подзаголовки секций
        context.drawText(this.textRenderer, Text.literal("Описание:"), left + 20, top + 18, CourierNetworking.COLOR_TEXT, false);
        context.drawText(this.textRenderer, Text.literal("Опыт для курьера (0-10000):").formatted(Formatting.BOLD), left + 20, top + 43, CourierNetworking.COLOR_TEXT, false);
        context.drawText(this.textRenderer, Text.literal("Запрашиваемые предметы:"), left + 20, top + 80, CourierNetworking.COLOR_ACCENT, false);
        context.drawText(this.textRenderer, Text.literal("Награда:"), left + 270, top + 80, CourierNetworking.COLOR_WARNING, false);
        
        // Рендерим поле описания
        descriptionField.render(context, mouseX, mouseY, delta);
        
        // Рендерим поле опыта с визуальным индикатором
        if (experienceField != null) {
            // Рисуем рамку вокруг поля опыта
            int expFieldX = left + 20;
            int expFieldY = top + 55;
            context.drawBorder(expFieldX - 2, expFieldY - 2, 104, 24, CourierNetworking.COLOR_WARNING);
            experienceField.render(context, mouseX, mouseY, delta);
        }
        
        // Рендерим слоты предметов
        for (ItemSlot slot : requestSlots) {
            slot.render(context, mouseX, mouseY, delta);
        }
        for (ItemSlot slot : rewardSlots) {
            slot.render(context, mouseX, mouseY, delta);
        }
        
        // Рендерим кнопки
        super.render(context, mouseX, mouseY, delta);
        
        // Подсказки
        if (!createButton.active) {
            String hint = "Заполните описание, добавьте предметы запроса и награды";
            context.drawCenteredTextWithShadow(this.textRenderer, 
                Text.literal(hint).formatted(Formatting.GRAY), 
                centerX, top + screenHeight - 65, CourierNetworking.COLOR_TEXT_SECONDARY);
        }
        
        // Подсказка для поля опыта
        String experienceHint = "Опыт, который получит курьер за выполнение заказа (0-10000)";
        context.drawText(this.textRenderer, 
            Text.literal(experienceHint).formatted(Formatting.GRAY), 
            left + 130, top + 60, CourierNetworking.COLOR_TEXT_SECONDARY, false);
        
        // Показываем ошибку при неверном вводе опыта
        try {
            int exp = Integer.parseInt(experienceField.getText());
            if (exp < 0 || exp > 10000) {
                String errorText = "Опыт должен быть от 0 до 10000!";
                context.drawText(this.textRenderer, 
                    Text.literal(errorText).formatted(Formatting.RED, Formatting.BOLD), 
                    left + 20, top + 75, CourierNetworking.COLOR_ERROR, false);
            }
        } catch (NumberFormatException e) {
            if (!experienceField.getText().isEmpty()) {
                context.drawText(this.textRenderer, 
                    Text.literal("Введите число!").formatted(Formatting.RED, Formatting.BOLD), 
                    left + 20, top + 75, CourierNetworking.COLOR_ERROR, false);
            }
        }
        
        // Рендерим подсказки для слотов
        for (ItemSlot slot : requestSlots) {
            if (slot.isHovered(mouseX, mouseY)) {
                slot.renderTooltip(context, mouseX, mouseY);
            }
        }
        for (ItemSlot slot : rewardSlots) {
            if (slot.isHovered(mouseX, mouseY)) {
                slot.renderTooltip(context, mouseX, mouseY);
            }
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Проверяем клики по слотам
        for (ItemSlot slot : requestSlots) {
            if (slot.isHovered((int)mouseX, (int)mouseY)) {
                slot.onClick(button);
                return true;
            }
        }
        for (ItemSlot slot : rewardSlots) {
            if (slot.isHovered((int)mouseX, (int)mouseY)) {
                slot.onClick(button);
                return true;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // ESC для закрытия
        if (keyCode == 256) { // GLFW_KEY_ESCAPE
            this.close();
            return true;
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    /**
     * Класс для представления слота предмета
     */
    private class ItemSlot {
        private final int x, y;
        private final int index;
        private final boolean isRequest;
        private ItemStack itemStack = ItemStack.EMPTY;
        private int count = 1;
        
        public ItemSlot(int x, int y, int index, boolean isRequest) {
            this.x = x;
            this.y = y;
            this.index = index;
            this.isRequest = isRequest;
        }
        
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            // Фон слота
            int slotColor = isHovered(mouseX, mouseY) ? 0x80FFFFFF : 0x80000000;
            context.fill(x, y, x + 40, y + 40, slotColor);
            context.drawBorder(x, y, 40, 40, CourierNetworking.COLOR_PRIMARY);
            
            if (itemStack.isEmpty()) {
                // Рендерим кнопку "+"
                context.drawCenteredTextWithShadow(CreateOrderScreen.this.textRenderer, 
                    Text.literal("+"), x + 20, y + 16, CourierNetworking.COLOR_ACCENT);
            } else {
                // Рендерим предмет
                try {
                    context.drawItem(itemStack, x + 12, y + 12);
                    
                    // Рендерим количество
                    if (count > 1) {
                        String countText = String.valueOf(count);
                        context.drawText(CreateOrderScreen.this.textRenderer, countText, 
                            x + 32 - CreateOrderScreen.this.textRenderer.getWidth(countText), 
                            y + 32, CourierNetworking.COLOR_TEXT, true);
                    }
                } catch (Exception e) {
                    System.out.println("DEBUG: Error rendering item in slot " + index + ": " + e.getMessage());
                    // Если не можем отрендерить предмет, показываем его название
                    String itemName = itemStack.getName().getString();
                    if (itemName.length() > 6) {
                        itemName = itemName.substring(0, 6) + "...";
                    }
                    context.drawText(CreateOrderScreen.this.textRenderer, itemName, 
                        x + 2, y + 16, CourierNetworking.COLOR_TEXT, false);
                }
            }
        }
        
        public void renderTooltip(DrawContext context, int mouseX, int mouseY) {
            if (itemStack.isEmpty()) {
                List<Text> tooltip = List.of(Text.literal("Нажмите, чтобы выбрать предмет"));
                context.drawTooltip(CreateOrderScreen.this.textRenderer, tooltip, mouseX, mouseY);
            } else {
                List<Text> tooltip = new ArrayList<>();
                tooltip.add(itemStack.getName());
                tooltip.add(Text.literal("Количество: " + count).formatted(Formatting.GRAY));
                tooltip.add(Text.literal("ЛКМ - изменить количество").formatted(Formatting.GRAY));
                tooltip.add(Text.literal("ПКМ - изменить количество").formatted(Formatting.GRAY));
                tooltip.add(Text.literal("Shift+ПКМ - удалить").formatted(Formatting.GRAY));
                context.drawTooltip(CreateOrderScreen.this.textRenderer, tooltip, mouseX, mouseY);
            }
        }
        
        public boolean isHovered(int mouseX, int mouseY) {
            return mouseX >= x && mouseX < x + 40 && mouseY >= y && mouseY < y + 40;
        }
        
        public void onClick(int button) {
            if (button == 0) { // Левая кнопка
                if (itemStack.isEmpty()) {
                    // Открываем экран выбора предмета с ссылкой на родительский экран
                    MinecraftClient.getInstance().setScreen(new ItemPickerScreen(this::setItem, CreateOrderScreen.this));
                } else {
                    // Если предмет уже выбран, открываем диалог изменения количества
                    String itemName = itemStack.getName().getString();
                    MinecraftClient.getInstance().setScreen(new QuantityInputScreen(itemName, count, this::setQuantity, CreateOrderScreen.this));
                }
            } else if (button == 1) { // Правая кнопка
                if (!itemStack.isEmpty()) {
                    if (Screen.hasShiftDown()) {
                        // Удаляем предмет
                        itemStack = ItemStack.EMPTY;
                        count = 1;
                        
                        // Сохраняем изменения в статических полях
                        if (isRequest) {
                            savedRequestItems[index] = this.itemStack.copy();
                            savedRequestCounts[index] = this.count;
                        } else {
                            savedRewardItems[index] = this.itemStack.copy();
                            savedRewardCounts[index] = this.count;
                        }
                    } else {
                        // Открываем диалог изменения количества
                        String itemName = itemStack.getName().getString();
                        MinecraftClient.getInstance().setScreen(new QuantityInputScreen(itemName, count, this::setQuantity, CreateOrderScreen.this));
                    }
                }
            }
        }
        
        public void setItem(ItemStack item) {
            System.out.println("DEBUG: Setting item: " + item.getName().getString() + " in slot " + index + " (isRequest: " + isRequest + ")");
            this.itemStack = item.copy();
            this.count = 1;
            
            // Сохраняем состояние в статических полях
            if (isRequest) {
                savedRequestItems[index] = this.itemStack.copy();
                savedRequestCounts[index] = this.count;
            } else {
                savedRewardItems[index] = this.itemStack.copy();
                savedRewardCounts[index] = this.count;
            }
            
            // Принудительно обновляем состояние кнопки создания
            CreateOrderScreen.this.updateCreateButtonState();
            
            System.out.println("DEBUG: Item set successfully. ItemStack empty: " + this.itemStack.isEmpty());
            System.out.println("DEBUG: Item name: " + this.itemStack.getName().getString());
            System.out.println("DEBUG: Item object: " + this.itemStack);
            System.out.println("DEBUG: Item count: " + this.itemStack.getCount());
        }
        
        public void setQuantity(int newQuantity) {
            this.count = Math.max(1, Math.min(newQuantity, CourierNetworking.MAX_ITEM_COUNT));
            
            // Сохраняем изменения в статических полях
            if (isRequest) {
                savedRequestCounts[index] = this.count;
            } else {
                savedRewardCounts[index] = this.count;
            }
            
            System.out.println("DEBUG: Quantity set to " + this.count + " for slot " + index);
        }
    }
}