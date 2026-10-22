package genesis.api.magic;

import genesis.api.material.ElementAffinity;
import genesis.api.material.MaterialProfile;

/**
 * A magic material: a thing without life that adapted well enough to magic to carry an effect.
 *
 * <p>Magic materials are the second half of what brewing and alchemy accept. The first half are
 * magic organs, which carry the life of their host; a magic material carries no life but takes up
 * magic readily and keeps it, which is what makes it a good vessel for an effect.
 *
 * @param id          a stable identifier, usually {@code namespace:name}
 * @param displayName a human readable name
 * @param adaptability how well the material takes magic up, {@code 0} to {@code 1}
 * @param storedPower how much magic the material carries, in mana units
 * @param decay       how much of that magic is lost per use, in mana units
 * @param affinity    which element the material answers to
 */
public record MagicMaterial(
        String id,
        String displayName,
        double adaptability,
        double storedPower,
        double decay,
        ElementAffinity affinity) {

    public MagicMaterial {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("A magic material needs an id");
        }
        if (affinity == null) {
            affinity = ElementAffinity.ARCANE;
        }
        displayName = displayName == null ? id : displayName;
        adaptability = clamp(adaptability);
        storedPower = Math.max(0.0, storedPower);
        decay = Math.max(0.0, decay);
    }

    private static double clamp(double value) {
        return value < 0.0 ? 0.0 : (value > 1.0 ? 1.0 : value);
    }

    /**
     * Reads a magic material out of a material profile.
     *
     * <p>The profile knows how well the material adapts to magic and how much magic it can hold;
     * how fast that magic fades is a property of the material and not of the profile.
     */
    public static MagicMaterial of(String id, MaterialProfile profile, double decay) {
        return new MagicMaterial(id, id, profile.magic().adaptability(), profile.magic().solidifiedMagic(), decay,
                profile.magic().affinity());
    }

    /** Whether the material can be used as an ingredient of a potion or an elixir. */
    public boolean isBrewable() {
        return adaptability >= 0.55 && storedPower > 0.0;
    }

    /** How many uses the material survives before its magic is gone. */
    public int uses() {
        if (decay <= 0.0) {
            return storedPower > 0.0 ? Integer.MAX_VALUE : 0;
        }
        return (int) Math.floor(storedPower / decay);
    }

    /** How much magic is left after the material was used a number of times. */
    public double powerAfter(int uses) {
        return Math.max(0.0, storedPower - decay * uses);
    }

    /** The strength of the magic the material carries. */
    public MagicTier tier() {
        return MagicTier.of(storedPower);
    }
}
