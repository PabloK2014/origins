package net.xach.testmod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class VillageCompassPacket {
    private final int villageX;
    private final int villageZ;

    public VillageCompassPacket(int villageX, int villageZ) {
        this.villageX = villageX;
        this.villageZ = villageZ;
    }

    public VillageCompassPacket(FriendlyByteBuf buf) {
        this.villageX = buf.readInt();
        this.villageZ = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(villageX);
        buf.writeInt(villageZ);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Обрабатываем на клиенте
            VillageCompassRenderer.setVillagePosition(villageX, villageZ);
        });
        context.setPacketHandled(true);
    }
}
