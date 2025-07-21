package io.github.apace100.origins.skill;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.BoneMealItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class CookActiveSkillHandler {
    public static void activateCookSkill(ServerPlayerEntity player) {
        World world = player.getWorld();
        if (!(world instanceof ServerWorld serverWorld)) return;
        BlockPos center = player.getBlockPos();
        int radius = 10;
        int affected = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = center.add(dx, dy, dz);
                    BlockState state = serverWorld.getBlockState(pos);
                    if (state.getBlock() instanceof CropBlock crop && !crop.isMature(state)) {
                        // Применяем эффект костной муки (магически)
                        crop.applyGrowth(serverWorld, pos, state);
                        serverWorld.syncWorldEvent(2005, pos, 0); // Визуальный эффект костной муки
                        affected++;
                    }
                }
            }
        }
        // Снимаем ровно 2 HP (1 сердечко)
        float before = player.getHealth();
        player.damage(player.getDamageSources().generic(), 2.0f);
        if (player.getHealth() == before) {
            // Если урон не прошёл (например, бессмертие или креатив), снимаем здоровье вручную
            if (!player.isCreative() && !player.isSpectator()) {
                player.setHealth(Math.max(0.0f, player.getHealth() - 2.0f));
            }
        }
        player.sendMessage(net.minecraft.text.Text.literal("[Повар] Удобрено растений: " + affected + ", потеряно 1 сердечко").formatted(net.minecraft.util.Formatting.GREEN), false);
    }
} 