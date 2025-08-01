package io.github.apace100.origins.progression;

import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import dev.onyxstudios.cca.api.v3.component.tick.ServerTickingComponent;
import io.github.apace100.origins.Origins;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Map;

/**
 * Компонент для хранения прогрессии происхождений игрока
 */
public class OriginProgressionComponent implements AutoSyncedComponent, ServerTickingComponent {

    public static final ComponentKey<OriginProgressionComponent> KEY = ProgressionComponents.PROGRESSION;

    private final PlayerEntity player;
    private final Map<String, OriginProgression> progressions = new HashMap<>();
    private int tickCounter = 0;

    public OriginProgressionComponent(PlayerEntity player) {
        this.player = player;
    }

    /**
     * Получить прогрессию для происхождения
     */
    public OriginProgression getProgression(String originId) {
        return progressions.computeIfAbsent(originId, OriginProgression::new);
    }

    /**
     * Добавить опыт для текущего происхождения
     */
    public void addExperience(int exp) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        // Получаем текущее происхождение игрока
        String currentOrigin = getCurrentOriginId(serverPlayer);
        if (currentOrigin == null || currentOrigin.equals("origins:human")) return;

        OriginProgression progression = getProgression(currentOrigin);
        int oldLevel = progression.getLevel();
        progression.addExperience(exp);
        boolean leveledUp = progression.getLevel() > oldLevel;

        // Уведомляем игрока о получении опыта
        if (exp > 0) {
            serverPlayer.sendMessage(Text.literal("+" + exp + " опыта")
                .formatted(Formatting.GREEN), true);
        }

        // Уведомляем о повышении уровня
        if (leveledUp) {
            serverPlayer.sendMessage(Text.literal("🎉 Уровень повышен! ")
                .formatted(Formatting.GOLD)
                .append(Text.literal("Уровень " + progression.getLevel())
                    .formatted(Formatting.YELLOW)), false);

            // Уровень повышен
        }

        // Синхронизируем с клиентом
        KEY.sync(serverPlayer);
    }



    /**
     * Получить ID текущего происхождения игрока
     */
    private String getCurrentOriginId(ServerPlayerEntity player) {
        try {
            // Используем API Origins для получения текущего происхождения
            var originComponent = io.github.apace100.origins.registry.ModComponents.ORIGIN.get(player);
            var origin = originComponent.getOrigin(io.github.apace100.origins.origin.OriginLayers.getLayer(
                Origins.identifier("origin")));

            return origin != null ? origin.getIdentifier().toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Получить прогрессию текущего происхождения
     */
    public OriginProgression getCurrentProgression() {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return null;

        String currentOrigin = getCurrentOriginId(serverPlayer);
        if (currentOrigin == null) return null;

        return getProgression(currentOrigin);
    }

    /**
     * Установить уровень для происхождения (для команд/тестирования)
     */
    public void setLevel(String originId, int level) {
        OriginProgression progression = getProgression(originId);
        progression.setLevel(level);
        progression.setExperience(0);

        if (player instanceof ServerPlayerEntity serverPlayer) {
            KEY.sync(serverPlayer);
        }
    }

    @Override
    public void serverTick() {
        tickCounter++;

        // Каждые 20 тиков (1 секунда) проверяем пассивное получение опыта
        if (tickCounter % 20 == 0) {
            // Здесь можно добавить пассивное получение опыта
            // Например, за ношение определенных предметов или нахождение в определенных местах
        }
    }

    @Override
    public void readFromNbt(NbtCompound tag) {
        progressions.clear();

        NbtCompound progressionsNbt = tag.getCompound("progressions");
        for (String key : progressionsNbt.getKeys()) {
            NbtCompound progressionNbt = progressionsNbt.getCompound(key);
            OriginProgression progression = OriginProgression.fromNbt(key, progressionNbt);
            progressions.put(key, progression);
        }
    }

    @Override
    public void writeToNbt(NbtCompound tag) {
        NbtCompound progressionsNbt = new NbtCompound();

        for (Map.Entry<String, OriginProgression> entry : progressions.entrySet()) {
            progressionsNbt.put(entry.getKey(), entry.getValue().writeToNbt());
        }

        tag.put("progressions", progressionsNbt);
    }
}