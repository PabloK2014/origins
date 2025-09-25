package net.xach.testmod;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;

@Mod.EventBusSubscriber(modid = TestMod.MOD_ID)
public class SmithSkillHandler {

    private static final Map<UUID, Long> instantRepairCooldowns = new HashMap<>();
    private static final Map<UUID, Long> hotStrikeCooldowns = new HashMap<>();
    private static final Map<UUID, Long> lastAutoRepairTime = new HashMap<>();
    private static final Map<UUID, Boolean> hotStrikeReady = new HashMap<>();

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
            if (!cap.getPlayerClass().equals("smith")) return;

            int fireImmunityLevel = cap.getSkillLevel("fire_immunity");
            if (fireImmunityLevel > 0) {
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, true, false));
            }

            int autoRepairLevel = cap.getSkillLevel("auto_repair");
            if (autoRepairLevel > 0) {
                applyAutoRepair(player, autoRepairLevel);
            }

            int forgeMasterLevel = cap.getSkillLevel("forge_master");
            if (forgeMasterLevel > 0 && player.tickCount % 100 == 0) {
                showForgeMasterEffect(player, forgeMasterLevel);
            }
        });
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(TestMod.PlayerClassCapability.CAPABILITY).ifPresent(cap -> {
                if (!cap.getPlayerClass().equals("smith")) return;

                ItemStack result = event.getCrafting();

                int extraDurabilityLevel = cap.getSkillLevel("extra_durability");
                if (extraDurabilityLevel > 0 && result.isDamageableItem()) {
                    applyExtraDurability(result, extraDurabilityLevel, player);
                }

                int resourceEfficiencyLevel = cap.getSkillLevel("resource_efficiency");
                if (resourceEfficiencyLevel > 0) {
                    applyResourceEfficiency(player, resourceEfficiencyLevel, event);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onItemSmelted(PlayerEvent.ItemSmeltedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(TestMod.PlayerClassCapability.CAPABILITY).ifPresent(cap -> {
                if (!cap.getPlayerClass().equals("smith")) return;

                ItemStack result = event.getSmelting();

                int doubleIngotLevel = cap.getSkillLevel("double_ingot");
                if (doubleIngotLevel > 0 && isIngot(result)) {
                    applyDoubleIngot(player, result, doubleIngotLevel);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            player.getCapability(TestMod.PlayerClassCapability.CAPABILITY).ifPresent(cap -> {
                if (cap.getPlayerClass().equals("smith")) {
                    UUID playerId = player.getUUID();
                    if (hotStrikeReady.getOrDefault(playerId, false)) {
                        LivingEntity target = event.getEntity();
                        target.setSecondsOnFire(5);
                        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));

                        Level level = player.level();
                        for (int i = 0; i < 10; i++) {
                            double x = target.getX() + (player.getRandom().nextDouble() - 0.5) * 2;
                            double y = target.getY() + player.getRandom().nextDouble() * 2;
                            double z = target.getZ() + (player.getRandom().nextDouble() - 0.5) * 2;
                            level.addParticle(net.minecraft.core.particles.ParticleTypes.FLAME, x, y, z, 0, 0.1, 0);
                        }

                        hotStrikeReady.put(playerId, false);
                    }
                }
            });
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(TestMod.PlayerClassCapability.CAPABILITY).ifPresent(cap -> {
                if (cap.getPlayerClass().equals("smith")) {
                    int fireImmunityLevel = cap.getSkillLevel("fire_immunity");
                    if (fireImmunityLevel > 0) {
                        if (event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FIRE) ||
                                event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_LIGHTNING)) {
                            event.setCanceled(true);
                        }
                    }
                }
            });
        }
    }

    public static void activateInstantRepair(Player player, TestMod.PlayerClassCapability cap) {
        if (cap.getSkillLevel("instant_repair") > 0 && cap.getSurgeEnergy() >= 50) {
            UUID playerId = player.getUUID();
            long currentTime = System.currentTimeMillis();
            Long lastUse = instantRepairCooldowns.get(playerId);

            if (lastUse == null || currentTime - lastUse >= 300000) {
                int repairedItems = repairAllItems(player);
                if (repairedItems > 0) {
                    cap.useSurgeEnergy(50);
                    cap.sync((ServerPlayer) player);
                    instantRepairCooldowns.put(playerId, currentTime);
                    player.sendSystemMessage(Component.literal("Мгновенный ремонт! Починено предметов: " + repairedItems));
                } else {
                    player.sendSystemMessage(Component.literal("Нет предметов для ремонта!"));
                }
            } else {
                long remainingCooldown = 300000 - (currentTime - lastUse);
                player.sendSystemMessage(Component.literal("Перезарядка: " + (remainingCooldown / 1000) + " сек"));
            }
        }
    }

    public static void activateHotStrike(Player player, TestMod.PlayerClassCapability cap) {
        if (cap.getSkillLevel("hot_strike") > 0 && cap.getSurgeEnergy() >= 20) {
            UUID playerId = player.getUUID();
            long currentTime = System.currentTimeMillis();
            Long lastUse = hotStrikeCooldowns.get(playerId);

            if (lastUse == null || currentTime - lastUse >= 30000) {
                hotStrikeReady.put(playerId, true);
                cap.useSurgeEnergy(20);
                cap.sync((ServerPlayer) player);
                hotStrikeCooldowns.put(playerId, currentTime);
                player.sendSystemMessage(Component.literal("Раскалённый удар готов! Следующая атака подожжёт цель!"));

                player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
            } else {
                long remainingCooldown = 30000 - (currentTime - lastUse);
                player.sendSystemMessage(Component.literal("Перезарядка: " + (remainingCooldown / 1000) + " сек"));
            }
        }
    }

    private static void applyAutoRepair(Player player, int level) {
        UUID playerId = player.getUUID();
        long currentTime = System.currentTimeMillis();
        Long lastRepair = lastAutoRepairTime.get(playerId);

        if (lastRepair == null || currentTime - lastRepair >= 30000) {
            ItemStack repairedItem = repairRandomItem(player);
            if (repairedItem != null) {
                lastAutoRepairTime.put(playerId, currentTime);
                player.sendSystemMessage(Component.literal("Авторемонт: " + repairedItem.getDisplayName().getString() + " частично починен"));
            }
        }
    }

    private static void applyExtraDurability(ItemStack item, int level, Player player) {
        Random random = new Random();
        double chance = 0.02 * level;

        if (random.nextDouble() < chance) {
            // Создаем копию предмета с увеличенной прочностью
            ItemStack newItem = item.copy();
            
            // Увеличиваем максимальную прочность на 10%
            int maxDamage = newItem.getMaxDamage();
            int bonusDurability = (int) (maxDamage * 0.1);
            
            // Применяем бонус к прочности (обратите внимание, что в Minecraft меньшее значение damage означает больше прочности)
            int currentDamage = newItem.getDamageValue();
            newItem.setDamageValue(Math.max(0, currentDamage - bonusDurability));
            
            // Заменяем оригинальный предмет на предмет с бонусной прочностью
            // Находим предмет в инвентаре и заменяем его
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                if (player.getInventory().getItem(i) == item) {
                    player.getInventory().setItem(i, newItem);
                    break;
                }
            }
            
            player.sendSystemMessage(Component.literal("Повышенная прочность! " + newItem.getDisplayName().getString() + " создан с бонусной прочностью!"));
        }
    }

    private static void applyResourceEfficiency(Player player, int level, PlayerEvent.ItemCraftedEvent event) {
        Random random = new Random();
        double chance = 0.02 * level;

        if (random.nextDouble() < chance) {
            // Возвращаем часть ингредиентов игроку
            List<ItemStack> remainingItems = event.getInventory().getRemainingItems();
            
            for (ItemStack remainingItem : remainingItems) {
                if (!remainingItem.isEmpty()) {
                    // 50% шанс вернуть каждый оставшийся предмет
                    if (random.nextFloat() < 0.5f) {
                        ItemStack bonus = remainingItem.copy();
                        if (!player.getInventory().add(bonus)) {
                            player.drop(bonus, false);
                        }
                    }
                }
            }
            
            player.sendSystemMessage(Component.literal("Ресурсная экономия! Часть материалов сохранена!"));
        }
    }

    private static void applyDoubleIngot(Player player, ItemStack result, int level) {
        Random random = new Random();
        double chance = 0.05 * level;

        if (random.nextDouble() < chance) {
            ItemStack bonus = result.copy();
            bonus.setCount(result.getCount());

            if (!player.getInventory().add(bonus)) {
                player.drop(bonus, false);
            }

            player.sendSystemMessage(Component.literal("Удвоение слитков! Получен бонусный " + result.getDisplayName().getString()));
        }
    }

    private static boolean isIngot(ItemStack item) {
        return item.is(Items.IRON_INGOT) || item.is(Items.GOLD_INGOT) ||
                item.is(Items.COPPER_INGOT) || item.is(Items.NETHERITE_INGOT);
    }

    private static ItemStack repairRandomItem(Player player) {
        List<ItemStack> damagedItems = new ArrayList<>();

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item.isDamageableItem() && item.isDamaged()) {
                damagedItems.add(item);
            }
        }

        if (!damagedItems.isEmpty()) {
            ItemStack itemToRepair = damagedItems.get(new Random().nextInt(damagedItems.size()));
            int repairAmount = Math.max(1, itemToRepair.getMaxDamage() / 20);
            itemToRepair.setDamageValue(Math.max(0, itemToRepair.getDamageValue() - repairAmount));
            return itemToRepair;
        }

        return null;
    }

    private static int repairAllItems(Player player) {
        int repairedCount = 0;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item.isDamageableItem() && item.isDamaged()) {
                int repairAmount = item.getMaxDamage() / 2;
                item.setDamageValue(Math.max(0, item.getDamageValue() - repairAmount));
                repairedCount++;
            }
        }

        return repairedCount;
    }

    private static void showForgeMasterEffect(Player player, int level) {
        BlockPos playerPos = player.blockPosition();
        Level world = player.level();
        boolean foundFurnace = false;

        for (int x = -8; x <= 8; x++) {
            for (int y = -4; y <= 4; y++) {
                for (int z = -8; z <= 8; z++) {
                    BlockPos pos = playerPos.offset(x, y, z);
                    BlockEntity blockEntity = world.getBlockEntity(pos);

                    if (blockEntity instanceof AbstractFurnaceBlockEntity) {
                        foundFurnace = true;

                        for (int i = 0; i < 3; i++) {
                            double particleX = pos.getX() + 0.5 + (world.random.nextDouble() - 0.5) * 0.8;
                            double particleY = pos.getY() + 0.5 + world.random.nextDouble() * 0.5;
                            double particleZ = pos.getZ() + 0.5 + (world.random.nextDouble() - 0.5) * 0.8;
                            world.addParticle(net.minecraft.core.particles.ParticleTypes.FLAME,
                                    particleX, particleY, particleZ, 0, 0.05, 0);
                        }
                    }
                }
            }
        }
    }
}
