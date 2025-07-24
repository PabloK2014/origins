package io.github.apace100.origins.networking;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.quest.BountyBoardScreenHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
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
            int questIndex = buf.readInt();
            
            server.execute(() -> {
                if (player.currentScreenHandler instanceof BountyBoardScreenHandler bountyHandler) {
                    bountyHandler.acceptQuest(questIndex);
                }
            });
        });
        
        // Пакет для завершения квеста
        ServerPlayNetworking.registerGlobalReceiver(COMPLETE_QUEST_PACKET, (server, player, handler, buf, responseSender) -> {
            int questIndex = buf.readInt();
            
            server.execute(() -> {
                if (player.currentScreenHandler instanceof BountyBoardScreenHandler bountyHandler) {
                    var quest = bountyHandler.getSelectedQuest();
                    if (quest != null) {
                        bountyHandler.completeQuest(quest, player);
                    }
                }
            });
        });
    }
}