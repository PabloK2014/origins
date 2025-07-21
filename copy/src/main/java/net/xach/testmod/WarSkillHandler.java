package net.xach.testmod;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.xach.testmod.entity.DagestanskiBrother;
import net.xach.testmod.entity.DagestanskiRogue;
import net.xach.testmod.entity.ModEntities;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;


@Mod.EventBusSubscriber(modid = TestMod.MOD_ID)
public class WarSkillHandler {

    // Уникальные идентификаторы для модификаторов атрибутов
    private static final UUID ATTACK_MODIFIER_UUID = UUID.fromString("c4d5e6f7-a8b9-4c0d-1e2f-3a4b5c6d7e8f");
    private static final UUID ATTACK_SPEED_MODIFIER_UUID = UUID.fromString("d5e6f7a8-b9c0-4d1e-2f3a-4b5c6d7e8f9a");
    private static final UUID DEFENSE_MODIFIER_UUID = UUID.fromString("e6f7a8b9-c0d1-4e2f-3a4b-5c6d7e8f9a0b");
    private static final UUID HEALTH_REGEN_MODIFIER_UUID = UUID.fromString("f7a8b9c0-d1e2-4f3a-5b6c-7d8e9f0a1b2c");
    private static final UUID CARRY_MODIFIER_UUID = UUID.fromString("a8b9c0d1-e2f3-4a5b-6c7d-8e9f0a1b2c3d");

