package genesis.core.terrain;

import genesis.api.planet.PlanetDefinition;
import genesis.api.planet.TerrainProfile;
import genesis.core.math.MathUtil;
import genesis.core.math.Noise;
import genesis.core.math.Spline;
import genesis.core.topology.PlanetSurface;
import genesis.core.topology.SurfacePoint;

/**
 * The terrain model of a planet.
 *
 * <p>Elevation is assembled from four independent layers, each of which can be tuned per planet
 * through a {@link TerrainProfile}:
 * <ol>
 *   <li><strong>Continentalness</strong> decides where land and ocean are.</li>
 *   <li><strong>Erosion</strong> decides how worn down the land is; eroded regions are flat,
 *       uneroded regions are rugged.</li>
 *   <li><strong>Ridges</strong> raise mountain chains, but only inland where the continent is
 *       thick enough to support them.</li>
 *   <li><strong>Detail</strong> adds the small scale roughness that makes the ground look natural
 *       instead of mathematical.</li>
 * </ol>
 *
 * <p>All layers are evaluated on the surface of the planet, so the resulting terrain is seamless
 * across the world boundary by construction.
 */
public final class TerrainModel {

    private final PlanetSurface surface;
    private final TerrainProfile profile;
    private final Noise continentalNoise;
    private final Noise erosionNoise;
    private final Noise ridgeNoise;
    private final Noise detailNoise;
    private final Noise plateauNoise;
    private final Spline continentalSpline;
    private final Spline erosionSpline;
    private final Spline ridgeSpline;

    /** The wavelength of the landmass layer, in blocks. */
    private final double continentalWavelength;
    /** The wavelength of the erosion layer, in blocks. */
    private final double erosionWavelength;
    /** The wavelength of the mountain layer, in blocks. */
    private final double ridgeWavelength;
    /** The wavelength of the plateau layer, in blocks. */
    private final double plateauWavelength;
    /** The wavelength of the surface detail layer, in blocks. */
    private final double detailWavelength;
    /** The wavelength of the medium scale roughness that the water system works on, in blocks. */
    private final double roughnessWavelength;

    public TerrainModel(long seed, PlanetDefinition planet) {
        this(planet, seed);
    }

    public TerrainModel(PlanetDefinition planet, long seed) {
        this.surface = new PlanetSurface(planet.halfSpan(), planet.radius(), planet.samplingMode());
        this.profile = planet.terrain();
        this.continentalNoise = new Noise(seed).fork("continental");
        this.erosionNoise = new Noise(seed).fork("erosion");
        this.ridgeNoise = new Noise(seed).fork("ridge");
        this.detailNoise = new Noise(seed).fork("detail");
        this.plateauNoise = new Noise(seed).fork("plateau");
        this.continentalSpline = profile.continentalSpline();
        this.erosionSpline = profile.erosionSpline();
        this.ridgeSpline = profile.ridgeSpline();

        double base = Math.max(64.0, profile.continentalFrequency());
        this.continentalWavelength = base;
        this.erosionWavelength = base * 0.45;
        this.ridgeWavelength = base * 0.35;
        this.plateauWavelength = base * 1.2;
        this.detailWavelength = Math.max(1.0, profile.detailFrequency());
        this.roughnessWavelength = Math.max(48.0, base * 0.08);
    }

    public PlanetSurface surface() {
        return surface;
    }

    public TerrainProfile profile() {
        return profile;
    }

    public double seaLevel() {
        return profile.seaLevel();
    }

