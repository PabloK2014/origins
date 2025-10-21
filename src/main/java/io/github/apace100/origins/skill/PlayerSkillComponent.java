package io.github.apace100.origins.skill;

import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import dev.onyxstudios.cca.api.v3.component.tick.ServerTickingComponent;
import io.github.apace100.origins.Origins;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginLayers;
import io.github.apace100.origins.registry.ModComponents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

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
    
    // Система отложенной установки ловушки
    private BlockPos delayedTrapPosition = null;
    private boolean hasDelayedTrap = false;
    private int trapSetupDelay = 0;
    
    // Система энергии
    private int currentEnergy = 20; // Текущая энергия
    private int maxEnergy = 20; // Максимальная энергия
    private int energyRegenRate = 1; // Восстановление энергии за тик (20 тиков = 1 секунда)
    private int energyRegenDelay = 0; // Задержка перед восстановлением энергии после использования навыка
    
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
    
    // ========== СИСТЕМА ЭНЕРГИИ ==========
    
    /**
     * Получает текущую энергию
     */
    public int getCurrentEnergy() {
        return currentEnergy;
    }
    
    /**
     * Получает максимальную энергию
     */
    public int getMaxEnergy() {
        return maxEnergy;
    }
    
    /**
     * Устанавливает текущую энергию
     */
    public void setCurrentEnergy(int energy) {
        this.currentEnergy = Math.max(0, Math.min(energy, maxEnergy));
        if (player instanceof ServerPlayerEntity serverPlayer) {
            KEY.sync(serverPlayer);
        }
    }
    
    /**
     * Устанавливает максимальную энергию
     */
    public void setMaxEnergy(int maxEnergy) {
        this.maxEnergy = Math.max(1, maxEnergy);
        this.currentEnergy = Math.min(this.currentEnergy, this.maxEnergy);
        if (player instanceof ServerPlayerEntity serverPlayer) {
            KEY.sync(serverPlayer);
        }
    }
    
    /**
     * Проверяет, хватает ли энергии для использования навыка
     */
    public boolean hasEnoughEnergy(int requiredEnergy) {
        return currentEnergy >= requiredEnergy;
    }
    
    /**
     * Тратит энергию на использование навыка
     */
    public boolean consumeEnergy(int amount) {
        if (currentEnergy >= amount) {
            currentEnergy -= amount;
            energyRegenDelay = 60; // 3 секунды задержки перед восстановлением
            if (player instanceof ServerPlayerEntity serverPlayer) {
                KEY.sync(serverPlayer);
            }
            return true;
        }
        return false;
    }
    
    /**
     * Восстанавливает энергию
     */
    public void restoreEnergy(int amount) {
        currentEnergy = Math.min(currentEnergy + amount, maxEnergy);
        if (player instanceof ServerPlayerEntity serverPlayer) {
            KEY.sync(serverPlayer);
        }
    }
    
    /**
     * Получает процент энергии (0.0 - 1.0)
     */
    public float getEnergyPercentage() {
        return (float) currentEnergy / maxEnergy;
    }

    public int getEnergyRegenRate() {
        return energyRegenRate;
    }

    public void setEnergyRegenRate(int regenRate) {
        this.energyRegenRate = Math.max(1, regenRate);
        if (player instanceof ServerPlayerEntity serverPlayer) {
            KEY.sync(serverPlayer);
        }
    }
    
    // ========== СИСТЕМА ОТЛОЖЕННОЙ УСТАНОВКИ ЛОВУШКИ ==========
    
    public void setDelayedTrap(BlockPos pos) {
        this.delayedTrapPosition = pos;
        this.hasDelayedTrap = true;
        this.trapSetupDelay = 4; // 0.2 секунды (4 тика при 20 тпс)
    }
    
    public boolean hasDelayedTrap() {
        return hasDelayedTrap;
    }
    
    public BlockPos getDelayedTrapPosition() {
        return delayedTrapPosition;
    }
    
    public void clearDelayedTrap() {
        this.hasDelayedTrap = false;
        this.delayedTrapPosition = null;
        this.trapSetupDelay = 0;
    }
    
    public boolean updateDelayedTrap() {
        if (hasDelayedTrap && trapSetupDelay > 0) {
            trapSetupDelay--;
            if (trapSetupDelay <= 0) {
                return true; // Время установить ловушку
            }
        }
        return false; // Еще не время
    }
    
    /**
     * Проверяет, можно ли использовать навык (проверяет энергию и кулдаун)
     */
    public boolean canUseSkill(String skillId, int energyCost) {
        return hasEnoughEnergy(energyCost) && !isSkillOnCooldown(skillId);
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
        
        // Применяем специфические эффекты для курьера
        if ("origins:courier".equals(currentClass) && player instanceof ServerPlayerEntity serverPlayer) {
            // Применяем эффекты магнитных карманов
            int magneticPocketsLevel = getSkillLevel("magnetic_pockets");
            if (magneticPocketsLevel > 0) {
                CourierSkillHandler.handleMagneticPockets(serverPlayer, magneticPocketsLevel);
            }
            
            // Применяем эффекты базовой скорости
            int speedBasicLevel = getSkillLevel("speed_basic");
            if (speedBasicLevel > 0) {
                // Скорость будет применяться через миксины
                // Здесь мы просто регистрируем, что скилл активен
            }
            
            // Применяем эффекты снижения голода
            int hungerReductionLevel = getSkillLevel("hunger_reduction");
            if (hungerReductionLevel > 0) {
                // Снижение голода будет применяться через миксины
                // Здесь мы просто регистрируем, что скилл активен
            }
            
            // Применяем эффекты сумки для еды
            int inventorySlotsBasicLevel = getSkillLevel("inventory_slots_basic");
            if (inventorySlotsBasicLevel > 0) {
                // Дополнительные слоты инвентаря будут применяться через миксины
                // Здесь мы просто регистрируем, что скилл активен
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
                
            // Пассивные скиллы повара
            case "fresh_product":
                // Еда восстанавливает на 2% сытости больше за уровень
                // Эффект будет применяться в миксинах при потреблении еды
                break;
            case "hearty_meal":
                // Эффект сытости длится в 2 раза дольше
                // Эффект будет применяться в миксинах при потреблении еды
                break;
            case "chef_master":
                // Приготовленная еда даёт случайные позитивные эффекты
                // Эффект будет применяться в миксинах при потреблении еды
                break;
            case "flambe":
                // При убийстве подожжённого врага происходит взрыв
                // Эффект будет применяться в миксинах при убийстве врагов
                break;
            case "fire_immunity":
                // Иммунитет к огню и лаве
                // Эффект будет применяться в миксинах при получении урона
                break;
            case "ready":
                // Приготовление еды даёт бафф 'Вдохновение' (+10% урона)
                // Эффект будет применяться в миксинах при потреблении еды
                break;
            case "quick_snack":
                // Можно есть на бегу без замедления
                // Эффект будет применяться в миксинах при потреблении еды
                break;
                
            // Пассивные скиллы курьера
            case "speed_basic":
                // Увеличение базовой скорости передвижения
                // Эффект будет применяться в миксинах при движении
                break;
            case "carry_capacity_basic":
                // Увеличение вместимости инвентаря
                // Эффект будет применяться в миксинах при работе с инвентарем
                break;
            case "hunger_reduction":
                // Снижение расхода голода
                // Эффект будет применяться в миксинах при движении и действиях
                break;
            case "inventory_slots_basic":
                // Дополнительные слоты инвентаря
                // Эффект будет применяться в миксинах при работе с инвентарем
                break;
            case "magnetic_pockets":
                // Увеличение радиуса подбора предметов
                // Эффект будет применяться в миксинах при движении
                break;
            case "sprint_boost":
                // Увеличение скорости при спринте
                // Эффект будет применяться в миксинах при спринте
                break;
            case "speed_surge":
                // Временное увеличение скорости и восстановление голода
                // Эффект будет применяться в миксинах при активации
                break;
            case "carry_surge":
                // Выброс огненного заряда
                // Эффект будет применяться в миксинах при активации
                break;
            case "inventory_surge":
                // Создание сумки с едой
                // Эффект будет применяться в миксинах при активации
                break;
            case "shulker_carry":
                // Создание карты деревень
                // Эффект будет применяться в миксинах при активации
                break;
                
            // Добавить другие навыки по мере необходимости
        }
    }
    
    /**
     * Активирует указанный активный навык у игрока
     * @param skillId ID навыка
     */
    public void activateSkill(String skillId) {
        Origins.LOGGER.info("Попытка активации навыка: {}", skillId);
        String currentClass = getCurrentClass();
        if (currentClass == null) {
            Origins.LOGGER.info("Не удалось активировать навык: не найден текущий класс");
            return;
        }
        
        SkillTreeHandler.SkillTree skillTree = SkillTreeHandler.getSkillTree(currentClass);
        if (skillTree == null) {
            Origins.LOGGER.info("Не удалось активировать навык: не найдено дерево навыков для класса {}", currentClass);
            return;
        }
        
        SkillTreeHandler.Skill skill = null;
        for (SkillTreeHandler.Skill s : skillTree.getAllSkills()) {
            if (s.getId().equals(skillId) && s.getType() == SkillTreeHandler.SkillType.ACTIVE) {
                skill = s;
                break;
            }
        }
        
        if (skill == null) {
            Origins.LOGGER.info("Не удалось активировать навык: активный навык {} не найден в дереве", skillId);
            // Попробуем найти обработчик по ID навыка
            handleSkillById(skillId);
            return;
        }
        
        int skillLevel = getSkillLevel(skillId);
        if (skillLevel <= 0) {
            Origins.LOGGER.info("Не удалось активировать навык: навык {} не изучен", skillId);
            return;
        }
        
        // Вызов обработчика по ID навыка
        handleSkillById(skillId);
    }
    
    /**
     * Вызывает соответствующий обработчик для навыка по его ID
     * @param skillId ID навыка
     */
    private void handleSkillById(String skillId) {
        if ("bottle_throw".equals(skillId) && player instanceof ServerPlayerEntity serverPlayer) {
            int level = getSkillLevel(skillId);
            if (BrewerSkillHandler.isBrewer(player)) {
                BrewerSkillHandler.handleBottleThrow(serverPlayer, level);
            }
            return;
        }
        
        if ("mad_boost".equals(skillId) && player instanceof ServerPlayerEntity serverPlayer) {
            int level = getSkillLevel(skillId);
            if (WarriorSkillHandler.isWarrior(player)) {
                WarriorSkillHandler.handleMadBoost(serverPlayer, level);
            }
            return;
        }
        
        if ("sprint_boost".equals(skillId) && player instanceof ServerPlayerEntity serverPlayer) {
            int level = getSkillLevel(skillId);
            if (CourierSkillHandler.isCourier(player)) {
                CourierSkillHandler.handleSprintBoost(serverPlayer, level);
            }
            return;
        }
        
        if ("instant_repair".equals(skillId) && player instanceof ServerPlayerEntity serverPlayer) {
            int level = getSkillLevel(skillId);
            if (BlacksmithSkillHandler.isBlacksmith(player)) {
                BlacksmithSkillHandler.handleInstantRepair(serverPlayer, level);
            }
            return;
        }
        
        if ("hot_strike".equals(skillId) && player instanceof ServerPlayerEntity serverPlayer) {
            int level = getSkillLevel(skillId);
            if (BlacksmithSkillHandler.isBlacksmith(player)) {
                BlacksmithSkillHandler.handleHotStrike(serverPlayer, level);
            }
            return;
        }
        
        if ("indestructibility".equals(skillId) && player instanceof ServerPlayerEntity serverPlayer) {
            int level = getSkillLevel(skillId);
            if (WarriorSkillHandler.isWarrior(player)) {
                WarriorSkillHandler.handleIndestructibility(serverPlayer, level);
            }
            return;
        }
        
        if ("dagestan".equals(skillId) && player instanceof ServerPlayerEntity serverPlayer) {
            int level = getSkillLevel(skillId);
            if (WarriorSkillHandler.isWarrior(player)) {
                WarriorSkillHandler.handleDagestanskayaBratva(serverPlayer, level);
            }
            return;
        }
        
        if ("berserker_drink".equals(skillId) && player instanceof ServerPlayerEntity serverPlayer) {
            // Обрабатывается в SkillActivationHandler
            return;
        }
        
        if ("healing_ale".equals(skillId) && player instanceof ServerPlayerEntity serverPlayer) {
            // Обрабатывается в SkillActivationHandler
            return;
        }
        
        if ("party_time".equals(skillId) && player instanceof ServerPlayerEntity serverPlayer) {
            // Обрабатывается в SkillActivationHandler
            return;
        }
        
        if ("banquet".equals(skillId) && player instanceof ServerPlayerEntity serverPlayer) {
            // Обрабатывается в SkillActivationHandler
            return;
        }
        
        // Обработчики для курьерских скиллов
        if ("speed_basic".equals(skillId) && player instanceof ServerPlayerEntity serverPlayer) {
            int level = getSkillLevel(skillId);
            if (CourierSkillHandler.isCourier(player)) {
                CourierSkillHandler.handleSpeedBasic(serverPlayer, level);
            }
            return;
        }
        
        if ("carry_surge".equals(skillId) && player instanceof ServerPlayerEntity serverPlayer) {
            int level = getSkillLevel(skillId);
            if (CourierSkillHandler.isCourier(player)) {
                CourierSkillHandler.handleCarrySurge(serverPlayer, level);
            }
            return;
        }
        
        if ("inventory_surge".equals(skillId) && player instanceof ServerPlayerEntity serverPlayer) {
            int level = getSkillLevel(skillId);
            if (CourierSkillHandler.isCourier(player)) {
                CourierSkillHandler.handleInventorySurge(serverPlayer, level);
            }
            return;
        }
        
        if ("shulker_carry".equals(skillId) && player instanceof ServerPlayerEntity serverPlayer) {
            int level = getSkillLevel(skillId);
            if (CourierSkillHandler.isCourier(player)) {
                CourierSkillHandler.handleShulkerCarry(serverPlayer, level);
            }
            return;
        }
        
        if ("hunger_reduction".equals(skillId) && player instanceof ServerPlayerEntity serverPlayer) {
            int level = getSkillLevel(skillId);
            if (CourierSkillHandler.isCourier(player)) {
                CourierSkillHandler.handleHungerReduction(serverPlayer, level);
            }
            return;
        }
        
        if ("speed_surge".equals(skillId) && player instanceof ServerPlayerEntity serverPlayer) {
            int level = getSkillLevel(skillId);
            if (CourierSkillHandler.isCourier(player)) {
                CourierSkillHandler.handleSpeedSurge(serverPlayer, level);
            }
            return;
        }
        
        if ("magnetic_pockets".equals(skillId) && player instanceof ServerPlayerEntity serverPlayer) {
            int level = getSkillLevel(skillId);
            if (CourierSkillHandler.isCourier(player)) {
                CourierSkillHandler.handleMagneticPockets(serverPlayer, level);
            }
            return;
        }
        
        if ("inventory_slots_basic".equals(skillId) && player instanceof ServerPlayerEntity serverPlayer) {
            int level = getSkillLevel(skillId);
            if (CourierSkillHandler.isCourier(player)) {
                CourierSkillHandler.handleExtraSlots(serverPlayer, level);
            }
            return;
        }
        
        if ("carry_capacity_basic".equals(skillId) && player instanceof ServerPlayerEntity serverPlayer) {
            int level = getSkillLevel(skillId);
            if (CourierSkillHandler.isCourier(player)) {
                CourierSkillHandler.handleTrap(serverPlayer, level);
            }
            return;
        }
        
        // Добавить обработчики для других активных навыков по мере их реализации
        Origins.LOGGER.warn("Обработчик для активного навыка {} не найден", skillId);
    }

    @Override
    public void serverTick() {
        tickCounter++;
        
        // Восстановление энергии
        if (energyRegenDelay > 0) {
            energyRegenDelay--;
        } else if (currentEnergy < maxEnergy) {
            // Восстанавливаем энергию каждый тик (можно настроить частоту)
            if (tickCounter % 20 == 0) { // Каждую секунду
                restoreEnergy(energyRegenRate);
            }
        }
        
        // Обработка отложенной установки ловушки
        if (hasDelayedTrap()) {
            if (updateDelayedTrap()) {
                // Время установить ловушку
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    setDelayedTrapAtPosition(serverPlayer);
                }
                clearDelayedTrap();
            }
        }
        
        // Применяем пассивные эффекты каждые 20 тиков (1 секунда)
        if (tickCounter % 20 == 0) {
            applyPassiveSkillEffects();
        }
    }
    
    /**
     * Устанавливает ловушку в позиции, запомненной ранее
     */
    private void setDelayedTrapAtPosition(ServerPlayerEntity player) {
        if (delayedTrapPosition != null) {
            // Проверяем, можно ли все еще установить ловушку в этой позиции
            net.minecraft.block.BlockState blockState = player.getWorld().getBlockState(delayedTrapPosition);
            if (blockState.isAir() || blockState.isReplaceable() || 
                blockState.isOf(net.minecraft.block.Blocks.GRASS) || 
                blockState.isOf(net.minecraft.block.Blocks.TALL_GRASS) ||
                blockState.isOf(net.minecraft.block.Blocks.SNOW) ||
                blockState.isOf(net.minecraft.block.Blocks.DIRT) ||
                blockState.isOf(net.minecraft.block.Blocks.COARSE_DIRT) ||
                blockState.isOf(net.minecraft.block.Blocks.PODZOL) ||
                blockState.isOf(net.minecraft.block.Blocks.ROOTED_DIRT) ||
                blockState.isOf(net.minecraft.block.Blocks.SAND) ||
                blockState.isOf(net.minecraft.block.Blocks.RED_SAND) ||
                blockState.isOf(net.minecraft.block.Blocks.GRAVEL)) {
                
                try {
                    // Попытка установки кастомной ловушки
                    player.getWorld().setBlockState(delayedTrapPosition, io.github.apace100.origins.block.ModBlocks.TRAP_BLOCK.getDefaultState());
                    // Звук установки
                    player.getWorld().playSound(null, delayedTrapPosition, 
                        net.minecraft.sound.SoundEvents.BLOCK_STONE_PLACE,
                        net.minecraft.sound.SoundCategory.BLOCKS, 1.0f, 1.0f);
                } catch (Throwable t) { // Используем Throwable для перехвата ExceptionInInitializerError
                    // Если установка кастомной ловушки не удалась, используем стандартный подход
                    player.getWorld().setBlockState(delayedTrapPosition, net.minecraft.block.Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE.getDefaultState());
                    // Звук установки
                    player.getWorld().playSound(null, delayedTrapPosition, 
                        net.minecraft.sound.SoundEvents.BLOCK_STONE_PLACE,
                        net.minecraft.sound.SoundCategory.BLOCKS, 1.0f, 1.0f);
                }
                
                player.sendMessage(
                    Text.literal("Ловушка установлена под вами!")
                        .formatted(Formatting.RED), 
                    true // action bar
                );
            } else {
                player.sendMessage(
                    Text.literal("Нельзя установить ловушку там, где вы стояли!")
                        .formatted(Formatting.RED), 
                    true // action bar
                );
            }
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
        
        // Загружаем систему энергии
        currentEnergy = tag.getInt("currentEnergy");
        maxEnergy = tag.getInt("maxEnergy");
        energyRegenRate = tag.getInt("energyRegenRate");
        energyRegenDelay = tag.getInt("energyRegenDelay");
        
        // Устанавливаем значения по умолчанию, если они не были сохранены
        if (maxEnergy <= 0) maxEnergy = 20;
        if (currentEnergy < 0) currentEnergy = maxEnergy;
        if (energyRegenRate <= 0) energyRegenRate = 1;
        
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
        
        // Сохраняем систему энергии
        tag.putInt("currentEnergy", currentEnergy);
        tag.putInt("maxEnergy", maxEnergy);
        tag.putInt("energyRegenRate", energyRegenRate);
        tag.putInt("energyRegenDelay", energyRegenDelay);

    }
    
    // ==================== МЕТОДЫ ДЛЯ ПОЛУЧЕНИЯ УРОВНЕЙ СКИЛЛОВ КУРЬЕРА ====================
    
    /**
     * Получает уровень скилла "Базовая скорость" (speed_basic)
     * @return уровень скилла или 0, если скилл не изучен
     */
    public int getSpeedBasicLevel() {
        return getSkillLevel("speed_basic");
    }
    
    /**
     * Получает уровень скилла "Снижение голода" (hunger_reduction)
     * @return уровень скилла или 0, если скилл не изучен
     */
    public int getHungerReductionLevel() {
        return getSkillLevel("hunger_reduction");
    }
    
    /**
     * Получает уровень скилла "Базовые слоты" (inventory_slots_basic)
     * @return уровень скилла или 0, если скилл не изучен
     */
    public int getInventorySlotsBasicLevel() {
        return getSkillLevel("inventory_slots_basic");
    }
    
    /**
     * Получает уровень скилла "Рывок" (sprint_boost)
     * @return уровень скилла или 0, если скилл не изучен
     */
    public int getSprintBoostLevel() {
        return getSkillLevel("sprint_boost");
    }
    
    /**
     * Получает уровень скилла "Всплеск скорости" (speed_surge)
     * @return уровень скилла или 0, если скилл не изучен
     */
    public int getSpeedSurgeLevel() {
        return getSkillLevel("speed_surge");
    }
    
    /**
     * Получает уровень скилла "Магнитные карманы" (magnetic_pockets)
     * @return уровень скилла или 0, если скилл не изучен
     */
    public int getMagneticPocketsLevel() {
        return getSkillLevel("magnetic_pockets");
    }
    
    /**
     * Получает уровень скилла "Граната с перцем" (carry_surge)
     * @return уровень скилла или 0, если скилл не изучен
     */
    public int getCarrySurgeLevel() {
        return getSkillLevel("carry_surge");
    }
    
    /**
     * Получает уровень скилла "Сумка для еды" (inventory_surge) - переименовано с "Улыбка Курьера"
     * @return уровень скилла или 0, если скилл не изучен
     */
    public int getInventorySurgeLevel() {
        return getSkillLevel("inventory_surge");
    }
    
    /**
     * Получает уровень скилла "Карта в голове" (shulker_carry)
     * @return уровень скилла или 0, если скилл не изучен
     */
    public int getShulkerCarryLevel() {
        return getSkillLevel("shulker_carry");
    }
    
    /**
     * Получает уровень скилла "Ловушка" (carry_capacity_basic)
     * @return уровень скилла или 0, если скилл не изучен
     */
    public int getCarryCapacityBasicLevel() {
        return getSkillLevel("carry_capacity_basic");
    }
    
    // ==================== МЕТОДЫ ДЛЯ ПОЛУЧЕНИЯ УРОВНЕЙ СКИЛЛОВ ПОВАРА ====================
    
    /**
     * Получает уровень скилла "Свежий продукт" (fresh_product)
     * @return уровень скилла или 0, если скилл не изучен
     */
    public int getFreshProductLevel() {
        return getSkillLevel("fresh_product");
    }
    
    /**
     * Получает уровень скилла "Сытный обед" (hearty_meal)
     * @return уровень скилла или 0, если скилл не изучен
     */
    public int getHeartyMealLevel() {
        return getSkillLevel("hearty_meal");
    }
    
    /**
     * Получает уровень скилла "Шеф-повар" (chef_master)
     * @return уровень скилла или 0, если скилл не изучен
     */
    public int getChefMasterLevel() {
        return getSkillLevel("chef_master");
    }
    
    /**
     * Получает уровень скилла "Фламбе" (flambe)
     * @return уровень скилла или 0, если скилл не изучен
     */
    public int getFlambeLevel() {
        return getSkillLevel("flambe");
    }
    
    /**
     * Получает уровень скилла "Огнестойкость" (fire_immunity)
     * @return уровень скилла или 0, если скилл не изучен
     */
    public int getFireImmunityLevel() {
        return getSkillLevel("fire_immunity");
    }
    
    /**
     * Получает уровень скилла "Готово!" (ready)
     * @return уровень скилла или 0, если скилл не изучен
     */
    public int getReadyLevel() {
        return getSkillLevel("ready");
    }
    
    /**
     * Получает уровень скилла "Быстрый перекус" (quick_snack)
     * @return уровень скилла или 0, если скилл не изучен
     */
    public int getQuickSnackLevel() {
        return getSkillLevel("quick_snack");
    }
    
    /**
     * Применяет бонус урона от скилла "Фламбе" к горящей цели
     * @param target цель атаки
     * @return множитель урона (1.0 = без изменений)
     */
    public float applyFlambeBoost(net.minecraft.entity.Entity target) {
        // Проверяем, что цель - живое существо и горит
        if (target instanceof net.minecraft.entity.LivingEntity livingTarget && livingTarget.isOnFire()) {
            int flambeLevel = getFlambeLevel();
            if (flambeLevel > 0) {
                // Увеличиваем урон на 10% за уровень
                float bonus = 1.0f + (flambeLevel * 0.1f);
                Origins.LOGGER.debug("Повар {} применил бонус 'Фламбе' к горящей цели {}: множитель {}", 
                    player.getName().getString(), target.getName().getString(), bonus);
                return bonus;
            }
        }
        return 1.0f;
    }
}