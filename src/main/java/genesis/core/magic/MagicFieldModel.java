package genesis.core.magic;

import genesis.api.magic.MagicConcentration;
import genesis.api.magic.MagicTier;
import genesis.api.planet.AtmosphereSpec;
import genesis.api.planet.PlanetDefinition;
import genesis.core.math.Noise;
import genesis.core.topology.PlanetSurface;
import genesis.core.topology.SurfacePoint;

/**
 * The distribution of free floating magic over a planet.
 *
 * <p>Magic is not a property of the world but of the ground: every region of a planet carries its
 * own concentration, and the concentration decides what can be done there. A region with a strong
 * concentration is a place where magic can be gathered easily, where a potion can be brewed and
 * where a creature with a magic organ grows strong.
 *
 * <p>The field is built from two layers:
 * <ul>
 *   <li>a <strong>distribution</strong> of large regions that are richer or poorer in magic,</li>
 *   <li><strong>veins</strong>, the ridges where magic pools and runs through the ground like
 *       water through an aquifer.</li>
 * </ul>
 *
 * <p>Both layers are evaluated on the surface of the planet, so the field is seamless across the
 * world boundary: a place has the same concentration no matter whether it is approached from the
 * east or from the west.
 */
public final class MagicFieldModel {

    /** The concentration of a world of average richness, in magic units per block. */
    public static final double STANDARD_BASELINE = 0.85;

    private final PlanetSurface surface;
    private final double baseline;
    private final Noise distribution;
    private final Noise veins;

    public MagicFieldModel(PlanetDefinition planet, long seed) {
        this.surface = new PlanetSurface(planet.halfSpan(), planet.radius(), planet.samplingMode());
        this.baseline = baselineOf(planet);
        this.distribution = new Noise(seed).fork("magic_distribution");
        this.veins = new Noise(seed).fork("magic_veins");
    }

    /**
     * The concentration a whole planet has before it is divided into regions.
     *
     * <p>A thick, wet atmosphere carries more magic than a thin, dry one, which is why the desert
     * world Ember is a poor place for magic and the home world is a good one.
     */
    public static double baselineOf(PlanetDefinition planet) {
        AtmosphereSpec air = planet.atmosphere();
        double atmosphere = 0.55 + 0.45 * Math.min(2.0, air.density());
        double wetness = 0.7 + 0.3 * Math.max(0.0, Math.min(1.0, air.precipitation()));
        return STANDARD_BASELINE * atmosphere * wetness;
    }

    /** The concentration the planet has on average. */
    public double baseline() {
        return baseline;
    }

    /**
     * The concentration of the ground at a position, before any outside influence.
     *
     * <p>The result is never negative: a region can be poor in magic, but it cannot owe magic.
     */
    public double baseConcentrationAt(double x, double z) {
        SurfacePoint point = surface.toSurface(x, z);
        double broad = distribution.fbm3(point.x() / 2400.0, point.y() / 2400.0, point.z() / 2400.0, 4, 2.0, 0.5);
        double region = baseline * (0.8 + 0.6 * broad);

        double ridge = veins.ridged3(point.x() / 850.0, point.y() / 850.0, point.z() / 850.0, 3, 2.0, 0.5);
        double vein = ridge > 0.72 ? (ridge - 0.72) / 0.28 : 0.0;
        return Math.max(0.0, region * (1.0 + 2.4 * vein * vein));
    }

    /**
     * The concentration at a height above sea level.
     *
     * <p>Magic thins out when the air gets thin and pools under the ground, so a deep valley is
     * richer in magic than a high mountain.
     */
    public double concentrationAt(double x, double z, double altitude) {
        double base = baseConcentrationAt(x, z);
        double factor;
        if (altitude >= 0.0) {
            factor = 1.0 / (1.0 + altitude / 420.0);
        } else {
            factor = 1.0 + Math.min(0.6, -altitude / 600.0);
        }
        return base * factor;
    }

    /** The concentration of the ground at a position. */
    public MagicConcentration concentration(double x, double z) {
        return new MagicConcentration(baseConcentrationAt(x, z));
    }

    /** The strength of the magic of a region. */
    public MagicTier tierAt(double x, double z) {
        return concentration(x, z).tier();
    }
}
