package genesis.core.magic;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import genesis.api.magic.MagicConcentration;
import genesis.api.magic.MagicTier;

/**
 * The magic of a world: the concentration of the ground, plus everything that was done to it.
 *
 * <p>The field is created while the world is generated, because that is when the seed is known.
 * Afterwards the world is not read only: a ritual, a sacrifice or a drained leech can change the
 * magic of a region, and the change stays until the world ends.
 *
 * <p>The influences are kept in a copy on write list: they are written rarely and read often, and
 * the concentration is asked for by every chunk that is generated.
 */
public final class MagicField {

    private final MagicFieldModel model;
    private final List<Influence> influences = new CopyOnWriteArrayList<>();

    public MagicField(MagicFieldModel model) {
        if (model == null) {
            throw new IllegalArgumentException("A magic field needs a model");
        }
        this.model = model;
    }

    /** The distribution the field was built from. */
    public MagicFieldModel model() {
        return model;
    }

    /** The concentration of the ground at a position, including every outside influence. */
    public double concentrationAt(double x, double z) {
        double value = model.baseConcentrationAt(x, z);
        for (Influence influence : influences) {
            value += influence.contributionAt(x, z);
        }
        return Math.max(0.0, value);
    }

    /** The concentration of the ground at a position. */
    public MagicConcentration concentration(double x, double z) {
        return new MagicConcentration(concentrationAt(x, z));
    }

    /** The strength of the magic of a region. */
    public MagicTier tierAt(double x, double z) {
        return concentration(x, z).tier();
    }

    /**
     * Changes the magic of a region from the outside.
     *
     * <p>A positive delta feeds the ground, a negative one drains it. The change falls off towards
     * the edge of the area, so a ritual affects its circle and not the whole world.
     */
    public void influence(double x, double z, double radius, double delta) {
        if (radius <= 0.0 || delta == 0.0) {
            return;
        }
        influences.add(new Influence(x, z, radius, delta));
    }

    /** Every outside influence that was applied to the field. */
    public List<Influence> influences() {
        return List.copyOf(influences);
    }

    /** Undoes every outside influence. */
    public void clearInfluences() {
        influences.clear();
    }

    /**
     * One outside operation on the magic of a region.
     *
     * @param x      the centre of the area, in blocks
     * @param z      the centre of the area, in blocks
     * @param radius how far the operation reaches, in blocks
     * @param delta  how much magic the operation adds or takes away
     */
    public record Influence(double x, double z, double radius, double delta) {

        public Influence {
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
}
