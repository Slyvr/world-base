package com.mycelbot.worldbase;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.mycelbot.worldbase.config.GameConfig;

/**
 * Main menu screen with New Game, Load Game, Settings, and Exit buttons.
 * Load Game and Settings are disabled for now.
 * New Game transitions to the world view.
 * Exit closes the application.
 */
public class MainMenuScreen extends ScreenAdapter {

    private final WorldBaseGame game;
    private final GameConfig config;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont titleFont;
    private BitmapFont buttonFont;
    private BitmapFont disabledFont;
    private GlyphLayout titleLayout;

    private static final String[] BUTTON_LABELS = {"New Game", "Load Game", "Settings", "Exit"};
    private static final boolean[] BUTTON_ENABLED = {true, false, false, true};
    private static final int BUTTON_COUNT = 4;

    // Button geometry — computed in show()
    private float[] buttonX;
    private float[] buttonY;
    private float buttonW;
    private float buttonH;
    private GlyphLayout[] buttonLayouts;

    // Colors
    private static final Color ENABLED_BG    = new Color(0.20f, 0.20f, 0.30f, 1f);
    private static final Color ENABLED_HOVER = new Color(0.30f, 0.35f, 0.50f, 1f);
    private static final Color DISABLED_BG   = new Color(0.15f, 0.15f, 0.20f, 1f);
    private static final Color DISABLED_TEXT = new Color(0.40f, 0.40f, 0.45f, 1f);
    private static final Color ENABLED_TEXT  = Color.WHITE;
    private static final Color TITLE_COLOR   = new Color(1f, 0.75f, 0.25f, 1f);

    public MainMenuScreen(WorldBaseGame game, GameConfig config) {
        this.game = game;
        this.config = config;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        titleFont = new BitmapFont();
        titleFont.getData().setScale(3.0f);
        titleLayout = new GlyphLayout(titleFont, "WORLDBASE");

        buttonFont = new BitmapFont();
        buttonFont.getData().setScale(1.5f);

        disabledFont = new BitmapFont();
        disabledFont.getData().setScale(1.5f);

        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        buttonW = 280f;
        buttonH = 60f;
        float startY = h * 0.70f;
        float gap = 80f;

        buttonX = new float[BUTTON_COUNT];
        buttonY = new float[BUTTON_COUNT];
        buttonLayouts = new GlyphLayout[BUTTON_COUNT];

        for (int i = 0; i < BUTTON_COUNT; i++) {
            buttonX[i] = (w - buttonW) / 2f;
            buttonY[i] = startY - i * gap;
            buttonLayouts[i] = new GlyphLayout(
                BUTTON_ENABLED[i] ? buttonFont : disabledFont,
                BUTTON_LABELS[i]
            );
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.08f, 0.08f, 0.12f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float mx = Gdx.input.getX();
        float my = Gdx.graphics.getHeight() - Gdx.input.getY(); // invert Y

        // Handle click
        if (Gdx.input.justTouched()) {
            for (int i = 0; i < BUTTON_COUNT; i++) {
                if (!BUTTON_ENABLED[i]) continue;
                if (mx >= buttonX[i] && mx <= buttonX[i] + buttonW
                    && my >= buttonY[i] - buttonH && my <= buttonY[i]) {
                    handleButton(i);
                    return;
                }
            }
        }

        // Draw buttons with ShapeRenderer
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < BUTTON_COUNT; i++) {
            Color bg;
            if (BUTTON_ENABLED[i]) {
                boolean hovered = mx >= buttonX[i] && mx <= buttonX[i] + buttonW
                    && my >= buttonY[i] - buttonH && my <= buttonY[i];
                bg = hovered ? ENABLED_HOVER : ENABLED_BG;
            } else {
                bg = DISABLED_BG;
            }
            shapeRenderer.setColor(bg);
            shapeRenderer.rect(buttonX[i], buttonY[i] - buttonH, buttonW, buttonH);
        }
        shapeRenderer.end();

        // Draw button text and title
        batch.begin();

        // Title
        float cx = Gdx.graphics.getWidth() / 2f;
        titleFont.setColor(TITLE_COLOR);
        titleFont.draw(batch, titleLayout, cx - titleLayout.width / 2f,
            Gdx.graphics.getHeight() * 0.93f);

        for (int i = 0; i < BUTTON_COUNT; i++) {
            if (BUTTON_ENABLED[i]) {
                buttonFont.setColor(ENABLED_TEXT);
                buttonFont.draw(batch, buttonLayouts[i],
                    buttonX[i] + (buttonW - buttonLayouts[i].width) / 2f,
                    buttonY[i] - (buttonH - buttonLayouts[i].height) / 2f);
            } else {
                disabledFont.setColor(DISABLED_TEXT);
                disabledFont.draw(batch, buttonLayouts[i],
                    buttonX[i] + (buttonW - buttonLayouts[i].width) / 2f,
                    buttonY[i] - (buttonH - buttonLayouts[i].height) / 2f);
            }
        }

        batch.end();
    }

    private void handleButton(int index) {
        switch (index) {
            case 0: // New Game
                game.setScreen(new WorldScreen(game, config));
                dispose();
                break;
            case 1: // Load Game — disabled
                break;
            case 2: // Settings — disabled
                break;
            case 3: // Exit
                Gdx.app.exit();
                break;
        }
    }

    @Override
    public void resize(int width, int height) {
        batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
        shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        titleFont.dispose();
        buttonFont.dispose();
        disabledFont.dispose();
    }
}
