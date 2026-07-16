package com.mycelbot.worldbase;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.mycelbot.worldbase.config.GameConfig;
import com.mycelbot.worldbase.engine.World;
import com.mycelbot.worldbase.engine.systems.AutoTileSystem;
import com.mycelbot.worldbase.engine.systems.CameraSystem;
import com.mycelbot.worldbase.engine.systems.TerrainSmoother;
import com.mycelbot.worldbase.engine.systems.IslandGenerator;
import com.mycelbot.worldbase.engine.systems.RenderSystem;
import com.mycelbot.worldbase.util.SpriteSheetLoader;

/**
 * The main game screen that sets up the ECS world, systems, and
 * runs the render loop.
 * <p>
 * Configuration is loaded from a JSON file via GameConfig — edit the
 * config file to tweak world size, generator parameters, camera limits,
 * and more without recompiling.
 */
public class WorldScreen extends ScreenAdapter {

    private World world;
    private RenderSystem renderSystem;
    private CameraSystem cameraSystem;
    private SpriteSheetLoader spritesheet;
    private SpriteBatch hudBatch;
    private BitmapFont font;

    private final GameConfig config;

    /** Provide a config; null-safe (loads defaults if null). */
    public WorldScreen(GameConfig config) {
        this.config = config != null ? config : GameConfig.load();
    }

    @Override
    public void show() {
        // Apply clear colour from config
        Gdx.gl.glClearColor(
            config.getClearColorR(),
            config.getClearColorG(),
            config.getClearColorB(),
            config.getClearColorA()
        );

        // Load spritesheet
        spritesheet = new SpriteSheetLoader(
            config.getSpritesheetImage(),
            config.getSpritesheetData()
        );

        // Create ECS world with island generator, sized from config
        world = new World(config, new IslandGenerator(config));

        // Smooth terrain: remove thin grass features before auto-tiling
        new TerrainSmoother().smooth(
            world.getEntityManager(), world.getWidth(), world.getHeight());

        // Auto-tile grass tiles to use correct edge/corner sprites
        new AutoTileSystem(spritesheet).autoTileGrass(
            world.getEntityManager(), world.getWidth(), world.getHeight());

        // Create systems
        renderSystem = new RenderSystem(
            world.getEntityManager(), spritesheet, config);
        cameraSystem = new CameraSystem(config);
        cameraSystem.centerOn(1600f, 1600f);
        Gdx.input.setInputProcessor(cameraSystem);

        // HUD
        hudBatch = new SpriteBatch();
        font = new BitmapFont();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        renderSystem.render(
            cameraSystem.getOffsetX(),
            cameraSystem.getOffsetY(),
            cameraSystem.getZoom()
        );

        hudBatch.begin();
        font.draw(hudBatch, config.getHudTitle(), 10,
            Gdx.graphics.getHeight() - 10);
        font.draw(hudBatch, String.format("Zoom: %.1f",
            cameraSystem.getZoom()), 10, 25);
        font.draw(hudBatch, String.format("Entities: %d",
            world.getEntityManager().entityCount()), 10, 40);
        hudBatch.end();
    }

    @Override
    public void dispose() {
        renderSystem.dispose();
        hudBatch.dispose();
        font.dispose();
    }
}
