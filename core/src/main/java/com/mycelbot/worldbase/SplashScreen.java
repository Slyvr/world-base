package com.mycelbot.worldbase;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.mycelbot.worldbase.config.GameConfig;

/**
 * Splash screen shown for 3 seconds before transitioning to the main menu.
 * Displays the WorldBase title with a fade-in/fade-out effect.
 */
public class SplashScreen extends ScreenAdapter {

    private static final float SPLASH_DURATION = 3.0f;
    private static final float FADE_DURATION = 0.5f;

    private final WorldBaseGame game;
    private final GameConfig config;
    private SpriteBatch batch;
    private BitmapFont titleFont;
    private BitmapFont subtitleFont;
    private GlyphLayout titleLayout;
    private GlyphLayout subtitleLayout;
    private float elapsed;

    public SplashScreen(WorldBaseGame game, GameConfig config) {
        this.game = game;
        this.config = config;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();

        // Use default font, scaled up for the title
        titleFont = new BitmapFont();
        titleFont.getData().setScale(4.0f);

        subtitleFont = new BitmapFont();
        subtitleFont.getData().setScale(1.2f);

        // Precompute text dimensions for centering
        titleLayout = new GlyphLayout(titleFont, "WORLDBASE");
        subtitleLayout = new GlyphLayout(subtitleFont, "Procedural Worlds Await");

        elapsed = 0f;
    }

    @Override
    public void render(float delta) {
        elapsed += delta;

        float alpha;
        if (elapsed < FADE_DURATION) {
            alpha = elapsed / FADE_DURATION;
        } else if (elapsed > SPLASH_DURATION - FADE_DURATION) {
            alpha = (SPLASH_DURATION - elapsed) / FADE_DURATION;
        } else {
            alpha = 1.0f;
        }
        alpha = Math.max(0f, Math.min(1f, alpha));

        Gdx.gl.glClearColor(0.08f, 0.08f, 0.12f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();

        float cx = Gdx.graphics.getWidth() / 2f;
        float cy = Gdx.graphics.getHeight() / 2f;

        titleFont.setColor(1, 1, 1, alpha);
        titleFont.draw(batch, titleLayout, cx - titleLayout.width / 2f, cy + titleLayout.height / 2f + 10f);

        subtitleFont.setColor(0.7f, 0.7f, 0.85f, alpha);
        subtitleFont.draw(batch, subtitleLayout, cx - subtitleLayout.width / 2f, cy - subtitleLayout.height / 2f - 10f);

        batch.end();

        if (elapsed >= SPLASH_DURATION) {
            game.setScreen(new MainMenuScreen(game, config));
            dispose();
        }
    }

    @Override
    public void resize(int width, int height) {
        batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
    }

    @Override
    public void dispose() {
        batch.dispose();
        titleFont.dispose();
        subtitleFont.dispose();
    }
}