    // Добавить Map для отслеживания перезарядок
    private static final Map<UUID, Long> lastChanceCooldowns = new HashMap<>();
    // Добавить Map для отслеживания времени стояния
    private static final Map<UUID, Integer> fortressStandingTime = new HashMap<>();
    // Map для отслеживания призванных мобов с их временем жизни
    private static final Map<Entity, Integer> summonedMobsLifetime = new HashMap<>();

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            if (ModKeyBindings.ACTIVATE_SKILL.consumeClick()) {
                TestMod.NETWORK.send(PacketDistributor.SERVER.noArg(), new SkillActivationPacket());
            }
            if (ModKeyBindings.OPEN_ACTIVE_SKILL_MENU.consumeClick()) {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.player != null) {
                    minecraft.player.getCapability(TestMod.PlayerClassCapability.CAPABILITY).ifPresent(cap -> {
                        // Проверяем, есть ли у игрока активные или глобальные навыки
                        List<SkillTreeHandler.Skill> availableSkills = SkillTreeHandler.CLASS_SKILL_TREES.getOrDefault(cap.getPlayerClass(), null)
                                .getAllSkills().stream()
                                .filter(skill -> (skill.getType() == SkillTreeHandler.SkillType.ACTIVE || skill.getType() == SkillTreeHandler.SkillType.GLOBAL) && cap.getSkillLevel(skill.getId()) > 0)
                                .toList();
                        if (!availableSkills.isEmpty()) {
                            minecraft.setScreen(new ActiveSkillSelectionScreen(
                                    new ActiveSkillSelectionMenu(0, minecraft.player.getInventory()),
                                    minecraft.player.getInventory(),
                                    Component.literal("Выбор активного навыка")
                            ));
                        }
                    });
                }
            }
        }
    }

    // Основной тик для обработки пассивных и активных навыков
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) return;

        Player player = event.player;
        player.getCapability(TestMod.PlayerClassCapability.CAPABILITY).ifPresent(cap -> {
            if (!cap.getPlayerClass().equals("war")) return;

            // Ветка Берсерка
            int berserkLevel = cap.getSkillLevel("berserk_way");
            if (berserkLevel > 0 && player.getHealth() <= player.getMaxHealth() * 0.3f) {
                applyBerserkWay(player, berserkLevel);
            }

            int bloodyWoundLevel = cap.getSkillLevel("bloody_wound");
            // Обработка кровотечения через событие атаки (см. ниже)

            // Ветка Защитника
            int ironLevel = cap.getSkillLevel("iron");
            if (ironLevel > 0) {
                applyIronWall(player, ironLevel);
            }

            int fortressLevel = cap.getSkillLevel("fortress");
            if (fortressLevel > 0) {
                UUID playerId = player.getUUID();
                boolean isStanding = Math.abs(player.getDeltaMovement().x) < 0.01 &&
                        Math.abs(player.getDeltaMovement().z) < 0.01;

                if (isStanding) {
                    int standingTime = fortressStandingTime.getOrDefault(playerId, 0) + 1;
                    fortressStandingTime.put(playerId, standingTime);

                    // Активируем эффект после 3 секунд стояния (60 тиков)
                    if (standingTime >= 60) {
                        applyFortress(player);
                    }
                } else {
                    fortressStandingTime.put(playerId, 0);
                    // Убираем эффекты при движении
                    removeFortressEffects(player);
                }
            }

            int tadjicLevel = cap.getSkillLevel("tadjic");
            if (tadjicLevel > 0) {
                applyTadjicRegen(player, tadjicLevel);
            }

            int carryLevel = cap.getSkillLevel("carry");
            if (carryLevel > 0) {
                applyCarryDamage(player, carryLevel);
            }
        });
    }

    // Тик для управления призванными мобами
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // Обновляем время жизни призванных мобов
        Iterator<Map.Entry<Entity, Integer>> iterator = summonedMobsLifetime.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Entity, Integer> entry = iterator.next();
            Entity mob = entry.getKey();
            int lifetime = entry.getValue() + 1;

            if (lifetime >= 900) { // 45 секунд (20 тиков * 45)
                System.out.println("Removing summoned mob after 45 seconds: " + mob.getClass().getSimpleName());
                mob.discard(); // Удаляем моба
                iterator.remove(); // Удаляем из Map
            } else {
                entry.setValue(lifetime);
            }
        }
    }

    // Путь Берсерка: Увеличение урона и скорости атаки при низком здоровье
    private static void applyBerserkWay(Player player, int level) {
        AttributeModifier attackModifier = new AttributeModifier(
                ATTACK_MODIFIER_UUID,
                "berserk_attack_boost",
                0.1 * level, // +10% урона за уровень
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );
        AttributeModifier speedModifier = new AttributeModifier(
                ATTACK_SPEED_MODIFIER_UUID,
                "berserk_speed_boost",
                0.1 * level, // +10% скорости атаки за уровень
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );

        player.getAttribute(Attributes.ATTACK_DAMAGE).removeModifier(ATTACK_MODIFIER_UUID);
        player.getAttribute(Attributes.ATTACK_SPEED).removeModifier(ATTACK_SPEED_MODIFIER_UUID);
        player.getAttribute(Attributes.ATTACK_DAMAGE).addPermanentModifier(attackModifier);
        player.getAttribute(Attributes.ATTACK_SPEED).addPermanentModifier(speedModifier);
    }

    // Кровавая Рана: Шанс нанести кровотечение при атаке
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getSource().getEntity() instanceof Player player && !player.level().isClientSide()) {
            player.getCapability(TestMod.PlayerClassCapability.CAPABILITY).ifPresent(cap -> {
                if (cap.getPlayerClass().equals("war")) {
                    int bloodyWoundLevel = cap.getSkillLevel("bloody_wound");
                    if (bloodyWoundLevel > 0 && new Random().nextFloat() < 0.1 * bloodyWoundLevel) { // 10% шанс за уровень
                        LivingEntity target = event.getEntity();
                        target.addEffect(new MobEffectInstance(
                                MobEffects.POISON, // Эффект яда как аналог кровотечения
                                100, // 5 секунд
                                bloodyWoundLevel - 1,
                                false,
                                true
                        ));
                    }
                }
            });
        }
    }

    // Безумный Рывок: Активный навык
    public static void activateMadBoost(Player player, TestMod.PlayerClassCapability cap) {
        if (cap.getSkillLevel("mad_boost") > 0 && cap.getSurgeEnergy() >= 20) {
            AABB area = new AABB(player.blockPosition()).inflate(3.0);
            List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, area, e -> e != player);
            for (LivingEntity target : targets) {
                target.hurt(player.level().damageSources().playerAttack(player), 5.0f); // Урон по врагам
            }
            player.setDeltaMovement(player.getLookAngle().scale(2.0)); // Рывок вперед
            player.hurtMarked = true; // Обновляем движение
            cap.useSurgeEnergy(20);
            cap.sync((ServerPlayer) player);
        }
    }

    // Жажда Битвы: Восстановление здоровья при убийстве
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            player.getCapability(TestMod.PlayerClassCapability.CAPABILITY).ifPresent(cap -> {
                if (cap.getPlayerClass().equals("war") && cap.getSkillLevel("thirst_battle") > 0) {
                    player.heal(player.getMaxHealth() * 0.1f); // Восстанавливает 10% здоровья
                    cap.sync((ServerPlayer) player);
                }
            });
        }
    }

    // Последний Шанс: Неуязвимость при смертельном ударе
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !player.level().isClientSide()) {
            player.getCapability(TestMod.PlayerClassCapability.CAPABILITY).ifPresent(cap -> {
                if (cap.getPlayerClass().equals("war") && cap.getSkillLevel("last_chance") > 0) {
                    if (player.getHealth() - event.getAmount() <= 0) {
                        // Проверяем перезарядку (2 минуты = 120000 мс)
                        long currentTime = System.currentTimeMillis();
                        Long lastUse = lastChanceCooldowns.get(player.getUUID());

                        if (lastUse == null || currentTime - lastUse >= 120000) {
                            player.setHealth(1.0f); // Оставить 1 HP
                            player.addEffect(new MobEffectInstance(
                                    MobEffects.DAMAGE_RESISTANCE,
                                    60, // 3 секунды неуязвимости
                                    5, // Высокий уровень сопротивления
                                    false,
                                    true
                            ));
                            event.setCanceled(true); // Отменяем смертельный урон
                            lastChanceCooldowns.put(player.getUUID(), currentTime);
                            player.sendSystemMessage(Component.literal("Последний шанс активирован!"));
                            cap.sync((ServerPlayer) player);
                        }
                    }
                }
            });
        }
    }

    // Железная Стена: Уменьшение входящего урона
    private static void applyIronWall(Player player, int level) {
        AttributeModifier defenseModifier = new AttributeModifier(
                DEFENSE_MODIFIER_UUID,
                "iron_wall_defense",
                -0.02 * level, // -2% урона за уровень
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );
        player.getAttribute(Attributes.ARMOR).removeModifier(DEFENSE_MODIFIER_UUID);
        player.getAttribute(Attributes.ARMOR).addPermanentModifier(defenseModifier);
    }

    // Несокрушимость: Сопротивление урону при низком здоровье
    public static void activateIndestructibility(Player player, TestMod.PlayerClassCapability cap) {
        if (cap.getSkillLevel("indestructibility") > 0 && player.getHealth() <= player.getMaxHealth() * 0.3f && cap.getSurgeEnergy() >= 20) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.DAMAGE_RESISTANCE,
                    100, // 5 секунд
                    cap.getSkillLevel("indestructibility") - 1,
                    false,
                    true
            ));
            cap.useSurgeEnergy(20);
            cap.sync((ServerPlayer) player);
        }
    }

    // Крепость: Защита и иммунитет к отбрасыванию при стоянии на месте
    private static void applyFortress(Player player) {
        player.addEffect(new MobEffectInstance(
                MobEffects.DAMAGE_RESISTANCE,
                20, // 1 секунда (обновляется каждую секунду при стоянии)
                1, // +30% защиты
                false,
                true
        ));
        player.getAttribute(Attributes.KNOCKBACK_RESISTANCE).addPermanentModifier(
                new AttributeModifier(
                        UUID.randomUUID(),
                        "fortress_knockback_resistance",
                        1.0, // Полный иммунитет к отбрасыванию
                        AttributeModifier.Operation.ADDITION
                )
        );
    }

    // Добавить метод для удаления эффектов крепости:
    private static void removeFortressEffects(Player player) {
        // Убираем модификатор сопротивления отбрасыванию
        player.getAttribute(Attributes.KNOCKBACK_RESISTANCE).removeModifier(
                UUID.fromString("12345678-1234-5678-9abc-123456789abc")
        );
    }

    // Монобровь: Увеличение регенерации здоровья
    private static void applyTadjicRegen(Player player, int level) {
        if (player.tickCount % 20 == 0) { // Каждую секунду
            player.heal(0.02f * level * player.getMaxHealth()); // +2% регенерации за уровень
        }
    }

    // Армянская сила: Увеличение урона
    private static void applyCarryDamage(Player player, int level) {
        AttributeModifier carryModifier = new AttributeModifier(
                CARRY_MODIFIER_UUID,
                "carry_damage_boost",
                0.02 * level, // +2% урона за уровень
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );
        player.getAttribute(Attributes.ATTACK_DAMAGE).removeModifier(CARRY_MODIFIER_UUID);
        player.getAttribute(Attributes.ATTACK_DAMAGE).addPermanentModifier(carryModifier);
    }

    // Дагестанская братва: Призыв союзников (ИСПРАВЛЕННАЯ ВЕРСИЯ)
    public static void activateDagestan(Player player, TestMod.PlayerClassCapability cap) {
        System.out.println("Dagestan skill activation started for player: " + player.getName().getString());

        if (cap.getSkillLevel("dagestan") > 0 && cap.getSurgeEnergy() >= 50) {
            Level level = player.level();
            System.out.println("Skill level: " + cap.getSkillLevel("dagestan") + ", Energy: " + cap.getSurgeEnergy());

            // Призываем 3 разбойников с правильным ИИ и оружием
            for (int i = 0; i < 3; i++) {
                try {
                    System.out.println("Creating pillager " + (i + 1));
                    Pillager pillager = new Pillager(EntityType.PILLAGER, level);

                    // Размещаем разбойников вокруг игрока
                    double angle = (2 * Math.PI * i) / 3;
                    double x = player.getX() + Math.cos(angle) * 2;
                    double z = player.getZ() + Math.sin(angle) * 2;

                    pillager.setPos(x, player.getY(), z);

                    // ВАЖНО: Даем арбалет разбойнику
                    ItemStack crossbow = new ItemStack(Items.CROSSBOW);
                    pillager.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, crossbow);

                    // Полностью очищаем все цели атаки
                    pillager.goalSelector.removeAllGoals(goal -> true);
                    pillager.targetSelector.removeAllGoals(goal -> true);

                    // Добавляем только базовые цели поведения (без атаки игроков)
                    pillager.goalSelector.addGoal(0, new net.minecraft.world.entity.ai.goal.FloatGoal(pillager));
                    pillager.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.RangedCrossbowAttackGoal<>(pillager, 1.0D, 8.0F));
                    pillager.goalSelector.addGoal(8, new net.minecraft.world.entity.ai.goal.RandomStrollGoal(pillager, 0.6D));
                    pillager.goalSelector.addGoal(9, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(pillager, Player.class, 15.0F, 1.0F));
                    pillager.goalSelector.addGoal(10, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(pillager, LivingEntity.class, 15.0F));

                    // Добавляем цель атаковать ТОЛЬКО монстров (исключая других союзников)
                    pillager.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(pillager, Monster.class, true,
                            (target) -> !(target instanceof Pillager) && !(target instanceof Vindicator)));

                    // Убираем текущую цель
                    pillager.setTarget(null);

                    // Добавляем эффекты
                    pillager.addEffect(new MobEffectInstance(MobEffects.GLOWING, 900, 0)); // Подсветка на 45 сек
                    pillager.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 900, 1)); // Усиление урона
                    pillager.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 900, 0)); // Ускорение

                    boolean spawned = level.addFreshEntity(pillager);
                    if (spawned) {
                        summonedMobsLifetime.put(pillager, 0);
                        System.out.println("Added pillager to lifetime tracking");
                    }
                    System.out.println("Pillager " + (i + 1) + " spawned: " + spawned + " at " + x + ", " + player.getY() + ", " + z);
                } catch (Exception e) {
                    System.err.println("Error creating pillager " + (i + 1) + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // Призываем 2 поборников с правильным ИИ
            for (int i = 0; i < 2; i++) {
                try {
                    System.out.println("Creating vindicator " + (i + 1));
                    Vindicator vindicator = new Vindicator(EntityType.VINDICATOR, level);

                    // Размещаем поборников рядом с игроком
                    double angle = Math.PI * i; // 0 и 180 градусов
                    double x = player.getX() + Math.cos(angle) * 3;
                    double z = player.getZ() + Math.sin(angle) * 3;

                    vindicator.setPos(x, player.getY(), z);

                    // Даем топор поборнику
                    ItemStack axe = new ItemStack(Items.IRON_AXE);
                    vindicator.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, axe);

                    // Полностью очищаем все цели атаки
                    vindicator.goalSelector.removeAllGoals(goal -> true);
                    vindicator.targetSelector.removeAllGoals(goal -> true);

                    // Добавляем только базовые цели поведения
                    vindicator.goalSelector.addGoal(0, new net.minecraft.world.entity.ai.goal.FloatGoal(vindicator));
                    vindicator.goalSelector.addGoal(4, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(vindicator, 1.0D, false));
                    vindicator.goalSelector.addGoal(8, new net.minecraft.world.entity.ai.goal.RandomStrollGoal(vindicator, 0.6D));
                    vindicator.goalSelector.addGoal(9, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(vindicator, Player.class, 3.0F, 1.0F));
                    vindicator.goalSelector.addGoal(10, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(vindicator, LivingEntity.class, 8.0F));

                    // Добавляем цель атаковать ТОЛЬКО монстров
                    vindicator.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(vindicator, Monster.class, true,
                            (target) -> !(target instanceof Vindicator) && !(target instanceof Pillager)));

                    // Убираем текущую цель
                    vindicator.setTarget(null);

                    // Добавляем эффекты
                    vindicator.addEffect(new MobEffectInstance(MobEffects.GLOWING, 900, 0)); // Подсветка на 45 сек
                    vindicator.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 900, 2)); // Сильное усиление урона
                    vindicator.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 900, 1)); // Ускорение

                    boolean spawned = level.addFreshEntity(vindicator);
                    if (spawned) {
                        summonedMobsLifetime.put(vindicator, 0);
                        System.out.println("Added vindicator to lifetime tracking");
                    }
                    System.out.println("Vindicator " + (i + 1) + " spawned: " + spawned + " at " + x + ", " + player.getY() + ", " + z);
                } catch (Exception e) {
                    System.err.println("Error creating vindicator " + (i + 1) + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // Тратим энергию и отправляем сообщение
            cap.useSurgeEnergy(50);
            cap.sync((ServerPlayer) player);
            player.sendSystemMessage(Component.literal("Дагестанская братва призвана! 3 разбойника и 2 поборника готовы к бою!"));

            System.out.println("Dagestan skill completed. Total summoned mobs in tracking: " + summonedMobsLifetime.size());
        } else {
            // Сообщение об ошибке
            if (cap.getSkillLevel("dagestan") == 0) {
                player.sendSystemMessage(Component.literal("У вас нет навыка 'Дагестанская братва'!"));
                System.out.println("Player " + player.getName().getString() + " doesn't have dagestan skill");
            } else if (cap.getSurgeEnergy() < 50) {
                player.sendSystemMessage(Component.literal("Недостаточно энергии! Требуется: 50, у вас: " + cap.getSurgeEnergy()));
                System.out.println("Player " + player.getName().getString() + " doesn't have enough energy: " + cap.getSurgeEnergy());
            }
        }
    }
}
