package genesis.api.material;

import genesis.api.physical.Temperature;

/**
 * The physical properties of a material.
 *
 * <p>These are the values that decide how a block behaves in the world: how hard it is to break,
 * how much force it survives, how well it conducts heat, when it melts and how slippery it is.
 *
 * <p>The mass of a unit is not written down separately: it follows from the density and the volume
 * of the unit, exactly as it does in the real world. A block of stone occupies one cubic metre and
 * weighs as much as 2600 kilogrammes of stone, while a filled bottle of potion weighs half a
 * kilogramme.
 *
 * <p>All temperatures are thermodynamic, in kelvin.
 *
 * @param density             the mass per cubic metre, in kilograms
 * @param volume              the volume of one unit, in cubic metres; one block is one cubic metre
 * @param hardness            the Mohs hardness, also used as the mining hardness
 * @param toughness           how much damage the block absorbs before breaking
 * @param elasticity          how much the block springs back, {@code 0} to {@code 1}
 * @param thermalConductivity how well heat passes through, in watts per metre kelvin
 * @param specificHeat        the energy needed to raise one kilogram by one kelvin
 * @param meltingPoint        the temperature at which the block turns into a liquid
 * @param boilingPoint        the temperature at which the liquid turns into a gas
 * @param friction            how much the block slows down entities walking on it
 * @param lightEmission       how much light the block emits, {@code 0} to {@code 15}
 */
public record PhysicalProperties(
        double density,
        double volume,
        double hardness,
        double toughness,
        double elasticity,
        double thermalConductivity,
        double specificHeat,
        Temperature meltingPoint,
        Temperature boilingPoint,
        double friction,
        int lightEmission) {

    /** The temperature below which a material is considered to melt in a reachable world. */
    private static final double REACHABLE_MELTING = 3731.15;

    /** The temperature below which a material is considered to boil in a reachable world. */
    private static final double REACHABLE_BOILING = 5731.15;

    public PhysicalProperties {
        if (density < 0.0) {
            throw new IllegalArgumentException("density must not be negative");
        }
        if (volume <= 0.0) {
            throw new IllegalArgumentException("volume must be positive");
        }
        if (hardness < 0.0) {
            throw new IllegalArgumentException("hardness must not be negative");
        }
        if (lightEmission < 0 || lightEmission > 15) {
            throw new IllegalArgumentException("lightEmission must be between 0 and 15");
        }
        if (meltingPoint != null && boilingPoint != null && boilingPoint.isBelow(meltingPoint)) {
            throw new IllegalArgumentException("the boiling point cannot be below the melting point");
        }
    }

    /** The mass of one unit, in kilograms. */
    public double mass() {
        return density * volume;
    }

    /**
     * Returns the same material in a different amount.
     *
     * <p>Only the volume changes; the density and every other property stay the same, so the mass
     * of the unit follows from the density and the new volume.
     *
     * @param newVolume the volume of one unit, in cubic metres
     */
    public PhysicalProperties withVolume(double newVolume) {
        return new PhysicalProperties(density, newVolume, hardness, toughness, elasticity,
                thermalConductivity, specificHeat, meltingPoint, boilingPoint, friction, lightEmission);
    }

    /** How much heat one unit absorbs when its temperature rises by one kelvin, in joules. */
    public double thermalMass() {
        return mass() * specificHeat;
    }

    /** How well the material survives an explosion, derived from its toughness. */
    public double blastResistance() {
        return toughness;
    }

    /** Whether the material melts into a liquid at a reachable temperature. */
    public boolean isMelting() {
        return meltingPoint != null && meltingPoint.kelvin() > 0.0 && meltingPoint.kelvin() < REACHABLE_MELTING;
    }

    /** Whether the material boils into a gas at a reachable temperature. */
    public boolean isBoiling() {
        return boilingPoint != null && boilingPoint.kelvin() > 0.0 && boilingPoint.kelvin() < REACHABLE_BOILING;
    }

    /** The state the material is in at the given ambient temperature. */
    public Phase phaseAt(Temperature ambient) {
        return Phase.at(ambient, meltingPoint, boilingPoint);
    }

    /** Whether the material is fluid at the given ambient temperature. */
    public boolean isFluidAt(Temperature ambient) {
        return phaseAt(ambient).isFluid();
    }
}
