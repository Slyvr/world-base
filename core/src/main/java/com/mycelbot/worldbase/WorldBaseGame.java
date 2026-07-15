package com.mycelbot.worldbase;

import com.badlogic.gdx.Game;

/**
 * Main entry point for WorldBase.
 * Extends Game so we can swap screens later (world, menu, etc.).
 */
public class WorldBaseGame extends Game {

    @Override
    public void create() {
        setScreen(new WorldScreen());
    }

    @Override
    public void dispose() {
        if (getScreen() != null) {
            getScreen().dispose();
        }
    }
}
