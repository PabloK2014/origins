package io.github.apace100.origins.quest;

import io.github.apace100.origins.Origins;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

/**
 * Базовый класс для классовых досок объявлений, которые получают квесты через API
 */
public abstract class ClassBountyBoardBlockEntity extends BountyBoardBlockEntity {
    
    private long lastApiUpdate = 0;
    private boolean apiQuestsLoaded = false;
    
    public ClassBountyBoardBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    
    /**
     * Возвращает класс игрока для этой доски
     */
    protected abstract String getBoardClass();
    
    @Override
    public Text getDisplayName() {
        String className = getBoardClass();
        return Text.translatable("gui.origins.bounty_board." + className + ".title");
    }
    
    /**
     * Загружает квесты через API вместо генерации случайных
     */
    @Override
    public void tryInitialPopulation() {
        if (world == null || world.isClient) {
            return;
        }
        
        // Проверяем, нужно ли загрузить квесты через API
        if (!apiQuestsLoaded || shouldUpdateFromApi()) {
            loadQuestsFromApi();
        }
        
        // Классовые доски НЕ используют fallback - только API квесты
        // Если API недоступен, доска остается пустой
    }
    
    /**
     * Загружает квесты через API
     */
    private void loadQuestsFromApi() {
        if (!(world instanceof ServerWorld)) {
            return;
        }
        
        QuestApiManager manager = QuestApiManager.getInstance();
        
        // Используем новый метод updateBoard для обновления этой доски
        manager.updateBoard(this);
        
        apiQuestsLoaded = true;
        lastApiUpdate = world.getTime();
        
        // Уведомляем клиентов об изменениях в интерфейсе
        markDirty();
        if (world instanceof ServerWorld) {
            ((ServerWorld) world).getChunkManager().markForUpdate(pos);
        }
        
        Origins.LOGGER.info("Updated " + getBoardClass() + " board via API manager");
    }
    
    /**
     * Проверяет, нужно ли обновить квесты с API
     */
    private boolean shouldUpdateFromApi() {
        if (world == null) return false;
        
        // Обновляем каждые 30 минут (36000 тиков)
        return world.getTime() - lastApiUpdate >= 36000L;
    }
    
    /**
     * Получает время до следующего обновления в минутах
     */
    public int getMinutesUntilUpdate() {
        if (world == null || !(world instanceof ServerWorld)) return 0;
        
        QuestApiManager manager = QuestApiManager.getInstance();
        return manager.getMinutesUntilNextUpdate(getBoardClass(), (ServerWorld) world);
    }
    
    /**
     * Получает время до следующего обновления в секундах
     */
    public int getSecondsUntilUpdate() {
        if (world == null || !(world instanceof ServerWorld)) return 0;
        
        QuestApiManager manager = QuestApiManager.getInstance();
        long ticksUntilUpdate = manager.getTimeUntilNextUpdate(getBoardClass(), (ServerWorld) world);
        int totalSeconds = (int) (ticksUntilUpdate / 20);
        
        // Возвращаем только секунды (остаток от деления на 60)
        return totalSeconds % 60;
    }
    
    /**
     * Принудительно обновляет квесты с API
     */
    public void forceUpdateFromApi() {
        if (world instanceof ServerWorld) {
            QuestApiManager.getInstance().forceUpdateClass(getBoardClass(), (ServerWorld) world);
            loadQuestsFromApi();
        }
    }
    
    /**
     * Переопределяем tick для обработки обновлений API
     */
    public static void tick(World world, BlockPos pos, BlockState state, ClassBountyBoardBlockEntity entity) {
        if (world.isClient) return;
        
        // Инициализация если нужно
        entity.tryInitialPopulation();
        
        // Проверяем обновления API каждые 20 тиков (1 секунда)
        if (world.getTime() % 20L == 0L) {
            // Принудительно обновляем доску каждую секунду, чтобы подхватить новые квесты
            entity.loadQuestsFromApi();
        }
        
        // Обработка декретов (если нужно)
        if (world.getTime() % 20L == 0L) {
            // Логика обработки декретов
        }
    }
    
    /**
     * Переопределяем метод для предотвращения случайных обновлений
     */
    @Override
    public void refreshQuests() {
        // Для классовых досок обновляем ТОЛЬКО через API
        // Если API недоступен, доска остается пустой
        if (QuestApiManager.getInstance().isApiAvailable()) {
            forceUpdateFromApi();
        }
        // НЕ используем fallback - доска должна быть пустой без API
    }
    
    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        
        if (nbt.contains("lastApiUpdate")) {
            lastApiUpdate = nbt.getLong("lastApiUpdate");
        }
        
        if (nbt.contains("apiQuestsLoaded")) {
            apiQuestsLoaded = nbt.getBoolean("apiQuestsLoaded");
        }
    }
    
    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        
        nbt.putLong("lastApiUpdate", lastApiUpdate);
        nbt.putBoolean("apiQuestsLoaded", apiQuestsLoaded);
    }
}