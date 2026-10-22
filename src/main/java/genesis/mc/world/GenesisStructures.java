package genesis.mc.world;

import genesis.api.planet.PlanetDefinition;
import genesis.core.math.Hash;
import genesis.core.settlement.RoadPath;
import genesis.core.settlement.SettlementSize;
import genesis.core.settlement.StructureSite;
import genesis.core.settlement.WorldPlan;
import genesis.mc.GenesisBlocksAccess;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;

/**
 * Places the structures of a Genesis planet into the world.
 *
 * <p>The structures are driven by the {@link WorldPlan} of the planet, which is computed once from
 * the world seed. Because the plan is global, the roads that connect two settlements can cross
 * chunk borders without any seams: every chunk simply draws the part of the network that falls
 * into it.
 */
public final class GenesisStructures {

    private GenesisStructures() {
    }

    /** Places every structure that touches the given chunk. */
    public static void place(net.minecraft.world.level.WorldGenLevel level, ChunkAccess chunk) {
        if (!(level instanceof WorldGenRegion region)) {
            return;
        }
        ServerLevel serverLevel = region.getLevel();
        ChunkGenerator generator = serverLevel.getChunkSource().getGenerator();
        if (!(generator instanceof GenesisChunkGenerator genesis)) {
            return;
        }
        PlanetDefinition planet = genesis.planet();
        WorldPlan plan = PlanetPlans.of(serverLevel, planet);

        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        int maxX = minX + 15;
        int maxZ = minZ + 15;

        placeRoads(genesis, chunk, plan, minX, minZ, maxX, maxZ);
        placeSettlements(genesis, chunk, plan, minX, minZ, maxX, maxZ);
        placeRuins(genesis, chunk, plan, minX, minZ, maxX, maxZ);
        placeHidden(genesis, chunk, plan, minX, minZ, maxX, maxZ);
    }

    // ---------------------------------------------------------------------
    // roads
    // ---------------------------------------------------------------------

    private static void placeRoads(GenesisChunkGenerator generator, ChunkAccess chunk, WorldPlan plan,
            int minX, int minZ, int maxX, int maxZ) {
        BlockState surface = roadBlock(RoadPath.RoadSurface.DIRT);
        for (RoadPath road : plan.roads()) {
            if (!intersects(road, minX, minZ, maxX, maxZ)) {
                continue;
            }
            BlockState state = roadBlock(road.surface());
            java.util.List<int[]> points = road.points();
            for (int i = 1; i < points.size(); i++) {
                int[] from = points.get(i - 1);
                int[] to = points.get(i);
                double dx = to[0] - from[0];
                double dz = to[1] - from[1];
                int steps = Math.max(1, (int) Math.ceil(Math.sqrt(dx * dx + dz * dz)));
                for (int step = 0; step <= steps; step++) {
                    double t = (double) step / steps;
                    int x = (int) Math.round(from[0] + dx * t);
                    int z = (int) Math.round(from[1] + dz * t);
                    if (x < minX || x > maxX || z < minZ || z > maxZ) {
                        continue;
                    }
                    int y = generator.heightAt(x, z, chunk.getHeightAccessorForGeneration());
                    if (y <= chunk.getMinY()) {
                        continue;
                    }
                    set(chunk, new BlockPos(x, y, z), state);
                }
            }
        }
    }

    private static boolean intersects(RoadPath road, int minX, int minZ, int maxX, int maxZ) {
        for (int[] point : road.points()) {
            if (point[0] >= minX - 32 && point[0] <= maxX + 32 && point[1] >= minZ - 32 && point[1] <= maxZ + 32) {
                return true;
            }
        }
        return false;
    }

    private static BlockState roadBlock(RoadPath.RoadSurface surface) {
        return switch (surface) {
            case DIRT -> GenesisBlocksAccess.packedEarth();
            case GRAVEL -> GenesisBlocksAccess.gravelPath();
            case COBBLESTONE -> GenesisBlocksAccess.cobblePath();
        };
    }

    // ---------------------------------------------------------------------
    // settlements
    // ---------------------------------------------------------------------

    private static void placeSettlements(GenesisChunkGenerator generator, ChunkAccess chunk, WorldPlan plan,
            int minX, int minZ, int maxX, int maxZ) {
        for (StructureSite site : plan.settlements()) {
            int radius = site.radius();
            if (site.x() + radius < minX || site.x() - radius > maxX
                    || site.z() + radius < minZ || site.z() - radius > maxZ) {
                continue;
            }
            buildSettlement(generator, chunk, site, minX, minZ, maxX, maxZ);
        }
    }

