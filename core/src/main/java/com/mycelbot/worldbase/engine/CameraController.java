package com.mycelbot.worldbase.engine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;

/**
 * Controls the 2D top-down camera via mouse input.
 * - Middle-click + drag: pan the view
 * - Scroll wheel: zoom in/out (centered on cursor position)
 * <p>
 * Designed to be modular — controls can be remapped or extended.
 */
public class CameraController extends InputAdapter {

    private float offsetX = 0f;
    private float offsetY = 0f;
    private float zoom = 1.0f;

    private static final float MIN_ZOOM = 0.2f;
    private static final float MAX_ZOOM = 4.0f;
    private static final float ZOOM_SPEED = 0.1f;

    private int lastMouseX, lastMouseY;
    private boolean dragging = false;

    public CameraController() {}

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
            float dx = (screenX - lastMouseX);
            float dy = (screenY - lastMouseY);
            offsetX += dx;
            offsetY -= dy;
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
        // Calculate the world point under the cursor before zooming
        float worldX = (lastMouseX - offsetX) / zoom;
        float worldY = (lastMouseY - offsetY) / zoom;

        // Apply zoom
        zoom -= amountY * ZOOM_SPEED;
        zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom));

        // Reposition offset so the same world point stays under the cursor
        offsetX = lastMouseX - worldX * zoom;
        offsetY = lastMouseY - worldY * zoom;
        return true;
    }

    // ---- Public accessors ----

    public float getOffsetX() {
        return offsetX;
    }

    public float getOffsetY() {
        return offsetY;
    }

    public float getZoom() {
        return zoom;
    }

    public void setOffsetX(float offsetX) {
        this.offsetX = offsetX;
    }

    public void setOffsetY(float offsetY) {
        this.offsetY = offsetY;
    }

    public void setZoom(float zoom) {
        this.zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom));
    }

    /**
     * Center the view on the given world pixel coordinates.
     * Assumes a 1280x720 window.
     */
    public void centerOn(float worldX, float worldY) {
        offsetX = 640f - worldX * zoom;
        offsetY = 360f - worldY * zoom;
    }
}
