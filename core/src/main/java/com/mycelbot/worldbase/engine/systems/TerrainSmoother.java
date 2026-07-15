package com.mycelbot.worldbase.engine.systems;

import com.mycelbot.worldbase.engine.components.*;
import com.mycelbot.worldbase.engine.ecs.Entity;
import com.mycelbot.worldbase.engine.ecs.EntityManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Post-processes generated terrain to remove thin grass features
 * (isolated tiles, pairs, narrow peninsulas) for a cleaner shoreline.
 * <p>
 * Runs one or more passes. Each pass removes grass tiles that have
 * fewer grass neighbors than the configured threshold.
 * <p>
 * Multi-pass example (default):
 * <ol>
 *   <li>Remove tiles with ≤1 neighbor — kills singles and pairs</li>
 *   <li>Remove tiles with ≤2 neighbors — kills narrow peninsulas</li>
 * </ol>
 */
public class TerrainSmoother {

    private final int[] thresholds;

    /** Default: two-pass smoothing (≤1 then ≤2). */
    public TerrainSmoother() {
        this(1, 2);
    }

    /**
     * @param thresholds One or more neighbor-count thresholds. Each pass
     *                   removes grass tiles whose neighbor count ≤ this value.
     */
    public TerrainSmoother(int... thresholds) {
        this.thresholds = thresholds;
    }

    /**
     * Remove thin grass features from the world.
     * Must be called before auto-tiling.
     */
    public void smooth(EntityManager em, int width, int height) {
        //               tl    tc    tr    ml    mr    bl    bc    br
        int[][] offsets = {{-1,1},{0,1},{1,1},{-1,0},{1,0},{-1,-1},{0,-1},{1,-1}};

        boolean[][] isGrass = buildGrid(em, width, height);

        for (int threshold : thresholds) {
            List<Entity> toRemove = new ArrayList<>();

            for (Entity entity : em.getAllEntitiesWith(PositionComponent.class, TileComponent.class)) {
                TileComponent tc = em.getComponent(entity, TileComponent.class);
                if (tc.type != com.mycelbot.worldbase.engine.TileType.GRASS) continue;

                PositionComponent pos = em.getComponent(entity, PositionComponent.class);
                int count = 0;
                for (int[] off : offsets) {
                    int nx = pos.x + off[0];
                    int ny = pos.y + off[1];
                    if (nx >= 0 && nx < width && ny >= 0 && ny < height && isGrass[nx][ny]) {
                        count++;
                    }
                }
                if (count <= threshold) {
                    toRemove.add(entity);
                }
            }

            // Remove marked tiles — the Z=0 water entity underneath remains
            for (Entity entity : toRemove) {
                PositionComponent pos = em.getComponent(entity, PositionComponent.class);
                isGrass[pos.x][pos.y] = false;
                em.destroyEntity(entity);
            }
        }
    }

    private boolean[][] buildGrid(EntityManager em, int width, int height) {
        boolean[][] grid = new boolean[width][height];
        for (Entity entity : em.getAllEntitiesWith(PositionComponent.class, TileComponent.class)) {
            TileComponent tc = em.getComponent(entity, TileComponent.class);
            if (tc.type == com.mycelbot.worldbase.engine.TileType.GRASS) {
                PositionComponent pos = em.getComponent(entity, PositionComponent.class);
                grid[pos.x][pos.y] = true;
            }
        }
        return grid;
    }
}
