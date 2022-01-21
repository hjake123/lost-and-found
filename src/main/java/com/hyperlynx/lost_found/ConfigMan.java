package com.hyperlynx.lost_found;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class ConfigMan {
    public static class Common {
        ForgeConfigSpec.BooleanValue protectNamedItems;
        ForgeConfigSpec.BooleanValue protectEnchantedItems;
        ForgeConfigSpec.BooleanValue protectRareItems;
        ForgeConfigSpec.DoubleValue itemFishChance;
        ForgeConfigSpec.DoubleValue itemDamagedChance;
        ForgeConfigSpec.DoubleValue itemDamageMultiplier;
        ForgeConfigSpec.IntValue worldInventorySize;

        Common(ForgeConfigSpec.Builder builder){
            builder.comment("Item Protection Settings")
                    .push("Protection");
            protectNamedItems = builder.comment("Catch items with custom names. [Default: true]")
                    .define("protectNamedItems", true);
            protectEnchantedItems = builder.comment("Catch items that are enchanted. [Default: true]")
                    .define("protectEnchantedItems", true);
            protectRareItems = builder.comment("Catch items that have the tag 'lost_found:RareItem'. [Default: true]")
                    .define("protectRareItems", true);
            builder.pop();

            builder.comment("Recovery Settings")
                    .push("Recovery");
            itemFishChance = builder.comment("Chance that fishing will recover a lost item instead of getting normal loot, if any lost items are stored. [Default: 0.25]")
                    .defineInRange("itemFishChance", 0.25, 0, 1);
            itemDamagedChance = builder.comment("Chance that lost items with durability will be damaged. [Default: 0.7]")
                    .defineInRange("itemDamagedChance", 0.7, 0, 1);
            itemDamageMultiplier = builder.comment("Proportion of a lost item's durability to be removed in the worst case. [Default: 0.5]")
                    .defineInRange("itemDamageMultiplier", 0.5, 0, 0.99);
            worldInventorySize = builder.comment("Number of items to be preserved in total. Excessively large values might cause lag when fishing. [Default: 20]")
                            .defineInRange("worldInventorySize", 20, 1, Integer.MAX_VALUE);
            builder.pop();
        }
    }

    public static final ForgeConfigSpec commonSpec;
    public static final Common COMMON;

    static {
        final Pair<Common, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Common::new);
        commonSpec = specPair.getRight();
        COMMON = specPair.getLeft();
    }

}
