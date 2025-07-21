package io.github.apace100.origins.profession;

import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import dev.onyxstudios.cca.api.v3.component.tick.ServerTickingComponent;
import io.github.apace100.origins.Origins;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginLayer;
import io.github.apace100.origins.origin.OriginLayers;
import io.github.apace100.origins.registry.ModComponents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Компонент для хранения прогресса профессий игрока
 */
public class ProfessionComponent implements AutoSyncedComponent, ServerTickingComponent {

    public static final ComponentKey<ProfessionComponent> KEY =
        ComponentRegistry.getOrCreate(Origins.identifier("profession"), ProfessionComponent.class);

    private final PlayerEntity player;
    private final Map<Identifier, ProfessionProgress> progressMap = new HashMap<>();
    private int tickCounter = 0;

    public ProfessionComponent(PlayerEntity player) {
        this.player = player;
    }

    /**
     * Получает прогресс для указанной профессии
     */
    public ProfessionProgress getProgress(Identifier professionId) {
        return progressMap.computeIfAbsent(professionId, id -> new ProfessionProgress(id));
    }

    /**
     * Получает прогресс для текущей профессии игрока
     */
    public ProfessionProgress getCurrentProgress() {
        Identifier currentProfessionId = getCurrentProfessionId();
        if (currentProfessionId == null) return null;
        return getProgress(currentProfessionId);
    }

    /**
     * Получает идентификатор текущей профессии игрока
     */
    public Identifier getCurrentProfessionId() {
        OriginComponent originComponent = ModComponents.ORIGIN.get(player);
        OriginLayer mainLayer = OriginLayers.getLayer(new Identifier(Origins.MODID, "origin"));
        
        if (mainLayer == null) return null;
        
        Origin origin = originComponent.getOrigin(mainLayer);
        if (origin == null || origin == Origin.EMPTY) return null;
        
        // Преобразуем идентификатор происхождения в идентификатор профессии
        String originPath = origin.getIdentifier().getPath();
        return new Identifier(Origins.MODID, originPath);
    }

    /**
     * Добавляет опыт для текущей профессии
     */
    public boolean addExperience(int exp) {
        if (!(player instanceof ServerPlayerEntity)) return false;

        Identifier currentProfessionId = getCurrentProfessionId();
        if (currentProfessionId == null) return false;

        ProfessionProgress progress = getProgress(currentProfessionId);
        boolean leveledUp = progress.addExperience(exp);

        // Отправляем сообщение об опыте
        if (exp > 0 && player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.sendMessage(Text.literal("+" + exp + " опыта")
                .formatted(Formatting.GREEN), true);
        }

        if (leveledUp) {
            // Уведомляем игрока о повышении уровня
            Profession profession = ProfessionRegistry.get(currentProfessionId);
            if (profession != null) {
                player.sendMessage(
                    Text.literal("Уровень профессии повышен! ")
                        .append(profession.getName())
                        .append(Text.literal(" достиг уровня " + progress.getLevel()))
                        .formatted(Formatting.GREEN, Formatting.BOLD),
                    true
                );
            }
        }

        // Синхронизируем данные с клиентом
        if (player instanceof ServerPlayerEntity serverPlayer) {
            KEY.sync(serverPlayer);
        }
        return leveledUp;
    }
    
    /**
     * Устанавливает уровень текущей профессии
     */
    public void setLevel(int level) {
        ProfessionProgress currentProgress = getCurrentProgress();
        if (currentProgress != null) {
            currentProgress.setLevel(level);
            
            // Синхронизируем данные с клиентом
            if (player instanceof ServerPlayerEntity serverPlayer) {
                KEY.sync(serverPlayer);
            }
        }
    }

    @Override
    public void serverTick() {
        // Периодическая синхронизация (каждые 20 тиков = 1 секунда)
        if (++tickCounter >= 20) {
            tickCounter = 0;
            if (player instanceof ServerPlayerEntity serverPlayer) {
                KEY.sync(serverPlayer);
            }
        }
    }

    @Override
    public void readFromNbt(NbtCompound tag) {
        progressMap.clear();
        
        if (tag.contains("Professions", NbtElement.LIST_TYPE)) {
            NbtList list = tag.getList("Professions", NbtElement.COMPOUND_TYPE);
            
            for (int i = 0; i < list.size(); i++) {
                NbtCompound progressTag = list.getCompound(i);
                String id = progressTag.getString("Id");
                Identifier professionId = new Identifier(id);
                
                ProfessionProgress progress = new ProfessionProgress(professionId);
                progress.readFromNbt(progressTag);
                
                progressMap.put(professionId, progress);
            }
        }
    }

    @Override
    public void writeToNbt(NbtCompound tag) {
        NbtList list = new NbtList();
        
        for (Map.Entry<Identifier, ProfessionProgress> entry : progressMap.entrySet()) {
            NbtCompound progressTag = new NbtCompound();
            progressTag.putString("Id", entry.getKey().toString());
            
            entry.getValue().writeToNbt(progressTag);
            list.add(progressTag);
        }
        
        tag.put("Professions", list);
    }
}