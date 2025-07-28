package io.github.apace100.origins.quest;

import io.github.apace100.origins.Origins;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import java.util.*;

public class BountyBoardBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory, NamedScreenHandlerFactory {

    private final SimpleInventory decrees = new SimpleInventory(3);
    private final SimpleInventory bounties = new SimpleInventory(21); // 3x7 слотов для квестов
    private final Map<String, Set<Integer>> takenMask = new HashMap<>();
    private final Map<String, Integer> finishMap = new HashMap<>();
    private final List<Quest> availableQuests = new ArrayList<>();

    public BountyBoardBlockEntity(BlockPos pos, BlockState state) {
        super(QuestRegistry.BOUNTY_BOARD_BLOCK_ENTITY, pos, state);
        // Генерируем квесты при создании блока
        generateRandomQuests();
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("gui.origins.bounty_board.title");
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new BountyBoardScreenHandler(syncId, inv, this);
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeBlockPos(this.pos);
    }

    public SimpleInventory getDecrees() {
        return decrees;
    }

    public SimpleInventory getBounties() {
        return bounties;
    }

    public List<Quest> getAvailableQuests() {
        return availableQuests;
    }

    public void addQuest(Quest quest, int slot) {
        if (slot >= 0 && slot < 21 && quest != null) {
            availableQuests.add(quest);
            markDirty();
        }
    }

    public void removeQuest(Quest quest) {
        if (quest != null) {
            availableQuests.remove(quest);
            markDirty();
        }
    }
    
    public void removeQuest(int slot) {
        if (slot >= 0 && slot < availableQuests.size()) {
            availableQuests.remove(slot);
            markDirty();
        }
    }
    
    public void refreshQuests() {
        availableQuests.clear();
        generateRandomQuests();
        markDirty();
    }
    
