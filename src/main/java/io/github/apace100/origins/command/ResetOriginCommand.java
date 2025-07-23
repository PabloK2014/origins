package io.github.apace100.origins.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.networking.ModPackets;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginLayers;
import io.github.apace100.origins.registry.ModComponents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Collection;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ResetOriginCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            literal("resetorigin")
                .requires(cs -> cs.hasPermissionLevel(2))
                .then(argument("targets", EntityArgumentType.players())
                    .executes(ResetOriginCommand::resetOrigin)
                )
                .executes(ctx -> resetOrigin(ctx, ctx.getSource().getPlayerOrThrow()))
        );
    }
    
    private static int resetOrigin(CommandContext<ServerCommandSource> context) {
        try {
            Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(context, "targets");
            for (ServerPlayerEntity player : players) {
                resetOrigin(context, player);
            }
            return players.size();
        } catch (Exception e) {
            try {
                return resetOrigin(context, context.getSource().getPlayerOrThrow());
            } catch (Exception ex) {
                return 0;
            }
        }
    }
    
    private static int resetOrigin(CommandContext<ServerCommandSource> context, ServerPlayerEntity player) {
        OriginComponent component = ModComponents.ORIGIN.get(player);
        
        // Сбрасываем все происхождения
        OriginLayers.getLayers().forEach(layer -> {
            if (layer.isEnabled()) {
                component.setOrigin(layer, Origin.EMPTY);
            }
        });
        
        component.sync();
        
        // Открываем окно выбора происхождения
        PacketByteBuf data = PacketByteBufs.create();
        data.writeBoolean(true);
        ServerPlayNetworking.send(player, ModPackets.OPEN_ORIGIN_SCREEN, data);
        
        context.getSource().sendFeedback(
            () -> Text.literal("Происхождение игрока " + player.getName().getString() + " сброшено"), 
            true
        );
        
        return 1;
    }
}