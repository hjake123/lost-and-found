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
import java.util.Random;

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
                if (world_hand.orElseThrow().getSlots() <= despawn_count) {
                    despawn_count = 0; // loop back and start replacing stacks already occupied.
                }
                    ItemStack caught_item = event.getEntityItem().getItem();
                    degradeItem(caught_item, event.getEntityItem().level.random);
                    world_hand.orElseThrow().extractItem(despawn_count, 100, false); // remove anything already in the slot
                    world_hand.orElseThrow().insertItem(despawn_count, caught_item, false);
                }
            }
        despawn_count++;
    }

    private void degradeItem(ItemStack in, Random r){ // Makes the itemstack worse somehow, as a penalty for losing it.
        // TODO: Should be configurable!
        if(r.nextFloat() < 0.4) {
            if (in.getCount() > 1) {
                in.setCount(in.getCount() - (int) Math.floor(r.nextFloat() * in.getCount()));
            } else if (in.isDamageableItem()) {
                in.hurt(r.nextInt(1, (in.getMaxDamage() - in.getDamageValue() - 20)), r, null);
            }
        }
    }

    @SubscribeEvent
    public void onItemFished(ItemFishedEvent event){
        //Chance for any lost item fishing result to be rolled. TODO: Should be configurable!
        double FISH_ITEM_CHANCE = 0.25;
        if(event.getEntity().getLevel().random.nextFloat() < FISH_ITEM_CHANCE){
            Optional<IItemHandler> world_hand = event.getEntity().getLevel().getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).resolve();
            int slot = -1;
            for(int i = 0; i < world_hand.orElseThrow().getSlots(); i++){
                if(world_hand.orElseThrow().getStackInSlot(i) != ItemStack.EMPTY){
                    slot = i;
                    break;
                }
            }
            if(slot >= 0) {
                ItemStack found_item = world_hand.orElseThrow().extractItem(slot, 100, false);
                // Code ripped from vanilla fishing hook.
                ItemEntity found_item_entity = new ItemEntity(event.getEntity().level, event.getHookEntity().getX(), event.getHookEntity().getY(), event.getHookEntity().getZ(), found_item);
                double d0 = event.getPlayer().getX() - event.getHookEntity().getX();
                double d1 = event.getPlayer().getY() - event.getHookEntity().getY();
                double d2 = event.getPlayer().getZ() - event.getHookEntity().getZ();
                double d3 = 0.1D;
                found_item_entity.setDeltaMovement(d0 * 0.1D, d1 * 0.1D + Math.sqrt(Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2)) * 0.08D, d2 * 0.1D);
                event.getEntity().getLevel().addFreshEntity(found_item_entity);
                // Prevent normal fishing loot from appearing alongside the item.
                event.setCanceled(true);
            }
        }
    }
}
