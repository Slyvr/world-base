package com.mycelbot.worldbase.engine;

/**
 * Defines tile types available in the game world.
 * Each type maps to a sprite ID from the base_out_atlas spritesheet.
 */
public enum TileType {
    GRASS(118, "Grass"),
    DIRT_LIGHT(112, "Dirt (Light)"),
    DIRT_DARK(115, "Dirt (Dark)"),
    DIRT_LIGHT_HOLE(121, "Dirt Light Hole"),
    DIRT_DARK_HOLE(301, "Dirt Dark Hole"),
    STONE(307, "Stone"),
    STONE_HOLE(124, "Stone Hole"),
    LAVA(304, "Lava"),
    WATER(470, "Water");

    private final int spriteId;
    private final String displayName;

    TileType(int spriteId, String displayName) {
        this.spriteId = spriteId;
        this.displayName = displayName;
    }

    public int getSpriteId() {
        return spriteId;
    }

    public String getDisplayName() {
        return displayName;
    }
}
