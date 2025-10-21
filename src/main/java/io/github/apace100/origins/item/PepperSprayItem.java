package io.github.apace100.origins.item;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.particle.ParticleTypes;
import io.github.apace100.origins.registry.ModItems;

public class PepperSprayItem extends Item {
    public PepperSprayItem(Settings settings) {
        super(settings);
    }
    
    public static ItemStack createPepperSpray(int skillLevel) {
        ItemStack stack = new ItemStack(ModItems.PEPPER_SPRAY);
        
        // Set max damage based on skill level
        int maxDamage = getDurabilityForSkillLevel(skillLevel);
        stack.getOrCreateNbt().putInt("MaxDamage", maxDamage);
        stack.setDamage(0); // Start with full durability
        
        // Store skill level for tooltip and damage calculation
        stack.getOrCreateNbt().putInt("SkillLevel", skillLevel);
        
        return stack;
    }
    
    private static int getDurabilityForSkillLevel(int skillLevel) {
        switch (skillLevel) {
            case 1: return 32; // 32 durability at level 1
            case 2: return 64; // 64 durability at level 2
            case 3: case 4: return 64; // 64 durability at levels 3 and 4
            default: return 32; // Default to level 1
        }
    }
    


    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        
        // Check if item has durability left using custom max damage
        if (stack.getDamage() >= getActualMaxDamage(stack)) {
            user.sendMessage(net.minecraft.text.Text.literal("Перцовый баллончик пуст!"), true);
            return TypedActionResult.fail(stack);
        }
        
