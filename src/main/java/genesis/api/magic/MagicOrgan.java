package genesis.api.magic;

import genesis.api.material.ElementAffinity;

/**
 * A magic organ: the part of a living being that manipulates magic.
 *
 * <p>Animals and plants carry organs just as human beings do, and every organ is also an
 * ingredient: a harvested organ is one of the two kinds of magic material that brewing and alchemy
 * accept, the other being the magic materials that never were alive.
 *
 * <p>A human soul is an organ like any other, only a much stronger one.
 *
 * @param id           a stable identifier, usually {@code namespace:name}
 * @param displayName  a human readable name
 * @param host         who carries the organ
 * @param kind         what sort of organ this is
 * @param capacity     how much magic the organ can hold, in mana units
 * @param efficiency   how well the organ guides magic, {@code 0} to {@code 1}
 * @param adaptability how well the organ takes magic up, {@code 0} to {@code 1}
 * @param affinity     which element the organ answers to
 */
public record MagicOrgan(
        String id,
        String displayName,
        MagicHost host,
        MagicOrganKind kind,
        double capacity,
        double efficiency,
        double adaptability,
        ElementAffinity affinity) {

    public MagicOrgan {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("A magic organ needs an id");
        }
        if (host == null) {
            host = MagicHost.NONE;
        }
        if (kind == null) {
            kind = MagicOrganKind.ORGAN;
        }
        if (affinity == null) {
            affinity = ElementAffinity.ARCANE;
        }
        displayName = displayName == null ? id : displayName;
        capacity = Math.max(0.0, capacity);
        efficiency = clamp(efficiency);
        adaptability = clamp(adaptability);
    }

    private static double clamp(double value) {
        return value < 0.0 ? 0.0 : (value > 1.0 ? 1.0 : value);
    }

    /** Whether the organ is still part of a living being. */
    public boolean isAttached() {
        return host.isLiving();
    }

    /** Whether this is a human soul. */
    public boolean isSoul() {
        return kind == MagicOrganKind.SOUL;
    }

    /** Whether the organ can be used as an ingredient of a potion or an elixir. */
    public boolean isBrewable() {
        return capacity > 0.0 && adaptability >= 0.3;
    }

    /** How much usable magic the organ produces, in mana units. */
    public double output() {
        return capacity * efficiency * adaptability;
    }

    /** The soul of a human being: strong, adaptable and able to do everything an organ can do. */
    public static MagicOrgan soul(String id, double capacity) {
        return new MagicOrgan(id, id, MagicHost.HUMAN, MagicOrganKind.SOUL, capacity, 0.95, 0.9,
                ElementAffinity.ARCANE);
    }

    /** An organ of an animal. */
    public static MagicOrgan animal(String id, MagicOrganKind kind, double capacity) {
        return new MagicOrgan(id, id, MagicHost.ANIMAL, kind, capacity, 0.6, 0.55, ElementAffinity.ORGANIC);
    }

    /** An organ of a plant. */
    public static MagicOrgan plant(String id, MagicOrganKind kind, double capacity) {
        return new MagicOrgan(id, id, MagicHost.PLANT, kind, capacity, 0.5, 0.7, ElementAffinity.ORGANIC);
    }
}
