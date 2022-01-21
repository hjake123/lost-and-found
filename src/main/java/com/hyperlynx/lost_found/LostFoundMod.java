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
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;
import java.util.Random;

@Mod(LostFoundMod.MODID)
public class LostFoundMod
{
    private static final Logger LOGGER = LogManager.getLogger();
    public static final String MODID = "lost_found";

    private static final ResourceLocation WORLD_INVENTORY_KEY = new ResourceLocation(MODID, "world_inv");

    private int despawn_count = 0;

    public LostFoundMod() {
        MinecraftForge.EVENT_BUS.register(this);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ConfigMan.commonSpec);
    }

    @SubscribeEvent
    public void onACE(AttachCapabilitiesEvent<Level> event){
        WorldInventory WI = new WorldInventory(ConfigMan.COMMON.worldInventorySize.get());

        if(!event.getObject().isClientSide() && event.getObject().dimension().equals(Level.OVERWORLD)) {
            event.addCapability(WORLD_INVENTORY_KEY, WI);
            LOGGER.info("Dimension is " + event.getObject().dimension());
        }
    }

    private boolean checkItemImportant(ItemStack itemStack){
        return (itemStack.hasCustomHoverName() && ConfigMan.COMMON.protectNamedItems.get())
                || (itemStack.isEnchanted() && ConfigMan.COMMON.protectEnchantedItems.get())
                /*|| (itemStack.getItem().getTags().contains(Tags.Blocks.)) && ConfigMan.COMMON.protectRareItems.get()*/;
        // TODO: Add rare item tag for things that should always be saved.
    }

    @SubscribeEvent
    public void onEntityDespawn(ItemExpireEvent event) {
        ItemStack caught_item = event.getEntityItem().getItem();
        degradeItem(caught_item, event.getEntityItem().level.random);

        // If there is a World Inventory, if it's important, capture the item into it.
        if(event.getEntityItem().level.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).isPresent() && checkItemImportant(caught_item)) {
            Optional<IItemHandler> world_hand = event.getEntityItem().level.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).resolve();
            if (world_hand.orElseThrow().getSlots() <= despawn_count) {
                despawn_count = 0; // loop back and start replacing stacks already occupied.
            }
                world_hand.orElseThrow().extractItem(despawn_count, 100, false); // remove anything already in the slot
                world_hand.orElseThrow().insertItem(despawn_count, caught_item, false);
        }
        despawn_count++;
    }

    private void degradeItem(ItemStack in, Random r){
        // Hurts damageable items as a penalty for losing them.
        if (in.isDamageableItem() && r.nextFloat() < ConfigMan.COMMON.itemDamagedChance.get()) {
                in.hurt(r.nextInt(0, (int)((in.getMaxDamage() - in.getDamageValue()) * ConfigMan.COMMON.itemDamageMultiplier.get())), r, null);
        }
    }

    @SubscribeEvent
    public void onItemFished(ItemFishedEvent event){
        //Chance for any lost item fishing result to be rolled.
        if(event.getEntity().getLevel().getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).isPresent()) {
            if (event.getEntity().getLevel().random.nextFloat() < ConfigMan.COMMON.itemFishChance.get()) {
                Optional<IItemHandler> world_hand = event.getEntity().getLevel().getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).resolve();
                LOGGER.info("There were " +world_hand.orElseThrow().getSlots() + " slots when we found that item.");
                int slot = -1;

                //Scan the world inventory for lost items.
                for (int i = 0; i < world_hand.orElseThrow().getSlots(); i++) {
                    if (world_hand.orElseThrow().getStackInSlot(i) != ItemStack.EMPTY) {
                        slot = i;
                        break;
                    }
                }
                if (slot >= 0) {
                    ItemStack found_item = world_hand.orElseThrow().extractItem(slot, 100, false);

                    // Code ripped from vanilla fishing hook, spawns the item and launches it at the player like normal fishing.
                    ItemEntity found_item_entity = new ItemEntity(event.getEntity().level, event.getHookEntity().getX(), event.getHookEntity().getY(), event.getHookEntity().getZ(), found_item);
                    double d0 = event.getPlayer().getX() - event.getHookEntity().getX();
                    double d1 = event.getPlayer().getY() - event.getHookEntity().getY();
                    double d2 = event.getPlayer().getZ() - event.getHookEntity().getZ();
                    found_item_entity.setDeltaMovement(d0 * 0.1D, d1 * 0.1D + Math.sqrt(Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2)) * 0.08D, d2 * 0.1D);
                    event.getEntity().getLevel().addFreshEntity(found_item_entity);

                    // Prevent normal fishing loot from appearing alongside the item.
                    event.setCanceled(true);
                }
            }
        }
    }
}
