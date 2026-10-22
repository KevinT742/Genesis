package genesis.api.magic;

/**
 * The kind of organ that manipulates magic.
 *
 * <p>The organ decides what the organ is good for. A root takes magic up from the ground, a leaf
 * makes magic out of light, a vein moves it through the body and the soul of a human does all of
 * it at once.
 */
public enum MagicOrganKind {
    /** 灵魂: the soul of a human being. The strongest organ there is. */
    SOUL,
    /** 内脏: the organ that mixes magic with the blood of an animal. */
    ORGAN,
    /** 叶: a leaf that makes magic out of light. */
    LEAF,
    /** 根: a root that draws magic up from the ground. */
    ROOT,
    /** 眼: an eye that sees magic. */
    EYE,
    /** 脉: a vein that carries magic through a body. */
    VEIN,
    /** 骨: a bone that stores magic. */
    BONE,
    /** 角: a horn that releases magic. */
    HORN;

    /** Whether this organ takes magic up from the ground of the region. */
    public boolean isAbsorbing() {
        return this == ROOT || this == ORGAN || this == BONE || this == SOUL;
    }

    /** Whether this organ turns something else into magic. */
    public boolean isProducing() {
        return this == LEAF || this == HORN || this == SOUL;
    }

    /** Whether this organ only observes. */
    public boolean isPerceiving() {
        return this == EYE;
    }
}
