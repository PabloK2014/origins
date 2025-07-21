package net.xach.testmod;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.brewing.PotionBrewEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;

@Mod.EventBusSubscriber(modid = TestMod.MOD_ID)
public class PivoSkillHandler {

    // Карты для отслеживания перезарядок навыков
    private static final Map<UUID, Long> masterBrewerCooldowns = new HashMap<>();
    private static final Map<UUID, Long> bottleThrowCooldowns = new HashMap<>();
    private static final Map<UUID, Long> berserkerDrinkCooldowns = new HashMap<>();
    private static final Map<UUID, Long> healingAleCooldowns = new HashMap<>();
    private static final Map<UUID, Long> partyTimeCooldowns = new HashMap<>();

    // Карта для отслеживания времени действия берсерка
    private static final Map<UUID, Long> berserkerEndTimes = new HashMap<>();

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            if (ModKeyBindings.ACTIVATE_SKILL.consumeClick()) {
                TestMod.NETWORK.send(PacketDistributor.SERVER.noArg(), new SkillActivationPacket());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) return;

        Player player = event.player;
        player.getCapability(TestMod.PlayerClassCapability.CAPABILITY).ifPresent(cap -> {
            if (!cap.getPlayerClass().equals("pivo")) return;

            // Групповой бафф - постоянно действует
            int groupBuffLevel = cap.getSkillLevel("group_buff");
            if (groupBuffLevel > 0) {
                applyGroupBuff(player, groupBuffLevel);
            }

            // Пьяная сила - увеличение урона при низком здоровье
            int drunkStrengthLevel = cap.getSkillLevel("drunk_strength");
            if (drunkStrengthLevel > 0 && player.getHealth() <= player.getMaxHealth() * 0.5f) {
                applyDrunkStrength(player, drunkStrengthLevel);
            }

            // Проверяем окончание эффекта берсерка
            UUID playerId = player.getUUID();
            if (berserkerEndTimes.containsKey(playerId)) {
                long currentTime = System.currentTimeMillis();
                if (currentTime > berserkerEndTimes.get(playerId)) {
                    berserkerEndTimes.remove(playerId);
                    // Убираем эффекты берсерка
                    player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
                    player.removeEffect(MobEffects.DAMAGE_BOOST);
                    player.removeEffect(MobEffects.MOVEMENT_SPEED);
                    player.sendSystemMessage(Component.literal("Эффект напитка берсерка закончился"));
                }
            }
        });
    }

    // Обработка варки зелий
    @SubscribeEvent
    public static void onPotionBrew(PotionBrewEvent.Pre event) {
        // Этот метод вызывается перед варкой зелья
        // Здесь можно добавить логику для ускорения варки, но это требует более сложной реализации
    }

    @SubscribeEvent
    public static void onPotionBrewPost(PotionBrewEvent.Post event) {
        // Обработка после варки зелья для навыка "Двойная варка"
        // Требует доступ к игроку, что сложно в этом событии
    }

    // Сопротивление негативным эффектам
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(TestMod.PlayerClassCapability.CAPABILITY).ifPresent(cap -> {
                if (cap.getPlayerClass().equals("pivo")) {
                    int resistanceLevel = cap.getSkillLevel("alcohol_resistance");
                    if (resistanceLevel > 0) {
                        // Уменьшаем урон от негативных эффектов на 10% за уровень
                        if (player.hasEffect(MobEffects.POISON) || player.hasEffect(MobEffects.WITHER) ||
                                player.hasEffect(MobEffects.HUNGER) || player.hasEffect(MobEffects.WEAKNESS)) {
                            float reduction = 0.1f * resistanceLevel;
                            event.setAmount(event.getAmount() * (1.0f - reduction));
                        }
                    }
                }
            });
        }
    }

    // Активные навыки
    public static void activateMasterBrewer(Player player, TestMod.PlayerClassCapability cap) {
        if (cap.getSkillLevel("master_brewer") > 0 && cap.getSurgeEnergy() >= 40) {
            UUID playerId = player.getUUID();
            long currentTime = System.currentTimeMillis();
            Long lastUse = masterBrewerCooldowns.get(playerId);

            if (lastUse == null || currentTime - lastUse >= 300000) { // 5 минут перезарядка
                createSpecialDrinks(player);
                cap.useSurgeEnergy(40);
                cap.sync((ServerPlayer) player);
                masterBrewerCooldowns.put(playerId, currentTime);
                player.sendSystemMessage(Component.literal("Мастер-пивовар создал особые напитки!"));
            } else {
                long remainingCooldown = 300000 - (currentTime - lastUse);
                player.sendSystemMessage(Component.literal("Перезарядка: " + (remainingCooldown / 1000) + " сек"));
            }
        }
    }

    public static void activateBottleThrow(Player player, TestMod.PlayerClassCapability cap) {
        if (cap.getSkillLevel("bottle_throw") > 0 && cap.getSurgeEnergy() >= 25) {
            UUID playerId = player.getUUID();
            long currentTime = System.currentTimeMillis();
            Long lastUse = bottleThrowCooldowns.get(playerId);

            if (lastUse == null || currentTime - lastUse >= 30000) { // 30 секунд перезарядка
                throwExplosiveBottle(player);
                cap.useSurgeEnergy(25);
                cap.sync((ServerPlayer) player);
                bottleThrowCooldowns.put(playerId, currentTime);
                player.sendSystemMessage(Component.literal("Взрывная бутылка брошена!"));
            } else {
                long remainingCooldown = 30000 - (currentTime - lastUse);
                player.sendSystemMessage(Component.literal("Перезарядка: " + (remainingCooldown / 1000) + " сек"));
            }
        }
    }

    public static void activateBerserkerDrink(Player player, TestMod.PlayerClassCapability cap) {
        if (cap.getSkillLevel("berserker_drink") > 0 && cap.getSurgeEnergy() >= 60) {
            UUID playerId = player.getUUID();
            long currentTime = System.currentTimeMillis();
            Long lastUse = berserkerDrinkCooldowns.get(playerId);

            if (lastUse == null || currentTime - lastUse >= 600000) { // 10 минут перезарядка
                applyBerserkerDrink(player);
                cap.useSurgeEnergy(60);
                cap.sync((ServerPlayer) player);
                berserkerDrinkCooldowns.put(playerId, currentTime);
                berserkerEndTimes.put(playerId, currentTime + 30000); // 30 секунд действия
                player.sendSystemMessage(Component.literal("Напиток берсерка выпит! Ярость на 30 секунд!"));
            } else {
                long remainingCooldown = 600000 - (currentTime - lastUse);
                player.sendSystemMessage(Component.literal("Перезарядка: " + (remainingCooldown / 1000) + " сек"));
            }
        }
    }

    public static void activateHealingAle(Player player, TestMod.PlayerClassCapability cap) {
        if (cap.getSkillLevel("healing_ale") > 0 && cap.getSurgeEnergy() >= 35) {
            UUID playerId = player.getUUID();
            long currentTime = System.currentTimeMillis();
            Long lastUse = healingAleCooldowns.get(playerId);

            if (lastUse == null || currentTime - lastUse >= 120000) { // 2 минуты перезарядка
                healNearbyAllies(player);
                cap.useSurgeEnergy(35);
                cap.sync((ServerPlayer) player);
                healingAleCooldowns.put(playerId, currentTime);
                player.sendSystemMessage(Component.literal("Лечебный эль роздан союзникам!"));
            } else {
                long remainingCooldown = 120000 - (currentTime - lastUse);
                player.sendSystemMessage(Component.literal("Перезарядка: " + (remainingCooldown / 1000) + " сек"));
            }
        }
    }

    public static void activatePartyTime(Player player, TestMod.PlayerClassCapability cap) {
        if (cap.getSkillLevel("party_time") > 0 && cap.getSurgeEnergy() >= 80) {
            UUID playerId = player.getUUID();
            long currentTime = System.currentTimeMillis();
            Long lastUse = partyTimeCooldowns.get(playerId);

            if (lastUse == null || currentTime - lastUse >= 900000) { // 15 минут перезарядка
                startPartyTime(player);
                cap.useSurgeEnergy(80);
                cap.sync((ServerPlayer) player);
                partyTimeCooldowns.put(playerId, currentTime);
                player.sendSystemMessage(Component.literal("ВРЕМЯ ВЕЧЕРИНКИ! Все получают мощные баффы!"));
            } else {
                long remainingCooldown = 900000 - (currentTime - lastUse);
                player.sendSystemMessage(Component.literal("Перезарядка: " + (remainingCooldown / 1000) + " сек"));
            }
        }
    }

    // Вспомогательные методы
    private static void applyGroupBuff(Player player, int level) {
        if (player.tickCount % 100 == 0) { // Каждые 5 секунд
            AABB area = new AABB(player.blockPosition()).inflate(5.0 + level); // Радиус увеличивается с уровнем
            List<Player> nearbyPlayers = player.level().getEntitiesOfClass(Player.class, area,
                    p -> p != player && p.distanceTo(player) <= 5.0 + level);

            for (Player ally : nearbyPlayers) {
                // Даём небольшие баффы союзникам
                ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 0, true, false));
                if (level >= 2) {
                    ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120, 0, true, false));
                }
                if (level >= 3) {
                    ally.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 0, true, false));
                }
            }
        }
    }

    private static void applyDrunkStrength(Player player, int level) {
        // Увеличиваем урон при низком здоровье
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20, level - 1, true, false));
    }

    private static void createSpecialDrinks(Player player) {
        // Создаём особые алкогольные напитки
        ItemStack vodka = new ItemStack(Items.POTION);
        PotionUtils.setPotion(vodka, Potions.STRENGTH);
        vodka.setHoverName(Component.literal("Водка Пивовара"));

        ItemStack beer = new ItemStack(Items.POTION);
        PotionUtils.setPotion(beer, Potions.REGENERATION);
        beer.setHoverName(Component.literal("Лечебное пиво"));

        ItemStack whiskey = new ItemStack(Items.POTION);
        PotionUtils.setPotion(whiskey, Potions.FIRE_RESISTANCE);
        whiskey.setHoverName(Component.literal("Огненный виски"));

        // Добавляем в инвентарь или дропаем
        if (!player.getInventory().add(vodka)) {
            player.drop(vodka, false);
        }
        if (!player.getInventory().add(beer)) {
            player.drop(beer, false);
        }
        if (!player.getInventory().add(whiskey)) {
            player.drop(whiskey, false);
        }
    }

    private static void throwExplosiveBottle(Player player) {
        Level level = player.level();

        // Создаём взрывное зелье
        ItemStack explosivePotion = new ItemStack(Items.SPLASH_POTION);
        PotionUtils.setPotion(explosivePotion, Potions.HARMING);

        ThrownPotion thrownPotion = new ThrownPotion(level, player);
        thrownPotion.setItem(explosivePotion);
        thrownPotion.shootFromRotation(player, player.getXRot(), player.getYRot(), -20.0F, 0.5F, 1.0F);

        level.addFreshEntity(thrownPotion);
    }

    private static void applyBerserkerDrink(Player player) {
        // Даём мощные эффекты на 30 секунд
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 2)); // Сопротивление урону III
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 3)); // Сила IV
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 2)); // Скорость III
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 1)); // Регенерация II
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 600, 0)); // Огнестойкость
    }

    private static void healNearbyAllies(Player player) {
        AABB area = new AABB(player.blockPosition()).inflate(8.0);
        List<Player> nearbyPlayers = player.level().getEntitiesOfClass(Player.class, area,
                p -> p != player && p.distanceTo(player) <= 8.0);

        for (Player ally : nearbyPlayers) {
            ally.heal(ally.getMaxHealth() * 0.5f); // Лечим на 50% от максимального здоровья
            ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 2)); // Регенерация III на 10 сек
            ally.sendSystemMessage(Component.literal("Вы получили лечебный эль от " + player.getName().getString()));
        }

        // Лечим и самого пивовара
        player.heal(player.getMaxHealth() * 0.3f);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
    }

    private static void startPartyTime(Player player) {
        AABB area = new AABB(player.blockPosition()).inflate(15.0); // Большой радиус
        List<Player> nearbyPlayers = player.level().getEntitiesOfClass(Player.class, area,
                p -> p.distanceTo(player) <= 15.0);

        for (Player ally : nearbyPlayers) {
            // Мощные баффы для всех в радиусе
            ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200, 2)); // Сила III на 1 минуту
            ally.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1200, 2)); // Скорость III
            ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 1200, 1)); // Регенерация II
            ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1200, 1)); // Сопротивление II
            ally.addEffect(new MobEffectInstance(MobEffects.LUCK, 1200, 2)); // Удача III
            ally.addEffect(new MobEffectInstance(MobEffects.SATURATION, 1200, 0)); // Насыщение

            ally.sendSystemMessage(Component.literal("🍺 ВРЕМЯ ВЕЧЕРИНКИ! 🍺 Получены мощные баффы!"));
        }
    }
}
