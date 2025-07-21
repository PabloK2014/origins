package io.github.apace100.origins.profession;

import io.github.apace100.origins.Origins;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Реестр доступных профессий
 */
public class ProfessionRegistry {
    private static final Map<Identifier, Profession> PROFESSIONS = new HashMap<>();
    
    // Предопределенные профессии
    public static final Profession MINER = register(
        new Profession(
            "origins:miner",
            "Шахтер",
            "Мастер подземных работ",
            "textures/gui/icons/miner.png",
            java.util.Arrays.asList("mining", "ore_found"),
            50
        )
    );
    
    public static final Profession COOK = register(
        new Profession(
            "origins:cook",
            "Повар",
            "Мастер кулинарного искусства",
            "textures/gui/icons/cook.png",
            java.util.Arrays.asList("cooking", "food_eaten"),
            50
        )
    );
    
    public static final Profession BLACKSMITH = register(
        new Profession(
            "origins:blacksmith",
            "Кузнец",
            "Мастер обработки металла",
            "textures/gui/icons/blacksmith.png",
            java.util.Arrays.asList("crafting", "smelting"),
            50
        )
    );
    
    public static final Profession BREWER = register(
        new Profession(
            "origins:brewer",
            "Пивовар",
            "Мастер зельеварения",
            "textures/gui/icons/brewer.png",
            java.util.Arrays.asList("brewing", "potion_used"),
            50
        )
    );
    
    public static final Profession WARRIOR = register(
        new Profession(
            "origins:warrior",
            "Воин",
            "Мастер боевых искусств",
            "textures/gui/icons/warrior.png",
            java.util.Arrays.asList("combat", "enemy_killed"),
            50
        )
    );
    
    /**
     * Регистрирует новую профессию
     */
    public static Profession register(Profession profession) {
        PROFESSIONS.put(new Identifier(profession.getId()), profession);
        return profession;
    }
    
    /**
     * Получает профессию по идентификатору
     */
    public static Profession get(Identifier id) {
        return PROFESSIONS.get(id);
    }
    
    /**
     * Получает профессию по строковому идентификатору
     */
    public static Profession get(String id) {
        return get(new Identifier(id));
    }
    
    /**
     * Получает все зарегистрированные профессии
     */
    public static Map<Identifier, Profession> getAll() {
        return PROFESSIONS;
    }
    
    /**
     * Инициализирует реестр профессий
     */
    public static void init() {
        // Профессии уже зарегистрированы через статические поля
        Origins.LOGGER.info("Зарегистрировано {} профессий", PROFESSIONS.size());
    }
}