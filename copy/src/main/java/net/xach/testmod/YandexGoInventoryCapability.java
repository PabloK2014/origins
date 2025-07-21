package net.xach.testmod;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.INBTSerializable;

public class YandexGoInventoryCapability implements INBTSerializable<CompoundTag> {
    private final ItemStack[] extraSlots = new ItemStack[3];

    public YandexGoInventoryCapability() {
        for (int i = 0; i < extraSlots.length; i++) {
            extraSlots[i] = ItemStack.EMPTY;
        }
    }

    public ItemStack getStackInSlot(int slot) {
        if (slot >= 0 && slot < extraSlots.length) {
            return extraSlots[slot];
        }
        return ItemStack.EMPTY;
    }

    public void setStackInSlot(int slot, ItemStack stack) {
        if (slot >= 0 && slot < extraSlots.length) {
            extraSlots[slot] = stack;
        }
    }

    public int getSlots() {
        return extraSlots.length;
    }

    public boolean isSlotUnlocked(int slot, int skillLevel) {
        return slot < skillLevel; // Слот разблокирован если его номер меньше уровня скилла
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        ListTag itemsList = new ListTag();

        for (int i = 0; i < extraSlots.length; i++) {
            if (!extraSlots[i].isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putByte("Slot", (byte) i);
                extraSlots[i].save(itemTag);
                itemsList.add(itemTag);
            }
        }

        tag.put("ExtraSlots", itemsList);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        // Очищаем слоты
        for (int i = 0; i < extraSlots.length; i++) {
            extraSlots[i] = ItemStack.EMPTY;
        }

        ListTag itemsList = tag.getList("ExtraSlots", 10);
        for (int i = 0; i < itemsList.size(); i++) {
            CompoundTag itemTag = itemsList.getCompound(i);
            int slot = itemTag.getByte("Slot") & 255;
            if (slot >= 0 && slot < extraSlots.length) {
                extraSlots[slot] = ItemStack.of(itemTag);
            }
        }
    }
}
