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
        // For edge sprites (5-cons) and concave corners (7-cons), check the
        // "critical" open position — the one directly opposite the constrained
        // face that the sprite's texture expects to be water:
        //   id=86  top edge        → tc must be water
        //   id=117 left edge       → ml must be water
        //   id=119 right edge      → mr must be water
        //   id=150 bottom edge     → bc must be water
        //   id=22-55 concave corner → missing corner must be water
        Map<Integer, Set<String>> spriteConstraints = new HashMap<>();
        Map<Integer, String> spriteCritical = new HashMap<>();
        for (SpriteEntry entry : candidates) {
            spriteConstraints.put(entry.spriteId, entry.constraints);
            // Determine the critical open position based on constraint pattern
            if (entry.constraints.size() >= 5) {
                spriteCritical.put(entry.spriteId, findCriticalOpen(entry.constraints));
            }
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
                String critical = spriteCritical.get(app.spriteId);
                boolean missingRequired = required == null || !active.containsAll(required);
                // Critical-open check: if the sprite expects water at a specific
                // position (e.g. tc for top-edge), that position must not be grass.
                boolean extraGrass = critical != null && active.contains(critical);
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

    /**
     * For a sprite with ≥5 constraints, find the single critical open position
     * that MUST be water for the sprite to be correct.
     * <p>
     * Edge sprites (5-cons): open positions form a row/column; the middle
     * element is critical (e.g. tc for top-edge, ml for left-edge).
     * Concave corners (7-cons): the single missing corner is critical.
     */
    private static String findCriticalOpen(Set<String> constraints) {
        Set<String> all = new HashSet<>(Arrays.asList(NAMES));
        all.removeAll(constraints); // this is the open set

        if (all.size() == 1) {
            // Concave corner — the single missing position
            return all.iterator().next();
        }

        // Edge sprites — the open set is a line of 3; the middle one is critical.
        // Lines: {tl,tc,tr}, {ml,_,mr}, {bl,bc,br}, {tl,ml,bl}, {tc,_,bc}, {tr,mr,br}
        if (all.contains("tc")) return "tc";
        if (all.contains("ml")) return "ml";
        if (all.contains("mr")) return "mr";
        if (all.contains("bc")) return "bc";
        return null;
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
