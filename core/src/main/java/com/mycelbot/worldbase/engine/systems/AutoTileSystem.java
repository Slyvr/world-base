package com.mycelbot.worldbase.engine.systems;

import com.mycelbot.worldbase.engine.TileType;
import com.mycelbot.worldbase.engine.components.AppearanceComponent;
import com.mycelbot.worldbase.engine.components.PositionComponent;
import com.mycelbot.worldbase.engine.components.TileComponent;
import com.mycelbot.worldbase.engine.ecs.Entity;
import com.mycelbot.worldbase.engine.ecs.EntityManager;
import com.mycelbot.worldbase.util.SpriteSheetLoader;

import java.util.*;

/**
 * Auto-tiles terrain by matching each tile's neighbor pattern against
 * the constraint data in the spritesheet JSON.
 * <p>
 * For each tile, inspects all 8 surrounding cells, builds a set of
 * neighbor positions where the same terrain type exists, then looks up
 * the sprite whose constraints exactly match that set.
 */
public class AutoTileSystem {

    /** Maps a constraint set (sorted) to sprite ID, per tag. */
    private final Map<String, Map<String, Integer>> tileLookup;

    public AutoTileSystem(SpriteSheetLoader loader) {
        this.tileLookup = new HashMap<>();
        for (var sprite : loader.getSpritesByTag("grass_normal")) {
            String key = constraintKey(sprite.constraints);
            tileLookup.computeIfAbsent("grass_normal", k -> new HashMap<>())
                      .put(key, sprite.id);
        }
    }

    /** Build a canonical string key from a sorted array of constraint codes. */
    private static String constraintKey(String[] constraints) {
        String[] sorted = constraints.clone();
        java.util.Arrays.sort(sorted);
        return String.join(",", sorted);
    }

    /**
     * Update all grass tiles in the world so their AppearanceComponent
     * uses the correct edge/corner sprite based on 8-neighbor matching.
     */
    public void autoTileGrass(EntityManager em, int width, int height) {
        // Build a quick terrain grid: true = grass, false = other
        boolean[][] isGrass = buildGrassGrid(em, width, height);

        Map<String, Integer> grassLookup = tileLookup.get("grass_normal");
        if (grassLookup == null || grassLookup.isEmpty()) return;

        // Neighbor offsets in tile coordinates
        // tl    tc    tr
        //  ml  [ ]  mr
        // bl    bc    br
        String[] allPos = {"tl","tc","tr","ml","mr","bl","bc","br"};
        int[][] offsets = {
            {-1, 1}, {0, 1}, {1, 1},   // tl, tc, tr
            {-1, 0},         {1, 0},   // ml,     mr
            {-1,-1}, {0,-1}, {1,-1}    // bl, bc, br
        };

        // Collect all grass entities at Z=1
        for (Entity entity : em.getAllEntitiesWith(PositionComponent.class, TileComponent.class, AppearanceComponent.class)) {
            TileComponent tc = em.getComponent(entity, TileComponent.class);
            if (tc.type != TileType.GRASS) continue;

            PositionComponent pos = em.getComponent(entity, PositionComponent.class);

            // Build constraint set for this tile's neighbors
            List<String> active = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                int nx = pos.x + offsets[i][0];
                int ny = pos.y + offsets[i][1];
                if (nx >= 0 && nx < width && ny >= 0 && ny < height && isGrass[nx][ny]) {
                    active.add(allPos[i]);
                }
            }

            String key = constraintKey(active.toArray(new String[0]));
            Integer spriteId = grassLookup.get(key);
            if (spriteId != null) {
                em.getComponent(entity, AppearanceComponent.class).spriteId = spriteId;
            }
        }
    }

    /** Build a 2D boolean grid indicating which cells are grass. */
    private boolean[][] buildGrassGrid(EntityManager em, int width, int height) {
        boolean[][] grid = new boolean[width][height];

        for (Entity entity : em.getAllEntitiesWith(PositionComponent.class, TileComponent.class)) {
            PositionComponent pos = em.getComponent(entity, PositionComponent.class);
            TileComponent tc = em.getComponent(entity, TileComponent.class);
            if (tc.type == TileType.GRASS) {
                grid[pos.x][pos.y] = true;
            }
        }
        return grid;
    }
}
