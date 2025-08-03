package io.github.apace100.origins.quest;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.util.List;
import java.util.ArrayList;

/**
 * Содержимое тултипа для квестов
 */
public class QuestTooltipContent {
    private final List<Text> lines;
    
    public QuestTooltipContent() {
        this.lines = new ArrayList<>();
    }
    
    /**
     * Добавляет строку в тултип
     */
    public void addLine(Text text) {
        lines.add(text);
    }
    
    /**
     * Добавляет строку с форматированием
     */
    public void addLine(String text, Formatting formatting) {
        lines.add(Text.literal(text).formatted(formatting));
    }
    
    /**
     * Получает все строки тултипа
     */
    public List<Text> getLines() {
        return new ArrayList<>(lines);
    }
    
    /**
     * Очищает содержимое тултипа
     */
    public void clear() {
        lines.clear();
    }
    
    /**
     * Проверяет, пуст ли тултип
     */
    public boolean isEmpty() {
        return lines.isEmpty();
    }
    
    /**
     * Создает тултип для квеста
     */
    public static QuestTooltipContent forQuest(Quest quest) {
        QuestTooltipContent content = new QuestTooltipContent();
        
        if (quest == null) {
            content.addLine("Неизвестный квест", Formatting.RED);
            return content;
        }
        
        // Добавляем название квеста
        content.addLine(quest.getTitle(), Formatting.GOLD);
        
        // Добавляем описание
        if (quest.getDescription() != null && !quest.getDescription().isEmpty()) {
            content.addLine(quest.getDescription(), Formatting.GRAY);
        }
        
        // Добавляем требования к классу
        String profession = quest.getPlayerClass();
        if (profession != null && !profession.isEmpty() && !profession.equals("any")) {
            content.addLine("Класс: " + QuestItem.getClassDisplayName(profession), Formatting.AQUA);
        }
        
        return content;
    }
}