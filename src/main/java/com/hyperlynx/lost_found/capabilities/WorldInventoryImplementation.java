package com.hyperlynx.lost_found.capabilities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;

public class WorldInventoryImplementation implements IWorldInventory{
    private final ArrayDeque<ItemStack> world_item_queue = new ArrayDeque<>();

    @Override
    public void addItem(ItemStack i) {
        world_item_queue.add(i);
    }

    @Override
    public @Nullable ItemStack popItem() {
        return world_item_queue.poll();
    }

    @Override
    public ItemStack peekItem() {
        return world_item_queue.peek();
    }

    @Override
    public void reset() {
        world_item_queue.clear();
    }

    @Override
    public CompoundTag serializeNBT() {
        ListTag item_list_tag = new ListTag();
        int n = world_item_queue.size();
        for(int i = 0; i < n; i++){
            ItemStack stack = world_item_queue.remove();
            CompoundTag tag = new CompoundTag();
            tag = stack.save(tag);
            item_list_tag.add(tag);
            world_item_queue.add(stack);
        }
        CompoundTag ret_tag = new CompoundTag();
        ret_tag.put("Items", item_list_tag);
        return ret_tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        ListTag item_list_tag = nbt.getList("Items", Tag.TAG_COMPOUND);
        for(int i = 0; i < item_list_tag.size(); i++){
            world_item_queue.add(ItemStack.of(item_list_tag.getCompound(i)));
        }
    }
}
