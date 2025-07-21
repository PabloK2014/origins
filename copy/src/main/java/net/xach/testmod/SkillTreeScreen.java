package net.xach.testmod;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class SkillTreeScreen extends AbstractContainerScreen<SkillTreeMenu> {
    private static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation(TestMod.MOD_ID, "textures/gui/custom_skill_tree.png");
    private static final ResourceLocation TABS_TEXTURE = new ResourceLocation("minecraft", "textures/gui/advancements/tabs.png");
    private final List<SkillTab> tabs = new ArrayList<>();
    private SkillTab selectedTab;
    private int minX = Integer.MAX_VALUE;
    private int maxX = Integer.MIN_VALUE;
    private int minY = Integer.MAX_VALUE;
    private int maxY = Integer.MIN_VALUE;

    public SkillTreeScreen(SkillTreeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 256;
        this.imageHeight = 256;
    }

    @Override
    protected void init() {
        super.init();
        tabs.clear();
        int previousTabIndex = selectedTab != null ? selectedTab.index : 0;
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.getCapability(TestMod.PlayerClassCapability.CAPABILITY).ifPresent(cap -> {
                String playerClass = cap.getPlayerClass();
                SkillTreeHandler.SkillTree skillTree = SkillTreeHandler.CLASS_SKILL_TREES.get(playerClass);
                if (skillTree != null) {
                    if (playerClass.equals("yandex.go")) {
                        tabs.add(new SkillTab(0, Component.literal("Скорость"), skillTree.getBranches().get(0), cap));
                        tabs.add(new SkillTab(1, Component.literal("Инвентарь"), skillTree.getBranches().get(1), cap));
                        tabs.add(new SkillTab(2, Component.literal("Грузоподъёмность"), skillTree.getBranches().get(2), cap));
                    } else if (playerClass.equals("war")) {
                        tabs.add(new SkillTab(0, Component.literal("Берсерк"), skillTree.getBranches().get(0), cap));
                        tabs.add(new SkillTab(1, Component.literal("Защитник"), skillTree.getBranches().get(1), cap));
                        tabs.add(new SkillTab(2, Component.literal("Таджик"), skillTree.getBranches().get(2), cap));
                    } else if (playerClass.equals("pivo")) {
                        tabs.add(new SkillTab(0, Component.literal("Мастер ферментации"), skillTree.getBranches().get(0), cap));
                        tabs.add(new SkillTab(1, Component.literal("Пьяный мастер"), skillTree.getBranches().get(1), cap));
                        tabs.add(new SkillTab(2, Component.literal("Вечеринка в таверне"), skillTree.getBranches().get(2), cap));
                    } else if (playerClass.equals("cook")) {
                        tabs.add(new SkillTab(0, Component.literal("Гурман"), skillTree.getBranches().get(0), cap));
                        tabs.add(new SkillTab(1, Component.literal("Огненная кухня"), skillTree.getBranches().get(1), cap));
                        tabs.add(new SkillTab(2, Component.literal("Ресторанный критик"), skillTree.getBranches().get(2), cap));
                    } else if (playerClass.equals("miner")) {
                        tabs.add(new SkillTab(0, Component.literal("Добыча"), skillTree.getBranches().get(0), cap));
                        tabs.add(new SkillTab(1, Component.literal("Исследование"), skillTree.getBranches().get(1), cap));
                        tabs.add(new SkillTab(2, Component.literal("Выживание"), skillTree.getBranches().get(2), cap));
                    } else if (playerClass.equals("smith")) {
                        tabs.add(new SkillTab(0, Component.literal("Крафт"), skillTree.getBranches().get(0), cap));
                        tabs.add(new SkillTab(1, Component.literal("Ремонт"), skillTree.getBranches().get(1), cap));
                        tabs.add(new SkillTab(2, Component.literal("Огненное мастерство"), skillTree.getBranches().get(2), cap));
                    }
                    selectedTab = tabs.get(Math.min(previousTabIndex, tabs.size() - 1));
                    updateBounds();
                    updateTabButtons();
                }
            });
        }
    }

    private void updateBounds() {
        minX = Integer.MAX_VALUE;
        maxX = Integer.MIN_VALUE;
        minY = Integer.MAX_VALUE;
        maxY = Integer.MIN_VALUE;
        for (SkillTab tab : tabs) {
            for (SkillNode node : tab.nodes) {
                minX = Math.min(minX, node.x);
                maxX = Math.max(maxX, node.x + 20);
                minY = Math.min(minY, node.y);
                maxY = Math.max(maxY, node.y + 20);
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        try {
            RenderSystem.setShaderTexture(0, BACKGROUND_TEXTURE);
            int x = (this.width - this.imageWidth) / 2;
            int y = (this.height - this.imageHeight) / 2;
            guiGraphics.blit(BACKGROUND_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
        } catch (Exception e) {
        }

        if (selectedTab != null) {
            int offsetX = (this.width - imageWidth) / 2;
            int offsetY = (this.height - imageHeight) / 2 + 30;

            for (SkillNode node : selectedTab.nodes) {
                if (node.parent != null) {
                    int startX = offsetX + node.parent.x + 10;
                    int startY = offsetY + node.parent.y + 10;
                    int endX = offsetX + node.x + 10;
                    int endY = offsetY + node.y + 10;
                    guiGraphics.fill(startX, startY, endX, endY, 0xFF00FF00);
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        this.renderBg(guiGraphics, partialTicks, mouseX, mouseY);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        int baseTabX = (this.width - this.imageWidth) / 2;
        int tabY = (this.height - imageHeight) / 2 + 10;
        RenderSystem.setShaderTexture(0, TABS_TEXTURE);
        int[] tabXs = {baseTabX + 30, baseTabX + this.imageWidth / 2 - 14, baseTabX + this.imageWidth - 58};
        for (int i = 0; i < tabs.size(); i++) {
            SkillTab tab = tabs.get(i);
            int tabX = tabXs[i];
            int u = tab == selectedTab ? 0 : 28;
            guiGraphics.blit(TABS_TEXTURE, tabX, tabY, u, 0, 28, 32);
            guiGraphics.drawCenteredString(this.font, tab.title, tabX + 14, tabY + 8, 0xFFFFFF);
        }

        if (selectedTab != null) {
            int offsetX = (this.width - imageWidth) / 2;
            int offsetY = (this.height - imageHeight) / 2 + 30;
            boolean isCarryingCapacity = selectedTab.index == 2;
            for (SkillNode node : selectedTab.nodes) {
                int textX = isCarryingCapacity
                        ? offsetX + node.x - this.font.width(node.skillText) - 4
                        : offsetX + node.x + 24;
                int textY = offsetY + node.y + 6;
                int color = node.button.isLocked() ? 0x888888 : 0xFFFFFF;
                guiGraphics.drawString(this.font, node.skillText, textX, textY, color);
            }
        }

        if (minecraft != null && minecraft.player != null) {
            minecraft.player.getCapability(TestMod.PlayerClassCapability.CAPABILITY).ifPresent(cap -> {
                int textX = (this.width - imageWidth) / 2 + 10;
                int textY = (this.height - imageHeight) / 2 + 50;
                guiGraphics.drawString(this.font, "Уровень: " + cap.getLevel(), textX, textY, 0xFFFFFF);
                guiGraphics.drawString(this.font, "Опыт: " + cap.getExperience() + "/" + (cap.getLevel() * 100), textX, textY + 10, 0xFFFFFF);
                guiGraphics.drawString(this.font, "Очки навыков: " + cap.getSkillPoints(), textX, textY + 20, 0xFFFFFF);
                guiGraphics.drawString(this.font, "Энергия: " + cap.getSurgeEnergy() + "/100", textX, textY + 30, 0xFFFFFF);
            });
        }

        if (selectedTab != null) {
            for (SkillNode node : selectedTab.nodes) {
                if (node.button.isMouseOver(mouseX, mouseY)) {
                    guiGraphics.renderTooltip(font, Component.literal(node.skill.getDescription()), mouseX, mouseY);
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int offsetX = (this.width - imageWidth) / 2;
        int offsetY = (this.height - imageHeight) / 2 + 30;

        // Проверка клика по вкладкам
        int baseTabX = (this.width - this.imageWidth) / 2;
        int tabY = (this.height - imageHeight) / 2 + 10;
        int[] tabXs = {baseTabX + 30, baseTabX + this.imageWidth / 2 - 14, baseTabX + this.imageWidth - 58};
        for (int i = 0; i < tabs.size(); i++) {
            int tabX = tabXs[i];
            if (mouseX >= tabX && mouseX < tabX + 28 && mouseY >= tabY && mouseY < tabY + 32) {
                selectedTab = tabs.get(i);
                updateTabButtons();
                return true;
            }
        }

        // Проверка клика по тексту
        if (selectedTab != null) {
            boolean isCarryingCapacity = selectedTab.index == 2;
            for (SkillNode node : selectedTab.nodes) {
                int textX = isCarryingCapacity
                        ? offsetX + node.x - this.font.width(node.skillText) - 4
                        : offsetX + node.x + 24;
                int textY = offsetY + node.y + 6;
                if (mouseX >= textX && mouseX < textX + this.font.width(node.skillText) &&
                        mouseY >= textY && mouseY < textY + this.font.lineHeight) {
                    return true;
                }
            }
        }

        // Проверка клика по кнопкам навыков
        if (selectedTab != null) {
            for (SkillNode node : selectedTab.nodes) {
                if (node.button.isMouseOver(mouseX, mouseY)) {
                    node.button.mouseClicked(mouseX, mouseY, button);
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    private void updateTabButtons() {
        this.renderables.removeIf(widget -> widget instanceof CustomSkillButton);
        if (selectedTab != null) {
            int offsetX = (this.width - imageWidth) / 2;
            int offsetY = (this.height - imageHeight) / 2 + 30;
            for (SkillNode node : selectedTab.nodes) {
                node.button.setX(offsetX + node.x);
                node.button.setY(offsetY + node.y);
                this.addRenderableWidget(node.button);
            }
        }
    }

    private class SkillTab {
        private final int index;
        private final Component title;
        private final List<SkillNode> nodes = new ArrayList<>();

        public SkillTab(int index, Component title, List<SkillTreeHandler.Skill> skills, TestMod.PlayerClassCapability cap) {
            this.index = index;
            this.title = title;
            int baseX;
            switch (index) {
                case 0: baseX = 30; break;
                case 1: baseX = 256 / 2 - 10; break;
                case 2: baseX = 256 - 50; break;
                default: baseX = 30;
            }
            int currentY = 60;
            for (SkillTreeHandler.Skill skill : skills) {
                SkillNode parent = nodes.stream()
                        .filter(n -> n.skill.getId().equals(skill.getParentId()))
                        .findFirst()
                        .orElse(null);
                int y = currentY;
                boolean isLocked = cap.getLevel() < skill.getRequiredLevel() ||
                        cap.getSkillPoints() <= 0 ||
                        cap.getSkillLevel(skill.getId()) >= skill.getMaxLevel() ||
                        (skill.getParentId() != null && cap.getSkillLevel(skill.getParentId()) == 0);
                Component skillText = Component.literal(skill.getName() + " (" + cap.getSkillLevel(skill.getId()) + "/" + skill.getMaxLevel() + ")");
                CustomSkillButton button = new CustomSkillButton(
                        baseX, y, 20, 20,
                        Component.empty(),
                        () -> upgradeSkill(skill.getId()),
                        isLocked
                );
                nodes.add(new SkillNode(skill, button, skillText, baseX, y, parent));
                addRenderableWidget(button);
                currentY += 30;
            }
        }
    }

    private class SkillNode {
        private final SkillTreeHandler.Skill skill;
        private final CustomSkillButton button;
        private final Component skillText;
        private final int x;
        private final int y;
        private final SkillNode parent;

        public SkillNode(SkillTreeHandler.Skill skill, CustomSkillButton button, Component skillText, int x, int y, SkillNode parent) {
            this.skill = skill;
            this.button = button;
            this.skillText = skillText;
            this.x = x;
            this.y = y;
            this.parent = parent;
        }
    }

    private void upgradeSkill(String skillId) {
        if (minecraft != null && minecraft.player != null) {
            TestMod.NETWORK.sendToServer(new SkillUpgradePacket(skillId));
            init();
            updateTabButtons();
        }
    }
}
