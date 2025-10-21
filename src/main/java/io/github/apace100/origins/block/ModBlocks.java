package io.github.apace100.origins.block;

import io.github.apace100.origins.Origins;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registry;

public class ModBlocks {
    public static final TrapBlock TRAP_BLOCK = new TrapBlock(Block.Settings.create().strength(0.1f).noCollision());

    public static void register() {
        // Регистрируем блок
        Registry.register(Registries.BLOCK, new Identifier(Origins.MODID, "trap_block"), TRAP_BLOCK);
        // Регистрируем Item-представление блока
        Registry.register(Registries.ITEM, new Identifier(Origins.MODID, "trap_block"), new BlockItem(TRAP_BLOCK, new Item.Settings()));
    }
    
    public static class TrapBlock extends Block {
        private static final VoxelShape SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 1.0, 16.0);

        public TrapBlock(Settings settings) {
            super(settings);
        }

        @Override
        public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
            return SHAPE;
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
            return VoxelShapes.empty(); // Невидимая для коллизий
        }

        @Override
        public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
            if (!world.isClient && entity instanceof LivingEntity livingEntity) {
                // Активируем ловушку для любого живого существа (не только монстров)
                System.out.println("Trap triggered by: " + entity.getName().getString() + " at " + pos);

                // Наносим урон (5 единиц = 2.5 сердца)
                livingEntity.damage(world.getDamageSources().magic(), 5.0f);

                // Применяем эффекты
                livingEntity.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                        net.minecraft.entity.effect.StatusEffects.BLINDNESS,
                        60, // 3 секунды
                        0,  // Базовый уровень слепоты
                        false,
                        true
                ));

                // Звуковой эффект
                world.playSound(null, pos, net.minecraft.sound.SoundEvents.BLOCK_TRIPWIRE_CLICK_ON, 
                    net.minecraft.sound.SoundCategory.BLOCKS, 0.5f, 1.0f);

                // Удаляем ловушку сразу после активации
                world.removeBlock(pos, false);
            }
            super.onEntityCollision(state, world, pos, entity);
        }

        public float getHardness(BlockState state, BlockView world, BlockPos pos) {
            return 0.1f; // Мгновенно ломается
        }
    }
}