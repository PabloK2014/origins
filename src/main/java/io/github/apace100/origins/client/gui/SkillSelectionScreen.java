package io.github.apace100.origins.client.gui;

import io.github.apace100.origins.skill.PlayerSkillComponent;
import io.github.apace100.origins.skill.SkillTreeHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI экран для выбора активного навыка
 */
public class SkillSelectionScreen extends Screen {
    
    private static final Identifier SET_ACTIVE_SKILL_PACKET = new Identifier("origins", "set_active_skill");
    
    private List<SkillInfo> availableSkills = new ArrayList<>();
    private String currentActiveSkill = "";
    
    public SkillSelectionScreen() {
        super(Text.literal("Выбор активного навыка"));
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Получаем доступные навыки
        loadAvailableSkills();
        
        // Создаем кнопки для каждого доступного навыка
        int buttonWidth = 200;
        int buttonHeight = 20;
        int centerX = this.width / 2;
        int startY = this.height / 2 - (availableSkills.size() * buttonHeight) / 2;
        
        if (availableSkills.isEmpty()) {
            // Если нет доступных навыков, показываем сообщение
            addDrawableChild(ButtonWidget.builder(
                Text.literal("У вас нет доступных активных навыков").formatted(Formatting.GRAY),
                button -> this.close()
            ).dimensions(centerX - buttonWidth / 2, startY, buttonWidth, buttonHeight).build());
        } else {
            for (int i = 0; i < availableSkills.size(); i++) {
                SkillInfo skill = availableSkills.get(i);
                int buttonY = startY + i * (buttonHeight + 5);
                
                // Определяем цвет кнопки в зависимости от того, выбран ли навык
                Formatting color = skill.id.equals(currentActiveSkill) ? Formatting.GREEN : Formatting.WHITE;
                String prefix = skill.id.equals(currentActiveSkill) ? "► " : "";
                
                Text buttonText = Text.literal(prefix + skill.name + " (Уровень " + skill.level + ")").formatted(color);
                
                final int skillIndex = i;
                addDrawableChild(ButtonWidget.builder(
                    buttonText,
                    button -> selectSkill(skillIndex)
                ).dimensions(centerX - buttonWidth / 2, buttonY, buttonWidth, buttonHeight).build());
            }
        }
        
        // Кнопка закрытия
        addDrawableChild(ButtonWidget.builder(
            Text.literal("Закрыть"),
            button -> this.close()
        ).dimensions(centerX - 50, this.height - 30, 100, 20).build());
    }
    
    /**
     * Загружает доступные навыки игрока
     */
    private void loadAvailableSkills() {
        availableSkills.clear();
        
        if (client == null || client.player == null) return;
        
        // Получаем компонент навыков игрока
        PlayerSkillComponent skillComponent = PlayerSkillComponent.KEY.get(client.player);
        if (skillComponent == null) return;
        
        // Получаем текущий активный навык
        currentActiveSkill = skillComponent.getActiveSkill();
        if (currentActiveSkill == null) currentActiveSkill = "";
        
        // Определяем класс игрока через компонент Origins
        String playerClass = getPlayerClass();
        
        // Получаем дерево навыков для класса игрока
        SkillTreeHandler.SkillTree skillTree = SkillTreeHandler.getSkillTree(playerClass);
        if (skillTree == null) return;
        
        // Проходим по всем навыкам в дереве
        for (SkillTreeHandler.Skill skill : skillTree.getAllSkills()) {
            // Проверяем, что навык активный или глобальный
            if (skill.getType() == SkillTreeHandler.SkillType.ACTIVE || 
                skill.getType() == SkillTreeHandler.SkillType.GLOBAL) {
                // Проверяем, изучен ли навык
                int skillLevel = skillComponent.getSkillLevel(skill.getId());
                if (skillLevel > 0) {
                    availableSkills.add(new SkillInfo(skill.getId(), skill.getName(), skillLevel));
                }
            }
        }
    }
    
    /**
     * Выбирает навык по индексу
     */
    private void selectSkill(int skillIndex) {
        if (skillIndex >= 0 && skillIndex < availableSkills.size()) {
            SkillInfo selectedSkill = availableSkills.get(skillIndex);
            
            // Отправляем пакет на сервер для установки активного навыка
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeString(selectedSkill.id);
            ClientPlayNetworking.send(SET_ACTIVE_SKILL_PACKET, buf);
            
            // Закрываем экран
            this.close();
            
            // Показываем сообщение игроку
            if (client != null && client.player != null) {
                client.player.sendMessage(
                    Text.literal("Активный навык установлен: " + selectedSkill.name)
                        .formatted(Formatting.GREEN), 
                    true
                );
            }
        }
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Рендерим затемненный фон
        this.renderBackground(context);
        
        // Рендерим виджеты
        super.render(context, mouseX, mouseY, delta);
        
        // Рендерим заголовок
        context.drawCenteredTextWithShadow(
            this.textRenderer, 
            this.title, 
            this.width / 2, 
            20, 
            0xFFFFFF
        );
        
        // Рендерим инструкцию
        if (!availableSkills.isEmpty()) {
            context.drawCenteredTextWithShadow(
                this.textRenderer, 
                Text.literal("Выберите навык для активации клавишей K").formatted(Formatting.GRAY), 
                this.width / 2, 
                40, 
                0xAAAAAA
            );
        }
    }
    
    @Override
    public boolean shouldPause() {
        return false; // Не ставим игру на паузу
    }
    
    /**
     * Получает класс игрока через компонент Origins
     */
    private String getPlayerClass() {
        if (client == null || client.player == null) return null;
        
        try {
            io.github.apace100.origins.component.OriginComponent originComponent = 
                io.github.apace100.origins.registry.ModComponents.ORIGIN.get(client.player);
            
            if (originComponent != null) {
                var origin = originComponent.getOrigin(
                    io.github.apace100.origins.origin.OriginLayers.getLayer(
                        io.github.apace100.origins.Origins.identifier("origin")
                    )
                );
                
                if (origin != null) {
                    return origin.getIdentifier().toString();
                }
            }
        } catch (Exception e) {
            // Логируем ошибку, но не крашим игру
            System.err.println("Ошибка при получении класса игрока: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Класс для хранения информации о навыке
     */
    private static class SkillInfo {
        public final String id;
        public final String name;
        public final int level;
        
        public SkillInfo(String id, String name, int level) {
            this.id = id;
            this.name = name;
            this.level = level;
        }
    }
}