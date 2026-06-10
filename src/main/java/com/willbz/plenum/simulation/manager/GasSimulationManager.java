package com.willbz.plenum.simulation.manager;

import com.willbz.plenum.api.simulation.GasCellWorld;
import com.willbz.plenum.client.debug.ClientboundGasDebugPacket;
import com.willbz.plenum.client.debug.GasDebugCell;
import com.willbz.plenum.simulation.constants.GasSimulationConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import static com.willbz.plenum.simulation.constants.GasSimulationConstants.*;

// TEMP
// This should be saved per level
public class GasSimulationManager {
    private static final Map<ServerLevel, GasCellWorld> WORLDS = new WeakHashMap<>();

    private GasSimulationManager() {}

    public static GasCellWorld get(ServerLevel level) {
        return WORLDS.computeIfAbsent(level, ignored -> new GasCellWorld());
    }

    public static void wakeGasAround(ServerLevel level, BlockPos pos) {
        get(level).wakeGasAround(pos);
    }

    public static void onBlockChanged(ServerLevel level, BlockPos pos) {
        GasCellWorld gasWorld = get(level);
        gasWorld.invalidateOccupancySectionsAround(pos);
        gasWorld.wakeGasAround(pos);
    }

    public static void tick(ServerLevel level) {
        boolean shouldRunGameElements = level.getServer().tickRateManager().runsNormally();

        if (shouldRunGameElements && level.getGameTime() % SIMULATION_INTERVAL_TICKS == 0L) {
            get(level).tick(level);
        }

        if (GasSimulationConstants.DEBUG_SYNC_GAS_CELLS
                && shouldRunGameElements
                && level.getGameTime() % GasSimulationConstants.DEBUG_SYNC_INTERVAL_TICKS == 0L) {
            syncDebugGasCells(level);
        }
    }

    private static void syncDebugGasCells(ServerLevel level) {
        GasCellWorld gasWorld = get(level);

        for (ServerPlayer player : level.players()) {
            List<GasDebugCell> snapshot = gasWorld.createDebugSnapshot(
                    player,
                    DEBUG_SYNC_HORIZONTAL_RANGE,
                    DEBUG_SYNC_VERTICAL_RANGE,
                    DEBUG_SYNC_MAX_CELLS_PER_PLAYER
            );

            PacketDistributor.sendToPlayer(player, new ClientboundGasDebugPacket(snapshot));
        }
    }
}
