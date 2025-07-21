package net.xach.testmod;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;

@Mod.EventBusSubscriber(modid = TestMod.MOD_ID)
public class MinerSkillHandler {

    // Карты для отслеживания перезарядок навыков
    private static final Map<UUID, Long> oreHighlightCooldowns = new HashMap<>();
    private static final Map<UUID, Long> veinMinerCooldowns = new HashMap<>();
    private static final Map<UUID, Long> nightVisionCooldowns = new HashMap<>();

    // Карта для отслеживания подсвеченных блоков
    private static final Map<UUID, Set<BlockPos>> highlightedOres = new HashMap<>();
    private static final Map<UUID, Long> highlightEndTimes = new HashMap<>();

    // Карта для отслеживания состояния широкого копания
    private static final Map<UUID, Boolean> areaMiningEnabled = new HashMap<>();

    // Список блоков руды
    private static final Set<Block> ORE_BLOCKS = Set.of(
            Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE,
            Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE,
            Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE,
            Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
            Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
            Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
            Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE,
            Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE,
            Blocks.NETHER_GOLD_ORE, Blocks.NETHER_QUARTZ_ORE,
            Blocks.ANCIENT_DEBRIS
    );

    // Список случайных предметов для навыка "Случайная находка"
    private static final List<ItemStack> RANDOM_FINDS = Arrays.asList(
            new ItemStack(Items.DIAMOND),
            new ItemStack(Items.EMERALD),
            new ItemStack(Items.GOLD_INGOT),
            new ItemStack(Items.IRON_INGOT),
            new ItemStack(Items.COAL),
            new ItemStack(Items.REDSTONE),
            new ItemStack(Items.LAPIS_LAZULI),
            new ItemStack(Items.COPPER_INGOT),
            new ItemStack(Items.QUARTZ),
            new ItemStack(Items.EXPERIENCE_BOTTLE)
    );

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            if (ModKeyBindings.ACTIVATE_SKILL.consumeClick()) {
                TestMod.NETWORK.send(PacketDistributor.SERVER.noArg(), new SkillActivationPacket());
            }
            if (ModKeyBindings.TOGGLE_AREA_MINING.consumeClick()) {
                TestMod.NETWORK.send(PacketDistributor.SERVER.noArg(), new ToggleAreaMiningPacket());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) return;

