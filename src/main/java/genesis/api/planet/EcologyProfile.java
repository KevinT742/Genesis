package genesis.api.planet;

import genesis.api.physical.Temperature;

/**
 * The ecology parameters of a planet: how climate is derived from latitude, altitude and the
 * hydrology, and how the resulting biomes are distributed.
 *
 * <p>All temperatures are thermodynamic, in kelvin. The temperature <em>differences</em> of this
 * profile &mdash; the polar drop and the lapse rate &mdash; are differences in kelvin, which are
 * numerically the same as differences in degrees Celsius, because only the offset differs between
 * the two scales.
 *
 * @param equatorialTemperature the average surface temperature at the equator
 * @param polarTemperatureDrop   how much colder the poles are compared to the equator, in kelvin
 * @param altitudeLapseRate      how much temperature drops per block of altitude, in kelvin
 * @param moistureNoiseScale     the wavelength of the moisture pattern, in blocks
 * @param coastlineMoisture      the moisture bonus received close to the sea
 * @param riverMoisture          the moisture bonus received next to running water
 * @param treeDensity            the average amount of vegetation per biome
 * @param grassColorSaturation   how saturated the grass colour is, {@code 0} is grey
 */
public record EcologyProfile(
        Temperature equatorialTemperature,
        double polarTemperatureDrop,
        double altitudeLapseRate,
        double moistureNoiseScale,
        double coastlineMoisture,
        double riverMoisture,
        double treeDensity,
        double grassColorSaturation) {

    /** The default profile used by the Genesis home world. */
    public static EcologyProfile earthLike() {
        return new EcologyProfile(Temperature.ofKelvin(301.15), 58.0, 0.55, 1400.0, 0.25, 0.35, 1.0, 0.75);
    }

    /** A profile for a hot world with weak seasonal bands. */
    public static EcologyProfile jungle() {
        return new EcologyProfile(Temperature.ofKelvin(307.15), 24.0, 0.35, 900.0, 0.4, 0.4, 1.6, 1.0);
    }

    /** A profile for a frozen world with a wide ice belt. */
    public static EcologyProfile tundra() {
        return new EcologyProfile(Temperature.ofKelvin(269.15), 42.0, 0.45, 1100.0, 0.2, 0.25, 0.35, 0.35);
    }
}
