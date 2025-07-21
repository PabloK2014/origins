package net.xach.testmod;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.xach.testmod.block.TestModBlocks;
import net.xach.testmod.entity.ModEntities;
import net.xach.testmod.items.TestModItems;
import net.xach.testmod.worldgen.ModBiomeModifiers;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.TickEvent;

@Mod(TestMod.MOD_ID)
public class TestMod {
    public static final String MOD_ID = "testmod";
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel NETWORK = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public TestMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(this::clientSetup);
        bus.addListener(this::registerCapabilities);
        bus.addListener(this::setup);

        MenuRegistry.register(bus);
        TestModBlocks.BLOCKS.register(bus);
        TestModItems.ITEMS.register(bus);
        ModEntities.register(bus);
        ModBiomeModifiers.BIOME_MODIFIER_SERIALIZERS.register(bus);
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(WarSkillHandler.class);
        MinecraftForge.EVENT_BUS.register(PivoSkillHandler.class);
        MinecraftForge.EVENT_BUS.register(CookSkillHandler.class);
        MinecraftForge.EVENT_BUS.register(SmithSkillHandler.class);

        registerNetworkPackets();

        System.out.println("TestMod constructor completed, entities should be registered");
    }

    private void registerNetworkPackets() {
        int id = 0;
        NETWORK.registerMessage(id++, ClassSelectionPacket.class, ClassSelectionPacket::toBytes, ClassSelectionPacket::new, ClassSelectionPacket::handle);
        NETWORK.registerMessage(id++, PlayerDataSyncPacket.class, PlayerDataSyncPacket::toBytes, PlayerDataSyncPacket::new, PlayerDataSyncPacket::handle);
        NETWORK.registerMessage(id++, OpenSkillTreePacket.class, OpenSkillTreePacket::toBytes, OpenSkillTreePacket::new, OpenSkillTreePacket::handle);
        NETWORK.registerMessage(id++, ActiveSkillSelectionPacket.class, ActiveSkillSelectionPacket::toBytes, ActiveSkillSelectionPacket::new, ActiveSkillSelectionPacket::handle);
        NETWORK.registerMessage(id++, OpenClassSelectionPacket.class, OpenClassSelectionPacket::toBytes, OpenClassSelectionPacket::new, OpenClassSelectionPacket::handle);
        NETWORK.registerMessage(id++, SkillActivationPacket.class, SkillActivationPacket::toBytes, SkillActivationPacket::new, SkillActivationPacket::handle);
        NETWORK.registerMessage(id++, SkillUpgradePacket.class, SkillUpgradePacket::toBytes, SkillUpgradePacket::new, SkillUpgradePacket::handle);
        NETWORK.registerMessage(id++, VillageCompassPacket.class, VillageCompassPacket::toBytes, VillageCompassPacket::new, VillageCompassPacket::handle);
        NETWORK.registerMessage(id++, ToggleAreaMiningPacket.class, ToggleAreaMiningPacket::toBytes, ToggleAreaMiningPacket::new, ToggleAreaMiningPacket::handle);
        NETWORK.registerMessage(id++, OreHighlightPacket.class, OreHighlightPacket::toBytes, OreHighlightPacket::new, OreHighlightPacket::handle);
    }

    private void setup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            System.out.println("Common setup completed for " + MOD_ID);
        });
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(MenuRegistry.CLASS_SELECTION.get(), ClassSelectionScreen::new);
            MenuScreens.register(MenuRegistry.SKILL_TREE.get(), SkillTreeScreen::new);
            MenuScreens.register(MenuRegistry.ACTIVE_SKILL_SELECTION.get(), ActiveSkillSelectionScreen::new);
            MenuScreens.register(MenuRegistry.FOOD_BAG.get(), FoodBagScreen::new);
            System.out.println("Client setup completed for " + MOD_ID);
        });
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(PlayerClassCapability.class);
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!event.getEntity().level().isClientSide()) {
            Player player = event.getEntity();
            player.getCapability(PlayerClassCapability.CAPABILITY).ifPresent(cap -> {
                if (cap.getPlayerClass().isEmpty()) {
                    player.sendSystemMessage(Component.literal("Используйте команду /testmod class для выбора класса"));
                } else {
                    cap.sync((ServerPlayer) player);
                }
            });
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.getEntity().level().isClientSide()) {
            Player original = event.getOriginal();
            Player newPlayer = event.getEntity();
            System.out.println("Player clone event triggered for player: " + newPlayer.getName().getString());
            original.reviveCaps();
            original.getCapability(PlayerClassCapability.CAPABILITY).ifPresent(oldCap -> {
                newPlayer.getCapability(PlayerClassCapability.CAPABILITY).ifPresent(newCap -> {
                    CompoundTag nbt = oldCap.serializeNBT();
                    System.out.println("Cloning player capability: class=" + oldCap.getPlayerClass() + ", level=" + oldCap.getLevel() + ", exp=" + oldCap.getExperience() + ", points=" + oldCap.getSkillPoints() + ", active skill: " + oldCap.getActiveSkill());
                    newCap.deserializeNBT(nbt);
                    newCap.sync((ServerPlayer) newPlayer);
                    System.out.println("Cloned to new player: class=" + newCap.getPlayerClass() + ", level=" + newCap.getLevel() + ", exp=" + newCap.getExperience() + ", points=" + newCap.getSkillPoints() + ", active skill: " + newCap.getActiveSkill());
                });
            });
            original.invalidateCaps();
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!event.getEntity().level().isClientSide()) {
            ServerPlayer player = (ServerPlayer) event.getEntity();
            player.getCapability(PlayerClassCapability.CAPABILITY).ifPresent(cap -> {
                System.out.println("Player respawned with class: " + cap.getPlayerClass() + ", level: " + cap.getLevel() + ", exp: " + cap.getExperience() + ", points: " + cap.getSkillPoints() + ", active skill: " + cap.getActiveSkill());
                cap.sync(player);
            });
        }
    }

    @SubscribeEvent
    public void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(new ResourceLocation(MOD_ID, "player_class"), new PlayerClassCapability.Provider());
        }
    }

    @SubscribeEvent
    public void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(TestModItems.STRAWBERRY);
            event.accept(TestModItems.STRAWBERRY_SEEDS);
            event.accept(TestModItems.PEPPER_SPRAY);
            event.accept(TestModItems.TRAP_PLACER);
            event.accept(TestModItems.FOOD_BAG);
            event.accept(TestModBlocks.TRAP_BLOCK);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
    }

    public static class PlayerClassCapability implements INBTSerializable<CompoundTag> {
        public static final Capability<PlayerClassCapability> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});
        private boolean hasTradeHandler = false;
        private boolean hasCraftingHandler = false;
        private boolean hasTrapHandler = false;
        private String playerClass = "";
        private int level = 1;
        private int experience = 0;
        private int skillPoints = 1;
        private int surgeEnergy = 100;
        private String activeSkill = "";
        private final Map<String, Integer> skillLevels = new HashMap<>();

        public int getSurgeEnergy() {
            return surgeEnergy;
        }

        public boolean hasTradeHandler() {
            return hasTradeHandler;
        }

        public void setTradeHandler(boolean value) {
            hasTradeHandler = value;
        }

        public boolean hasCraftingHandler() {
            return hasCraftingHandler;
        }

        public void setCraftingHandler(boolean value) {
            hasCraftingHandler = value;
        }

        public boolean hasTrapHandler() {
            return hasTrapHandler;
        }

        public void setTrapHandler(boolean value) {
            hasTrapHandler = value;
        }

        public void setSurgeEnergy(int surgeEnergy) {
            this.surgeEnergy = Math.max(0, Math.min(100, surgeEnergy));
        }

        public void useSurgeEnergy(int amount) {
            setSurgeEnergy(surgeEnergy - amount);
        }

        public void addSurgeEnergy(int amount) {
            setSurgeEnergy(surgeEnergy + amount);
        }

        public String getPlayerClass() {
            return playerClass;
        }

        public void setPlayerClass(String playerClass) {
            this.playerClass = playerClass != null ? playerClass : "";
        }

        public int getLevel() {
            return level;
        }

        public void setLevel(int level) {
            this.level = level;
            this.skillPoints = level;
        }

        public int getExperience() {
            return experience;
        }

        public void setExperience(int experience) {
            this.experience = experience;
        }

        public void addExperience(int amount) {
            this.experience += amount;
            while (this.experience >= getExperienceForNextLevel()) {
                this.experience -= getExperienceForNextLevel();
                this.level++;
                this.skillPoints++;
            }
        }

        private int getExperienceForNextLevel() {
            return 100 * level;
        }

        public int getSkillPoints() {
            return skillPoints;
        }

        public void setSkillPoints(int skillPoints) {
            this.skillPoints = skillPoints;
        }

        public void spendSkillPoint() {
            if (skillPoints > 0) {
                skillPoints--;
            }
        }

        public int getSkillLevel(String skillId) {
            return skillLevels.getOrDefault(skillId, 0);
        }

        public void setSkillLevel(String skillId, int level) {
            skillLevels.put(skillId, level);
        }

        public void clearSkillLevels() {
            skillLevels.clear();
        }

        public String getActiveSkill() {
            return activeSkill;
        }

        public void setActiveSkill(String skillId) {
            this.activeSkill = skillId != null ? skillId : "";
        }

        public void sync(ServerPlayer player) {
            NETWORK.sendTo(
                    new PlayerDataSyncPacket(playerClass, level, experience, skillLevels, skillPoints, surgeEnergy, activeSkill),
                    player.connection.connection,
                    NetworkDirection.PLAY_TO_CLIENT
            );
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putString("playerClass", playerClass);
            tag.putInt("level", level);
            tag.putInt("experience", experience);
            tag.putInt("skillPoints", skillPoints);
            tag.putInt("surgeEnergy", surgeEnergy);
            tag.putString("activeSkill", activeSkill);
            CompoundTag skillsTag = new CompoundTag();
            skillLevels.forEach(skillsTag::putInt);
            tag.put("skills", skillsTag);
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            playerClass = tag.getString("playerClass");
            level = tag.getInt("level");
            experience = tag.getInt("experience");
            skillPoints = tag.getInt("skillPoints");
            surgeEnergy = tag.getInt("surgeEnergy");
            activeSkill = tag.getString("activeSkill");
            CompoundTag skillsTag = tag.getCompound("skills");
            skillLevels.clear();
            skillsTag.getAllKeys().forEach(key -> skillLevels.put(key, skillsTag.getInt(key)));
        }

        public static class Provider implements net.minecraftforge.common.capabilities.ICapabilitySerializable<CompoundTag> {
            private final PlayerClassCapability instance = new PlayerClassCapability();
            private final LazyOptional<PlayerClassCapability> optional = LazyOptional.of(() -> instance);

            @Override
            public <T> LazyOptional<T> getCapability(Capability<T> cap, net.minecraft.core.Direction side) {
                return cap == CAPABILITY ? optional.cast() : LazyOptional.empty();
            }

            @Override
            public CompoundTag serializeNBT() {
                return instance.serializeNBT();
            }

            @Override
            public void deserializeNBT(CompoundTag nbt) {
                instance.deserializeNBT(nbt);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class ClassSelectionScreen extends AbstractContainerScreen<ClassSelectionMenu> {
        public ClassSelectionScreen(ClassSelectionMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
        }

        @Override
        protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        }

        @Override
        protected void init() {
            super.init();
            int buttonWidth = 100;
            int buttonHeight = 20;
            int centerX = this.width / 2;
            int centerY = this.height / 2;

            this.addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
                            Component.literal("Воин"),
                            button -> selectClass("war"))
                    .pos(centerX - buttonWidth / 2, centerY - buttonHeight / 2 - 40)
                    .size(buttonWidth, buttonHeight)
                    .build());

            this.addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
                            Component.literal("Повар"),
                            button -> selectClass("cook"))
                    .pos(centerX - buttonWidth / 2, centerY - buttonHeight / 2 - 20)
                    .size(buttonWidth, buttonHeight)
                    .build());

            this.addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
                            Component.literal("Курьер Yandex.Go"),
                            button -> selectClass("yandex.go"))
                    .pos(centerX - buttonWidth / 2, centerY - buttonHeight / 2)
                    .size(buttonWidth, buttonHeight)
                    .build());

            this.addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
                            Component.literal("Пивовар"),
                            button -> selectClass("pivo"))
                    .pos(centerX - buttonWidth / 2, centerY - buttonHeight / 2 + 20)
                    .size(buttonWidth, buttonHeight)
                    .build());

            this.addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
                            Component.literal("Шахтёр"),
                            button -> selectClass("miner"))
                    .pos(centerX - buttonWidth / 2, centerY - buttonHeight / 2 + 40)
                    .size(buttonWidth, buttonHeight)
                    .build());

            this.addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
                            Component.literal("Кузнец"),
                            button -> selectClass("smith"))
                    .pos(centerX - buttonWidth / 2, centerY - buttonHeight / 2 + 60)
                    .size(buttonWidth, buttonHeight)
                    .build());
        }

        private void selectClass(String className) {
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.getCapability(PlayerClassCapability.CAPABILITY).ifPresent(cap -> {
                    cap.setPlayerClass(className);
                    this.minecraft.setScreen(null);
                    NETWORK.sendToServer(new ClassSelectionPacket(className));
                });
            }
        }

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            this.renderBackground(guiGraphics);
            super.render(guiGraphics, mouseX, mouseY, partialTicks);
            guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        }

        @Override
        public boolean isPauseScreen() {
            return true;
        }
    }
}
