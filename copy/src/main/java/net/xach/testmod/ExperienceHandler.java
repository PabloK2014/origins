package net.xach.testmod;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TestMod.MOD_ID)
public class ExperienceHandler {

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player && !player.level().isClientSide()) {
            player.getCapability(TestMod.PlayerClassCapability.CAPABILITY).ifPresent(cap -> {
                if (!cap.getPlayerClass().isEmpty()) {
                    cap.addExperience(10);
                    cap.sync(player);
                }
            });
        }
    }
}