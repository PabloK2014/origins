package io.github.apace100.origins.mixin;

import io.github.apace100.origins.power.BlacksmithQualityCraftingPower;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Миксин для эффектов движения легендарной брони
 */
@Mixin(PlayerEntity.class)
public class LegendaryArmorMovementMixin {
    
    private static final UUID LEGENDARY_LEGGINGS_SPEED_UUID = UUID.fromString("845DB27C-C624-495F-8C9F-6020A9A58B6B");
    private static final UUID LEGENDARY_BOOTS_JUMP_UUID = UUID.fromString("7107DE5E-7CE8-4030-940E-514C1F160890");
    
    @Inject(method = "tick", at = @At("HEAD"))
    private void applyLegendaryArmorEffects(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        
        if (player.getWorld().isClient) {
            return;
        }
        
        // Проверяем легендарные поножи (скорость)
        ItemStack leggings = player.getEquippedStack(EquipmentSlot.LEGS);
        if (!leggings.isEmpty() && leggings.getItem() instanceof ArmorItem && 
            BlacksmithQualityCraftingPower.isLegendary(leggings)) {
            applyLegendaryLeggingsEffect(player);
        } else {
            removeLegendaryLeggingsEffect(player);
        }
        
        // Проверяем легендарные ботинки (прыжок)
        ItemStack boots = player.getEquippedStack(EquipmentSlot.FEET);
        if (!boots.isEmpty() && boots.getItem() instanceof ArmorItem && 
            BlacksmithQualityCraftingPower.isLegendary(boots)) {
            applyLegendaryBootsEffect(player);
        } else {
            removeLegendaryBootsEffect(player);
        }
    }
    
    private void applyLegendaryLeggingsEffect(PlayerEntity player) {
        // +20% скорость передвижения
        var speedAttribute = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speedAttribute != null) {
            // Проверяем, есть ли уже модификатор с таким UUID
            boolean hasModifier = speedAttribute.getModifiers().stream()
                .anyMatch(modifier -> modifier.getId().equals(LEGENDARY_LEGGINGS_SPEED_UUID));
            
            if (!hasModifier) {
                EntityAttributeModifier speedModifier = new EntityAttributeModifier(
                    LEGENDARY_LEGGINGS_SPEED_UUID,
                    "Legendary Leggings Speed",
                    0.20, // +20% скорости
                    EntityAttributeModifier.Operation.MULTIPLY_TOTAL
                );
                speedAttribute.addPersistentModifier(speedModifier);
            }
        }
        
        // Игнорирование замедления в воде
        if (player.isTouchingWater() || player.isSubmergedIn(net.minecraft.registry.tag.FluidTags.WATER)) {
            // Применяем эффект скорости в воде через статус эффект
            player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                net.minecraft.entity.effect.StatusEffects.DOLPHINS_GRACE, 20, 0, true, false));
        }
    }
    
    private void removeLegendaryLeggingsEffect(PlayerEntity player) {
        var speedAttribute = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speedAttribute != null) {
            // Проверяем, есть ли модификатор с таким UUID
            boolean hasModifier = speedAttribute.getModifiers().stream()
                .anyMatch(modifier -> modifier.getId().equals(LEGENDARY_LEGGINGS_SPEED_UUID));
            
            if (hasModifier) {
                speedAttribute.removeModifier(LEGENDARY_LEGGINGS_SPEED_UUID);
            }
        }
    }
    
    private void applyLegendaryBootsEffect(PlayerEntity player) {
        // +1 блок к высоте прыжка через Jump Boost эффект
        if (!player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.JUMP_BOOST)) {
            player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                net.minecraft.entity.effect.StatusEffects.JUMP_BOOST, 40, 1, true, false));
        }
    }
    
    private void removeLegendaryBootsEffect(PlayerEntity player) {
        // Убираем Jump Boost эффект только если он от легендарных ботинок
        if (player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.JUMP_BOOST)) {
            var effect = player.getStatusEffect(net.minecraft.entity.effect.StatusEffects.JUMP_BOOST);
            if (effect != null && effect.isAmbient()) { // Проверяем, что это наш эффект
                player.removeStatusEffect(net.minecraft.entity.effect.StatusEffects.JUMP_BOOST);
            }
        }
    }
}