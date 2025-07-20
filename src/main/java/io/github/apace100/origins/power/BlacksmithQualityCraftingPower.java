package io.github.apace100.origins.power;

import io.github.apace100.apoli.power.Power;
import io.github.apace100.apoli.power.PowerType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Сила кузнеца для создания предметов с различным качеством
 */
public class BlacksmithQualityCraftingPower extends Power {
    
    public enum ItemQuality {
        POOR(0.10f, "Плохое", Formatting.RED, -0.25f, -0.15f),
        NORMAL(0.64f, "Обычное", Formatting.WHITE, 0.0f, 0.0f),
        GOOD(0.25f, "Хорошее", Formatting.GREEN, 0.20f, 0.10f),
        LEGENDARY(0.01f, "Легендарное", Formatting.GOLD, 0.50f, 0.25f);
        
        private final float chance;
        private final String displayName;
        private final Formatting color;
        private final float durabilityModifier;
        private final float primaryStatModifier;
        
        ItemQuality(float chance, String displayName, Formatting color, 
                   float durabilityModifier, float primaryStatModifier) {
            this.chance = chance;
            this.displayName = displayName;
            this.color = color;
            this.durabilityModifier = durabilityModifier;
            this.primaryStatModifier = primaryStatModifier;
        }
        
        public float getChance() { return chance; }
        public String getDisplayName() { return displayName; }
        public Formatting getColor() { return color; }
        public float getDurabilityModifier() { return durabilityModifier; }
        public float getPrimaryStatModifier() { return primaryStatModifier; }
        
        /**
         * Получить специфичные модификаторы для типа предмета
         */
        public QualityModifiers getModifiersForItem(ItemStack stack) {
            Item item = stack.getItem();
            
            if (item instanceof SwordItem) {
                return getSwordModifiers();
            } else if (item instanceof PickaxeItem) {
                return getPickaxeModifiers();
            } else if (item instanceof AxeItem) {
                return getAxeModifiers();
            } else if (item instanceof ShovelItem) {
                return getShovelModifiers();
            } else if (item instanceof HoeItem) {
                return getHoeModifiers();
            } else if (item instanceof ArmorItem armor) {
                return getArmorModifiers(armor.getSlotType());
            }
            
            return new QualityModifiers(durabilityModifier, primaryStatModifier);
        }
        
        private QualityModifiers getSwordModifiers() {
            return switch (this) {
                case POOR -> new QualityModifiers(-0.25f, -0.15f);
                case NORMAL -> new QualityModifiers(0.0f, 0.0f);
                case GOOD -> new QualityModifiers(0.20f, 0.10f);
                case LEGENDARY -> new QualityModifiers(0.50f, 0.25f);
            };
        }
        
        private QualityModifiers getPickaxeModifiers() {
            return switch (this) {
                case POOR -> new QualityModifiers(-0.30f, -0.20f);
                case NORMAL -> new QualityModifiers(0.0f, 0.0f);
                case GOOD -> new QualityModifiers(0.25f, 0.15f);
                case LEGENDARY -> new QualityModifiers(0.60f, 0.35f);
            };
        }
        
        private QualityModifiers getAxeModifiers() {
            return switch (this) {
                case POOR -> new QualityModifiers(-0.25f, -0.10f);
                case NORMAL -> new QualityModifiers(0.0f, 0.0f);
                case GOOD -> new QualityModifiers(0.20f, 0.10f);
                case LEGENDARY -> new QualityModifiers(0.50f, 0.25f);
            };
        }
        
        private QualityModifiers getShovelModifiers() {
            return switch (this) {
                case POOR -> new QualityModifiers(-0.30f, -0.15f);
                case NORMAL -> new QualityModifiers(0.0f, 0.0f);
                case GOOD -> new QualityModifiers(0.25f, 0.10f);
                case LEGENDARY -> new QualityModifiers(0.60f, 0.30f);
            };
        }
        
        private QualityModifiers getHoeModifiers() {
            return switch (this) {
                case POOR -> new QualityModifiers(-0.25f, -0.10f);
                case NORMAL -> new QualityModifiers(0.0f, 0.0f);
                case GOOD -> new QualityModifiers(0.20f, 0.15f);
                case LEGENDARY -> new QualityModifiers(0.50f, 0.30f);
            };
        }
        
        private QualityModifiers getArmorModifiers(net.minecraft.entity.EquipmentSlot slot) {
            return switch (this) {
                case POOR -> new QualityModifiers(-0.20f, -0.10f);
                case NORMAL -> new QualityModifiers(0.0f, 0.0f);
                case GOOD -> new QualityModifiers(0.20f, 0.10f);
                case LEGENDARY -> new QualityModifiers(0.50f, 0.25f);
            };
        }
    }
    
    /**
     * Класс для хранения модификаторов качества
     */
    public static class QualityModifiers {
        public final float durabilityModifier;
        public final float primaryStatModifier;
        
        public QualityModifiers(float durabilityModifier, float primaryStatModifier) {
            this.durabilityModifier = durabilityModifier;
            this.primaryStatModifier = primaryStatModifier;
        }
    }
    
    public BlacksmithQualityCraftingPower(PowerType<?> type, LivingEntity entity) {
        super(type, entity);
    }
    
    /**
     * Определяет качество предмета на основе случайности
     */
    public ItemQuality determineQuality(Random random) {
        float roll = random.nextFloat();
        float cumulative = 0.0f;
        
        // Проверяем шансы в порядке: плохое -> обычное -> хорошее -> легендарное
        for (ItemQuality quality : ItemQuality.values()) {
            cumulative += quality.getChance();
            if (roll <= cumulative) {
                return quality;
            }
        }
        
        return ItemQuality.NORMAL; // Fallback на случай ошибки
    }
    
