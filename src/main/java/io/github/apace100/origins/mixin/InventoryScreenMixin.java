package io.github.apace100.origins.mixin;

import io.github.apace100.origins.courier.client.ClientOrderManager;
import io.github.apace100.origins.courier.client.CreateOrderScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public class InventoryScreenMixin {
    
    @Inject(method = "init", at = @At("TAIL"))
    private void addCourierOrdersButton(CallbackInfo ci) {
        InventoryScreen screen = (InventoryScreen) (Object) this;
        MinecraftClient client = MinecraftClient.getInstance();
        
        if (client.player == null) return;
        
        // Получаем размеры экрана
        int screenWidth = screen.width;
        int screenHeight = screen.height;
        
        // Определяем позицию кнопок (ниже инвентаря, в строку)
        int buttonX = (screenWidth - 176) / 2; // Слева от инвентаря
        int buttonY = (screenHeight + 166) / 2 + 10; // Ниже инвентаря
        
        // Добавляем кнопку для курьеров (список заказов)
        ButtonWidget ordersButton = ButtonWidget.builder(Text.literal("📦"), button -> {
            ClientOrderManager.getInstance().openOrdersScreen();
        })
        .dimensions(buttonX, buttonY, 20, 20)
        .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.translatable("gui.origins.inventory.orders_button")))
        .build();
        
        // Добавляем кнопку для создания заказа
        ButtonWidget createOrderButton = ButtonWidget.builder(Text.literal("✉"), button -> {
            client.setScreen(new CreateOrderScreen());
        })
        .dimensions(buttonX + 25, buttonY, 20, 20) // Смещаем вправо
        .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.translatable("gui.origins.inventory.create_order_button")))
        .build();
        
        // Добавляем кнопку навыков
        ButtonWidget skillsButton = ButtonWidget.builder(Text.literal("⚡"), button -> {
            client.setScreen(new io.github.apace100.origins.client.gui.LevelZSkillScreen());
        })
        .dimensions(buttonX + 50, buttonY, 20, 20) // Смещаем еще правее
        .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.translatable("gui.origins.inventory.skills_button")))
        .build();
        
        // Добавляем кнопки через рефлексию
        try {
            java.lang.reflect.Method addDrawableChild = net.minecraft.client.gui.screen.Screen.class.getDeclaredMethod("addDrawableChild", net.minecraft.client.gui.Element.class);
            addDrawableChild.setAccessible(true);
            addDrawableChild.invoke(screen, ordersButton);
            addDrawableChild.invoke(screen, createOrderButton);
            addDrawableChild.invoke(screen, skillsButton);
        } catch (Exception e) {
            // Fallback: добавляем напрямую в коллекции
            try {
                java.lang.reflect.Field childrenField = net.minecraft.client.gui.screen.Screen.class.getDeclaredField("children");
                childrenField.setAccessible(true);
                @SuppressWarnings("unchecked")
                java.util.List<net.minecraft.client.gui.Element> children = (java.util.List<net.minecraft.client.gui.Element>) childrenField.get(screen);
                children.add(ordersButton);
                children.add(createOrderButton);
                children.add(skillsButton);
                
                java.lang.reflect.Field drawablesField = net.minecraft.client.gui.screen.Screen.class.getDeclaredField("drawables");
                drawablesField.setAccessible(true);
                @SuppressWarnings("unchecked")
                java.util.List<net.minecraft.client.gui.Drawable> drawables = (java.util.List<net.minecraft.client.gui.Drawable>) drawablesField.get(screen);
                drawables.add(ordersButton);
                drawables.add(createOrderButton);
                drawables.add(skillsButton);
                
                java.lang.reflect.Field selectablesField = net.minecraft.client.gui.screen.Screen.class.getDeclaredField("selectables");
                selectablesField.setAccessible(true);
                @SuppressWarnings("unchecked")
                java.util.List<net.minecraft.client.gui.Selectable> selectables = (java.util.List<net.minecraft.client.gui.Selectable>) selectablesField.get(screen);
                selectables.add(ordersButton);
                selectables.add(createOrderButton);
                selectables.add(skillsButton);
            } catch (Exception ex) {
                System.err.println("Failed to add courier buttons to inventory screen: " + ex.getMessage());
            }
        }
    }
}