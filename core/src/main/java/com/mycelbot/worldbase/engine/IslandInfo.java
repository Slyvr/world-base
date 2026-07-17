package com.mycelbot.worldbase.engine;

/**
 * Holds information about a detected island (connected component of grass tiles).
 * <p>
 * Populated during world generation and stored in World so other systems
 * (rendering, gameplay, AI) can use it.
 */
public class IslandInfo {

    public final int id;
    public final int tileCount;
    public final float centroidX;
    public final float centroidY;

    public IslandInfo(int id, int tileCount, float centroidX, float centroidY) {
        this.id = id;
        this.tileCount = tileCount;
        this.centroidX = centroidX;
        this.centroidY = centroidY;
    }

    @Override
    public String toString() {
        return "IslandInfo{id=" + id + ", tiles=" + tileCount
            + ", centroid=(" + String.format("%.1f", centroidX)
            + ", " + String.format("%.1f", centroidY) + ")}";
    }
}