    /**
     * Проверяет, должна ли сила применяться к крафту
     */
    public boolean shouldApplyQuality() {
        return isActive();
    }
    
    /**
     * Применяет качество к предмету
     */
    public ItemStack applyQualityToItem(ItemStack stack, ItemQuality quality) {
        if (stack.isEmpty() || quality == ItemQuality.NORMAL) {
            return stack;
        }
        
        ItemStack result = stack.copy();
        QualityModifiers modifiers = quality.getModifiersForItem(result);
        
        // Применяем модификатор прочности
        if (modifiers.durabilityModifier != 0.0f && result.getItem().isDamageable()) {
            int baseDurability = result.getMaxDamage();
            int newDurability = Math.max(1, (int)(baseDurability * (1.0f + modifiers.durabilityModifier)));
            
            // Сохраняем качество в NBT
            result.getOrCreateNbt().putString("ItemQuality", quality.name());
            result.getOrCreateNbt().putInt("OriginalMaxDamage", baseDurability);
            result.getOrCreateNbt().putInt("ModifiedMaxDamage", newDurability);
        }
        
        // Добавляем визуальное отображение качества
        addQualityTooltip(result, quality);
        
        return result;
    }
    
    /**
     * Добавляет информацию о качестве в tooltip предмета
     */
    private void addQualityTooltip(ItemStack stack, ItemQuality quality) {
        var nbt = stack.getOrCreateNbt();
        nbt.putString("QualityDisplay", quality.getDisplayName());
        nbt.putString("QualityColor", quality.getColor().getName());
        
        // Добавляем легендарные эффекты в описание
        if (quality == ItemQuality.LEGENDARY) {
            addLegendaryEffectTooltip(stack);
        }
    }
    
    /**
     * Добавляет описание легендарных эффектов
     */
    private void addLegendaryEffectTooltip(ItemStack stack) {
        Item item = stack.getItem();
        var nbt = stack.getOrCreateNbt();
        
        if (item instanceof SwordItem) {
            nbt.putString("LegendaryEffect1", "Вампиризм: +20% исцеления от урона");
            nbt.putString("LegendaryEffect2", "25% шанс поджечь врага");
        } else if (item instanceof PickaxeItem) {
            nbt.putString("LegendaryEffect1", "Копание 3x3");
            nbt.putString("LegendaryEffect2", "+1 уровень Fortune");
        } else if (item instanceof AxeItem) {
            nbt.putString("LegendaryEffect1", "Удвоенный дроп древесины");
        } else if (item instanceof ShovelItem) {
            nbt.putString("LegendaryEffect1", "Мгновенное копание песка/гравия");
            nbt.putString("LegendaryEffect2", "20% шанс удвоенного дропа");
        } else if (item instanceof HoeItem) {
            nbt.putString("LegendaryEffect1", "Возделывание 3x3");
        } else if (item instanceof ArmorItem armor) {
            switch (armor.getSlotType()) {
                case HEAD -> {
                    nbt.putString("LegendaryEffect1", "25% блокировка стрел");
                }
                case CHEST -> {
                    nbt.putString("LegendaryEffect1", "40% игнорирование урона");
                    nbt.putString("LegendaryEffect2", "Замедление атакующего");
                }
                case LEGS -> {
                    nbt.putString("LegendaryEffect1", "+Скорость передвижения");
                    nbt.putString("LegendaryEffect2", "Игнорирование замедления в воде");
                }
                case FEET -> {
                    nbt.putString("LegendaryEffect1", "+1 блок к прыжку");
                    nbt.putString("LegendaryEffect2", "Иммунитет к урону от падения");
                }
            }
        }
    }
    
    /**
     * Получает качество предмета из NBT
     */
    public static ItemQuality getItemQuality(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasNbt()) {
            return ItemQuality.NORMAL;
        }
        
        String qualityName = stack.getNbt().getString("ItemQuality");
        if (qualityName.isEmpty()) {
            return ItemQuality.NORMAL;
        }
        
        try {
            return ItemQuality.valueOf(qualityName);
        } catch (IllegalArgumentException e) {
            return ItemQuality.NORMAL;
        }
    }
    
    /**
     * Проверяет, является ли предмет легендарным
     */
    public static boolean isLegendary(ItemStack stack) {
        return getItemQuality(stack) == ItemQuality.LEGENDARY;
    }
    
    /**
     * Проверяет, может ли предмет получить качество
     */
    public static boolean canHaveQuality(ItemStack stack) {
        if (stack.isEmpty()) return false;
        
        Item item = stack.getItem();
        return item instanceof ToolItem || 
               item instanceof ArmorItem || 
               item instanceof SwordItem ||
               item instanceof BowItem ||
               item instanceof CrossbowItem ||
               item instanceof TridentItem ||
               item instanceof ShieldItem;
    }
    
    /**
     * Получает отображаемое имя качества с цветом
     */
    public static Text getQualityDisplayName(ItemStack stack) {
        ItemQuality quality = getItemQuality(stack);
        return Text.literal(quality.getDisplayName()).formatted(quality.getColor());
    }
    
    /**
     * Проверяет, имеет ли предмет улучшенную прочность
     */
    public static boolean hasEnhancedDurability(ItemStack stack) {
        return stack.hasNbt() && stack.getNbt().contains("ModifiedMaxDamage");
    }
    
    /**
     * Получает улучшенную максимальную прочность предмета
     */
    public static int getEnhancedMaxDamage(ItemStack stack) {
        if (!hasEnhancedDurability(stack)) {
            return stack.getMaxDamage();
        }
        
        return stack.getNbt().getInt("ModifiedMaxDamage");
    }
}