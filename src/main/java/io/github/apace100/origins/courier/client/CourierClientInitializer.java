package io.github.apace100.origins.courier.client;

import io.github.apace100.origins.Origins;
import net.fabricmc.api.ClientModInitializer;

/**
 * Клиентский инициализатор для системы заказов курьера
 */
public class CourierClientInitializer implements ClientModInitializer {
    
    @Override
    public void onInitializeClient() {
        // Регистрируем клиентские обработчики пакетов
        ClientOrderManager.registerClientHandlers();
        
        // Регистрируем клиентские команды
        CourierClientCommands.register();
        
            }
}