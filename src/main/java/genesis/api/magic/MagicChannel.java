package genesis.api.magic;

/**
 * A way of channeling magic.
 *
 * <p>A channel is the second half of the magic model. Magic is free floating energy in the ground
 * of a region; a channel is the shape in which that energy is moved, controlled and given an
 * effect. A channel can be <em>solidified</em> &mdash; written into a scroll, a recipe or an
 * enchantment &mdash; or it can be <em>free</em>, which is what a spoken spell is.
 *
 * @param id          a stable identifier, usually {@code namespace:name}
 * @param displayName a human readable name
 * @param kind        what sort of channel this is
 * @param efficiency  how much of the magic that is fed in comes out as an effect, {@code 0} to {@code 1}
 * @param precision   how precisely the effect is shaped, {@code 0} to {@code 1}
 * @param cost        how much magic one use costs, in mana units
 * @param range       how far the effect reaches, in blocks
 * @param solidified  whether the channel survives being written down
 */
public record MagicChannel(
        String id,
        String displayName,
        MagicChannelKind kind,
        double efficiency,
        double precision,
        double cost,
        double range,
        boolean solidified) {

    public MagicChannel {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("A magic channel needs an id");
        }
        if (kind == null) {
            kind = MagicChannelKind.SPELL;
        }
        displayName = displayName == null ? id : displayName;
        efficiency = clamp(efficiency);
        precision = clamp(precision);
        cost = Math.max(0.0, cost);
        range = Math.max(0.0, range);
    }

    private static double clamp(double value) {
        return value < 0.0 ? 0.0 : (value > 1.0 ? 1.0 : value);
    }

    /** Whether the channel is spoken rather than written down, like a spell. */
    public boolean isFree() {
        return !solidified;
    }

    /** Whether the channel survives being written down. */
    public boolean isSolidified() {
        return solidified;
    }

    /** Whether the channel needs a living being to be carried out. */
    public boolean needsSpeaker() {
        return kind.needsLivingSpeaker() || isFree();
    }

    /** How much usable effect one unit of magic produces through this channel. */
    public double throughput() {
        return efficiency * precision;
    }

    /** Whether the channel is worth its cost at the given concentration of the region. */
    public boolean isViableAt(double concentration, double requiredConcentration) {
        return concentration >= requiredConcentration;
    }

    /** A solidified alchemical channel. */
    public static MagicChannel alchemy(String id, double efficiency, double precision, double cost) {
        return new MagicChannel(id, id, MagicChannelKind.ALCHEMY, efficiency, precision, cost, 0.0, true);
    }

    /** A solidified enchanting channel. */
    public static MagicChannel enchantment(String id, double efficiency, double precision, double cost) {
        return new MagicChannel(id, id, MagicChannelKind.ENCHANTMENT, efficiency, precision, cost, 0.0, true);
    }

    /** A spoken spell. */
    public static MagicChannel spell(String id, double efficiency, double precision, double cost, double range) {
        return new MagicChannel(id, id, MagicChannelKind.SPELL, efficiency, precision, cost, range, false);
    }

    /** A ritual that needs a place to be carried out. */
    public static MagicChannel rite(String id, double efficiency, double precision, double cost, double range) {
        return new MagicChannel(id, id, MagicChannelKind.RITE, efficiency, precision, cost, range, true);
    }
}
