package com.mycelbot.worldbase.engine.components;

import com.mycelbot.worldbase.engine.ecs.Component;

/**
 * Grid position of an entity in tile coordinates.
 */
public class PositionComponent implements Component {
    public int x;
    public int y;

    public PositionComponent(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
