package com.mycelbot.worldbase.util;

import java.util.Random;

/**
 * 2D simplex noise implementation with configurable seed.
 * <p>
 * Public domain — based on Stefan Gustavson's implementation.
 * Output range is approximately [-1, 1].
 */
public class SimplexNoise {

    private static final int[] grad2 = new int[]{
         1,  1, -1,  1,  1, -1, -1, -1,
         1,  0, -1,  0,  0,  1,  0, -1
    };

    private final int[] perm;
    private static final double F2 = 0.5 * (Math.sqrt(3.0) - 1.0);
    private static final double G2 = (3.0 - Math.sqrt(3.0)) / 6.0;

    /**
     * Create a simplex noise instance with the given seed.
     * Different seeds produce entirely different noise fields.
     */
    public SimplexNoise(long seed) {
        this.perm = new int[512];
        int[] p = new int[256];
        for (int i = 0; i < 256; i++) p[i] = i;
        Random rng = new Random(seed);
        for (int i = 255; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int tmp = p[i]; p[i] = p[j]; p[j] = tmp;
        }
        for (int i = 0; i < 512; i++) perm[i] = p[i & 255];
    }

    private int fastFloor(double x) {
        int xi = (int) x;
        return x < xi ? xi - 1 : xi;
    }

    private double dot2(int gi, double x, double y) {
        return grad2[gi * 2] * x + grad2[gi * 2 + 1] * y;
    }

    /** 2D simplex noise at the given coordinates. Returns value in [-1, 1]. */
    public double noise(double x, double y) {
        double s = (x + y) * F2;
        int i = fastFloor(x + s);
        int j = fastFloor(y + s);
        double t = (i + j) * G2;
        double x0 = x - (i - t);
        double y0 = y - (j - t);

        int i1 = x0 > y0 ? 1 : 0;
        int j1 = x0 > y0 ? 0 : 1;

        double x1 = x0 - i1 + G2;
        double y1 = y0 - j1 + G2;
        double x2 = x0 - 1.0 + 2.0 * G2;
        double y2 = y0 - 1.0 + 2.0 * G2;

        int ii = i & 255;
        int jj = j & 255;
        int gi0 = perm[ii + perm[jj]] % 8;
        int gi1 = perm[ii + i1 + perm[jj + j1]] % 8;
        int gi2 = perm[ii + 1 + perm[jj + 1]] % 8;

        double n0 = 0, n1 = 0, n2 = 0;

        double t0 = 0.5 - x0 * x0 - y0 * y0;
        if (t0 >= 0) { t0 *= t0; n0 = t0 * t0 * dot2(gi0, x0, y0); }

        double t1 = 0.5 - x1 * x1 - y1 * y1;
        if (t1 >= 0) { t1 *= t1; n1 = t1 * t1 * dot2(gi1, x1, y1); }

        double t2 = 0.5 - x2 * x2 - y2 * y2;
        if (t2 >= 0) { t2 *= t2; n2 = t2 * t2 * dot2(gi2, x2, y2); }

        return 70.0 * (n0 + n1 + n2);
    }

    /**
     * Multi-octave fractal noise (fBm).
     *
     * @param x       X coordinate
     * @param y       Y coordinate
     * @param octaves Number of octaves
     * @param lacunarity Frequency multiplier per octave (typically ~2.0)
     * @param gain     Amplitude multiplier per octave (typically ~0.5)
     */
    public double fbm(double x, double y, int octaves, double lacunarity, double gain) {
        double value = 0.0;
        double amplitude = 1.0;
        double frequency = 1.0;
        double maxValue = 0.0;

        for (int i = 0; i < octaves; i++) {
            value += amplitude * noise(x * frequency, y * frequency);
            maxValue += amplitude;
            amplitude *= gain;
            frequency *= lacunarity;
        }
        return value / maxValue;
    }
}