        if (!world.isClient) {
            // Get the skill level from player's skill component
            int skillLevel = getSkillLevel(user);
            
            net.minecraft.util.math.Vec3d lookDirection = user.getRotationVector();
            net.minecraft.util.math.Vec3d userEyePos = new net.minecraft.util.math.Vec3d(
                user.getX(), user.getEyeY(), user.getZ()
            );
            ServerWorld serverWorld = (ServerWorld)world;

            // 1. Directional spray (Far range) - MINIMAL PARTICLES
            // Только 3 точки по направлению взгляда
            for (int i = 2; i <= 6; i += 2) { // 3 точки: 2, 4, 6 метров
                net.minecraft.util.math.Vec3d sprayPos = userEyePos.add(lookDirection.multiply(i));
                
                // Только одна CAMPFIRE_COSY_SMOKE частица на точку
                serverWorld.spawnParticles(
                    ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    sprayPos.x, sprayPos.y, sprayPos.z,
                    1, 
                    0.0, 0.0, 0.0, 
                    0.0 
                );
            }
            
            // 2. Burst effect at the user's position - MINIMAL PARTICLES
            // Только 3 частицы вокруг игрока
            for (int i = 0; i < 3; i++) { 
                double offsetX = (world.random.nextFloat() - 0.5) * 0.5;
                double offsetY = (world.random.nextFloat() - 0.5) * 0.5;
                double offsetZ = (world.random.nextFloat() - 0.5) * 0.5;
                
                serverWorld.spawnParticles(
                    ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    user.getX(), user.getY(), user.getZ(),
                    1, 
                    offsetX * 0.05, 
                    offsetY * 0.05,
                    offsetZ * 0.05,
                    0.0
                );
            }
            
            // 3. Cone-shaped particle effect - MINIMAL PARTICLES
            // Только 5 частиц в области спрея
            for (int i = 0; i < 5; i++) { 
                double distance = 2.0 + world.random.nextDouble() * 4.0; // от 2 до 6 метров
                double angle = world.random.nextDouble() * Math.PI * 0.5; // уменьшенный угол распыла
                double spread = distance * 0.1; 
                
                double x = userEyePos.x + lookDirection.x * distance + Math.cos(angle) * spread;
                double y = userEyePos.y + lookDirection.y * distance;
                double z = userEyePos.z + lookDirection.z * distance + Math.sin(angle) * spread;
                
                serverWorld.spawnParticles(
                    ParticleTypes.SMOKE,
                    x, y, z,
                    1,
                    0.0, 0.0, 0.0,
                    0.0
                );
            }

            // --- ЛОГИКА АТАКЕ (НЕ ИЗМЕНЕНА) ---
            
            net.minecraft.util.math.Box baseBox = new net.minecraft.util.math.Box(
                userEyePos.x - 1.0, userEyePos.y - 1.0, userEyePos.z - 1.0,
                userEyePos.x + 1.0, userEyePos.y + 1.0, userEyePos.z + 1.0
            );
            net.minecraft.util.math.Box sprayArea = baseBox.stretch(lookDirection.multiply(6.0));

            java.util.List<net.minecraft.entity.LivingEntity> targets = world.getEntitiesByClass(
                net.minecraft.entity.LivingEntity.class, 
                sprayArea,
                entity -> entity != user && 
                             entity.isAlive() && 
                             entity.isAttackable() && 
                             (entity instanceof net.minecraft.entity.mob.MobEntity ||
                              entity instanceof net.minecraft.entity.decoration.ArmorStandEntity ||
                              entity instanceof net.minecraft.entity.passive.IronGolemEntity ||
                              entity instanceof net.minecraft.entity.passive.SnowGolemEntity)
            );

            for (net.minecraft.entity.LivingEntity target : targets) {
                net.minecraft.util.math.Vec3d toTarget = target.getPos().subtract(userEyePos).normalize();
                double dotProduct = lookDirection.dotProduct(toTarget);
                
                if (dotProduct > 0.5) { 
                    double distance = target.squaredDistanceTo(user);
                    if (distance <= 25.0) { 
                        // Damage based on skill level
                        float damage = getDamageForSkillLevel(skillLevel);
                        target.damage(world.getDamageSources().magic(), damage);
                        
                        target.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 140, 1));
                        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 2));
                        target.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 60, 0));
                        
                        // Poison chance on level 4
                        if (skillLevel == 4 && world.random.nextInt(100) < 10) { // 10% chance
                            target.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 80, 0)); // 4 seconds of poison
                        }
                    }
                }
            }
            
            // Damage the pepper spray item
            if (!user.isCreative()) {
                stack.damage(1, user, (player) -> player.sendToolBreakStatus(hand));
            }
            
            // Play sound - ИСПРАВЛЕНО
            world.playSound(null, user.getX(), user.getEyeY(), user.getZ(), 
                net.minecraft.sound.SoundEvents.BLOCK_REDSTONE_TORCH_BURNOUT, // Шипящий звук
                SoundCategory.PLAYERS, 
                1.5f, // Громче
                0.8f); // Ниже тон
            
            if (user instanceof ServerPlayerEntity serverPlayer) {
                // ... (логика для шейка экрана)
            }
        } else {
            // On client, play sound
            world.playSound(user, user.getX(), user.getEyeY(), user.getZ(), 
                net.minecraft.sound.SoundEvents.BLOCK_REDSTONE_TORCH_BURNOUT,
                SoundCategory.PLAYERS, 
                1.5f, 
                0.8f);
        }
        
        return TypedActionResult.success(stack);
    }
    
    public int getActualMaxDamage(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains("MaxDamage")) {
            return nbt.getInt("MaxDamage");
        }
        return 64; // Default max damage
    }
    
    private int getSkillLevel(PlayerEntity player) {
        // Get the skill level from player's skill component
        try {
            io.github.apace100.origins.skill.PlayerSkillComponent skillComponent = 
                io.github.apace100.origins.skill.PlayerSkillComponent.KEY.get(player);
            if (skillComponent != null) {
                return skillComponent.getSkillLevel("carry_surge");
            }
        } catch (Exception e) {
            // If component is not available, assume level 1
            return 1;
        }
        return 1; // Default to level 1
    }
    
    private float getDamageForSkillLevel(int skillLevel) {
        switch (skillLevel) {
            case 1: return 4.0f; // 4 damage at level 1
            case 2: return 6.0f; // 6 damage at level 2
            case 3: case 4: return 8.0f; // 8 damage at levels 3 and 4
            default: return 4.0f; // Default to level 1 if something goes wrong
        }
    }
    
    @Override
    public void appendTooltip(ItemStack stack, World world, java.util.List<net.minecraft.text.Text> tooltip, net.minecraft.client.item.TooltipContext context) {
        int actualMaxDamage = getActualMaxDamage(stack);
        int remainingUses = actualMaxDamage - stack.getDamage();
        tooltip.add(net.minecraft.text.Text.translatable("item.origins.pepper_spray.uses", remainingUses, actualMaxDamage)
            .formatted(Formatting.GRAY));
        
        // Get the skill level from NBT to show appropriate damage in tooltip
        NbtCompound nbt = stack.getNbt();
        int skillLevel = 1;
        if (nbt != null && nbt.contains("SkillLevel")) {
            skillLevel = nbt.getInt("SkillLevel");
        }
        
        tooltip.add(net.minecraft.text.Text.translatable("item.origins.pepper_spray.damage_info", getDamageForSkillLevel(skillLevel))
            .formatted(Formatting.RED));
        
        // Show poison chance for level 4
        if (skillLevel == 4) {
            tooltip.add(net.minecraft.text.Text.translatable("item.origins.pepper_spray.poison_chance")
                .formatted(Formatting.DARK_GREEN));
        }
        
        tooltip.add(net.minecraft.text.Text.translatable("item.origins.pepper_spray.effect_info")
            .formatted(Formatting.BLUE));
        super.appendTooltip(stack, world, tooltip, context);
    }
}