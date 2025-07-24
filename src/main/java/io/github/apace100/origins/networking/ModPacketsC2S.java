package io.github.apace100.origins.networking;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginLayer;
import io.github.apace100.origins.origin.OriginLayers;
import io.github.apace100.origins.origin.OriginRegistry;
import io.github.apace100.origins.profession.ProfessionComponent;
import io.github.apace100.origins.profession.ProfessionProgress;
import io.github.apace100.origins.profession.ProfessionSkills;
import io.github.apace100.origins.registry.ModComponents;
import io.github.apace100.origins.skill.PlayerSkillComponent;
import io.github.apace100.origins.skill.SkillTreeHandler;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

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

        // Обработчик выбора происхождения
        ServerPlayNetworking.registerGlobalReceiver(ModPackets.CHOOSE_ORIGIN, (server, player, handler, buf, responseSender) -> {
            String originId = buf.readString();
            String layerId = buf.readString();
            
            server.execute(() -> {
                try {
                    Identifier originIdentifier = new Identifier(originId);
                    Identifier layerIdentifier = new Identifier(layerId);
                    
                    Origin origin = OriginRegistry.get(originIdentifier);
                    OriginLayer layer = OriginLayers.getLayer(layerIdentifier);
                    
                    if (origin != null && layer != null) {
                        OriginComponent component = ModComponents.ORIGIN.get(player);
                        component.setOrigin(layer, origin);
                        component.sync();
                        
                        // Отправляем подтверждение клиенту
                        PacketByteBuf confirmBuf = PacketByteBufs.create();
                        confirmBuf.writeIdentifier(layerIdentifier);
                        confirmBuf.writeIdentifier(originIdentifier);
                        ServerPlayNetworking.send(player, ModPackets.CONFIRM_ORIGIN, confirmBuf);
                        
                        // Проверяем, выбраны ли все происхождения
                        if (component.hasAllOrigins()) {
                            OriginComponent.onChosen(player, false);
                        }
                        
                        Origins.LOGGER.info("Игрок {} выбрал происхождение {} в слое {}", 
                            player.getName().getString(), originId, layerId);
                    }
                } catch (Exception e) {
                    Origins.LOGGER.error("Ошибка при выборе происхождения: " + e.getMessage(), e);
                }
            });
        });

        // Обработчик случайного выбора происхождения
        ServerPlayNetworking.registerGlobalReceiver(ModPackets.CHOOSE_RANDOM_ORIGIN, (server, player, handler, buf, responseSender) -> {
            String layerId = buf.readString();
            
            server.execute(() -> {
                try {
                    Identifier layerIdentifier = new Identifier(layerId);
                    OriginLayer layer = OriginLayers.getLayer(layerIdentifier);
                    
                    if (layer != null && layer.isRandomAllowed()) {
                        var randomOrigins = layer.getRandomOrigins(player);
                        if (!randomOrigins.isEmpty()) {
                            Identifier randomOriginId = randomOrigins.get(server.getOverworld().getRandom().nextInt(randomOrigins.size()));
                            Origin randomOrigin = OriginRegistry.get(randomOriginId);
                            
                            OriginComponent component = ModComponents.ORIGIN.get(player);
                            component.setOrigin(layer, randomOrigin);
                            component.sync();
                            
                            // Отправляем подтверждение клиенту
                            PacketByteBuf confirmBuf = PacketByteBufs.create();
                            confirmBuf.writeIdentifier(layerIdentifier);
                            confirmBuf.writeIdentifier(randomOrigin.getIdentifier());
                            ServerPlayNetworking.send(player, ModPackets.CONFIRM_ORIGIN, confirmBuf);
                            
                            // Проверяем, выбраны ли все происхождения
                            if (component.hasAllOrigins()) {
                                OriginComponent.onChosen(player, false);
                            }
                            
                            Origins.LOGGER.info("Игрок {} получил случайное происхождение {} в слое {}", 
                                player.getName().getString(), randomOrigin.getIdentifier(), layerId);
                        }
                    }
                } catch (Exception e) {
                    Origins.LOGGER.error("Ошибка при случайном выборе происхождения: " + e.getMessage(), e);
                }
            });
        });

        // Обработчик использования активных способностей
        ServerPlayNetworking.registerGlobalReceiver(ModPackets.USE_ACTIVE_POWERS, (server, player, handler, buf, responseSender) -> {
            String key = buf.readString();
            
            server.execute(() -> {
                try {
                    OriginComponent component = ModComponents.ORIGIN.get(player);
                    component.getOrigins().values().forEach(origin -> {
                        origin.getPowerTypes().forEach(powerType -> {
                            // Здесь нужно будет добавить логику для активных способностей
                            // В зависимости от того, как реализованы активные способности в вашем моде
                            Origins.LOGGER.info("Использование активной способности: {} для игрока {}", 
                                powerType.getIdentifier(), player.getName().getString());
                        });
                    });
                } catch (Exception e) {
                    Origins.LOGGER.error("Ошибка при использовании активной способности: " + e.getMessage(), e);
                }
            });
        });

        // Обработчик прокачки энергии
        ServerPlayNetworking.registerGlobalReceiver(new Identifier("origins", "upgrade_energy"),
            (MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler,
             PacketByteBuf buf, net.fabricmc.fabric.api.networking.v1.PacketSender responseSender) -> {
                String upgradeType = buf.readString();

                server.execute(() -> {
                    PlayerSkillComponent skillComponent = PlayerSkillComponent.KEY.get(player);
                    ProfessionComponent professionComponent = ProfessionComponent.KEY.get(player);
                    ProfessionProgress progress = professionComponent.getCurrentProgress();
                    
                    if (progress != null && progress.getSkillPoints() > 0) {
                        if ("upgrade_max_energy".equals(upgradeType)) {
                            // Увеличиваем максимальную энергию на 5
                            int oldMaxEnergy = skillComponent.getMaxEnergy();
                            int energyIncrease = 5;
                            int maxEnergyLimit = 100; // Максимальный лимит энергии
                            
                            if (oldMaxEnergy >= maxEnergyLimit) {
                                player.sendMessage(
                                    Text.translatable("message.origins.energy.max_limit_reached", maxEnergyLimit)
                                        .formatted(net.minecraft.util.Formatting.RED), 
                                    false
                                );
                                return;
                            }
                            
                            int newMaxEnergy = Math.min(oldMaxEnergy + energyIncrease, maxEnergyLimit);
                            skillComponent.setMaxEnergy(newMaxEnergy);
                            progress.spendSkillPoint();
                            
                            player.sendMessage(
                                Text.translatable("message.origins.energy.max_upgraded", oldMaxEnergy, skillComponent.getMaxEnergy(), energyIncrease)
                                    .formatted(net.minecraft.util.Formatting.GREEN), 
                                false
                            );
                        } else if ("upgrade_energy_regen".equals(upgradeType)) {
                            // Увеличиваем скорость восстановления энергии на 1
                            int oldRegenRate = skillComponent.getEnergyRegenRate();
                            int regenIncrease = 1;
                            int maxRegenLimit = 10; // Максимальный лимит восстановления
                            
                            if (oldRegenRate >= maxRegenLimit) {
                                player.sendMessage(
                                    Text.translatable("message.origins.energy.regen_limit_reached", maxRegenLimit)
                                        .formatted(net.minecraft.util.Formatting.RED), 
                                    false
                                );
                                return;
                            }
                            
                            int newRegenRate = Math.min(oldRegenRate + regenIncrease, maxRegenLimit);
                            skillComponent.setEnergyRegenRate(newRegenRate);
                            progress.spendSkillPoint();
                            
                            player.sendMessage(
                                Text.translatable("message.origins.energy.regen_upgraded", oldRegenRate, newRegenRate, regenIncrease)
                                    .formatted(net.minecraft.util.Formatting.GREEN), 
                                false
                            );
                        }
                        
                        // Синхронизируем изменения с клиентом
                        PlayerSkillComponent.KEY.sync(player);
                        ProfessionComponent.KEY.sync(player);
                    }
                });
            });

        // Примечание: Обработчики ACTIVATE_GLOBAL_SKILL и ACTIVATE_ACTIVE_SKILL 
        // теперь находятся в SkillActivationHandler.register()
    }
    

}