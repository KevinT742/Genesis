package genesis.api.material;

/**
 * The chemical properties of a material.
 *
 * <p>Chemistry decides how a block interacts with the environment: whether it dissolves in water,
 * whether it burns, how reactive it is and whether it is safe to live next to.
 *
 * <p>The composition states which elements the material is made of; the molar mass and the formula
 * follow from it, so they are never written down twice.
 *
 * @param composition     which elements the material is made of, and how many of each
 * @param oxidationState  the common oxidation state
 * @param phase           the state the material is in under normal conditions
 * @param reactivity      how eagerly the material reacts, {@code 0} to {@code 1}
 * @param solubility      how well it dissolves in water, {@code 0} to {@code 1}
 * @param combustibility  how readily it burns, {@code 0} to {@code 1}
 * @param toxicity        how dangerous it is to live next to, {@code 0} to {@code 1}
 * @param conductivity    how well it conducts electricity, {@code 0} to {@code 1}
 * @param corrosivity     how aggressively it attacks other materials, {@code 0} to {@code 1}
 * @param heatOfCombustion the energy released when one kilogram of it burns, in megajoules
 */
public record ChemicalProperties(
        Composition composition,
        double oxidationState,
        Phase phase,
        double reactivity,
        double solubility,
        double combustibility,
        double toxicity,
        double conductivity,
        double corrosivity,
        double heatOfCombustion) {

    public ChemicalProperties {
        if (composition == null) {
            composition = Composition.EMPTY;
        }
        if (phase == null) {
            phase = Phase.SOLID;
        }
        reactivity = clamp(reactivity);
        solubility = clamp(solubility);
        combustibility = clamp(combustibility);
        toxicity = clamp(toxicity);
        conductivity = clamp(conductivity);
        corrosivity = clamp(corrosivity);
        heatOfCombustion = Math.max(0.0, heatOfCombustion);
    }

    private static double clamp(double value) {
        return value < 0.0 ? 0.0 : (value > 1.0 ? 1.0 : value);
    }

    /** The chemical formula of the material, for example {@code SiO2}. */
    public String formula() {
        return composition.formula();
    }

    /** The mass of one mole of the material, in grams. */
    public double molarMass() {
        return composition.molarMass();
    }

    /** Whether the material dissolves in water. */
    public boolean isSoluble() {
        return solubility > 0.35;
    }

    /** Whether the material can be set alight. */
    public boolean isFlammable() {
        return combustibility > 0.2;
    }

    /** Whether the material carries a current. */
    public boolean isConductive() {
        return conductivity > 0.5;
    }

    /** Whether the material is a single element rather than a compound. */
    public boolean isElement() {
        return composition.atomCount() == 1;
    }

    /** Whether the material is a compound of several elements. */
    public boolean isCompound() {
        return composition.atomCount() > 1;
    }
}
