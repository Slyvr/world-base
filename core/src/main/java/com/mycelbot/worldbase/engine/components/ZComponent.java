package com.mycelbot.worldbase.engine.components;

import com.mycelbot.worldbase.engine.ecs.Component;

/**
 * Render layer / Z-depth of an entity.
 * <p>
 * Lower values are drawn first (behind). Standard layers:
 * <pre>
 *   0 — water / ground
 *   1 — terrain features (grass, dirt, stone)
 *   2 — props / decorations
 *   3 — entities (player, NPCs)
 *   4 — effects / overlays
 * </pre>
 */
public class ZComponent implements Component {
    public int layer;

    public ZComponent(int layer) {
        this.layer = layer;
    }
}
