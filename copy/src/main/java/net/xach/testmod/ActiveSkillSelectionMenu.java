package net.xach.testmod;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class ActiveSkillSelectionMenu extends AbstractContainerMenu {
    public ActiveSkillSelectionMenu(int containerId, Inventory inventory) {
        super(MenuRegistry.ACTIVE_SKILL_SELECTION.get(), containerId);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}