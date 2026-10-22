package genesis.api.material;

/**
 * The elemental affinity of a material.
 *
 * <p>This is the Minecraft flavoured half of the material model. Affinities drive how a block
 * interacts with the magic systems of the mod family: which blocks an enchantment reacts to, which
 * blocks a rune can be carved into and how strongly a block resists magical corruption.
 */
public enum ElementAffinity {
    /** Stone, metal and earth. Stable, slow, dependable. */
    EARTH,
    /** Water, ice and everything that flows. */
    WATER,
    /** Fire, lava and everything that burns. */
    FIRE,
    /** Air, wind and light things. */
    AIR,
    /** Light, glass, glowstone and radiant materials. */
    LIGHT,
    /** Shadow, obsidian and things that swallow light. */
    DARK,
    /** Arcane, runic and enchanted materials. */
    ARCANE,
    /** The void, the end and materials that exist outside space. */
    VOID,
    /** Metals and conductors. */
    METAL,
    /** Crystals and resonant materials. */
    CRYSTAL,
    /** Organic material: wood, leaves, flesh. */
    ORGANIC;

    /** The opposite affinity of an element, used for magical reactions. */
    public ElementAffinity opposite() {
        return switch (this) {
            case EARTH -> AIR;
            case WATER -> FIRE;
            case FIRE -> WATER;
            case AIR -> EARTH;
            case LIGHT -> DARK;
            case DARK -> LIGHT;
            case ARCANE -> VOID;
            case VOID -> ARCANE;
            case METAL -> CRYSTAL;
            case CRYSTAL -> METAL;
            case ORGANIC -> FIRE;
        };
    }

    /** Whether the affinity reacts strongly with its opposite. */
    public boolean isReactive() {
        return this != ORGANIC;
    }
}
