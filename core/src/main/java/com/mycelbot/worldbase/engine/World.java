package com.mycelbot.worldbase.engine;

import com.mycelbot.worldbase.engine.ecs.EntityManager;
import com.mycelbot.worldbase.engine.systems.WorldGenerator;

/**
 * The game world — a grid of tile entities managed via EntityManager.
 * <p>
 * Accepts a WorldGenerator to populate the grid, keeping the generation
 * strategy pluggable and the World class decoupled from tile creation logic.
 */
public class World {

    private final int width;
    private final int height;
    private final EntityManager entityManager;

    public World(int width, int height, WorldGenerator generator) {
        this.width = width;
        this.height = height;
        this.entityManager = new EntityManager();
        generator.generate(entityManager, width, height);
    }

    public int getWidth()  { return width; }
    public int getHeight() { return height; }
    public EntityManager getEntityManager() { return entityManager; }
}
