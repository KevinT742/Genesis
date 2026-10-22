package genesis.core.ecology;

import genesis.api.physical.Temperature;

/**
 * The climate of a position on the planet.
 *
 * <p>All temperatures are thermodynamic, in kelvin, so that the ratios of two climates mean
 * something.
 *
 * @param temperature  the annual mean temperature
 * @param moisture     the moisture availability, {@code 0} is bone dry, {@code 1} is saturated
 * @param continentalness the landmass factor, {@code -1} deep ocean, {@code 1} inland
 * @param weirdness    the noise factor used to break up the edges between biomes
 */
public record Climate(Temperature temperature, double moisture, double continentalness, double weirdness) {

    /** The temperature at which water freezes. */
    public static final Temperature FREEZING = Temperature.WATER_FREEZING;

    /** The temperature from which a place counts as hot. */
    public static final Temperature HOT = Temperature.ofKelvin(297.15);

    public Climate {
        if (temperature == null) {
            temperature = FREEZING;
        }
    }

    /** Whether the temperature is low enough to freeze water. */
    public boolean isFreezing() {
        return temperature.isAtMost(FREEZING);
    }

    /** Whether the temperature is hot enough to be desert. */
    public boolean isHot() {
        return temperature.isAtLeast(HOT);
    }
}
