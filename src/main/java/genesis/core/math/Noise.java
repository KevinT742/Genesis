package genesis.core.math;

import java.util.function.DoubleUnaryOperator;

/**
 * Gradient (Perlin style) noise used by every Genesis terrain model.
 *
 * <p>The implementation is stateless and allocation free in its hot paths: the gradient vectors are
 * derived directly from a hash of the lattice coordinate instead of a permutation table. That keeps
 * the generator reproducible across machines and cheap enough to call once per block column.
 *
 * <p>Two flavours are offered:
 * <ul>
 *   <li>{@link #perlin(double, double)} &mdash; plain gradient noise.</li>
 *   <li>{@link #perlinPeriodic(double, double, int, int)} &mdash; the same noise but with the
 *       lattice wrapped, which is what makes a planet seamless along its longitude.</li>
 * </ul>
 */
public final class Noise {

    private final long seed;

    public Noise(long seed) {
        this.seed = seed;
    }

    public long seed() {
        return seed;
    }

    /** Derives an independent noise field from this one. */
    public Noise fork(String channel) {
        return new Noise(Hash.derive(seed, channel));
    }

    // ---------------------------------------------------------------------
    // 2D
    // ---------------------------------------------------------------------

    /** Gradient noise in {@code [-1, 1]}. */
    public double perlin(double x, double y) {
        int x0 = fastFloor(x);
        int y0 = fastFloor(y);
        double fx = x - x0;
        double fy = y - y0;

        double n00 = grad2(Hash.mix2(seed, x0, y0), fx, fy);
        double n10 = grad2(Hash.mix2(seed, x0 + 1, y0), fx - 1.0, fy);
        double n01 = grad2(Hash.mix2(seed, x0, y0 + 1), fx, fy - 1.0);
        double n11 = grad2(Hash.mix2(seed, x0 + 1, y0 + 1), fx - 1.0, fy - 1.0);

        double u = fade(fx);
        double v = fade(fy);
        return lerp(lerp(n00, n10, u), lerp(n01, n11, u), v);
    }

    /**
     * Gradient noise whose lattice repeats every {@code periodX} / {@code periodY} cells.
     *
     * <p>This is the key to seamless planets: sampling along a closed longitude circle with a
     * periodic lattice makes the noise return to exactly the same value after a full revolution,
     * so terrain never shows a seam at the world boundary.
     */
    public double perlinPeriodic(double x, double y, int periodX, int periodY) {
        int x0 = fastFloor(x);
        int y0 = fastFloor(y);
        double fx = x - x0;
        double fy = y - y0;

        int px = Math.max(1, periodX);
        int py = Math.max(1, periodY);
        int xa = Math.floorMod(x0, px);
        int xb = Math.floorMod(x0 + 1, px);
        int ya = Math.floorMod(y0, py);
        int yb = Math.floorMod(y0 + 1, py);

        double n00 = grad2(Hash.mix2(seed, xa, ya), fx, fy);
        double n10 = grad2(Hash.mix2(seed, xb, ya), fx - 1.0, fy);
        double n01 = grad2(Hash.mix2(seed, xa, yb), fx, fy - 1.0);
        double n11 = grad2(Hash.mix2(seed, xb, yb), fx - 1.0, fy - 1.0);

        double u = fade(fx);
        double v = fade(fy);
        return lerp(lerp(n00, n10, u), lerp(n01, n11, u), v);
    }

    /** Fractal Brownian motion over {@link #perlin(double, double)}. */
    public double fbm(double x, double y, int octaves, double lacunarity, double gain) {
        double sum = 0.0;
        double amp = 1.0;
        double norm = 0.0;
        double freq = 1.0;
        for (int i = 0; i < octaves; i++) {
            sum += amp * perlin(x * freq, y * freq);
            norm += amp;
            amp *= gain;
            freq *= lacunarity;
        }
        return norm == 0.0 ? 0.0 : sum / norm;
    }

    /** Fractal Brownian motion over a periodic lattice. */
    public double fbmPeriodic(double x, double y, int octaves, int periodX, int periodY,
            double lacunarity, double gain) {
        double sum = 0.0;
        double amp = 1.0;
        double norm = 0.0;
        double freq = 1.0;
        int px = periodX;
        int py = periodY;
        for (int i = 0; i < octaves; i++) {
            sum += amp * perlinPeriodic(x * freq, y * freq, px, py);
            norm += amp;
            amp *= gain;
            freq *= lacunarity;
            px = Math.max(1, (int) Math.round(px * lacunarity));
            py = Math.max(1, (int) Math.round(py * lacunarity));
        }
        return norm == 0.0 ? 0.0 : sum / norm;
    }

    // ---------------------------------------------------------------------
    // 3D
    // ---------------------------------------------------------------------

