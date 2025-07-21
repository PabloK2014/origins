package net.xach.testmod;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;



@Mod.EventBusSubscriber(modid = TestMod.MOD_ID)
public class ClassEffectHandler {


    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide()) {
            Player player = event.player;
            player.getCapability(TestMod.PlayerClassCapability.CAPABILITY).ifPresent(cap -> {
                String playerClass = cap.getPlayerClass();
                if (!playerClass.isEmpty()) {
                    if (player.tickCount % 20 == 0) { // Every second (20 ticks)
                        cap.addSurgeEnergy(1);
                        cap.sync((ServerPlayer) player);
                    }
                    if (Config.classEffects == null) {

                        return;
                    }
                    var effects = Config.classEffects.get(playerClass);
                    if (effects != null && !effects.isEmpty()) {

                        for (Config.EffectConfig effectConfig : effects) {
                            player.addEffect(new MobEffectInstance(
                                    effectConfig.effect,
                                    effectConfig.duration,
                                    effectConfig.level - 1));
                        }
                    } else {

                    }
                }
            });
        }
    }
}