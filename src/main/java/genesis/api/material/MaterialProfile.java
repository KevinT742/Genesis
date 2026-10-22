package genesis.api.material;

/**
 * The unified description of a material.
 *
 * <p>Every block and every item in a Genesis world, vanilla or added by a member of the mod family,
 * is described by one of these. Bundling physics, chemistry and magic into a single record means
 * that a new block only has to state what makes it different, and everything else &mdash; mining
 * speed, explosion resistance, flammability, how heavy it is, what it is made of, whether a rune can
 * be carved into it &mdash; follows from the same rules.
 *
 * @param id       the identifier of the material, usually {@code namespace:name}
 * @param physical the physical properties
 * @param chemical the chemical properties
 * @param magic    the magical properties
 */
public record MaterialProfile(
        String id,
        PhysicalProperties physical,
        ChemicalProperties chemical,
        MagicProperties magic) {

    public MaterialProfile {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("A material needs an id");
        }
        physical = physical == null ? MaterialFamily.rock("stone").physical() : physical;
        chemical = chemical == null ? MaterialFamily.rock("stone").chemical() : chemical;
        magic = magic == null ? MaterialFamily.rock("stone").magic() : magic;
    }

    /** The mining hardness derived from the physical properties. */
    public float miningHardness() {
        return (float) physical.hardness();
    }

    /** The explosion resistance derived from the physical properties. */
    public float explosionResistance() {
        return (float) Math.min(1200.0, physical.toughness() * 5.0);
    }

    /** The light the block emits. */
    public int lightEmission() {
        return physical.lightEmission();
    }

    /** The mass of one unit of this material, in kilograms. */
    public double mass() {
        return physical.mass();
    }

    /** The volume of one unit of this material, in cubic metres. */
    public double volume() {
        return physical.volume();
    }

    /** The density of this material, in kilograms per cubic metre. */
    public double density() {
        return physical.density();
    }

    /** The state this material is in at the given ambient temperature. */
    public Phase phaseAt(genesis.api.physical.Temperature ambient) {
        return physical.phaseAt(ambient);
    }

    /** The chemical formula of this material. */
    public String formula() {
        return chemical.formula();
    }

    /** The molar mass of this material, in grams per mole. */
    public double molarMass() {
        return chemical.molarMass();
    }

    /** Whether the block needs the correct tool to drop anything. */
    public boolean requiresCorrectTool() {
        return physical.hardness() >= 1.5;
    }

    /** Whether the block can be used as a magical catalyst. */
    public boolean isCatalyst() {
        return magic.isCatalyst();
    }

    /** Whether the material is a 魔力材料: a thing without life that carries an effect. */
    public boolean isMagicMaterial() {
        return magic.isMagicMaterial();
    }

    /** Whether the material can guide magic in a controlled way. */
    public boolean isChanneler() {
        return magic.isChanneler();
    }

    /** How much magic this material can hold in solid form, in mana units. */
    public double solidifiedMagic() {
        return magic.solidifiedMagic();
    }

    /** Returns a copy of this profile with a different identifier. */
    public MaterialProfile withId(String newId) {
        return new MaterialProfile(newId, physical, chemical, magic);
    }

    /**
     * Returns a copy of this profile that describes a different <em>amount</em> of the material.
     *
     * <p>The material itself does not change: a lump of iron and a bar of iron are the same iron.
     * Only the volume changes, and the mass follows from it, exactly as it does in the real world.
     * This is what lets one profile describe both a block of a material and a single item of it.
     *
     * @param volume the volume of one unit, in cubic metres
     */
    public MaterialProfile withVolume(double volume) {
        return new MaterialProfile(id, physical.withVolume(volume), chemical, magic);
    }
}
