package com.mycelbot.worldbase.engine.systems;

import com.mycelbot.worldbase.engine.TileType;
import com.mycelbot.worldbase.engine.components.PositionComponent;
import com.mycelbot.worldbase.engine.components.TileComponent;
import com.mycelbot.worldbase.engine.ecs.Entity;
import com.mycelbot.worldbase.engine.ecs.EntityManager;

import java.util.HashSet;
import java.util.Set;

/**
 * Post-processes generated terrain to remove problematic diagonal-only
 * grass adjacency patterns.
 * <p>
 * After TerrainSmoother removes thin grass features, some remaining grass
 * tiles may only connect diagonally — two tiles touching at a corner with
 * no edge-sharing grass tile between them. These patterns can't be auto-tiled
 * cleanly and often lead to holes in the coastline.
 * <p>
 * Pattern matched (four variants):
 * <pre>
 *   G W        W G
 *   W G   or   G W     (also mirrored vertically)
 * </pre>
 * <p>
 * Runs up to 5 passes; stops early if no tiles need removal.
 */
public class DiagonalCleaner {

    // The 4 diagonal offsets (dx, dy) and their two edge-connecting offsets
    private static final int[][] DIAG_OFFSETS = {
        { 1,  1}, // TR
        {-1,  1}, // TL
        { 1, -1}, // BR
        {-1, -1}  // BL
    };
    // For each diagonal offset, the two edge-connecting positions to check.
    // Edge positions are (x+dx, y) and (x, y+dy), i.e. the cardinals
    // between the starting tile and its diagonal neighbour.
    private static final int[][] EDGE_DX = {{1,0}, {-1,0}, {1,0}, {-1,0}};
    private static final int[][] EDGE_DY = {{0,1}, {0,1}, {0,-1}, {0,-1}};

    private static final int MAX_PASSES = 5;

    /**
     * Remove grass tiles that only connect diagonally without an edge
     * neighbour bridging them.
     * <p>
     * Runs up to {@value #MAX_PASSES} passes, stopping early when no
     * tiles are removed in a pass.
     */
    public void clean(EntityManager em, int width, int height) {
        boolean[][] isGrass = buildGrid(em, width, height);

        for (int pass = 0; pass < MAX_PASSES; pass++) {
            Set<Long> toRemove = new HashSet<>(); // canonical keys: (x << 16) | y

            for (Entity entity : em.getAllEntitiesWith(
                    PositionComponent.class, TileComponent.class)) {
                TileComponent tc = em.getComponent(entity, TileComponent.class);
                if (tc.type != TileType.GRASS) continue;
                PositionComponent pos = em.getComponent(entity, PositionComponent.class);

                int x = pos.x;
                int y = pos.y;

                for (int d = 0; d < 4; d++) {
                    int nx = x + DIAG_OFFSETS[d][0];
                    int ny = y + DIAG_OFFSETS[d][1];

                    // Diagonal neighbour must be in bounds and grass
                    if (nx < 0 || nx >= width || ny < 0 || ny >= height) continue;
                    if (!isGrass[nx][ny]) continue;

                    // Check the two edge-connecting tiles
                    int ex1 = x + EDGE_DX[d][0];
                    int ey1 = y + EDGE_DY[d][0];
                    int ex2 = x + EDGE_DX[d][1];
                    int ey2 = y + EDGE_DY[d][1];

                    boolean edge1Grass = isGrass[ex1][ey1];
                    boolean edge2Grass = isGrass[ex2][ey2];

                    // If neither edge tile is grass, this is a bad diagonal
                    if (!edge1Grass && !edge2Grass) {
                        // Deterministic: remove the tile with higher y, or higher x if y ties
                        long loserKey;
                        if (y > ny || (y == ny && x > nx)) {
                            loserKey = ((long) x << 32) | (y & 0xffffffffL);
                        } else {
                            loserKey = ((long) nx << 32) | (ny & 0xffffffffL);
                        }
                        toRemove.add(loserKey);
                    }
                }
            }

            if (toRemove.isEmpty()) {
                break; // stable — nothing more to clean
            }

            // Destroy all marked tiles and update grid
            for (long key : toRemove) {
                int rx = (int) (key >> 32);
                int ry = (int) (key & 0xffffffffL);

                // Find and destroy the entity at this position
                for (Entity entity : em.getAllEntitiesWith(
                        PositionComponent.class, TileComponent.class)) {
                    TileComponent tc = em.getComponent(entity, TileComponent.class);
                    if (tc.type != TileType.GRASS) continue;
                    PositionComponent pos = em.getComponent(entity, PositionComponent.class);
                    if (pos.x == rx && pos.y == ry) {
                        isGrass[rx][ry] = false;
                        em.destroyEntity(entity);
                        break;
                    }
                }
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
