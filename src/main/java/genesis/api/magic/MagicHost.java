package genesis.api.magic;

/**
 * Who carries a magic organ.
 *
 * <p>Everything alive has an organ that manipulates magic. Animals and plants have theirs in a
 * visible part of the body; a human has the soul, which is the organ of every human magic.
 */
public enum MagicHost {
    /** Nothing carries it: a harvested organ whose host is already gone. */
    NONE,
    /** An animal. */
    ANIMAL,
    /** A plant. */
    PLANT,
    /** A human being, including players, villagers and witches. */
    HUMAN;

    /** Whether the host is alive. */
    public boolean isLiving() {
        return this != NONE;
    }
}
