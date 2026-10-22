package genesis.mc;

import genesis.GenesisConfig;
import genesis.api.planet.PlanetDefinition;
import genesis.core.math.MathUtil;
import genesis.mc.world.GenesisChunkGenerator;
import genesis.mc.world.GenesisModels;
import genesis.mc.world.PlanetPlans;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/**
 * The server side events of Genesis.
 *
 * <p>The most important one is the boundary handling: when a player walks past the edge of the
 * world they are moved to the other side, so the planet never ends. Because the terrain is
 * generated on the surface of the planet, the ground on the other side is exactly the ground they
 * were standing on.
 */
public final class GenesisEvents {

    /** Clears the cached plans when a server starts. */
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        GenesisModels.clear();
        PlanetPlans.clear();
    }

    /** Clears the cached plans when a server stops. */
    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        GenesisModels.clear();
        PlanetPlans.clear();
    }

    /**
     * Moves a player back into the world when they leave it.
     *
     * <p>The longitude axis wraps: walking east past the end of the world continues at the
     * western end. The latitude axis is mirrored at the poles, exactly like folding a paper map
     * over its edge.
     */
    @SubscribeEvent
    public void onPlayerTick(PlayerEvent.PlayerTick event) {
        ServerPlayer player = event.getEntity();
        if (player.level() == null || player.connection == null) {
            return;
        }
        ServerLevel level = player.level();
        if (!(level.getChunkSource().getGenerator() instanceof GenesisChunkGenerator generator)) {
            return;
        }
        if (!GenesisConfig.CONTINUOUS_WORLD.get()) {
            return;
        }
        PlanetDefinition planet = generator.planet();
        double half = planet.halfSpan();

        double x = player.getX();
        double z = player.getZ();
        double newX = x;
        double newZ = z;

        if (x > half || x < -half) {
            newX = MathUtil.wrap(x, half);
        }
        if (z > half || z < -half) {
            newZ = z > half ? (2.0 * half - z) : (-2.0 * half - z);
        }

        if (newX != x || newZ != z) {
            player.connection.teleport(newX, player.getY(), newZ, player.getYRot(), player.getXRot());
        }
    }
}