    private void generateRandomQuests() {
        // Очищаем старые квесты
        availableQuests.clear();
        
        // Всегда создаем тестовые квесты для стабильной работы
        createTestQuests();
        System.out.println("Создано тестовых квестов: " + availableQuests.size());
        
        // Пытаемся дополнительно загрузить квесты из JSON файлов
        if (world != null && !world.isClient && world.getServer() != null) {
            try {
                System.out.println("Попытка загрузки квестов из JSON...");
                QuestGenerator.loadQuestsFromResources(world.getServer().getResourceManager());
                
                // Проверяем, сколько квестов загрузилось
                int totalJsonQuests = QuestGenerator.getTotalQuestCount();
                System.out.println("Загружено квестов из JSON: " + totalJsonQuests);
                
                if (totalJsonQuests > 0) {
                    // Добавляем квесты из JSON (если загрузились)
                    String[] professions = {"warrior", "cook", "courier", "brewer", "blacksmith", "miner"};
                    for (String profession : professions) {
                        List<Quest> professionQuests = QuestGenerator.getRandomQuestsForProfession(profession, 1);
                        if (!professionQuests.isEmpty()) {
                            availableQuests.addAll(professionQuests);
                            System.out.println("Добавлено " + professionQuests.size() + " квестов для " + profession);
                        }
                    }
                } else {
                    System.out.println("JSON квесты не загрузились, используем только тестовые");
                }
                
                System.out.println("Общее количество квестов: " + availableQuests.size());
            } catch (Exception e) {
                System.out.println("Ошибка загрузки JSON квестов: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Создает тестовые квесты для всех классов
     */
    private void createTestQuests() {
        // Воин - 2 квеста
        availableQuests.add(new Quest("test_warrior_1", "warrior", 1, "Сбор костей", "Соберите кости для тренировки", 
            new QuestObjective(QuestObjective.ObjectiveType.COLLECT, "minecraft:bone", 8), 30, 
            new QuestReward(QuestReward.RewardType.SKILL_POINT_TOKEN, 1, 400)));
        availableQuests.add(new Quest("test_warrior_2", "warrior", 1, "Охота на зомби", "Убейте зомби для получения опыта", 
            new QuestObjective(QuestObjective.ObjectiveType.COLLECT, "minecraft:rotten_flesh", 5), 25, 
            new QuestReward(QuestReward.RewardType.SKILL_POINT_TOKEN, 1, 350)));
            
        // Повар - 2 квеста
        availableQuests.add(new Quest("test_cook_1", "cook", 1, "Сбор пшеницы", "Соберите пшеницу для хлеба", 
            new QuestObjective(QuestObjective.ObjectiveType.COLLECT, "minecraft:wheat", 10), 20, 
            new QuestReward(QuestReward.RewardType.SKILL_POINT_TOKEN, 1, 300)));
        availableQuests.add(new Quest("test_cook_2", "cook", 1, "Выпечка хлеба", "Испеките хлеб для жителей", 
            new QuestObjective(QuestObjective.ObjectiveType.CRAFT, "minecraft:bread", 5), 25, 
            new QuestReward(QuestReward.RewardType.SKILL_POINT_TOKEN, 1, 400)));
            
        // Шахтер - 2 квеста
        availableQuests.add(new Quest("test_miner_1", "miner", 1, "Добыча угля", "Добудьте уголь в шахтах", 
            new QuestObjective(QuestObjective.ObjectiveType.COLLECT, "minecraft:coal", 16), 30, 
            new QuestReward(QuestReward.RewardType.SKILL_POINT_TOKEN, 1, 350)));
        availableQuests.add(new Quest("test_miner_2", "miner", 1, "Железная руда", "Добудьте железную руду", 
            new QuestObjective(QuestObjective.ObjectiveType.COLLECT, "minecraft:raw_iron", 6), 35, 
            new QuestReward(QuestReward.RewardType.SKILL_POINT_TOKEN, 1, 500)));
            
        // Кузнец - 2 квеста
        availableQuests.add(new Quest("test_blacksmith_1", "blacksmith", 1, "Железные слитки", "Выплавите железные слитки", 
            new QuestObjective(QuestObjective.ObjectiveType.COLLECT, "minecraft:iron_ingot", 8), 25, 
            new QuestReward(QuestReward.RewardType.SKILL_POINT_TOKEN, 1, 400)));
        availableQuests.add(new Quest("test_blacksmith_2", "blacksmith", 1, "Железные инструменты", "Создайте железную кирку", 
            new QuestObjective(QuestObjective.ObjectiveType.CRAFT, "minecraft:iron_pickaxe", 1), 30, 
            new QuestReward(QuestReward.RewardType.SKILL_POINT_TOKEN, 1, 450)));
            
        // Курьер - 2 квеста
        availableQuests.add(new Quest("test_courier_1", "courier", 1, "Быстрые ноги", "Создайте кожаные ботинки", 
            new QuestObjective(QuestObjective.ObjectiveType.CRAFT, "minecraft:leather_boots", 1), 20, 
            new QuestReward(QuestReward.RewardType.SKILL_POINT_TOKEN, 1, 300)));
        availableQuests.add(new Quest("test_courier_2", "courier", 1, "Навигация", "Создайте компас", 
            new QuestObjective(QuestObjective.ObjectiveType.CRAFT, "minecraft:compass", 1), 25, 
            new QuestReward(QuestReward.RewardType.SKILL_POINT_TOKEN, 1, 400)));
            
        // Пивовар - 2 квеста
        availableQuests.add(new Quest("test_brewer_1", "brewer", 1, "Адский нарост", "Соберите адский нарост", 
            new QuestObjective(QuestObjective.ObjectiveType.COLLECT, "minecraft:nether_wart", 8), 40, 
            new QuestReward(QuestReward.RewardType.SKILL_POINT_TOKEN, 1, 500)));
        availableQuests.add(new Quest("test_brewer_2", "brewer", 1, "Варочная стойка", "Создайте варочную стойку", 
            new QuestObjective(QuestObjective.ObjectiveType.CRAFT, "minecraft:brewing_stand", 1), 30, 
            new QuestReward(QuestReward.RewardType.SKILL_POINT_TOKEN, 1, 450)));
    }

    public Set<Integer> maskFor(PlayerEntity player) {
        return takenMask.computeIfAbsent(player.getUuidAsString(), k -> new HashSet<>());
    }

    public int getNumCompleted() {
        return finishMap.values().stream().mapToInt(Integer::intValue).sum();
    }

    public void updateCompletedBounties(PlayerEntity player) {
        finishMap.merge(player.getUuidAsString(), 1, Integer::sum);
        markDirty();
    }

    private SimpleInventory getMaskedInventory(PlayerEntity player) {
        SimpleInventory maskedBounties = new SimpleInventory(21);
        for (int i = 0; i < 21; i++) {
            if (!maskFor(player).contains(i)) {
                maskedBounties.setStack(i, bounties.getStack(i).copy());
            }
        }
        return maskedBounties;
    }

    private void setDecree() {
        if (world instanceof ServerWorld && decrees.isEmpty()) {
            int slot = new Random().nextInt(3);
            decrees.setStack(slot, createDecreeStack());
            markDirty();
        }
    }

    private ItemStack createDecreeStack() {
        // Заглушка, замените на реальную реализацию DecreeItem
        return new ItemStack(QuestRegistry.SKILL_POINT_TOKEN_TIER1); // Пример
    }

    public void tryInitialPopulation() {
        if (isPristine()) {
            if (decrees.isEmpty()) {
                setDecree();
            }
            for (int i = 0; i < 6; i++) {
                randomlyUpdateBoard();
            }
            markDirty();
        }
    }

    private boolean isPristine() {
        return bounties.isEmpty() && finishMap.isEmpty() && takenMask.isEmpty();
    }

    private void randomlyUpdateBoard() {
        if (!(world instanceof ServerWorld)) return;

        int slotToAddTo = new Random().nextInt(21);
        int slotsToRemove = new Random().nextInt(3) > 0 ? (new Random().nextBoolean() ? 1 : 2) : 0;
        List<Integer> slotsToRemoveList = new ArrayList<>();
        for (int i = 0; i < slotsToRemove; i++) {
            int slot = new Random().nextInt(21);
            if (slot != slotToAddTo) slotsToRemoveList.add(slot);
        }

        // Генерируем новый квест для случайной профессии
        String[] professions = {"warrior", "cook", "courier", "brewer", "blacksmith", "miner"};
        String profession = professions[new Random().nextInt(professions.length)];
        int level = new Random().nextInt(3) + 1; // Уровень от 1 до 3
        Quest newQuest = QuestGenerator.generateRandomQuest(profession, level);
        if (newQuest != null) {
            removeQuest(slotToAddTo);
            addQuest(newQuest, slotToAddTo);
        }

        Set<Integer> playerMask = maskFor((PlayerEntity) world.getPlayers().get(0)); // Пример, замените на реального игрока
        playerMask.removeIf(slot -> slot == slotToAddTo || slotsToRemoveList.contains(slot));
        slotsToRemoveList.forEach(this::removeQuest);

        markDirty();
    }

    public int[] levelProgress(int done) {
        int doneAcc = done;
        int perAcc = 2;
        int levels = 0;

        while (doneAcc >= perAcc * 5) {
            levels += 5;
            doneAcc -= perAcc * 5;
            perAcc++;
        }
        levels += doneAcc / perAcc;
        return new int[]{levels, doneAcc % perAcc, perAcc};
    }

    public int getTotalNumCompleted() {
        return getNumCompleted();
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        // Читаем инвентари из NBT
        NbtCompound decreeNbt = nbt.getCompound("decree_inv");
        NbtCompound bountyNbt = nbt.getCompound("bounty_inv");
        
        // Очищаем инвентари перед загрузкой
        decrees.clear();
        bounties.clear();
        
        // Загружаем предметы
        for (int i = 0; i < decrees.size(); i++) {
            if (decreeNbt.contains(String.valueOf(i))) {
                decrees.setStack(i, ItemStack.fromNbt(decreeNbt.getCompound(String.valueOf(i))));
            }
        }
        
        for (int i = 0; i < bounties.size(); i++) {
            if (bountyNbt.contains(String.valueOf(i))) {
                bounties.setStack(i, ItemStack.fromNbt(bountyNbt.getCompound(String.valueOf(i))));
            }
        }

        // Загрузка карты завершенных квестов
        if (nbt.contains("completed", NbtElement.COMPOUND_TYPE)) {
            finishMap.clear();
            NbtCompound completedNbt = nbt.getCompound("completed");
            for (String key : completedNbt.getKeys()) {
                finishMap.put(key, completedNbt.getInt(key));
            }
        }

        // Загрузка маски взятых квестов
        if (nbt.contains("taken", NbtElement.COMPOUND_TYPE)) {
            takenMask.clear();
            NbtCompound takenNbt = nbt.getCompound("taken");
            for (String playerUuid : takenNbt.getKeys()) {
                NbtList playerTaken = takenNbt.getList(playerUuid, NbtElement.INT_TYPE);
                Set<Integer> takenSet = new HashSet<>();
                for (int i = 0; i < playerTaken.size(); i++) {
                    takenSet.add(playerTaken.getInt(i));
                }
                takenMask.put(playerUuid, takenSet);
            }
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        NbtCompound decreeList = new NbtCompound();
        // Сохраняем предметы в NBT
        for (int i = 0; i < decrees.size(); i++) {
            ItemStack stack = decrees.getStack(i);
            if (!stack.isEmpty()) {
                decreeList.put(String.valueOf(i), stack.writeNbt(new NbtCompound()));
            }
        }
        
        NbtCompound bountyList = new NbtCompound();
        for (int i = 0; i < bounties.size(); i++) {
            ItemStack stack = bounties.getStack(i);
            if (!stack.isEmpty()) {
                bountyList.put(String.valueOf(i), stack.writeNbt(new NbtCompound()));
            }
        }
        nbt.put("decree_inv", decreeList);
        nbt.put("bounty_inv", bountyList);

        // Сохранение карты завершенных квестов
        NbtCompound completedNbt = new NbtCompound();
        for (Map.Entry<String, Integer> entry : finishMap.entrySet()) {
            completedNbt.putInt(entry.getKey(), entry.getValue());
        }
        nbt.put("completed", completedNbt);

        // Сохранение маски взятых квестов
        NbtCompound takenNbt = new NbtCompound();
        for (Map.Entry<String, Set<Integer>> entry : takenMask.entrySet()) {
            NbtList playerTakenList = new NbtList();
            for (Integer slot : entry.getValue()) {
                playerTakenList.add(NbtString.of(slot.toString()));
            }
            takenNbt.put(entry.getKey(), playerTakenList);
        }
        nbt.put("taken", takenNbt);
    }

    public static void tick(World world, BlockPos pos, BlockState state, BountyBoardBlockEntity entity) {
        if (world.isClient) return;

        entity.tryInitialPopulation();

        // Обработка декретов (пока отключена)
        if (world.getTime() % 20L == 0L) {
            // Логика обработки декретов будет добавлена позже
        }

        if ((world.getTime() + 13L) % (20L * 60) == 0L) { // Пример частоты обновления
            entity.randomlyUpdateBoard();
        }

        // Проверка истечения времени квестов
        if (world.getTime() % 100L == 4L) {
            // Логика проверки времени квестов будет добавлена позже
        }
    }

    private PropertyDelegate getDoneProperty() {
        return new PropertyDelegate() {
            @Override
            public int get(int index) {
                return getNumCompleted();
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int size() {
                return 1;
            }
        };
    }
    

}