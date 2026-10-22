package genesis.api.magic;

/**
 * One outside operation on the magic of a region.
 *
 * <p>A ritual, a sacrifice or a drained leech does not change the magic of the world directly:
 * it applies an <em>influence</em> to a region, and the influence raises or lowers the
 * concentration of the ground around its centre. The change falls off towards the edge of the
 * region, so a ritual affects its circle and not the whole world.
 *
 * <p>An influence is plain data, which is what makes it possible to write the magic of a world
 * to disk. The format it is written in lives in the Minecraft layer, so that this contract stays
 * free of any dependency on the game.
 *
 * @param x      the centre of the area, in blocks
 * @param z      the centre of the area, in blocks
 * @param radius how far the operation reaches, in blocks
 * @param delta  how much magic the operation adds or takes away
 */
public record MagicInfluence(double x, double z, double radius, double delta) {

    public MagicInfluence {
        radius = Math.max(0.0, radius);
    }

    /** How much this influence changes the concentration at a position. */
    public double contributionAt(double pointX, double pointZ) {
        if (radius <= 0.0) {
            return 0.0;
        }
        double distance = Math.hypot(pointX - x, pointZ - z);
        if (distance >= radius) {
            return 0.0;
        }
        double falloff = 1.0 - distance / radius;
        return delta * falloff * falloff;
    }
}
