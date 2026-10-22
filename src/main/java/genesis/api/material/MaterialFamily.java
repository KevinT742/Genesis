package genesis.api.material;

import genesis.api.physical.Temperature;

/**
 * Ready made material profiles for the families of blocks and items that exist in every Minecraft
 * world.
 *
 * <p>These are the templates the mod family starts from. A new block usually picks the closest
 * family and then overrides only the values that actually differ.
 *
 * <p>Where the real world has an answer, it is used: the density, the melting point, the boiling
 * point and the conductivities of a metal come from {@link ElementData}, and the chemical formula
 * decides the molar mass instead of the other way round.
 *
 * <p>All temperatures are thermodynamic, in kelvin.
 */
public final class MaterialFamily {

    /** The volume of one block, in cubic metres. */
    public static final double BLOCK_VOLUME = 1.0;

    private MaterialFamily() {
    }

    /** Generic stone. */
    public static MaterialProfile rock(String id) {
        return new MaterialProfile(id,
                new PhysicalProperties(2600.0, BLOCK_VOLUME, 1.5, 6.0, 0.05, 2.1, 790.0,
                        Temperature.ofKelvin(1986.15), Temperature.ofKelvin(3223.15), 0.6, 0),
                new ChemicalProperties(Composition.parse("SiO2"), 4.0, Phase.SOLID, 0.08, 0.01, 0.0, 0.0, 0.0, 0.02, 0.0),
                new MagicProperties(ElementAffinity.EARTH, 8.0, 0.15, 42.0, 0.85, 0.2, 0.15, 0.1, 0.2));
    }

    /** Deep stone, denser and tougher than ordinary rock. */
    public static MaterialProfile deepStone(String id) {
        return new MaterialProfile(id,
                new PhysicalProperties(2900.0, BLOCK_VOLUME, 3.0, 12.0, 0.04, 2.6, 780.0,
                        Temperature.ofKelvin(1986.15), Temperature.ofKelvin(3223.15), 0.6, 0),
                new ChemicalProperties(Composition.parse("SiO2"), 4.0, Phase.SOLID, 0.05, 0.005, 0.0, 0.0, 0.0, 0.02, 0.0),
                new MagicProperties(ElementAffinity.EARTH, 14.0, 0.12, 38.0, 0.92, 0.25, 0.12, 0.08, 0.18));
    }

    /**
     * Ordinary metal.
     *
     * <p>The density, the melting point, the boiling point and the conductivities come from the
     * real element, so iron melts at 1811 kelvin and copper carries a current better than gold.
     */
    public static MaterialProfile metal(String id, String element, double hardness) {
        ElementData.Values data = ElementData.of(element);
        double density = data == null ? 7800.0 : data.density();
        Temperature melting = data == null ? Temperature.ofKelvin(1773.15) : data.meltingPoint();
        Temperature boiling = data == null ? Temperature.ofKelvin(3203.15) : data.boilingPoint();
        double thermal = data == null ? 50.0 : data.thermalConductivity();
        double electrical = data == null ? 0.9 : ElementData.normalizedConductivity(element);
        double specificHeat = data == null ? 450.0 : data.specificHeat();
        return new MaterialProfile(id,
                new PhysicalProperties(density, BLOCK_VOLUME, hardness, hardness * 4.0, 0.12, thermal, specificHeat,
                        melting, boiling, 0.6, 0),
                new ChemicalProperties(Composition.of(element, 1), 0.0, Phase.SOLID, 0.3, 0.0, 0.0, 0.05, electrical, 0.15, 0.0),
                new MagicProperties(ElementAffinity.METAL, 20.0, 0.08, 120.0, 0.9, 0.3, 0.2, 0.25, 0.35));
    }

    /** Noble metal, chemically inert and highly magical. */
    public static MaterialProfile nobleMetal(String id, String element, double hardness) {
        ElementData.Values data = ElementData.of(element);
        double density = data == null ? 19300.0 : data.density();
        Temperature melting = data == null ? Temperature.ofKelvin(1337.15) : data.meltingPoint();
        Temperature boiling = data == null ? Temperature.ofKelvin(3129.15) : data.boilingPoint();
        double thermal = data == null ? 90.0 : data.thermalConductivity();
        double electrical = data == null ? 0.95 : ElementData.normalizedConductivity(element);
        double specificHeat = data == null ? 130.0 : data.specificHeat();
        return new MaterialProfile(id,
                new PhysicalProperties(density, BLOCK_VOLUME, hardness, hardness * 3.0, 0.2, thermal, specificHeat,
                        melting, boiling, 0.6, 0),
                new ChemicalProperties(Composition.of(element, 1), 1.0, Phase.SOLID, 0.02, 0.0, 0.0, 0.02, electrical, 0.0, 0.0),
                new MagicProperties(ElementAffinity.LIGHT, 120.0, 0.05, 440.0, 0.95, 0.7, 0.4, 0.45, 0.55));
    }

