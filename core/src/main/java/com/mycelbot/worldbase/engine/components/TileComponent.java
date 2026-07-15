package com.mycelbot.worldbase.engine.components;

import com.mycelbot.worldbase.engine.TileType;
import com.mycelbot.worldbase.engine.ecs.Component;

/**
 * Marks an entity as a ground tile with a specific terrain type.
 */
public class TileComponent implements Component {
    public TileType type;

    public TileComponent(TileType type) {
        this.type = type;
    }
}
