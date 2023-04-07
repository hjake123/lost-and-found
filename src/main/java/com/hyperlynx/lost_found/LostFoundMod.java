package com.hyperlynx.lost_found;

import com.hyperlynx.lost_found.capabilities.WorldInventory;
import com.hyperlynx.lost_found.capabilities.WorldInventoryAttacher;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.item.ItemExpireEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Random;

@Mod(LostFoundMod.MODID)
public class LostFoundMod
{
    private static final Logger LOGGER = LogManager.getLogger();
    public static final String MODID = "lost_found";

    public LostFoundMod() {
        MinecraftForge.EVENT_BUS.register(this);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ConfigMan.commonSpec);
    }

    @SubscribeEvent
    public void onCommandRegister(RegisterCommandsEvent event){
        LostFishedCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onACE(AttachCapabilitiesEvent<Level> event){
        WorldInventoryAttacher.attach(event);
    }

    private boolean checkItemImportant(ItemStack itemStack){
        TagKey<Item> mustSaveTag = TagKey.create(Registry.ITEM_REGISTRY, new ResourceLocation("lost_found:must_save"));
        TagKey<Item> noSaveTag = TagKey.create(Registry.ITEM_REGISTRY, new ResourceLocation("lost_found:no_save"));
        return (itemStack.hasCustomHoverName() && ConfigMan.COMMON.protectNamedItems.get()
                || itemStack.isEnchanted() && ConfigMan.COMMON.protectEnchantedItems.get()
                || itemStack.is(mustSaveTag)) && !itemStack.is(noSaveTag);
    }

    @SubscribeEvent
    public void onEntityDespawn(ItemExpireEvent event) {
        ItemStack caught_item = event.getEntityItem().getItem();
        degradeItem(caught_item, event.getEntityItem().level.random);

        // If there is a World Inventory, if it's important, capture the item into it.
        if(event.getEntityItem().level.getCapability(WorldInventory.INSTANCE).isPresent() && checkItemImportant(caught_item)) {
            event.getEntityItem().level.getCapability(WorldInventory.INSTANCE).resolve().orElseThrow().addItem(caught_item);
        }
    }

    private void degradeItem(ItemStack stack, Random r){
        // Hurts damageable items as a penalty for losing them.
        if (stack.isDamageableItem() && r.nextFloat() < ConfigMan.COMMON.itemDamagedChance.get()) {
            if(stack.getMaxDamage() - stack.getDamageValue() == 0)
                return;
            stack.hurt(r.nextInt(0, (int)((stack.getMaxDamage() - stack.getDamageValue()) * ConfigMan.COMMON.itemDamageMultiplier.get())), r, null);
        }
    }

    @SubscribeEvent
    public void onItemFished(ItemFishedEvent event){
        //Chance for any lost item fishing result to be rolled.
        if(event.getEntity().getLevel().getCapability(WorldInventory.INSTANCE).isPresent()) {
            if (event.getEntity().getLevel().random.nextFloat() < ConfigMan.COMMON.itemFishChance.get()) {
                ItemStack found_item = event.getEntity().getLevel().getCapability(WorldInventory.INSTANCE).resolve().orElseThrow().popItem();

                if(found_item != null) {
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
