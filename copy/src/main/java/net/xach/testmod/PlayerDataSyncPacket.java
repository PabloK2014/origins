package net.xach.testmod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.client.Minecraft;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class PlayerDataSyncPacket {
    private final String playerClass;
    private final int level;
    private final int experience;
    private final Map<String, Integer> skillLevels;
    private final int skillPoints;
    private final int surgeEnergy;
    private final String activeSkill;

    public PlayerDataSyncPacket(String playerClass, int level, int experience, Map<String, Integer> skillLevels, int skillPoints, int surgeEnergy, String activeSkill) {
        this.playerClass = playerClass;
        this.level = level;
        this.experience = experience;
        this.skillLevels = new HashMap<>(skillLevels);
        this.skillPoints = skillPoints;
        this.surgeEnergy = surgeEnergy;
        this.activeSkill = activeSkill;
    }

    public PlayerDataSyncPacket(FriendlyByteBuf buf) {
        this.playerClass = buf.readUtf(32767);
        this.level = buf.readInt();
        this.experience = buf.readInt();
        this.skillPoints = buf.readInt();
        this.surgeEnergy = buf.readInt();
        this.activeSkill = buf.readUtf(32767);
        this.skillLevels = new HashMap<>();
        int skillCount = buf.readInt();
        for (int i = 0; i < skillCount; i++) {
            skillLevels.put(buf.readUtf(32767), buf.readInt());
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(playerClass, 32767);
        buf.writeInt(level);
        buf.writeInt(experience);
        buf.writeInt(skillPoints);
        buf.writeInt(surgeEnergy);
        buf.writeUtf(activeSkill, 32767);
        buf.writeInt(skillLevels.size());
        skillLevels.forEach((id, level) -> {
            buf.writeUtf(id, 32767);
            buf.writeInt(level);
        });
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.getCapability(TestMod.PlayerClassCapability.CAPABILITY).ifPresent(cap -> {
                    cap.setPlayerClass(playerClass);
                    cap.setLevel(level);
                    cap.setExperience(experience);
                    cap.setSkillPoints(skillPoints);
                    cap.setSurgeEnergy(surgeEnergy);
                    cap.setActiveSkill(activeSkill);
                    cap.clearSkillLevels();
                    skillLevels.forEach(cap::setSkillLevel);
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}