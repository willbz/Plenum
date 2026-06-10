package com.willbz.plenum.api.simulation;

import com.willbz.plenum.api.gas.GasEffect;
import com.willbz.plenum.api.gas.GasStack;
import com.willbz.plenum.client.debug.GasDebugCell;
import com.willbz.plenum.simulation.solver.GasFlowSimulator;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

import static com.willbz.plenum.Plenum.LOGGER;
import static com.willbz.plenum.simulation.constants.GasSimulationConstants.MIN_GAS_AMOUNT;

public class GasCellWorld implements GasCellAccess {
    private static final int SECTION_SIZE = 16;
    private static final int SECTION_CELL_COUNT = SECTION_SIZE * SECTION_SIZE * SECTION_SIZE;

    private final Long2ObjectOpenHashMap<GasCell> cells = new Long2ObjectOpenHashMap<>();
    private final LongOpenHashSet activeCells = new LongOpenHashSet();

    private final Long2ObjectOpenHashMap<GasOccupancySection> gasOccupancySections = new Long2ObjectOpenHashMap<>();
    private final BlockPos.MutableBlockPos occupancyProbePos = new BlockPos.MutableBlockPos();

    private static long key(BlockPos pos) {
        return pos.asLong();
    }

    private static BlockPos pos(long key) {
        return BlockPos.of(key);
    }

    private static long relative(long pos, Direction direction) {
        return BlockPos.asLong(
                BlockPos.getX(pos) + direction.getStepX(),
                BlockPos.getY(pos) + direction.getStepY(),
                BlockPos.getZ(pos) + direction.getStepZ()
        );
    }

    @Override
    public GasStack getGas(long pos) {
        GasCell cell = cells.get(pos);

        if (cell == null || cell.isEmpty()) {
            return GasStack.EMPTY;
        }

        return cell.getGas();
    }

    public GasCell getCell(BlockPos pos) {
        return cells.get(key(pos));
    }

    public boolean hasGas(BlockPos pos) {
        GasCell cell = cells.get(key(pos));
        return cell != null && !cell.isEmpty();
    }

    public void wakeGasAt(long pos) {
        GasCell cell = cells.get(pos);

        if (cell != null && !cell.isEmpty()) {
            cell.markActive();
            activeCells.add(pos);
        }
    }

    public void wakeGasNeighbors(long pos) {
        for (Direction direction : Direction.values()) {
            wakeGasAt(relative(pos, direction));
        }
    }

    public void wakeGasAtAndNeighbors(long pos) {
        wakeGasAt(pos);
        wakeGasNeighbors(pos);
    }

    public void wakeGasAround(BlockPos pos) {
        BlockPos.betweenClosed(
                pos.getX() - 1,
                pos.getY() - 1,
                pos.getZ() - 1,
                pos.getX() + 1,
                pos.getY() + 1,
                pos.getZ() + 1
        ).forEach(blockPos -> wakeGasAt(blockPos.asLong()));
    }

    @Override
    public boolean canGasOccupy(Level level, long longPos) {
        int y = BlockPos.getY(longPos);

        if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) {
            return false;
        }

        long sectionKey = SectionPos.asLong(
                BlockPos.getX(longPos) >> 4,
                y >> 4,
                BlockPos.getZ(longPos) >> 4
        );

        GasOccupancySection section = gasOccupancySections.computeIfAbsent(
                sectionKey,
                GasOccupancySection::new
        );

