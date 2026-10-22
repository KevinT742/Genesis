package genesis.api.material;

/**
 * The magical properties of a material.
 *
 * <p>The magic of Genesis is not a property of the world but a property of <em>everything that can
 * carry it</em>. Magic itself is free floating energy that is present in the ground of every
 * region; what makes a thing magical is how well it adapts to that energy:
 *
 * <ul>
 *   <li><strong>适应度 (adaptability)</strong> &mdash; how well the material takes magic up. A
 *       material with a high adaptability and a real effect is a <em>魔力材料</em>, the living free
 *       ingredient of every potion, elixir and enchanted work.</li>
 *   <li><strong>引导效率 (channel efficiency)</strong> &mdash; how well the material lets magic
 *       pass through it in a controlled way. This is what a rune, a focusing lens or an organ is
 *       made of.</li>
 *   <li><strong>留存 (retention)</strong> &mdash; how well the material keeps magic that was put
 *       into it. A potion is magic that was solidified, so retention is what decides whether a
 *       recipe can be bottled at all.</li>
 * </ul>
 *
 * <p>The remaining values describe the character of the magic itself: which element it answers to,
 * how much of it the material can hold, how disordered it is, which frequency it resonates at and
 * how well it is bound to its element.
 *
 * @param affinity          the element the material is attuned to
 * @param manaCapacity      how much magical energy the material can hold, in mana units
 * @param entropy           how disordered the material is, which drives random behaviour
 * @param resonance         the resonant frequency of the material, used to match magical tools
 * @param stability         how resistant the material is to change and corruption, {@code 0} to {@code 1}
 * @param attunement        how strongly the material is bound to its affinity
 * @param adaptability      how well the material takes magic up, {@code 0} to {@code 1}
 * @param channelEfficiency how well the material guides magic, {@code 0} to {@code 1}
 * @param retention         how well the material keeps the magic it was given, {@code 0} to {@code 1}
 */
public record MagicProperties(
        ElementAffinity affinity,
        double manaCapacity,
        double entropy,
        double resonance,
        double stability,
        double attunement,
        double adaptability,
        double channelEfficiency,
        double retention) {

    /** The adaptability a material needs before it counts as a magic material. */
    public static final double MAGIC_MATERIAL_THRESHOLD = 0.55;

    public MagicProperties {
        if (affinity == null) {
            affinity = ElementAffinity.EARTH;
        }
        stability = clamp(stability);
        attunement = clamp(attunement);
        adaptability = clamp(adaptability);
        channelEfficiency = clamp(channelEfficiency);
        retention = clamp(retention);
        manaCapacity = Math.max(0.0, manaCapacity);
    }

    private static double clamp(double value) {
        return value < 0.0 ? 0.0 : (value > 1.0 ? 1.0 : value);
    }

    /** Whether the material reacts with the given element. */
    public boolean reactsWith(ElementAffinity other) {
        return other != null && affinity.isReactive() && affinity.opposite() == other;
    }

    /** Whether the block can be used as a magical catalyst. */
    public boolean isCatalyst() {
        return manaCapacity >= 32.0 && attunement >= 0.6;
    }

    /**
     * Whether the material is a <em>魔力材料</em>: a thing without life that adapted well enough
     * to magic to carry an effect of its own.
     */
    public boolean isMagicMaterial() {
        return adaptability >= MAGIC_MATERIAL_THRESHOLD && manaCapacity > 0.0;
    }

    /** Whether the material can guide magic in a controlled way. */
    public boolean isChanneler() {
        return channelEfficiency > 0.3;
    }

    /** Whether the material can hold magic that was put into it. */
    public boolean isRetaining() {
        return retention > 0.25;
    }

    /**
     * How much magic this material can hold in solid form, in mana units.
     *
     * <p>This is the number that decides whether a potion can be brewed from it: the magic has to
     * be taken up by the material and then kept there.
     */
    public double solidifiedMagic() {
        return manaCapacity * adaptability * retention;
    }
}
