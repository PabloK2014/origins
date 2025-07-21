package net.xach.testmod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.function.Supplier;


public class OpenClassSelectionPacket {


    public OpenClassSelectionPacket() {}

    public OpenClassSelectionPacket(FriendlyByteBuf buf) {}

    public void toBytes(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {

                NetworkHooks.openScreen(player, new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return Component.literal("Select Class");
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
                        return new ClassSelectionMenu(id, inv);
                    }
                });
            } else {

            }
        });
        ctx.get().setPacketHandled(true);
    }
}