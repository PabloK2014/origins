package net.xach.testmod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;


public class SkillUpgradePacket {

    private final String skillId;

    public SkillUpgradePacket(String skillId) {
        this.skillId = skillId;

    }

    public SkillUpgradePacket(FriendlyByteBuf buf) {
        this.skillId = buf.readUtf(32767);

    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(skillId, 32767);

    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                player.getCapability(TestMod.PlayerClassCapability.CAPABILITY).ifPresent(cap -> {
                    if (cap.getSkillPoints() > 0) {
                        int currentLevel = cap.getSkillLevel(skillId);
                        cap.setSkillLevel(skillId, currentLevel + 1);
                        cap.spendSkillPoint();
                        cap.sync(player);

                    }
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}