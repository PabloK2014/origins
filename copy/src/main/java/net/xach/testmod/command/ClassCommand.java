package net.xach.testmod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;
import net.xach.testmod.ClassSelectionMenu;
import net.xach.testmod.MenuRegistry;
import net.xach.testmod.SkillTreeHandler;
import net.xach.testmod.TestMod;

@Mod.EventBusSubscriber(modid = TestMod.MOD_ID)
public class ClassCommand {

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("testmod")
                        .then(Commands.literal("class")
                                .executes(context -> executeClassSelection(context))
                                .then(Commands.argument("className", StringArgumentType.string())
                                        .executes(context -> executeClassSet(context))
                                )
                        )
                        .then(Commands.literal("skills")
                                .executes(context -> executeSkillTree(context))
                        )
                        .then(Commands.literal("info")
                                .executes(context -> executePlayerInfo(context))
                        )
                        .requires(source -> source.hasPermission(0)) // Доступно всем игрокам
        );
    }

    private static int executeClassSelection(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            player.getCapability(TestMod.PlayerClassCapability.CAPABILITY).ifPresent(cap -> {
                if (cap.getPlayerClass().isEmpty()) {
                    // Открываем GUI выбора класса
                    NetworkHooks.openScreen(player, new ClassSelectionMenu.Provider(), buf -> {});
                } else {
                    player.sendSystemMessage(Component.literal("Ваш текущий класс: " + getClassDisplayName(cap.getPlayerClass())));
                    player.sendSystemMessage(Component.literal("Доступные классы:"));
                    player.sendSystemMessage(Component.literal("- /testmod class war (Воин)"));
                    player.sendSystemMessage(Component.literal("- /testmod class cook (Повар)"));
                    player.sendSystemMessage(Component.literal("- /testmod class yandex.go (Курьер Yandex.Go)"));
                    player.sendSystemMessage(Component.literal("- /testmod class pivo (Пивовар)"));
                    player.sendSystemMessage(Component.literal("- /testmod class miner (Шахтёр)"));
                    player.sendSystemMessage(Component.literal("- /testmod class smith (Кузнец)"));
                }
            });
        }
        return 1;
    }

    private static int executeClassSet(CommandContext<CommandSourceStack> context) {
        String className = StringArgumentType.getString(context, "className");

        // Проверяем валидность класса
        if (!isValidClass(className)) {
            context.getSource().sendFailure(Component.literal("Неизвестный класс: " + className));
            context.getSource().sendFailure(Component.literal("Доступные классы: war, cook, yandex.go, pivo, miner"));
            return 0;
        }

        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            player.getCapability(TestMod.PlayerClassCapability.CAPABILITY).ifPresent(cap -> {
                String oldClass = cap.getPlayerClass();
                cap.setPlayerClass(className);
                cap.clearSkillLevels(); // Сбрасываем навыки при смене класса
                cap.setSkillPoints(cap.getLevel()); // Возвращаем очки навыков
                cap.sync(player);

                if (oldClass.isEmpty()) {
                    player.sendSystemMessage(Component.literal("Класс выбран: " + getClassDisplayName(className)));
                } else {
                    player.sendSystemMessage(Component.literal("Класс изменён с " + getClassDisplayName(oldClass) + " на " + getClassDisplayName(className)));
                }

                player.sendSystemMessage(Component.literal("Используйте /testmod skills для открытия дерева навыков"));
            });
        }
        return 1;
    }

    private static int executeSkillTree(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            player.getCapability(TestMod.PlayerClassCapability.CAPABILITY).ifPresent(cap -> {
                if (cap.getPlayerClass().isEmpty()) {
                    player.sendSystemMessage(Component.literal("Сначала выберите класс командой /testmod class"));
                } else {
                    // Открываем дерево навыков
                    NetworkHooks.openScreen(player, new net.xach.testmod.SkillTreeMenu.Provider(), buf -> {});
                }
            });
        }
        return 1;
    }

    private static int executePlayerInfo(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            player.getCapability(TestMod.PlayerClassCapability.CAPABILITY).ifPresent(cap -> {
                player.sendSystemMessage(Component.literal("=== Информация о персонаже ==="));
                player.sendSystemMessage(Component.literal("Класс: " + (cap.getPlayerClass().isEmpty() ? "Не выбран" : getClassDisplayName(cap.getPlayerClass()))));
                player.sendSystemMessage(Component.literal("Уровень: " + cap.getLevel()));
                player.sendSystemMessage(Component.literal("Опыт: " + cap.getExperience() + "/" + (cap.getLevel() * 100)));
                player.sendSystemMessage(Component.literal("Очки навыков: " + cap.getSkillPoints()));
                player.sendSystemMessage(Component.literal("Энергия: " + cap.getSurgeEnergy() + "/100"));
                player.sendSystemMessage(Component.literal("Активный навык: " + (cap.getActiveSkill().isEmpty() ? "Не выбран" : cap.getActiveSkill())));

                // Показываем изученные навыки
                if (!cap.getPlayerClass().isEmpty()) {
                    SkillTreeHandler.SkillTree skillTree = SkillTreeHandler.CLASS_SKILL_TREES.get(cap.getPlayerClass());
                    if (skillTree != null) {
                        player.sendSystemMessage(Component.literal("=== Изученные навыки ==="));
                        skillTree.getAllSkills().forEach(skill -> {
                            int level = cap.getSkillLevel(skill.getId());
                            if (level > 0) {
                                player.sendSystemMessage(Component.literal("- " + skill.getName() + ": " + level + "/" + skill.getMaxLevel()));
                            }
                        });
                    }
                }
            });
        }
        return 1;
    }

    private static boolean isValidClass(String className) {
        return className.equals("war") ||
                className.equals("cook") ||
                className.equals("yandex.go") ||
                className.equals("pivo") ||
                className.equals("miner") ||
                className.equals("smith");
    }

    private static String getClassDisplayName(String className) {
        return switch (className) {
            case "war" -> "Воин";
            case "cook" -> "Повар";
            case "yandex.go" -> "Курьер Yandex.Go";
            case "pivo" -> "Пивовар";
            case "miner" -> "Шахтёр";
            case "smith" -> "Кузнец";
            default -> className;
        };
    }
}
