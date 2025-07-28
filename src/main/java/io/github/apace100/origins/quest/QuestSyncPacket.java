package io.github.apace100.origins.quest;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Packet for synchronizing quest list between server and client
 */
public class QuestSyncPacket {
    public static final Identifier ID = new Identifier("origins", "quest_sync");
    
    private final List<Quest> quests;
    
    public QuestSyncPacket(List<Quest> quests) {
        this.quests = quests != null ? new ArrayList<>(quests) : new ArrayList<>();
    }
    
    /**
     * Creates a packet from the quest list
     */
    public static QuestSyncPacket create(List<Quest> quests) {
        return new QuestSyncPacket(quests);
    }
    
    /**
     * Writes the packet data to the buffer
     */
    public PacketByteBuf toPacketByteBuf() {
        PacketByteBuf buf = PacketByteBufs.create();
        
        // Write number of quests
        buf.writeInt(quests.size());
        
        // Write each quest
        for (Quest quest : quests) {
            writeQuest(buf, quest);
        }
        
        return buf;
    }
    
    /**
     * Reads the packet data from the buffer
     */
    public static QuestSyncPacket fromPacketByteBuf(PacketByteBuf buf) {
        List<Quest> quests = new ArrayList<>();
        
        // Read number of quests
        int questCount = buf.readInt();
        
        // Read each quest
        for (int i = 0; i < questCount; i++) {
            Quest quest = readQuest(buf);
            if (quest != null) {
                quests.add(quest);
            }
        }
        
        return new QuestSyncPacket(quests);
    }
    
    /**
     * Writes a quest to the buffer
     */
    private static void writeQuest(PacketByteBuf buf, Quest quest) {
        if (quest == null) {
            buf.writeBoolean(false);
            return;
        }
        
        buf.writeBoolean(true);
        buf.writeString(quest.getId());
        buf.writeString(quest.getTitle());
        buf.writeString(quest.getDescription());
        buf.writeString(quest.getPlayerClass());
        buf.writeInt(quest.getLevel());
        buf.writeInt(quest.getTimeLimit());
        
        // Write objective
        QuestObjective objective = quest.getObjective();
        if (objective != null) {
            buf.writeBoolean(true);
            buf.writeString(objective.getType().name());
            buf.writeString(objective.getTarget());
            buf.writeInt(objective.getAmount());
            buf.writeInt(objective.getProgress());
            buf.writeBoolean(objective.isCompleted());
        } else {
            buf.writeBoolean(false);
        }
        
        // Write reward
        QuestReward reward = quest.getReward();
        if (reward != null) {
            buf.writeBoolean(true);
            buf.writeString(reward.getType().name());
            buf.writeInt(reward.getTier());
            buf.writeInt(reward.getExperience());
        } else {
            buf.writeBoolean(false);
        }
    }
    
    /**
     * Reads a quest from the buffer
     */
    private static Quest readQuest(PacketByteBuf buf) {
        if (!buf.readBoolean()) {
            return null;
        }
        
        try {
            String id = buf.readString();
            String title = buf.readString();
            String description = buf.readString();
            String playerClass = buf.readString();
            int level = buf.readInt();
            int timeLimit = buf.readInt();
            
            // Read objective
            QuestObjective objective = null;
            if (buf.readBoolean()) {
                QuestObjective.ObjectiveType type = QuestObjective.ObjectiveType.valueOf(buf.readString());
                String target = buf.readString();
                int amount = buf.readInt();
                int progress = buf.readInt();
                boolean completed = buf.readBoolean();
                
                objective = new QuestObjective(type, target, amount);
                objective.setProgress(progress);
                if (completed) {
                    objective.setCompleted(true);
                }
            }
            
            // Read reward
            QuestReward reward = null;
            if (buf.readBoolean()) {
                QuestReward.RewardType type = QuestReward.RewardType.valueOf(buf.readString());
                int tier = buf.readInt();
                int experience = buf.readInt();
                
                reward = new QuestReward(type, tier, experience);
            }
            
            return new Quest(id, playerClass, level, title, description, objective, timeLimit, reward);
            
        } catch (Exception e) {
            io.github.apace100.origins.Origins.LOGGER.error("Error reading quest from packet: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Gets the quest list from the packet
     */
    public List<Quest> getQuests() {
        return new ArrayList<>(quests);
    }
}