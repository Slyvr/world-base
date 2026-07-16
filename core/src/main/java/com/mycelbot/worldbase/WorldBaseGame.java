package com.mycelbot.worldbase;

import com.badlogic.gdx.Game;
import com.mycelbot.worldbase.config.GameConfig;

/**
 * Main entry point for WorldBase.
 * Extends Game so we can swap screens later (world, menu, etc.).
 * <p>
 * Loads GameConfig at startup so all screens share the same config.
 */
public class WorldBaseGame extends Game {

    private GameConfig config;

    @Override
    public void create() {
        config = GameConfig.load();
        setScreen(new SplashScreen(this, config));
    }

    @Override
    public void dispose() {
        if (getScreen() != null) {
            getScreen().dispose();
        }
    }
}
