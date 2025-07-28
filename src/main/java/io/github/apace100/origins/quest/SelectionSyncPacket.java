package io.github.apace100.origins.quest;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

/**
 * Packet for synchronizing quest selection state between server and client
 */
public class SelectionSyncPacket {
    public static final Identifier ID = new Identifier("origins", "selection_sync");
    
    private final int selectedIndex;
    private final String selectedQuestId;
    
    public SelectionSyncPacket(int selectedIndex, String selectedQuestId) {
        this.selectedIndex = selectedIndex;
        this.selectedQuestId = selectedQuestId;
    }
    
    public SelectionSyncPacket(int selectedIndex) {
        this(selectedIndex, null);
    }
    
    /**
     * Creates a packet from selection state
     */
    public static SelectionSyncPacket create(int selectedIndex, String selectedQuestId) {
        return new SelectionSyncPacket(selectedIndex, selectedQuestId);
    }
    
    /**
     * Writes the packet data to the buffer
     */
    public PacketByteBuf toPacketByteBuf() {
        PacketByteBuf buf = PacketByteBufs.create();
        
        buf.writeInt(selectedIndex);
        buf.writeBoolean(selectedQuestId != null);
        if (selectedQuestId != null) {
            buf.writeString(selectedQuestId);
        }
        
        return buf;
    }
    
    /**
     * Reads the packet data from the buffer
     */
    public static SelectionSyncPacket fromPacketByteBuf(PacketByteBuf buf) {
        int selectedIndex = buf.readInt();
        String selectedQuestId = null;
        
        if (buf.readBoolean()) {
            selectedQuestId = buf.readString();
        }
        
        return new SelectionSyncPacket(selectedIndex, selectedQuestId);
    }
    
    /**
     * Gets the selected quest index
     */
    public int getSelectedIndex() {
        return selectedIndex;
    }
    
    /**
     * Gets the selected quest ID
     */
    public String getSelectedQuestId() {
        return selectedQuestId;
    }
}