        Player player = event.player;
        player.getCapability(TestMod.PlayerClassCapability.CAPABILITY).ifPresent(cap -> {
            if (!cap.getPlayerClass().equals("miner")) return;

            // Обработка подсветки руды
            UUID playerId = player.getUUID();
            if (highlightEndTimes.containsKey(playerId)) {
                long currentTime = System.currentTimeMillis();
                if (currentTime > highlightEndTimes.get(playerId)) {
                    // Убираем подсветку
                    highlightedOres.remove(playerId);
                    highlightEndTimes.remove(playerId);
                }
            }

            // Обработка устойчивости к взрывам
            int blastResistanceLevel = cap.getSkillLevel("blast_resistance");
            if (blastResistanceLevel > 0) {
                applyBlastResistance(player, blastResistanceLevel);
            }
        });
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            player.getCapability(TestMod.PlayerClassCapability.CAPABILITY).ifPresent(cap -> {
                if (!cap.getPlayerClass().equals("miner")) return;

                BlockState brokenBlock = event.getState();
                BlockPos pos = event.getPos();
                Level level = player.level();

                // Широкое копание (только если включено)
                int digAreaLevel = cap.getSkillLevel("dig_area");
                if (digAreaLevel > 0 && areaMiningEnabled.getOrDefault(player.getUUID(), false)) {
                    applyAreaMining(player, pos, brokenBlock, digAreaLevel);
                }

                // Случайная находка
                int randomFindLevel = cap.getSkillLevel("ore_drop_chance");
                if (randomFindLevel > 0) {
                    applyRandomFind(player, pos, randomFindLevel);
                }

                // Удвоение руды
                int doubleOreLevel = cap.getSkillLevel("ore_double");
                if (doubleOreLevel > 0 && ORE_BLOCKS.contains(brokenBlock.getBlock())) {
                    applyOreDoubling(player, pos, brokenBlock, doubleOreLevel);
                }

                // Глубинный шахтёр
                int deepMinerLevel = cap.getSkillLevel("deep_miner");
                if (deepMinerLevel > 0 && pos.getY() < 0) {
                    applyDeepMinerBonus(player, pos, brokenBlock, deepMinerLevel);
                }
            });
        }
    }

    // Переключение широкого копания
    public static void toggleAreaMining(Player player) {
        player.getCapability(TestMod.PlayerClassCapability.CAPABILITY).ifPresent(cap -> {
            if (!cap.getPlayerClass().equals("miner")) {
                player.sendSystemMessage(Component.literal("Только шахтёры могут использовать широкое копание!"));
                return;
            }

            int digAreaLevel = cap.getSkillLevel("dig_area");
            if (digAreaLevel == 0) {
                player.sendSystemMessage(Component.literal("У вас нет навыка 'Широкое копание'!"));
                return;
            }

            UUID playerId = player.getUUID();
            boolean currentState = areaMiningEnabled.getOrDefault(playerId, false);
            areaMiningEnabled.put(playerId, !currentState);

            if (!currentState) {
                String sizeText = switch (digAreaLevel) {
                    case 1 -> "1x1 (стандартное)";
                    case 2 -> "2x2";
                    case 3 -> "3x3";
                    default -> digAreaLevel + "x" + digAreaLevel;
                };
                player.sendSystemMessage(Component.literal("Широкое копание включено (" + sizeText + ")"));
            } else {
                player.sendSystemMessage(Component.literal("Широкое копание отключено"));
            }
        });
    }

    // Активные навыки
    public static void activateOreHighlight(Player player, TestMod.PlayerClassCapability cap) {
        if (cap.getSkillLevel("ore_highlight") > 0 && cap.getSurgeEnergy() >= 30) {
            UUID playerId = player.getUUID();
            long currentTime = System.currentTimeMillis();
            Long lastUse = oreHighlightCooldowns.get(playerId);

            if (lastUse == null || currentTime - lastUse >= 60000) { // 60 секунд перезарядка
                Set<BlockPos> orePositions = highlightOresAroundPlayer(player);
                cap.useSurgeEnergy(30);
                cap.sync((ServerPlayer) player);
                oreHighlightCooldowns.put(playerId, currentTime);
                highlightEndTimes.put(playerId, currentTime + 30000); // 30 секунд действия

                // Отправляем пакет на клиент для подсветки
                TestMod.NETWORK.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player),
                        new OreHighlightPacket(orePositions, 30000));

                player.sendSystemMessage(Component.literal("Руда подсвечена на 30 секунд! Найдено: " + orePositions.size()));
            } else {
                long remainingCooldown = 60000 - (currentTime - lastUse);
                player.sendSystemMessage(Component.literal("Перезарядка: " + (remainingCooldown / 1000) + " сек"));
            }
        }
    }

    public static void activateVeinMiner(Player player, TestMod.PlayerClassCapability cap) {
        if (cap.getSkillLevel("vein_miner") > 0 && cap.getSurgeEnergy() >= 50) {
            UUID playerId = player.getUUID();
            long currentTime = System.currentTimeMillis();
            Long lastUse = veinMinerCooldowns.get(playerId);

            if (lastUse == null || currentTime - lastUse >= 120000) { // 120 секунд перезарядка
                int minedOres = mineAllOresInRadius(player, 10);
                if (minedOres > 0) {
                    cap.useSurgeEnergy(50);
                    cap.sync((ServerPlayer) player);
                    veinMinerCooldowns.put(playerId, currentTime);
                    player.sendSystemMessage(Component.literal("Жилокопатель активирован! Добыто руды: " + minedOres));
                } else {
                    player.sendSystemMessage(Component.literal("Поблизости нет руды!"));
                }
            } else {
                long remainingCooldown = 120000 - (currentTime - lastUse);
                player.sendSystemMessage(Component.literal("Перезарядка: " + (remainingCooldown / 1000) + " сек"));
            }
        }
    }

    public static void activateNightVision(Player player, TestMod.PlayerClassCapability cap) {
        if (cap.getSkillLevel("night_vision") > 0 && cap.getSurgeEnergy() >= 20) {
            UUID playerId = player.getUUID();
            long currentTime = System.currentTimeMillis();
            Long lastUse = nightVisionCooldowns.get(playerId);

            if (lastUse == null || currentTime - lastUse >= 180000) { // 180 секунд перезарядка
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 2400, 0)); // 2 минуты
                cap.useSurgeEnergy(20);
                cap.sync((ServerPlayer) player);
                nightVisionCooldowns.put(playerId, currentTime);
                player.sendSystemMessage(Component.literal("Ночное зрение активировано на 2 минуты!"));
            } else {
                long remainingCooldown = 180000 - (currentTime - lastUse);
                player.sendSystemMessage(Component.literal("Перезарядка: " + (remainingCooldown / 1000) + " сек"));
            }
        }
    }

    // Вспомогательные методы
    private static void applyAreaMining(Player player, BlockPos centerPos, BlockState originalBlock, int level) {
        Level world = player.level();

        // Уровень 1: 1x1 (стандартное копание) - ничего не делаем
        if (level == 1) return;

        // Исправленная логика: level 2 = 2x2, level 3 = 3x3
        int size = level; // Размер области
        int offset = (size - 1) / 2; // Смещение от центра

        for (int x = -offset; x <= offset; x++) {
            for (int y = -offset; y <= offset; y++) {
                for (int z = -offset; z <= offset; z++) {
                    if (x == 0 && y == 0 && z == 0) continue; // Пропускаем центральный блок

                    BlockPos pos = centerPos.offset(x, y, z);
                    BlockState state = world.getBlockState(pos);

                    // Ломаем только блоки того же типа или камень/булыжник
                    if (state.getBlock() == originalBlock.getBlock() ||
                            state.getBlock() == Blocks.STONE ||
                            state.getBlock() == Blocks.COBBLESTONE ||
                            state.getBlock() == Blocks.DEEPSLATE ||
                            state.getBlock() == Blocks.COBBLED_DEEPSLATE) {

                        world.destroyBlock(pos, true, player);
                    }
                }
            }
        }
    }

    private static void applyRandomFind(Player player, BlockPos pos, int level) {
        Random random = new Random();
        double chance = 0.0001 * level; // 0.01% за уровень

        if (random.nextDouble() < chance) {
            ItemStack randomItem = RANDOM_FINDS.get(random.nextInt(RANDOM_FINDS.size())).copy();
            Level level1 = player.level();
            ItemEntity itemEntity = new ItemEntity(level1, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, randomItem);
            level1.addFreshEntity(itemEntity);
            player.sendSystemMessage(Component.literal("Случайная находка: " + randomItem.getDisplayName().getString()));
        }
    }

    private static void applyOreDoubling(Player player, BlockPos pos, BlockState oreBlock, int level) {
        Random random = new Random();
        double chance = 0.02 * level; // 2% за уровень

        if (random.nextDouble() < chance) {
            // Дропаем дополнительные ресурсы
            Level level1 = player.level();
            Block.dropResources(oreBlock, level1, pos, null, player, player.getMainHandItem());
            player.sendSystemMessage(Component.literal("Удвоение руды!"));
        }
    }

    private static void applyDeepMinerBonus(Player player, BlockPos pos, BlockState block, int level) {
        // 25% бонус к добыче в глубоких шахтах за уровень
        Random random = new Random();
        double chance = 0.25 * level;
        if (random.nextDouble() < chance) {
            Level world = player.level();
            Block.dropResources(block, world, pos, null, player, player.getMainHandItem());
        }
    }

    private static void applyBlastResistance(Player player, int level) {
        // Применяем сопротивление взрывам (15% за уровень)
        // Это будет обрабатываться в событии получения урона
    }

    private static Set<BlockPos> highlightOresAroundPlayer(Player player) {
        Level level = player.level();
        BlockPos playerPos = player.blockPosition();
        Set<BlockPos> orePositions = new HashSet<>();

        // Ищем руду в радиусе 50 блоков
        for (int x = -50; x <= 50; x++) {
            for (int y = -50; y <= 50; y++) {
                for (int z = -50; z <= 50; z++) {
                    BlockPos pos = playerPos.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (ORE_BLOCKS.contains(state.getBlock())) {
                        orePositions.add(pos);
                    }
                }
            }
        }

        return orePositions;
    }

    private static int mineAllOresInRadius(Player player, int radius) {
        Level level = player.level();
        BlockPos playerPos = player.blockPosition();
        int minedCount = 0;

        // Ищем и ломаем все руды в радиусе
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = playerPos.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);

                    if (ORE_BLOCKS.contains(state.getBlock())) {
                        // Получаем дроп от блока
                        List<ItemStack> drops = Block.getDrops(state, (net.minecraft.server.level.ServerLevel) level, pos, null, player, player.getMainHandItem());

                        // Добавляем дроп в инвентарь игрока
                        for (ItemStack drop : drops) {
                            if (!player.getInventory().add(drop)) {
                                // Если инвентарь полон, дропаем на землю
                                ItemEntity itemEntity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop);
                                level.addFreshEntity(itemEntity);
                            }
                        }

                        // Ломаем блок без дропа (так как мы уже добавили дроп в инвентарь)
                        level.destroyBlock(pos, false, player);
                        minedCount++;
                    }
                }
            }
        }

        return minedCount;
    }
}
