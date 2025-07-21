package io.github.apace100.origins.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.apace100.origins.profession.Profession;
import io.github.apace100.origins.profession.ProfessionComponent;
import io.github.apace100.origins.profession.ProfessionProgress;
import io.github.apace100.origins.profession.ProfessionRegistry;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * Команды для управления системой прогрессии
 */
public class ProgressionCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("progression")
            .requires(source -> source.hasPermissionLevel(2))
            
            // /progression info [игрок]
            .then(literal("info")
                .executes(context -> executeInfo(context, null))
                .then(argument("player", EntityArgumentType.player())
                    .executes(context -> executeInfo(context, EntityArgumentType.getPlayer(context, "player")))
                )
            )
            
            // /progression add <игрок> <опыт>
            .then(literal("add")
                .then(argument("player", EntityArgumentType.player())
                    .then(argument("experience", IntegerArgumentType.integer(1))
                        .executes(ProgressionCommand::executeAddExperience)
                    )
                )
            )
            
            // /progression set <игрок> <уровень>
            .then(literal("set")
                .then(argument("player", EntityArgumentType.player())
                    .then(argument("level", IntegerArgumentType.integer(1))
                        .executes(ProgressionCommand::executeSetLevel)
                    )
                )
            )
            
            // /progression reset <игрок>
            .then(literal("reset")
                .then(argument("player", EntityArgumentType.player())
                    .executes(ProgressionCommand::executeReset)
                )
            )
        );
    }
    
    public static int executeInfo(CommandContext<ServerCommandSource> context, ServerPlayerEntity targetPlayer) throws CommandSyntaxException {
        ServerPlayerEntity player = targetPlayer != null ? targetPlayer : context.getSource().getPlayerOrThrow();
        
        ProfessionComponent component = ProfessionComponent.KEY.get(player);
        ProfessionProgress progress = component.getCurrentProgress();
        
        if (progress == null) {
            context.getSource().sendFeedback(() -> Text.literal("У игрока " + player.getName().getString() + " нет активного происхождения")
                .formatted(Formatting.YELLOW), false);
            return 0;
        }
        
        Profession profession = ProfessionRegistry.get(progress.getProfessionId());
        String professionName = profession != null ? profession.getName() : progress.getProfessionId().toString();
        
        context.getSource().sendFeedback(() -> Text.literal("=== Прогрессия игрока " + player.getName().getString() + " ===")
            .formatted(Formatting.GOLD), false);
        
        context.getSource().sendFeedback(() -> Text.literal("Происхождение: " + professionName)
            .formatted(Formatting.AQUA), false);
        
        context.getSource().sendFeedback(() -> Text.literal("Уровень: " + progress.getLevel())
            .formatted(Formatting.GREEN), false);
        
        context.getSource().sendFeedback(() -> Text.literal("Опыт: " + progress.getExperience() + "/" + progress.getExperienceForNextLevel() + " (" + (int)(progress.getLevelProgress() * 100) + "%)")
            .formatted(Formatting.BLUE), false);
        
        context.getSource().sendFeedback(() -> Text.literal("Общий опыт: " + progress.getTotalExperience())
            .formatted(Formatting.GRAY), false);
        
        return 1;
    }
    
    public static int executeAddExperience(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        int experience = IntegerArgumentType.getInteger(context, "experience");
        
        ProfessionComponent component = ProfessionComponent.KEY.get(player);
        ProfessionProgress progress = component.getCurrentProgress();
        
        if (progress == null) {
            context.getSource().sendError(Text.literal("У игрока нет активного происхождения"));
            return 0;
        }
        
        int oldLevel = progress.getLevel();
        component.addExperience(experience);
        int newLevel = progress.getLevel();
        
        context.getSource().sendFeedback(() -> Text.literal("Добавлено " + experience + " опыта игроку " + player.getName().getString())
            .formatted(Formatting.GREEN), true);
        
        if (newLevel > oldLevel) {
            context.getSource().sendFeedback(() -> Text.literal("Игрок повысил уровень с " + oldLevel + " до " + newLevel + "!")
                .formatted(Formatting.GOLD), true);
        }
        
        return 1;
    }
    
    public static int executeSetLevel(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        int level = IntegerArgumentType.getInteger(context, "level");
        
        ProfessionComponent component = ProfessionComponent.KEY.get(player);
        ProfessionProgress progress = component.getCurrentProgress();
        
        if (progress == null) {
            context.getSource().sendError(Text.literal("У игрока нет активного происхождения"));
            return 0;
        }
        
        // Устанавливаем уровень через компонент
        component.setLevel(level);
        
        context.getSource().sendFeedback(() -> Text.literal("Установлен уровень " + level + " для игрока " + player.getName().getString())
            .formatted(Formatting.GREEN), true);
        
        return 1;
    }
    
    public static int executeReset(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        
        ProfessionComponent component = ProfessionComponent.KEY.get(player);
        ProfessionProgress progress = component.getCurrentProgress();
        
        if (progress == null) {
            context.getSource().sendError(Text.literal("У игрока нет активного происхождения"));
            return 0;
        }
        
        // Сбрасываем уровень до 1
        component.setLevel(1);
        
        context.getSource().sendFeedback(() -> Text.literal("Сброшена прогрессия игрока " + player.getName().getString())
            .formatted(Formatting.YELLOW), true);
        
        return 1;
    }
    
    private static String getOriginDisplayName(String originId) {
        return switch (originId) {
            case "origins:blacksmith" -> "🔨 Кузнец";
            case "origins:brewer" -> "🍺 Пивовар";
            case "origins:cook" -> "👨‍🍳 Повар";
            case "origins:courier" -> "📦 Курьер";
            case "origins:warrior" -> "⚔️ Воин";
            case "origins:miner" -> "⛏️ Шахтер";
            case "origins:human" -> "👤 Человек";
            default -> originId.replace("origins:", "").replace("_", " ");
        };
    }
}