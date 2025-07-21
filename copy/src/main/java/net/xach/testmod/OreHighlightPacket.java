package net.xach.testmod;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public class OreHighlightPacket {
    private final Set<BlockPos> orePositions;
    private final long duration;

    public OreHighlightPacket(Set<BlockPos> orePositions, long duration) {
        this.orePositions = orePositions;
        this.duration = duration;
    }

    public OreHighlightPacket(FriendlyByteBuf buf) {
        int size = buf.readInt();
        this.orePositions = new HashSet<>();
        for (int i = 0; i < size; i++) {
            this.orePositions.add(buf.readBlockPos());
        }
        this.duration = buf.readLong();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(orePositions.size());
        for (BlockPos pos : orePositions) {
            buf.writeBlockPos(pos);
        }
        buf.writeLong(duration);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // Это выполняется на клиенте
            if (context.getDirection().getReceptionSide().isClient()) {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.player != null) {
                    OreHighlightRenderer.setHighlightedOres(mc.player.getUUID(), orePositions, duration);
                }
            }
        });
        return true;
    }
}
