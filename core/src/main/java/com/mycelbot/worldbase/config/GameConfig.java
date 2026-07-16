package com.mycelbot.worldbase.config;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

/**
 * Central configuration for WorldBase.
 *
 * Loaded from a JSON file at startup. Every field has a sensible default
 * so the game runs even without a config file. Keeps all magic numbers
 * in one place instead of scattered across constructors and render code.
 *
 * File resolution order:
 * 1. {@code Gdx.files.local("config/worldbase-config.json")} — editable working dir
 * 2. {@code Gdx.files.internal("config/worldbase-config.json")} — classpath fallback
 */
public class GameConfig {

    // ──────────────────────────────────────────────
    //  World dimensions
    // ──────────────────────────────────────────────
    private int worldWidth  = 100;
    private int worldHeight = 100;

    // ──────────────────────────────────────────────
    //  Generator — IslandGenerator defaults
    // ──────────────────────────────────────────────
    private String   generatorType       = "island";
    private double   noiseFrequency      = 0.04;
    private int      noiseOctaves        = 4;
    private double   islandRadiusFraction = 0.45;
    private double   islandThreshold     = 0.0;

    // ──────────────────────────────────────────────
    //  Tile rendering
    // ──────────────────────────────────────────────
    private int tileSize = 32;

    // ──────────────────────────────────────────────
    //  Sprite sheet
    // ──────────────────────────────────────────────
    private String spritesheetImage = "spritesheets/base_out_atlas.png";
    private String spritesheetData  = "spritesheets/base_out_atlas.json";

    // ──────────────────────────────────────────────
    //  Entity limits
    // ──────────────────────────────────────────────
    private int maxEntities = 50000;

    // ──────────────────────────────────────────────
    //  Camera
    // ──────────────────────────────────────────────
    private float cameraMinZoom  = 0.2f;
    private float cameraMaxZoom  = 4.0f;
    private float cameraZoomSpeed = 0.1f;

    // ──────────────────────────────────────────────
    //  Clear colour (RGBA 0–1)
    // ──────────────────────────────────────────────
    private float clearColorR = 0.15f;
    private float clearColorG = 0.15f;
    private float clearColorB = 0.20f;
    private float clearColorA = 1.0f;

    // ──────────────────────────────────────────────
    //  HUD
    // ──────────────────────────────────────────────
    private String hudTitle = "WorldBase [ECS]  |  Middle-click: Pan  |  Scroll: Zoom";

    // ──────────────────────────────────────────────
    //  Load
    // ──────────────────────────────────────────────

    /**
     * Load config from a JSON file.
     * Tries local (working directory) first, then internal (classpath).
     * If neither exists, returns defaults with a log message.
     */
    public static GameConfig load() {
        return load("config/worldbase-config.json");
    }

    static GameConfig load(String relPath) {
        GameConfig cfg = new GameConfig();

        // Try local (working directory) first
        FileHandle file = Gdx.files.local(relPath);
        if (!file.exists()) {
            // Fall back to internal (classpath/assets)
            file = Gdx.files.internal(relPath);
        }
        if (!file.exists()) {
            Gdx.app.log("GameConfig", "No config file found at \"" + relPath
                + "\" — using built-in defaults. Create one to customise.");
            return cfg;
        }

        try {
            JsonValue root = new JsonReader().parse(file);
            cfg.apply(root);
            Gdx.app.log("GameConfig", "Loaded config from " + file.path());
        } catch (Exception e) {
            Gdx.app.error("GameConfig", "Failed to parse config at "
                + file.path() + " — using defaults. Error: " + e.getMessage());
        }

        return cfg;
    }

    /** Apply values from parsed JSON to this config object. */
    private void apply(JsonValue root) {
        worldWidth       = root.getInt("worldWidth",       worldWidth);
        worldHeight      = root.getInt("worldHeight",      worldHeight);
        tileSize         = root.getInt("tileSize",         tileSize);
        maxEntities      = root.getInt("maxEntities",      maxEntities);

        generatorType       = root.getString("generatorType",        generatorType);
        noiseFrequency      = root.getDouble("noiseFrequency",       noiseFrequency);
        noiseOctaves        = root.getInt("noiseOctaves",            noiseOctaves);
        islandRadiusFraction = root.getDouble("islandRadiusFraction", islandRadiusFraction);
        islandThreshold     = root.getDouble("islandThreshold",      islandThreshold);

        spritesheetImage = root.getString("spritesheetImage", spritesheetImage);
        spritesheetData  = root.getString("spritesheetData",  spritesheetData);

        cameraMinZoom    = root.getFloat("cameraMinZoom",    cameraMinZoom);
        cameraMaxZoom    = root.getFloat("cameraMaxZoom",    cameraMaxZoom);
        cameraZoomSpeed  = root.getFloat("cameraZoomSpeed",  cameraZoomSpeed);

        clearColorR = root.getFloat("clearColorR", clearColorR);
        clearColorG = root.getFloat("clearColorG", clearColorG);
        clearColorB = root.getFloat("clearColorB", clearColorB);
        clearColorA = root.getFloat("clearColorA", clearColorA);

        hudTitle = root.getString("hudTitle", hudTitle);
    }

    // ──────────────────────────────────────────────
    //  Getters
    // ──────────────────────────────────────────────

    public int    getWorldWidth()              { return worldWidth; }
    public int    getWorldHeight()             { return worldHeight; }
    public int    getTileSize()                { return tileSize; }
    public int    getMaxEntities()             { return maxEntities; }

    public String getGeneratorType()           { return generatorType; }
    public double getNoiseFrequency()          { return noiseFrequency; }
    public int    getNoiseOctaves()            { return noiseOctaves; }
    public double getIslandRadiusFraction()    { return islandRadiusFraction; }
    public double getIslandThreshold()         { return islandThreshold; }

    public String getSpritesheetImage()        { return spritesheetImage; }
    public String getSpritesheetData()         { return spritesheetData; }

    public float  getCameraMinZoom()           { return cameraMinZoom; }
    public float  getCameraMaxZoom()           { return cameraMaxZoom; }
    public float  getCameraZoomSpeed()         { return cameraZoomSpeed; }

    public float  getClearColorR()             { return clearColorR; }
    public float  getClearColorG()             { return clearColorG; }
    public float  getClearColorB()             { return clearColorB; }
    public float  getClearColorA()             { return clearColorA; }

    public String getHudTitle()                { return hudTitle; }
}