    /** Wood and other organic structural material. */
    public static MaterialProfile wood(String id) {
        return new MaterialProfile(id,
                new PhysicalProperties(700.0, BLOCK_VOLUME, 2.0, 3.0, 0.25, 0.15, 1700.0,
                        Temperature.ofKelvin(573.15), Temperature.ofKelvin(723.15), 0.6, 0),
                new ChemicalProperties(Composition.parse("C6H10O5"), 0.0, Phase.SOLID, 0.35, 0.12, 0.65, 0.0, 0.0, 0.1, 16.0),
                new MagicProperties(ElementAffinity.ORGANIC, 10.0, 0.45, 66.0, 0.6, 0.25, 0.3, 0.2, 0.4));
    }

    /** Leaves and other foliage. */
    public static MaterialProfile foliage(String id) {
        return new MaterialProfile(id,
                new PhysicalProperties(300.0, BLOCK_VOLUME, 0.2, 0.2, 0.4, 0.08, 1600.0,
                        Temperature.ofKelvin(553.15), Temperature.ofKelvin(653.15), 0.6, 0),
                new ChemicalProperties(Composition.parse("C6H10O5"), 0.0, Phase.SOLID, 0.5, 0.2, 0.85, 0.03, 0.0, 0.1, 14.0),
                new MagicProperties(ElementAffinity.ORGANIC, 18.0, 0.75, 92.0, 0.35, 0.3, 0.45, 0.3, 0.25));
    }

    /** Sand and gravel. */
    public static MaterialProfile granular(String id) {
        return new MaterialProfile(id,
                new PhysicalProperties(1600.0, BLOCK_VOLUME, 0.5, 0.5, 0.02, 0.5, 830.0,
                        Temperature.ofKelvin(1986.15), Temperature.ofKelvin(3223.15), 0.6, 0),
                new ChemicalProperties(Composition.parse("SiO2"), 4.0, Phase.SOLID, 0.1, 0.02, 0.0, 0.0, 0.0, 0.02, 0.0),
                new MagicProperties(ElementAffinity.EARTH, 4.0, 0.6, 28.0, 0.4, 0.1, 0.08, 0.05, 0.15));
    }

    /** Ice and snow. */
    public static MaterialProfile frozen(String id) {
        return new MaterialProfile(id,
                new PhysicalProperties(917.0, BLOCK_VOLUME, 0.5, 0.5, 0.02, 2.2, 2100.0,
                        Temperature.WATER_FREEZING, Temperature.WATER_BOILING, 0.98, 0),
                new ChemicalProperties(Composition.parse("H2O"), -2.0, Phase.SOLID, 0.15, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0),
                new MagicProperties(ElementAffinity.WATER, 26.0, 0.3, 210.0, 0.5, 0.4, 0.35, 0.3, 0.45));
    }

    /** Liquid water. */
    public static MaterialProfile liquid(String id) {
        return new MaterialProfile(id,
                new PhysicalProperties(1000.0, BLOCK_VOLUME, 0.0, 100.0, 0.0, 0.6, 4186.0,
                        Temperature.WATER_FREEZING, Temperature.WATER_BOILING, 0.98, 0),
                new ChemicalProperties(Composition.parse("H2O"), -2.0, Phase.LIQUID, 0.2, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0),
                new MagicProperties(ElementAffinity.WATER, 40.0, 0.4, 190.0, 0.3, 0.35, 0.4, 0.35, 0.4));
    }

