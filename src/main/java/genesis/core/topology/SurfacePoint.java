package genesis.core.topology;

/**
 * A point on the surface of a planet, expressed in the three dimensional space used for noise
 * sampling.
 *
 * @param x        the first noise space coordinate
 * @param y        the second noise space coordinate (the polar axis)
 * @param z        the third noise space coordinate
 * @param reflected whether the source latitude was beyond a pole and therefore mirrored
 */
public record SurfacePoint(double x, double y, double z, boolean reflected) {

    /** Euclidean distance from the origin. */
    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    /** Returns this point scaled to the given radius. */
    public SurfacePoint scaledTo(double radius) {
        double len = length();
        if (len == 0.0) {
            return new SurfacePoint(0.0, 0.0, 0.0, reflected);
        }
        double k = radius / len;
        return new SurfacePoint(x * k, y * k, z * k, reflected);
    }
}
