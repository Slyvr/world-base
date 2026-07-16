package com.mycelbot.worldbase.engine.systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.mycelbot.worldbase.config.GameConfig;
import com.mycelbot.worldbase.engine.components.AppearanceComponent;
import com.mycelbot.worldbase.engine.components.PositionComponent;
import com.mycelbot.worldbase.engine.components.ZComponent;
import com.mycelbot.worldbase.engine.ecs.Entity;
import com.mycelbot.worldbase.engine.ecs.EntityManager;
import com.mycelbot.worldbase.util.SpriteSheetLoader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * System that renders entities with Position + Appearance + Z components.
 * Entities are sorted by Z (ascending) so lower layers draw first.
 * Only tiles within the viewport are drawn.
 * <p>
 * Tile size is pulled from GameConfig.
 */
public class RenderSystem {

    private final int tileSize;

    private final SpriteBatch batch;
    private final OrthographicCamera camera;
    private final SpriteSheetLoader spritesheet;
    private final EntityManager entityManager;

    public RenderSystem(EntityManager entityManager, SpriteSheetLoader spritesheet, GameConfig config) {
        this.tileSize = config.getTileSize();
        this.batch = new SpriteBatch();
        this.camera = new OrthographicCamera();
        this.spritesheet = spritesheet;
        this.entityManager = entityManager;
    }

    /** Render all entities that have Position + Appearance + Z components. */
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
        float viewLeft  = -offsetX / (tileSize * zoom);
        float viewTop   = -offsetY / (tileSize * zoom);
        float viewRight = viewLeft + Gdx.graphics.getWidth()  / (tileSize * zoom);
        float viewBottom = viewTop + Gdx.graphics.getHeight() / (tileSize * zoom);

        Collection<Entity> candidates = entityManager.getAllEntitiesWith(
            PositionComponent.class, AppearanceComponent.class, ZComponent.class);

        // Collect visible entities into a list for sorting
        List<Entity> visible = new ArrayList<>();
        for (Entity entity : candidates) {
            PositionComponent pos = entityManager.getComponent(entity, PositionComponent.class);
            AppearanceComponent app = entityManager.getComponent(entity, AppearanceComponent.class);
            if (!app.visible) continue;

            if (pos.x < (int) Math.floor(viewLeft)  - 1 || pos.x > (int) Math.ceil(viewRight)) continue;
            if (pos.y < (int) Math.floor(viewTop)   - 1 || pos.y > (int) Math.ceil(viewBottom)) continue;

            visible.add(entity);
        }

        // Sort by Z (ascending) so lower layers draw first
        visible.sort(Comparator.comparingInt(e -> entityManager.getComponent(e, ZComponent.class).layer));

        for (Entity entity : visible) {
            PositionComponent pos = entityManager.getComponent(entity, PositionComponent.class);
            AppearanceComponent app = entityManager.getComponent(entity, AppearanceComponent.class);

            TextureRegion region = spritesheet.getRegion(app.spriteId);
            if (region == null) continue;

            float drawX = pos.x * tileSize * zoom + offsetX;
            float drawY = pos.y * tileSize * zoom + offsetY;
            batch.draw(region, drawX, drawY, tileSize * zoom, tileSize * zoom);
        }
    }

    public void dispose() {
        batch.dispose();
    }
}
