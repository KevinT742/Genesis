package genesis.core.topology;

import genesis.api.planet.SamplingMode;
import genesis.core.math.MathUtil;

/**
 * The topology of a Genesis planet.
 *
 * <p>Genesis worlds are <em>cylinders wrapped around a sphere</em>:
 * <ul>
 *   <li>The <strong>longitude</strong> axis (X) is periodic. Walking east past {@code +halfSpan}
 *       continues at {@code -halfSpan} with exactly the same terrain, which is what produces the
 *       "the world has no edge" feeling the mod is built around.</li>
 *   <li>The <strong>latitude</strong> axis (Z) runs from one pole to the other and is mirrored at
 *       the poles, so travelling north past the northern pole brings you back down the very same
 *       meridian, exactly like folding a paper map over its edge.</li>
 * </ul>
 *
 * <p>Terrain noise is sampled on the resulting three dimensional surface. Because the longitude
 * axis maps onto a closed circle in three dimensions, the noise field is automatically seamless
 * across the world boundary &mdash; there is no stitching step and no visible seam.
 *
 * <p>Two sampling modes exist:
 * <ul>
 *   <li>{@link SamplingMode#CYLINDER} keeps the horizontal size of terrain features constant at
 *       every latitude. This is the gameplay friendly default.</li>
 *   <li>{@link SamplingMode#SPHERE} uses the true spherical geometry, so features shrink towards
 *       the poles exactly like they do on a real planet.</li>
 * </ul>
 */
public final class PlanetSurface {

    /** How terrain noise is projected onto the planet. */
    private final double halfSpan;
    private final double radius;
    private final SamplingMode samplingMode;

    /**
     * @param halfSpan     half the world extent along the latitude axis, in blocks
     * @param radius       the physical radius of the planet, in blocks
     * @param samplingMode how terrain noise is projected onto the surface
     */
    public PlanetSurface(double halfSpan, double radius, SamplingMode samplingMode) {
        if (halfSpan <= 0.0) {
            throw new IllegalArgumentException("halfSpan must be positive");
        }
        this.halfSpan = halfSpan;
        this.radius = radius;
        this.samplingMode = samplingMode;
    }

    public double halfSpan() {
        return halfSpan;
    }

    public double radius() {
        return radius;
    }

    public SamplingMode samplingMode() {
        return samplingMode;
    }

    /** The full length of one revolution around the planet, in blocks. */
    public double circumference() {
        return halfSpan * 2.0;
    }

    /**
     * Wraps a longitude coordinate into the periodic domain {@code [-halfSpan, halfSpan)}.
     *
     * <p>Block at {@code +halfSpan - 1} is directly adjacent to block at {@code -halfSpan}: that is
     * the continuous world edge the mod is built around.
     */
    public double wrapLongitude(double x) {
        return MathUtil.wrap(x, halfSpan);
    }

    /**
     * Mirrors a latitude coordinate back into {@code [-halfSpan, halfSpan]} when it runs past a
     * pole, keeping the longitude untouched so the terrain stays continuous.
     */
    public double foldLatitude(double z) {
        if (z > halfSpan) {
            return halfSpan - (z - halfSpan);
        }
        if (z < -halfSpan) {
            return -halfSpan - (z + halfSpan);
        }
        return z;
    }

    /** Latitude of a Z coordinate in radians, in {@code [-pi/2, pi/2]}. */
    public double latitudeOf(double z) {
        double folded = foldLatitude(z);
        return (Math.PI * 0.5) * (folded / halfSpan);
    }

    /** Longitude of an X coordinate in radians, in {@code [-pi, pi)}. */
    public double longitudeOf(double x) {
        double wrapped = wrapLongitude(x);
        return (2.0 * Math.PI) * (wrapped / circumference());
    }

    /**
     * Projects a flat world coordinate onto the planet surface.
     *
     * <p>The returned point lives in noise space and is what terrain functions are evaluated at.
     */
    public SurfacePoint toSurface(double x, double z) {
        double longitude = longitudeOf(x);
        double latitude = latitudeOf(z);
        boolean reflected = Math.abs(z) > halfSpan;

        double cosLat = Math.cos(latitude);
        double sinLat = Math.sin(latitude);
        double cosLon = Math.cos(longitude);
        double sinLon = Math.sin(longitude);

        if (samplingMode == SamplingMode.SPHERE) {
            return new SurfacePoint(radius * cosLat * cosLon, radius * sinLat, radius * cosLat * sinLon,
                    reflected);
        }
        return new SurfacePoint(radius * cosLon, radius * sinLat, radius * sinLon, reflected);
    }

    /**
     * How many blocks one unit of noise space covers along the longitude direction at the given
     * latitude.
     *
     * <p>In {@link SamplingMode#SPHERE} this shrinks towards the poles; in
     * {@link SamplingMode#CYLINDER} it stays constant.
     */
    public double horizontalScale(double z) {
        if (samplingMode == SamplingMode.CYLINDER) {
            return 1.0;
        }
        return Math.max(0.05, Math.cos(latitudeOf(z)));
    }

    /**
     * Converts a world distance along the longitude axis into a distance in noise space, so that
     * callers can keep feature sizes consistent regardless of the latitude.
     */
    public double toNoiseLongitude(double blocks) {
        return blocks / Math.max(1.0e-6, radius);
    }

    /** Converts a world distance along the latitude axis into a distance in noise space. */
    public double toNoiseLatitude(double blocks) {
        return blocks / Math.max(1.0e-6, radius);
    }
}
