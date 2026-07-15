package com.mycelbot.worldbase.engine.systems;

import com.mycelbot.worldbase.engine.TileType;
import com.mycelbot.worldbase.engine.components.AppearanceComponent;
import com.mycelbot.worldbase.engine.components.PositionComponent;
import com.mycelbot.worldbase.engine.components.TileComponent;
import com.mycelbot.worldbase.engine.ecs.EntityManager;
import com.mycelbot.worldbase.util.SimplexNoise;

/**
 * Generates a world of water with a noise-shaped island at the center.
 * <p>
 * Uses 2D simplex noise (fBm) combined with a radial center falloff to
 * create a natural-looking island with an irregular shoreline.
 * A new random seed is used each generation so every run produces
 * a different island shape.
 * <p>
 * Tuning parameters control island size, noise frequency, and shoreline
 * roughness — all configurable via constructor.
 */
public class IslandGenerator extends WorldGenerator {

    /** Base noise frequency — lower = larger noise features. */
    private final double frequency;

    /** Number of noise octaves — higher = more detail. */
    private final int octaves;

    /** Radius of the island as a fraction of the half-world diagonal. */
    private final double radiusFraction;

    /** Noise amplitude threshold for land vs water. */
    private final double threshold;

    public IslandGenerator() {
        this(0.04, 4, 0.45, 0.0);
    }

    /**
     * @param frequency       Base noise frequency (lower = broader features)
     * @param octaves         fBm octaves (more = finer shoreline detail)
     * @param radiusFraction  Island radius as fraction of half-diagonal (0..1)
     * @param threshold       Noise cutoff — values above this become land
     */
    public IslandGenerator(double frequency, int octaves, double radiusFraction, double threshold) {
        this.frequency = frequency;
        this.octaves = octaves;
        this.radiusFraction = radiusFraction;
        this.threshold = threshold;
    }

    @Override
    public void generate(EntityManager em, int width, int height) {
        // Random seed per generation so every run produces a different island
        SimplexNoise noiseGen = new SimplexNoise(System.currentTimeMillis());

        float cx = width / 2f;
        float cy = height / 2f;
        double maxDist = Math.sqrt(cx * cx + cy * cy);
        double radius = maxDist * radiusFraction;
        double invRadius = 1.0 / radius;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                var entity = em.createEntity();
                em.addComponent(entity, new PositionComponent(x, y));

                // Sample noise
                double n = noiseGen.fbm(x * frequency, y * frequency, octaves, 2.0, 0.5);

                // Radial falloff: 1 at center, 0 at radius, negative beyond
                double dx = x - cx;
                double dy = y - cy;
                double dist = Math.sqrt(dx * dx + dy * dy);
                double falloff = Math.max(0.0, 1.0 - dist * invRadius);

                // Combined height: noise with center bias, then threshold
                double h = n * 0.5 + falloff * 1.0 - 0.25;

                TileType type = h > threshold ? TileType.GRASS : TileType.WATER;

                em.addComponent(entity, new TileComponent(type));
                em.addComponent(entity, new AppearanceComponent(type.getSpriteId()));
            }
        }
    }
}
