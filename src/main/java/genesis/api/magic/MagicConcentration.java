package genesis.api.magic;

/**
 * The amount of free floating magic in a place.
 *
 * <p>Every region of a world carries its own concentration, and the concentration is what decides
 * whether magic can be gathered there at all: a potion that can be brewed in a strong region simply
 * cannot be brewed in a dead one.
 *
 * @param value the concentration, in magic units per block
 */
public record MagicConcentration(double value) {

    public MagicConcentration {
        value = Math.max(0.0, value);
    }

    /** The strength of the magic of this place. */
    public MagicTier tier() {
        return MagicTier.of(value);
    }

    /** Whether the concentration is at least the given one. */
    public boolean isAtLeast(double required) {
        return value >= required;
    }

    /**
     * How much of a requirement the concentration meets, between {@code 0} and {@code 1}.
     *
     * <p>A place that is exactly as strong as the requirement gives {@code 1}, a place that is twice
     * as strong still gives {@code 1}: a stronger place is not more useful than a strong enough
     * one, it only works faster.
     */
    public double coverage(double required) {
        if (required <= 0.0) {
            return 1.0;
        }
        return Math.min(1.0, value / required);
    }

    /** How fast magic can be gathered here, compared to a place of normal strength. */
    public double gatheringSpeed() {
        return Math.min(4.0, value / MagicTier.MODERATE.lowerBound());
    }
}
