package com.hyperlynx.lost_found.capabilities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.INBTSerializable;

public interface IWorldInventory extends INBTSerializable<CompoundTag> {
    void addItem(ItemStack i);
    ItemStack popItem();
    ItemStack peekItem();

    void reset();
}
