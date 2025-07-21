package io.github.apace100.origins.mixin;

import io.github.apace100.origins.profession.ProfessionComponent;
import io.github.apace100.origins.profession.ProfessionProgress;
import io.github.apace100.origins.profession.ProfessionSkills;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerSkillsMixin extends LivingEntity {

    protected PlayerSkillsMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void readCustomDataFromNbt(NbtCompound tag, CallbackInfo info) {
        updateAttributes();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo info) {
        if (!this.getWorld().isClient) {
            updateAttributes();
        }
    }

    private void updateAttributes() {
        PlayerEntity player = (PlayerEntity)(Object)this;
        ProfessionComponent component = ProfessionComponent.KEY.get(player);
        ProfessionProgress progress = component.getCurrentProgress();
        if (progress == null) return;

        ProfessionSkills skills = progress.getSkills();

        // Здоровье
        EntityAttributeInstance healthAttribute = this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (healthAttribute != null) {
            healthAttribute.setBaseValue(20.0 + skills.getHealthBonus());
        }

        // Сила
        EntityAttributeInstance strengthAttribute = this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (strengthAttribute != null) {
            strengthAttribute.setBaseValue(1.0 + skills.getStrengthBonus());
        }

        // Ловкость
        EntityAttributeInstance agilityAttribute = this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (agilityAttribute != null) {
            agilityAttribute.setBaseValue(0.1 * (1.0 + skills.getAgilityBonus()));
        }

        // Защита
        EntityAttributeInstance armorAttribute = this.getAttributeInstance(EntityAttributes.GENERIC_ARMOR);
        if (armorAttribute != null) {
            armorAttribute.setBaseValue(0.0 + skills.getDefenseBonus());
        }
    }
} 