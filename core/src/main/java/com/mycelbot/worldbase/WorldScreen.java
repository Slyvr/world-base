package com.mycelbot.worldbase;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.mycelbot.worldbase.engine.CameraController;
import com.mycelbot.worldbase.engine.Renderer;
import com.mycelbot.worldbase.engine.World;
import com.mycelbot.worldbase.util.SpriteSheetLoader;

/**
 * The main game screen that sets up the world, renderer, and camera,
 * then runs the render loop.
 */
public class WorldScreen extends ScreenAdapter {

    private World world;
    private Renderer renderer;
    private CameraController cameraController;
    private SpriteSheetLoader spritesheet;
    private SpriteBatch hudBatch;
    private BitmapFont font;

    // HUD text info
    private String hudText;

    @Override
    public void show() {
        // Load the spritesheet
        spritesheet = new SpriteSheetLoader(
            "spritesheets/base_out_atlas.png",
            "spritesheets/base_out_atlas.json"
        );

        // Initialize renderer
        renderer = new Renderer(spritesheet);
        hudBatch = new SpriteBatch();
        font = new BitmapFont();

        // Generate a 40x40 world (all grass by default)
        world = new World(40, 40);

        // Set up camera
        cameraController = new CameraController();
        // Center on the middle of the world (pixel coords: 20*32=640, 20*32=640)
        cameraController.centerOn(640f, 640f);
        Gdx.input.setInputProcessor(cameraController);

        hudText = "WorldBase  |  Middle-click: Pan  |  Scroll: Zoom";
    }

    @Override
    public void render(float delta) {
        // Clear screen with a dark background
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Render the world with spritesheet textures
        renderer.render(world,
            cameraController.getOffsetX(),
            cameraController.getOffsetY(),
            cameraController.getZoom());

        // Draw HUD text
        hudBatch.begin();
        font.draw(hudBatch, hudText, 10, Gdx.graphics.getHeight() - 10);
        font.draw(hudBatch, String.format("Zoom: %.1f", cameraController.getZoom()), 10, 25);
        hudBatch.end();
    }

    @Override
    public void resize(int width, int height) {
        // Renderer handles this internally via camera setup each frame
    }

    @Override
    public void dispose() {
        renderer.dispose();
        hudBatch.dispose();
        font.dispose();
    }
}
