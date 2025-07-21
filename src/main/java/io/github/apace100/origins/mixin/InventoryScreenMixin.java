package io.github.apace100.origins.mixin;

import io.github.apace100.origins.client.gui.OriginProgressionScreen;
import io.github.apace100.origins.client.gui.SkillTreeScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Миксин для добавления кнопок Origins в экран инвентаря
 */
@Mixin(InventoryScreen.class)
public class InventoryScreenMixin {

    @Inject(method = "init", at = @At("TAIL"))
    private void addOriginsButtons(CallbackInfo ci) {
        InventoryScreen screen = (InventoryScreen) (Object) this;
        
        // Используем рефлексию для получения доступа к защищенным полям
        try {
            // Получаем поля x, y и client через рефлексию из правильных классов
            Class<?> handledScreenClass = screen.getClass().getSuperclass(); // HandledScreen
            Class<?> screenClass = handledScreenClass.getSuperclass(); // Screen
            
            java.lang.reflect.Field xField = handledScreenClass.getDeclaredField("x");
            java.lang.reflect.Field yField = handledScreenClass.getDeclaredField("y");
            java.lang.reflect.Field clientField = screenClass.getDeclaredField("client");
            
            xField.setAccessible(true);
            yField.setAccessible(true);
            clientField.setAccessible(true);
            
            int x = xField.getInt(screen);
            int y = yField.getInt(screen);
            MinecraftClient client = (MinecraftClient) clientField.get(screen);
            
            // Кнопка "Прогрессия"
            ButtonWidget progressionButton = ButtonWidget.builder(
                Text.literal("Прогрессия"), 
                button -> {
                    if (client != null) {
                        client.setScreen(new OriginProgressionScreen());
                    }
                }
            ).dimensions(x - 80, y + 10, 75, 20).build();
            
            // Кнопка "Навыки" - временно отключаем, чтобы избежать краша
            ButtonWidget skillsButton = ButtonWidget.builder(
                Text.literal("Навыки"), 
                button -> {
                    if (client != null && client.player != null) {
                        // Временно показываем сообщение вместо открытия экрана
                        client.player.sendMessage(Text.literal("Система навыков в разработке"), false);
                    }
                }
            ).dimensions(x - 80, y + 35, 75, 20).build();
            
            // Используем рефлексию для вызова addDrawableChild
            java.lang.reflect.Method addDrawableChildMethod = screenClass.getDeclaredMethod("addDrawableChild", Object.class);
            addDrawableChildMethod.setAccessible(true);
            
            addDrawableChildMethod.invoke(screen, progressionButton);
            addDrawableChildMethod.invoke(screen, skillsButton);
            
        } catch (Exception e) {
            // Выводим ошибку для отладки
            System.err.println("Ошибка при добавлении кнопок Origins в инвентарь: " + e.getMessage());
            e.printStackTrace();
        }
    }
}