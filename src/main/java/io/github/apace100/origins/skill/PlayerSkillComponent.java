package io.github.apace100.origins.skill;

import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import dev.onyxstudios.cca.api.v3.component.tick.ServerTickingComponent;
import io.github.apace100.origins.Origins;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginLayers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Map;

/**
 * Компонент для хранения навыков игрока с поддержкой уровней
 */
public class PlayerSkillComponent implements AutoSyncedComponent, ServerTickingComponent {
    
    public static final ComponentKey<PlayerSkillComponent> KEY =
        ComponentRegistry.getOrCreate(Origins.identifier("player_skills"), PlayerSkillComponent.class);
    
    private final PlayerEntity player;
    // Вместо одного skillLevels, делаем Map<professionId, Map<skillId, Integer>>
    private final Map<String, Map<String, Integer>> professionSkillLevels = new HashMap<>();
    private int tickCounter = 0;
    
    public PlayerSkillComponent(PlayerEntity player) {
        this.player = player;
    }

    /**
     * Изучает навык или повышает его уровень
     */
    public void learnSkill(String skillId) {
        String currentClass = getCurrentClass();
        if (currentClass == null) return;
        SkillTreeHandler.SkillTree skillTree = SkillTreeHandler.getSkillTree(currentClass);
        if (skillTree == null) return;
        SkillTreeHandler.Skill skill = null;
        for (SkillTreeHandler.Skill s : skillTree.getAllSkills()) {
            if (s.getId().equals(skillId)) {
                skill = s;
                break;
            }
        }
        if (skill == null) return;
        if (!canLearnSkill(skill)) return;
        io.github.apace100.origins.profession.ProfessionComponent professionComponent =
            io.github.apace100.origins.profession.ProfessionComponent.KEY.get(player);
        io.github.apace100.origins.profession.ProfessionProgress professionProgress = professionComponent.getCurrentProgress();
        if (professionProgress == null || professionProgress.getSkillPoints() <= 0) return;
        Map<String, Integer> skillLevels = professionSkillLevels.computeIfAbsent(currentClass, k -> new HashMap<>());
        int currentLevel = skillLevels.getOrDefault(skillId, 0);
        if (currentLevel < skill.getMaxLevel()) {
            skillLevels.put(skillId, currentLevel + 1);
            professionProgress.spendSkillPoint();
            // Синхронизируем всегда
            KEY.sync(player);
            professionComponent.KEY.sync(player);
        }
        if (player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.sendMessage(
                Text.literal("Вы изучили навык: ")
                    .formatted(Formatting.GREEN)
                    .append(Text.literal(skill.getName()).formatted(Formatting.GOLD)),
                false
            );
        }
    }

    /**
     * Проверяет, может ли игрок изучить навык
     */
    public boolean canLearnSkill(SkillTreeHandler.Skill skill) {
        // Проверяем уровень игрока
        int playerLevel = getPlayerLevel();
        if (playerLevel < skill.getRequiredLevel()) {
            return false;
        }

        String currentClass = getCurrentClass();
        if (currentClass == null) return false;

        Map<String, Integer> skillLevels = professionSkillLevels.computeIfAbsent(currentClass, k -> new HashMap<>());
        
        // Проверяем родительский навык
        if (skill.getParentId() != null) {
            int parentLevel = skillLevels.getOrDefault(skill.getParentId(), 0);
            // Изменяем условие: родительский навык должен быть максимального уровня
            SkillTreeHandler.SkillTree skillTree = SkillTreeHandler.getSkillTree(currentClass);
            if (skillTree != null) {
                SkillTreeHandler.Skill parentSkill = null;
                for (SkillTreeHandler.Skill s : skillTree.getAllSkills()) {
                    if (s.getId().equals(skill.getParentId())) {
                        parentSkill = s;
                        break;
                    }
                }
                if (parentSkill != null && parentLevel < parentSkill.getMaxLevel()) {
                    return false;
                }
            } else if (parentLevel == 0) {
                return false;
            }
        }

        // Проверяем текущий уровень навыка
        int currentLevel = skillLevels.getOrDefault(skill.getId(), 0);
        if (currentLevel >= skill.getMaxLevel()) {
            return false;
        }

        // Проверяем наличие очков навыков
        io.github.apace100.origins.profession.ProfessionComponent professionComponent =
            io.github.apace100.origins.profession.ProfessionComponent.KEY.get(player);
        io.github.apace100.origins.profession.ProfessionProgress professionProgress = professionComponent.getCurrentProgress();
        int availablePoints = professionProgress != null ? professionProgress.getSkillPoints() : 0;
        return availablePoints > 0;
    }

    /**
     * Получает доступные очки навыков
     */
    public int getAvailableSkillPoints() {
        io.github.apace100.origins.profession.ProfessionComponent professionComponent =
            io.github.apace100.origins.profession.ProfessionComponent.KEY.get(player);
        io.github.apace100.origins.profession.ProfessionProgress professionProgress = professionComponent.getCurrentProgress();
        return professionProgress != null ? professionProgress.getSkillPoints() : 0;
    }

