package io.github.apace100.origins.quest;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Класс для отслеживания прогресса квеста, аналогичный Progress из Bountiful
 */
public class QuestProgress {
    private final double current;
    private final double goal;
    private final int precision;
    
    public QuestProgress(double current, double goal) {
        this(current, goal, 0);
    }
    
    public QuestProgress(int current, int goal) {
        this(current, goal, 0);
    }
    
    public QuestProgress(double current, double goal, int precision) {
        this.current = current;
        this.goal = goal;
        this.precision = precision;
    }
    
    public QuestProgress(int current, int goal, int precision) {
        this.current = current;
        this.goal = goal;
        this.precision = precision;
    }
    
    /**
     * Проверяет, завершена ли цель
     */
    public boolean isComplete() {
        return current >= goal;
    }
    
    /**
     * Получает цвет для отображения прогресса
     */
    public Formatting getColor() {
        return isComplete() ? Formatting.GREEN : Formatting.RED;
    }
    
    /**
     * Получает текст с прогрессом для отображения
     */
    public MutableText getProgressText() {
        if (precision == 0) {
            return Text.literal(" (" + (int)current + "/" + (int)goal + ")");
        } else {
            String format = " (%." + precision + "f/%." + precision + "f)";
            return Text.literal(String.format(format, current, goal));
        }
    }
    
    /**
     * Получает текст с количеством для награды
     */
    public MutableText getAmountText() {
        if (precision == 0) {
            return Text.literal((int)goal + "x ");
        } else {
            String format = "%." + precision + "fx ";
            return Text.literal(String.format(format, goal));
        }
    }
    
    public double getCurrent() {
        return current;
    }
    
    public double getGoal() {
        return goal;
    }
    
    public int getPrecision() {
        return precision;
    }
}