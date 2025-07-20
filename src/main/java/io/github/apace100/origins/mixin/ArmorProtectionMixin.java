package io.github.apace100.origins.mixin;

import io.github.apace100.origins.power.BlacksmithQualityCraftingPower;
import io.github.apace100.origins.util.ItemQualityHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Миксин для модификации защиты брони в зависимости от качества
 */
@Mixin(value = LivingEntity.class, priority = 1000)
public class ArmorProtectionMixin {
    
    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
    private float modifyArmorProtection(float amount, DamageSource source) {
        LivingEntity entity = (LivingEntity) (Object) this;
        
        if (entity instanceof PlayerEntity player) {
            float totalProtectionModifier = 0.0f;
            int armorPieces = 0;
            
            // Проверяем все части брони
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                    ItemStack armorPiece = player.getEquippedStack(slot);
                    
                    if (!armorPiece.isEmpty() && armorPiece.getItem() instanceof ArmorItem) {
                        float protectionModifier = ItemQualityHelper.getArmorProtectionModifier(armorPiece);
                        
                        if (protectionModifier != 0.0f) {
                            totalProtectionModifier += protectionModifier;
                            armorPieces++;
                        }
                        
                        // Применяем легендарные эффекты брони
                        if (BlacksmithQualityCraftingPower.isLegendary(armorPiece)) {
                            amount = applyLegendaryArmorEffects(armorPiece, amount, source, player);
                        }
                    }
                }
            }
            
            // Применяем средний модификатор защиты
            if (armorPieces > 0) {
                float averageModifier = totalProtectionModifier / armorPieces;
                amount *= (1.0f - averageModifier * 0.5f); // Уменьшаем урон
            }
        }
        
        return amount;
    }
    
    private float applyLegendaryArmorEffects(ItemStack armorPiece, float damage, 
                                           DamageSource source, PlayerEntity player) {
        ArmorItem armor = (ArmorItem) armorPiece.getItem();
        
        switch (armor.getSlotType()) {
            case HEAD -> {
                // 25% блокировка стрел
                if (isProjectileDamage(source) && player.getRandom().nextFloat() < 0.25f) {
                    return 0.0f; // Полностью блокируем урон от стрел
                }
            }
            case CHEST -> {
                // 40% игнорирование урона (перезарядка 5 сек)
                if (player.getRandom().nextFloat() < 0.40f && canUseChestplateEffect(player)) {
                    setChestplateEffectCooldown(player);
                    
                    // Замедление атакующего врага на 1.5 сек
                    if (source.getAttacker() instanceof LivingEntity attacker) {
                        attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 30, 1)); // 1.5 сек замедления
                    }
                    
                    return 0.0f; // Игнорируем урон
                }
            }
            case LEGS -> {
                // Эффекты скорости применяются в другом миксине
            }
            case FEET -> {
                // Эффекты прыжка и падения применяются в другом миксине
            }
        }
        
        return damage;
    }
    
    private boolean isProjectileDamage(DamageSource source) {
        // Проверяем, является ли урон от снаряда через анализ источника
        if (source.getSource() != null) {
            String className = source.getSource().getClass().getSimpleName();
            return className.contains("Arrow") || 
                   className.contains("Projectile") ||
                   className.contains("Trident") ||
                   className.contains("Fireball") ||
                   className.contains("Snowball") ||
                   className.contains("Egg") ||
                   className.contains("EnderPearl");
        }
        
        // Проверяем по типу урона через registry key
        try {
            String damageTypeId = source.getType().msgId();
            return damageTypeId.equals("arrow") || 
                   damageTypeId.equals("trident") || 
                   damageTypeId.equals("fireball") ||
                   damageTypeId.equals("thrown") ||
                   damageTypeId.contains("projectile");
        } catch (Exception e) {
            // Fallback на проверку через источник
            return source.getSource() != null && 
                   (source.getSource().getClass().getSimpleName().contains("Projectile") ||
                    source.getSource().getClass().getSimpleName().contains("Arrow"));
        }
    }
    
    private boolean canUseChestplateEffect(PlayerEntity player) {
        // Используем NBT данные игрока для кулдауна
        NbtCompound nbt = new NbtCompound();
        player.writeNbt(nbt);
        
        if (!nbt.contains("LegendaryChestplateCooldown")) {
            return true;
        }
        
        long currentTime = player.getWorld().getTime();
        long lastUse = nbt.getLong("LegendaryChestplateCooldown");
        
        return currentTime - lastUse > 100; // 5 секунд = 100 тиков
    }
    
    private void setChestplateEffectCooldown(PlayerEntity player) {
        // Сохраняем время последнего использования в NBT
        NbtCompound nbt = new NbtCompound();
        player.writeNbt(nbt);
        nbt.putLong("LegendaryChestplateCooldown", player.getWorld().getTime());
        player.readNbt(nbt);
    }
}