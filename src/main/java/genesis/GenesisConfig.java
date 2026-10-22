package genesis;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The configuration of Genesis.
 *
 * <p>The options are deliberately coarse: the fine tuning of a planet happens in its
 * {@link genesis.api.planet.PlanetDefinition}, not in the configuration file.
 */
public final class GenesisConfig {

    /** The configuration spec. */
    public static final ModConfigSpec SPEC;

    /** Whether the world wraps at its boundary, turning it into a planet. */
    public static final ModConfigSpec.BooleanValue CONTINUOUS_WORLD;
    /** Whether settlements, ruins and hidden sites are generated. */
    public static final ModConfigSpec.BooleanValue GENERATE_STRUCTURES;
    /** Whether the roads between the settlements are generated. */
    public static final ModConfigSpec.BooleanValue GENERATE_ROADS;
    /** Whether caves are carved into the terrain. */
    public static final ModConfigSpec.BooleanValue GENERATE_CAVES;
    /** How many settlements the planner aims for, as a percentage of the default. */
    public static final ModConfigSpec.IntValue SETTLEMENT_DENSITY;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("World generation").push("world");
        CONTINUOUS_WORLD = builder
                .comment("Whether the world continues past its boundary instead of ending there.")
                .define("continuous_world", true);
        GENERATE_CAVES = builder
                .comment("Whether caves are carved into the terrain.")
                .define("generate_caves", true);
        builder.pop();

        builder.comment("Structures").push("structures");
        GENERATE_STRUCTURES = builder
                .comment("Whether settlements, ruins and hidden sites are generated.")
                .define("generate_structures", true);
        GENERATE_ROADS = builder
                .comment("Whether the roads that connect the settlements are generated.")
                .define("generate_roads", true);
        SETTLEMENT_DENSITY = builder
                .comment("The density of the settlements, in percent of the default density.")
                .defineInRange("settlement_density", 100, 25, 400);
        builder.pop();

        SPEC = builder.build();
    }

    private GenesisConfig() {
    }

    /** The effective settlement density as a multiplier. */
    public static double settlementDensity() {
        return SETTLEMENT_DENSITY.get() / 100.0;
    }
}
