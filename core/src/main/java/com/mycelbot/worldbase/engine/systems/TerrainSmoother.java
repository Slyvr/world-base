package com.mycelbot.worldbase.engine.systems;

import com.mycelbot.worldbase.engine.TileType;
import com.mycelbot.worldbase.engine.components.PositionComponent;
import com.mycelbot.worldbase.engine.components.TileComponent;
import com.mycelbot.worldbase.engine.ecs.Entity;
import com.mycelbot.worldbase.engine.ecs.EntityManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Post-processes generated terrain to remove unstable grass tiles.
 * <p>
 * A grass tile survives only if it has grass neighbours in at least
 * two perpendicular cardinal directions — e.g. top+right, bottom+left,
 * top+left, or bottom+right. Opposite-only pairs (top+bottom, left+right)
 * and single-neighbour tiles are removed.
 * <p>
 * Runs iteratively until stable, so removing one tile may trigger
 * cascading removal of its neighbours.
 */
public class TerrainSmoother {

    // Cardinal offsets: tc, bc, ml, mr
    private static final int[] CX = { 0,  0, -1,  1};
    private static final int[] CY = { 1, -1,  0,  0};

    // Diagonal offsets: tl, tr, bl, br
    private static final int[] DXd = {-1,  1, -1,  1};
    private static final int[] DYd = { 1,  1, -1, -1};

    /**
     * Remove grass tiles that lack at least two perpendicular edges
     * connecting to other grass tiles, then remove tiles where opposite
     * diagonal corners are both water (patterns the auto-tile can't handle).
     * Both phases iterate until stable.
     */
    public void smooth(EntityManager em, int width, int height) {
        boolean[][] isGrass = buildGrid(em, width, height);

        // ─── Phase 1: perpendicular-edge survival rule ───
        boolean changed = true;
        while (changed) {
            changed = false;
            List<Entity> toRemove = new ArrayList<>();

            for (Entity entity : em.getAllEntitiesWith(
                    PositionComponent.class, TileComponent.class)) {
                TileComponent tc = em.getComponent(entity, TileComponent.class);
                if (tc.type != TileType.GRASS) continue;
                PositionComponent pos = em.getComponent(entity, PositionComponent.class);

                int x = pos.x;
                int y = pos.y;

                // Sample 4 cardinal neighbours
                boolean top    = y + 1 < height && isGrass[x][y + 1];
                boolean bottom = y - 1 >= 0    && isGrass[x][y - 1];
                boolean left   = x - 1 >= 0    && isGrass[x - 1][y];
                boolean right  = x + 1 < width && isGrass[x + 1][y];

                // Check for at least one perpendicular pair
                boolean valid = (top && left) || (top && right)
                             || (bottom && left) || (bottom && right);

                if (!valid) {
                    toRemove.add(entity);
                }
            }

            // Remove marked tiles and update grid
            for (Entity entity : toRemove) {
                PositionComponent pos = em.getComponent(entity, PositionComponent.class);
                isGrass[pos.x][pos.y] = false;
                em.destroyEntity(entity);
                changed = true;
            }
        }

        // ─── Phase 2: opposite-corner water cleanup ───
        // The auto-tile system's subset matching can't correctly handle
        // tiles where two opposite diagonal neighbours are both water
        // (e.g. tl+br or tr+bl water). Remove those tiles so the auto-tiler
        // gets a clean edge it can work with.
        changed = true;
        while (changed) {
            changed = false;
            List<Entity> toRemove = new ArrayList<>();

            for (Entity entity : em.getAllEntitiesWith(
                    PositionComponent.class, TileComponent.class)) {
                TileComponent tc = em.getComponent(entity, TileComponent.class);
                if (tc.type != TileType.GRASS) continue;
                PositionComponent pos = em.getComponent(entity, PositionComponent.class);

                int x = pos.x;
                int y = pos.y;

                // Check 4 diagonal neighbours
                boolean tl = x - 1 >= 0 && y + 1 < height && isGrass[x - 1][y + 1];
                boolean tr = x + 1 < width && y + 1 < height && isGrass[x + 1][y + 1];
                boolean bl = x - 1 >= 0 && y - 1 >= 0 && isGrass[x - 1][y - 1];
                boolean br = x + 1 < width && y - 1 >= 0 && isGrass[x + 1][y - 1];

                // Opposite corners both lack grass (either water or out of bounds)
                if ((!tl && !br) || (!tr && !bl)) {
                    toRemove.add(entity);
                }
            }

            for (Entity entity : toRemove) {
                PositionComponent pos = em.getComponent(entity, PositionComponent.class);
                isGrass[pos.x][pos.y] = false;
                em.destroyEntity(entity);
                changed = true;
            }
        }
    }

    private boolean[][] buildGrid(EntityManager em, int width, int height) {
        boolean[][] grid = new boolean[width][height];
        for (Entity entity : em.getAllEntitiesWith(
                PositionComponent.class, TileComponent.class)) {
            TileComponent tc = em.getComponent(entity, TileComponent.class);
            if (tc.type == TileType.GRASS) {
                PositionComponent pos = em.getComponent(entity, PositionComponent.class);
                grid[pos.x][pos.y] = true;
            }
        }
        return grid;
    }
}
