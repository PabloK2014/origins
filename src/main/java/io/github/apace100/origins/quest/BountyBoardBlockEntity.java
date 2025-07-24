package io.github.apace100.origins.quest;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class BountyBoardBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory {

    private final List<BountyQuest> availableQuests = new ArrayList<>();

    public BountyBoardBlockEntity(BlockPos pos, BlockState state) {
        super(QuestRegistry.BOUNTY_BOARD_BLOCK_ENTITY, pos, state);
        // Нельзя вызывать generateRandomQuests() здесь — world ещё null
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("gui.origins.bounty_board.title");
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new BountyBoardScreenHandler(syncId, inv, this);
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeBlockPos(this.pos);
    }

    public List<BountyQuest> getAvailableQuests() {
        return availableQuests;
    }

    public void addQuest(BountyQuest quest) {
        if (availableQuests.size() < 6) {
            availableQuests.add(quest);
            markDirty();
        }
    }

    public void removeQuest(BountyQuest quest) {
        availableQuests.remove(quest);
        markDirty();
    }

    public void generateNewQuests() {
        if (world == null || world.isClient) return;

        availableQuests.clear();
        for (int i = 0; i < 3 + world.random.nextInt(2); i++) {
            BountyQuest quest = QuestGenerator.generateRandomQuest();
            if (quest != null) {
                availableQuests.add(quest);
            }
        }
        markDirty();
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        availableQuests.clear();
        NbtList questList = nbt.getList("AvailableQuests", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < questList.size(); i++) {
            NbtCompound questNbt = questList.getCompound(i);
            BountyQuest quest = BountyQuest.fromNbt(questNbt);
            availableQuests.add(quest);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        NbtList questList = new NbtList();
        for (BountyQuest quest : availableQuests) {
            NbtCompound questNbt = new NbtCompound();
            quest.writeToNbt(questNbt);
            questList.add(questNbt);
        }
        nbt.put("AvailableQuests", questList);
    }
}
