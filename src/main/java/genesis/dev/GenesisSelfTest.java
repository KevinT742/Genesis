package genesis.dev;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import genesis.api.item.ItemCategory;
import genesis.api.item.ItemRarity;
import genesis.api.item.ItemTemplate;
import genesis.api.magic.MagicChannel;
import genesis.api.magic.MagicChannelKind;
import genesis.api.magic.MagicHost;
import genesis.api.magic.MagicInput;
import genesis.api.magic.MagicMaterial;
import genesis.api.magic.MagicOrgan;
import genesis.api.magic.MagicOrganKind;
import genesis.api.magic.MagicTier;
import genesis.api.physical.Temperature;
import genesis.api.material.BlockTemplate;
import genesis.api.material.Composition;
import genesis.api.material.ElementAffinity;
import genesis.api.material.MaterialFamily;
import genesis.api.material.MaterialProfile;
import genesis.api.material.MaterialRegistry;
import genesis.api.material.Phase;
import genesis.api.planet.PlanetDefinition;
import genesis.core.ecology.BiomeKind;
import genesis.core.ecology.EcologyModel;
import genesis.core.magic.MagicField;
import genesis.core.magic.MagicFieldModel;
import genesis.core.math.Hash;
import genesis.core.settlement.RoadPath;
import genesis.core.settlement.SettlementPlanner;
import genesis.core.settlement.StructureSite;
import genesis.core.settlement.WorldPlan;
import genesis.core.terrain.HydrologyModel;
import genesis.core.terrain.TerrainModel;
import genesis.core.terrain.TerrainSample;
import genesis.core.topology.PlanetSurface;

/**
 * The self test of the Genesis algorithms.
 *
 * <p>It runs without a Minecraft instance and checks the promises of the mod:
 * <ol>
 *   <li>the material template produces consistent values,</li>
 *   <li>the physics and the chemistry of a material follow from its composition,</li>
 *   <li>the magic model tells materials, organs, items and channels apart,</li>
 *   <li>the magic of a region differs from region to region and can be changed from outside,</li>
 *   <li>the terrain, the water system and the ecosystems behave like a real landscape,</li>
 *   <li>the world is continuous across its boundary,</li>
 *   <li>and the structures are placed and connected.</li>
 * </ol>
 *
 * <p>Run it with {@code ./gradlew :base:selfTest} from the family repository root.
 */
public final class GenesisSelfTest {

    private static int checks;
    private static int failures;

    private GenesisSelfTest() {
    }

    public static void main(String[] args) {
        testMaterialTemplate();
        testMaterialPhysicsAndChemistry();
        testMagicModel();
        testMagicField();
        testMagicStorage();
        testPlanetContinuity();
        testTerrainAndHydrology();
        testEcology();
        testStructuresAndRoads();
        testDeterminism();

        System.out.println();
        System.out.println("checks  : " + checks);
        System.out.println("failures: " + failures);
        if (failures > 0) {
            System.out.println("SELF TEST FAILED");
            System.exit(1);
        }
        System.out.println("SELF TEST PASSED");
    }

    // ---------------------------------------------------------------------
    // 1. the unified material template
    // ---------------------------------------------------------------------

