package genesis.core.math;

/**
 * Small numeric helpers shared by the terrain, hydrology and ecology models.
 */
public final class MathUtil {

    private MathUtil() {
    }

    public static double clamp(double value, double min, double max) {
        return value < min ? min : (value > max ? max : value);
    }

    public static int clamp(int value, int min, int max) {
        return value < min ? min : (value > max ? max : value);
    }

    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    /** Inverse of {@link #lerp(double, double, double)}. */
    public static double inverseLerp(double a, double b, double value) {
        return a == b ? 0.0 : (value - a) / (b - a);
    }

    /** Hermite smoothstep, clamped to {@code [0, 1]}. */
    public static double smoothstep(double t) {
        double x = clamp(t, 0.0, 1.0);
        return x * x * (3.0 - 2.0 * x);
    }

    /** Quintic smootherstep, clamped to {@code [0, 1]}. */
    public static double smootherstep(double t) {
        double x = clamp(t, 0.0, 1.0);
        return x * x * x * (x * (x * 6.0 - 15.0) + 10.0);
    }

    /** Rounds to the nearest integer, halfway cases away from zero. */
    public static int round(double value) {
        return (int) Math.floor(value + 0.5);
    }

    /** Fractional remainder that always stays in {@code [0, m)}. */
    public static double floorMod(double value, double m) {
        double r = value % m;
        return r < 0.0 ? r + m : r;
    }

    /** Integer modulo that always stays in {@code [0, m)}. */
    public static int floorMod(int value, int m) {
        int r = value % m;
        return r < 0 ? r + m : r;
    }

    /** Wraps a value into the half open interval {@code [-half, half)}. */
    public static double wrap(double value, double half) {
        double span = half * 2.0;
        double r = floorMod(value + half, span);
        return r - half;
    }

    /** Wraps an integer into the half open interval {@code [-half, half)}. */
    public static int wrap(int value, int half) {
        int span = half * 2;
        int r = floorMod(value + half, span);
        return r - half;
    }

    /** Smooth minimum, useful for blending terrain features without creases. */
    public static double smoothMin(double a, double b, double k) {
        double h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);
        return lerp(b, a, h) - k * h * (1.0 - h);
    }

    /** Smooth maximum, the dual of {@link #smoothMin(double, double, double)}. */
    public static double smoothMax(double a, double b, double k) {
        return -smoothMin(-a, -b, k);
    }
}
