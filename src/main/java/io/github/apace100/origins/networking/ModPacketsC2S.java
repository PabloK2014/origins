package io.github.apace100.origins.networking;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.skill.PlayerSkillComponent;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;

/**
 * Регистрация обработчиков сетевых пакетов от клиента к серверу
 */
public class ModPacketsC2S {
    
    public static final Identifier LEARN_SKILL = new Identifier(Origins.MODID, "learn_skill");
    
    public static void register() {
        // Регистрируем обработчик пакета для изучения навыка
        ServerPlayNetworking.registerGlobalReceiver(LEARN_SKILL, (server, player, handler, buf, responseSender) -> {
            // Получаем идентификатор навыка
            Identifier skillId = buf.readIdentifier();
            
            // Выполняем действие на сервере
            server.execute(() -> {
                // Получаем компонент навыков игрока
                PlayerSkillComponent playerSkills = PlayerSkillComponent.KEY.get(player);
                
                // Пытаемся изучить навык
                playerSkills.learnSkill(skillId.toString());
            });
        });
    }
}