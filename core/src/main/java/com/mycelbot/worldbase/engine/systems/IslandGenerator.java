package com.mycelbot.worldbase.engine.systems;

import com.mycelbot.worldbase.engine.TileType;
import com.mycelbot.worldbase.engine.components.AppearanceComponent;
import com.mycelbot.worldbase.engine.components.PositionComponent;
import com.mycelbot.worldbase.engine.components.TileComponent;
import com.mycelbot.worldbase.engine.ecs.EntityManager;

/**
 * Generates a world of all water with a 100x100 grass island at the center.
 * <p>
 * The island spans from (centerX - 50, centerY - 50) to (centerX + 49, centerY + 49),
 * creating a square of green surrounded by blue water tiles.
 */
public class IslandGenerator extends WorldGenerator {

    private static final int ISLAND_SIZE = 100;
    private static final int HALF_ISLAND = ISLAND_SIZE / 2;

    @Override
    public void generate(EntityManager em, int width, int height) {
        int centerX = width / 2;
        int centerY = height / 2;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                var entity = em.createEntity();
                em.addComponent(entity, new PositionComponent(x, y));

                boolean onIsland = Math.abs(x - centerX) < HALF_ISLAND
                                && Math.abs(y - centerY) < HALF_ISLAND;
                TileType type = onIsland ? TileType.GRASS : TileType.WATER;

                em.addComponent(entity, new TileComponent(type));
                em.addComponent(entity, new AppearanceComponent(type.getSpriteId()));
            }
        }
    }
}