    /** Gradient noise in {@code [-1, 1]}. */
    public double perlin3(double x, double y, double z) {
        int x0 = fastFloor(x);
        int y0 = fastFloor(y);
        int z0 = fastFloor(z);
        double fx = x - x0;
        double fy = y - y0;
        double fz = z - z0;

        double n000 = grad3(Hash.mix3(seed, x0, y0, z0), fx, fy, fz);
        double n100 = grad3(Hash.mix3(seed, x0 + 1, y0, z0), fx - 1.0, fy, fz);
        double n010 = grad3(Hash.mix3(seed, x0, y0 + 1, z0), fx, fy - 1.0, fz);
        double n110 = grad3(Hash.mix3(seed, x0 + 1, y0 + 1, z0), fx - 1.0, fy - 1.0, fz);
        double n001 = grad3(Hash.mix3(seed, x0, y0, z0 + 1), fx, fy, fz - 1.0);
        double n101 = grad3(Hash.mix3(seed, x0 + 1, y0, z0 + 1), fx - 1.0, fy, fz - 1.0);
        double n011 = grad3(Hash.mix3(seed, x0, y0 + 1, z0 + 1), fx, fy - 1.0, fz - 1.0);
        double n111 = grad3(Hash.mix3(seed, x0 + 1, y0 + 1, z0 + 1), fx - 1.0, fy - 1.0, fz - 1.0);

        double u = fade(fx);
        double v = fade(fy);
        double w = fade(fz);

        double x00 = lerp(n000, n100, u);
        double x10 = lerp(n010, n110, u);
        double x01 = lerp(n001, n101, u);
        double x11 = lerp(n011, n111, u);
        double y0v = lerp(x00, x10, v);
        double y1v = lerp(x01, x11, v);
        return lerp(y0v, y1v, w);
    }

    /** Fractal Brownian motion over {@link #perlin3(double, double, double)}. */
    public double fbm3(double x, double y, double z, int octaves, double lacunarity, double gain) {
        double sum = 0.0;
        double amp = 1.0;
        double norm = 0.0;
        double freq = 1.0;
        for (int i = 0; i < octaves; i++) {
            sum += amp * perlin3(x * freq, y * freq, z * freq);
            norm += amp;
            amp *= gain;
            freq *= lacunarity;
        }
        return norm == 0.0 ? 0.0 : sum / norm;
    }

    /** Ridged multifractal noise, produces sharp mountain crests. */
    public double ridged3(double x, double y, double z, int octaves, double lacunarity, double gain) {
        double sum = 0.0;
        double amp = 1.0;
        double norm = 0.0;
        double freq = 1.0;
        double weight = 1.0;
        for (int i = 0; i < octaves; i++) {
            double n = 1.0 - Math.abs(perlin3(x * freq, y * freq, z * freq));
            n *= n;
            n *= weight;
            weight = MathUtil.clamp(n * 2.0, 0.0, 1.0);
            sum += amp * n;
            norm += amp;
            amp *= gain;
            freq *= lacunarity;
        }
        return norm == 0.0 ? 0.0 : sum / norm;
    }

    /** Domain warps the sample position by another noise field. */
    public double warpedFbm3(double x, double y, double z, int octaves, double lacunarity, double gain,
            double strength) {
        Noise warp = fork("warp");
        double wx = warp.perlin3(x * 0.5, y * 0.5, z * 0.5);
        double wy = warp.perlin3(x * 0.5 + 31.4, y * 0.5 + 17.2, z * 0.5 + 9.1);
        double wz = warp.perlin3(x * 0.5 + 71.7, y * 0.5 + 53.9, z * 0.5 + 41.3);
        return fbm3(x + wx * strength, y + wy * strength, z + wz * strength, octaves, lacunarity, gain);
    }

    // ---------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------

    private static int fastFloor(double value) {
        int i = (int) value;
        return value < i ? i - 1 : i;
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double grad2(long hash, double dx, double dy) {
        int g = (int) (hash & 7L);
        switch (g) {
            case 0: return dx + dy;
            case 1: return dx - dy;
            case 2: return -dx + dy;
            case 3: return -dx - dy;
            case 4: return dx;
            case 5: return -dx;
            case 6: return dy;
            default: return -dy;
        }
    }

    private static double grad3(long hash, double dx, double dy, double dz) {
        int g = (int) (hash & 15L);
        switch (g) {
            case 0: return dx + dy;
            case 1: return -dx + dy;
            case 2: return dx - dy;
            case 3: return -dx - dy;
            case 4: return dx + dz;
            case 5: return -dx + dz;
            case 6: return dx - dz;
            case 7: return -dx - dz;
            case 8: return dy + dz;
            case 9: return -dy + dz;
            case 10: return dy - dz;
            case 11: return -dy - dz;
            case 12: return dx + dy;
            case 13: return -dx + dy;
            case 14: return -dy + dz;
            default: return -dy - dz;
        }
    }

    /** Applies an arbitrary shaping function to a raw noise value, clamped to {@code [0, 1]}. */
    public static double shape(double value, DoubleUnaryOperator shaper) {
        return MathUtil.clamp(shaper.applyAsDouble(value), 0.0, 1.0);
    }}
