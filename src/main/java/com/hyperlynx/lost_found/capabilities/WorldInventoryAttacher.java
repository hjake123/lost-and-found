package com.hyperlynx.lost_found.capabilities;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.hyperlynx.lost_found.LostFoundMod.MODID;

public class WorldInventoryAttacher { // Code adapted from the Forge wiki (https://forge.gemwire.uk/wiki/Capabilities)

    private static class WorldInventoryProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
        public static final ResourceLocation IDENTIFIER = new ResourceLocation(MODID, "world_inv");

        private final IWorldInventory wi = new WorldInventoryImplementation();
        private final LazyOptional<IWorldInventory> lowi = LazyOptional.of(() -> wi);

        @NotNull
        @Override
        public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return WorldInventory.INSTANCE.orEmpty(cap, this.lowi);
        }

        void invalidate() {
            this.lowi.invalidate();
        }

        @Override
        public CompoundTag serializeNBT() {
            return this.wi.serializeNBT();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            this.wi.deserializeNBT(nbt);
        }
    }

    public static void attach(final AttachCapabilitiesEvent<Level> event) {
        final WorldInventoryProvider provider = new WorldInventoryProvider();
        event.addCapability(WorldInventoryProvider.IDENTIFIER, provider);
    }

}