    /**
     * Получает уровень игрока
     */
    private int getPlayerLevel() {
        // Получаем компонент прогрессии
        ServerPlayerEntity serverPlayer = player instanceof ServerPlayerEntity ? (ServerPlayerEntity) player : null;
        if (serverPlayer == null) return 1;
        
        try {
            io.github.apace100.origins.profession.ProfessionComponent progressionComponent = 
                io.github.apace100.origins.profession.ProfessionComponent.KEY.get(serverPlayer);
            
            if (progressionComponent == null) {
                return 1;
            }
            
            io.github.apace100.origins.profession.ProfessionProgress progress = progressionComponent.getCurrentProgress();
            return progress != null ? progress.getLevel() : 1;
        } catch (Exception e) {
            return 1;
        }
    }

    /**
     * Получает текущий класс игрока
     */
    private String getCurrentClass() {
        try {
            // Используем API Origins для получения текущего происхождения
            OriginComponent originComponent = io.github.apace100.origins.registry.ModComponents.ORIGIN.get(player);
            Origin origin = originComponent.getOrigin(OriginLayers.getLayer(Origins.identifier("origin")));
            return origin != null ? origin.getIdentifier().toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Получает уровни всех навыков игрока
     */
    public Map<String, Integer> getSkillLevels() {
        String currentClass = getCurrentClass();
        return new HashMap<>(professionSkillLevels.getOrDefault(currentClass, new HashMap<>()));
    }

    /**
     * Получает уровень конкретного навыка
     */
    public int getSkillLevel(String skillId) {
        String currentClass = getCurrentClass();
        return professionSkillLevels.getOrDefault(currentClass, new HashMap<>()).getOrDefault(skillId, 0);
    }

    /**
     * Проверяет, изучен ли навык
     */
    public boolean hasSkill(String skillId) {
        return getSkillLevel(skillId) > 0;
    }

    /**
     * Сбрасывает все навыки игрока
     */
    public void resetSkills() {
        String currentClass = getCurrentClass();
        if (currentClass != null) {
            professionSkillLevels.remove(currentClass);
        }
        if (player instanceof ServerPlayerEntity serverPlayer) {
            KEY.sync(serverPlayer);
            serverPlayer.sendMessage(
                Text.literal("Все навыки были сброшены!")
                    .formatted(Formatting.YELLOW),
                false
            );
        }
    }

    /**
     * Применяет эффекты пассивных навыков
     */
    public void applyPassiveSkillEffects() {
        String currentClass = getCurrentClass();
        if (currentClass == null) return;

        SkillTreeHandler.SkillTree skillTree = SkillTreeHandler.getSkillTree(currentClass);
        if (skillTree == null) return;

        // Проходим по всем изученным навыкам
        for (Map.Entry<String, Integer> entry : getSkillLevels().entrySet()) {
            String skillId = entry.getKey();
            int skillLevel = entry.getValue();
            
            if (skillLevel > 0) {
                // Находим навык
                SkillTreeHandler.Skill skill = null;
                for (SkillTreeHandler.Skill s : skillTree.getAllSkills()) {
                    if (s.getId().equals(skillId)) {
                        skill = s;
                        break;
                    }
                }
                
                if (skill != null && skill.getType() == SkillTreeHandler.SkillType.PASSIVE) {
                    // Применяем эффекты пассивного навыка
                    applySkillEffect(skill, skillLevel);
                }
            }
        }
    }

    /**
     * Применяет эффект конкретного навыка
     */
    private void applySkillEffect(SkillTreeHandler.Skill skill, int level) {
        // Здесь будет логика применения эффектов навыков
        // Пока что заглушка - в будущем можно расширить
        switch (skill.getId()) {
            case "blacksmith_hammer_mastery":
                // Увеличение скорости крафта
                break;
            case "blacksmith_quality_boost":
                // Увеличение качества предметов
                break;
            case "cook_speed_boost":
                // Увеличение скорости готовки
                break;
            case "brewer_efficiency":
                // Увеличение эффективности варки
                break;
            // Добавить другие навыки по мере необходимости
        }
    }

    @Override
    public void serverTick() {
        tickCounter++;
        
        // Применяем пассивные эффекты каждые 20 тиков (1 секунда)
        if (tickCounter % 20 == 0) {
            applyPassiveSkillEffects();
        }
    }

    @Override
    public void readFromNbt(NbtCompound tag) {
        professionSkillLevels.clear();
        NbtCompound allSkillsTag = tag.getCompound("professionSkills");
        for (String profession : allSkillsTag.getKeys()) {
            NbtCompound skillsTag = allSkillsTag.getCompound(profession);
            Map<String, Integer> skillLevels = new HashMap<>();
            for (String key : skillsTag.getKeys()) {
                skillLevels.put(key, skillsTag.getInt(key));
            }
            professionSkillLevels.put(profession, skillLevels);
        }
        if (player instanceof ServerPlayerEntity serverPlayer) {
            KEY.sync(serverPlayer);
        }
    }

    @Override
    public void writeToNbt(NbtCompound tag) {
        NbtCompound allSkillsTag = new NbtCompound();
        for (Map.Entry<String, Map<String, Integer>> entry : professionSkillLevels.entrySet()) {
            NbtCompound skillsTag = new NbtCompound();
            for (Map.Entry<String, Integer> skillEntry : entry.getValue().entrySet()) {
                skillsTag.putInt(skillEntry.getKey(), skillEntry.getValue());
            }
            allSkillsTag.put(entry.getKey(), skillsTag);
        }
        tag.put("professionSkills", allSkillsTag);
    }
}