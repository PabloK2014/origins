package io.github.apace100.origins.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import io.github.apace100.origins.quest.QuestGenerator;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class TestQuestLoadingCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(CommandManager.literal("test_quest_loading")
            .requires(source -> source.hasPermissionLevel(2))
            .executes(TestQuestLoadingCommand::testLoading)
        );
    }
    
    private static int testLoading(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        try {
            // Загружаем квесты из JSON файлов
            QuestGenerator.loadQuestsFromResources(source.getServer().getResourceManager());
            
            int totalQuests = QuestGenerator.getTotalQuestCount();
            source.sendFeedback(() -> Text.literal("Загружено квестов: " + totalQuests), false);
            
            // Показываем квесты по профессиям
            String[] professions = {"cook", "warrior", "courier", "brewer", "blacksmith", "miner"};
            for (String profession : professions) {
                int count = QuestGenerator.getQuestCountForProfession(profession);
                source.sendFeedback(() -> Text.literal(profession + ": " + count + " квестов"), false);
            }
            
            return totalQuests;
        } catch (Exception e) {
            source.sendError(Text.literal("Ошибка загрузки квестов: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }
}