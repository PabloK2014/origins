package io.github.apace100.origins.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class FoodBagItem extends Item {
    private static final int INVENTORY_SIZE = 27; // 3 rows of 9 slots

    public FoodBagItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        
        if (!world.isClient) {
            // Open the food bag inventory
            user.openHandledScreen(new FoodBagScreenHandlerFactory(stack));
        }
        
        return TypedActionResult.success(stack);
    }

    // Screen handler factory for the food bag
    private static class FoodBagScreenHandlerFactory implements net.minecraft.screen.NamedScreenHandlerFactory {
        private final ItemStack foodBagStack;

        public FoodBagScreenHandlerFactory(ItemStack foodBagStack) {
            this.foodBagStack = foodBagStack;
        }

        @Override
        public Text getDisplayName() {
            return Text.translatable("item.origins.food_bag");
        }

        @Override
        public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
            return new FoodBagScreenHandler(syncId, playerInventory, foodBagStack);
        }
    }

    // Screen handler for the food bag
    public static class FoodBagScreenHandler extends GenericContainerScreenHandler {
        private final ItemStack foodBagStack;
        private final SimpleInventory inventory;

        public FoodBagScreenHandler(int syncId, PlayerInventory playerInventory, ItemStack foodBagStack) {
            super(ScreenHandlerType.GENERIC_9X3, syncId, playerInventory, new SimpleInventory(INVENTORY_SIZE), 3);
            this.foodBagStack = foodBagStack;
            this.inventory = (SimpleInventory) getInventory();
            
            // Load items from NBT
            loadInventoryFromNBT();
            
            // Add listener to save inventory when it changes
            inventory.addListener(this::onInventoryChanged);
        }

        private void loadInventoryFromNBT() {
            NbtCompound tag = foodBagStack.getOrCreateNbt();
            if (tag.contains("Items", 9)) { // 9 is NBT list type
                NbtList itemsList = tag.getList("Items", 10); // 10 is NBT compound type
                for (int i = 0; i < itemsList.size(); i++) {
                    NbtCompound itemTag = itemsList.getCompound(i);
                    int slot = itemTag.getByte("Slot") & 255;
                    if (slot >= 0 && slot < inventory.size()) {
                        inventory.setStack(slot, ItemStack.fromNbt(itemTag));
                    }
                }
            }
        }

        private void saveInventoryToNBT() {
            NbtCompound tag = foodBagStack.getOrCreateNbt();
            NbtList itemsList = new NbtList();
            
            for (int i = 0; i < inventory.size(); i++) {
                ItemStack stack = inventory.getStack(i);
                if (!stack.isEmpty()) {
                    NbtCompound itemTag = new NbtCompound();
                    itemTag.putByte("Slot", (byte) i);
                    stack.writeNbt(itemTag);
                    itemsList.add(itemTag);
                }
            }
            
            tag.put("Items", itemsList);
        }

        private void onInventoryChanged(net.minecraft.inventory.Inventory inventory) {
            saveInventoryToNBT();
        }

        @Override
        public boolean canUse(PlayerEntity player) {
            return true;
        }

        @Override
        public ItemStack quickMove(PlayerEntity player, int index) {
            ItemStack itemStack = ItemStack.EMPTY;
            net.minecraft.screen.slot.Slot slot = this.slots.get(index);
            
            if (slot != null && slot.hasStack()) {
                ItemStack itemStack2 = slot.getStack();
                itemStack = itemStack2.copy();
                
                // If clicking in the food bag inventory
                if (index < INVENTORY_SIZE) {
                    // Try to move to player inventory
                    if (!this.insertItem(itemStack2, INVENTORY_SIZE, this.slots.size(), true)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    // If clicking in player inventory, only allow food items into the food bag
                    if (itemStack2.getItem().isFood()) {
                        if (!this.insertItem(itemStack2, 0, INVENTORY_SIZE, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else {
                        return ItemStack.EMPTY;
                    }
                }
                
                if (itemStack2.isEmpty()) {
                    slot.setStack(ItemStack.EMPTY);
                } else {
                    slot.markDirty();
                }
            }
            
            return itemStack;
        }
    }
}