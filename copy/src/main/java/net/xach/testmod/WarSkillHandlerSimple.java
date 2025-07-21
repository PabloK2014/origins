package net.xach.testmod;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

// Временная простая версия для тестирования
public class WarSkillHandlerSimple {

    // Простая версия навыка для тестирования
    public static void activateDagestaSimple(Player player, TestMod.PlayerClassCapability cap) {
        if (cap.getSkillLevel("dagestan") > 0 && cap.getSurgeEnergy() >= 50) {
            Level level = player.level();
            System.out.println("Simple Dagestan activation for: " + player.getName().getString());

            // Призываем обычных волков как тест
            for (int i = 0; i < 3; i++) {
                try {
                    Wolf wolf = new Wolf(EntityType.WOLF, level);

                    double angle = (2 * Math.PI * i) / 3;
                    double x = player.getX() + Math.cos(angle) * 2;
                    double z = player.getZ() + Math.sin(angle) * 2;

                    wolf.setPos(x, player.getY(), z);
                    wolf.setTame(true);
                    wolf.setOwnerUUID(player.getUUID());
                    wolf.addEffect(new MobEffectInstance(MobEffects.GLOWING, 900, 0));

                    boolean spawned = level.addFreshEntity(wolf);
                    System.out.println("Wolf " + (i + 1) + " spawned: " + spawned);
                } catch (Exception e) {
                    System.err.println("Error spawning wolf: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            cap.useSurgeEnergy(50);
            cap.sync((ServerPlayer) player);
            player.sendSystemMessage(Component.literal("Простая версия братвы призвана! (3 волка для теста)"));
        }
    }
}
