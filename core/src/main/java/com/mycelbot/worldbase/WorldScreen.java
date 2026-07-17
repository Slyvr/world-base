package com.mycelbot.worldbase;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
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
 * runs the render loop — now with a live in-game menu overlay.
 * <p>
 * Configuration is loaded from a JSON file via GameConfig — edit the
 * config file to tweak world size, generator parameters, camera limits,
 * and more without recompiling.
 * <p>
 * The in-game menu shows a bottom bar with action buttons and a hamburger
 * menu that opens a centre-screen submenu (Resume, Save, Settings, Main Menu).
 */
public class WorldScreen extends ScreenAdapter {

    private World world;
    private RenderSystem renderSystem;
    private CameraSystem cameraSystem;
    private SpriteSheetLoader spritesheet;
    private SpriteBatch hudBatch;
    private BitmapFont font;

    private final GameConfig config;

    // ── In-game menu state ──
    private boolean menuOpen = false;
    private ShapeRenderer barRenderer;
    private BitmapFont barFont;
    private BitmapFont smallFont;
    private GlyphLayout measure;

    // Bottom bar geometry
    private static final float BAR_HEIGHT = 52f;
    private int screenW, screenH;

    // Hamburger button (bottom-left corner of the bar)
    private static final float HAMBURGER_PAD = 8f;
    private static final float HAMBURGER_SIZE = 36f;
    private float hamburgerX, hamburgerY; // bottom-left corner

    // Action buttons on the bar (centered on the bar, right of hamburger)
    private static final String[] ACTION_LABELS = {"Build", "Craft", "Recruit"};
    private static final int ACTION_COUNT = 3;
    private float[] actionBtnX;   // bottom-left X of each button
    private float[] actionBtnW;   // width of each button
    private GlyphLayout[] actionLayouts;

    // Submenu overlay
    private static final String[] SUBMENU_LABELS = {"Resume", "Save", "Settings", "Main Menu"};
    private static final boolean[] SUBMENU_ENABLED = {true, false, false, true};
    private static final int SUBMENU_COUNT = 4;
    private static final float SUBMENU_BTN_W = 220f;
    private static final float SUBMENU_BTN_H = 50f;
    private static final float PANEL_PAD = 20f;

    private float panelX, panelY, panelW, panelH;
    private float[] smBtnX;          // bottom-left X
    private float[] smBtnY;          // bottom-left Y
    private GlyphLayout[] smLayouts;

    // Colors
    private static final Color BAR_BG       = new Color(0.12f, 0.12f, 0.18f, 0.92f);
    private static final Color HAMBURGER_BG = new Color(0.22f, 0.25f, 0.35f, 1f);
    private static final Color HAMBURGER_HV = new Color(0.32f, 0.38f, 0.55f, 1f);
    private static final Color HAMBURGER_TX = new Color(0.85f, 0.85f, 0.90f, 1f);
    private static final Color ACT_BTN_BG   = new Color(0.25f, 0.30f, 0.42f, 1f);
    private static final Color ACT_BTN_HV   = new Color(0.35f, 0.42f, 0.58f, 1f);
    private static final Color ACT_BTN_TX   = Color.WHITE;
    private static final Color OVERLAY_BG   = new Color(0f, 0f, 0f, 0.55f);
    private static final Color PANEL_BG     = new Color(0.12f, 0.12f, 0.18f, 0.95f);
    private static final Color SM_BTN_EN    = new Color(0.22f, 0.25f, 0.35f, 1f);
    private static final Color SM_BTN_HV    = new Color(0.32f, 0.38f, 0.55f, 1f);
    private static final Color SM_BTN_DIS   = new Color(0.15f, 0.15f, 0.20f, 1f);
    private static final Color SM_TX_EN     = Color.WHITE;
    private static final Color SM_TX_DIS    = new Color(0.40f, 0.40f, 0.45f, 1f);

        private final WorldBaseGame game;

    /** Provide the game and an optional config; null-safe config (loads defaults if null). */
    public WorldScreen(WorldBaseGame game, GameConfig config) {
        this.game   = game;
        this.config = config != null ? config : GameConfig.load();
    }

    // ──────────────────────────────────────────────
    //  Lifecycle
    // ──────────────────────────────────────────────

    @Override
    public void show() {
        screenW = Gdx.graphics.getWidth();
        screenH = Gdx.graphics.getHeight();

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

        // HUD
        hudBatch = new SpriteBatch();
        font = new BitmapFont();

        // ── In-game menu setup ──
        initMenuUI();

        // Input multiplexer: menu handler first, then camera
        InputMultiplexer mux = new InputMultiplexer();
        mux.addProcessor(new MenuInputHandler());
        mux.addProcessor(cameraSystem);
        Gdx.input.setInputProcessor(mux);
    }

