package com.mycelbot.worldbase.engine;

/**
 * Represents a single tile in the game world — a cell on the 2D grid
 * with a terrain type.
 */
public class Tile {

    private TileType type;

    public Tile() {
        this(TileType.GRASS);
    }

    public Tile(TileType type) {
        this.type = type;
    }

    public TileType getType() {
        return type;
    }

    public void setType(TileType type) {
        this.type = type;
    }
}
