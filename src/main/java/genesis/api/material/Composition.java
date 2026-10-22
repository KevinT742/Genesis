package genesis.api.material;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * The chemical composition of a material: which elements it is made of and how many of each.
 *
 * <p>A composition is the chemical half of the truth about a material. Together with the physical
 * properties it answers the questions the mod family asks about every item: how heavy a block is,
 * at which temperature it melts, how much of it dissolves in water and what it turns into when it
 * burns.
 *
 * <p>Formulas are parsed with the usual notation of chemistry, including parentheses:
 * <pre>
 *   SiO2       one silicon, two oxygen
 *   C6H10O5    cellulose
 *   Ca(OH)2    calcium hydroxide
 * </pre>
 *
 * <p>The molar mass is computed from the composition with the standard atomic weights, so a profile
 * never has to state the same number twice.
 */
public record Composition(Map<String, Integer> elements) {

    /** The composition of a material that has no known chemistry. */
    public static final Composition EMPTY = new Composition(Map.of());

    /** The standard atomic weights, in grams per mole. */
    private static final Map<String, Double> ATOMIC_WEIGHTS = createAtomicWeights();

    public Composition {
        Map<String, Integer> copy = new LinkedHashMap<>();
        if (elements != null) {
            for (Map.Entry<String, Integer> entry : elements.entrySet()) {
                String symbol = entry.getKey();
                int count = entry.getValue();
                if (symbol == null || symbol.isBlank() || count <= 0) {
                    continue;
                }
                copy.put(symbol.trim().toUpperCase(Locale.ROOT), count);
            }
        }
        elements = Collections.unmodifiableMap(copy);
    }

    /** A composition of a single element. */
    public static Composition of(String element, int count) {
        return new Composition(Map.of(element, count));
    }

    /**
     * Parses a chemical formula.
     *
     * <p>Unknown element symbols are kept, but they contribute no mass, so a formula with a typo
     * still describes a usable material instead of failing the whole registration.
     */
    public static Composition parse(String formula) {
        if (formula == null || formula.isBlank()) {
            return EMPTY;
        }
        Map<String, Integer> counts = new LinkedHashMap<>();
        parseGroup(formula, 0, formula.length(), counts, 1);
        return new Composition(counts);
    }

    private static void parseGroup(String formula, int from, int to, Map<String, Integer> counts, int multiplier) {
        int index = from;
        while (index < to) {
            char c = formula.charAt(index);
            if (Character.isWhitespace(c) || c == '+' || c == '.') {
                index++;
                continue;
            }
            if (c == '(') {
                int close = matchingParenthesis(formula, index, to);
                if (close < 0) {
                    return;
                }
                int groupMultiplier = readNumber(formula, close + 1, to);
                parseGroup(formula, index + 1, close, counts, multiplier * groupMultiplier);
                index = close + 1 + digits(formula, close + 1, to);
                continue;
            }
            if (!Character.isLetter(c)) {
                index++;
                continue;
            }
            int start = index;
            index++;
            while (index < to && Character.isLowerCase(formula.charAt(index))) {
                index++;
            }
            String symbol = formula.substring(start, index).toUpperCase(Locale.ROOT);
            int count = readNumber(formula, index, to);
            index += digits(formula, index, to);
            counts.merge(symbol, count * multiplier, Integer::sum);
        }
    }

