package genesis.core.magic;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import genesis.api.magic.MagicConcentration;
import genesis.api.magic.MagicInfluence;
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
 *
 * <p>The list of influences can be handed in from the outside. That is how the persistence layer
 * works: the world stores the influences as plain data and gives them back to the field when the
 * world is loaded, and the field writes them straight into that list while the world is played.
 */
public final class MagicField {

    /**
     * Notified whenever the magic of the world changes.
     *
     * <p>The persistence layer uses this to know that the world has to be written to disk.
     */
    @FunctionalInterface
    public interface ChangeListener {
        /** Called after the magic of the world has changed. */
        void onChanged(MagicField field);
    }

    private final MagicFieldModel model;
    private final List<MagicInfluence> influences;
    private volatile ChangeListener listener = field -> {
    };

    public MagicField(MagicFieldModel model) {
        this(model, new CopyOnWriteArrayList<>());
    }

    /**
     * Creates a field that writes its influences into an existing list.
     *
     * <p>The list is used as it is, it is not copied. Passing the list of a stored world here
     * means that a change to the magic is a change to the stored world.
     *
     * @param model   the distribution of the magic over the planet
     * @param backing the list the influences are kept in
     */
    public MagicField(MagicFieldModel model, List<MagicInfluence> backing) {
        if (model == null) {
            throw new IllegalArgumentException("A magic field needs a model");
        }
        if (backing == null) {
            throw new IllegalArgumentException("A magic field needs a list to keep its influences in");
        }
        this.model = model;
        this.influences = backing;
    }

    /** The distribution the field was built from. */
    public MagicFieldModel model() {
        return model;
    }

    /** The concentration of the ground at a position, including every outside influence. */
    public double concentrationAt(double x, double z) {
        double value = model.baseConcentrationAt(x, z);
        for (MagicInfluence influence : influences) {
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
        influences.add(new MagicInfluence(x, z, radius, delta));
        listener.onChanged(this);
    }

    /** Every outside influence that was applied to the field. */
    public List<MagicInfluence> influences() {
        return List.copyOf(influences);
    }

    /** Replaces every outside influence of the field. */
    public void setInfluences(List<MagicInfluence> replacement) {
        influences.clear();
        influences.addAll(replacement);
        listener.onChanged(this);
    }

    /** Undoes every outside influence. */
    public void clearInfluences() {
        influences.clear();
        listener.onChanged(this);
    }

    /** Sets the listener that is told when the magic of the world changes. */
    public void setChangeListener(ChangeListener value) {
        this.listener = value == null ? field -> {
        } : value;
    }
}
