package com.mycelbot.worldbase.engine;

import com.mycelbot.worldbase.config.GameConfig;
import com.mycelbot.worldbase.engine.ecs.EntityManager;
import com.mycelbot.worldbase.engine.systems.WorldGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * The game world — a grid of tile entities managed via EntityManager.
 * <p>
 * World size, generator strategy, and other global settings come from
 * a GameConfig so they can be tuned without recompiling.
 * <p>
 * After generation, island metadata (size, centroid) is available
 * via {@link #getIslands()}.
 */
public class World {

    private final int width;
    private final int height;
    private final EntityManager entityManager;
    private final List<IslandInfo> islands;

    public World(GameConfig config, WorldGenerator generator) {
        this.width  = config.getWorldWidth();
        this.height = config.getWorldHeight();
        this.entityManager = new EntityManager();
        generator.generate(entityManager, width, height);
        this.islands = new ArrayList<>(generator.getIslands());
    }

    public int getWidth()  { return width; }
    public int getHeight() { return height; }
    public EntityManager getEntityManager() { return entityManager; }
    public List<IslandInfo> getIslands() { return islands; }
}
