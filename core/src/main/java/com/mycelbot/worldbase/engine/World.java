package com.mycelbot.worldbase.engine;

import com.mycelbot.worldbase.engine.components.AppearanceComponent;
import com.mycelbot.worldbase.engine.components.PositionComponent;
import com.mycelbot.worldbase.engine.components.TileComponent;
import com.mycelbot.worldbase.engine.ecs.EntityManager;

/**
 * The game world — manages tile entities via an EntityManager.
 * <p>
 * On creation, populates the grid with tile entities, each carrying
 * PositionComponent, AppearanceComponent, and TileComponent.
 * Future entities (players, items, etc.) can be added dynamically.
 */
public class World {

    private final int width;
    private final int height;
    private final EntityManager entityManager;

    public World(int width, int height) {
        this.width = width;
        this.height = height;
        this.entityManager = new EntityManager();
        populateTiles(TileType.GRASS);
    }

    /** Fill the grid with tile entities of the given type. */
    private void populateTiles(TileType type) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                var entity = entityManager.createEntity();
                entityManager.addComponent(entity, new PositionComponent(x, y));
                entityManager.addComponent(entity, new TileComponent(type));
                entityManager.addComponent(entity, new AppearanceComponent(type.getSpriteId()));
            }
        }
    }

    public int getWidth()  { return width; }
    public int getHeight() { return height; }
    public EntityManager getEntityManager() { return entityManager; }
}
