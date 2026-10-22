package genesis.api.magic;

/**
 * What an item is, as far as the magic of Genesis is concerned.
 *
 * <p>Magic in Genesis always comes from the same place: the magic that is free floating in the
 * ground of a region. Everything else is a way of <em>using</em> that magic:
 *
 * <ul>
 *   <li>A <strong>魔药</strong> is magic that was <em>solidified</em>: the free magic of a region was
 *       taken up by a material and kept there.</li>
 *   <li><strong>炼金</strong> and <strong>附魔</strong> are solidified <em>ways of channeling</em>
 *       magic: a recipe and an enchantment are instructions that survive being written down.</li>
 *   <li>A <strong>魔咒</strong> is a way of channeling magic that is <em>not</em> solidified: it is
 *       spoken, it needs a living speaker and it is gone the moment it is cast.</li>
 * </ul>
 *
 * <p>This enum says which of those an item is, which is what the brewing and the alchemy recipes
 * of the mod family look at.
 */
public enum MagicInput {
    /** An ordinary item that has nothing to do with magic. */
    NONE,
    /** A 魔力材料: a thing without life that adapted well enough to magic to carry an effect. */
    MATERIAL,
    /** A 魔力器官: the part of a living being that manipulates magic, including a human soul. */
    ORGAN,
    /** Solidified magic: the essence a potion is made of. */
    ESSENCE,
    /** A solidified way of channeling magic: a scroll, a tome, a rune tablet. */
    CHANNEL;

    /** Whether an item of this kind can be used as an ingredient of a potion or an elixir. */
    public boolean isBrewable() {
        return this == MATERIAL || this == ORGAN || this == ESSENCE;
    }

    /** Whether an item of this kind carries an effect of its own. */
    public boolean carriesEffect() {
        return this == MATERIAL || this == ESSENCE;
    }
}
