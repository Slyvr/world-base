package com.mycelbot.worldbase.engine.systems;

import com.mycelbot.worldbase.config.GameConfig;
import com.mycelbot.worldbase.engine.IslandInfo;
import com.mycelbot.worldbase.engine.TileType;
import com.mycelbot.worldbase.engine.components.AppearanceComponent;
import com.mycelbot.worldbase.engine.components.PositionComponent;
import com.mycelbot.worldbase.engine.components.TileComponent;
import com.mycelbot.worldbase.engine.components.ZComponent;
import com.mycelbot.worldbase.engine.ecs.Entity;
import com.mycelbot.worldbase.engine.ecs.EntityManager;
import com.mycelbot.worldbase.util.SimplexNoise;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * Generates a world of water with noise-shaped islands at the center.
 * <p>
 * Two render layers:
 * <ul>
 *   <li>Z=0 — full width×height water grid (background)</li>
 *   <li>Z=1 — grass island tiles from noise + radial falloff</li>
 * </ul>
 * <p>
 * After generating grass tiles, runs connected-component analysis to
 * detect individual islands. Any island with fewer than 5 grass tiles
 * is removed entirely. Surviving islands are exposed via {@link #getIslands()}.
 * <p>
 * All tunable parameters (frequency, octaves, radius, threshold) are
 * taken from GameConfig so you can tweak world generation without recompiling.
 */
public class IslandGenerator extends WorldGenerator {

    private static final int MIN_ISLAND_TILES = 5;

    private final double frequency;
    private final int octaves;
    private final double radiusFraction;
    private final double threshold;

    private List<IslandInfo> islands;

    public IslandGenerator() {
        this(0.04, 4, 0.45, 0.0);
    }

    public IslandGenerator(GameConfig config) {
        this(
            config.getNoiseFrequency(),
            config.getNoiseOctaves(),
            config.getIslandRadiusFraction(),
            config.getIslandThreshold()
        );
    }

    public IslandGenerator(double frequency, int octaves, double radiusFraction, double threshold) {
        this.frequency = frequency;
        this.octaves = octaves;
        this.radiusFraction = radiusFraction;
        this.threshold = threshold;
    }

    @Override
    public void generate(EntityManager em, int width, int height) {
        SimplexNoise noiseGen = new SimplexNoise(System.currentTimeMillis());

        float cx = width / 2f;
        float cy = height / 2f;
        double maxDist = Math.sqrt(cx * cx + cy * cy);
        double radius = maxDist * radiusFraction;
        double invRadius = 1.0 / radius;

        // Layer 0: full water grid
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                var e = em.createEntity();
                em.addComponent(e, new PositionComponent(x, y));
                em.addComponent(e, new ZComponent(0));
                em.addComponent(e, new TileComponent(TileType.WATER));
                em.addComponent(e, new AppearanceComponent(TileType.WATER.getSpriteId()));
            }
        }

        // Layer 1: grass island on top
        boolean[][] isGrass = new boolean[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                double n = noiseGen.fbm(x * frequency, y * frequency, octaves, 2.0, 0.5);
                double dx = x - cx;
                double dy = y - cy;
                double dist = Math.sqrt(dx * dx + dy * dy);
                double falloff = Math.max(0.0, 1.0 - dist * invRadius);
                double h = n * 0.5 + falloff * 1.0 - 0.25;

                if (h > threshold) {
                    var e = em.createEntity();
                    em.addComponent(e, new PositionComponent(x, y));
                    em.addComponent(e, new ZComponent(1));
                    em.addComponent(e, new TileComponent(TileType.GRASS));
                    em.addComponent(e, new AppearanceComponent(TileType.GRASS.getSpriteId()));
                    isGrass[x][y] = true;
                }
            }
        }

        // Connected-component analysis — find islands, remove small ones
        removeSmallIslands(em, width, height);
    }

    /**
     * Detect connected components of grass tiles (4-direction cardinal
     * adjacency). Islands with fewer than {@value #MIN_ISLAND_TILES} tiles
     * are deleted. Surviving islands are stored for later retrieval.
     * <p>
     * Builds the grass grid from the current EntityManager state, so this
     * can be called at any point in the pipeline (before or after smoothing).
     */
    public void removeSmallIslands(EntityManager em, int width, int height) {
        boolean[][] isGrass = buildGrassGrid(em, width, height);
        boolean[][] visited = new boolean[width][height];
        List<IslandInfo> detected = new ArrayList<>();
        int islandId = 0;

        // Cardinal BFS offsets
        int[] dx = { 0,  0, -1,  1};
        int[] dy = { 1, -1,  0,  0};

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (!isGrass[x][y] || visited[x][y]) continue;

                // BFS flood-fill to find this component
                List<int[]> componentTiles = new ArrayList<>();
                Queue<int[]> queue = new ArrayDeque<>();
                queue.add(new int[]{x, y});
                visited[x][y] = true;

                while (!queue.isEmpty()) {
                    int[] tile = queue.poll();
                    componentTiles.add(tile);

                    for (int d = 0; d < 4; d++) {
                        int nx = tile[0] + dx[d];
                        int ny = tile[1] + dy[d];
                        if (nx >= 0 && nx < width && ny >= 0 && ny < height
                                && isGrass[nx][ny] && !visited[nx][ny]) {
                            visited[nx][ny] = true;
                            queue.add(new int[]{nx, ny});
                        }
                    }
                }

                int size = componentTiles.size();
                if (size < MIN_ISLAND_TILES) {
                    // Delete all tiles in this tiny island
                    for (int[] tile : componentTiles) {
                        // Find and destroy the entity at this position
                        for (Entity entity : em.getAllEntitiesWith(
                                PositionComponent.class, TileComponent.class)) {
                            TileComponent tc = em.getComponent(entity, TileComponent.class);
                            if (tc.type != TileType.GRASS) continue;
                            PositionComponent pos = em.getComponent(entity, PositionComponent.class);
                            if (pos.x == tile[0] && pos.y == tile[1]) {
                                isGrass[tile[0]][tile[1]] = false;
                                em.destroyEntity(entity);
                                break;
                            }
                        }
                    }
                } else {
                    // Record the island
                    float sumX = 0, sumY = 0;
                    for (int[] tile : componentTiles) {
                        sumX += tile[0];
                        sumY += tile[1];
                    }
                    detected.add(new IslandInfo(
                        islandId++,
                        size,
                        sumX / size,
                        sumY / size
                    ));
                }
            }
        }

        this.islands = detected;
    }

    /** Build a grass boolean grid from the current EntityManager state. */
    private boolean[][] buildGrassGrid(EntityManager em, int width, int height) {
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

    @Override
    public List<IslandInfo> getIslands() {
        return islands != null ? islands : List.of();
    }
}
