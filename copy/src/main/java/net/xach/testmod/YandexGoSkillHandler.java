package net.xach.testmod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.PacketDistributor;
import net.xach.testmod.items.TestModItems;

import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = TestMod.MOD_ID)
public class YandexGoSkillHandler {

    private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("a2b3c4d5-e6f7-4a9b-0c1d-2e3f4a5b6c7d");
    private static final UUID HUNGER_MODIFIER_UUID = UUID.fromString("b3c4d5e6-f7a8-4b9c-1d0e-3f4a5b6c7d8e");

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getServer().getCommands().getDispatcher();
        dispatcher.register(
                Commands.literal("testmod")
                        .then(Commands.literal("speed")
                                .executes(YandexGoSkillHandler::executeSpeedCommand))
                        .then(Commands.literal("class")
                                .then(Commands.literal("yandex.go")
                                        .executes(YandexGoSkillHandler::selectYandexGoClass))
                                .then(Commands.literal("war")
                                        .executes(YandexGoSkillHandler::selectWarClass))
                                .then(Commands.literal("cook")
                                        .executes(YandexGoSkillHandler::selectCookClass))
                                .then(Commands.literal("pivo")
                                        .executes(YandexGoSkillHandler::selectPivoClass)))
        );
    }

    private static int selectYandexGoClass(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            player.getCapability(TestMod.PlayerClassCapability.CAPABILITY).ifPresent(cap -> {
                cap.setPlayerClass("yandex.go");
                player.sendSystemMessage(Component.literal("Выбран класс: Курьер Yandex.Go"));
                cap.sync(player);
            });
        }
        return 1;
    }
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

    private static int selectWarClass(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            player.getCapability(TestMod.PlayerClassCapability.CAPABILITY).ifPresent(cap -> {
                cap.setPlayerClass("war");
                player.sendSystemMessage(Component.literal("Выбран класс: Воин"));
                cap.sync(player);
            });
        }
        return 1;
    }

    private static int selectCookClass(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            player.getCapability(TestMod.PlayerClassCapability.CAPABILITY).ifPresent(cap -> {
                cap.setPlayerClass("cook");
                player.sendSystemMessage(Component.literal("Выбран класс: Повар"));
                cap.sync(player);
            });
        }
        return 1;
    }

    private static int selectPivoClass(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            player.getCapability(TestMod.PlayerClassCapability.CAPABILITY).ifPresent(cap -> {
                cap.setPlayerClass("pivo");
                player.sendSystemMessage(Component.literal("Выбран класс: Пивовар"));
                cap.sync(player);
            });
        }
        return 1;
    }

    private static int executeSpeedCommand(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            player.getCapability(TestMod.PlayerClassCapability.CAPABILITY).ifPresent(cap -> {
                if (cap.getPlayerClass().equals("yandex.go")) {
                    int speedLevel = cap.getSkillLevel("speed_basic");
                    double speed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
                    player.sendSystemMessage(Component.literal(
                            "Уровень скилла speed_basic: " + speedLevel + ", Скорость: " + String.format("%.3f", speed)
                    ));
                } else {
                    player.sendSystemMessage(Component.literal("Вы не являетесь классом yandex.go"));
                }
            });
        }
        return 1;
    }



    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide()) {
            Player player = event.player;
            player.getCapability(TestMod.PlayerClassCapability.CAPABILITY).ifPresent(cap -> {
                if (!cap.getPlayerClass().equals("yandex.go")) return;

                int speedLevel = cap.getSkillLevel("speed_basic");
                if (speedLevel > 0) {
                    applySpeedBoost(player, speedLevel);
                }

                int hungerReductionLevel = cap.getSkillLevel("hunger_reduction");
                if (hungerReductionLevel > 0) {
                    applyHungerReduction(player, hungerReductionLevel);
                }

                int craftingEfficiencyLevel = cap.getSkillLevel("crafting_efficiency");
                if (craftingEfficiencyLevel > 0 && !cap.hasCraftingHandler()) {
                    applyCraftingEfficiency(player, craftingEfficiencyLevel);
                    cap.setCraftingHandler(true);
                }

                int carryCapacityLevel = cap.getSkillLevel("carry_capacity_basic");
                if (carryCapacityLevel > 0 && !cap.hasTrapHandler()) {
                    applyCarryCapacityTrap(player, carryCapacityLevel);
                    cap.setTrapHandler(true);
                }

                int shulkerCarryLevel = cap.getSkillLevel("shulker_carry");
                if (shulkerCarryLevel > 0) {
                    applyShulkerCarry(player);
                }
            });
        }
    }

    public static void applySpeedBoost(Player player, int level) {
        AttributeModifier speedModifier = new AttributeModifier(
                SPEED_MODIFIER_UUID,
                "yandex_speed_boost",
                0.05 * level,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );
        player.getAttribute(Attributes.MOVEMENT_SPEED)
                .removeModifier(SPEED_MODIFIER_UUID);
        player.getAttribute(Attributes.MOVEMENT_SPEED)
                .addPermanentModifier(speedModifier);
    }

    private static void applyHungerReduction(Player player, int level) {
        if (player.tickCount % (20 * 10) == 0) {
            player.getFoodData().addExhaustion(-0.1f * level);
        }
    }

    public static void activateSprintBoost(Player player, TestMod.PlayerClassCapability cap) {
        player.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SPEED,
                200,
                2,
                false,
                true
        ));
        cap.useSurgeEnergy(20);
        cap.sync((ServerPlayer) player);
        System.out.println("Activated Sprint Boost for player: " + player.getName().getString());
    }

    public static void activateSpeedSurge(Player player, TestMod.PlayerClassCapability cap) {
        player.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SPEED,
                600,
                3,
                false,
                true
        ));
        player.addEffect(new MobEffectInstance(
                MobEffects.SATURATION,
                600,
                1,
                false,
                true
        ));
        cap.useSurgeEnergy(50);
        cap.sync((ServerPlayer) player);
        System.out.println("Activated Speed Surge for player: " + player.getName().getString());
    }

    private static void applyCraftingEfficiency(Player player, int level) {
        if (level == 1) {
            ItemStack foodBag = new ItemStack(TestModItems.FOOD_BAG.get(), 1);
            if (player.getInventory().add(foodBag)) {
                player.sendSystemMessage(Component.literal("Получена сумка для еды!"));
            } else {
                player.sendSystemMessage(Component.literal("Инвентарь полон! Освободите место для сумки."));
            }
        }

        if (player.tickCount % (20 * 60) == 0 && level > 1) {
            ItemStack foodBag = new ItemStack(TestModItems.FOOD_BAG.get(), 1);
            if (player.getInventory().add(foodBag)) {
                player.sendSystemMessage(Component.literal("Пополнен запас сумок для еды!"));
            }
        }
    }

    public static void activateInventorySurge(Player player, TestMod.PlayerClassCapability cap) {
        if (!cap.hasTradeHandler()) {
            MinecraftForge.EVENT_BUS.register(new Object() {
                private long activationTime = System.currentTimeMillis();

                @SubscribeEvent
                public void onVillagerTrade(PlayerInteractEvent.EntityInteract event) {
                    if (System.currentTimeMillis() - activationTime > 60000) {
                        MinecraftForge.EVENT_BUS.unregister(this);
                        return;
                    }

                    if (event.getTarget() instanceof Villager villager && event.getEntity().getUUID().equals(player.getUUID())) {
                        villager.getOffers().forEach(offer -> {
                            ItemStack costA = offer.getCostA();
                            ItemStack costB = offer.getCostB();
                            if (!costA.isEmpty()) {
                                int newCountA = Math.max(1, (int) (costA.getCount() * 0.85));
                                costA.setCount(newCountA);
                            }
                            if (!costB.isEmpty()) {
                                int newCountB = Math.max(1, (int) (costB.getCount() * 0.85));
                                costB.setCount(newCountB);
                            }
                        });
                        player.sendSystemMessage(Component.literal("Применена скидка 15% на торговлю!"));
                    }
                }
            });
            cap.setTradeHandler(true);
        }

        cap.useSurgeEnergy(50);
        cap.sync((ServerPlayer) player);
        player.sendSystemMessage(Component.literal("Улыбка Курьера активирована на 60 секунд!"));
    }

    private static void applyCarryCapacityTrap(Player player, int level) {
        if (level == 1) {
            ItemStack trapPlacer = new ItemStack(TestModItems.TRAP_PLACER.get(), 1);
            if (player.getInventory().add(trapPlacer)) {
                player.sendSystemMessage(Component.literal("Получен установщик ловушек!"));
            } else {
                player.sendSystemMessage(Component.literal("Инвентарь полон! Освободите место для установщика ловушек."));
            }
        }

        if (player.tickCount % (20 * 30) == 0 && level > 1) {
            ItemStack trapPlacer = new ItemStack(TestModItems.TRAP_PLACER.get(), 1);
            if (player.getInventory().add(trapPlacer)) {
                player.sendSystemMessage(Component.literal("Пополнен запас ловушек!"));
            }
        }
    }

    private static void applyShulkerCarry(Player player) {
        if (player.tickCount % (20 * 5) == 0) {
            Level level = player.level();
            BlockPos playerPos = player.blockPosition();

            BlockPos nearestVillage = findNearestVillage(level, playerPos, 200);

            if (nearestVillage != null) {
                double distance = Math.sqrt(playerPos.distSqr(nearestVillage));

                if (distance >= 40) {
                    if (player instanceof ServerPlayer serverPlayer) {
                        TestMod.NETWORK.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                                new VillageCompassPacket(nearestVillage.getX(), nearestVillage.getZ()));
                    }
                }

                AABB searchArea = new AABB(playerPos).inflate(100);
                List<LivingEntity> villagers = level.getEntitiesOfClass(LivingEntity.class, searchArea,
                        entity -> entity instanceof Villager);

                for (LivingEntity villager : villagers) {
                    villager.addEffect(new MobEffectInstance(
                            MobEffects.GLOWING,
                            120,
                            0,
                            false,
                            false
                    ));
                }
            }
        }
    }

    private static BlockPos findNearestVillage(Level level, BlockPos playerPos, int radius) {
        AABB searchArea = new AABB(playerPos).inflate(radius);
        List<LivingEntity> villagers = level.getEntitiesOfClass(LivingEntity.class, searchArea,
                entity -> entity instanceof Villager);

        if (!villagers.isEmpty()) {
            LivingEntity nearestVillager = villagers.get(0);
            double nearestDistance = playerPos.distSqr(nearestVillager.blockPosition());

            for (LivingEntity villager : villagers) {
                double distance = playerPos.distSqr(villager.blockPosition());
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestVillager = villager;
                }
            }

            return nearestVillager.blockPosition();
        }

        return null;
    }

    public static void activateCarrySurge(Player player, TestMod.PlayerClassCapability cap) {
        ItemStack pepperSpray = new ItemStack(TestModItems.PEPPER_SPRAY.get(), 1);

        if (player.getInventory().add(pepperSpray)) {
            player.sendSystemMessage(Component.literal("Получен перцовый баллончик!"));
            cap.useSurgeEnergy(50);
            cap.sync((ServerPlayer) player);
        } else {
            player.sendSystemMessage(Component.literal("Инвентарь полон! Освободите место для перцового баллончика."));
        }
    }
}
