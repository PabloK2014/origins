package net.xach.testmod;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class FoodBagInventory implements Container {
    private final ItemStack bagStack;
    private final ItemStack[] items = new ItemStack[27]; // Изменено с 54 на 27
    private static final String ITEMS_TAG = "Items";

    public FoodBagInventory(ItemStack bagStack) {
        this.bagStack = bagStack;
        load();
    }

    private void load() {
        for (int i = 0; i < items.length; i++) {
            items[i] = ItemStack.EMPTY;
        }

        CompoundTag tag = bagStack.getOrCreateTag();
        ListTag itemsList = tag.getList(ITEMS_TAG, 10);

        for (int i = 0; i < itemsList.size(); i++) {
            CompoundTag itemTag = itemsList.getCompound(i);
            int slot = itemTag.getByte("Slot") & 255;
            if (slot >= 0 && slot < items.length) {
                items[slot] = ItemStack.of(itemTag);
            }
        }
    }

    public void save() {
        CompoundTag tag = bagStack.getOrCreateTag();
        ListTag itemsList = new ListTag();

        for (int i = 0; i < items.length; i++) {
            if (!items[i].isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putByte("Slot", (byte) i);
                items[i].save(itemTag);
                itemsList.add(itemTag);
            }
        }

        tag.put(ITEMS_TAG, itemsList);
    }

    @Override
    public int getContainerSize() {
        return items.length;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack item : items) {
            if (!item.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int index) {
        return index >= 0 && index < items.length ? items[index] : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        if (index >= 0 && index < items.length && !items[index].isEmpty() && count > 0) {
            return items[index].split(count);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        if (index >= 0 && index < items.length) {
            ItemStack item = items[index];
            items[index] = ItemStack.EMPTY;
            return item;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        if (index >= 0 && index < items.length) {
            items[index] = stack;
            if (stack.getCount() > getMaxStackSize()) {
                stack.setCount(getMaxStackSize());
            }
        }
    }

    @Override
    public void setChanged() {
        save();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < items.length; i++) {
            items[i] = ItemStack.EMPTY;
        }
    }
}
