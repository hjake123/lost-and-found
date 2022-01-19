package com.hyperlynx.lost_found;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.item.ItemExpireEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(LostFoundMod.MODID)
public class LostFoundMod
{
    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();
    public static final String MODID = "lost_found";

    private static final ResourceLocation WORLD_INVENTORY_KEY = new ResourceLocation(MODID, "world_inv");
    WorldInventory wi = new WorldInventory();

    private int despawn_count = 0;

    public LostFoundMod() {
        // Register the setup method for modloading
        //FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    /*private void setup(final FMLCommonSetupEvent event)
    {
        // some pre-init code
    }*/

    @SubscribeEvent
    public void onACE(AttachCapabilitiesEvent<Level> event){
        if(!event.getObject().isClientSide()) {
            LOGGER.info("Tried to attach capability to the level...");
            LOGGER.info("Level details:" + event.getObject().toString());
            LOGGER.info("Cap count before: " + event.getCapabilities().size());
            event.addCapability(WORLD_INVENTORY_KEY, wi);
            LOGGER.info("Cap count after: " + event.getCapabilities().size());
        }
    }

    @SubscribeEvent
    public void onEntityDespawn(ItemExpireEvent event) {
        LOGGER.info("An item despawned! That's despawn number " + despawn_count);
        LOGGER.info("Item data: " + event.getEntityItem().getItem().toString());
        if(event.getEntityItem().level.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).isPresent()) {
            if (event.getEntityItem().getItem().hasCustomHoverName()) {
                LOGGER.info("Since it has a name, we should try to save the item!");
                Optional<IItemHandler> world_hand = event.getEntityItem().level.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).resolve();
                if (world_hand.orElseThrow().getSlots() >= despawn_count) {
                    despawn_count = 0; // loop back and start replacing stacks already occupied.
                }
                    world_hand.orElseThrow().extractItem(despawn_count, 100, false); // remove anything already in the slot
                    world_hand.orElseThrow().insertItem(despawn_count, event.getEntityItem().getItem(), false);
                    LOGGER.info("Slot contents:" + world_hand.orElseThrow().getStackInSlot(despawn_count));
                }
            }
        despawn_count++;
    }
}
