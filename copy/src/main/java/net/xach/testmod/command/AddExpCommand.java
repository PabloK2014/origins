package net.xach.testmod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xach.testmod.TestMod;
import net.xach.testmod.SkillTreeHandler;

@Mod.EventBusSubscriber(modid = TestMod.MOD_ID)
public class AddExpCommand {

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        // Команда добавления опыта
        dispatcher.register(
                Commands.literal("testmod")
                        .then(Commands.literal("addexp")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                        .executes(context -> {
                                            int amount = IntegerArgumentType.getInteger(context, "amount");
                                            CommandSourceStack source = context.getSource();
                                            ServerPlayer player = source.getPlayerOrException();
                                            player.getCapability(TestMod.PlayerClassCapability.CAPABILITY).ifPresent(cap -> {
                                                cap.addExperience(amount);
                                                cap.sync(player);
                                                source.sendSuccess(() -> Component.literal("Добавлено " + amount + " опыта. Уровень: " + cap.getLevel() + ", опыт: " + cap.getExperience()), false);
                                            });
                                            return 1;
                                        })
                                )
                        )
                        // Команда тестового спавна
                        .then(Commands.literal("testspawn")
                                .executes(context -> executeTestSpawn(context))
                        )
                        // Команда проверки навыков воина
                        .then(Commands.literal("war")
                                .then(Commands.literal("skills")
                                        .executes(context -> executeWarSkillsCommand(context))
                                )
                        )
                        .requires(source -> source.hasPermission(2))
        );
    }

    private static int executeTestSpawn(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            Level level = player.level();

            try {
                Wolf wolf = new Wolf(EntityType.WOLF, level);
                wolf.setPos(player.getX() + 2, player.getY(), player.getZ());
                wolf.setTame(true);
                wolf.setOwnerUUID(player.getUUID());

                boolean spawned = level.addFreshEntity(wolf);
                player.sendSystemMessage(Component.literal("Тест спавна волка: " + spawned));
                System.out.println("Test spawn result: " + spawned);

            } catch (Exception e) {
                player.sendSystemMessage(Component.literal("Ошибка спавна: " + e.getMessage()));
                System.err.println("Test spawn error: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return 1;
    }

    private static int executeWarSkillsCommand(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            player.getCapability(TestMod.PlayerClassCapability.CAPABILITY).ifPresent(cap -> {
                if (cap.getPlayerClass().equals("war")) {
                    StringBuilder skillsInfo = new StringBuilder("Навыки Воина:\n");
                    SkillTreeHandler.CLASS_SKILL_TREES.get("war").getAllSkills().forEach(skill -> {
                        int level = cap.getSkillLevel(skill.getId());
                        if (level > 0) {
                            skillsInfo.append(skill.getName()).append(": Уровень ").append(level).append("\n");
                        }
                    });
                    player.sendSystemMessage(Component.literal(skillsInfo.toString()));
                } else {
                    player.sendSystemMessage(Component.literal("Вы не Воин!"));
                }
            });
        }
        return 1;
    }
}
