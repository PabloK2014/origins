package io.github.apace100.origins.quest;

import com.google.gson.JsonObject;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Основной класс данных квеста, аналогичный BountyData из Bountiful
 */
public class BountifulQuestData {
    private final List<BountifulQuestEntry> objectives = new ArrayList<>();
    private final List<BountifulQuestEntry> rewards = new ArrayList<>();
    private boolean pingComplete = false;
    
    public List<BountifulQuestEntry> getObjectives() {
        return objectives;
    }
    
    public List<BountifulQuestEntry> getRewards() {
        return rewards;
    }
    
    /**
     * Проверяет завершены ли все цели квеста
     */
    private boolean hasFinishedAllObjectives(PlayerEntity player) {
        return objectives.stream().allMatch(entry -> 
            entry.getObjectiveType().getProgress(entry, player).isComplete()
        );
    }
    
    /**
     * Пытается завершить все цели квеста
     */
    private boolean tryFinishObjectives(PlayerEntity player) {
        return objectives.stream().allMatch(entry -> 
            entry.getObjectiveType().tryFinishObjective(entry, player)
        );
    }
    
    /**
     * Проверяет завершение квеста и уведомляет игрока
     */
    public BountifulQuestData checkForCompletionAndAlert(PlayerEntity player, ItemStack stack) {
        BountifulQuestInfo info = BountifulQuestInfo.get(stack);
        
        boolean isDone = objectives.stream().allMatch(entry -> 
            entry.getObjectiveType().getProgress(entry, player).isComplete()
        ) && info.timeLeft(player.getWorld()) > 0;
        
        if (isDone) {
            if (!pingComplete) {
                pingComplete = true;
                
                // Воспроизводим звук завершения
                if (player instanceof ServerPlayerEntity) {
                    player.playSound(
                        SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                        SoundCategory.MASTER,
                        1.0f, 1.0f
                    );
                }
                
                // Сохраняем обновленные данные
                set(stack, this);
            }
        } else {
            pingComplete = false;
            set(stack, this);
        }
        
        return this;
    }
    
    /**
     * Награждает игрока за выполнение квеста
     */
    private void rewardPlayer(PlayerEntity player) {
        // Воспроизводим звук получения опыта
        player.playSound(
            SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
            SoundCategory.MASTER,
            1.0f, 1.0f
        );
        
        // Даем опыт игроку
        int totalExp = rewards.stream()
            .mapToInt(reward -> reward.getRarity().ordinal() * 2 + 1)
            .sum();
        player.addExperience(totalExp);
        
        // Выдаем награды
        for (BountifulQuestEntry reward : rewards) {
            reward.getRewardType().giveReward(reward, player);
        }
    }
    
    /**
     * Пытается сдать квест
     */
    public boolean tryCashIn(PlayerEntity player, ItemStack stack) {
        BountifulQuestInfo info = BountifulQuestInfo.get(stack);
        
        if (info.timeLeft(player.getWorld()) <= 0) {
            player.sendMessage(Text.translatable("origins.quest.expired"), false);
            return false;
        }
        
        if (hasFinishedAllObjectives(player)) {
            tryFinishObjectives(player);
            rewardPlayer(player);
            stack.decrement(stack.getMaxCount());
            return true;
        } else {
            player.sendMessage(Text.translatable("origins.quest.requirements_not_met"), false);
            return false;
        }
    }
    
    /**
     * Получает данные квеста из ItemStack
     */
    public static BountifulQuestData get(ItemStack stack) {
        NbtCompound nbt = stack.getOrCreateNbt();
        if (nbt.contains("QuestData")) {
            return fromNbt(nbt.getCompound("QuestData"));
        }
        return new BountifulQuestData();
    }
    
    /**
     * Сохраняет данные квеста в ItemStack
     */
    public static void set(ItemStack stack, BountifulQuestData data) {
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.put("QuestData", data.toNbt());
    }
    
    /**
     * Создает данные квеста из NBT
     */
    public static BountifulQuestData fromNbt(NbtCompound nbt) {
        BountifulQuestData data = new BountifulQuestData();
        
        // Загружаем цели
        if (nbt.contains("objectives")) {
            NbtCompound objectivesNbt = nbt.getCompound("objectives");
            for (String key : objectivesNbt.getKeys()) {
                BountifulQuestEntry entry = BountifulQuestEntry.fromNbt(objectivesNbt.getCompound(key));
                data.objectives.add(entry);
            }
        }
        
        // Загружаем награды
        if (nbt.contains("rewards")) {
            NbtCompound rewardsNbt = nbt.getCompound("rewards");
            for (String key : rewardsNbt.getKeys()) {
                BountifulQuestEntry entry = BountifulQuestEntry.fromNbt(rewardsNbt.getCompound(key));
                data.rewards.add(entry);
            }
        }
        
        data.pingComplete = nbt.getBoolean("pingComplete");
        
        return data;
    }
    
    /**
     * Сохраняет данные квеста в NBT
     */
    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        
        // Сохраняем цели
        NbtCompound objectivesNbt = new NbtCompound();
        for (int i = 0; i < objectives.size(); i++) {
            objectivesNbt.put(String.valueOf(i), objectives.get(i).toNbt());
        }
        nbt.put("objectives", objectivesNbt);
        
        // Сохраняем награды
        NbtCompound rewardsNbt = new NbtCompound();
        for (int i = 0; i < rewards.size(); i++) {
            rewardsNbt.put(String.valueOf(i), rewards.get(i).toNbt());
        }
        nbt.put("rewards", rewardsNbt);
        
        nbt.putBoolean("pingComplete", pingComplete);
        
        return nbt;
    }
}