    private static int matchingParenthesis(String formula, int open, int to) {
        int depth = 0;
        for (int i = open; i < to; i++) {
            char c = formula.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static int readNumber(String formula, int from, int to) {
        int length = digits(formula, from, to);
        if (length == 0) {
            return 1;
        }
        try {
            return Integer.parseInt(formula.substring(from, from + length));
        } catch (NumberFormatException exception) {
            return 1;
        }
    }

    private static int digits(String formula, int from, int to) {
        int index = from;
        while (index < to && Character.isDigit(formula.charAt(index))) {
            index++;
        }
        return index - from;
    }

    /** The number of atoms of the given element in one molecule. */
    public int countOf(String element) {
        Integer count = elements.get(element == null ? "" : element.trim().toUpperCase(Locale.ROOT));
        return count == null ? 0 : count;
    }

    /** The number of atoms in one molecule. */
    public int atomCount() {
        int total = 0;
        for (int count : elements.values()) {
            total += count;
        }
        return total;
    }

    /** The molar mass of the composition, in grams per mole. */
    public double molarMass() {
        double total = 0.0;
        for (Map.Entry<String, Integer> entry : elements.entrySet()) {
            Double weight = ATOMIC_WEIGHTS.get(entry.getKey());
            if (weight != null) {
                total += weight * entry.getValue();
            }
        }
        return total;
    }

    /** The mass of one element inside one mole of the composition, in grams. */
    public double massOf(String element) {
        Double weight = ATOMIC_WEIGHTS.get(element == null ? "" : element.trim().toUpperCase(Locale.ROOT));
        if (weight == null) {
            return 0.0;
        }
        return weight * countOf(element);
    }

    /**
     * The fraction of the mass that one element contributes, between {@code 0} and {@code 1}.
     *
     * <p>Returns {@code 0} for an empty composition, so it is always safe to use in a division.
     */
    public double massFraction(String element) {
        double total = molarMass();
        if (total <= 0.0) {
            return 0.0;
        }
        return massOf(element) / total;
    }

    /** Whether the composition contains the given element. */
    public boolean contains(String element) {
        return countOf(element) > 0;
    }

    /** Whether nothing is known about the chemistry of the material. */
    public boolean isEmpty() {
        return elements.isEmpty();
    }

    /**
     * The formula of the composition, written in Hill notation.
     *
     * <p>Carbon comes first, hydrogen second, everything else in alphabetical order.
     */
    public String formula() {
        if (elements.isEmpty()) {
            return "?";
        }
        TreeMap<String, Integer> sorted = new TreeMap<>(elements);
        StringBuilder builder = new StringBuilder();
        if (sorted.containsKey("C")) {
            Integer carbon = sorted.remove("C");
            append(builder, "C", carbon);
            Integer hydrogen = sorted.remove("H");
            if (hydrogen != null) {
                append(builder, "H", hydrogen);
            }
        }
        for (Map.Entry<String, Integer> entry : sorted.entrySet()) {
            append(builder, entry.getKey(), entry.getValue());
        }
        return builder.toString();
    }

    private static void append(StringBuilder builder, String symbol, int count) {
        builder.append(symbol);
        if (count > 1) {
            builder.append(count);
        }
    }

    /** The atomic weight of an element, in grams per mole, or {@code 0} when it is unknown. */
    public static double atomicWeight(String element) {
        Double weight = ATOMIC_WEIGHTS.get(element == null ? "" : element.trim().toUpperCase(Locale.ROOT));
        return weight == null ? 0.0 : weight;
    }

    private static Map<String, Double> createAtomicWeights() {
        Map<String, Double> weights = new LinkedHashMap<>();
        weights.put("H", 1.008);
        weights.put("HE", 4.0026);
        weights.put("LI", 6.94);
        weights.put("BE", 9.0122);
        weights.put("B", 10.81);
        weights.put("C", 12.011);
        weights.put("N", 14.007);
        weights.put("O", 15.999);
        weights.put("F", 18.998);
        weights.put("NE", 20.180);
        weights.put("NA", 22.990);
        weights.put("MG", 24.305);
        weights.put("AL", 26.982);
        weights.put("SI", 28.085);
        weights.put("P", 30.974);
        weights.put("S", 32.06);
        weights.put("CL", 35.45);
        weights.put("AR", 39.948);
        weights.put("K", 39.098);
        weights.put("CA", 40.078);
        weights.put("SC", 44.956);
        weights.put("TI", 47.867);
        weights.put("V", 50.942);
        weights.put("CR", 51.996);
        weights.put("MN", 54.938);
        weights.put("FE", 55.845);
        weights.put("CO", 58.933);
        weights.put("NI", 58.693);
        weights.put("CU", 63.546);
        weights.put("ZN", 65.38);
        weights.put("AS", 74.922);
        weights.put("SE", 78.971);
        weights.put("BR", 79.904);
        weights.put("KR", 83.798);
        weights.put("MO", 95.95);
        weights.put("AG", 107.868);
        weights.put("CD", 112.414);
        weights.put("SN", 118.710);
        weights.put("SB", 121.760);
        weights.put("TE", 127.60);
        weights.put("I", 126.904);
        weights.put("XE", 131.293);
        weights.put("PT", 195.084);
        weights.put("AU", 196.967);
        weights.put("HG", 200.592);
        weights.put("PB", 207.2);
        weights.put("BI", 208.980);
        weights.put("U", 238.029);
        return Collections.unmodifiableMap(weights);
    }
}
