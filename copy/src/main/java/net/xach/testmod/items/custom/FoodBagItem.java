package net.xach.testmod.items.custom;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import net.xach.testmod.FoodBagMenu;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FoodBagItem extends Item {
    private static final int MAX_FOOD_ITEMS = 27; // Как сундук
    private static final String ITEMS_TAG = "Items";

    public FoodBagItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack bagStack = player.getItemInHand(hand);

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.literal("Сумка для еды");
                }

                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
                    return new FoodBagMenu(containerId, playerInventory, bagStack);
                }
            });
        }

        return InteractionResultHolder.sidedSuccess(bagStack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag bagTag = stack.getOrCreateTag();
        ListTag itemsList = bagTag.getList(ITEMS_TAG, 10);

        int itemCount = 0;
        for (int i = 0; i < itemsList.size(); i++) {
            CompoundTag itemTag = itemsList.getCompound(i);
            ItemStack itemStack = ItemStack.of(itemTag);
            if (!itemStack.isEmpty()) {
                itemCount++;
            }
        }

        tooltip.add(Component.literal("Еды в сумке: " + itemCount + "/" + MAX_FOOD_ITEMS));
        tooltip.add(Component.literal("ПКМ - открыть сумку"));
        tooltip.add(Component.literal("Только для еды!"));

        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isRepairable(ItemStack stack) {
        return false;
    }
}
