package io.github.apace100.origins.networking;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.skill.PlayerSkillComponent;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;

/**
 * Регистрация обработчиков сетевых пакетов от клиента к серверу
 */
public class ModPacketsC2S {
    
    public static final Identifier LEARN_SKILL = new Identifier(Origins.MODID, "learn_skill");
    
    public static void register() {
        // Регистрируем обработчик пакета для изучения навыка
        ServerPlayNetworking.registerGlobalReceiver(LEARN_SKILL, (server, player, handler, buf, responseSender) -> {
            // Получаем идентификатор навыка
            Identifier skillId = buf.readIdentifier();
            
            // Выполняем действие на сервере
            server.execute(() -> {
                // Получаем компонент навыков игрока
                PlayerSkillComponent playerSkills = PlayerSkillComponent.KEY.get(player);
                
                // Пытаемся изучить навык
                playerSkills.learnSkill(skillId.toString());
            });
        });
        // Обработчик выбора конкретной расы
        ServerPlayNetworking.registerGlobalReceiver(io.github.apace100.origins.networking.ModPackets.CHOOSE_ORIGIN, (server, player, handler, buf, responseSender) -> {
            String originIdStr = buf.readString();
            String layerIdStr = buf.readString();
            server.execute(() -> {
                try {
                    Identifier originId = Identifier.tryParse(originIdStr);
                    Identifier layerId = Identifier.tryParse(layerIdStr);
                    var originComponent = io.github.apace100.origins.registry.ModComponents.ORIGIN.get(player);
                    var layer = io.github.apace100.origins.origin.OriginLayers.getLayer(layerId);
                    var origin = io.github.apace100.origins.origin.OriginRegistry.get(originId);
                    originComponent.setOrigin(layer, origin);
                    originComponent.sync();
                    // Отправляем клиенту подтверждение выбора
                    net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, io.github.apace100.origins.networking.ModPackets.CONFIRM_ORIGIN, createConfirmOriginBuf(layer, origin));
                    // Можно добавить вызов OriginComponent.onChosen(player, originComponent.hadOriginBefore());
                } catch (Exception e) {
                    io.github.apace100.origins.Origins.LOGGER.error("Ошибка при обработке выбора расы: " + e.getMessage());
                }
            });
        });
        // Обработчик выбора случайной расы
        ServerPlayNetworking.registerGlobalReceiver(io.github.apace100.origins.networking.ModPackets.CHOOSE_RANDOM_ORIGIN, (server, player, handler, buf, responseSender) -> {
            String layerIdStr = buf.readString();
            server.execute(() -> {
                try {
                    Identifier layerId = Identifier.tryParse(layerIdStr);
                    var layer = io.github.apace100.origins.origin.OriginLayers.getLayer(layerId);
                    var possibleOrigins = layer.getRandomOrigins(player);
                    if (!possibleOrigins.isEmpty()) {
                        var randomOriginId = possibleOrigins.get(player.getRandom().nextInt(possibleOrigins.size()));
                        var origin = io.github.apace100.origins.origin.OriginRegistry.get(randomOriginId);
                        var originComponent = io.github.apace100.origins.registry.ModComponents.ORIGIN.get(player);
                        originComponent.setOrigin(layer, origin);
                        originComponent.sync();
                        // Отправляем клиенту подтверждение выбора
                        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, io.github.apace100.origins.networking.ModPackets.CONFIRM_ORIGIN, createConfirmOriginBuf(layer, origin));
                        // Можно добавить вызов OriginComponent.onChosen(player, originComponent.hadOriginBefore());
                    }
                } catch (Exception e) {
                    io.github.apace100.origins.Origins.LOGGER.error("Ошибка при обработке выбора случайной расы: " + e.getMessage());
                }
            });
        });
    }

    // Вспомогательный метод для создания буфера подтверждения
    private static net.minecraft.network.PacketByteBuf createConfirmOriginBuf(io.github.apace100.origins.origin.OriginLayer layer, io.github.apace100.origins.origin.Origin origin) {
        var buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
        buf.writeIdentifier(layer.getIdentifier());
        buf.writeIdentifier(origin.getIdentifier());
        return buf;
    }
}