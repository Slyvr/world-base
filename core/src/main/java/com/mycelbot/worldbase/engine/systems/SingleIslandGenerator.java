package com.mycelbot.worldbase.engine.systems;

import com.mycelbot.worldbase.config.GameConfig;
import com.mycelbot.worldbase.engine.SingleIsland;
import com.mycelbot.worldbase.util.SimplexNoise;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * Generates a single island from noise + radial falloff, then smooths
 * its edges (perpendicular-edge rule) and trims to its bounding box.
 * <p>
 * The result has a known rectangular size ({@link SingleIsland#width},
 * {@link SingleIsland#height}) and total tile count, making it easy to
 * compose multiple islands in a larger world without overlap.
 */
public class SingleIslandGenerator {

    private final double frequency;
    private final int octaves;
    private final double radiusFraction;
    private final double threshold;

    public SingleIslandGenerator(GameConfig config) {
        this(
            config.getNoiseFrequency(),
            config.getNoiseOctaves(),
            config.getIslandRadiusFraction(),
            config.getIslandThreshold()
        );
    }

    public SingleIslandGenerator(double frequency, int octaves,
                                  double radiusFraction, double threshold) {
        this.frequency = frequency;
        this.octaves = octaves;
        this.radiusFraction = radiusFraction;
        this.threshold = threshold;
    }

    /**
     * Generate a single smoothed island within a {@code genSize × genSize}
     * grid, then trim it to its bounding box.
     *
     * @param seed    RNG seed for noise
     * @param genSize size of the generation grid (island scales with this)
     * @return the island, already smoothed and trimmed
     */
    public SingleIsland generate(long seed, int genSize) {
        SimplexNoise noiseGen = new SimplexNoise(seed);

        float cx = genSize / 2f;
        float cy = genSize / 2f;
        double maxDist = Math.sqrt(cx * cx + cy * cy);
        double radius = maxDist * radiusFraction;
        double invRadius = 1.0 / radius;

        boolean[][] raw = new boolean[genSize][genSize];

        for (int x = 0; x < genSize; x++) {
            for (int y = 0; y < genSize; y++) {
                double n = noiseGen.fbm(x * frequency, y * frequency,
                                        octaves, 2.0, 0.5);
                double dx = x - cx;
                double dy = y - cy;
                double dist = Math.sqrt(dx * dx + dy * dy);
                double falloff = Math.max(0.0, 1.0 - dist * invRadius);
                double h = n * 0.5 + falloff * 1.0 - 0.25;

                if (h > threshold) {
                    raw[x][y] = true;
                }
            }
        }

        // Smooth edges with perpendicular-edge rule
        boolean[][] smoothed = smooth(raw, genSize, genSize);

        // Find all connected components (4-direction), keep only the largest
        boolean[][] visited = new boolean[genSize][genSize];
        int[] dx = { 0,  0, -1,  1};
        int[] dy = { 1, -1,  0,  0};

        List<int[]> largestComponent = new ArrayList<>();

        for (int x = 0; x < genSize; x++) {
            for (int y = 0; y < genSize; y++) {
                if (!smoothed[x][y] || visited[x][y]) continue;

                // BFS this component
                List<int[]> component = new ArrayList<>();
                Queue<int[]> queue = new ArrayDeque<>();
                queue.add(new int[]{x, y});
                visited[x][y] = true;

                while (!queue.isEmpty()) {
                    int[] tile = queue.poll();
                    component.add(tile);

                    for (int d = 0; d < 4; d++) {
                        int nx = tile[0] + dx[d];
                        int ny = tile[1] + dy[d];
                        if (nx >= 0 && nx < genSize && ny >= 0 && ny < genSize
                                && smoothed[nx][ny] && !visited[nx][ny]) {
                            visited[nx][ny] = true;
                            queue.add(new int[]{nx, ny});
                        }
                    }
                }

                if (component.size() > largestComponent.size()) {
                    largestComponent = component;
                }
            }
        }

        int count = largestComponent.size();

        // Defensive: empty island
        if (count == 0) {
            return new SingleIsland(new boolean[1][1], 1, 1, 0);
        }

        // Find bounding box of the largest component only
        int minX = genSize, minY = genSize, maxX = 0, maxY = 0;
        for (int[] tile : largestComponent) {
            if (tile[0] < minX) minX = tile[0];
            if (tile[0] > maxX) maxX = tile[0];
            if (tile[1] < minY) minY = tile[1];
            if (tile[1] > maxY) maxY = tile[1];
        }

        // Trim to bounding box — only largest component's tiles kept
        boolean[][] grid = new boolean[genSize][genSize];
        for (int[] tile : largestComponent) {
            grid[tile[0]][tile[1]] = true;
        }

        int bw = maxX - minX + 1;
        int bh = maxY - minY + 1;
        boolean[][] trimmed = new boolean[bw][bh];
        for (int x = minX; x <= maxX; x++) {
            System.arraycopy(grid[x], minY, trimmed[x - minX], 0, bh);
        }

        return new SingleIsland(trimmed, bw, bh, count);
    }

    /**
     * Apply the perpendicular-edge survival rule iteratively until stable.
     * A grass tile survives only if it has grass neighbours in at least
     * two perpendicular cardinal directions (e.g. top+right).
     */
    private boolean[][] smooth(boolean[][] grid, int w, int h) {
        boolean cur[][] = grid;
        boolean changed = true;

        while (changed) {
            changed = false;
            boolean[][] next = new boolean[w][h];

            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    if (!cur[x][y]) continue;

                    boolean top  = y + 1 < h && cur[x][y + 1];
                    boolean bot  = y - 1 >= 0 && cur[x][y - 1];
                    boolean left = x - 1 >= 0 && cur[x - 1][y];
                    boolean right = x + 1 < w && cur[x + 1][y];

                    boolean valid = (top && left) || (top && right)
                                 || (bot && left) || (bot && right);

                    if (valid) {
                        next[x][y] = true;
                    } else {
                        changed = true;
                    }
                }
            }

            cur = next;
        }

        return cur;
    }
}
