package com.mycelbot.worldbase.engine.systems;

import com.mycelbot.worldbase.engine.IslandInfo;
import com.mycelbot.worldbase.engine.ecs.EntityManager;

import java.util.Collections;
import java.util.List;

/**
 * Abstract base for world generators.
 * <p>
 * Each generator defines how tile entities are created and placed
 * in the EntityManager. Multiple generator implementations let us
 * swap world types (island, continent, etc.) without changing
 * the core World class.
 */
public abstract class WorldGenerator {

    /** Populate the EntityManager with tile entities for a world of the given size. */
    public abstract void generate(EntityManager entityManager, int width, int height);

    /**
     * Return island metadata detected during generation.
     * Default implementation returns an empty list.
     */
    public List<IslandInfo> getIslands() {
        return Collections.emptyList();
    }
}
