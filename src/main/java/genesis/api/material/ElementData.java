package genesis.api.material;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import genesis.api.physical.Temperature;

/**
 * The physical constants of the elements.
 *
 * <p>The mod family does not invent numbers for the things that exist in the real world. The
 * density, the melting point, the boiling point and the conductivities of iron, copper, gold and
 * the other common metals are the real ones, so that a block of gold in Genesis weighs what a
 * block of gold weighs and melts when gold melts.
 *
 * <p>All temperatures are thermodynamic, in kelvin.
 */
public final class ElementData {

    /**
     * The physical constants of one element.
     *
     * @param symbol                 the chemical symbol
     * @param density                the density at normal conditions, in kilograms per cubic metre
     * @param meltingPoint           the melting point
     * @param boilingPoint           the boiling point
     * @param mohsHardness           the hardness on the Mohs scale
     * @param thermalConductivity    how well heat passes through, in watts per metre kelvin
     * @param electricalConductivity how well a current passes through, in siemens per metre
     * @param specificHeat           the energy needed to raise one kilogram by one kelvin
     */
    public record Values(
            String symbol,
            double density,
            Temperature meltingPoint,
            Temperature boilingPoint,
            double mohsHardness,
            double thermalConductivity,
            double electricalConductivity,
            double specificHeat) {
    }

    /**
     * The conductivity of a good conductor, in siemens per metre.
     *
     * <p>It is the reference of the scale the material model uses: every metal that carries a
     * current well reaches {@code 1}, and the poor conductors fall away below it.
     */
    private static final double REFERENCE_CONDUCTIVITY = 1.0e7;

    private static final Map<String, Values> ELEMENTS = createElements();

    private ElementData() {
    }

    /** The constants of an element, or {@code null} when the element is unknown. */
    public static Values of(String symbol) {
        if (symbol == null) {
            return null;
        }
        return ELEMENTS.get(symbol.trim().toUpperCase(Locale.ROOT));
    }

    /** The density of an element, or {@code 0} when it is unknown. */
    public static double densityOf(String symbol) {
        Values values = of(symbol);
        return values == null ? 0.0 : values.density();
    }

    /** The melting point of an element, or {@code null} when it is unknown. */
    public static Temperature meltingPointOf(String symbol) {
        Values values = of(symbol);
        return values == null ? null : values.meltingPoint();
    }

    /** The boiling point of an element, or {@code null} when it is unknown. */
    public static Temperature boilingPointOf(String symbol) {
        Values values = of(symbol);
        return values == null ? null : values.boilingPoint();
    }

    /**
     * The conductivity of an element on the scale the material model uses, between {@code 0} and
     * {@code 1}.
     */
    public static double normalizedConductivity(String symbol) {
        Values values = of(symbol);
        if (values == null) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, values.electricalConductivity() / REFERENCE_CONDUCTIVITY));
    }

    /** The symbols of all known elements. */
    public static java.util.Collection<String> known() {
        return ELEMENTS.keySet();
    }

    private static Map<String, Values> createElements() {
        Map<String, Values> elements = new LinkedHashMap<>();
        elements.put("C", new Values("C", 3515.0, Temperature.ofKelvin(3823.15), Temperature.ofKelvin(4300.15),
                10.0, 2000.0, 1.0e4, 710.0));
        elements.put("SI", new Values("SI", 2330.0, Temperature.ofKelvin(1687.15), Temperature.ofKelvin(3538.15),
                7.0, 150.0, 1.0e-5, 705.0));
        elements.put("AL", new Values("AL", 2700.0, Temperature.ofKelvin(1206.15), Temperature.ofKelvin(3016.15),
                2.75, 237.0, 3.77e7, 897.0));
        elements.put("FE", new Values("FE", 7874.0, Temperature.ofKelvin(1811.15), Temperature.ofKelvin(3135.15),
                4.0, 80.4, 1.0e7, 449.0));
        elements.put("CU", new Values("CU", 8960.0, Temperature.ofKelvin(1358.15), Temperature.ofKelvin(2835.15),
                3.0, 401.0, 5.96e7, 385.0));
        elements.put("ZN", new Values("ZN", 7140.0, Temperature.ofKelvin(693.15), Temperature.ofKelvin(1180.15),
                2.5, 116.0, 1.69e7, 388.0));
        elements.put("AG", new Values("AG", 10490.0, Temperature.ofKelvin(1235.15), Temperature.ofKelvin(2435.15),
                2.5, 429.0, 6.30e7, 235.0));
        elements.put("SN", new Values("SN", 7317.0, Temperature.ofKelvin(505.15), Temperature.ofKelvin(2875.15),
                1.5, 64.0, 9.10e6, 228.0));
        elements.put("AU", new Values("AU", 19300.0, Temperature.ofKelvin(1337.15), Temperature.ofKelvin(3129.15),
                2.5, 317.0, 4.09e7, 129.0));
        elements.put("PB", new Values("PB", 11340.0, Temperature.ofKelvin(600.15), Temperature.ofKelvin(2022.15),
                1.5, 35.0, 9.10e6, 129.0));
        elements.put("MG", new Values("MG", 1740.0, Temperature.ofKelvin(923.15), Temperature.ofKelvin(1364.15),
                2.5, 156.0, 2.18e7, 1020.0));
        elements.put("W", new Values("W", 19250.0, Temperature.ofKelvin(3695.15), Temperature.ofKelvin(5828.15),
                7.5, 170.0, 1.85e7, 140.0));
        elements.put("PT", new Values("PT", 21450.0, Temperature.ofKelvin(2041.15), Temperature.ofKelvin(4098.15),
                3.5, 71.0, 8.83e6, 133.0));
        elements.put("HG", new Values("HG", 13534.0, Temperature.ofKelvin(234.35), Temperature.ofKelvin(629.85),
                1.5, 8.3, 1.04e6, 140.0));
        elements.put("S", new Values("S", 1960.0, Temperature.ofKelvin(388.15), Temperature.ofKelvin(718.15),
                2.0, 0.205, 1.0e-6, 705.0));
        return Collections.unmodifiableMap(elements);
    }
}
