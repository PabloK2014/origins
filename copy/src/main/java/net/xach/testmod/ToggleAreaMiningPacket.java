package net.xach.testmod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ToggleAreaMiningPacket {

    public ToggleAreaMiningPacket() {
    }

    public ToggleAreaMiningPacket(FriendlyByteBuf buf) {
        // Пакет не содержит данных
    }

    public void toBytes(FriendlyByteBuf buf) {
        // Пакет не содержит данных
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                MinerSkillHandler.toggleAreaMining(player);
            }
        });
        return true;
    }
}
