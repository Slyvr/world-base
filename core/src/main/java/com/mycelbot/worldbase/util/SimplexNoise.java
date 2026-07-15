package com.mycelbot.worldbase.util;

/**
 * 2D simplex noise implementation.
 * <p>
 * Public domain — based on Stefan Gustavson's implementation.
 * Output range is approximately [-1, 1].
 */
public class SimplexNoise {

    private static final int[] grad2 = new int[]{
         1,  1, -1,  1,  1, -1, -1, -1,
         1,  0, -1,  0,  0,  1,  0, -1
    };

    private static final int[] perm = new int[512];
    private static final double F2 = 0.5 * (Math.sqrt(3.0) - 1.0);
    private static final double G2 = (3.0 - Math.sqrt(3.0)) / 6.0;

    static {
        // Initialize permutation table from a fixed seed for reproducibility
        int[] p = new int[256];
        for (int i = 0; i < 256; i++) p[i] = i;
        // Shuffle with a fixed seed so islands are reproducible
        java.util.Random rng = new java.util.Random(42);
        for (int i = 255; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int tmp = p[i]; p[i] = p[j]; p[j] = tmp;
        }
        for (int i = 0; i < 512; i++) perm[i] = p[i & 255];
    }

    private static int fastFloor(double x) {
        int xi = (int) x;
        return x < xi ? xi - 1 : xi;
    }

    private static double dot2(int[] g, double x, double y) {
        return g[0] * x + g[1] * y;
    }

    /** 2D simplex noise at the given coordinates. Returns value in [-1, 1]. */
    public static double noise(double x, double y) {
        double s = (x + y) * F2;
        int i = fastFloor(x + s);
        int j = fastFloor(y + s);
        double t = (i + j) * G2;
        double xx = i - t;
        double yy = j - t;
        double x0 = x - xx;
        double y0 = y - yy;

        int i1, j1;
        if (x0 > y0) { i1 = 1; j1 = 0; }
        else         { i1 = 0; j1 = 1; }

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
        if (t0 >= 0) {
            t0 *= t0;
            n0 = t0 * t0 * dot2(new int[]{grad2[gi0 * 2], grad2[gi0 * 2 + 1]}, x0, y0);
        }

        double t1 = 0.5 - x1 * x1 - y1 * y1;
        if (t1 >= 0) {
            t1 *= t1;
            n1 = t1 * t1 * dot2(new int[]{grad2[gi1 * 2], grad2[gi1 * 2 + 1]}, x1, y1);
        }

        double t2 = 0.5 - x2 * x2 - y2 * y2;
        if (t2 >= 0) {
            t2 *= t2;
            n2 = t2 * t2 * dot2(new int[]{grad2[gi2 * 2], grad2[gi2 * 2 + 1]}, x2, y2);
        }

        return 70.0 * (n0 + n1 + n2);
    }

    /**
     * Multi-octave fractal noise (fBm).
     *
     * @param x       X coordinate
     * @param y       Y coordinate
     * @param octaves Number of octaves (more = finer detail)
     * @param lacunarity Frequency multiplier per octave (typically ~2.0)
     * @param gain     Amplitude multiplier per octave (typically ~0.5)
     */
    public static double fbm(double x, double y, int octaves, double lacunarity, double gain) {
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