    /** Initialise / re-compute all menu geometry. */
    private void initMenuUI() {
        float w = screenW;
        float h = screenH;

        barRenderer = new ShapeRenderer();
        barFont = new BitmapFont();
        barFont.getData().setScale(1.15f);
        smallFont = new BitmapFont();
        smallFont.getData().setScale(0.95f);
        measure = new GlyphLayout();

        // ── Hamburger button (bottom-left) ──
        hamburgerX = HAMBURGER_PAD;
        hamburgerY = HAMBURGER_PAD;

        // ── Action buttons on the bar ──
        float areaLeft = hamburgerX + HAMBURGER_SIZE + HAMBURGER_PAD * 2f;
        float areaW    = w - areaLeft - 10f;
        float gap      = 12f;

        actionLayouts = new GlyphLayout[ACTION_COUNT];
        actionBtnX    = new float[ACTION_COUNT];
        actionBtnW    = new float[ACTION_COUNT];

        float totalW = 0f;
        for (int i = 0; i < ACTION_COUNT; i++) {
            actionLayouts[i] = new GlyphLayout(smallFont, ACTION_LABELS[i]);
            actionBtnW[i]    = actionLayouts[i].width + 20f;
            totalW          += actionBtnW[i];
        }
        totalW += gap * (ACTION_COUNT - 1);

        float startX = areaLeft + Math.max(0f, (areaW - totalW) / 2f);
        float cx = startX;
        for (int i = 0; i < ACTION_COUNT; i++) {
            actionBtnX[i] = cx;
            cx += actionBtnW[i] + gap;
        }

        // ── Submenu overlay ──
        float innerH = SUBMENU_COUNT * SUBMENU_BTN_H + (SUBMENU_COUNT - 1) * 10f;
        panelW = SUBMENU_BTN_W + PANEL_PAD * 2f;
        panelH = innerH + PANEL_PAD * 2f;
        panelX = (w - panelW) / 2f;
        panelY = (h - panelH) / 2f;

        smBtnX = new float[SUBMENU_COUNT];
        smBtnY = new float[SUBMENU_COUNT];
        smLayouts = new GlyphLayout[SUBMENU_COUNT];

        float btnTop = panelY + panelH - PANEL_PAD - SUBMENU_BTN_H;
        for (int i = 0; i < SUBMENU_COUNT; i++) {
            smBtnX[i] = panelX + PANEL_PAD;
            smBtnY[i] = btnTop - i * (SUBMENU_BTN_H + 10f);
            smLayouts[i] = new GlyphLayout(
                SUBMENU_ENABLED[i] ? barFont : smallFont,
                SUBMENU_LABELS[i]);
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // ── Render world ──
        renderSystem.render(
            cameraSystem.getOffsetX(),
            cameraSystem.getOffsetY(),
            cameraSystem.getZoom()
        );

        // ── Top HUD ──
        hudBatch.begin();
        font.draw(hudBatch, config.getHudTitle(), 10,
            Gdx.graphics.getHeight() - 10);
        font.draw(hudBatch, String.format("Zoom: %.1f",
            cameraSystem.getZoom()), 10, 25);
        font.draw(hudBatch, String.format("Entities: %d",
            world.getEntityManager().entityCount()), 10, 40);
        hudBatch.end();

        // ── Bottom bar ──
        drawBottomBar();

        // ── Submenu overlay ──
        if (menuOpen) {
            drawSubmenu();
        }
    }

    // ──────────────────────────────────────────────
    //  Drawing helpers
    // ──────────────────────────────────────────────

    private void drawBottomBar() {
        float mx = Gdx.input.getX();
        float my = screenH - Gdx.input.getY();

        // Bar background
        barRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Entire bottom area
        barRenderer.setColor(BAR_BG);
        barRenderer.rect(0, 0, screenW, BAR_HEIGHT);

        // Hamburger button
        boolean hamburgerHovered = menuOpen || isInside(mx, my,
            hamburgerX, hamburgerY, HAMBURGER_SIZE, HAMBURGER_SIZE);
        barRenderer.setColor(hamburgerHovered ? HAMBURGER_HV : HAMBURGER_BG);
        barRenderer.rect(hamburgerX, hamburgerY, HAMBURGER_SIZE, HAMBURGER_SIZE);

        // Action buttons
        for (int i = 0; i < ACTION_COUNT; i++) {
            boolean hovered = isInside(mx, my,
                actionBtnX[i], HAMBURGER_PAD, actionBtnW[i], HAMBURGER_SIZE);
            barRenderer.setColor(hovered ? ACT_BTN_HV : ACT_BTN_BG);
            barRenderer.rect(actionBtnX[i], HAMBURGER_PAD,
                actionBtnW[i], HAMBURGER_SIZE);
        }

        barRenderer.end();

        // Text overlay
        hudBatch.begin();

        // Hamburger "☰" using a simple text approach
        barFont.setColor(HAMBURGER_TX);
        measure.setText(barFont, "\u2630");
        barFont.draw(hudBatch, measure,
            hamburgerX + (HAMBURGER_SIZE - measure.width) / 2f,
            hamburgerY + (HAMBURGER_SIZE + measure.height) / 2f);

        // Action button labels
        smallFont.setColor(ACT_BTN_TX);
        for (int i = 0; i < ACTION_COUNT; i++) {
            smallFont.draw(hudBatch, actionLayouts[i],
                actionBtnX[i] + (actionBtnW[i] - actionLayouts[i].width) / 2f,
                HAMBURGER_PAD + (HAMBURGER_SIZE + actionLayouts[i].height) / 2f);
        }

        hudBatch.end();
    }

    private void drawSubmenu() {
        float mx = Gdx.input.getX();
        float my = screenH - Gdx.input.getY();

        // Semi-transparent backdrop
        barRenderer.begin(ShapeRenderer.ShapeType.Filled);
        barRenderer.setColor(OVERLAY_BG);
        barRenderer.rect(0, 0, screenW, screenH);

        // Panel background
        barRenderer.setColor(PANEL_BG);
        barRenderer.rect(panelX, panelY, panelW, panelH);

        // Submenu buttons
        for (int i = 0; i < SUBMENU_COUNT; i++) {
            boolean hovered = SUBMENU_ENABLED[i]
                && isInside(mx, my, smBtnX[i], smBtnY[i],
                    SUBMENU_BTN_W, SUBMENU_BTN_H);
            barRenderer.setColor(
                !SUBMENU_ENABLED[i] ? SM_BTN_DIS
                : hovered ? SM_BTN_HV : SM_BTN_EN);
            barRenderer.rect(smBtnX[i], smBtnY[i], SUBMENU_BTN_W, SUBMENU_BTN_H);
        }
        barRenderer.end();

        // Text
        hudBatch.begin();
        for (int i = 0; i < SUBMENU_COUNT; i++) {
            BitmapFont f = SUBMENU_ENABLED[i] ? barFont : smallFont;
            f.setColor(SUBMENU_ENABLED[i] ? SM_TX_EN : SM_TX_DIS);
            f.draw(hudBatch, smLayouts[i],
                smBtnX[i] + (SUBMENU_BTN_W - smLayouts[i].width) / 2f,
                smBtnY[i] + (SUBMENU_BTN_H + smLayouts[i].height) / 2f);
        }
        hudBatch.end();
    }

    // ──────────────────────────────────────────────
    //  Helpers
    // ──────────────────────────────────────────────

    /** Check if (px,py) is inside the rect defined by (rx,ry,rw,rh). */
    private static boolean isInside(float px, float py,
                                    float rx, float ry, float rw, float rh) {
        return px >= rx && px <= rx + rw && py >= ry && py <= ry + rh;
    }

    // ──────────────────────────────────────────────
    //  Input handler
    // ──────────────────────────────────────────────

    /** Inner class that handles in-game menu clicks. */
    private class MenuInputHandler extends InputAdapter {
        @Override
        public boolean touchDown(int sx, int sy, int pointer, int button) {
            if (button != Input.Buttons.LEFT) return false;
            float mx = sx;
            float my = screenH - sy; // invert Y

            // ── Submenu (if open) takes priority ──
            if (menuOpen) {
                // Check submenu buttons
                for (int i = 0; i < SUBMENU_COUNT; i++) {
                    if (!SUBMENU_ENABLED[i]) continue;
                    if (isInside(mx, my, smBtnX[i], smBtnY[i],
                            SUBMENU_BTN_W, SUBMENU_BTN_H)) {
                        handleSubmenu(i);
                        return true;
                    }
                }
                // Click outside panel → close menu
                if (!isInside(mx, my, panelX, panelY, panelW, panelH)) {
                    menuOpen = false;
                    return true;
                }
                return true; // consume all clicks while menu is open
            }

            // ── Bottom bar interactions ──
            if (my < BAR_HEIGHT) {
                // Hamburger button
                if (isInside(mx, my, hamburgerX, hamburgerY,
                        HAMBURGER_SIZE, HAMBURGER_SIZE)) {
                    menuOpen = true;
                    return true;
                }
                // Action buttons
                for (int i = 0; i < ACTION_COUNT; i++) {
                    if (isInside(mx, my, actionBtnX[i], HAMBURGER_PAD,
                            actionBtnW[i], HAMBURGER_SIZE)) {
                        handleAction(i);
                        return true;
                    }
                }
                return true; // consume clicks on the bar area
            }

            return false; // let camera system handle it
        }
    }

    private void handleAction(int index) {
        switch (index) {
            case 0: Gdx.app.log("WorldScreen", "Build"); break;
            case 1: Gdx.app.log("WorldScreen", "Craft"); break;
            case 2: Gdx.app.log("WorldScreen", "Recruit"); break;
        }
    }

    private void handleSubmenu(int index) {
        switch (index) {
            case 0: // Resume
                menuOpen = false;
                break;
            case 1: // Save — disabled
                break;
            case 2: // Settings — disabled
                break;
            case 3: // Main Menu
                game.setScreen(new MainMenuScreen(game, config));
                dispose();
                break;
        }
    }

    @Override
    public void resize(int width, int height) {
        screenW = width;
        screenH = height;
        hudBatch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
        barRenderer.setProjectionMatrix(hudBatch.getProjectionMatrix());
        initMenuUI();
    }

    @Override
    public void dispose() {
        renderSystem.dispose();
        hudBatch.dispose();
        font.dispose();
        if (barRenderer != null) barRenderer.dispose();
        if (barFont != null) barFont.dispose();
        if (smallFont != null) smallFont.dispose();
    }
}
