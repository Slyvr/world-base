package com.mycelbot.worldbase.engine.components;

import com.mycelbot.worldbase.engine.ecs.Component;

/**
 * Visual appearance of an entity on screen.
 */
public class AppearanceComponent implements Component {
    public int spriteId;
    public boolean visible;

    public AppearanceComponent(int spriteId) {
        this.spriteId = spriteId;
        this.visible = true;
    }
}
