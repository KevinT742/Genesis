package genesis.api.magic;

/**
 * The kinds of way of channeling magic.
 *
 * <p>Every kind can be solidified or spoken. Alchemy and enchanting are normally written down and
 * therefore survive without a speaker; a spell is normally spoken and therefore needs a living one.
 */
public enum MagicChannelKind {
    /** 炼金: turns materials into other materials by moving magic through them. */
    ALCHEMY,
    /** 附魔: binds magic to a thing so that the thing gains a property. */
    ENCHANTMENT,
    /** 魔咒: releases magic in one breath. */
    SPELL,
    /** 仪式: needs a place, a circle and time. */
    RITE;

    /** Whether this kind of channel is normally written down instead of spoken. */
    public boolean isNormallySolidified() {
        return this == ALCHEMY || this == ENCHANTMENT || this == RITE;
    }

    /** Whether this kind of channel needs a living speaker. */
    public boolean needsLivingSpeaker() {
        return this == SPELL;
    }
}
