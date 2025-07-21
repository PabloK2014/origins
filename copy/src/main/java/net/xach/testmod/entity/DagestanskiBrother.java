package net.xach.testmod.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.particles.ParticleTypes;

public class DagestanskiBrother extends Wolf {
    private int lifeTime = 0;
    private static final int MAX_LIFE_TIME = 900; // 45 секунд (20 тиков/сек * 45)

    public DagestanskiBrother(EntityType<? extends Wolf> entityType, Level level) {
        super(entityType, level);
        this.setTame(true);
        this.setHealth(this.getMaxHealth());
    }

    @Override
    protected void registerGoals() {
        // Очищаем все цели
        this.goalSelector.removeAllGoals(goal -> true);
        this.targetSelector.removeAllGoals(goal -> true);

        // Базовые цели поведения
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(4, new LeapAtTargetGoal(this, 0.4F));
        this.goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(6, new FollowOwnerGoal(this, 1.0D, 10.0F, 2.0F, false));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));

        // Цели для атаки - ТОЛЬКО враждебные мобы
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this, Player.class).setAlertOthers()); // Не атакуем игроков в ответ
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Monster.class, true));
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
        // Не атакуем игроков и других дружественных существ
        if (target instanceof Player) {
            return false;
        }

        // Не атакуем других братьев
        if (target instanceof DagestanskiBrother || target instanceof DagestanskiRogue) {
            return false;
        }

        // Атакуем только враждебных мобов
        return target instanceof Monster && super.canAttack(target);
    }

    @Override
    public boolean isFood(net.minecraft.world.item.ItemStack stack) {
        return false; // Братва не ест
    }

    @Override
    public boolean canBeLeashed(Player player) {
        return false; // Нельзя привязать
    }

    @Override
    protected void dropAllDeathLoot(net.minecraft.world.damagesource.DamageSource damageSource) {
        // Не дропаем лут при смерти
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false; // Не исчезаем при удалении игрока
    }
}
