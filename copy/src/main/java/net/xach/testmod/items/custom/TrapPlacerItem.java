package net.xach.testmod.items.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.xach.testmod.block.TestModBlocks;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TrapPlacerItem extends Item {
    private static final int MAX_USES = 5;

    public TrapPlacerItem(Properties properties) {
        super(properties.stacksTo(1).durability(MAX_USES));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos().above();
        Player player = context.getPlayer();
        ItemStack itemStack = context.getItemInHand();

        if (!level.isClientSide() && player != null) {
            // Проверяем, можно ли установить ловушку
            if (level.getBlockState(pos).isAir() || level.getBlockState(pos).is(Blocks.GRASS) ||
                    level.getBlockState(pos).is(Blocks.TALL_GRASS)) {

                // Устанавливаем ловушку
                level.setBlock(pos, TestModBlocks.TRAP_BLOCK.get().defaultBlockState(), 3);

                // Звук установки
                level.playSound(null, pos, SoundEvents.TRIPWIRE_ATTACH, SoundSource.BLOCKS, 0.7f, 1.2f);

                // Уменьшаем прочность
                itemStack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(context.getHand()));

                // Сообщение игроку
                int remainingUses = itemStack.getMaxDamage() - itemStack.getDamageValue();
                player.sendSystemMessage(Component.literal("Ловушка установлена! Осталось: " + remainingUses));

                return InteractionResult.SUCCESS;
            } else {
                player.sendSystemMessage(Component.literal("Нельзя установить ловушку здесь!"));
                return InteractionResult.FAIL;
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            // Устанавливаем ловушку под игроком
            BlockPos pos = player.blockPosition();

            if (level.getBlockState(pos).isAir() || level.getBlockState(pos).is(Blocks.GRASS) ||
                    level.getBlockState(pos).is(Blocks.TALL_GRASS)) {

                level.setBlock(pos, TestModBlocks.TRAP_BLOCK.get().defaultBlockState(), 3);
                level.playSound(null, pos, SoundEvents.TRIPWIRE_ATTACH, SoundSource.BLOCKS, 0.7f, 1.2f);

                itemStack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(hand));

                int remainingUses = itemStack.getMaxDamage() - itemStack.getDamageValue();
                player.sendSystemMessage(Component.literal("Ловушка установлена под вами! Осталось: " + remainingUses));

                return InteractionResultHolder.success(itemStack);
            } else {
                player.sendSystemMessage(Component.literal("Нельзя установить ловушку здесь!"));
                return InteractionResultHolder.fail(itemStack);
            }
        }

        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int remainingUses = stack.getMaxDamage() - stack.getDamageValue();
        tooltip.add(Component.literal("Использований: " + remainingUses + "/" + MAX_USES));
        tooltip.add(Component.literal("ПКМ - установить ловушку"));
        tooltip.add(Component.literal("ПКМ по блоку - установить на блок"));
        tooltip.add(Component.literal("Оглушает мобов на 2 секунды"));
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
