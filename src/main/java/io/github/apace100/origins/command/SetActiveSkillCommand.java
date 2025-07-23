package io.github.apace100.origins.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.apace100.origins.skill.PlayerSkillComponent;
import io.github.apace100.origins.skill.BlacksmithSkillHandler;
import io.github.apace100.origins.skill.WarriorSkillHandler;
import io.github.apace100.origins.skill.CourierSkillHandler;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class SetActiveSkillCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            literal("setactiveskill")
                .then(argument("number", IntegerArgumentType.integer(1))
                    .executes(SetActiveSkillCommand::setActiveSkill)
                )
        );
    }
    
    private static int setActiveSkill(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
            int skillNumber = IntegerArgumentType.getInteger(context, "number");
            
            PlayerSkillComponent skillComponent = PlayerSkillComponent.KEY.get(player);
            if (skillComponent == null) {
                player.sendMessage(
                    Text.literal("Ошибка: компонент навыков не найден")
                        .formatted(Formatting.RED), 
                    false
                );
                return 0;
            }
            
            // Получаем список доступных активных навыков
            List<SkillInfo> availableSkills = getAvailableSkills(player, skillComponent);
            
            if (availableSkills.isEmpty()) {
                player.sendMessage(
                    Text.literal("У вас нет доступных активных навыков")
                        .formatted(Formatting.GRAY), 
                    false
                );
                return 0;
            }
            
            if (skillNumber < 1 || skillNumber > availableSkills.size()) {
                player.sendMessage(
                    Text.literal("Неверный номер навыка! Доступные номера: 1-" + availableSkills.size())
                        .formatted(Formatting.RED), 
                    false
                );
                return 0;
            }
            
            SkillInfo selectedSkill = availableSkills.get(skillNumber - 1);
            
            // Сохраняем выбранный навык в компоненте игрока
            setPlayerActiveSkill(player, selectedSkill.id);
            
            player.sendMessage(
                Text.literal("Активный навык установлен: " + selectedSkill.name + " (Уровень " + selectedSkill.level + ")")
                    .formatted(Formatting.GREEN), 
                false
            );
            
            player.sendMessage(
                Text.literal("Используйте клавишу K для активации навыка")
                    .formatted(Formatting.GRAY), 
                false
            );
            
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendError(Text.literal("Ошибка при установке активного навыка: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Устанавливает активный навык для игрока
     */
    private static void setPlayerActiveSkill(ServerPlayerEntity player, String skillId) {
        // Здесь нужно сохранить выбранный навык в NBT данных игрока
        // Пока используем простое хранение в компоненте
        PlayerSkillComponent skillComponent = PlayerSkillComponent.KEY.get(player);
        if (skillComponent != null) {
            // Добавляем метод для сохранения активного навыка
            skillComponent.setActiveSkill(skillId);
            PlayerSkillComponent.KEY.sync(player);
        }
    }
    
    /**
     * Получает список доступных активных навыков для игрока
     */
    private static List<SkillInfo> getAvailableSkills(ServerPlayerEntity player, PlayerSkillComponent skillComponent) {
        List<SkillInfo> availableSkills = new ArrayList<>();
        
        // Определяем класс игрока и добавляем соответствующие навыки
        if (BlacksmithSkillHandler.isBlacksmith(player)) {
            // Навыки кузнеца
            int hotStrikeLevel = skillComponent.getSkillLevel("hot_strike");
            if (hotStrikeLevel > 0) {
                availableSkills.add(new SkillInfo("hot_strike", "Раскалённый удар", hotStrikeLevel));
            }
            
            int instantRepairLevel = skillComponent.getSkillLevel("instant_repair");
            if (instantRepairLevel > 0) {
                availableSkills.add(new SkillInfo("instant_repair", "Мгновенный ремонт", instantRepairLevel));
            }
        } 
        else if (WarriorSkillHandler.isWarrior(player)) {
            // Навыки воина
            int madBoostLevel = skillComponent.getSkillLevel("mad_boost");
            if (madBoostLevel > 0) {
                availableSkills.add(new SkillInfo("mad_boost", "Безумный рывок", madBoostLevel));
            }
            
            int indestructibilityLevel = skillComponent.getSkillLevel("indestructibility");
            if (indestructibilityLevel > 0) {
                availableSkills.add(new SkillInfo("indestructibility", "Несокрушимость", indestructibilityLevel));
            }
            
            int dagestanskayaBratvaLevel = skillComponent.getSkillLevel("dagestan");
            if (dagestanskayaBratvaLevel > 0) {
                availableSkills.add(new SkillInfo("dagestan", "Дагестанская братва", dagestanskayaBratvaLevel));
            }
        } 
        else if (CourierSkillHandler.isCourier(player)) {
            // Навыки курьера
            int sprintBoostLevel = skillComponent.getSkillLevel("sprint_boost");
            if (sprintBoostLevel > 0) {
                availableSkills.add(new SkillInfo("sprint_boost", "Рывок", sprintBoostLevel));
            }
            
            int speedSurgeLevel = skillComponent.getSkillLevel("speed_surge");
            if (speedSurgeLevel > 0) {
                availableSkills.add(new SkillInfo("speed_surge", "Всплеск скорости", speedSurgeLevel));
            }
            
            int trapLevel = skillComponent.getSkillLevel("carry_capacity_basic");
            if (trapLevel > 0) {
                availableSkills.add(new SkillInfo("carry_capacity_basic", "Ловушка", trapLevel));
            }
        }
        
        return availableSkills;
    }
    
    /**
     * Класс для хранения информации о навыке
     */
    private static class SkillInfo {
        public final String id;
        public final String name;
        public final int level;
        
        public SkillInfo(String id, String name, int level) {
            this.id = id;
            this.name = name;
            this.level = level;
        }
    }
}