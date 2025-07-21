package net.xach.testmod;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class YandexGoInventoryScreen extends InventoryScreen {
    private static final ResourceLocation EXTRA_SLOTS_TEXTURE = new ResourceLocation(TestMod.MOD_ID, "textures/gui/yandex_go_inventory.png");

    public YandexGoInventoryScreen(Player player) {
        super(player);
        this.imageWidth = 176 + 60; // Расширяем ширину для дополнительных слотов
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // Рисуем обычный инвентарь
        super.renderBg(guiGraphics, partialTick, mouseX, mouseY);

        // Рисуем дополнительные слоты для yandex.go
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.getCapability(TestMod.PlayerClassCapability.CAPABILITY).ifPresent(cap -> {
                if (cap.getPlayerClass().equals("yandex.go")) {
                    int skillLevel = cap.getSkillLevel("inventory_slots_basic");
                    renderExtraSlots(guiGraphics, skillLevel);
                }
            });
        }
    }

    private void renderExtraSlots(GuiGraphics guiGraphics, int skillLevel) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = this.leftPos + 176; // Справа от обычного инвентаря
        int y = this.topPos + 7;

        // Рисуем фон для дополнительных слотов
        guiGraphics.fill(x, y, x + 54, y + 76, 0xC0101010);
        guiGraphics.fill(x + 1, y + 1, x + 53, y + 75, 0x80555555);

        // Рисуем слоты
        for (int i = 0; i < 3; i++) {
            int slotX = x + 18;
            int slotY = y + 8 + i * 20;

            if (i < skillLevel) {
                // Разблокированный слот (зеленый)
                guiGraphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF006600);
                guiGraphics.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0xFF004400);
            } else {
                // Заблокированный слот (серый)
                guiGraphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF666666);
                guiGraphics.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0xFF333333);

                // Рисуем замок (используем символ X вместо эмодзи)
                guiGraphics.drawString(minecraft.font, "X", slotX + 6, slotY + 5, 0xFFFFFFFF);
            }
        }

        // Заголовок
        guiGraphics.drawString(minecraft.font, "Доп.", x + 5, y - 10, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Обрабатываем клики по дополнительным слотам
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.getCapability(TestMod.PlayerClassCapability.CAPABILITY).ifPresent(cap -> {
                if (cap.getPlayerClass().equals("yandex.go")) {
                    int skillLevel = cap.getSkillLevel("inventory_slots_basic");
                    handleExtraSlotClick(mouseX, mouseY, button, skillLevel);
                }
            });
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void handleExtraSlotClick(double mouseX, double mouseY, int button, int skillLevel) {
        int x = this.leftPos + 176;
        int y = this.topPos + 7;

        for (int i = 0; i < 3; i++) {
            int slotX = x + 18;
            int slotY = y + 8 + i * 20;

            if (mouseX >= slotX && mouseX < slotX + 18 && mouseY >= slotY && mouseY < slotY + 18) {
                if (i < skillLevel) {
                    // Слот разблокирован - можно взаимодействовать
                    if (minecraft != null && minecraft.player != null) {
                        minecraft.player.sendSystemMessage(Component.literal("Клик по дополнительному слоту " + (i + 1)));
                    }
                } else {
                    // Слот заблокирован
                    if (minecraft != null && minecraft.player != null) {
                        minecraft.player.sendSystemMessage(Component.literal("Слот заблокирован! Нужен уровень " + (i + 1) + " навыка inventory_slots_basic"));
                    }
                }
                break;
            }
        }
    }
}
