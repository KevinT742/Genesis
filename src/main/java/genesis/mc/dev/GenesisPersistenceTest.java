package genesis.mc.dev;

import genesis.api.magic.MagicInfluence;
import genesis.api.planet.PlanetDefinition;
import genesis.core.magic.MagicField;
import genesis.core.magic.MagicFieldModel;
import genesis.mc.magic.GenesisMagicData;

import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

/**
 * The self test of the persistence of the magic.
 *
 * <p>It lives in the Minecraft layer because it needs the NBT machinery of the game, and it is
 * kept apart from {@code genesis.dev.GenesisSelfTest} on purpose: that one is compiled against a
 * narrow class path, which proves that the algorithm layer does not depend on the game. This one
 * checks the part that has to.
 *
 * <p>What is checked here is the promise that a ritual survives a restart: the magic of a world
 * is written to disk, and reading it back gives the same world.
 */
public final class GenesisPersistenceTest {

    private static int checks;
    private static int failures;

    private GenesisPersistenceTest() {
    }

    public static void main(String[] args) {
        System.out.println("== The persistence of the magic ==");
        testRoundTrip();
        testDirtyFlag();

        System.out.println("checks  : " + checks);
        System.out.println("failures: " + failures);
        if (failures > 0) {
            System.out.println("PERSISTENCE TEST FAILED");
            System.exit(1);
        }
        System.out.println("PERSISTENCE TEST PASSED");
    }

    private static void testRoundTrip() {
        PlanetDefinition planet = PlanetDefinition.homeWorld();
        GenesisMagicData data = new GenesisMagicData();
        MagicField field = data.field(planet, new MagicFieldModel(planet, 20261021L));

        field.influence(120.0, -340.0, 96.0, 0.75);
        field.influence(-800.0, 250.0, 64.0, -0.25);
        check("a change of the magic reaches the stored world", data.influences().size() == 2);

        // The format the world is stored in has to survive a round trip, otherwise a ritual would
        // be lost when the server restarts.
        Tag encoded = GenesisMagicData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow();
        GenesisMagicData restored = GenesisMagicData.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
        check("the stored magic survives a round trip", restored.influences().size() == 2);

        MagicInfluence first = restored.influences().get(0);
        check("the stored magic keeps its numbers",
                Math.abs(first.x() - 120.0) < 1.0e-9
                        && Math.abs(first.z() - (-340.0)) < 1.0e-9
                        && Math.abs(first.radius() - 96.0) < 1.0e-9
                        && Math.abs(first.delta() - 0.75) < 1.0e-9);

        // A world that is read back continues where it left off.
        MagicField reloaded = restored.field(planet, new MagicFieldModel(planet, 20261021L));
        double ground = new MagicFieldModel(planet, 20261021L).baseConcentrationAt(120.0, -340.0);
        check("a world that is read back keeps the magic it was given",
                Math.abs((reloaded.concentrationAt(120.0, -340.0) - ground) - 0.75) < 1.0e-6);
    }

    private static void testDirtyFlag() {
        PlanetDefinition planet = PlanetDefinition.homeWorld();
        GenesisMagicData data = new GenesisMagicData();
        MagicField field = data.field(planet, new MagicFieldModel(planet, 20261021L));

        check("a world that was not touched is not written", !data.isDirty());
        field.influence(0.0, 0.0, 64.0, 0.5);
        check("a changed world is written to disk", data.isDirty());
    }

    private static void check(String name, boolean condition) {
        checks++;
        if (condition) {
            System.out.println("  [ok]   " + name);
        } else {
            failures++;
            System.out.println("  [FAIL] " + name);
        }
    }
}
