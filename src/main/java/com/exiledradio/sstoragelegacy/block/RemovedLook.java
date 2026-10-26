/*
 * Written for Sophisticated Storage Legacy by ExiledRadio, GPLv3.
 */
package com.exiledradio.sstoragelegacy.block;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Client side only: the look (synchronized data) of storage tiles removed in the last few
 * seconds, for a new tile of the same kind at the same spot to take over. The server's own
 * packets still win whenever they arrive. Only touched from the client thread.
 */
final class RemovedLook {
    /** Fancy Block Particles' place animation takes well under a second; slow settings, a few. */
    private static final long KEEP_MS = 5000;
    private static final Map<BlockPos, RemovedLook> LOOKS = new HashMap<>();

    /** Weak, so a world left behind isn't kept in memory. */
    private final WeakReference<World> world;
    private final Class<?> tileClass;
    private final NBTTagCompound look;
    private final long removedAt;

    private RemovedLook(World world, Class<?> tileClass, NBTTagCompound look, long removedAt) {
        this.world = new WeakReference<>(world);
        this.tileClass = tileClass;
        this.look = look;
        this.removedAt = removedAt;
    }

    static void remember(World world, BlockPos pos, Class<?> tileClass, NBTTagCompound look) {
        long now = System.currentTimeMillis();
        for (Iterator<RemovedLook> it = LOOKS.values().iterator(); it.hasNext(); ) {
            RemovedLook removed = it.next();
            if (now - removed.removedAt > KEEP_MS || removed.world.get() != world) {
                it.remove();
            }
        }
        LOOKS.put(pos.toImmutable(), new RemovedLook(world, tileClass, look, now));
    }

    @Nullable
    static NBTTagCompound take(World world, BlockPos pos, Class<?> tileClass) {
        if (LOOKS.isEmpty()) {
            return null;
        }
        RemovedLook removed = LOOKS.remove(pos);
        if (removed == null || removed.world.get() != world || removed.tileClass != tileClass
                || System.currentTimeMillis() - removed.removedAt > KEEP_MS) {
            return null;
        }
        return removed.look;
    }
}
