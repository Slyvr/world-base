package com.mycelbot.worldbase;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.mycelbot.worldbase.engine.World;
import com.mycelbot.worldbase.engine.systems.CameraSystem;
import com.mycelbot.worldbase.engine.systems.IslandGenerator;
import com.mycelbot.worldbase.engine.systems.RenderSystem;
import com.mycelbot.worldbase.util.SpriteSheetLoader;

/**
 * The main game screen that sets up the ECS world, systems, and
 * runs the render loop.
 */
public class WorldScreen extends ScreenAdapter {

    private World world;
    private RenderSystem renderSystem;
    private CameraSystem cameraSystem;
    private SpriteSheetLoader spritesheet;
    private SpriteBatch hudBatch;
    private BitmapFont font;

    private String hudText;

    @Override
    public void show() {
        // Load spritesheet
        spritesheet = new SpriteSheetLoader(
            "spritesheets/base_out_atlas.png",
            "spritesheets/base_out_atlas.json"
        );

        // Create ECS world with island generator
        world = new World(100, 100, new IslandGenerator());

        // Create systems
        renderSystem = new RenderSystem(world.getEntityManager(), spritesheet);
        cameraSystem = new CameraSystem();
        cameraSystem.centerOn(1600f, 1600f);
        Gdx.input.setInputProcessor(cameraSystem);

        // HUD
        hudBatch = new SpriteBatch();
        font = new BitmapFont();
        hudText = "WorldBase [ECS]  |  Middle-click: Pan  |  Scroll: Zoom";
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        renderSystem.render(
            cameraSystem.getOffsetX(),
            cameraSystem.getOffsetY(),
            cameraSystem.getZoom()
        );

        hudBatch.begin();
        font.draw(hudBatch, hudText, 10, Gdx.graphics.getHeight() - 10);
        font.draw(hudBatch, String.format("Zoom: %.1f", cameraSystem.getZoom()), 10, 25);
        font.draw(hudBatch, String.format("Entities: %d", world.getEntityManager().entityCount()), 10, 40);
        hudBatch.end();
    }

    @Override
    public void dispose() {
        renderSystem.dispose();
        hudBatch.dispose();
        font.dispose();
    }
}