    private static void testMaterialTemplate() {
        section("Material template");
        MaterialProfile stone = MaterialFamily.rock("genesis:test_stone");
        check("rock hardness is positive", stone.miningHardness() > 0.0);
        check("rock needs the correct tool", stone.requiresCorrectTool());
        check("rock is not flammable", !stone.chemical().isFlammable());
        check("rock conducts no current", !stone.chemical().isConductive());

        MaterialProfile metal = MaterialFamily.metal("genesis:test_metal", "Fe", 5.0);
        check("metal conducts a current", metal.chemical().isConductive());
        check("metal has a higher hardness than stone", metal.miningHardness() > stone.miningHardness());

        MaterialProfile arcane = MaterialFamily.arcane("genesis:test_arcane");
        check("arcane is a catalyst", arcane.isCatalyst());
        check("arcane reacts with the void", arcane.magic().reactsWith(ElementAffinity.VOID));

        MaterialProfile wood = MaterialFamily.wood("genesis:test_wood");
        check("wood is flammable", wood.chemical().isFlammable());
        check("wood melts at a low temperature", wood.physical().meltingPoint().kelvin() < 1000.0);

        BlockTemplate template = BlockTemplate.builder("test_brick")
                .profile(stone)
                .toolTier(BlockTemplate.ToolTier.STONE)
                .sound(BlockTemplate.SoundKind.STONE)
                .build();
        check("the template keeps the profile", template.profile() == stone);
        check("the template derives the hardness", template.miningHardness() == stone.miningHardness());

        MaterialRegistry.register(stone);
        check("the registry finds the profile", MaterialRegistry.get("genesis:test_stone").isPresent());
    }

    // ---------------------------------------------------------------------
    // 2. the physics and the chemistry of the materials
    // ---------------------------------------------------------------------