    private static void buildSettlement(GenesisChunkGenerator generator, ChunkAccess chunk, StructureSite site,
            int minX, int minZ, int maxX, int maxZ) {
        int buildings = site.size().buildingCount();
        int spacing = site.size() == SettlementSize.CITY ? 12 : 10;
        BlockState wall = GenesisBlocksAccess.planks();
        BlockState floor = GenesisBlocksAccess.floor();
        BlockState path = GenesisBlocksAccess.cobblePath();

        for (int index = 0; index < buildings; index++) {
            long hash = Hash.mix2(site.seed(), index, 0xB01D);
            double angle = Hash.toUnit(Hash.mix2(hash, 1, 2)) * Math.PI * 2.0;
            double distance = 4.0 + Hash.toUnit(Hash.mix2(hash, 3, 4)) * site.radius() * 0.8;
            int centerX = site.x() + (int) Math.round(Math.cos(angle) * distance);
            int centerZ = site.z() + (int) Math.round(Math.sin(angle) * distance);
            int half = 2 + (int) (Hash.toUnit(Hash.mix2(hash, 5, 6)) * 2.0);

            for (int dx = -half; dx <= half; dx++) {
                for (int dz = -half; dz <= half; dz++) {
                    boolean edge = dx == -half || dx == half || dz == -half || dz == half;
                    int x = centerX + dx;
                    int z = centerZ + dz;
                    if (x < minX || x > maxX || z < minZ || z > maxZ) {
                        continue;
                    }
                    int y = generator.heightAt(x, z, chunk.getHeightAccessorForGeneration());
                    if (y <= chunk.getMinY()) {
                        continue;
                    }
                    if (edge) {
                        set(chunk, new BlockPos(x, y, z), wall);
                        set(chunk, new BlockPos(x, y + 1, z), wall);
                    } else {
                        set(chunk, new BlockPos(x, y, z), floor);
                        set(chunk, new BlockPos(x, y + 1, z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                    }
                }
            }
            // A short path leading to the house.
            for (int step = 0; step < 4; step++) {
                int x = centerX + (int) Math.round(Math.cos(angle) * (half + 1 + step));
                int z = centerZ + (int) Math.round(Math.sin(angle) * (half + 1 + step));
                if (x < minX || x > maxX || z < minZ || z > maxZ) {
                    continue;
                }
                int y = generator.heightAt(x, z, chunk.getHeightAccessorForGeneration());
                if (y <= chunk.getMinY()) {
                    continue;
                }
                set(chunk, new BlockPos(x, y, z), path);
            }
        }
    }

    // ---------------------------------------------------------------------
    // ruins
    // ---------------------------------------------------------------------

    private static void placeRuins(GenesisChunkGenerator generator, ChunkAccess chunk, WorldPlan plan,
            int minX, int minZ, int maxX, int maxZ) {
        for (StructureSite site : plan.ruins()) {
            if (site.x() + site.radius() < minX || site.x() - site.radius() > maxX
                    || site.z() + site.radius() < minZ || site.z() - site.radius() > maxZ) {
                continue;
            }
            BlockState wall = Blocks.COBBLESTONE.defaultBlockState();
            BlockState mossy = Blocks.MOSSY_COBBLESTONE.defaultBlockState();
            int half = site.radius() / 2;
            for (int dx = -half; dx <= half; dx++) {
                for (int dz = -half; dz <= half; dz++) {
                    boolean edge = dx == -half || dx == half || dz == -half || dz == half;
                    if (!edge) {
                        continue;
                    }
                    // Parts of the wall have collapsed.
                    long hash = Hash.mix2(site.seed(), dx, dz);
                    if (Hash.toUnit(hash) < 0.35) {
                        continue;
                    }
                    int x = site.x() + dx;
                    int z = site.z() + dz;
                    if (x < minX || x > maxX || z < minZ || z > maxZ) {
                        continue;
                    }
                    int y = generator.heightAt(x, z, chunk.getHeightAccessorForGeneration());
                    if (y <= chunk.getMinY()) {
                        continue;
                    }
                    set(chunk, new BlockPos(x, y, z), Hash.toUnit(Hash.mix2(hash, 7, 9)) < 0.5 ? mossy : wall);
                    set(chunk, new BlockPos(x, y + 1, z), wall);
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // hidden sites
    // ---------------------------------------------------------------------

    private static void placeHidden(GenesisChunkGenerator generator, ChunkAccess chunk, WorldPlan plan,
            int minX, int minZ, int maxX, int maxZ) {
        for (StructureSite site : plan.hidden()) {
            if (site.x() + site.radius() < minX || site.x() - site.radius() > maxX
                    || site.z() + site.radius() < minZ || site.z() - site.radius() > maxZ) {
                continue;
            }
            BlockState shell = Blocks.DEEPSLATE.defaultBlockState();
            BlockState core = GenesisBlocksAccess.crystal();
            int y = generator.heightAt(site.x(), site.z(), chunk.getHeightAccessorForGeneration());
            if (y <= chunk.getMinY() + 4) {
                continue;
            }
            // The chamber is carved into the ground and roofed over.
            int roof = y - 2;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    for (int dy = 0; dy <= 3; dy++) {
                        int x = site.x() + dx;
                        int z = site.z() + dz;
                        if (x < minX || x > maxX || z < minZ || z > maxZ) {
                            continue;
                        }
                        boolean border = Math.abs(dx) == 2 || Math.abs(dz) == 2 || dy == 0 || dy == 3;
                        if (border) {
                            set(chunk, new BlockPos(x, roof - dy, z), shell);
                        } else {
                            set(chunk, new BlockPos(x, roof - dy, z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                        }
                    }
                }
            }
            if (site.x() >= minX && site.x() <= maxX && site.z() >= minZ && site.z() <= maxZ) {
                set(chunk, new BlockPos(site.x(), roof - 1, site.z()), core);
            }
        }
    }

    // ---------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------

    private static void set(ChunkAccess chunk, BlockPos pos, BlockState state) {
        if (pos.getY() < chunk.getMinY() || pos.getY() >= chunk.getMinY() + chunk.getHeight()) {
            return;
        }
        chunk.setBlockState(pos, state, 2);
    }
}
