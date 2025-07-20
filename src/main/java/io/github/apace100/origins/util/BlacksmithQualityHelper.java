package io.github.apace100.origins.util;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Formatting;

/**
 * Вспомогательный класс для работы с качеством предметов кузнеца
 */
public class BlacksmithQualityHelper {
    
    public static final String QUALITY_KEY = "BlacksmithQuality";
    public static final String ENHANCED_MAX_DAMAGE_KEY = "EnhancedMaxDamage";
    
    public enum Quality {
        POOR("плохое", "poor", 0.75f, Formatting.RED),
        NORMAL("обычное", "normal", 1.0f, Formatting.GRAY),
        GOOD("хорошее", "good", 1.5f, Formatting.YELLOW),
        LEGENDARY("легендарное", "legendary", 2.0f, Formatting.GOLD);
        
        private final String russianName;
        private final String englishName;
        private final float durabilityMultiplier;
        private final Formatting color;
        
        Quality(String russianName, String englishName, float durabilityMultiplier, Formatting color) {
            this.russianName = russianName;
            this.englishName = englishName;
            this.durabilityMultiplier = durabilityMultiplier;
            this.color = color;
        }
        
        public String getRussianName() { return russianName; }
        public String getEnglishName() { return englishName; }
        public float getDurabilityMultiplier() { return durabilityMultiplier; }
        public Formatting getColor() { return color; }
        
        public static Quality fromString(String qualityStr) {
            for (Quality quality : values()) {
                if (quality.russianName.equalsIgnoreCase(qualityStr) || 
                    quality.englishName.equalsIgnoreCase(qualityStr)) {
                    return quality;
                }
            }
            return NORMAL;
        }
    }
    
    /**
     * Применяет качество к предмету
     */
    public static void applyQuality(ItemStack stack, Quality quality) {
        if (stack.isEmpty() || !stack.isDamageable()) {
            return;
        }
        
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putString(QUALITY_KEY, quality.getRussianName());
        
        // Вычисляем новую максимальную прочность
        int originalMaxDamage = stack.getItem().getMaxDamage();
        int enhancedMaxDamage = Math.round(originalMaxDamage * quality.getDurabilityMultiplier());
        nbt.putInt(ENHANCED_MAX_DAMAGE_KEY, enhancedMaxDamage);
    }
    
    /**
     * Получает качество предмета
     */
    public static Quality getQuality(ItemStack stack) {
        if (!stack.hasNbt()) {
            return Quality.NORMAL;
        }
        
        NbtCompound nbt = stack.getNbt();
        if (nbt.contains(QUALITY_KEY)) {
            String qualityStr = nbt.getString(QUALITY_KEY);
            return Quality.fromString(qualityStr);
        }
        
        return Quality.NORMAL;
    }
    
    /**
     * Проверяет, имеет ли предмет качество кузнеца
     */
    public static boolean hasQuality(ItemStack stack) {
        return stack.hasNbt() && stack.getNbt().contains(QUALITY_KEY);
    }
    
    /**
     * Получает улучшенную максимальную прочность
     */
    public static int getEnhancedMaxDamage(ItemStack stack) {
        if (!stack.hasNbt()) {
            return stack.getMaxDamage();
        }
        
        NbtCompound nbt = stack.getNbt();
        if (nbt.contains(ENHANCED_MAX_DAMAGE_KEY)) {
            return nbt.getInt(ENHANCED_MAX_DAMAGE_KEY);
        }
        
        return stack.getMaxDamage();
    }
}