    private static void testMaterialPhysicsAndChemistry() {
        section("Material physics and chemistry");

        Composition quartz = Composition.parse("SiO2");
        check("the formula is parsed", quartz.countOf("SI") == 1 && quartz.countOf("O") == 2);
        check("the molar mass follows from the composition",
                Math.abs(quartz.molarMass() - 60.084) < 0.2);
        check("the mass fraction of oxygen is about 53 percent",
                Math.abs(quartz.massFraction("O") - 0.533) < 0.02);
        check("the formula is written in Hill notation",
                Composition.parse("H2O").formula().equals("H2O")
                        && Composition.parse("H2O2").formula().equals("H2O2")
                        && Composition.parse("C6H10O5").formula().equals("C6H10O5"));
        check("parentheses are parsed",
                Composition.parse("Ca(OH)2").countOf("O") == 2
                        && Composition.parse("Ca(OH)2").countOf("CA") == 1);

        MaterialProfile stone = MaterialFamily.rock("genesis:test_stone_2");
        check("a block weighs its density in kilogrammes", Math.abs(stone.mass() - 2600.0) < 1.0e-6);
        check("a block occupies one cubic metre", Math.abs(stone.volume() - 1.0) < 1.0e-9);
        check("stone is solid at room temperature",
                stone.phaseAt(Temperature.ofCelsius(20.0)) == Phase.SOLID);
        check("stone melts above its melting point",
                stone.phaseAt(Temperature.ofCelsius(1800.0)) == Phase.LIQUID);
        check("water is liquid at room temperature",
                MaterialFamily.liquid("genesis:test_water").phaseAt(Temperature.ofCelsius(20.0)) == Phase.LIQUID);
        check("ice turns into water above zero degrees",
                MaterialFamily.frozen("genesis:test_ice").phaseAt(Temperature.ofCelsius(20.0)) == Phase.LIQUID);

        MaterialProfile iron = MaterialFamily.metal("genesis:test_iron", "Fe", 5.0);
        check("iron melts at 1538 degrees", Math.abs(iron.physical().meltingPoint().celsius() - 1538.0) < 0.5);
        check("the melting point is thermodynamic",
                Math.abs(iron.physical().meltingPoint().kelvin() - 1811.15) < 0.5);
        check("iron weighs 7874 kilogrammes per cubic metre", Math.abs(iron.density() - 7874.0) < 0.5);
        check("the molar mass of iron follows from the element", Math.abs(iron.molarMass() - 55.845) < 0.01);
        check("iron conducts a current", iron.chemical().isConductive());
        check("iron is a pure element", iron.chemical().isElement());

        // A temperature is one scale: kelvin and celsius describe the same point.
        Temperature freezing = Temperature.WATER_FREEZING;
        check("the freezing point of water is 273.15 kelvin", Math.abs(freezing.kelvin() - 273.15) < 1.0e-9);
        check("the freezing point of water is 0 degrees celsius", Math.abs(freezing.celsius()) < 1.0e-9);
        check("the display shows both scales", freezing.format().equals("273.15K（0℃）"));
        check("the display trims the zeros that say nothing",
                Temperature.ofKelvin(1811.15).format().equals("1811.15K（1538℃）"));
        check("a temperature difference is the same in both scales",
                Math.abs(Temperature.ofCelsius(30.0).differenceTo(freezing) - 30.0) < 1.0e-9);
        boolean rejected = false;
        try {
            Temperature.ofCelsius(-300.0);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        check("a temperature cannot be below absolute zero", rejected);
        check("cooling never goes below absolute zero",
                Temperature.ofKelvin(50.0).minusKelvin(100.0).kelvin() == 0.0);

        MaterialProfile wood = MaterialFamily.wood("genesis:test_wood_2");
        check("wood gives off heat when it burns", wood.chemical().heatOfCombustion() > 10.0);
        check("wood is a compound", wood.chemical().isCompound());
    }

    // ---------------------------------------------------------------------
    // 3. the magic model
    // ---------------------------------------------------------------------

    private static void testMagicModel() {
        section("The magic model");

        MaterialProfile shard = MaterialFamily.magicMaterial("genesis:test_shard", 0.0002);
        check("a magic material adapts to magic", shard.magic().isMagicMaterial());
        check("a magic material keeps the magic it took up", shard.solidifiedMagic() > 50.0);
        check("stone is no magic material",
                !MaterialFamily.rock("genesis:test_stone_3").isMagicMaterial());
        check("a crystal can be used as a catalyst",
                MaterialFamily.crystal("genesis:test_crystal").isCatalyst());

        MagicOrgan soul = MagicOrgan.soul("genesis:test_soul", 400.0);
        MagicOrgan heart = MagicOrgan.animal("genesis:test_heart", MagicOrganKind.ORGAN, 400.0);
        check("a human soul is a magic organ", soul.isSoul() && soul.host() == MagicHost.HUMAN);
        check("a soul is stronger than an animal organ", soul.output() > heart.output());
        check("a soul can be used in brewing", soul.isBrewable());
        check("a root takes magic up from the ground",
                MagicOrgan.plant("genesis:test_root", MagicOrganKind.ROOT, 120.0).kind().isAbsorbing());
        check("a leaf makes magic out of light",
                MagicOrgan.plant("genesis:test_leaf", MagicOrganKind.LEAF, 60.0).kind().isProducing());

        MagicChannel alchemy = MagicChannel.alchemy("genesis:test_alchemy", 0.9, 0.85, 10.0);
        MagicChannel enchantment = MagicChannel.enchantment("genesis:test_enchantment", 0.8, 0.9, 12.0);
        MagicChannel spell = MagicChannel.spell("genesis:test_spell", 0.7, 0.6, 20.0, 32.0);
        check("alchemy is a solidified channel", alchemy.isSolidified() && !alchemy.isFree());
        check("enchanting is a solidified channel", enchantment.isSolidified());
        check("a spell is not solidified", spell.isFree());
        check("a spell needs a living speaker",
                spell.needsSpeaker() && spell.kind() == MagicChannelKind.SPELL);
        check("a channel carries its cost", alchemy.cost() == 10.0);

        ItemTemplate vial = ItemTemplate.builder("test_vial")
                .profile(MaterialFamily.solidifiedMagic("genesis:test_vial", 0.0005))
                .maxStackSize(16)
                .rarity(ItemRarity.RARE)
                .category(ItemCategory.BREWING)
                .magicInput(MagicInput.ESSENCE)
                .build();
        check("an item knows how many of it fit into a slot", vial.maxStackSize() == 16);
        check("an item knows how rare it is", vial.rarity() == ItemRarity.RARE);
        check("an item knows where it belongs", vial.category() == ItemCategory.BREWING);
        check("an item knows what it is for magic", vial.magicInput() == MagicInput.ESSENCE);
        check("an item weighs what its material weighs", Math.abs(vial.mass() - 0.6) < 1.0e-6);
        check("a potion is brewable", vial.isBrewable());
        check("a potion is no magic material", !vial.isMagicMaterial());

        ItemTemplate shardItem = ItemTemplate.builder("test_shard_item")
                .profile(MaterialFamily.magicMaterial("genesis:test_shard_item", 0.0002))
                .magicInput(MagicInput.MATERIAL)
                .build();
        check("a magic material is a magic material", shardItem.isMagicMaterial());
        check("a magic material is brewable", shardItem.isBrewable());
        MagicMaterial material = shardItem.asMagicMaterial();
        check("a magic material carries magic", material != null && material.storedPower() > 50.0);
        check("a magic material has a strength", material.tier() != MagicTier.NONE);

        check("an organ is an ingredient of brewing", MagicInput.ORGAN.isBrewable());
        check("an ordinary item is not", !MagicInput.NONE.isBrewable());
    }

    // ---------------------------------------------------------------------
    // 4. the magic of the world
    // ---------------------------------------------------------------------

    private static void testMagicField() {
        section("The magic field");
        PlanetDefinition planet = PlanetDefinition.homeWorld();
        MagicField field = new MagicField(new MagicFieldModel(planet, 20261021L));

        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        Set<MagicTier> tiers = new HashSet<>();
        for (int x = -8000; x < 8000; x += 233) {
            for (int z = -8000; z < 8000; z += 233) {
                double concentration = field.concentrationAt(x, z);
                min = Math.min(min, concentration);
                max = Math.max(max, concentration);
                tiers.add(field.tierAt(x, z));
            }
        }
        System.out.printf("  magic concentration %.3f .. %.3f%n", min, max);
        check("the magic is never negative", min >= 0.0);
        check("the magic differs from region to region", max - min > 0.3);
        check("the magic of a region stays in reach", max < 10.0);
        check("the magic of the world falls into several strengths", tiers.size() >= 3);

        // The magic is seamless across the world boundary, exactly like the terrain is.
        double half = planet.halfSpan();
        double east = field.concentrationAt(half + 0.5, 1200.0);
        double west = field.concentrationAt(-half + 0.5, 1200.0);
        check("the magic is continuous across the boundary", Math.abs(east - west) < 1.0e-9);

        // The same seed gives the same magic.
        MagicField other = new MagicField(new MagicFieldModel(planet, 20261021L));
        boolean same = true;
        for (int i = 0; i < 400; i++) {
            int x = i * 41 - 8000;
            if (Math.abs(field.concentrationAt(x, x * 3) - other.concentrationAt(x, x * 3)) > 1.0e-9) {
                same = false;
                break;
            }
        }
        check("the same seed always produces the same magic", same);

        // An outside operation changes the magic of a region, and nothing else.
        MagicField untouched = new MagicField(new MagicFieldModel(planet, 20261021L));
        double before = field.concentrationAt(0, 0);
        field.influence(0, 0, 128.0, 0.6);
        double after = field.concentrationAt(0, 0);
        check("an outside operation changes the magic", Math.abs((after - before) - 0.6) < 1.0e-6);
        check("an outside operation does not reach the whole world",
                Math.abs(field.concentrationAt(6000, 6000) - untouched.concentrationAt(6000, 6000)) < 1.0e-9);
        field.influence(0, 0, 128.0, -100.0);
        check("the magic never becomes negative", field.concentrationAt(0, 0) >= 0.0);

        // A thin, dry world carries less magic than the home world.
        check("a dry world carries less magic than a wet one",
                MagicFieldModel.baselineOf(PlanetDefinition.ember())
                        < MagicFieldModel.baselineOf(PlanetDefinition.homeWorld()));
    }

    // ---------------------------------------------------------------------
    // 4b. the storage of the magic
    // ---------------------------------------------------------------------

    private static void testMagicStorage() {
        section("The storage of the magic");
        PlanetDefinition planet = PlanetDefinition.homeWorld();
        MagicFieldModel model = new MagicFieldModel(planet, 20261021L);

        // The field writes into the list it was given. That is how the magic of a world reaches
        // the disk: the world keeps the list, the field writes into it.
        java.util.List<genesis.api.magic.MagicInfluence> stored = new java.util.ArrayList<>();
        MagicField field = new MagicField(model, stored);
        field.influence(120.0, -340.0, 96.0, 0.75);
        check("a change of the magic reaches the list it was given", stored.size() == 1);

        // A world that is read back continues where it left off.
        MagicField reloaded = new MagicField(model, stored);
        double ground = model.baseConcentrationAt(120.0, -340.0);
        check("a world that is read back keeps the magic it was given",
                Math.abs((reloaded.concentrationAt(120.0, -340.0) - ground) - 0.75) < 1.0e-6);

        // And the change is announced, so that the world knows that it has to be written.
        int[] announced = { 0 };
        field.setChangeListener(changed -> announced[0]++);
        field.influence(0.0, 0.0, 32.0, 0.5);
        check("a change of the magic is announced", announced[0] == 1);
        field.clearInfluences();
        check("undoing every influence is announced too", announced[0] == 2);
        check("undoing every influence empties the list", stored.isEmpty());
    }

    // ---------------------------------------------------------------------
    // 5. the continuity of the world
    // ---------------------------------------------------------------------

    private static void testPlanetContinuity() {
        section("Planet continuity");
        PlanetDefinition planet = PlanetDefinition.homeWorld();
        PlanetSurface surface = new PlanetSurface(planet.halfSpan(), planet.radius(), planet.samplingMode());
        double half = planet.halfSpan();

        // The block just past the eastern edge is the same block as the one just past the western
        // edge, so their surface points have to be identical.
        genesis.core.topology.SurfacePoint east = surface.toSurface(half + 0.5, 1200.0);
        genesis.core.topology.SurfacePoint west = surface.toSurface(-half + 0.5, 1200.0);
        double gap = Math.abs(east.x() - west.x()) + Math.abs(east.y() - west.y()) + Math.abs(east.z() - west.z());
        check("the surface is continuous across the boundary (gap " + String.format("%.6f", gap) + ")", gap < 1.0e-9);

        TerrainModel terrain = new TerrainModel(planet, 4242L);
        int maxDifference = 0;
        for (int z = -3000; z <= 3000; z += 137) {
            double a = terrain.sample(half + 0.5, z).height();
            double b = terrain.sample(-half + 0.5, z).height();
            maxDifference = Math.max(maxDifference, (int) Math.ceil(Math.abs(a - b) * 100.0));
        }
        check("the terrain is continuous across the boundary (max difference " + maxDifference + ")",
                maxDifference == 0);

        // The pole folds back onto the same meridian.
        genesis.core.topology.SurfacePoint poleA = surface.toSurface(0.0, half - 0.5);
        genesis.core.topology.SurfacePoint poleB = surface.toSurface(0.0, half + 0.5);
        check("the pole is mirrored onto the same meridian", Math.abs(poleA.x() - poleB.x()) < 1.0e-6);

        double wrapped = surface.wrapLongitude(half + 12.5);
        check("the longitude wraps into the world", Math.abs(wrapped - (-half + 12.5)) < 1.0e-6);
    }

    // ---------------------------------------------------------------------
    // 6. terrain, water and ecosystems
    // ---------------------------------------------------------------------

    private static void testTerrainAndHydrology() {
        section("Terrain and hydrology");
        PlanetDefinition planet = PlanetDefinition.homeWorld();
        TerrainModel terrain = new TerrainModel(planet, 1337L);
        HydrologyModel hydrology = new HydrologyModel(terrain, planet);

        int samples = 0;
        int submerged = 0;
        double minHeight = Double.MAX_VALUE;
        double maxHeight = -Double.MAX_VALUE;
        for (int x = -6000; x < 6000; x += 61) {
            for (int z = -6000; z < 6000; z += 61) {
                TerrainSample sample = terrain.sample(x, z);
                samples++;
                if (sample.submerged()) {
                    submerged++;
                }
                minHeight = Math.min(minHeight, sample.height());
                maxHeight = Math.max(maxHeight, sample.height());
            }
        }
        double oceanRatio = submerged / (double) samples;
        System.out.printf("  sampled %d columns, ocean ratio %.3f, height range %.1f .. %.1f%n",
                samples, oceanRatio, minHeight, maxHeight);
        check("the world contains both land and ocean", oceanRatio > 0.05 && oceanRatio < 0.85);
        check("the terrain has real relief", maxHeight - minHeight > 80.0);

        int rivers = 0;
        int lakes = 0;
        int checked = 0;
        for (int x = -4000; x < 4000; x += 37) {
            for (int z = -4000; z < 4000; z += 37) {
                checked++;
                if (hydrology.isRiver(x, z)) {
                    rivers++;
                }
                if (hydrology.waterDepthAt(x, z) > 0.05) {
                    lakes++;
                }
            }
        }
        double riverRatio = rivers / (double) checked;
        double lakeRatio = lakes / (double) checked;
        System.out.printf("  river ratio %.4f, lake ratio %.4f%n", riverRatio, lakeRatio);
        check("the water system produces rivers", riverRatio > 0.0005);
        check("the water system fills depressions into lakes", lakeRatio > 0.0002);

        // Physical invariant: the water surface can never lie below the ground it rests on.
        boolean surfaceAboveGround = true;
        for (int x = -4000; x < 4000 && surfaceAboveGround; x += 53) {
            for (int z = -4000; z < 4000; z += 53) {
                double surface = hydrology.waterSurfaceAt(x, z);
                double floor = hydrology.terrainFloor(x, z);
                if (surface < floor - 0.001) {
                    surfaceAboveGround = false;
                    break;
                }
            }
        }
        check("the water surface never lies below the ground", surfaceAboveGround);

        // Water has to converge: a river cell always has a neighbour that carries more water.
        boolean converges = true;
        int inspected = 0;
        for (int x = -4000; x < 4000 && inspected < 400; x += 53) {
            for (int z = -4000; z < 4000 && inspected < 400; z += 53) {
                double flow = hydrology.flowAt(x, z);
                if (flow < 400.0) {
                    continue;
                }
                inspected++;
                double best = 0.0;
                for (int dx = -8; dx <= 8; dx += 8) {
                    for (int dz = -8; dz <= 8; dz += 8) {
                        if (dx == 0 && dz == 0) {
                            continue;
                        }
                        best = Math.max(best, hydrology.flowAt(x + dx, z + dz));
                    }
                }
                if (best <= flow) {
                    converges = false;
                    break;
                }
            }
        }
        check("the water converges into bigger rivers (" + inspected + " river cells inspected)", converges);
    }

    private static void testEcology() {
        section("Ecology");
        PlanetDefinition planet = PlanetDefinition.homeWorld();
        TerrainModel terrain = new TerrainModel(planet, 77L);
        EcologyModel ecology = new EcologyModel(terrain, planet);

        BiomeKind equator = ecology.classifyAt(0.0, 0.0, 0.0, false);
        BiomeKind pole = ecology.classifyAt(0.0, planet.halfSpan() - 200.0, 0.0, true);
        System.out.println("  equator=" + equator + " pole=" + pole);
        check("the poles are colder than the equator", pole.isFrozen() || pole == BiomeKind.SNOWY_TUNDRA);
        check("the equator is not a frozen biome", !equator.isFrozen());

        Set<BiomeKind> found = new HashSet<>();
        for (int x = -8000; x < 8000; x += 211) {
            for (int z = -8000; z < 8000; z += 211) {
                found.add(ecology.classifyAt(x, z, 0.0, false));
            }
        }
        System.out.println("  biome families found: " + found);
        check("the ecosystem produces several biomes", found.size() >= 4);
    }

    // ---------------------------------------------------------------------
    // 7. structures and roads
    // ---------------------------------------------------------------------

    private static void testStructuresAndRoads() {
        section("Structures and roads");
        PlanetDefinition planet = PlanetDefinition.homeWorld();
        TerrainModel terrain = new TerrainModel(planet, 2024L);
        long start = System.currentTimeMillis();
        WorldPlan plan = new SettlementPlanner(2024L, planet, terrain).plan();
        long elapsed = System.currentTimeMillis() - start;

        System.out.printf("  %d settlements, %d roads, %d ruins, %d hidden (%d ms)%n",
                plan.settlements().size(), plan.roads().size(), plan.ruins().size(), plan.hidden().size(), elapsed);
        check("the planner produces settlements", plan.settlements().size() >= 8);
        check("the planner produces roads", plan.roads().size() >= 5);
        check("the planner produces ruins", plan.ruins().size() >= 2);
        check("the planner produces hidden sites", plan.hidden().size() >= 1);
        check("the planner is fast enough", elapsed < 60_000);

        // Every settlement must be reachable through the road network.
        Set<Integer> reachable = new HashSet<>();
        List<List<Integer>> adjacency = new ArrayList<>();
        for (int i = 0; i < plan.settlements().size(); i++) {
            adjacency.add(new ArrayList<>());
        }
        for (RoadPath road : plan.roads()) {
            adjacency.get(road.from()).add(road.to());
            adjacency.get(road.to()).add(road.from());
        }
        if (!adjacency.isEmpty()) {
            List<Integer> stack = new ArrayList<>();
            stack.add(0);
            reachable.add(0);
            while (!stack.isEmpty()) {
                int current = stack.remove(stack.size() - 1);
                for (int next : adjacency.get(current)) {
                    if (reachable.add(next)) {
                        stack.add(next);
                    }
                }
            }
        }
        check("every settlement is reachable by road (" + reachable.size() + "/" + plan.settlements().size() + ")",
                reachable.size() == plan.settlements().size());

        // Every road must actually reach the settlement it claims to connect.
        boolean roadsConnect = true;
        for (RoadPath road : plan.roads()) {
            StructureSite from = plan.settlements().get(road.from());
            StructureSite to = plan.settlements().get(road.to());
            if (road.points().isEmpty()) {
                roadsConnect = false;
                break;
            }
            int[] start2 = road.points().get(0);
            int[] end = road.points().get(road.points().size() - 1);
            if (Math.abs(start2[0] - from.x()) > 64 || Math.abs(start2[1] - from.z()) > 64
                    || Math.abs(end[0] - to.x()) > 64 || Math.abs(end[1] - to.z()) > 64) {
                roadsConnect = false;
                break;
            }
        }
        check("every road connects the settlements it links", roadsConnect);

        // The three families must be distinct.
        check("the structure families are distinct",
                plan.settlements().get(0).kind() != plan.ruins().get(0).kind());
    }

    private static void testDeterminism() {
        section("Determinism");
        PlanetDefinition planet = PlanetDefinition.homeWorld();
        TerrainModel first = new TerrainModel(planet, 5150L);
        TerrainModel second = new TerrainModel(planet, 5150L);
        boolean same = true;
        for (int i = 0; i < 500; i++) {
            int x = i * 37 - 9000;
            if (Math.abs(first.sample(x, x * 3).height() - second.sample(x, x * 3).height()) > 1.0e-9) {
                same = false;
                break;
            }
        }
        check("the same seed always produces the same terrain", same);

        WorldPlan planA = new SettlementPlanner(5150L, planet, first).plan();
        WorldPlan planB = new SettlementPlanner(5150L, planet, second).plan();
        check("the same seed always produces the same settlements",
                planA.settlements().size() == planB.settlements().size()
                        && planA.settlements().get(0).x() == planB.settlements().get(0).x());
    }

    // ---------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------

    private static void section(String name) {
        System.out.println();
        System.out.println("== " + name + " ==");
    }

    private static void check(String description, boolean condition) {
        checks++;
        if (condition) {
            System.out.println("  [ok]   " + description);
        } else {
            failures++;
            System.out.println("  [FAIL] " + description);
        }
    }
}

