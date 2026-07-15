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
 * neighbor positions where the same terrain type exists, then finds
 * the sprite whose constraints are the best subset match — the sprite
 * with the largest constraint set that fits within the active set.
 * <p>
 * This handles straight edges (5-6 active constraints) where no exact
 * sprite exists by falling back to the edge/row sprite (3 constraints).
 */
public class AutoTileSystem {

    // Sorted sprites per tag: largest constraint set first
    private final Map<String, List<SpriteEntry>> tagSprites;

    private static class SpriteEntry {
        final Set<String> constraints;
        final int spriteId;
        SpriteEntry(Set<String> constraints, int spriteId) {
            this.constraints = constraints;
            this.spriteId = spriteId;
        }
    }

    public AutoTileSystem(SpriteSheetLoader loader) {
        this.tagSprites = new HashMap<>();

        for (var sprite : loader.getSpritesByTag("grass_normal")) {
            Set<String> cons = new HashSet<>(Arrays.asList(sprite.constraints));
            tagSprites.computeIfAbsent("grass_normal", k -> new ArrayList<>())
                      .add(new SpriteEntry(cons, sprite.id));
        }

        // Sort largest constraint set first so we match the most specific sprite
        for (List<SpriteEntry> list : tagSprites.values()) {
            list.sort((a, b) -> Integer.compare(b.constraints.size(), a.constraints.size()));
        }
    }

    /**
     * Update all grass tiles so their AppearanceComponent uses the correct
     * edge/corner sprite based on 8-neighbor matching.
     */
    public void autoTileGrass(EntityManager em, int width, int height) {
        boolean[][] isGrass = buildGrassGrid(em, width, height);
        List<SpriteEntry> candidates = tagSprites.get("grass_normal");
        if (candidates == null || candidates.isEmpty()) return;

        //               tl    tc    tr    ml    mr    bl    bc    br
        int[][] offsets = {{-1,1},{0,1},{1,1},{-1,0},{1,0},{-1,-1},{0,-1},{1,-1}};

        for (Entity entity : em.getAllEntitiesWith(PositionComponent.class, TileComponent.class, AppearanceComponent.class)) {
            TileComponent tc = em.getComponent(entity, TileComponent.class);
            if (tc.type != TileType.GRASS) continue;

            PositionComponent pos = em.getComponent(entity, PositionComponent.class);

            // Build active constraint set
            Set<String> active = new HashSet<>();
            String[] names = {"tl","tc","tr","ml","mr","bl","bc","br"};
            for (int i = 0; i < 8; i++) {
                int nx = pos.x + offsets[i][0];
                int ny = pos.y + offsets[i][1];
                if (nx >= 0 && nx < width && ny >= 0 && ny < height && isGrass[nx][ny]) {
                    active.add(names[i]);
                }
            }

            // Best subset match: iterate sprites by constraint count descending.
            // - 8-cons: full interior
            // - 7-cons: concave corners (1 missing neighbor)
            // - 3-cons: straight edges
            // - 1-cons: convex corners
            // - 0-cons: isolated tile (only match when active is empty)
            int matched = 0;
            for (SpriteEntry entry : candidates) {
                // 0-constraint sprites should only match truly isolated tiles
                if (entry.constraints.isEmpty()) {
                    if (active.isEmpty()) {
                        em.getComponent(entity, AppearanceComponent.class).spriteId = entry.spriteId;
                        matched = 1;
                    }
                    break;
                }
                if (active.containsAll(entry.constraints)) {
                    em.getComponent(entity, AppearanceComponent.class).spriteId = entry.spriteId;
                    matched = 1;
                    break;
                }
            }
            // If nothing matched, leave default sprite (full grass 118)
        }
    }

    private boolean[][] buildGrassGrid(EntityManager em, int width, int height) {
        boolean[][] grid = new boolean[width][height];
        for (Entity entity : em.getAllEntitiesWith(PositionComponent.class, TileComponent.class)) {
            TileComponent tc = em.getComponent(entity, TileComponent.class);
            if (tc.type == TileType.GRASS) {
                PositionComponent pos = em.getComponent(entity, PositionComponent.class);
                grid[pos.x][pos.y] = true;
            }
        }
        return grid;
    }
}
