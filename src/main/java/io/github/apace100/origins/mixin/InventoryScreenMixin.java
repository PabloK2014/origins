package io.github.apace100.origins.mixin;

import io.github.apace100.origins.courier.client.ClientOrderManager;
import io.github.apace100.origins.courier.client.CreateOrderScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InventoryScreen.class)
public class InventoryScreenMixin {
    
    private int ordersButtonX, ordersButtonY;
    private int createOrderButtonX, createOrderButtonY;
    private int skillsButtonX, skillsButtonY;
    private boolean isHoveringOrders, isHoveringCreateOrder, isHoveringSkills;
    
    @Inject(method = "init", at = @At("TAIL"))
    private void addCourierOrdersButton(CallbackInfo ci) {
        InventoryScreen screen = (InventoryScreen) (Object) this;
        MinecraftClient client = MinecraftClient.getInstance();
        
        if (client.player == null) return;
        
        // Получаем размеры экрана
        int screenWidth = screen.width;
        int screenHeight = screen.height;
        
        // Рассчитываем позицию инвентаря и размещаем кнопки под ним
        int inventoryX = (screenWidth - 176) / 2; // Центрируем по X
        int inventoryY = (screenHeight - 166) / 2; // Центрируем по Y
        
        // Устанавливаем координаты для "кнопок" (областей клика)
        this.ordersButtonX = inventoryX + 50; // Слева от кнопки "назад"
        this.ordersButtonY = inventoryY + 166 + 10; // Ниже инвентаря
        
        this.createOrderButtonX = this.ordersButtonX + 25;
        this.createOrderButtonY = this.ordersButtonY;
        
        this.skillsButtonX = this.ordersButtonX + 50;
        this.skillsButtonY = this.ordersButtonY;
    }
    
    @Inject(method = "render", at = @At("TAIL"))
    private void renderOriginsButtons(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        InventoryScreen screen = (InventoryScreen) (Object) this;
        
        // Рендерим визуальные кнопки поверх интерфейса
        renderButton(context, this.ordersButtonX, this.ordersButtonY, "📦", mouseX, mouseY);
        renderButton(context, this.createOrderButtonX, this.createOrderButtonY, "✉", mouseX, mouseY);
        renderButton(context, this.skillsButtonX, this.skillsButtonY, "⚡", mouseX, mouseY);
    }
    
    private void renderButton(DrawContext context, int x, int y, String symbol, int mouseX, int mouseY) {
        boolean isHovering = mouseX >= x && mouseX <= x + 20 && mouseY >= y && mouseY <= y + 20;
        
        // Рисуем фон кнопки
        int color = isHovering ? 0xFFAAAAAA : 0xFF888888; // Изменение цвета при наведении
        context.fill(x, y, x + 20, y + 20, color);
        
        // Рисуем границу вручную (как в стандартном GUI)
        context.fill(x, y, x + 20, y + 1, 0xFF000000); // верх
        context.fill(x, y + 19, x + 20, y + 20, 0xFF000000); // низ
        context.fill(x, y, x + 1, y + 20, 0xFF000000); // лево
        context.fill(x + 19, y, x + 20, y + 20, 0xFF000000); // право
        
        // Рисуем символ
        context.drawText(MinecraftClient.getInstance().textRenderer, symbol, x + (20 - MinecraftClient.getInstance().textRenderer.getWidth(symbol)) / 2, y + (20 - 8) / 2, 0xFF000000, false);
        
        // Обновляем статус наведения для соответствующей кнопки
        if (x == this.ordersButtonX && y == this.ordersButtonY) {
            this.isHoveringOrders = isHovering;
        } else if (x == this.createOrderButtonX && y == this.createOrderButtonY) {
            this.isHoveringCreateOrder = isHovering;
        } else if (x == this.skillsButtonX && y == this.skillsButtonY) {
            this.isHoveringSkills = isHovering;
        }
    }
    
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onButtonClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button != 0) return; // Только левая кнопка мыши
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        // Проверяем, попал ли клик в область "кнопки заказов"
        if (mouseX >= this.ordersButtonX && mouseX <= this.ordersButtonX + 20 &&
            mouseY >= this.ordersButtonY && mouseY <= this.ordersButtonY + 20) {
            ClientOrderManager.getInstance().openOrdersScreen();
            cir.setReturnValue(true); // Обработка клика выполнена
        }
        // Проверяем, попал ли клик в область "кнопки создания заказа"
        else if (mouseX >= this.createOrderButtonX && mouseX <= this.createOrderButtonX + 20 &&
                 mouseY >= this.createOrderButtonY && mouseY <= this.createOrderButtonY + 20) {
            client.setScreen(new CreateOrderScreen());
            cir.setReturnValue(true); // Обработка клика выполнена
        }
        // Проверяем, попал ли клик в область "кнопки навыков"
        else if (mouseX >= this.skillsButtonX && mouseX <= this.skillsButtonX + 20 &&
                 mouseY >= this.skillsButtonY && mouseY <= this.skillsButtonY + 20) {
            client.setScreen(new io.github.apace100.origins.client.gui.LevelZSkillScreen());
            cir.setReturnValue(true); // Обработка клика выполнена
        }
    }
}