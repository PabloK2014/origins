package net.xach.testmod.items.custom;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PepperSprayItem extends Item {
    private static final int MAX_USES = 10;
    private static final double SPRAY_RANGE = 7.0;
    private static final float DAMAGE = 4.0f;

    public PepperSprayItem(Properties properties) {
        super(properties.stacksTo(1).durability(MAX_USES));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            sprayPepper(level, player, itemStack);
        }

        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
    }

    private void sprayPepper(Level level, Player player, ItemStack itemStack) {
        Vec3 lookDirection = player.getLookAngle();
        Vec3 playerPos = player.getEyePosition();

        for (int i = 1; i <= SPRAY_RANGE; i++) {
            Vec3 sprayPos = playerPos.add(lookDirection.scale(i));

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        sprayPos.x, sprayPos.y, sprayPos.z,
                        5, // количество частиц
                        0.3, 0.3, 0.3, // разброс
                        0.1 // скорость
                );
            }
        }

        AABB sprayArea = new AABB(
                playerPos.x - 1, playerPos.y - 1, playerPos.z - 1,
                playerPos.x + 1, playerPos.y + 1, playerPos.z + 1
        ).expandTowards(lookDirection.scale(SPRAY_RANGE));

        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, sprayArea,
                entity -> entity instanceof Monster && entity != player);

        for (LivingEntity target : targets) {
            Vec3 toTarget = target.position().subtract(playerPos).normalize();
            double dotProduct = lookDirection.dot(toTarget);

            if (dotProduct > 0.7) {
                double distance = target.distanceTo(player);
                if (distance <= SPRAY_RANGE) {
                    target.hurt(level.damageSources().playerAttack(player), DAMAGE);

                    target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.BLINDNESS, 60, 0)); // 3 секунды слепоты
                    target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40, 1)); // 2 секунды замедления
                }
            }
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0f, 1.5f);

        itemStack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(player.getUsedItemHand()));

        int remainingUses = itemStack.getMaxDamage() - itemStack.getDamageValue();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int remainingUses = stack.getMaxDamage() - stack.getDamageValue();
        tooltip.add(Component.literal("Использований: " + remainingUses + "/" + MAX_USES));
        tooltip.add(Component.literal("Дальность: " + (int)SPRAY_RANGE + " блоков"));
        tooltip.add(Component.literal("Урон: " + DAMAGE + " ♥"));
        tooltip.add(Component.literal("ПКМ - распылить"));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isRepairable(ItemStack stack) {
        return false;
    }
}
