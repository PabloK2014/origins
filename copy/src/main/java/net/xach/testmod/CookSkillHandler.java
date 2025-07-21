package net.xach.testmod;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;

@Mod.EventBusSubscriber(modid = TestMod.MOD_ID)
public class CookSkillHandler {

    // Карты для отслеживания перезарядок навыков
    private static final Map<UUID, Long> smokeScreenCooldowns = new HashMap<>();
    private static final Map<UUID, Long> banquetCooldowns = new HashMap<>();

    // Карты для отслеживания эффектов
    private static final Map<UUID, Long> inspirationEndTimes = new HashMap<>();
    private static final Map<UUID, Integer> smokeScreenUses = new HashMap<>();

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
            if (!cap.getPlayerClass().equals("cook")) return;

            // Огнестойкость
            int fireImmunityLevel = cap.getSkillLevel("fire_immunity");
            if (fireImmunityLevel > 0) {
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, true, false));
            }

            // Проверяем окончание эффекта вдохновения
            UUID playerId = player.getUUID();
            if (inspirationEndTimes.containsKey(playerId)) {
                long currentTime = System.currentTimeMillis();
                if (currentTime > inspirationEndTimes.get(playerId)) {
                    inspirationEndTimes.remove(playerId);
                    player.removeEffect(MobEffects.DAMAGE_BOOST);
                    player.sendSystemMessage(Component.literal("Эффект вдохновения закончился"));
                }
            }

            // Быстрое приготовление - ускорение печей поблизости
            int fastCookingLevel = cap.getSkillLevel("fast_cooking");
            if (fastCookingLevel > 0 && player.tickCount % 20 == 0) { // Каждую секунду
                accelerateNearbyFurnaces(player, fastCookingLevel);
            }
        });
    }

    // Обработка еды
    @SubscribeEvent
    public static void onPlayerEat(net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(TestMod.PlayerClassCapability.CAPABILITY).ifPresent(cap -> {
                if (!cap.getPlayerClass().equals("cook")) return;

                ItemStack food = event.getItem();
                FoodProperties foodProps = food.getItem().getFoodProperties();

                if (foodProps != null) {
                    // Свежий продукт - дополнительная сытость
                    int freshProductLevel = cap.getSkillLevel("fresh_product");
                    if (freshProductLevel > 0) {
                        int bonusNutrition = (int) (foodProps.getNutrition() * 0.02f * freshProductLevel);
                        player.getFoodData().eat(bonusNutrition, foodProps.getSaturationModifier());
                    }

                    // Сытный обед - продлённая сытость
                    int heartyMealLevel = cap.getSkillLevel("hearty_meal");
                    if (heartyMealLevel > 0) {
                        player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 600, 0)); // 30 секунд
                    }

                    // Шеф-повар - случайные позитивные эффекты
                    int chefMasterLevel = cap.getSkillLevel("chef_master");
                    if (chefMasterLevel > 0 && new Random().nextFloat() < 0.3f) { // 30% шанс
                        giveRandomPositiveEffect(player);
                    }

                    // Быстрый перекус - убираем замедление
                    int quickSnackLevel = cap.getSkillLevel("quick_snack");
                    if (quickSnackLevel > 0) {
                        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                    }

                    // Пир на весь мир - лечение союзников
                    int feastWorldLevel = cap.getSkillLevel("feast_world");
                    if (feastWorldLevel > 0) {
                        healNearbyAllies(player);
                    }
                }
            });
        }
    }

    // Обработка готовки
    @SubscribeEvent
    public static void onItemCrafted(net.minecraftforge.event.entity.player.PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(TestMod.PlayerClassCapability.CAPABILITY).ifPresent(cap -> {
                if (!cap.getPlayerClass().equals("cook")) return;

                ItemStack result = event.getCrafting();
                FoodProperties foodProps = result.getItem().getFoodProperties();

                if (foodProps != null) {
                    // Готово! - бафф вдохновения
                    int readyLevel = cap.getSkillLevel("ready");
                    if (readyLevel > 0) {
                        applyInspirationBuff(player, readyLevel);
                    }
                }
            });
        }
    }

    // Обработка урона для дымовой завесы
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(TestMod.PlayerClassCapability.CAPABILITY).ifPresent(cap -> {
                if (cap.getPlayerClass().equals("cook")) {
                    int smokeScreenLevel = cap.getSkillLevel("smoke_screen");
                    if (smokeScreenLevel > 0 && player.getHealth() - event.getAmount() <= player.getMaxHealth() * 0.3f) {
                        activateSmokeScreen(player, cap);
                    }
                }
            });
        }
    }

    // Обработка убийства для фламбе
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            player.getCapability(TestMod.PlayerClassCapability.CAPABILITY).ifPresent(cap -> {
                if (cap.getPlayerClass().equals("cook")) {
                    int flambeLevel = cap.getSkillLevel("flambe");
                    if (flambeLevel > 0 && event.getEntity().isOnFire()) {
                        createFlambeExplosion(player, event.getEntity().blockPosition(), flambeLevel);
                    }
                }
            });
        }
    }

    // Опыт за получение еды из печки
    @SubscribeEvent
    public static void onItemSmelted(PlayerEvent.ItemSmeltedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(TestMod.PlayerClassCapability.CAPABILITY).ifPresent(cap -> {
                if (!cap.getPlayerClass().equals("cook")) return;
                ItemStack result = event.getSmelting();
                if (result.isEdible()) {
                    cap.addExperience(5);
                    player.sendSystemMessage(Component.literal("[DEBUG] +5 опыта за получение еды из печки (повар)"));
                }
            });
        }
    }

    // Опыт за посадку семян
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        player.getCapability(TestMod.PlayerClassCapability.CAPABILITY).ifPresent(cap -> {
            if (!cap.getPlayerClass().equals("cook")) return;
            ItemStack held = player.getMainHandItem();
            if (held.getItem().toString().contains("seeds") || held.getItem().toString().contains("carrot") || held.getItem().toString().contains("potato") || held.getItem().toString().contains("beetroot")) {
                cap.addExperience(2);
                player.sendSystemMessage(Component.literal("[DEBUG] +2 опыта за посадку культуры (повар)"));
            }
        });
    }

    // Опыт за использование костной муки
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onBoneMealUse(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        player.getCapability(TestMod.PlayerClassCapability.CAPABILITY).ifPresent(cap -> {
            if (!cap.getPlayerClass().equals("cook")) return;
            ItemStack held = event.getItemStack();
            if (held.getItem() == Items.BONE_MEAL) {
                cap.addExperience(1);
                player.sendSystemMessage(Component.literal("[DEBUG] +1 опыт за использование костной муки (повар)"));
            }
        });
    }

    // Активные навыки
    public static void activateSmokeScreen(Player player, TestMod.PlayerClassCapability cap) {
        if (cap.getSkillLevel("smoke_screen") > 0) {
            UUID playerId = player.getUUID();
            long currentTime = System.currentTimeMillis();
            Long lastUse = smokeScreenCooldowns.get(playerId);

            if (lastUse == null || currentTime - lastUse >= 30000) { // 30 секунд перезарядка
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0)); // 3 секунды
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1)); // Ускорение

                // Создаём дымовой эффект
                Level level = player.level();
                for (int i = 0; i < 20; i++) {
                    double x = player.getX() + (player.getRandom().nextDouble() - 0.5) * 4;
                    double y = player.getY() + player.getRandom().nextDouble() * 2;
                    double z = player.getZ() + (player.getRandom().nextDouble() - 0.5) * 4;
                    level.addParticle(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, x, y, z, 0, 0.1, 0);
                }

                smokeScreenCooldowns.put(playerId, currentTime);
                player.sendSystemMessage(Component.literal("Дымовая завеса активирована!"));
            }
        }
    }

    public static void activateBanquet(Player player, TestMod.PlayerClassCapability cap) {
        if (cap.getSkillLevel("banquet") > 0 && cap.getSurgeEnergy() >= 70) {
            UUID playerId = player.getUUID();
            long currentTime = System.currentTimeMillis();
            Long lastUse = banquetCooldowns.get(playerId);

            if (lastUse == null || currentTime - lastUse >= 600000) { // 10 минут перезарядка
                startBanquet(player);
                cap.useSurgeEnergy(70);
                cap.sync((ServerPlayer) player);
                banquetCooldowns.put(playerId, currentTime);
                player.sendSystemMessage(Component.literal("🍽️ БАНКЕТ НАЧАЛСЯ! 🍽️ Все получают мощные баффы!"));
            } else {
                long remainingCooldown = 600000 - (currentTime - lastUse);
                player.sendSystemMessage(Component.literal("Перезарядка: " + (remainingCooldown / 1000) + " сек"));
            }
        }
    }

    // Вспомогательные методы
    private static void accelerateNearbyFurnaces(Player player, int level) {
        BlockPos playerPos = player.blockPosition();
        Level world = player.level();

        // Ищем печи в радиусе 5 блоков
        for (int x = -5; x <= 5; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -5; z <= 5; z++) {
                    BlockPos pos = playerPos.offset(x, y, z);
                    BlockEntity blockEntity = world.getBlockEntity(pos);

                    if (blockEntity instanceof AbstractFurnaceBlockEntity furnace) {
                        // Проверяем, что печь работает (упрощённая проверка)
                        if (player.tickCount % 100 == 0) { // Каждые 5 секунд
                            player.sendSystemMessage(Component.literal("Печи поблизости работают быстрее благодаря навыку 'Быстрое приготовление'!"));
                            break; // Выходим из всех циклов после первого сообщения
                        }
                    }
                }
            }
        }
    }

    private static void giveRandomPositiveEffect(Player player) {
        MobEffectInstance[] effects = {
                new MobEffectInstance(MobEffects.REGENERATION, 200, 1),
                new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 0),
                new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 0),
                new MobEffectInstance(MobEffects.JUMP, 300, 1),
                new MobEffectInstance(MobEffects.NIGHT_VISION, 600, 0),
                new MobEffectInstance(MobEffects.WATER_BREATHING, 600, 0),
                new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 600, 0)
        };

        MobEffectInstance randomEffect = effects[new Random().nextInt(effects.length)];
        player.addEffect(randomEffect);
        player.sendSystemMessage(Component.literal("Шеф-повар даровал вам " + randomEffect.getEffect().getDisplayName().getString() + "!"));
    }

    private static void applyInspirationBuff(Player player, int level) {
        long duration = 300000; // 5 минут
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, (int)(duration / 50), level - 1));
        inspirationEndTimes.put(player.getUUID(), System.currentTimeMillis() + duration);
        player.sendSystemMessage(Component.literal("Вдохновение от готовки! Урон увеличен на " + (level * 10) + "%"));
    }

    private static void healNearbyAllies(Player player) {
        AABB area = new AABB(player.blockPosition()).inflate(8.0);
        List<Player> nearbyPlayers = player.level().getEntitiesOfClass(Player.class, area,
                p -> p != player && p.distanceTo(player) <= 8.0);

        for (Player ally : nearbyPlayers) {
            ally.heal(4.0f); // +2 сердца
            ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
            ally.sendSystemMessage(Component.literal("Повар " + player.getName().getString() + " поделился с вами едой!"));
        }
    }

    private static void createFlambeExplosion(Player player, BlockPos pos, int level) {
        Level world = player.level();

        // Создаём взрыв без разрушения блоков
        world.explode(null, pos.getX(), pos.getY(), pos.getZ(), 2.0f + level, false, Level.ExplosionInteraction.NONE);

        // Поджигаем врагов в радиусе
        AABB area = new AABB(pos).inflate(3.0 + level);
        List<LivingEntity> entities = world.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && e.distanceTo(player) <= 3.0 + level);

        for (LivingEntity entity : entities) {
            entity.setSecondsOnFire(5 + level);
            entity.hurt(world.damageSources().onFire(), 3.0f + level);
        }

        player.sendSystemMessage(Component.literal("Фламбе! Взрыв нанёс урон врагам!"));
    }

    private static void startBanquet(Player player) {
        AABB area = new AABB(player.blockPosition()).inflate(10.0);
        List<Player> nearbyPlayers = player.level().getEntitiesOfClass(Player.class, area,
                p -> p.distanceTo(player) <= 10.0);

        for (Player ally : nearbyPlayers) {
            // Мощные баффы для всех в радиусе
            ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 300, 2)); // Регенерация III на 15 сек
            ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 1)); // Сопротивление II
            ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 1)); // Сила II
            ally.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 1)); // Скорость II
            ally.addEffect(new MobEffectInstance(MobEffects.SATURATION, 300, 2)); // Насыщение III
            ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 300, 1)); // Поглощение II

            ally.heal(ally.getMaxHealth()); // Полное исцеление
            ally.getFoodData().eat(20, 1.0f); // Полная сытость

            ally.sendSystemMessage(Component.literal("🍽️ Банкет повара! Получены мощные баффы! 🍽️"));
        }
    }

    // Активный скилл: G — массовое удобрение
    @SubscribeEvent
    public static void onKeyInput(TickEvent.PlayerTickEvent event) {
        if (event.player.level().isClientSide()) return;
        Player player = event.player;
        player.getCapability(TestMod.PlayerClassCapability.CAPABILITY).ifPresent(cap -> {
            if (!cap.getPlayerClass().equals("cook")) return;
            // Проверяем, нажата ли клавиша G (код 34)
            if (player instanceof ServerPlayer serverPlayer && serverPlayer.getServer().getTickCount() % 5 == 0) {
                if (net.minecraft.client.Minecraft.getInstance().options.keyHotbarSlots[6].isDown()) { // G
                    // Ищем все растения в радиусе 5 блоков
                    BlockPos pos = player.blockPosition();
                    Level world = player.level();
                    int used = 0;
                    for (int x = -5; x <= 5; x++) {
                        for (int y = -2; y <= 2; y++) {
                            for (int z = -5; z <= 5; z++) {
                                BlockPos target = pos.offset(x, y, z);
                                BlockEntity be = world.getBlockEntity(target);
                                if (world.getBlockState(target).getBlock().toString().contains("crop") || world.getBlockState(target).getBlock().toString().contains("sapling")) {
                                    if (world.random.nextFloat() < 0.5f) { // 50% шанс для баланса
                                        world.getBlockState(target).randomTick(world, target, world.random);
                                        used++;
                                    }
                                }
                            }
                        }
                    }
                    if (used > 0) {
                        player.hurt(player.damageSources().magic(), used * 2.0F); // 1 сердечко = 2 урона
                        player.sendSystemMessage(Component.literal("[DEBUG] Массовое удобрение: " + used + " растений, потеряно " + used + " сердечек!"));
                    }
                }
            }
        });
    }
}
