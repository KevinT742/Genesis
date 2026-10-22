package genesis.api.physical;

import java.util.Locale;

/**
 * A temperature on the thermodynamic scale.
 *
 * <p>Every temperature of the mod family is stored in <strong>kelvin</strong>, and it is stored in
 * this type rather than in a bare {@code double}. That is not pedantry: the algorithms of the
 * family are thermodynamic ones, and they need an absolute scale.
 *
 * <ul>
 *   <li><strong>Ratios need an absolute scale.</strong> "This surface is twice as hot as that one"
 *       is only a true statement in kelvin. On the Celsius scale the same statement is false.</li>
 *   <li><strong>Differences must not be mixed with points.</strong> A lapse rate, a temperature
 *       drop and a melting point are different kinds of quantity; keeping them in one type makes it
 *       impossible to add a melting point to a lapse rate by accident.</li>
 *   <li><strong>Offsets are the classic source of bugs.</strong> A melting point of {@code 273} and
 *       one of {@code 0} are the same temperature, and mixing the two produces water that is
 *       solid at room temperature. This type cannot be mixed up: it is kelvin or nothing.</li>
 * </ul>
 *
 * <p>Celsius and Fahrenheit exist as <em>conversions</em>, for the places where a value is shown
 * to a player or read from a configuration file, and for nothing else.
 *
 * @param kelvin the absolute temperature, in kelvin
 */
public record Temperature(double kelvin) implements Comparable<Temperature> {

    /** Absolute zero: the coldest temperature there is. */
    public static final Temperature ABSOLUTE_ZERO = new Temperature(0.0);

    /** The freezing point of water, at normal pressure. */
    public static final Temperature WATER_FREEZING = new Temperature(273.15);

    /** The boiling point of water, at normal pressure. */
    public static final Temperature WATER_BOILING = new Temperature(373.15);

    public Temperature {
        if (kelvin < 0.0) {
            throw new IllegalArgumentException("A temperature cannot be below absolute zero");
        }
        if (Double.isNaN(kelvin)) {
            throw new IllegalArgumentException("A temperature cannot be undefined");
        }
    }

    /** A temperature given in kelvin. */
    public static Temperature ofKelvin(double kelvin) {
        return new Temperature(kelvin);
    }

    /** A temperature given in degrees Celsius. */
    public static Temperature ofCelsius(double celsius) {
        return new Temperature(celsius + 273.15);
    }

    /** A temperature given in degrees Fahrenheit. */
    public static Temperature ofFahrenheit(double fahrenheit) {
        return new Temperature((fahrenheit - 32.0) * 5.0 / 9.0 + 273.15);
    }

    /** The temperature in degrees Celsius. */
    public double celsius() {
        return kelvin - 273.15;
    }

    /** The temperature in degrees Fahrenheit. */
    public double fahrenheit() {
        return (kelvin - 273.15) * 9.0 / 5.0 + 32.0;
    }

    /** This temperature, moved by the given number of kelvin. */
    public Temperature plusKelvin(double delta) {
        return new Temperature(Math.max(0.0, kelvin + delta));
    }

    /** This temperature, cooled by the given number of kelvin. */
    public Temperature minusKelvin(double delta) {
        return plusKelvin(-delta);
    }

    /**
     * How much colder this temperature is than another one, in kelvin.
     *
     * <p>The difference of two temperatures is the same number in kelvin and in degrees Celsius,
     * because only the offset differs between the two scales.
     */
    public double differenceTo(Temperature other) {
        return other == null ? 0.0 : kelvin - other.kelvin;
    }

    /** Whether this temperature is at or above the given one. */
    public boolean isAtLeast(Temperature other) {
        return other != null && kelvin >= other.kelvin;
    }

    /** Whether this temperature is below the given one. */
    public boolean isBelow(Temperature other) {
        return other != null && kelvin < other.kelvin;
    }

    /** Whether this temperature is at or below the given one. */
    public boolean isAtMost(Temperature other) {
        return other != null && kelvin <= other.kelvin;
    }

    /** Whether this temperature lies between the two given ones, lower bound included. */
    public boolean isBetween(Temperature low, Temperature high) {
        return low != null && high != null && kelvin >= low.kelvin && kelvin <= high.kelvin;
    }

    @Override
    public int compareTo(Temperature other) {
        return Double.compare(kelvin, other.kelvin);
    }

    /**
     * The temperature as it is shown to a player.
     *
     * <p>Both scales are given, because the thermodynamic one is what the simulation works with
     * and the Celsius one is what a player thinks in:
     *
     * <pre>
     *   273.15K（0℃）
     *   1811.15K（1538℃）
     * </pre>
     */
    public String format() {
        return String.format(Locale.ROOT, "%.2fK（%s℃）", kelvin, trim(celsius()));
    }

    /**
     * A number without the zeros that carry no information: {@code 0}, {@code 24.5}, {@code 1538}.
     */
    private static String trim(double value) {
        double rounded = Math.round(value * 100.0) / 100.0;
        if (rounded == 0.0) {
            return "0";
        }
        String text = String.format(Locale.ROOT, "%.2f", rounded);
        if (text.indexOf('.') >= 0) {
            text = text.replaceAll("0+$", "");
            if (text.endsWith(".")) {
                text = text.substring(0, text.length() - 1);
            }
        }
        return text;
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%.2f K", kelvin);
    }
}
