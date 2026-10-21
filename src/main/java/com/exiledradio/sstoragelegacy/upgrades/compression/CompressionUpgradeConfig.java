/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.upgrades.compression;

import net.minecraftforge.common.config.Config;

/** Compression upgrade settings (the original's CompressionUpgradeConfig). */
public class CompressionUpgradeConfig {
    @Config.Comment("Defines how many slots at a maximum compression upgrade is able to use")
    @Config.RangeInt(min = 3, max = 9)
    public int maxNumberOfSlots = 5;

    @Config.Comment({"List of items that can be decompressed by compression upgrade and their results.",
            "Item registry names are expected here in format of \"mod:itemBeingDecompressed=Nxmod:itemDecompressResult\" (N is 4 or 9);",
            "add \":meta\" after a name for a metadata variant"})
    public String[] additionalDecompressibleItems = {
            "minecraft:glowstone=4xminecraft:glowstone_dust",
            "minecraft:quartz_block=4xminecraft:quartz"
    };
}
