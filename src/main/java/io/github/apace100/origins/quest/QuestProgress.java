package io.github.apace100.origins.quest;

/**
 * Представляет прогресс выполнения квеста
 */
public class QuestProgress {
    private final int current;
    private final int required;
    private final boolean completed;
    
    public QuestProgress(int current, int required) {
        this.current = current;
        this.required = required;
        this.completed = current >= required;
    }
    
    public int getCurrent() {
        return current;
    }
    
    public int getRequired() {
        return required;
    }
    
    public boolean isCompleted() {
        return completed;
    }
    
    public boolean isComplete() {
        return completed;
    }
    
    public double getPercentage() {
        if (required <= 0) return 1.0;
        return Math.min(1.0, (double) current / required);
    }
    
    public net.minecraft.util.Formatting getColor() {
        if (completed) {
            return net.minecraft.util.Formatting.GREEN;
        } else if (current > 0) {
            return net.minecraft.util.Formatting.YELLOW;
        } else {
            return net.minecraft.util.Formatting.RED;
        }
    }
    
    public String getProgressText() {
        return " (" + current + "/" + required + ")";
    }
    
    public net.minecraft.text.MutableText getAmountText() {
        return net.minecraft.text.Text.literal(String.valueOf(current));
    }
}