package com.mycelbot.worldbase.engine.systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.mycelbot.worldbase.config.GameConfig;

/**
 * System that handles camera input and maintains view state.
 * - Middle-click + drag: pan
 * - Scroll wheel: zoom (centered on cursor)
 * <p>
 * Zoom limits and speed are pulled from GameConfig.
 */
public class CameraSystem extends InputAdapter {

    private float offsetX = 0f;
    private float offsetY = 0f;
    private float zoom = 1.0f;

    private final float minZoom;
    private final float maxZoom;
    private final float zoomSpeed;

    private int lastMouseX, lastMouseY;
    private boolean dragging = false;

    public CameraSystem(GameConfig config) {
        this.minZoom   = config.getCameraMinZoom();
        this.maxZoom   = config.getCameraMaxZoom();
        this.zoomSpeed = config.getCameraZoomSpeed();
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        lastMouseX = screenX;
        lastMouseY = screenY;
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (button == Input.Buttons.MIDDLE) {
            dragging = true;
            lastMouseX = screenX;
            lastMouseY = screenY;
            return true;
        }
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (dragging) {
            offsetX += screenX - lastMouseX;
            offsetY -= screenY - lastMouseY;
            lastMouseX = screenX;
            lastMouseY = screenY;
            return true;
        }
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (button == Input.Buttons.MIDDLE) {
            dragging = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        float worldX = (lastMouseX - offsetX) / zoom;
        float worldY = (lastMouseY - offsetY) / zoom;
        zoom = Math.max(minZoom, Math.min(maxZoom, zoom - amountY * zoomSpeed));
        offsetX = lastMouseX - worldX * zoom;
        offsetY = lastMouseY - worldY * zoom;
        return true;
    }

    // ---- Public accessors ----

    public float getOffsetX() { return offsetX; }
    public float getOffsetY() { return offsetY; }
    public float getZoom()    { return zoom; }

    public void centerOn(float worldX, float worldY) {
        offsetX = 640f - worldX * zoom;
        offsetY = 360f - worldY * zoom;
    }
}
