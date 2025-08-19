package io.github.apace100.origins.item;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

public class PepperSprayItem extends Item {
    public PepperSprayItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        
        if (!world.isClient) {
            // Define spray area
            Box sprayArea = user.getBoundingBox().expand(7.0); // 7 blocks range
            
            // Find entities in spray area
            world.getEntitiesByClass(LivingEntity.class, sprayArea, entity -> 
                entity != user && user.distanceTo(entity) <= 7.0)
                .forEach(entity -> {
                    // Apply effects to entity
                    entity.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                        net.minecraft.entity.effect.StatusEffects.BLINDNESS, 
                        60, // 3 seconds
                        0
                    ));
                    
                    entity.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                        net.minecraft.entity.effect.StatusEffects.SLOWNESS, 
                        40, // 2 seconds
                        1
                    ));
                });
            
            // Play sound
            world.playSound(null, user.getX(), user.getY(), user.getZ(), 
                SoundEvents.BLOCK_FIRE_EXTINGUISH, 
                net.minecraft.sound.SoundCategory.PLAYERS, 
                1.0f, 1.5f);
                
            // Damage the item
            stack.damage(1, user, (player) -> player.sendToolBreakStatus(hand));
            
            user.sendMessage(net.minecraft.text.Text.literal("Used pepper spray!"), true);
        }
        
        return TypedActionResult.success(stack);
    }
}