package genesis.api.magic;

/**
 * How strong the magic of a place is.
 *
 * <p>Magic is free floating in the ground of every region, and the amount of it differs from place
 * to place. The tiers name those differences so that recipes, monsters and structures can ask for
 * "a place with strong magic" instead of a number.
 */
public enum MagicTier {
    /** No magic at all. */
    NONE(0.0),
    /** A trace: barely enough to notice. */
    TRACE(0.2),
    /** Faint: a wizard could work here, but not comfortably. */
    FAINT(0.45),
    /** Moderate: the normal state of a good region. */
    MODERATE(0.8),
    /** Strong: magic comes easily here. */
    STRONG(1.3),
    /** Extreme: the ground itself is saturated with magic. */
    EXTREME(2.0);

    private final double lowerBound;

    MagicTier(double lowerBound) {
        this.lowerBound = lowerBound;
    }

    /** The concentration at which this tier starts. */
    public double lowerBound() {
        return lowerBound;
    }

    /** The tier a concentration of magic falls into. */
    public static MagicTier of(double concentration) {
        MagicTier tier = NONE;
        for (MagicTier candidate : values()) {
            if (concentration >= candidate.lowerBound) {
                tier = candidate;
            }
        }
        return tier;
    }

    /** Whether a concentration of this tier reaches the given one. */
    public boolean reaches(MagicTier required) {
        return required == null || ordinal() >= required.ordinal();
    }
}
