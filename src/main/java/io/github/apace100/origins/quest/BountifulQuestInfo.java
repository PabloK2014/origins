package io.github.apace100.origins.quest;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Информация о квесте, аналогичная BountyInfo из Bountiful
 */
public class BountifulQuestInfo {
    private Quest.QuestRarity rarity = Quest.QuestRarity.COMMON;
    private long timeStarted = -1L;
    private long timeToComplete = -1L;
    private String profession = "any";
    
    public BountifulQuestInfo() {}
    
    public BountifulQuestInfo(Quest.QuestRarity rarity, long timeStarted, long timeToComplete, String profession) {
        this.rarity = rarity;
        this.timeStarted = timeStarted;
        this.timeToComplete = timeToComplete;
        this.profession = profession;
    }
    
    public Quest.QuestRarity getRarity() {
        return rarity;
    }
    
    public void setRarity(Quest.QuestRarity rarity) {
        this.rarity = rarity;
    }
    
    public String getProfession() {
        return profession;
    }
    
    public void setProfession(String profession) {
        this.profession = profession;
    }
    
    /**
     * Получает оставшееся время квеста
     */
    public long timeLeft(World world) {
        if (timeStarted == -1L || timeToComplete == -1L) {
            return 1L; // Если время не установлено, считаем что квест не истек
        }
        return Math.max(timeStarted - world.getTime() + timeToComplete, 0L);
    }
    
    /**
     * Форматирует оставшееся время для отображения
     */
    public Text formattedTimeLeft(World world) {
        long timeLeft = timeLeft(world) / 20; // Конвертируем тики в секунды
        
        if (timeLeft <= 0) {
            return Text.literal("Истекло").formatted(Formatting.RED);
        }
        
        long hours = timeLeft / 3600;
        long minutes = (timeLeft % 3600) / 60;
        long seconds = timeLeft % 60;
        
        if (hours > 0) {
            return Text.literal(String.format("%dч %dм", hours, minutes)).formatted(Formatting.WHITE);
        } else if (minutes > 0) {
            return Text.literal(String.format("%dм %dс", minutes, seconds)).formatted(Formatting.WHITE);
        } else {
            return Text.literal(String.format("%dс", seconds)).formatted(Formatting.YELLOW);
        }
    }
    
    /**
     * Генерирует тултип для квеста
     */
    public List<MutableText> generateTooltip(BountifulQuestData questData, boolean isServer) {
        if (isServer) {
            return new ArrayList<>();
        }
        
        List<MutableText> tooltip = new ArrayList<>();
        
        // Добавляем профессию
        if (!profession.equals("any")) {
            tooltip.add(Text.translatable("origins.quest.profession." + profession)
                .formatted(Formatting.GOLD));
        }
        
        // Добавляем цели
        tooltip.add(Text.translatable("origins.quest.objectives").formatted(Formatting.GOLD).append(":"));
        for (BountifulQuestEntry objective : questData.getObjectives()) {
            tooltip.add(objective.getTextSummary(MinecraftClient.getInstance().player, true));
        }
        
        // Добавляем награды
        tooltip.add(Text.translatable("origins.quest.rewards").formatted(Formatting.GOLD).append(":"));
        for (BountifulQuestEntry reward : questData.getRewards()) {
            tooltip.add(reward.getTextSummary(MinecraftClient.getInstance().player, false));
        }
        
        return tooltip;
    }
    
    /**
     * Получает информацию о квесте из ItemStack
     */
    public static BountifulQuestInfo get(ItemStack stack) {
        NbtCompound nbt = stack.getOrCreateNbt();
        if (nbt.contains("QuestInfo")) {
            return fromNbt(nbt.getCompound("QuestInfo"));
        }
        return new BountifulQuestInfo();
    }
    
    /**
     * Сохраняет информацию о квесте в ItemStack
     */
    public static void set(ItemStack stack, BountifulQuestInfo info) {
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.put("QuestInfo", info.toNbt());
    }
    
    /**
     * Создает информацию о квесте из NBT
     */
    public static BountifulQuestInfo fromNbt(NbtCompound nbt) {
        BountifulQuestInfo info = new BountifulQuestInfo();
        
        if (nbt.contains("rarity")) {
            info.rarity = Quest.QuestRarity.fromName(nbt.getString("rarity"));
        }
        
        info.timeStarted = nbt.getLong("timeStarted");
        info.timeToComplete = nbt.getLong("timeToComplete");
        info.profession = nbt.getString("profession");
        
        return info;
    }
    
    /**
     * Сохраняет информацию о квесте в NBT
     */
    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        
        nbt.putString("rarity", rarity.getName());
        nbt.putLong("timeStarted", timeStarted);
        nbt.putLong("timeToComplete", timeToComplete);
        nbt.putString("profession", profession);
        
        return nbt;
    }
}