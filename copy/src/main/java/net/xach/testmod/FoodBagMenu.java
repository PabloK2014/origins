package net.xach.testmod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.xach.testmod.items.TestModItems;

public class FoodBagMenu extends AbstractContainerMenu {
    private final ItemStack bagStack;
    private final FoodBagInventory bagInventory;

    public FoodBagMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, playerInventory.player.getMainHandItem());
    }

    public FoodBagMenu(int containerId, Inventory playerInventory, ItemStack bagStack) {
        super(MenuRegistry.FOOD_BAG.get(), containerId);
        this.bagStack = bagStack;
        this.bagInventory = new FoodBagInventory(bagStack);

        // Изменено с 6 рядов на 3 ряда (3*9=27 слотов)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new FoodOnlySlot(bagInventory, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }

        // Инвентарь игрока (позиции Y скорректированы для 3 рядов)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Хотбар игрока
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            if (index < 27) { // Изменено с 54 на 27
                if (!this.moveItemStackTo(itemstack1, 27, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (itemstack1.getItem().getFoodProperties() != null) {
                    if (!this.moveItemStackTo(itemstack1, 0, 27, false)) { // Изменено с 54 на 27
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getMainHandItem().is(TestModItems.FOOD_BAG.get()) ||
                player.getOffhandItem().is(TestModItems.FOOD_BAG.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        bagInventory.save();
    }

    private static class FoodOnlySlot extends Slot {
        public FoodOnlySlot(FoodBagInventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem().getFoodProperties() != null;
        }
    }
}
