package io.github.apace100.origins.skill;

/**
 * Класс, представляющий отдельный навык в дереве навыков
 */
public class Skill {
    private final String id;
    private final String name;
    private final String description;
    private final SkillType type;
    private final int requiredLevel;
    private final int maxLevel;
    private final String parentId;
    private final String originType;

    public Skill(String id, String name, String description, SkillType type, 
                 int requiredLevel, int maxLevel, String parentId, String originType) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.requiredLevel = requiredLevel;
        this.maxLevel = maxLevel;
        this.parentId = parentId;
        this.originType = originType;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public SkillType getType() {
        return type;
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public String getParentId() {
        return parentId;
    }

    public String getOriginType() {
        return originType;
    }

    public boolean hasParent() {
        return parentId != null && !parentId.isEmpty();
    }

    /**
     * Типы навыков
     */
    public enum SkillType {
        PASSIVE,  // Пассивный навык (постоянный эффект)
        ACTIVE,   // Активный навык (требует активации)
        GLOBAL    // Глобальный навык (влияет на все аспекты игры)
    }

    @Override
    public String toString() {
        return "Skill{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", requiredLevel=" + requiredLevel +
                ", maxLevel=" + maxLevel +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Skill skill = (Skill) obj;
        return id.equals(skill.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}