package io.github.apace100.origins.quest;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Set;

/**
 * Packet for synchronizing quest mask state between server and client
 */
public class MaskSyncPacket {
    public static final Identifier ID = new Identifier("origins", "mask_sync");
    
    private final Set<Integer> maskedSlots;
    
    public MaskSyncPacket(Set<Integer> maskedSlots) {
        this.maskedSlots = maskedSlots != null ? new HashSet<>(maskedSlots) : new HashSet<>();
    }
    
    /**
     * Creates a packet from masked slots set
     */
    public static MaskSyncPacket create(Set<Integer> maskedSlots) {
        return new MaskSyncPacket(maskedSlots);
    }
    
    /**
     * Writes the packet data to the buffer
     */
    public PacketByteBuf toPacketByteBuf() {
        PacketByteBuf buf = PacketByteBufs.create();
        
        // Write number of masked slots
        buf.writeInt(maskedSlots.size());
        
        // Write each masked slot index
        for (Integer slotIndex : maskedSlots) {
            buf.writeInt(slotIndex);
        }
        
        return buf;
    }
    
    /**
     * Reads the packet data from the buffer
     */
    public static MaskSyncPacket fromPacketByteBuf(PacketByteBuf buf) {
        Set<Integer> maskedSlots = new HashSet<>();
        
        // Read number of masked slots
        int slotCount = buf.readInt();
        
        // Read each masked slot index
        for (int i = 0; i < slotCount; i++) {
            maskedSlots.add(buf.readInt());
        }
        
        return new MaskSyncPacket(maskedSlots);
    }
    
    /**
     * Gets the set of masked slot indices
     */
    public Set<Integer> getMaskedSlots() {
        return new HashSet<>(maskedSlots);
    }
}