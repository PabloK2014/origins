package io.github.apace100.origins.quest;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BountyBoard extends BlockWithEntity {

    public BountyBoard(Settings settings) {
        super(settings);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof BountyBoardBlockEntity board) {
                
                // Проверяем, держит ли игрок Shift и билет квеста
                if (player.isSneaking()) {
                    net.minecraft.item.ItemStack heldItem = player.getStackInHand(hand);
                    if (QuestTicketItem.isQuestTicket(heldItem)) {
                        // Пытаемся завершить квест
                        QuestTicketAcceptanceHandler handler = QuestTicketAcceptanceHandler.getInstance();
                        boolean completed = handler.completeQuestAtBoard(player, heldItem, board);
                        
                        return completed ? ActionResult.SUCCESS : ActionResult.FAIL;
                    }
                }
                
                // Принудительно инициализируем доску если нужно
                board.tryInitialPopulation();
                
                // Если квестов все еще нет, принудительно регенерируем
                if (board.getQuestCount() == 0) {
                    board.forceRegenerateQuests();
                }
                
                // Обычное открытие интерфейса доски
                player.openHandledScreen(board);
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new BountyBoardBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return checkType(type, QuestRegistry.BOUNTY_BOARD_BLOCK_ENTITY, BountyBoardBlockEntity::tick);
    }
} 