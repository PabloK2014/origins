package net.xach.testmod;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = TestMod.MOD_ID, value = Dist.CLIENT)
public class OreHighlightRenderer {

    // Карта подсвеченных руд для каждого игрока
    private static final Map<UUID, Set<BlockPos>> highlightedOres = new HashMap<>();
    private static final Map<UUID, Long> highlightEndTimes = new HashMap<>();

    // Список блоков руды
    private static final Set<Block> ORE_BLOCKS = Set.of(
            Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE,
            Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE,
            Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE,
            Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
            Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
            Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
            Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE,
            Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE,
            Blocks.NETHER_GOLD_ORE, Blocks.NETHER_QUARTZ_ORE,
            Blocks.ANCIENT_DEBRIS
    );

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        UUID playerId = mc.player.getUUID();
        long currentTime = System.currentTimeMillis();

        // Проверяем, активна ли подсветка
        if (!highlightEndTimes.containsKey(playerId) || currentTime > highlightEndTimes.get(playerId)) {
            highlightedOres.remove(playerId);
            highlightEndTimes.remove(playerId);
            return;
        }

        Set<BlockPos> orePositions = highlightedOres.get(playerId);
        if (orePositions == null || orePositions.isEmpty()) return;

        // Настройка рендеринга
        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = event.getCamera().getPosition();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (BlockPos pos : orePositions) {
            BlockState state = mc.level.getBlockState(pos);
            if (!ORE_BLOCKS.contains(state.getBlock())) continue;

            // Определяем цвет в зависимости от типа руды
            float[] color = getOreColor(state.getBlock());
            float alpha = 0.3f + 0.2f * (float) Math.sin(currentTime * 0.005); // Пульсирующий эффект

            // Рендерим контур блока
            renderBlockOutline(buffer, poseStack.last().pose(), pos, color[0], color[1], color[2], alpha);
        }

        tesselator.end();
        poseStack.popPose();

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void renderBlockOutline(BufferBuilder buffer, Matrix4f matrix, BlockPos pos, float r, float g, float b, float a) {
        float x1 = pos.getX();
        float y1 = pos.getY();
        float z1 = pos.getZ();
        float x2 = x1 + 1.0f;
        float y2 = y1 + 1.0f;
        float z2 = z1 + 1.0f;

        // Рендерим все 6 граней куба с прозрачностью
        // Нижняя грань
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x2, y1, z1).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x2, y1, z2).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x1, y1, z2).color(r, g, b, a).endVertex();

        // Верхняя грань
        buffer.vertex(matrix, x1, y2, z1).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x1, y2, z2).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x2, y2, z1).color(r, g, b, a).endVertex();

        // Северная грань
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x1, y2, z1).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x2, y2, z1).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x2, y1, z1).color(r, g, b, a).endVertex();

        // Южная грань
        buffer.vertex(matrix, x1, y1, z2).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x2, y1, z2).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x1, y2, z2).color(r, g, b, a).endVertex();

        // Западная грань
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x1, y1, z2).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x1, y2, z2).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x1, y2, z1).color(r, g, b, a).endVertex();

        // Восточная грань
        buffer.vertex(matrix, x2, y1, z1).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x2, y2, z1).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x2, y1, z2).color(r, g, b, a).endVertex();
    }

    private static float[] getOreColor(Block block) {
        if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE) {
            return new float[]{0.0f, 1.0f, 1.0f}; // Голубой для алмазов
        } else if (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE) {
            return new float[]{0.0f, 1.0f, 0.0f}; // Зеленый для изумрудов
        } else if (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE || block == Blocks.NETHER_GOLD_ORE) {
            return new float[]{1.0f, 1.0f, 0.0f}; // Желтый для золота
        } else if (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE) {
            return new float[]{0.8f, 0.8f, 0.8f}; // Серый для железа
        } else if (block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE) {
            return new float[]{1.0f, 0.0f, 0.0f}; // Красный для редстоуна
        } else if (block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE) {
            return new float[]{0.0f, 0.0f, 1.0f}; // Синий для лазурита
        } else if (block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE) {
            return new float[]{1.0f, 0.5f, 0.0f}; // Оранжевый для меди
        } else if (block == Blocks.ANCIENT_DEBRIS) {
            return new float[]{0.5f, 0.0f, 0.5f}; // Фиолетовый для древних обломков
        } else {
            return new float[]{1.0f, 1.0f, 1.0f}; // Белый для остальных
        }
    }

    // Методы для управления подсветкой
    public static void setHighlightedOres(UUID playerId, Set<BlockPos> orePositions, long durationMs) {
        highlightedOres.put(playerId, orePositions);
        highlightEndTimes.put(playerId, System.currentTimeMillis() + durationMs);
    }

    public static void clearHighlight(UUID playerId) {
        highlightedOres.remove(playerId);
        highlightEndTimes.remove(playerId);
    }
}