    /** Obsidian like material: extremely tough, hungry for magic. */
    public static MaterialProfile obsidian(String id) {
        return new MaterialProfile(id,
                new PhysicalProperties(2950.0, BLOCK_VOLUME, 50.0, 1200.0, 0.01, 1.1, 760.0,
                        Temperature.ofKelvin(2473.15), Temperature.ofKelvin(3773.15), 0.6, 0),
                new ChemicalProperties(Composition.parse("SiO2"), 4.0, Phase.SOLID, 0.02, 0.0, 0.0, 0.0, 0.0, 0.1, 0.0),
                new MagicProperties(ElementAffinity.DARK, 220.0, 0.08, 310.0, 0.98, 0.8, 0.25, 0.55, 0.9));
    }

    /** Crystal, the classic magical building material. */
    public static MaterialProfile crystal(String id) {
        return new MaterialProfile(id,
                new PhysicalProperties(3200.0, BLOCK_VOLUME, 8.0, 12.0, 0.02, 18.0, 720.0,
                        Temperature.ofKelvin(1986.15), Temperature.ofKelvin(3223.15), 0.6, 4),
                new ChemicalProperties(Composition.parse("SiO2"), 4.0, Phase.SOLID, 0.04, 0.0, 0.0, 0.0, 0.0, 0.05, 0.0),
                new MagicProperties(ElementAffinity.CRYSTAL, 260.0, 0.1, 520.0, 0.9, 0.9, 0.7, 0.45, 0.75));
    }

    /** A block that conducts redstone strongly. */
    public static MaterialProfile conductor(String id, String element) {
        ElementData.Values data = ElementData.of(element);
        double density = data == null ? 8900.0 : data.density();
        Temperature melting = data == null ? Temperature.ofKelvin(1358.15) : data.meltingPoint();
        Temperature boiling = data == null ? Temperature.ofKelvin(2835.15) : data.boilingPoint();
        double thermal = data == null ? 400.0 : data.thermalConductivity();
        double electrical = data == null ? 1.0 : ElementData.normalizedConductivity(element);
        double specificHeat = data == null ? 380.0 : data.specificHeat();
        return new MaterialProfile(id,
                new PhysicalProperties(density, BLOCK_VOLUME, 6.0, 20.0, 0.1, thermal, specificHeat, melting, boiling,
                        0.6, 0),
                new ChemicalProperties(Composition.of(element, 1), 1.0, Phase.SOLID, 0.4, 0.0, 0.0, 0.02, electrical, 0.2, 0.0),
                new MagicProperties(ElementAffinity.METAL, 60.0, 0.06, 260.0, 0.95, 0.45, 0.3, 0.5, 0.4));
    }

    /** Something alive, or once alive. */
    public static MaterialProfile organic(String id) {
        return new MaterialProfile(id,
                new PhysicalProperties(1000.0, BLOCK_VOLUME, 1.0, 1.0, 0.3, 0.5, 3400.0,
                        Temperature.ofKelvin(583.15), Temperature.ofKelvin(853.15), 0.6, 0),
                new ChemicalProperties(Composition.parse("C"), 0.0, Phase.SOLID, 0.6, 0.05, 0.9, 0.1, 0.0, 0.2, 17.0),
                new MagicProperties(ElementAffinity.ORGANIC, 16.0, 0.6, 74.0, 0.45, 0.35, 0.4, 0.35, 0.3));
    }

    /** A magical material with a strong arcane signature. */
    public static MaterialProfile arcane(String id) {
        return new MaterialProfile(id,
                new PhysicalProperties(1800.0, BLOCK_VOLUME, 4.0, 8.0, 0.05, 6.0, 900.0,
                        Temperature.ofKelvin(1773.15), Temperature.ofKelvin(3073.15), 0.6, 7),
                new ChemicalProperties(Composition.EMPTY, 0.0, Phase.SOLID, 0.5, 0.1, 0.0, 0.2, 0.3, 0.4, 0.0),
                new MagicProperties(ElementAffinity.ARCANE, 480.0, 0.25, 880.0, 0.7, 1.0, 0.65, 0.6, 0.7));
    }

    // ---------------------------------------------------------------------
    // the families the vanilla world is made of
    // ---------------------------------------------------------------------