    /**
     * Samples the raw terrain, before rivers and lakes are carved in.
     */
    public TerrainSample sample(double x, double z) {
        SurfacePoint point = surface.toSurface(x, z);

        // Every layer is sampled on the surface of the planet, divided by its wavelength. That way
        // one unit of noise always covers the same number of blocks, no matter where on the planet
        // the position is.
        double continentalRaw = continentalNoise.fbm3(
                point.x() / continentalWavelength, point.y() / continentalWavelength,
                point.z() / continentalWavelength, 5, 2.0, 0.5);
        double continental = continentalSpline.apply(continentalRaw);

        double erosionRaw = erosionNoise.fbm3(
                point.x() / erosionWavelength, point.y() / erosionWavelength, point.z() / erosionWavelength,
                4, 2.0, 0.5);
        double erosion = erosionSpline.apply(erosionRaw);

        double ridgeRaw = ridgeNoise.ridged3(
                point.x() / ridgeWavelength, point.y() / ridgeWavelength, point.z() / ridgeWavelength,
                5, 2.1, 0.5);
        double ridge = ridgeSpline.apply(ridgeRaw);

        double height = profile.continentalAmplitude() * profile.heightVariance();

        // The landmass value is relative to the sea level: zero is the coastline, positive is land
        // and negative is ocean.
        double relative = continental * height;

        // Erosion wears the land down. The factor is strongest where erosion is high.
        double erosionFactor = 1.0 - profile.erosionAmplitude() * (1.0 - erosion);
        if (relative > 0.0) {
            relative *= erosionFactor;
        }

        // Mountain chains only form on thick continents.
        double inland = Math.max(0.0, continental);
        relative += ridge * profile.ridgeAmplitude() * Math.pow(inland, profile.ridgeSharpness());

        // Plateaus flatten broad areas.
        double plateau = plateauNoise.fbm3(
                point.x() / plateauWavelength, point.y() / plateauWavelength, point.z() / plateauWavelength,
                3, 2.0, 0.5);
        double plateauMask = MathUtil.smoothstep(plateau * 0.5 + 0.5);
        double plateauFactor = 1.0 - plateauMask * profile.plateauFlatness() * 0.45;
        if (relative > 0.0) {
            relative *= plateauFactor;
        }

        double detail = detailNoise.fbm3(
                point.x() / detailWavelength, point.y() / detailWavelength, point.z() / detailWavelength,
                4, 2.0, 0.5);
        relative += detail * profile.detailAmplitude();

        double sea = profile.seaLevel();
        double finalHeight = sea + relative;
        return new TerrainSample(finalHeight, continentalRaw, erosion, ridge, finalHeight < sea);
    }

    /**
     * The coarse elevation used by the hydrology model.
     *
     * <p>Hydrology runs on a grid, so it uses a lower frequency version of the terrain. This keeps
     * rivers aligned with the large scale shape of the land without paying for the full detail
     * noise on every cell.
     */
    public double coarseElevation(int blockX, int blockZ) {
        SurfacePoint point = surface.toSurface(blockX, blockZ);
        double continentalRaw = continentalNoise.fbm3(
                point.x() / continentalWavelength, point.y() / continentalWavelength,
                point.z() / continentalWavelength, 4, 2.0, 0.5);
        double continental = continentalSpline.apply(continentalRaw);
        double erosionRaw = erosionNoise.fbm3(
                point.x() / erosionWavelength, point.y() / erosionWavelength, point.z() / erosionWavelength,
                3, 2.0, 0.5);
        double erosion = erosionSpline.apply(erosionRaw);

        double relative = continental * profile.continentalAmplitude() * profile.heightVariance();
        if (relative > 0.0) {
            relative *= 1.0 - profile.erosionAmplitude() * (1.0 - erosion);
        }
        double inland = Math.max(0.0, continental);
        relative += ridgeSpline.apply(ridgeNoise.ridged3(
                point.x() / ridgeWavelength, point.y() / ridgeWavelength, point.z() / ridgeWavelength,
                4, 2.1, 0.5)) * profile.ridgeAmplitude() * Math.pow(inland, profile.ridgeSharpness());

        // A medium scale roughness that is not part of the fine terrain. The water system runs on
        // this coarse elevation, and without it the valleys would be too smooth to ever hold a lake.
        double roughness = detailNoise.fbm3(
                point.x() / roughnessWavelength, point.y() / roughnessWavelength, point.z() / roughnessWavelength,
                3, 2.0, 0.5);
        relative += roughness * profile.detailAmplitude() * 0.8;

        return profile.seaLevel() + relative;
    }

    /** The latitude of a position in radians, used by the climate model. */
    public double latitudeOf(double z) {
        return surface.latitudeOf(z);
    }
}
