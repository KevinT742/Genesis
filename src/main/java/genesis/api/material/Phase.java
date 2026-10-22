package genesis.api.material;

import genesis.api.physical.Temperature;

/**
 * The state of a material.
 *
 * <p>The state is not written down by hand: it follows from the melting and the boiling point of
 * the material and from the temperature of the place it sits in. That is why every planet of the
 * mod family can have different weather without any block needing to know about it.
 *
 * <p>All temperatures are thermodynamic, so the comparison is a plain comparison of kelvin.
 */
public enum Phase {
    /** A solid: rigid, keeps its own shape. */
    SOLID,
    /** A liquid: flows, keeps its volume. */
    LIQUID,
    /** A gas: fills the space it is given. */
    GAS,
    /** A plasma: an ionised gas, hot enough to shine. */
    PLASMA;

    /**
     * The state a material is in at the given temperature.
     *
     * @param ambient      the temperature of the place, in kelvin
     * @param meltingPoint the melting point of the material, in kelvin
     * @param boilingPoint the boiling point of the material, in kelvin
     */
    public static Phase at(Temperature ambient, Temperature meltingPoint, Temperature boilingPoint) {
        if (ambient == null) {
            return SOLID;
        }
        if (meltingPoint != null && ambient.isAtLeast(meltingPoint)
                && (boilingPoint == null || ambient.isBelow(boilingPoint))) {
            return LIQUID;
        }
        if (boilingPoint != null && ambient.isAtLeast(boilingPoint)) {
            return ambient.kelvin() > 3000.0 ? PLASMA : GAS;
        }
        return SOLID;
    }

    /** Whether the material can be poured. */
    public boolean isFluid() {
        return this == LIQUID || this == GAS || this == PLASMA;
    }

    /** Whether the material fills the space it is given. */
    public boolean hasNoShape() {
        return this == GAS || this == PLASMA;
    }
}
