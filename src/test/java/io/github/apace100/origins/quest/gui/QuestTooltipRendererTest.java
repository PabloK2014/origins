package io.github.apace100.origins.quest.gui;

import io.github.apace100.origins.quest.Quest;
import io.github.apace100.origins.quest.QuestObjective;
import io.github.apace100.origins.quest.QuestReward;
import io.github.apace100.origins.quest.ActiveQuest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для QuestTooltipRenderer
 */
public class QuestTooltipRendererTest {
    
    @Test
    public void testQuestCreation() {
        QuestObjective objective = new QuestObjective(
            QuestObjective.ObjectiveType.COLLECT,
            "minecraft:dirt",
            10
        );
        
        QuestReward reward = new QuestReward(
            QuestReward.RewardType.SKILL_POINT_TOKEN,
            1,
            500
        );
        
        Quest quest = new Quest(
            "test_quest",
            "warrior",
            1,
            "Тестовый квест",
            "Соберите 10 блоков земли",
            objective,
            60,
            reward
        );
        
        assertNotNull(quest);
        assertEquals("test_quest", quest.getId());
        assertEquals("warrior", quest.getPlayerClass());
        assertEquals(1, quest.getLevel());
        assertEquals("Тестовый квест", quest.getTitle());
        assertEquals(60, quest.getTimeLimit());
        assertTrue(quest.isValid());
    }
    
    @Test
    public void testActiveQuestCreation() {
        QuestObjective objective = new QuestObjective(
            QuestObjective.ObjectiveType.KILL,
            "minecraft:zombie",
            5
        );
        
        QuestReward reward = new QuestReward(
            QuestReward.RewardType.EXPERIENCE,
            2,
            1000
        );
        
        Quest quest = new Quest(
            "kill_zombies",
            "warrior",
            2,
            "Убить зомби",
            "Убейте 5 зомби",
            objective,
            30,
            reward
        );
        
        ActiveQuest activeQuest = new ActiveQuest(quest, System.currentTimeMillis());
        
        assertNotNull(activeQuest);
        assertEquals(quest, activeQuest.getQuest());
        assertEquals(0.0f, activeQuest.getProgressPercentage());
        assertFalse(activeQuest.isCompleted());
        assertFalse(activeQuest.isExpired());
    }
    
    @Test
    public void testObjectiveProgress() {
        QuestObjective objective = new QuestObjective(
            QuestObjective.ObjectiveType.CRAFT,
            "minecraft:wooden_sword",
            3
        );
        
        assertEquals(0, objective.getProgress());
        assertFalse(objective.isCompleted());
        assertEquals(0.0f, objective.getProgressPercentage());
        
        objective.updateProgress(1);
        assertEquals(1, objective.getProgress());
        assertFalse(objective.isCompleted());
        assertEquals(1.0f/3.0f, objective.getProgressPercentage(), 0.01f);
        
        objective.updateProgress(2);
        assertEquals(3, objective.getProgress());
        assertTrue(objective.isCompleted());
        assertEquals(1.0f, objective.getProgressPercentage());
    }
}