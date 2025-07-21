package net.xach.testmod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;


public class ClassSelectionPacket {

    private final String className;

    public ClassSelectionPacket(String className) {
        this.className = className;

    }

    public ClassSelectionPacket(FriendlyByteBuf buf) {
        this.className = buf.readUtf(32767);

    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(className, 32767);

    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                player.getCapability(TestMod.PlayerClassCapability.CAPABILITY).ifPresent(cap -> {
                    cap.setPlayerClass(className);
                    cap.sync(player);
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}