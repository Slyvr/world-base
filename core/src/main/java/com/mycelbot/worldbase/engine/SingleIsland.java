package com.mycelbot.worldbase.engine;

/**
 * A single island produced by {@link com.mycelbot.worldbase.engine.systems.SingleIslandGenerator}.
 * <p>
 * Contains the grass tile grid in local bounding-box coordinates, plus
 * the bounding box dimensions and total tile count. The island has already
 * been smoothed (perpendicular-edge rule) and trimmed to its extent.
 */
public class SingleIsland {

    /** Grass tiles in local bounding-box coordinates. true = grass. */
    public final boolean[][] tiles;

    /** Bounding box width. */
    public final int width;

    /** Bounding box height. */
    public final int height;

    /** Total number of grass tiles in this island. */
    public final int tileCount;

    public SingleIsland(boolean[][] tiles, int width, int height, int tileCount) {
        this.tiles = tiles;
        this.width = width;
        this.height = height;
        this.tileCount = tileCount;
    }
}
