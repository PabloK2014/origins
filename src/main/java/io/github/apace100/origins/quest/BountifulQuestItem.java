package io.github.apace100.origins.quest;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.List;
import java.util.Locale;

/**
 * Предмет квеста, аналогичный BountyItem из Bountiful
 */
public class BountifulQuestItem extends Item {
    
    public BountifulQuestItem() {
        super(new Settings().maxCount(1).fireproof());
    }
    
    /**
     * Регистрирует предикаты модели для разных редкостей
     */
    @Environment(EnvType.CLIENT)
    public static void registerModelPredicates() {
        ModelPredicateProviderRegistry.register(
            QuestRegistry.BOUNTIFUL_QUEST_ITEM,
            new Identifier("origins", "rarity"),
            (stack, world, entity, seed) -> {
                BountifulQuestInfo info = BountifulQuestInfo.get(stack);
                return info.getRarity().ordinal() * 0.1f;
            }
        );
    }
    
    @Override
    public Text getName(ItemStack stack) {
        BountifulQuestInfo info = BountifulQuestInfo.get(stack);
        Quest.QuestRarity rarity = info.getRarity();
        
        // Создаем название квеста
        String rarityName = rarity.getName();
        String capitalizedRarity = rarityName.substring(0, 1).toUpperCase(Locale.ROOT) + 
                                 rarityName.substring(1);
        
        net.minecraft.text.MutableText questName = Text.translatable("origins.quest.name", capitalizedRarity)
            .formatted(rarity.getColor());
        
        // Добавляем жирность для эпических квестов
        if (rarity == Quest.QuestRarity.EPIC) {
            questName = questName.formatted(Formatting.BOLD);
        }
        
        // Добавляем профессию если она указана
        if (!info.getProfession().equals("any")) {
            questName = questName.append(" (")
                .append(Text.translatable("origins.profession." + info.getProfession()))
                .append(")");
        }
        
        return questName;
    }
    
    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        if (world != null) {
            BountifulQuestInfo info = BountifulQuestInfo.get(stack);
            BountifulQuestData data = BountifulQuestData.get(stack);
            
            List<net.minecraft.text.MutableText> questTooltip = info.generateTooltip(data, world instanceof ServerWorld);
            for (net.minecraft.text.MutableText text : questTooltip) {
                tooltip.add(text);
            }
        }
        
        super.appendTooltip(stack, world, tooltip, context);
    }
    
    /**
     * Создает новый квест-предмет
     */
    public static ItemStack createQuestItem(BountifulQuestData data, BountifulQuestInfo info) {
        ItemStack stack = new ItemStack(QuestRegistry.BOUNTIFUL_QUEST_ITEM);
        
        BountifulQuestData.set(stack, data);
        BountifulQuestInfo.set(stack, info);
        
        return stack;
    }
}