package io.github.apace100.origins.networking;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.quest.BountyBoardScreenHandler;
import io.github.apace100.origins.quest.Quest;
import io.github.apace100.origins.quest.QuestTicketAcceptanceHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Сетевые пакеты для системы квестов
 */
public class QuestPackets {
    
    public static final Identifier ACCEPT_QUEST_PACKET = new Identifier(Origins.MODID, "accept_quest");
    public static final Identifier COMPLETE_QUEST_PACKET = new Identifier(Origins.MODID, "complete_quest");
    
    public static void registerServerPackets() {
        // Пакет для принятия квеста
        ServerPlayNetworking.registerGlobalReceiver(ACCEPT_QUEST_PACKET, (server, player, handler, buf, responseSender) -> {
            try {
                int questIndex = buf.readInt();
                
                server.execute(() -> {
                    try {
                        // Проверяем, что игрок все еще подключен
                        if (player == null || player.isDisconnected()) {
                            Origins.LOGGER.warn("Попытка принять квест от отключенного игрока");
                            return;
                        }
                        
                        // Проверяем корректность индекса
                        if (questIndex < 0) {
                            Origins.LOGGER.warn("Некорректный индекс квеста: {}", questIndex);
                            return;
                        }
                        
                        if (player.currentScreenHandler instanceof BountyBoardScreenHandler bountyHandler) {
                            Quest quest = bountyHandler.getQuest(questIndex);
                            if (quest != null) {
                                bountyHandler.acceptQuest(quest, player);
                                Origins.LOGGER.info("Игрок {} принял квест: {}", player.getName().getString(), quest.getId());
                            } else {
                                Origins.LOGGER.warn("Квест с индексом {} не найден", questIndex);
                            }
                        } else {
                            Origins.LOGGER.warn("Игрок {} не использует BountyBoardScreenHandler", player.getName().getString());
                        }
                    } catch (Exception e) {
                        Origins.LOGGER.error("Ошибка при принятии квеста: " + e.getMessage(), e);
                    }
                });
            } catch (Exception e) {
                Origins.LOGGER.error("Ошибка при чтении пакета принятия квеста: " + e.getMessage(), e);
            }
        });
        
        // Пакет для завершения квеста
        ServerPlayNetworking.registerGlobalReceiver(COMPLETE_QUEST_PACKET, (server, player, handler, buf, responseSender) -> {
            try {
                int questIndex = buf.readInt();
                
                server.execute(() -> {
                    try {
                        // Проверяем, что игрок все еще подключен
                        if (player == null || player.isDisconnected()) {
                            Origins.LOGGER.warn("Попытка завершить квест от отключенного игрока");
                            return;
                        }
                        
                        // Проверяем корректность индекса
                        if (questIndex < 0) {
                            Origins.LOGGER.warn("Некорректный индекс квеста: {}", questIndex);
                            return;
                        }
                        
                        if (player.currentScreenHandler instanceof BountyBoardScreenHandler bountyHandler) {
                            Quest quest = bountyHandler.getSelectedQuest();
                            if (quest != null) {
                                // Заглушка для завершения квеста - функциональность будет добавлена позже
                                Origins.LOGGER.info("Игрок {} пытается завершить квест: {}", player.getName().getString(), quest.getId());
                                player.sendMessage(Text.literal("Завершение квестов пока не реализовано"), false);
                            } else {
                                Origins.LOGGER.warn("Нет выбранного квеста для завершения");
                            }
                        } else {
                            Origins.LOGGER.warn("Игрок {} не использует BountyBoardScreenHandler", player.getName().getString());
                        }
                    } catch (Exception e) {
                        Origins.LOGGER.error("Ошибка при завершении квеста: " + e.getMessage(), e);
                    }
                });
            } catch (Exception e) {
                Origins.LOGGER.error("Ошибка при чтении пакета завершения квеста: " + e.getMessage(), e);
            }
        });
        
        Origins.LOGGER.info("Зарегистрированы серверные пакеты квестов");
    }
}