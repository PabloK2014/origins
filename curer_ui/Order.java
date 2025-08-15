package com.example.courier;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.nbt.NbtElement;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Order {
    public enum Status { OPEN, ACCEPTED, COMPLETED, DECLINED }

    public final UUID id;
    public final String ownerName;
    public final UUID ownerUuid;
    public final List<ItemStack> requestItems = new ArrayList<>();
    public final List<ItemStack> rewardItems = new ArrayList<>();
    public String description = "";
    public Status status = Status.OPEN;
    public String acceptedByName = null;
    public UUID acceptedByUuid = null;

    public Order(UUID id, String ownerName, UUID ownerUuid) {
        this.id = id;
        this.ownerName = ownerName;
        this.ownerUuid = ownerUuid;
    }

    public NbtCompound toNbt() {
        NbtCompound n = new NbtCompound();
        n.putUuid("id", id);
        n.putString("ownerName", ownerName);
        n.putUuid("ownerUuid", ownerUuid);
        n.putString("status", status.name());
        if (acceptedByName != null) n.putString("acceptedByName", acceptedByName);
        if (acceptedByUuid != null) n.putUuid("acceptedByUuid", acceptedByUuid);
        n.putString("description", description);

        NbtList req = new NbtList();
        for (ItemStack s : requestItems) req.add(s.writeNbt(new NbtCompound()));
        n.put("requestItems", req);

        NbtList rew = new NbtList();
        for (ItemStack s : rewardItems) rew.add(s.writeNbt(new NbtCompound()));
        n.put("rewardItems", rew);

        return n;
    }

    public static Order fromNbt(NbtCompound n) {
        UUID id = n.getUuid("id");
        String ownerName = n.getString("ownerName");
        UUID ownerUuid = n.getUuid("ownerUuid");
        Order o = new Order(id, ownerName, ownerUuid);
        o.status = Status.valueOf(n.getString("status"));
        if (n.contains("acceptedByName")) o.acceptedByName = n.getString("acceptedByName");
        if (n.contains("acceptedByUuid")) o.acceptedByUuid = n.getUuid("acceptedByUuid");
        o.description = n.getString("description");

        NbtList req = n.getList("requestItems", NbtElement.COMPOUND_TYPE);
        for (int i=0;i<req.size();i++) o.requestItems.add(ItemStack.fromNbt(req.getCompound(i)));

        NbtList rew = n.getList("rewardItems", NbtElement.COMPOUND_TYPE);
        for (int i=0;i<rew.size();i++) o.rewardItems.add(ItemStack.fromNbt(rew.getCompound(i)));

        return o;
    }
}
