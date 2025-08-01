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
        // Квесты будут сгенерированы в tryInitialPopulation() когда world будет доступен
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
        // Создаем список квестов из инвентаря bounties
        List<Quest> quests = new ArrayList<>();
        
        for (int i = 0; i < bounties.size(); i++) {
            ItemStack stack = bounties.getStack(i);
            if (!stack.isEmpty()) {
                if (stack.getItem() instanceof QuestTicketItem) {
                    // Это билет квеста - извлекаем Quest из NBT
                    Quest quest = QuestItem.getQuestFromStack(stack);
                    if (quest != null) {
                        quests.add(quest);
                    }
                } else if (stack.getItem() instanceof BountifulQuestItem) {
                    // Это BountifulQuestItem - создаем простой Quest для совместимости
                    Quest simpleQuest = createSimpleQuestFromBountifulItem(stack, "unknown", 1);
                    if (simpleQuest != null) {
                        quests.add(simpleQuest);
                    }
                }
            }
        }
        
        return quests;
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
        if (world == null || world.isClient) {
            return;
        }
        
        // Принудительно очищаем все
        availableQuests.clear();
        bounties.clear();
        
        generateRandomQuests();
        markDirty();
    }
    
    /**
     * Принудительно пересоздает все квесты на доске
     */
    public void forceRegenerateQuests() {
        if (world == null || world.isClient) {
            return;
        }
        
        // Полностью очищаем все данные
        availableQuests.clear();
        bounties.clear();
        takenMask.clear();
        finishMap.clear();
        
        // Генерируем новые квесты
        generateRandomQuests();
        markDirty();
    }
    
    /**
     * Получить количество квестов на доске
     */
    public int getQuestCount() {
        int count = 0;
        for (int i = 0; i < bounties.size(); i++) {
            if (!bounties.getStack(i).isEmpty()) {
                count++;
            }
        }
        return count;
    }
    
    private void generateRandomQuests() {
        // Проверяем, что мир доступен и это серверная сторона
        if (world == null || world.isClient) {
            return;
        }
        
        // Очищаем старые квесты
        availableQuests.clear();
        bounties.clear(); // Очищаем также инвентарь квестов
        
        if (world instanceof ServerWorld) {
            try {
                // Проверяем, есть ли уже загруженные квесты (они должны быть загружены через QuestResourceReloadListener)
                int totalJsonQuests = QuestGenerator.getTotalQuestCount();
                
                if (totalJsonQuests > 0) {
                    // Загружаем квесты из JSON файлов в слоты доски
                    String[] professions = {"warrior", "cook", "courier", "brewer", "blacksmith", "miner"};
                    int slotIndex = 0;
                    
                    for (String profession : professions) {
                        List<Quest> professionQuests = QuestGenerator.getRandomQuestsForProfession(profession, 3);
                        System.out.println("Получено " + professionQuests.size() + " квестов для профессии " + profession);
                        
                        for (Quest quest : professionQuests) {
                            if (slotIndex < 21 && quest != null) {
                                // Создаем билет квеста из JSON квеста
                                ItemStack questTicket = QuestTicketItem.createQuestTicket(quest);
                                
                                if (!questTicket.isEmpty()) {
                                    bounties.setStack(slotIndex, questTicket);
                                    slotIndex++;
                                }
                            }
                        }
                    }
                } else {
                    generateBountifulQuests();
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                // Fallback к BountifulQuestCreator
                generateBountifulQuests();
            }
        }
    }
    
    /**
     * Генерирует квесты через BountifulQuestCreator как fallback
     */
    private void generateBountifulQuests() {
        String[] professions = {"warrior", "cook", "courier", "brewer", "blacksmith", "miner"};
        Random random = new Random();
        
        // Создаем 12-15 квестов для доски
        int questCount = 12 + random.nextInt(4);
        
        for (int i = 0; i < questCount && i < 21; i++) {
            String profession = professions[random.nextInt(professions.length)];
            int level = random.nextInt(10) + 1;
            
            BountifulQuestCreator creator = new BountifulQuestCreator(
                (ServerWorld) world,
                pos,
                profession,
                level,
                world.getTime()
            );
            
            ItemStack questItem = creator.createQuestItem();
            if (!questItem.isEmpty()) {
                bounties.setStack(i, questItem);
                

            }
        }
    }
    
    /**
     * Создает простой Quest объект из BountifulQuestItem для совместимости
     */
    private Quest createSimpleQuestFromBountifulItem(ItemStack questItem, String profession, int level) {
        try {
            // Создаем простой квест с базовой информацией
            String questId = "bountiful_" + profession + "_" + level + "_" + System.currentTimeMillis();
            String title = "Квест " + profession + " (Уровень " + level + ")";
            String description = "Автоматически созданный квест для " + profession;
            
            // Простая цель - собрать предмет
            QuestObjective objective = new QuestObjective(
                QuestObjective.ObjectiveType.COLLECT, 
                "minecraft:dirt", 
                1
            );
            
            // Простая награда
            QuestReward reward = new QuestReward(
                QuestReward.RewardType.SKILL_POINT_TOKEN, 
                1, 
                level * 100
            );
            
            return new Quest(questId, profession, level, title, description, objective, 30, reward);
        } catch (Exception e) {
            return null;
        }
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
        System.out.println("tryInitialPopulation() вызван. world: " + world + ", isPristine: " + isPristine());
        
        // Проверяем, что мир доступен и это серверная сторона
        if (world == null || world.isClient) {
            System.out.println("Мир недоступен или это клиентская сторона, пропускаем инициализацию");
            return;
        }
        
        // Проверяем количество валидных квестов
        int validQuestCount = getValidQuestCount();
        System.out.println("Валидных квестов на доске: " + validQuestCount);
        
        if (isPristine() || validQuestCount == 0) {
            System.out.println("Доска пустая или нет валидных квестов, начинаем инициализацию...");
            
            if (decrees.isEmpty()) {
                setDecree();
            }
            
            // Генерируем квесты через основной метод
            generateRandomQuests();
            
            markDirty();
            System.out.println("Инициализация завершена. Квестов в инвентаре: " + getQuestCount());
        }
    }
    
    /**
     * Получает количество валидных квестов (которые можно извлечь из билетов)
     */
    private int getValidQuestCount() {
        int count = 0;
        for (int i = 0; i < bounties.size(); i++) {
            ItemStack stack = bounties.getStack(i);
            if (!stack.isEmpty() && stack.getItem() instanceof QuestTicketItem) {
                Quest quest = QuestItem.getQuestFromStack(stack);
                if (quest != null) {
                    count++;
                }
            }
        }
        return count;
    }

    private boolean isPristine() {
        // Проверяем, есть ли предметы в инвентаре bounties
        boolean bountiesEmpty = true;
        for (int i = 0; i < bounties.size(); i++) {
            if (!bounties.getStack(i).isEmpty()) {
                bountiesEmpty = false;
                break;
            }
        }
        
        boolean isEmpty = bountiesEmpty && finishMap.isEmpty() && takenMask.isEmpty();
        System.out.println("isPristine(): bountiesEmpty=" + bountiesEmpty + 
                          ", finishMap.isEmpty=" + finishMap.isEmpty() + 
                          ", takenMask.isEmpty=" + takenMask.isEmpty() + 
                          ", result=" + isEmpty);
        return isEmpty;
    }

    private void randomlyUpdateBoard() {
        if (!(world instanceof ServerWorld)) return;

        Random random = new Random();
        int slotToAddTo = random.nextInt(21);
        int slotsToRemove = random.nextInt(3) > 0 ? (random.nextBoolean() ? 1 : 2) : 0;
        List<Integer> slotsToRemoveList = new ArrayList<>();
        for (int i = 0; i < slotsToRemove; i++) {
            int slot = random.nextInt(21);
            if (slot != slotToAddTo) slotsToRemoveList.add(slot);
        }

        // Сначала пытаемся взять квест из JSON файлов
        try {
            String[] professions = {"warrior", "cook", "courier", "brewer", "blacksmith", "miner"};
            String profession = professions[random.nextInt(professions.length)];
            
            List<Quest> availableJsonQuests = QuestGenerator.getRandomQuestsForProfession(profession, 1);
            
            if (!availableJsonQuests.isEmpty()) {
                // Используем квест из JSON
                Quest jsonQuest = availableJsonQuests.get(0);
                ItemStack questTicket = QuestTicketItem.createQuestTicket(jsonQuest);
                bounties.setStack(slotToAddTo, questTicket);
                System.out.println("Добавлен JSON квест " + jsonQuest.getTitle() + " в слот " + slotToAddTo);
            } else {
                // Fallback к BountifulQuestCreator
                int level = random.nextInt(10) + 1;
                
                BountifulQuestCreator creator = new BountifulQuestCreator(
                    (ServerWorld) world,
                    pos,
                    profession,
                    level,
                    world.getTime()
                );
                
                ItemStack newQuestItem = creator.createQuestItem();
                if (!newQuestItem.isEmpty()) {
                    bounties.setStack(slotToAddTo, newQuestItem);
                    System.out.println("Добавлен новый квест " + profession + " уровня " + level + " в слот " + slotToAddTo);
                }
            }
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при генерации квеста: " + e.getMessage());
            e.printStackTrace();
        }

        // Очищаем маски для обновленных слотов
        for (Set<Integer> mask : takenMask.values()) {
            mask.removeIf(slot -> slot == slotToAddTo || slotsToRemoveList.contains(slot));
        }
        
        // Удаляем квесты из указанных слотов
        slotsToRemoveList.forEach(slot -> {
            bounties.setStack(slot, ItemStack.EMPTY);
            System.out.println("Удален квест из слота " + slot);
        });

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