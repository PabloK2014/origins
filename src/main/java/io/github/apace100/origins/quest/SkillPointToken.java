package io.github.apace100.origins.quest;

import io.github.apace100.origins.profession.ProfessionComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

/**
 * Предмет-жетон для получения очков навыков
 */
public class SkillPointToken extends Item {
    
    private final int experienceValue;
    
    public SkillPointToken(Settings settings, int experienceValue) {
        super(settings);
        this.experienceValue = experienceValue;
    }
    
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        
        if (!world.isClient && user instanceof ServerPlayerEntity serverPlayer) {
            // Получаем компонент профессии игрока
            ProfessionComponent professionComponent = ProfessionComponent.KEY.get(serverPlayer);
            
            if (professionComponent != null) {
                // Добавляем опыт
                professionComponent.addExperience(experienceValue);
                
                // Уменьшаем количество предметов
                stack.decrement(1);
                
                // Отправляем сообщение игроку
                serverPlayer.sendMessage(
                    Text.literal("+" + experienceValue + " опыта получено!")
                        .formatted(Formatting.GREEN), 
                    false
                );
                
                return TypedActionResult.success(stack);
            }
        }
        
        return TypedActionResult.pass(stack);
    }
    
    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, net.minecraft.client.item.TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        
        tooltip.add(Text.literal("Дает " + experienceValue + " опыта")
            .formatted(Formatting.BLUE));
        tooltip.add(Text.literal("ПКМ для использования")
            .formatted(Formatting.GRAY));
    }
    
    public int getExperienceValue() {
        return experienceValue;
    }
}