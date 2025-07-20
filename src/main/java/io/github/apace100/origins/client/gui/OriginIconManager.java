package io.github.apace100.origins.client.gui;

import io.github.apace100.origins.Origins;
import net.minecraft.util.Identifier;
import java.util.HashMap;
import java.util.Map;

/**
 * Менеджер иконок происхождений
 */
public class OriginIconManager {
    
    private static final Map<String, Identifier> ICON_CACHE = new HashMap<>();
    private static final Identifier FALLBACK_TEXTURE = new Identifier(Origins.MODID, "textures/gui/inventory/missing.png");
    
    /**
     * Получает текстуру иконки для указанного происхождения
     */
    public static Identifier getIconTexture(String originId) {
        // Если иконка уже в кэше, возвращаем её
        if (ICON_CACHE.containsKey(originId)) {
            return ICON_CACHE.get(originId);
        }
        
        // Преобразуем ID происхождения в имя файла текстуры
        String textureFileName = originId;
        
        // Для некоторых происхождений используем специальные имена файлов
        if (originId.equals("blacksmith")) textureFileName = "customhp";
        else if (originId.equals("cook")) textureFileName = "chef";
        else if (originId.equals("warrior")) textureFileName = "war";
        else if (originId.equals("courier")) textureFileName = "yandex";
        
        // Создаем идентификатор текстуры
        Identifier iconTexture = new Identifier(Origins.MODID, "textures/gui/inventory/" + textureFileName + ".png");
        
        // Сохраняем в кэш и возвращаем
        ICON_CACHE.put(originId, iconTexture);
        return iconTexture;
    }
    
    /**
     * Получает запасную текстуру
     */
    public static Identifier getFallbackTexture() {
        return FALLBACK_TEXTURE;
    }
    
    /**
     * Очищает кэш иконок
     */
    public static void clearCache() {
        ICON_CACHE.clear();
    }
}