    /**
     * Glass: amorphous silicon dioxide.
     *
     * <p>It is the same chemistry as rock, but it has no grain and it lets the light through,
     * which is why it carries the affinity of light rather than of earth.
     */
    public static MaterialProfile glass(String id) {
        return new MaterialProfile(id,
                new PhysicalProperties(2500.0, BLOCK_VOLUME, 0.3, 0.3, 0.0, 1.0, 840.0,
                        Temperature.ofKelvin(1673.15), Temperature.ofKelvin(2503.15), 0.6, 0),
                new ChemicalProperties(Composition.parse("SiO2"), 4.0, Phase.SOLID, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
                new MagicProperties(ElementAffinity.LIGHT, 12.0, 0.2, 60.0, 0.7, 0.5, 0.2, 0.35, 0.35));
    }

    /**
     * Plant fibre: wool, string, paper.
     *
     * <p>Very light, very flammable and it takes magic up quite well, which is why a bundle of
     * fibre is a decent thing to bind a charm with.
     */
    public static MaterialProfile fibre(String id) {
        return new MaterialProfile(id,
                new PhysicalProperties(80.0, BLOCK_VOLUME, 0.1, 0.1, 0.3, 0.12, 1400.0,
                        Temperature.ofKelvin(573.15), Temperature.ofKelvin(723.15), 0.6, 0),
                new ChemicalProperties(Composition.parse("C6H10O5"), 0.0, Phase.SOLID, 0.5, 0.3, 0.9, 0.0, 0.0, 0.05, 16.0),
                new MagicProperties(ElementAffinity.ORGANIC, 14.0, 0.5, 70.0, 0.4, 0.3, 0.45, 0.35, 0.3));
    }

    /** Coal: nearly pure carbon. */
    public static MaterialProfile carbon(String id) {
        return new MaterialProfile(id,
                new PhysicalProperties(1500.0, BLOCK_VOLUME, 1.5, 3.0, 0.0, 1.5, 710.0,
                        Temperature.ofKelvin(3823.15), Temperature.ofKelvin(4300.15), 0.6, 0),
                new ChemicalProperties(Composition.parse("C"), 0.0, Phase.SOLID, 0.4, 0.9, 0.0, 0.0, 0.0, 0.2, 0.0),
                new MagicProperties(ElementAffinity.EARTH, 10.0, 0.35, 50.0, 0.6, 0.3, 0.3, 0.25, 0.25));
    }

    /**
     * Netherite: the dark alloy of the lower world.
     *
     * <p>It is not an element of the real world, so it has no place in the table of the elements.
     * Its numbers are the ones the game gives it: dense, hard, and it does not burn.
     */
    public static MaterialProfile netherite(String id) {
        return new MaterialProfile(id,
                new PhysicalProperties(5000.0, BLOCK_VOLUME, 7.0, 20.0, 0.02, 20.0, 500.0,
                        Temperature.ofKelvin(1600.0), Temperature.ofKelvin(3000.0), 0.6, 0),
                new ChemicalProperties(Composition.EMPTY, 0.0, Phase.SOLID, 0.1, 0.0, 0.0, 0.0, 0.1, 0.4, 0.0),
                new MagicProperties(ElementAffinity.DARK, 90.0, 0.15, 300.0, 0.95, 0.7, 0.35, 0.45, 0.5));
    }

    /**
     * A gem: a crystal of a real substance.
     *
     * <p>Unlike the metals, a gem does not melt in a world you can reach: it is given a melting
     * point far above the temperatures a furnace can reach, which is true of every gem that exists.
     *
     * @param id      the identifier of the material
     * @param formula the chemical formula of the gem
     * @param density the density of the gem, in kilograms per cubic metre
     * @param hardness the hardness of the gem on the Mohs scale
     */
    public static MaterialProfile gem(String id, String formula, double density, double hardness) {
        return new MaterialProfile(id,
                new PhysicalProperties(density, BLOCK_VOLUME, hardness, hardness * 3.0, 0.02, 18.0, 700.0,
                        Temperature.ofKelvin(2500.0), Temperature.ofKelvin(3500.0), 0.6, 0),
                new ChemicalProperties(Composition.parse(formula), 0.0, Phase.SOLID, 0.02, 0.0, 0.0, 0.0, 0.0, 0.1, 0.0),
                new MagicProperties(ElementAffinity.CRYSTAL, 240.0, 0.1, 560.0, 0.9, 0.8, 0.35, 0.5, 0.6));
    }

    /**
     * A material the family does not know better.
     *
     * <p>Every block and every item of the game has a material, even the ones nobody has looked
     * at yet. Rather than pretending to know, this profile says what is true of almost everything:
     * it is there, it has a mass, and it answers to the earth.
     */
    public static MaterialProfile generic(String id) {
        return new MaterialProfile(id,
                new PhysicalProperties(1000.0, BLOCK_VOLUME, 1.0, 1.0, 0.2, 0.5, 1500.0,
                        Temperature.ofKelvin(500.0), Temperature.ofKelvin(900.0), 0.6, 0),
                new ChemicalProperties(Composition.EMPTY, 0.0, Phase.SOLID, 0.1, 0.0, 0.0, 0.0, 0.0, 0.05, 0.0),
                new MagicProperties(ElementAffinity.EARTH, 5.0, 0.3, 30.0, 0.5, 0.2, 0.15, 0.1, 0.2));
    }

    // ---------------------------------------------------------------------
    // the families the magic model needs
    // ---------------------------------------------------------------------

    /**
     * A 魔力材料: a thing without life that took magic up and keeps it.
     *
     * <p>This is the material a potion is brewed from. The more it adapts to magic and the better
     * it holds on to it, the more magic the finished potion can carry.
     */
    public static MaterialProfile magicMaterial(String id, double volume) {
        return new MaterialProfile(id,
                new PhysicalProperties(2600.0, volume, 2.0, 3.0, 0.02, 12.0, 740.0,
                        Temperature.ofKelvin(1986.15), Temperature.ofKelvin(3223.15), 0.6, 2),
                new ChemicalProperties(Composition.parse("SiO2"), 4.0, Phase.SOLID, 0.05, 0.0, 0.0, 0.0, 0.0, 0.05, 0.0),
                new MagicProperties(ElementAffinity.CRYSTAL, 180.0, 0.1, 520.0, 0.75, 0.6, 0.85, 0.35, 0.8));
    }

    /**
     * Solidified magic: the essence a potion is made of.
     *
     * <p>Magic has no chemistry, which is why the composition is empty: the formula of a potion is
     * a question that physics does not answer.
     */
    public static MaterialProfile solidifiedMagic(String id, double volume) {
        return new MaterialProfile(id,
                new PhysicalProperties(1200.0, volume, 0.5, 1.0, 0.1, 0.9, 2600.0,
                        Temperature.ofKelvin(253.15), Temperature.ofKelvin(353.15), 0.6, 6),
                new ChemicalProperties(Composition.EMPTY, 0.0, Phase.LIQUID, 0.4, 1.0, 0.0, 0.2, 0.3, 0.3, 0.0),
                new MagicProperties(ElementAffinity.ARCANE, 320.0, 0.2, 700.0, 0.5, 0.9, 0.7, 0.5, 0.95));
    }

    /**
     * A harvested magic organ: the part of a living being that manipulated magic.
     *
     * <p>The organ keeps the character of its host: it is alive in the way that only something that
     * was part of a creature can be.
     */
    public static MaterialProfile organTissue(String id, double volume) {
        return new MaterialProfile(id,
                new PhysicalProperties(1050.0, volume, 1.0, 1.0, 0.35, 0.5, 3600.0,
                        Temperature.ofKelvin(583.15), Temperature.ofKelvin(853.15), 0.6, 0),
                new ChemicalProperties(Composition.parse("C6H10O5"), 0.0, Phase.SOLID, 0.6, 0.05, 0.9, 0.15, 0.0, 0.25, 17.0),
                new MagicProperties(ElementAffinity.ORGANIC, 260.0, 0.55, 90.0, 0.4, 0.4, 0.75, 0.8, 0.45));
    }

    /**
     * A solidified way of channeling magic: a scroll, a tome, a rune tablet.
     *
     * <p>The material itself hardly matters; what matters is that it guides magic precisely and
     * holds the instructions of a channel.
     */
    public static MaterialProfile channelMedium(String id, double volume) {
        return new MaterialProfile(id,
                new PhysicalProperties(1800.0, volume, 1.0, 2.0, 0.1, 0.5, 1400.0,
                        Temperature.ofKelvin(593.15), Temperature.ofKelvin(873.15), 0.6, 0),
                new ChemicalProperties(Composition.parse("C6H10O5"), 0.0, Phase.SOLID, 0.3, 0.02, 0.8, 0.05, 0.0, 0.1, 16.0),
                new MagicProperties(ElementAffinity.ARCANE, 90.0, 0.15, 640.0, 0.85, 0.95, 0.4, 0.95, 0.7));
    }
}
