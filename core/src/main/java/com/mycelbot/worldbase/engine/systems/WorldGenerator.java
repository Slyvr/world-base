package com.mycelbot.worldbase.engine.systems;

import com.mycelbot.worldbase.engine.ecs.EntityManager;

/**
 * Abstract base for world generators.
 * <p>
 * Each generator defines how tile entities are created and placed
 * in the EntityManager. Multiple generator implementations let us
 * swap world types (island, continent,洞穴, etc.) without changing
 * the core World class.
 */
public abstract class WorldGenerator {

    /** Populate the EntityManager with tile entities for a world of the given size. */
    public abstract void generate(EntityManager entityManager, int width, int height);
}
