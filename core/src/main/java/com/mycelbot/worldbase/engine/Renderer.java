package com.mycelbot.worldbase.engine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.mycelbot.worldbase.util.SpriteSheetLoader;

/**
 * Renders the 2D top-down world using sprites from the base_out_atlas spritesheet.
 * Each tile is drawn as a 32x32 sprite at its grid position, transformed
 * by the camera's offset and zoom.
 */
public class Renderer {

    private static final int TILE_SIZE = 32;

    private final SpriteBatch batch;
    private final OrthographicCamera camera;
    private final SpriteSheetLoader spritesheet;

    public Renderer(SpriteSheetLoader spritesheet) {
        this.batch = new SpriteBatch();
        this.camera = new OrthographicCamera();
        this.spritesheet = spritesheet;
    }

    /**
     * Main render call — draws the entire world.
     */
    public void render(World world, float offsetX, float offsetY, float zoom) {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        camera.setToOrtho(false, w, h);
        batch.setProjectionMatrix(camera.combined);
        batch.setColor(1f, 1f, 1f, 1f);

        batch.begin();
        drawTiles(world, offsetX, offsetY, zoom);
        batch.end();
    }

    /**
     * Draw all tiles in top-to-bottom, left-to-right order.
     * Calculates the visible range to avoid drawing off-screen tiles.
     */
    private void drawTiles(World world, float offsetX, float offsetY, float zoom) {
        int worldWidth = world.getWidth();
        int worldHeight = world.getHeight();

        // Calculate visible range
        float viewLeft = -offsetX / (TILE_SIZE * zoom);
        float viewTop = -offsetY / (TILE_SIZE * zoom);
        float viewRight = viewLeft + Gdx.graphics.getWidth() / (TILE_SIZE * zoom);
        float viewBottom = viewTop + Gdx.graphics.getHeight() / (TILE_SIZE * zoom);

        int startX = Math.max(0, (int) Math.floor(viewLeft));
        int startY = Math.max(0, (int) Math.floor(viewTop));
        int endX = Math.min(worldWidth, (int) Math.ceil(viewRight) + 1);
        int endY = Math.min(worldHeight, (int) Math.ceil(viewBottom) + 1);

        for (int y = startY; y < endY; y++) {
            for (int x = startX; x < endX; x++) {
                Tile tile = world.getTile(x, y);
                if (tile == null) continue;

                TextureRegion region = spritesheet.getRegion(tile.getType().getSpriteId());
                if (region == null) continue;

                float drawX = x * TILE_SIZE * zoom + offsetX;
                float drawY = y * TILE_SIZE * zoom + offsetY;

                batch.draw(region, drawX, drawY, TILE_SIZE * zoom, TILE_SIZE * zoom);
            }
        }
    }

    /**
     * Dispose all loaded textures and the sprite batch.
     */
    public void dispose() {
        spritesheet.dispose();
        batch.dispose();
    }
}
