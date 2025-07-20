package io.github.apace100.origins.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.apace100.origins.power.BlacksmithQualityCraftingPower;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * Команда для выдачи предметов с определенным качеством
 * Использование: /origins give <игрок> <предмет> <качество>
 */
public class GiveQualityItemCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(literal("origins")
            .then(literal("give")
                .requires(source -> source.hasPermissionLevel(2))
                .then(argument("player", EntityArgumentType.player())
                    .then(argument("item", ItemStackArgumentType.itemStack(registryAccess))
                        .then(argument("quality", IntegerArgumentType.integer(0, 3))
                            .executes(context -> executeGiveQualityItem(context, registryAccess))
                        )
                        .executes(context -> executeGiveRandomQualityItem(context, registryAccess))
                    )
                )
            )
        );
    }
    
    private static int executeGiveQualityItem(CommandContext<ServerCommandSource> context, CommandRegistryAccess registryAccess) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        ItemStack itemStack = ItemStackArgumentType.getItemStackArgument(context, "item").createStack(1, false);
        int qualityIndex = IntegerArgumentType.getInteger(context, "quality");
        
        // Конвертируем индекс в качество
        BlacksmithQualityCraftingPower.ItemQuality quality = getQualityByIndex(qualityIndex);
        
        // Создаем предмет с качеством
        ItemStack qualityItem = createItemWithQuality(itemStack, quality);
        
        // Выдаем предмет игроку
        if (!player.getInventory().insertStack(qualityItem)) {
            player.dropItem(qualityItem, false);
        }
        
        // Отправляем сообщение
        context.getSource().sendFeedback(() -> Text.literal(
            String.format("Выдан предмет %s с качеством %s игроку %s", 
                itemStack.getItem().getName().getString(),
                quality.getDisplayName(),
                player.getName().getString())
        ), true);
        
        return 1;
    }
    
    private static int executeGiveRandomQualityItem(CommandContext<ServerCommandSource> context, CommandRegistryAccess registryAccess) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        ItemStack itemStack = ItemStackArgumentType.getItemStackArgument(context, "item").createStack(1, false);
        
        // Случайное качество
        BlacksmithQualityCraftingPower.ItemQuality[] qualities = BlacksmithQualityCraftingPower.ItemQuality.values();
        BlacksmithQualityCraftingPower.ItemQuality quality = qualities[player.getRandom().nextInt(qualities.length)];
        
        // Создаем предмет с качеством
        ItemStack qualityItem = createItemWithQuality(itemStack, quality);
        
        // Выдаем предмет игроку
        if (!player.getInventory().insertStack(qualityItem)) {
            player.dropItem(qualityItem, false);
        }
        
        // Отправляем сообщение
        context.getSource().sendFeedback(() -> Text.literal(
            String.format("Выдан предмет %s со случайным качеством %s игроку %s", 
                itemStack.getItem().getName().getString(),
                quality.getDisplayName(),
                player.getName().getString())
        ), true);
        
        return 1;
    }
    
    private static BlacksmithQualityCraftingPower.ItemQuality getQualityByIndex(int index) {
        return switch (index) {
            case 0 -> BlacksmithQualityCraftingPower.ItemQuality.POOR;
            case 1 -> BlacksmithQualityCraftingPower.ItemQuality.NORMAL;
            case 2 -> BlacksmithQualityCraftingPower.ItemQuality.GOOD;
            case 3 -> BlacksmithQualityCraftingPower.ItemQuality.LEGENDARY;
            default -> BlacksmithQualityCraftingPower.ItemQuality.NORMAL;
        };
    }
    
    private static ItemStack createItemWithQuality(ItemStack baseItem, BlacksmithQualityCraftingPower.ItemQuality quality) {
        ItemStack result = baseItem.copy();
        
        // Применяем качество к предмету
        if (quality != BlacksmithQualityCraftingPower.ItemQuality.NORMAL) {
            var nbt = result.getOrCreateNbt();
            nbt.putString("ItemQuality", quality.name());
            nbt.putString("QualityDisplay", quality.getDisplayName());
            nbt.putString("QualityColor", quality.getColor().getName());
            
            // Применяем модификаторы качества
            BlacksmithQualityCraftingPower.QualityModifiers modifiers = quality.getModifiersForItem(result);
            
            if (modifiers.durabilityModifier != 0.0f && result.getItem().isDamageable()) {
                int baseDurability = result.getMaxDamage();
                int newDurability = Math.max(1, (int)(baseDurability * (1.0f + modifiers.durabilityModifier)));
                nbt.putInt("OriginalMaxDamage", baseDurability);
                nbt.putInt("ModifiedMaxDamage", newDurability);
            }
            
            // Добавляем легендарные эффекты
            if (quality == BlacksmithQualityCraftingPower.ItemQuality.LEGENDARY) {
                addLegendaryEffects(result);
            }
        }
        
        return result;
    }
    
    private static void addLegendaryEffects(ItemStack stack) {
        var nbt = stack.getOrCreateNbt();
        var item = stack.getItem();
        
        if (item instanceof net.minecraft.item.SwordItem) {
            nbt.putString("LegendaryEffect1", "Вампиризм: +20% исцеления от урона");
            nbt.putString("LegendaryEffect2", "25% шанс поджечь врага");
        } else if (item instanceof net.minecraft.item.PickaxeItem) {
            nbt.putString("LegendaryEffect1", "Копание 3x3");
            nbt.putString("LegendaryEffect2", "+1 уровень Fortune");
        } else if (item instanceof net.minecraft.item.AxeItem) {
            nbt.putString("LegendaryEffect1", "Удвоенный дроп древесины");
        } else if (item instanceof net.minecraft.item.ShovelItem) {
            nbt.putString("LegendaryEffect1", "Мгновенное копание песка/гравия");
            nbt.putString("LegendaryEffect2", "20% шанс удвоенного дропа");
        } else if (item instanceof net.minecraft.item.HoeItem) {
            nbt.putString("LegendaryEffect1", "Возделывание 3x3");
        } else if (item instanceof net.minecraft.item.ArmorItem armor) {
            switch (armor.getSlotType()) {
                case HEAD -> nbt.putString("LegendaryEffect1", "25% блокировка стрел");
                case CHEST -> {
                    nbt.putString("LegendaryEffect1", "40% игнорирование урона");
                    nbt.putString("LegendaryEffect2", "Замедление атакующего");
                }
                case LEGS -> {
                    nbt.putString("LegendaryEffect1", "+Скорость передвижения");
                    nbt.putString("LegendaryEffect2", "Игнорирование замедления в воде");
                }
                case FEET -> {
                    nbt.putString("LegendaryEffect1", "+1 блок к прыжку");
                    nbt.putString("LegendaryEffect2", "Иммунитет к урону от падения");
                }
            }
        }
    }
}