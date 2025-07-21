package net.xach.testmod.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.monster.Pillager;

public class DagestanskiRogue extends Pillager {
    private int lifeTime = 0;
    private static final int MAX_LIFE_TIME = 900; // 45 секунд (20 тиков/сек * 45)
    private Player owner;

    public DagestanskiRogue(EntityType<? extends Pillager> entityType, Level level) {
        super(entityType, level);
        this.setHealth(this.getMaxHealth());
    }

    public void setOwner(Player owner) {
        this.owner = owner;
    }

    public Player getOwner() {
        return this.owner;
    }

    @Override
    protected void registerGoals() {
        // Очищаем все цели
        this.goalSelector.removeAllGoals(goal -> true);
        this.targetSelector.removeAllGoals(goal -> true);

        // Базовые цели поведения
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new RangedCrossbowAttackGoal<>(this, 1.0D, 8.0F));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.6D));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 15.0F, 1.0F));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, LivingEntity.class, 15.0F));

        // Цели для атаки - ТОЛЬКО враждебные мобы
        this.targetSelector.addGoal(1, new CustomOwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new CustomOwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this, Player.class).setAlertOthers()); // Не атакуем игроков в ответ
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Monster.class, true,
                (target) -> !(target instanceof AbstractIllager) && !(target instanceof DagestanskiBrother) && !(target instanceof DagestanskiRogue)));
    }

    @Override
    public void tick() {
        super.tick();

        // Увеличиваем время жизни
        lifeTime++;

        // Удаляем через 45 секунд
        if (lifeTime >= MAX_LIFE_TIME) {
            // Эффект исчезновения
            if (this.level().isClientSide()) {
                for (int i = 0; i < 20; i++) {
                    this.level().addParticle(
                            ParticleTypes.POOF,
                            this.getX() + (this.random.nextDouble() - 0.5) * this.getBbWidth(),
                            this.getY() + this.random.nextDouble() * this.getBbHeight(),
                            this.getZ() + (this.random.nextDouble() - 0.5) * this.getBbWidth(),
                            0, 0.1, 0
                    );
                }
            }
            this.discard();
            return;
        }

        // Визуальный эффект исчезновения за 5 секунд до удаления
        if (lifeTime >= MAX_LIFE_TIME - 100) {
            if (this.level().isClientSide() && this.random.nextInt(10) == 0) {
                this.level().addParticle(
                        ParticleTypes.SMOKE,
                        this.getX() + (this.random.nextDouble() - 0.5) * this.getBbWidth(),
                        this.getY() + this.random.nextDouble() * this.getBbHeight(),
                        this.getZ() + (this.random.nextDouble() - 0.5) * this.getBbWidth(),
                        0, 0.1, 0
                );
            }
        }
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        // Не атакуем игроков
        if (target instanceof Player) {
            return false;
        }

        // Не атакуем других братьев и разбойников
        if (target instanceof DagestanskiBrother || target instanceof DagestanskiRogue) {
            return false;
        }

        // Не атакуем других иллагеров
        if (target instanceof AbstractIllager) {
            return false;
        }

        // Атакуем только враждебных мобов
        return target instanceof Monster && super.canAttack(target);
    }

    @Override
    protected void dropAllDeathLoot(net.minecraft.world.damagesource.DamageSource damageSource) {
        // Не дропаем лут при смерти
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false; // Не исчезаем при удалении игрока
    }

    // Кастомные цели для атаки владельца
    private static class CustomOwnerHurtByTargetGoal extends HurtByTargetGoal {
        private final DagestanskiRogue rogue;

        public CustomOwnerHurtByTargetGoal(DagestanskiRogue rogue) {
            super(rogue);
            this.rogue = rogue;
        }

        @Override
        public boolean canUse() {
            if (rogue.getOwner() == null) return false;
            LivingEntity lastHurtByMob = rogue.getOwner().getLastHurtByMob();
            return lastHurtByMob != null && !(lastHurtByMob instanceof Player) &&
                    !(lastHurtByMob instanceof DagestanskiBrother) && !(lastHurtByMob instanceof DagestanskiRogue);
        }

        @Override
        public void start() {
            if (rogue.getOwner() != null) {
                LivingEntity target = rogue.getOwner().getLastHurtByMob();
                if (target != null && rogue.canAttack(target)) {
                    rogue.setTarget(target);
                }
            }
        }
    }

    private static class CustomOwnerHurtTargetGoal extends HurtByTargetGoal {
        private final DagestanskiRogue rogue;

        public CustomOwnerHurtTargetGoal(DagestanskiRogue rogue) {
            super(rogue);
            this.rogue = rogue;
        }

        @Override
        public boolean canUse() {
            if (rogue.getOwner() == null) return false;
            LivingEntity lastHurtMob = rogue.getOwner().getLastHurtMob();
            return lastHurtMob != null && !(lastHurtMob instanceof Player) &&
                    !(lastHurtMob instanceof DagestanskiBrother) && !(lastHurtMob instanceof DagestanskiRogue);
        }

        @Override
        public void start() {
            if (rogue.getOwner() != null) {
                LivingEntity target = rogue.getOwner().getLastHurtMob();
                if (target != null && rogue.canAttack(target)) {
                    rogue.setTarget(target);
                }
            }
        }
    }
}
