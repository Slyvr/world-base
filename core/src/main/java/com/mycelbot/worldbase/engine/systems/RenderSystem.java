package com.mycelbot.worldbase.engine.systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.mycelbot.worldbase.engine.components.AppearanceComponent;
import com.mycelbot.worldbase.engine.components.PositionComponent;
import com.mycelbot.worldbase.engine.ecs.Entity;
import com.mycelbot.worldbase.engine.ecs.EntityManager;
import com.mycelbot.worldbase.util.SpriteSheetLoader;

/**
 * System that renders entities with Position + Appearance components.
 * Uses the spritesheet to get textures and draws only visible tiles.
 */
public class RenderSystem {

    private static final int TILE_SIZE = 32;

    private final SpriteBatch batch;
    private final OrthographicCamera camera;
    private final SpriteSheetLoader spritesheet;
    private final EntityManager entityManager;

    public RenderSystem(EntityManager entityManager, SpriteSheetLoader spritesheet) {
        this.batch = new SpriteBatch();
        this.camera = new OrthographicCamera();
        this.spritesheet = spritesheet;
        this.entityManager = entityManager;
    }

    /** Render all entities that have Position + Appearance components. */
    public void render(float offsetX, float offsetY, float zoom) {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        camera.setToOrtho(false, w, h);
        batch.setProjectionMatrix(camera.combined);
        batch.setColor(1f, 1f, 1f, 1f);

        batch.begin();
        drawVisibleEntities(offsetX, offsetY, zoom);
        batch.end();
    }

    private void drawVisibleEntities(float offsetX, float offsetY, float zoom) {
        // Viewport bounds in tile space
        float viewLeft = -offsetX / (TILE_SIZE * zoom);
        float viewTop  = -offsetY / (TILE_SIZE * zoom);
        float viewRight  = viewLeft + Gdx.graphics.getWidth()  / (TILE_SIZE * zoom);
        float viewBottom = viewTop  + Gdx.graphics.getHeight() / (TILE_SIZE * zoom);

        for (Entity entity : entityManager.getAllEntitiesWith(PositionComponent.class, AppearanceComponent.class)) {
            PositionComponent pos = entityManager.getComponent(entity, PositionComponent.class);
            AppearanceComponent app = entityManager.getComponent(entity, AppearanceComponent.class);
            if (!app.visible) continue;

            // Culling: skip tiles outside the viewport
            if (pos.x < (int) Math.floor(viewLeft)  - 1 || pos.x > (int) Math.ceil(viewRight)) continue;
            if (pos.y < (int) Math.floor(viewTop)   - 1 || pos.y > (int) Math.ceil(viewBottom)) continue;

            TextureRegion region = spritesheet.getRegion(app.spriteId);
            if (region == null) continue;

            float drawX = pos.x * TILE_SIZE * zoom + offsetX;
            float drawY = pos.y * TILE_SIZE * zoom + offsetY;
            batch.draw(region, drawX, drawY, TILE_SIZE * zoom, TILE_SIZE * zoom);
        }
    }

    public void dispose() {
        batch.dispose();
    }
}
