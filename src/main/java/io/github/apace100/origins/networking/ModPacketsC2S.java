package io.github.apace100.origins.networking;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginLayers;
import io.github.apace100.origins.profession.ProfessionComponent;
import io.github.apace100.origins.profession.ProfessionProgress;
import io.github.apace100.origins.profession.ProfessionSkills;
import io.github.apace100.origins.registry.ModComponents;
import io.github.apace100.origins.skill.PlayerSkillComponent;
import io.github.apace100.origins.skill.SkillTreeHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

public class ModPacketsC2S {
    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(ModPackets.SYNC_SKILLS, 
            (MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, 
             PacketByteBuf buf, net.fabricmc.fabric.api.networking.v1.PacketSender responseSender) -> {
                String skillId = buf.readString();
                
                server.execute(() -> {
                    ProfessionComponent component = ProfessionComponent.KEY.get(player);
                    ProfessionProgress progress = component.getCurrentProgress();
                    
                    if (progress != null && progress.getSkillPoints() > 0) {
                        ProfessionSkills skills = progress.getSkills();
                        if (skills.canIncreaseSkill(skillId)) {
                            skills.increaseSkill(skillId);
                            progress.spendSkillPoint();
                            // Синхронизируем изменения с клиентом
                            ProfessionComponent.KEY.sync(player);
                        }
                    }
                });
            });

        ServerPlayNetworking.registerGlobalReceiver(ModPackets.LEARN_TREE_SKILL,
            (MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler,
             PacketByteBuf buf, net.fabricmc.fabric.api.networking.v1.PacketSender responseSender) -> {
                String skillId = buf.readString();

                server.execute(() -> {
                    // Получаем текущий класс игрока
                    OriginComponent originComponent = ModComponents.ORIGIN.get(player);
                    Origin origin = originComponent.getOrigin(OriginLayers.getLayer(Origins.identifier("origin")));
                    if (origin == null) return;

                    String currentClass = origin.getIdentifier().toString();
                    SkillTreeHandler.SkillTree skillTree = SkillTreeHandler.getSkillTree(currentClass);
                    if (skillTree == null) return;

                    // Ищем навык в дереве
                    SkillTreeHandler.Skill skill = null;
                    for (SkillTreeHandler.Skill s : skillTree.getAllSkills()) {
                        if (s.getId().equals(skillId)) {
                            skill = s;
                            break;
                        }
                    }

                    if (skill != null) {
                        PlayerSkillComponent skillComponent = PlayerSkillComponent.KEY.get(player);
                        if (skillComponent.canLearnSkill(skill)) {
                            skillComponent.learnSkill(skillId);
                            // Синхронизируем изменения с клиентом
                            PlayerSkillComponent.KEY.sync(player);
                        }
                    }
                });
            });
    }
}