package com.mycelbot.worldbase.engine.systems;

import com.mycelbot.worldbase.engine.TileType;
import com.mycelbot.worldbase.engine.components.AppearanceComponent;
import com.mycelbot.worldbase.engine.components.PositionComponent;
import com.mycelbot.worldbase.engine.components.TileComponent;
import com.mycelbot.worldbase.engine.components.ZComponent;
import com.mycelbot.worldbase.engine.ecs.EntityManager;
import com.mycelbot.worldbase.util.SimplexNoise;

/**
 * Generates a world of water with a noise-shaped island at the center.
 * <p>
 * Two render layers:
 * <ul>
 *   <li>Z=0 — full 100x100 water grid (background)</li>
 *   <li>Z=1 — grass island tiles from noise + radial falloff</li>
 * </ul>
 */
public class IslandGenerator extends WorldGenerator {

    private final double frequency;
    private final int octaves;
    private final double radiusFraction;
    private final double threshold;

    public IslandGenerator() {
        this(0.04, 4, 0.45, 0.0);
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
                }
            }
        }
    }
}
