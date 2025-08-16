package io.github.apace100.origins.courier.client;

import io.github.apace100.origins.courier.CourierNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.function.Consumer;

/**
 * Экран ввода количества предметов
 */
public class QuantityInputScreen extends Screen {
    
    private final String itemName;
    private final int currentQuantity;
    private final Consumer<Integer> onQuantitySet;
    private final Screen parentScreen;
    
    private TextFieldWidget quantityField;
    private ButtonWidget confirmButton;
    private ButtonWidget cancelButton;
    
    private int animationTicks = 0;
    
    public QuantityInputScreen(String itemName, int currentQuantity, Consumer<Integer> onQuantitySet, Screen parentScreen) {
        super(Text.literal("Количество предметов"));
        this.itemName = itemName;
        this.currentQuantity = currentQuantity;
        this.onQuantitySet = onQuantitySet;
        this.parentScreen = parentScreen;
    }
    
    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int screenWidth = 300;
        int screenHeight = 150;
        
        int left = centerX - screenWidth / 2;
        int top = centerY - screenHeight / 2;
        
        // Поле ввода количества
        quantityField = new TextFieldWidget(this.textRenderer, left + 20, top + 60, screenWidth - 40, 20, Text.literal("Количество"));
        quantityField.setText(String.valueOf(currentQuantity));
        quantityField.setMaxLength(3); // Максимум 3 цифры (до 999)
        this.addSelectableChild(quantityField);
        this.setInitialFocus(quantityField);
        
        // Кнопки
        confirmButton = ButtonWidget.builder(Text.literal("Подтвердить"), button -> confirmQuantity())
            .dimensions(left + 20, top + screenHeight - 35, 100, 20)
            .build();
        this.addDrawableChild(confirmButton);
        
        cancelButton = ButtonWidget.builder(Text.literal("Отмена"), button -> cancel())
            .dimensions(left + screenWidth - 120, top + screenHeight - 35, 100, 20)
            .build();
        this.addDrawableChild(cancelButton);
    }
    
    @Override
    public void tick() {
        super.tick();
        animationTicks++;
        
        // Обновляем состояние кнопки подтверждения
        updateConfirmButton();
    }
    
    private void updateConfirmButton() {
        try {
            int quantity = Integer.parseInt(quantityField.getText());
            confirmButton.active = quantity > 0 && quantity <= CourierNetworking.MAX_ITEM_COUNT;
        } catch (NumberFormatException e) {
            confirmButton.active = false;
        }
    }
    
    private void confirmQuantity() {
        try {
            int quantity = Integer.parseInt(quantityField.getText());
            if (quantity > 0 && quantity <= CourierNetworking.MAX_ITEM_COUNT) {
                onQuantitySet.accept(quantity);
                this.client.setScreen(parentScreen);
            }
        } catch (NumberFormatException e) {
            // Игнорируем неверный ввод
        }
    }
    
    private void cancel() {
        this.client.setScreen(parentScreen);
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Рендерим фон
        context.fill(0, 0, this.width, this.height, 0x80000000);
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int screenWidth = 300;
        int screenHeight = 150;
        
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
        
        // Название предмета
        String itemText = "Предмет: " + itemName;
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(itemText), centerX, top + 25, CourierNetworking.COLOR_ACCENT);
        
        // Подсказка
        String hintText = "Введите количество (1-" + CourierNetworking.MAX_ITEM_COUNT + "):";
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(hintText), centerX, top + 45, CourierNetworking.COLOR_TEXT_SECONDARY);
        
        // Рендерим поле ввода
        quantityField.render(context, mouseX, mouseY, delta);
        
        // Рендерим кнопки
        super.render(context, mouseX, mouseY, delta);
        
        // Показываем ошибку при неверном вводе
        try {
            int quantity = Integer.parseInt(quantityField.getText());
            if (quantity <= 0 || quantity > CourierNetworking.MAX_ITEM_COUNT) {
                String errorText = "Количество должно быть от 1 до " + CourierNetworking.MAX_ITEM_COUNT;
                context.drawCenteredTextWithShadow(this.textRenderer, 
                    Text.literal(errorText).formatted(Formatting.RED), 
                    centerX, top + 90, CourierNetworking.COLOR_ERROR);
            }
        } catch (NumberFormatException e) {
            if (!quantityField.getText().isEmpty()) {
                context.drawCenteredTextWithShadow(this.textRenderer, 
                    Text.literal("Введите число").formatted(Formatting.RED), 
                    centerX, top + 90, CourierNetworking.COLOR_ERROR);
            }
        }
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Enter для подтверждения
        if (keyCode == 257) { // GLFW_KEY_ENTER
            confirmQuantity();
            return true;
        }
        
        // ESC для отмены
        if (keyCode == 256) { // GLFW_KEY_ESCAPE
            cancel();
            return true;
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean charTyped(char chr, int modifiers) {
        // Разрешаем только цифры
        if (Character.isDigit(chr)) {
            return super.charTyped(chr, modifiers);
        }
        return false;
    }
}