package io.github.apace100.origins.item;

import io.github.apace100.origins.block.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class TrapPlacerItem extends Item {
    public TrapPlacerItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        PlayerEntity player = context.getPlayer();
        ItemStack stack = context.getStack();

        if (!world.isClient) {
            // Place the trap under the player
            BlockPos pos = new BlockPos((int) Math.floor(player.getX()), (int) Math.floor(player.getY()) - 1, (int) Math.floor(player.getZ()));
            
            // Check if we can place the trap
            BlockState blockState = world.getBlockState(pos);
            if (blockState.isAir() || blockState.isOf(Blocks.GRASS) || blockState.isOf(Blocks.TALL_GRASS)) {
                // Place the trap block
                world.setBlockState(pos, ModBlocks.TRAP_BLOCK.getDefaultState());
                
                // Play sound
                world.playSound(null, pos, SoundEvents.BLOCK_STONE_PLACE, 
                    net.minecraft.sound.SoundCategory.BLOCKS, 1.0f, 1.0f);
                
                player.sendMessage(net.minecraft.text.Text.literal("Ловушка установлена под вами!"), true);
                
                return ActionResult.SUCCESS;
            } else {
                player.sendMessage(net.minecraft.text.Text.literal("Нельзя установить ловушку здесь!"), true);
                return ActionResult.FAIL;
            }
        }

        return ActionResult.SUCCESS;
    }
}