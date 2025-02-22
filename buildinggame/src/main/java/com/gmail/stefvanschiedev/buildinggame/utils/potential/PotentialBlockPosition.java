package com.gmail.stefvanschiedev.buildinggame.utils.potential;

import com.gmail.stefvanschiedev.buildinggame.utils.ChunkCoordinates;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * A block position that may or may not exist at this moment. The block position may not exist if the world in which it
 * resides is unloaded or deleted. If the world exists and is loaded, then this block position also exists.
 *
 * @since 9.0.1
 */
public class PotentialBlockPosition {

    /**
     * Supplies the world of this block position. While the supplier is always existent, the actual returned world may
     * not be.
     */
    @NotNull
    private final Supplier<World> worldSupplier;

    /**
     * The coordinates of the location.
     */
    private final int x, y, z;

    /**
     * Creates a new potential block position.
     *
     * @param worldSupplier the supplier for the world
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @since 9.0.1
     */
    public PotentialBlockPosition(@NotNull Supplier<World> worldSupplier, int x, int y, int z) {
        this.worldSupplier = worldSupplier;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Gets the block at this position or null if the block doesn't exist.
     *
     * @return the block or null
     * @since 9.0.1
     */
    @Nullable
    public Block getBlock() {
        World world = worldSupplier.get();

        if (world == null) {
            return null;
        }

        return world.getBlockAt(x, y, z);
    }

    /**
     * Gets whether this position is loaded. This position is loaded iff. the world in which it resides exists and is
     * loaded and the chunk this position is in is also loaded.
     *
     * @return whether this position is loaded.
     * @since 10.0.3
     */
    @Contract(pure = true)
    public boolean isLoaded() {
        World world = worldSupplier.get();

        if (world == null) {
            return false;
        }

        return world.isChunkLoaded(x >> 4, z >> 4);
    }

    /**
     * Gets a pair of chunk coordinates in which this block position is contained.
     *
     * @return chunk coordinates of this position
     * @since 10.0.3
     */
    @NotNull
    @Contract(pure = true)
    public ChunkCoordinates getChunkCoordinates() {
        return new ChunkCoordinates(x >> 4, z >> 4);
    }
}
