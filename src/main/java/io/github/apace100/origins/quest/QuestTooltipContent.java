package io.github.apace100.origins.quest;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages tooltip content for quests with fallback support
 */
public class QuestTooltipContent {
    private final List<Text> lines = new ArrayList<>();
    private final Map<String, Object> metadata = new HashMap<>();
    
    /**
     * Creates tooltip content from a quest with fallback handling
     */
    public static QuestTooltipContent fromQuest(Quest quest) {
        QuestTooltipContent content = new QuestTooltipContent();
        
        if (quest == null) {
            content.addLine(Text.literal("No quest data").formatted(Formatting.RED));
            return content;
        }
        
        // Add title with fallback
        String title = quest.getTitle();
        if (title != null && !title.isEmpty()) {
            content.addLine(Text.literal(title).formatted(Formatting.YELLOW));
        } else {
            content.addLine(Text.literal("Unnamed Quest").formatted(Formatting.GRAY));
        }
        
        // Add objective with fallback
        QuestObjective objective = quest.getObjective();
        if (objective != null) {
            content.addObjectiveLine(objective);
        } else {
            content.addLine(Text.literal("No objective").formatted(Formatting.GRAY));
        }
        
        // Add reward with fallback
        QuestReward reward = quest.getReward();
        if (reward != null) {
            content.addRewardLine(reward);
        } else {
            content.addLine(Text.literal("No reward").formatted(Formatting.GRAY));
        }
        
        // Add profession requirement
        String profession = quest.getPlayerClass();
        if (profession != null && !profession.isEmpty()) {
            content.addLine(Text.literal("Profession: " + profession).formatted(Formatting.AQUA));
        } else {
            content.addLine(Text.literal("Profession: Any").formatted(Formatting.GRAY));
        }
        
        return content;
    }
    
    /**
     * Adds a line to the tooltip
     */
    public void addLine(Text text) {
        lines.add(text);
    }
    
    /**
     * Adds an objective line with proper formatting
     */
    public void addObjectiveLine(QuestObjective objective) {
        if (objective == null) {
            addLine(Text.literal("No objective").formatted(Formatting.GRAY));
            return;
        }
        
        String objectiveText = getObjectiveDescription(objective);
        addLine(Text.literal("Objective: " + objectiveText).formatted(Formatting.GREEN));
        
        // Add amount if greater than 1
        if (objective.getAmount() > 1) {
            addLine(Text.literal("Amount: " + objective.getAmount()).formatted(Formatting.WHITE));
        }
    }
    
    /**
     * Adds a reward line with proper formatting
     */
    public void addRewardLine(QuestReward reward) {
        if (reward == null) {
            addLine(Text.literal("No reward").formatted(Formatting.GRAY));
            return;
        }
        
        String rewardText = getRewardDescription(reward);
        Formatting color = getRewardColor(reward.getTier());
        addLine(Text.literal("Reward: " + rewardText).formatted(color));
        
        // Add experience if available
        if (reward.getExperience() > 0) {
            addLine(Text.literal("Experience: " + reward.getExperience()).formatted(Formatting.LIGHT_PURPLE));
        }
    }
    
    /**
     * Gets the list of tooltip lines
     */
    public List<Text> getLines() {
        return new ArrayList<>(lines);
    }
    
    /**
     * Gets metadata associated with the tooltip
     */
    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata);
    }
    
    /**
     * Adds metadata to the tooltip
     */
    public void addMetadata(String key, Object value) {
        metadata.put(key, value);
    }
    
    /**
     * Gets a description for the quest objective
     */
    private String getObjectiveDescription(QuestObjective objective) {
        if (objective == null) return "Unknown";
        
        String target = objective.getTarget();
        if (target == null || target.isEmpty()) {
            target = "Unknown Item";
        } else {
            // Clean up the target name
            target = cleanItemName(target);
        }
        
        switch (objective.getType()) {
            case COLLECT:
                return "Collect " + target;
            case KILL:
                return "Kill " + target;
            case CRAFT:
                return "Craft " + target;
            default:
                return target;
        }
    }
    
    /**
     * Gets a description for the quest reward
     */
    private String getRewardDescription(QuestReward reward) {
        if (reward == null) return "Unknown";
        
        switch (reward.getType()) {
            case SKILL_POINT_TOKEN:
                return "Skill Point Token (Tier " + reward.getTier() + ")";
            default:
                return "Unknown Reward";
        }
    }
    
    /**
     * Gets the color formatting for a reward based on its tier
     */
    private Formatting getRewardColor(int tier) {
        switch (tier) {
            case 1:
                return Formatting.WHITE;
            case 2:
                return Formatting.YELLOW;
            case 3:
                return Formatting.AQUA;
            default:
                return Formatting.GRAY;
        }
    }
    
    /**
     * Cleans up item names for display
     */
    private String cleanItemName(String itemId) {
        if (itemId == null) return "Unknown";
        
        // Remove namespace prefix
        String name = itemId.replace("minecraft:", "");
        
        // Split by underscores and capitalize
        String[] parts = name.split("_");
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) result.append(" ");
            if (parts[i].length() > 0) {
                result.append(parts[i].substring(0, 1).toUpperCase());
                if (parts[i].length() > 1) {
                    result.append(parts[i].substring(1).toLowerCase());
                }
            }
        }
        
        return result.toString();
    }
}