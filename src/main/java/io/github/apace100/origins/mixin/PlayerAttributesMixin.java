package io.github.apace100.origins.mixin;

import io.github.apace100.origins.profession.ProfessionComponent;
import io.github.apace100.origins.profession.ProfessionProgress;
import io.github.apace100.origins.profession.ProfessionSkills;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(PlayerEntity.class)
public class PlayerAttributesMixin {
    private static final UUID HEALTH_MODIFIER_ID = UUID.fromString("d5d0d878-b3c2-4e90-8b4a-a2e3c4f6a56a");
    private static final UUID STRENGTH_MODIFIER_ID = UUID.fromString("d5d0d878-b3c2-4e90-8b4a-a2e3c4f6a56b");
    private static final UUID AGILITY_MODIFIER_ID = UUID.fromString("d5d0d878-b3c2-4e90-8b4a-a2e3c4f6a56c");
    private static final UUID DEFENSE_MODIFIER_ID = UUID.fromString("d5d0d878-b3c2-4e90-8b4a-a2e3c4f6a56d");

    @Inject(method = "tick", at = @At("HEAD"))
    private void updateAttributes(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.getWorld().isClient) return;

        ProfessionComponent component = ProfessionComponent.KEY.get(player);
        ProfessionProgress progress = component.getCurrentProgress();
        if (progress == null) return;

        ProfessionSkills skills = progress.getSkills();

        // Здоровье
        updateAttribute(player, (EntityAttribute)EntityAttributes.GENERIC_MAX_HEALTH,
            HEALTH_MODIFIER_ID, "Health Bonus", skills.getHealthBonus(),
            EntityAttributeModifier.Operation.ADDITION);

        // Сила
        updateAttribute(player, (EntityAttribute)EntityAttributes.GENERIC_ATTACK_DAMAGE,
            STRENGTH_MODIFIER_ID, "Strength Bonus", skills.getStrengthBonus(),
            EntityAttributeModifier.Operation.ADDITION);

        // Ловкость
        updateAttribute(player, (EntityAttribute)EntityAttributes.GENERIC_MOVEMENT_SPEED,
            AGILITY_MODIFIER_ID, "Agility Bonus", skills.getAgilityBonus(),
            EntityAttributeModifier.Operation.MULTIPLY_TOTAL);

        // Защита
        updateAttribute(player, (EntityAttribute)EntityAttributes.GENERIC_ARMOR,
            DEFENSE_MODIFIER_ID, "Defense Bonus", skills.getDefenseBonus(),
            EntityAttributeModifier.Operation.ADDITION);
    }

    private void updateAttribute(PlayerEntity player, EntityAttribute attribute, UUID modifierId,
                               String name, double value, EntityAttributeModifier.Operation operation) {
        EntityAttributeInstance instance = player.getAttributeInstance(attribute);
        if (instance == null) return;

        // Удаляем старый модификатор
        instance.removeModifier(modifierId);

        // Добавляем новый модификатор, если значение больше 0
        if (value > 0) {
            EntityAttributeModifier modifier = new EntityAttributeModifier(
                modifierId, name, value, operation
            );
            instance.addPersistentModifier(modifier);
        }
    }
} 