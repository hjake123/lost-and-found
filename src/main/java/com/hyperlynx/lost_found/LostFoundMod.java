package com.hyperlynx.lost_found;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.item.ItemExpireEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
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
            event.addCapability(WORLD_INVENTORY_KEY, wi);
        }
    }

    @SubscribeEvent
    public void onEntityDespawn(ItemExpireEvent event) {
        if(event.getEntityItem().level.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).isPresent()) {
            if (event.getEntityItem().getItem().hasCustomHoverName()) {
                Optional<IItemHandler> world_hand = event.getEntityItem().level.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).resolve();
                LOGGER.info("Just caught an item despawn. Slot count: " + world_hand.orElseThrow().getSlots());
                if (world_hand.orElseThrow().getSlots() <= despawn_count) { //TODO: Might not work.
                    despawn_count = 0; // loop back and start replacing stacks already occupied.
                }
                    world_hand.orElseThrow().extractItem(despawn_count, 100, false); // remove anything already in the slot
                    world_hand.orElseThrow().insertItem(despawn_count, event.getEntityItem().getItem(), false);
                }
            }
        despawn_count++;
    }

    @SubscribeEvent
    public void onItemFished(ItemFishedEvent event){
        //Chance for any lost item fishing result to be rolled. TODO: Should be configurable!
        double FISH_ITEM_CHANCE = 1;
        if(event.getEntity().getLevel().random.nextFloat() < FISH_ITEM_CHANCE){
            Optional<IItemHandler> world_hand = event.getEntity().getLevel().getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).resolve();
            int slot = -1;
            LOGGER.info("Slot count: " + world_hand.orElseThrow().getSlots());
            for(int i = 0; i < world_hand.orElseThrow().getSlots(); i++){
                LOGGER.info("Iterating to find item! i = " + i);
                if(world_hand.orElseThrow().getStackInSlot(i) != ItemStack.EMPTY){
                    LOGGER.info("Found!");
                    slot = i;
                    break;
                }
            }
            ItemStack found_item = world_hand.orElseThrow().extractItem(slot, 100, false);
            ItemEntity found_item_entity = new ItemEntity(event.getEntity().level, event.getHookEntity().getX(), event.getHookEntity().getY(), event.getHookEntity().getZ(), found_item);
            event.getEntity().getLevel().addFreshEntity(found_item_entity);
            if(slot >= 0) {
                event.setCanceled(true); // Prevent normal fishing loot from appearing alongside the item.
            }
        }
    }
}
