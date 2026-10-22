package genesis.api.planet;

/**
 * How terrain noise is projected onto the surface of a planet.
 */
public enum SamplingMode {

    /**
     * The horizontal size of a terrain feature stays constant at every latitude. Every block of the
     * world covers the same amount of ground, which keeps gameplay balanced from pole to pole.
     */
    CYLINDER,

    /**
     * True spherical geometry. Terrain features shrink towards the poles exactly like they do on a
     * real planet, which gives a stronger sense of standing on a globe at the cost of gameplay
     * consistency.
     */
    SPHERE
}
