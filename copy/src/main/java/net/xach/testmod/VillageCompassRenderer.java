package net.xach.testmod;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TestMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class VillageCompassRenderer {
    private static int villageX = 0;
    private static int villageZ = 0;
    private static boolean hasVillage = false;
    private static long lastUpdate = 0;

    public static void setVillagePosition(int x, int z) {
        villageX = x;
        villageZ = z;
        hasVillage = true;
        lastUpdate = System.currentTimeMillis();
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        // Убираем компас через 5 секунд без обновлений
        if (System.currentTimeMillis() - lastUpdate > 5000) {
            hasVillage = false;
            return;
        }

        if (!hasVillage) return;

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) return;

        // Проверяем, есть ли у игрока скилл shulker_carry
        player.getCapability(TestMod.PlayerClassCapability.CAPABILITY).ifPresent(cap -> {
            if (!cap.getPlayerClass().equals("yandex.go") || cap.getSkillLevel("shulker_carry") == 0) {
                return;
            }

            BlockPos playerPos = player.blockPosition();
            double deltaX = villageX - playerPos.getX();
            double deltaZ = villageZ - playerPos.getZ();
            double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

            // Если игрок ближе 40 блоков, компас исчезает
            if (distance < 40) {
                hasVillage = false;
                return;
            }

            GuiGraphics guiGraphics = event.getGuiGraphics();
            PoseStack poseStack = guiGraphics.pose();

            int screenWidth = minecraft.getWindow().getGuiScaledWidth();

            // Размеры полоски компаса
            int barWidth = 200;
            int barHeight = 8;
            int barX = (screenWidth - barWidth) / 2;
            int barY = 10;

            // ИСПРАВЛЕНО: Правильный расчет направления
            // Угол к деревне от игрока
            double angleToVillage = Math.atan2(deltaZ, deltaX);
            // Угол поворота игрока (в радианах, инвертируем Y для правильного направления)
            double playerYaw = Math.toRadians(-player.getYRot());
            // Относительный угол (куда нужно повернуться от текущего направления)
            double relativeAngle = angleToVillage - playerYaw;

            // Нормализуем угол к диапазону -PI до PI
            while (relativeAngle > Math.PI) relativeAngle -= 2 * Math.PI;
            while (relativeAngle < -Math.PI) relativeAngle += 2 * Math.PI;

            // Преобразуем угол в позицию на полоске
            // -PI (слева) до PI (справа), центр = 0
            double normalizedAngle = (relativeAngle + Math.PI) / (2 * Math.PI);
            int ballX = barX + (int)(normalizedAngle * barWidth);

            // Ограничиваем позицию шарика границами полоски
            ballX = Math.max(barX + 4, Math.min(ballX, barX + barWidth - 4));

            poseStack.pushPose();

            // Рисуем фон полоски
            guiGraphics.fill(barX - 2, barY - 2, barX + barWidth + 2, barY + barHeight + 2, 0x80000000);
            guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0x80333333);

            // Цвет шарика зависит от расстояния
            int ballColor = distance < 50 ? 0xFF00FF00 : distance < 100 ? 0xFFFFFF00 : 0xFFFF0000;

            // Рисуем шарик (круг)
            int ballRadius = 4;
            guiGraphics.fill(ballX - ballRadius, barY - 1, ballX + ballRadius, barY + barHeight + 1, ballColor);

            // Рисуем центральную линию для ориентира (прямо)
            guiGraphics.fill(barX + barWidth / 2 - 1, barY, barX + barWidth / 2 + 1, barY + barHeight, 0xFFFFFFFF);

            // Добавляем метки направлений
            // L (лево)
            guiGraphics.drawString(minecraft.font, "L", barX - 8, barY, 0xFFFFFFFF);
            // R (право)
            guiGraphics.drawString(minecraft.font, "R", barX + barWidth + 2, barY, 0xFFFFFFFF);

            // Текст с расстоянием под полоской
            String distanceText = String.format("Деревня: %.0fm", distance);
            int textWidth = minecraft.font.width(distanceText);
            guiGraphics.drawString(minecraft.font, distanceText, (screenWidth - textWidth) / 2, barY + barHeight + 5, 0xFFFFFFFF);

            poseStack.popPose();
        });
    }
}
