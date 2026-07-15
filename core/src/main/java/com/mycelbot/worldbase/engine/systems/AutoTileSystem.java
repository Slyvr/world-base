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

    //               tl    tc    tr    ml    mr    bl    bc    br
    private static final int[][] OFFSETS = {{-1,1},{0,1},{1,1},{-1,0},{1,0},{-1,-1},{0,-1},{1,-1}};
    private static final String[] NAMES = {"tl","tc","tr","ml","mr","bl","bc","br"};

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

        for (Entity entity : em.getAllEntitiesWith(PositionComponent.class, TileComponent.class, AppearanceComponent.class)) {
            TileComponent tc = em.getComponent(entity, TileComponent.class);
            if (tc.type != TileType.GRASS) continue;

            PositionComponent pos = em.getComponent(entity, PositionComponent.class);

            // Build active constraint set
            Set<String> active = new HashSet<>();
            for (int i = 0; i < 8; i++) {
                int nx = pos.x + OFFSETS[i][0];
                int ny = pos.y + OFFSETS[i][1];
                if (nx >= 0 && nx < width && ny >= 0 && ny < height && isGrass[nx][ny]) {
                    active.add(NAMES[i]);
                }
            }

            // Best subset match: iterate sprites by constraint count descending.
            for (SpriteEntry entry : candidates) {
                // 0-constraint sprites should only match truly isolated tiles
                if (entry.constraints.isEmpty()) {
                    if (active.isEmpty()) {
                        em.getComponent(entity, AppearanceComponent.class).spriteId = entry.spriteId;
                    }
                    break;
                }
                if (active.containsAll(entry.constraints)) {
                    em.getComponent(entity, AppearanceComponent.class).spriteId = entry.spriteId;
                    break;
                }
            }
            // If nothing matched, leave default sprite (full grass 118)
        }

        // Iterative validation: remove tiles whose assigned sprite doesn't match
        // their actual neighbors. After each removal pass, some remaining tiles
        // may lose neighbors and now also mismatch — repeat until stable.
        //
        // Two checks per tile:
        //  1) All of the sprite's required neighbors must be grass (constraints ⊆ active)
        //  2) None of the sprite's open positions (positions NOT in its constraints)
        //     should be grass — the sprite's texture expects water there.
        Map<Integer, Set<String>> spriteConstraints = new HashMap<>();
        Map<Integer, Set<String>> spriteOpens = new HashMap<>();
        Set<String> allPositions = new HashSet<>(Arrays.asList(NAMES));
        for (SpriteEntry entry : candidates) {
            spriteConstraints.put(entry.spriteId, entry.constraints);
            Set<String> open = new HashSet<>(allPositions);
            open.removeAll(entry.constraints);
            spriteOpens.put(entry.spriteId, open);
        }

        boolean changed = true;
        while (changed) {
            changed = false;
            List<Entity> toRemove = new ArrayList<>();

            for (Entity entity : em.getAllEntitiesWith(PositionComponent.class, TileComponent.class, AppearanceComponent.class)) {
                TileComponent tc = em.getComponent(entity, TileComponent.class);
                if (tc.type != TileType.GRASS) continue;

                PositionComponent pos = em.getComponent(entity, PositionComponent.class);
                AppearanceComponent app = em.getComponent(entity, AppearanceComponent.class);

                Set<String> active = new HashSet<>();
                for (int i = 0; i < 8; i++) {
                    int nx = pos.x + OFFSETS[i][0];
                    int ny = pos.y + OFFSETS[i][1];
                    if (nx >= 0 && nx < width && ny >= 0 && ny < height && isGrass[nx][ny]) {
                        active.add(NAMES[i]);
                    }
                }

                Set<String> required = spriteConstraints.get(app.spriteId);
                Set<String> open = spriteOpens.get(app.spriteId);
                // Remove if: constraints unknown, required neighbors missing,
                // or open-side positions have grass (sprite expects water there)
                boolean missingRequired = required == null || !active.containsAll(required);
                boolean extraGrass = open != null && !Collections.disjoint(active, open);
                if (missingRequired || extraGrass) {
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
