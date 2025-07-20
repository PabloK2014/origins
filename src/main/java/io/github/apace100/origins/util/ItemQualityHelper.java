package io.github.apace100.origins.util;

import io.github.apace100.origins.power.BlacksmithQualityCraftingPower;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.util.List;
import java.util.ArrayList;

/**
 * Утилитарный класс для работы с качеством предметов
 */
public class ItemQualityHelper {
    
    /**
     * Применяет модификаторы качества к характеристикам предмета
     */
    public static void applyQualityModifiers(ItemStack stack) {
        BlacksmithQualityCraftingPower.ItemQuality quality = 
            BlacksmithQualityCraftingPower.getItemQuality(stack);
        
        if (quality == BlacksmithQualityCraftingPower.ItemQuality.NORMAL) {
            return;
        }
        
        BlacksmithQualityCraftingPower.QualityModifiers modifiers = 
            quality.getModifiersForItem(stack);
        
        // Применяем модификатор прочности
        applyDurabilityModifier(stack, modifiers.durabilityModifier);
    }
    
    /**
     * Применяет модификатор прочности к предмету
     */
    private static void applyDurabilityModifier(ItemStack stack, float modifier) {
        if (modifier == 0.0f || !stack.getItem().isDamageable()) {
            return;
        }
        
        NbtCompound nbt = stack.getOrCreateNbt();
        int originalMaxDamage = stack.getMaxDamage();
        int modifiedMaxDamage = Math.max(1, (int)(originalMaxDamage * (1.0f + modifier)));
        
        nbt.putInt("OriginalMaxDamage", originalMaxDamage);
        nbt.putInt("ModifiedMaxDamage", modifiedMaxDamage);
    }
    
    /**
     * Получает модифицированную максимальную прочность предмета
     */
    public static int getModifiedMaxDamage(ItemStack stack) {
        if (!stack.hasNbt()) {
            return stack.getMaxDamage();
        }
        
        NbtCompound nbt = stack.getNbt();
        if (nbt.contains("ModifiedMaxDamage")) {
            return nbt.getInt("ModifiedMaxDamage");
        }
        
        return stack.getMaxDamage();
    }
    
    /**
     * Получает модификатор урона для оружия
     */
    public static float getDamageModifier(ItemStack stack) {
        BlacksmithQualityCraftingPower.ItemQuality quality = 
            BlacksmithQualityCraftingPower.getItemQuality(stack);
        
        if (stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem) {
            BlacksmithQualityCraftingPower.QualityModifiers modifiers = 
                quality.getModifiersForItem(stack);
            return modifiers.primaryStatModifier;
        }
        
        return 0.0f;
    }
    
    /**
     * Получает модификатор скорости добычи для инструментов
     */
    public static float getMiningSpeedModifier(ItemStack stack) {
        BlacksmithQualityCraftingPower.ItemQuality quality = 
            BlacksmithQualityCraftingPower.getItemQuality(stack);
        
        if (stack.getItem() instanceof MiningToolItem) {
            BlacksmithQualityCraftingPower.QualityModifiers modifiers = 
                quality.getModifiersForItem(stack);
            return modifiers.primaryStatModifier;
        }
        
        return 0.0f;
    }
    
    /**
     * Получает модификатор защиты для брони
     */
    public static float getArmorProtectionModifier(ItemStack stack) {
        BlacksmithQualityCraftingPower.ItemQuality quality = 
            BlacksmithQualityCraftingPower.getItemQuality(stack);
        
        if (stack.getItem() instanceof ArmorItem) {
            BlacksmithQualityCraftingPower.QualityModifiers modifiers = 
                quality.getModifiersForItem(stack);
            return modifiers.primaryStatModifier;
        }
        
        return 0.0f;
    }
    
    /**
     * Добавляет информацию о качестве в tooltip предмета
     */
    public static void addQualityTooltip(ItemStack stack, List<Text> tooltip) {
        BlacksmithQualityCraftingPower.ItemQuality quality = 
            BlacksmithQualityCraftingPower.getItemQuality(stack);
        
        if (quality == BlacksmithQualityCraftingPower.ItemQuality.NORMAL) {
            return;
        }
        
        // Добавляем название качества
        tooltip.add(Text.literal("Качество: " + quality.getDisplayName())
            .formatted(quality.getColor()));
        
        // Добавляем информацию о модификаторах
        BlacksmithQualityCraftingPower.QualityModifiers modifiers = 
            quality.getModifiersForItem(stack);
        
        if (modifiers.durabilityModifier != 0.0f) {
            String durabilityText = String.format("Прочность: %+.0f%%", 
                modifiers.durabilityModifier * 100);
            tooltip.add(Text.literal(durabilityText).formatted(Formatting.BLUE));
        }
        
        if (modifiers.primaryStatModifier != 0.0f) {
            String statName = getStatNameForItem(stack);
            String statText = String.format("%s: %+.0f%%", 
                statName, modifiers.primaryStatModifier * 100);
            tooltip.add(Text.literal(statText).formatted(Formatting.BLUE));
        }
        
        // Добавляем легендарные эффекты
        if (quality == BlacksmithQualityCraftingPower.ItemQuality.LEGENDARY) {
            addLegendaryEffectsTooltip(stack, tooltip);
        }
    }
    
    /**
     * Получает название основной характеристики для предмета
     */
    private static String getStatNameForItem(ItemStack stack) {
        Item item = stack.getItem();
        
        if (item instanceof SwordItem) {
            return "Урон";
        } else if (item instanceof MiningToolItem) {
            return "Скорость добычи";
        } else if (item instanceof ArmorItem) {
            return "Защита";
        } else if (item instanceof HoeItem) {
            return "Урожай";
        }
        
        return "Эффективность";
    }
    
    /**
     * Добавляет описание легендарных эффектов в tooltip
     */
    private static void addLegendaryEffectsTooltip(ItemStack stack, List<Text> tooltip) {
        if (!stack.hasNbt()) {
            return;
        }
        
        NbtCompound nbt = stack.getNbt();
        
        tooltip.add(Text.literal("Легендарные эффекты:").formatted(Formatting.GOLD));
        
        for (int i = 1; i <= 2; i++) {
            String effectKey = "LegendaryEffect" + i;
            if (nbt.contains(effectKey)) {
                String effect = nbt.getString(effectKey);
                tooltip.add(Text.literal("• " + effect).formatted(Formatting.YELLOW));
            }
        }
    }
    
    /**
     * Проверяет, может ли предмет иметь качество
     */
    public static boolean canHaveQuality(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof ToolItem || 
               item instanceof SwordItem || 
               item instanceof ArmorItem;
    }
}