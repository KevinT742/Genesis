package genesis.core.math;

import java.util.ArrayList;
import java.util.List;

/**
 * A monotone piecewise linear curve, used to shape terrain parameters (continentalness, erosion,
 * hydrology) the same way vanilla Minecraft shapes its own noise splines.
 *
 * <p>Control points are supplied as {@code (input, output)} pairs and are sorted on construction.
 */
public final class Spline {

    private final double[] xs;
    private final double[] ys;

    public Spline(double[] points) {
        if (points.length < 2 || points.length % 2 != 0) {
            throw new IllegalArgumentException("Spline needs at least one (x, y) pair");
        }
        int count = points.length / 2;
        List<double[]> sorted = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            sorted.add(new double[] {points[i * 2], points[i * 2 + 1]});
        }
        sorted.sort((a, b) -> Double.compare(a[0], b[0]));
        this.xs = new double[count];
        this.ys = new double[count];
        for (int i = 0; i < count; i++) {
            this.xs[i] = sorted.get(i)[0];
            this.ys[i] = sorted.get(i)[1];
        }
    }

    /** Evaluates the curve, clamping outside the defined domain. */
    public double apply(double input) {
        if (input <= xs[0]) {
            return ys[0];
        }
        int last = xs.length - 1;
        if (input >= xs[last]) {
            return ys[last];
        }
        int i = 0;
        while (i < last && input > xs[i + 1]) {
            i++;
        }
        double span = xs[i + 1] - xs[i];
        double t = span == 0.0 ? 0.0 : (input - xs[i]) / span;
        return ys[i] + (ys[i + 1] - ys[i]) * t;
    }

    /** Builds a curve from an even length array of alternating x and y values. */
    public static Spline of(double... points) {
        return new Spline(points);
    }

    /** A flat curve, useful as a neutral element when combining curves. */
    public static Spline constant(double value) {
        return new Spline(new double[] {0.0, value, 1.0, value});
    }
}