        return section.canGasOccupy(level, occupancyProbePos, longPos);
    }

    public void invalidateOccupancySection(BlockPos pos) {
        gasOccupancySections.remove(SectionPos.asLong(
                pos.getX() >> 4,
                pos.getY() >> 4,
                pos.getZ() >> 4
        ));
    }

    public void invalidateOccupancySectionsAround(BlockPos pos) {
        int sectionX = pos.getX() >> 4;
        int sectionY = pos.getY() >> 4;
        int sectionZ = pos.getZ() >> 4;

        for (int x = sectionX - 1; x <= sectionX + 1; x++) {
            for (int y = sectionY - 1; y <= sectionY + 1; y++) {
                for (int z = sectionZ - 1; z <= sectionZ + 1; z++) {
                    gasOccupancySections.remove(SectionPos.asLong(x, y, z));
                }
            }
        }
    }

    public void clearOccupancyCache() {
        gasOccupancySections.clear();
    }

    @Override
    public void addGas(long pos, GasStack gas) {
        if (gas.isEmpty()) {
            return;
        }

        GasCell cell = cells.computeIfAbsent(pos, p -> new GasCell(GasStack.EMPTY));
        boolean wasEmpty = cell.isEmpty();

        double added = cell.addGas(gas);

        if (added >= MIN_GAS_AMOUNT) {
            wakeGasAt(pos);

            if (wasEmpty) {
                wakeGasAtAndNeighbors(pos);
            }
        }

    }

    @Override
    public GasStack removeGas(long pos, double amount) {
        GasCell cell = cells.get(pos);

        if (cell == null || cell.isEmpty()) {
            return GasStack.EMPTY;
        }

        GasStack removed = cell.removeGas(amount);

        if (!removed.isEmpty()) {
            wakeGasNeighbors(pos);
        }

        if (cell.isEmpty() || cell.getGas().amount() <= MIN_GAS_AMOUNT) {
            cells.remove(pos);
            activeCells.remove(pos);
        } else {
            wakeGasAt(pos);
        }

        return removed;
    }

    @Override
    public void moveGas(long from, long to, double amount) {
        if (amount <= 0.0D) {
            return;
        }

        GasStack moved = removeGas(from, amount);

        if (moved.isEmpty()) {
            return;
        }

        GasCell target = cells.computeIfAbsent(to, p -> new GasCell(GasStack.EMPTY));

        double accepted = target.addGas(moved);

        if (accepted > MIN_GAS_AMOUNT) {

            wakeGasAtAndNeighbors(from);
            wakeGasAtAndNeighbors(to);
        }

        if (accepted < moved.amount()) {
            GasStack remainder = moved.copyWithAmount(moved.amount() - accepted);
            addGas(from, remainder);
        }
    }

    @Override
    public Vec3 getVelocity(long pos) {
        GasCell cell = cells.get(pos);

        if (cell == null || cell.isEmpty()) {
            return Vec3.ZERO;
        }

        return cell.getVelocity();
    }

    @Override
    public void setVelocity(long pos, Vec3 velocity) {
        setVelocity(pos, velocity, false);
    }

    @Override
    public void setVelocity(long pos, Vec3 velocity, boolean wake) {
        GasCell cell = cells.get(pos);

        if (cell == null || cell.isEmpty()) {
            return;
        }

        cell.setVelocity(velocity);

        if (wake) {
            wakeGasAt(pos);
        }
    }

    @Override
    public void addVelocity(long pos, Vec3 velocity) {
        addVelocity(pos, velocity, false);
    }

    @Override
    public void addVelocity(long pos, Vec3 velocity, boolean wake) {
        GasCell cell = cells.get(pos);

        if (cell == null || cell.isEmpty()) {
            return;
        }

        cell.addVelocity(velocity);

        if (wake) {
            wakeGasAt(pos);
        }
    }

    @Override
    public void dampVelocity(long pos) {
        dampVelocity(pos, false);
    }

    @Override
    public void dampVelocity(long pos, boolean wake) {
        GasCell cell = cells.get(pos);

        if (cell == null || cell.isEmpty()) {
            return;
        }

        cell.dampVelocity();

        if (wake) {
            wakeGasAt(pos);
        }
    }

    public void tick(Level level) {
        if (level.isClientSide()) {
            return;
        }

        if (activeCells.isEmpty()) {
            return;
        }

        LongArrayList activePositions = new LongArrayList(activeCells);

        activeCells.clear();

        for (long posKey : activePositions) {
            GasCell cell = cells.get(posKey);

            if (cell != null && !cell.isEmpty()) {
                cell.markInactive();
            }
        }

        long start = System.currentTimeMillis();

        GasFlowSimulator.simulate(level, this, activePositions);

        long elapsed = System.currentTimeMillis() - start;
        if (!activePositions.isEmpty()) {
            LOGGER.debug("GasFlowSimulator took {} ms to simulate {} active positions, total positions {}", elapsed, activePositions.size(), cells.size());
        }

        applyGasEffects(level);

        cells.long2ObjectEntrySet().removeIf(entry -> {
            boolean remove = entry.getValue().isEmpty()
                    || entry.getValue().getGas().amount() <= MIN_GAS_AMOUNT;

            if (remove) {
                activeCells.remove(entry.getLongKey());
            }
            return remove;
        });
    }

    private void applyGasEffects(Level level) {
        for (Map.Entry<Long, GasCell> entry : cells.long2ObjectEntrySet()) {
            long posKey = entry.getKey();
            GasCell cell = entry.getValue();

            if (cell == null || cell.isEmpty()) {
                continue;
            }

            GasStack gas = cell.getGas();

            if (gas.isEmpty() || gas.gas().effects().isEmpty()) {
                continue;
            }

            BlockPos pos = pos(posKey);
            BlockState state = level.getBlockState(pos);

            for (GasEffect effect : gas.gas().effects()) {
                effect.applyToBlock(level, pos, state, gas);
            }

            AABB bounds = new AABB(pos);
            List<Entity> entities = level.getEntities(null, bounds);

            for (Entity entity : entities) {
                for (GasEffect effect : gas.gas().effects()) {
                    effect.applyToEntity(level, entity, gas);
                }
            }
        }
    }

    public List<GasDebugCell> createDebugSnapshot(ServerPlayer player, int horizontalRange, int verticalRange, int maxCells) {
        BlockPos playerPos = player.blockPosition();
        List<GasDebugCell> snapshot = new ArrayList<>();

        for (Map.Entry<Long, GasCell> entry : cells.long2ObjectEntrySet()) {
            BlockPos pos = pos(entry.getKey());

            if (Math.abs(pos.getX() - playerPos.getX()) > horizontalRange) {
                continue;
            }

            if (Math.abs(pos.getZ() - playerPos.getZ()) > horizontalRange) {
                continue;
            }

            if (Math.abs(pos.getY() - playerPos.getY()) > verticalRange) {
                continue;
            }

            GasStack gas = entry.getValue().getGas();

            if (gas.isEmpty()) {
                continue;
            }

            snapshot.add(new GasDebugCell(
                    pos,
                    gas.gas().color(),
                    gas.amount(),
                    gas.temperatureC()
            ));
        }

        snapshot.sort(Comparator.comparingDouble(cell -> cell.pos().distSqr(playerPos)));

        if (snapshot.size() > maxCells) {
            return List.copyOf(snapshot.subList(0, maxCells));
        }

        return snapshot;
    }

    public int cellCount() {
        return cells.size();
    }

    private static final class GasOccupancySection {
        private final BitSet knownCells = new BitSet(SECTION_CELL_COUNT);
        private final BitSet occupiableCells = new BitSet(SECTION_CELL_COUNT);

        private GasOccupancySection(long sectionKey) {}

        private boolean canGasOccupy(Level level, BlockPos.MutableBlockPos probePos, long longPos) {
            int index = localIndex(longPos);

            if (knownCells.get(index)) {
                return occupiableCells.get(index);
            }

            probePos.set(
                    BlockPos.getX(longPos),
                    BlockPos.getY(longPos),
                    BlockPos.getZ(longPos)
            );

            boolean canOccupy = false;

            if (level.isInWorldBounds(probePos) && level.isLoaded(probePos)) {
                BlockState state = level.getBlockState(probePos);
                canOccupy = state.isAir() || state.getCollisionShape(level, probePos).isEmpty();
            }

            knownCells.set(index);

            if (canOccupy) {
                occupiableCells.set(index);
            }

            return canOccupy;
        }

        private static int localIndex(long longPos) {
            return localIndex(
                    BlockPos.getX(longPos) & 15,
                    BlockPos.getY(longPos) & 15,
                    BlockPos.getZ(longPos) & 15
            );
        }

        private static int localIndex(int localX, int localY, int localZ) {
            return localX | (localY << 4) | (localZ << 8);
        }
    }
}
