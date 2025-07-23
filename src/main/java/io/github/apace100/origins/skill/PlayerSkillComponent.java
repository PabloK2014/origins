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
    
    // Хранение активного навыка для каждой профессии
    private final Map<String, String> activeSkills = new HashMap<>();
    
    // Хранение состояния навыков (кулдауны, готовность и т.д.)
    private final Map<String, Long> skillCooldowns = new HashMap<>();
    private final Map<String, Boolean> skillStates = new HashMap<>();
    
    public PlayerSkillComponent(PlayerEntity player) {
        this.player = player;
    }

    /**
     * Изучает навык или повышает его уровень
     */
    public void learnSkill(String skillId) {
        Origins.LOGGER.info("Попытка изучить навык: {}", skillId);
        String currentClass = getCurrentClass();
        if (currentClass == null) {
            Origins.LOGGER.info("Не удалось изучить навык: не найден текущий класс");
            return;
        }
        SkillTreeHandler.SkillTree skillTree = SkillTreeHandler.getSkillTree(currentClass);
        if (skillTree == null) {
            Origins.LOGGER.info("Не удалось изучить навык: не найдено дерево навыков для класса {}", currentClass);
            return;
        }
        SkillTreeHandler.Skill skill = null;
        for (SkillTreeHandler.Skill s : skillTree.getAllSkills()) {
            if (s.getId().equals(skillId)) {
                skill = s;
                break;
            }
        }
        if (skill == null) {
            Origins.LOGGER.info("Не удалось изучить навык: навык {} не найден в дереве", skillId);
            return;
        }
        if (!canLearnSkill(skill)) {
            Origins.LOGGER.info("Не удалось изучить навык: навык {} недоступен для изучения", skillId);
            return;
        }
        io.github.apace100.origins.profession.ProfessionComponent professionComponent =
            io.github.apace100.origins.profession.ProfessionComponent.KEY.get(player);
        io.github.apace100.origins.profession.ProfessionProgress professionProgress = professionComponent.getCurrentProgress();
        if (professionProgress == null || professionProgress.getSkillPoints() <= 0) {
            Origins.LOGGER.info("Не удалось изучить навык: нет очков навыков");
            return;
        }
        Map<String, Integer> skillLevels = professionSkillLevels.computeIfAbsent(currentClass, k -> new HashMap<>());
        int currentLevel = skillLevels.getOrDefault(skillId, 0);
        if (currentLevel < skill.getMaxLevel()) {
            skillLevels.put(skillId, currentLevel + 1);
            professionProgress.spendSkillPoint();
            Origins.LOGGER.info("Навык {} изучен: новый уровень {}/{}", skillId, currentLevel + 1, skill.getMaxLevel());
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
        Origins.LOGGER.info("Проверка навыка: " + skill.getId());
        Origins.LOGGER.info("Текущий класс: " + getCurrentClass());

        String currentClass = getCurrentClass();
        if (currentClass == null) {
            Origins.LOGGER.info("Навык {} недоступен: не найден текущий класс", skill.getId());
            return false;
        }

        Map<String, Integer> skillLevels = professionSkillLevels.computeIfAbsent(currentClass, k -> new HashMap<>());
        Origins.LOGGER.info("Текущие уровни навыков: " + skillLevels);
        
        // Проверяем текущий уровень навыка
        int currentLevel = skillLevels.getOrDefault(skill.getId(), 0);
        Origins.LOGGER.info("Текущий уровень навыка {}: {}", skill.getId(), currentLevel);
        if (currentLevel >= skill.getMaxLevel()) {
            Origins.LOGGER.info("Навык {} недоступен: достигнут максимальный уровень {}/{}", 
                skill.getId(), currentLevel, skill.getMaxLevel());
            return false;
        }

        // Проверяем родительский навык
        if (skill.getParentId() != null) {
            Origins.LOGGER.info("Проверка родительского навыка: " + skill.getParentId());
            int parentLevel = skillLevels.getOrDefault(skill.getParentId(), 0);
            Origins.LOGGER.info("Уровень родительского навыка: " + parentLevel);
            
            SkillTreeHandler.SkillTree skillTree = SkillTreeHandler.getSkillTree(currentClass);
            if (skillTree != null) {
                SkillTreeHandler.Skill parentSkill = null;
                for (SkillTreeHandler.Skill s : skillTree.getAllSkills()) {
                    Origins.LOGGER.info("Сравниваем {} с {}", s.getId(), skill.getParentId());
                    if (s.getId().equals(skill.getParentId())) {
                        parentSkill = s;
                        break;
                    }
                }
                if (parentSkill != null) {
                    Origins.LOGGER.info("Найден родительский навык: {} (макс. уровень: {})", 
                        parentSkill.getId(), parentSkill.getMaxLevel());
                    if (parentLevel < parentSkill.getMaxLevel()) {
                        Origins.LOGGER.info("Навык {} недоступен: родительский навык не прокачан до максимума ({}/{})", 
                            skill.getId(), parentLevel, parentSkill.getMaxLevel());
                        return false;
                    }
                    Origins.LOGGER.info("Родительский навык {} прокачан до максимума ({}/{})", 
                        parentSkill.getId(), parentLevel, parentSkill.getMaxLevel());
                } else {
                    Origins.LOGGER.info("Родительский навык {} не найден в дереве навыков", skill.getParentId());
                    return false;
                }
            } else {
                Origins.LOGGER.info("Дерево навыков не найдено для класса {}", currentClass);
                return false;
            }
        }

        Origins.LOGGER.info("Навык {} доступен для изучения: уровень {}/{}", 
            skill.getId(), currentLevel, skill.getMaxLevel());
        return true;
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
    public int getPlayerLevel() {
        // Получаем компонент прогрессии
        ServerPlayerEntity serverPlayer = player instanceof ServerPlayerEntity ? (ServerPlayerEntity) player : null;
        if (serverPlayer == null) {
            io.github.apace100.origins.profession.ProfessionComponent professionComponent =
                io.github.apace100.origins.profession.ProfessionComponent.KEY.get(player);
            io.github.apace100.origins.profession.ProfessionProgress professionProgress = professionComponent.getCurrentProgress();
            return professionProgress != null ? professionProgress.getLevel() : 1;
        }
        
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
    public String getCurrentClass() {
        try {
            // Используем API Origins для получения текущего происхождения
            OriginComponent originComponent = io.github.apace100.origins.registry.ModComponents.ORIGIN.get(player);
            Origin origin = originComponent.getOrigin(OriginLayers.getLayer(Origins.identifier("origin")));
            return origin != null ? origin.getIdentifier().toString() : null;
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при получении текущего класса: " + e.getMessage());
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
            activeSkills.remove(currentClass);
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
     * Устанавливает активный навык для текущего класса
     */
    public void setActiveSkill(String skillId) {
        String currentClass = getCurrentClass();
        if (currentClass != null) {
            activeSkills.put(currentClass, skillId);
            if (player instanceof ServerPlayerEntity serverPlayer) {
                KEY.sync(serverPlayer);
            }
        }
    }
    
    /**
     * Получает активный навык для текущего класса
     */
    public String getActiveSkill() {
        String currentClass = getCurrentClass();
        return currentClass != null ? activeSkills.get(currentClass) : null;
    }
    
    /**
     * Проверяет, установлен ли активный навык
     */
    public boolean hasActiveSkill() {
        String activeSkill = getActiveSkill();
        return activeSkill != null && !activeSkill.isEmpty();
    }
    
    /**
     * Устанавливает кулдаун для навыка
     */
    public void setSkillCooldown(String skillId, long cooldownTicks) {
        long currentTime = player.getWorld().getTime();
        skillCooldowns.put(skillId, currentTime + cooldownTicks);
        if (player instanceof ServerPlayerEntity serverPlayer) {
            KEY.sync(serverPlayer);
        }
    }
    
    /**
     * Проверяет, находится ли навык на кулдауне
     */
    public boolean isSkillOnCooldown(String skillId) {
        Long cooldownEnd = skillCooldowns.get(skillId);
        if (cooldownEnd == null) return false;
        
        long currentTime = player.getWorld().getTime();
        return currentTime < cooldownEnd;
    }
    
    /**
     * Получает оставшееся время кулдауна в тиках
     */
    public long getSkillCooldownRemaining(String skillId) {
        Long cooldownEnd = skillCooldowns.get(skillId);
        if (cooldownEnd == null) return 0;
        
        long currentTime = player.getWorld().getTime();
        return Math.max(0, cooldownEnd - currentTime);
    }
    
    /**
     * Устанавливает состояние навыка
     */
    public void setSkillState(String skillId, boolean state) {
        skillStates.put(skillId, state);
        if (player instanceof ServerPlayerEntity serverPlayer) {
            KEY.sync(serverPlayer);
        }
    }
    
    /**
     * Получает состояние навыка
     */
    public boolean getSkillState(String skillId) {
        return skillStates.getOrDefault(skillId, false);
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
        activeSkills.clear();
        skillCooldowns.clear();
        skillStates.clear();
        
        NbtCompound allSkillsTag = tag.getCompound("professionSkills");
        Origins.LOGGER.info("Загрузка навыков из NBT: {}", allSkillsTag);
        for (String profession : allSkillsTag.getKeys()) {
            NbtCompound skillsTag = allSkillsTag.getCompound(profession);
            Map<String, Integer> skillLevels = new HashMap<>();
            for (String key : skillsTag.getKeys()) {
                skillLevels.put(key, skillsTag.getInt(key));
            }
            professionSkillLevels.put(profession, skillLevels);
        }
        
        // Загружаем активные навыки
        NbtCompound activeSkillsTag = tag.getCompound("activeSkills");
        for (String profession : activeSkillsTag.getKeys()) {
            activeSkills.put(profession, activeSkillsTag.getString(profession));
        }
        
        // Загружаем кулдауны навыков
        NbtCompound cooldownsTag = tag.getCompound("skillCooldowns");
        for (String skillId : cooldownsTag.getKeys()) {
            skillCooldowns.put(skillId, cooldownsTag.getLong(skillId));
        }
        
        // Загружаем состояния навыков
        NbtCompound statesTag = tag.getCompound("skillStates");
        for (String skillId : statesTag.getKeys()) {
            skillStates.put(skillId, statesTag.getBoolean(skillId));
        }
        
        Origins.LOGGER.info("Загруженные навыки: {}", professionSkillLevels);
        Origins.LOGGER.info("Загруженные активные навыки: {}", activeSkills);
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
        
        // Сохраняем активные навыки
        NbtCompound activeSkillsTag = new NbtCompound();
        for (Map.Entry<String, String> entry : activeSkills.entrySet()) {
            activeSkillsTag.putString(entry.getKey(), entry.getValue());
        }
        tag.put("activeSkills", activeSkillsTag);
        
        // Сохраняем кулдауны навыков
        NbtCompound cooldownsTag = new NbtCompound();
        for (Map.Entry<String, Long> entry : skillCooldowns.entrySet()) {
            cooldownsTag.putLong(entry.getKey(), entry.getValue());
        }
        tag.put("skillCooldowns", cooldownsTag);
        
        // Сохраняем состояния навыков
        NbtCompound statesTag = new NbtCompound();
        for (Map.Entry<String, Boolean> entry : skillStates.entrySet()) {
            statesTag.putBoolean(entry.getKey(), entry.getValue());
        }
        tag.put("skillStates", statesTag);
        
        Origins.LOGGER.info("Сохранение навыков в NBT: {}", allSkillsTag);
        Origins.LOGGER.info("Сохранение активных навыков в NBT: {}", activeSkillsTag);
    }
}