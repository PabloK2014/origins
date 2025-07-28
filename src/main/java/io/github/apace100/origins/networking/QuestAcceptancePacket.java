package io.github.apace100.origins.networking;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.quest.BountyBoardBlockEntity;
import io.github.apace100.origins.quest.Quest;
import io.github.apace100.origins.quest.QuestTicketAcceptanceHandler;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Пакет для принятия квеста через ЛКМ
 */
public class QuestAcceptancePacket {
    public static final Identifier ACCEPT_QUEST_ID = new Identifier(Origins.MODID, "accept_quest");
    
    /**
     * Создает пакет для отправки на сервер
     */
    public static PacketByteBuf createPacket(String questId, BlockPos boardPos) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(questId);
        buf.writeBlockPos(boardPos);
        return buf;
    }
    
    /**
     * Обрабатывает пакет на сервере
     */
    public static void handleAcceptQuest(MinecraftServer server, ServerPlayerEntity player, 
                                       net.minecraft.server.network.ServerPlayNetworkHandler handler, 
                                       PacketByteBuf buf, PacketSender responseSender) {
        try {
            String questId = buf.readString();
            BlockPos boardPos = buf.readBlockPos();
            
            // Выполняем на главном потоке сервера
            server.execute(() -> {
                try {
                    // Получаем BlockEntity доски объявлений
                    if (!(player.getWorld().getBlockEntity(boardPos) instanceof BountyBoardBlockEntity board)) {
                        Origins.LOGGER.warn("Игрок {} пытается принять квест с несуществующей доски в {}", 
                            player.getName().getString(), boardPos);
                        return;
                    }
                    
                    // Ищем квест на доске
                    Quest quest = findQuestOnBoard(board, questId);
                    if (quest == null) {
                        Origins.LOGGER.warn("Квест {} не найден на доске в {}", questId, boardPos);
                        return;
                    }
                    
                    // Принимаем квест
                    QuestTicketAcceptanceHandler acceptanceHandler = QuestTicketAcceptanceHandler.getInstance();
                    boolean accepted = acceptanceHandler.acceptQuestFromBoard(player, quest, board);
                    
                    if (accepted) {
                        Origins.LOGGER.info("Игрок {} успешно принял квест {} через пакет", 
                            player.getName().getString(), questId);
                    } else {
                        Origins.LOGGER.warn("Не удалось принять квест {} для игрока {}", 
                            questId, player.getName().getString());
                    }
                    
                } catch (Exception e) {
                    Origins.LOGGER.error("Ошибка при обработке пакета принятия квеста: {}", e.getMessage());
                }
            });
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при чтении пакета принятия квеста: {}", e.getMessage());
        }
    }
    
    /**
     * Ищет квест на доске по ID
     */
    private static Quest findQuestOnBoard(BountyBoardBlockEntity board, String questId) {
        try {
            // Получаем доступные квесты с доски
            for (Quest quest : board.getAvailableQuests()) {
                if (quest != null && quest.getId().equals(questId)) {
                    return quest;
                }
            }
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при поиске квеста на доске: {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Регистрирует обработчик пакета на сервере
     */
    public static void registerServerHandler() {
        ServerPlayNetworking.registerGlobalReceiver(ACCEPT_QUEST_ID, QuestAcceptancePacket::handleAcceptQuest);
        Origins.LOGGER.info("Зарегистрирован обработчик пакета принятия квестов");
    }
}