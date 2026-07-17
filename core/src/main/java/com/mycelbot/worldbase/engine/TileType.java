package com.mycelbot.worldbase.engine;

/**
 * Defines tile types available in the game world.
 * Each type maps to a sprite ID from the terrain spritesheet.
 * Interior tiles (fully covered on all 8 sides) have variants
 * used for randomisation during world generation.
 */
public enum TileType {
    GRASS(289, "Grass", new int[]{289, 352, 353, 354}),
    DIRT_LIGHT(112, "Dirt (Light)"),
    DIRT_DARK(115, "Dirt (Dark)"),
    DIRT_LIGHT_HOLE(121, "Dirt Light Hole"),
    DIRT_DARK_HOLE(301, "Dirt Dark Hole"),
    STONE(307, "Stone"),
    STONE_HOLE(124, "Stone Hole"),
    LAVA(304, "Lava"),
    WATER(124, "Water", new int[]{124, 187, 188, 189});

    private final int spriteId;
    private final String displayName;
    private final int[] interiorVariants;

    TileType(int spriteId, String displayName) {
        this.spriteId = spriteId;
        this.displayName = displayName;
        this.interiorVariants = new int[]{spriteId};
    }

    TileType(int spriteId, String displayName, int[] interiorVariants) {
        this.spriteId = spriteId;
        this.displayName = displayName;
        this.interiorVariants = interiorVariants;
    }

    public int getSpriteId() {
        return spriteId;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the interior variants for this tile type.
     * For types with no variants, returns a single-element array
     * containing the default sprite ID.
     */
    public int[] getInteriorVariants() {
        return interiorVariants;
    }
}
