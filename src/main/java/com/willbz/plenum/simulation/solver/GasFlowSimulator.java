package com.willbz.plenum.simulation.solver;

import com.willbz.plenum.api.gas.GasStack;
import com.willbz.plenum.api.simulation.GasCellAccess;
import com.willbz.plenum.math.GasMath;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.willbz.plenum.simulation.constants.GasSimulationConstants.*;

public class GasFlowSimulator {
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final Vec3[] DIRECTION_NORMALS = createDirectionNormals();

    private GasFlowSimulator() {}

    public static void simulate(Level level, GasCellAccess cells, @NotNull LongList positions) {
        GasTransferBatch transfers = new GasTransferBatch(DIRECTION_NORMALS);
        ObjectOpenHashSet<GasEdge> processedEdges = new ObjectOpenHashSet<>();
        Long2ObjectOpenHashMap<List<GasTransfer>> outgoingBySource = new Long2ObjectOpenHashMap<>();

        for (long pos : positions) {
            GasStack source = cells.getGas(pos);

            if (source.isEmpty()) {
                continue;
            }

            Vec3 currentVelocity = cells.getVelocity(pos);

            if (source.amount() <= MIN_GAS_AMOUNT && currentVelocity.lengthSqr() <= GAS_MIN_VELOCITY_SQR) {
                continue;
            }

            applyForces(cells, pos, source);

            calculateOutgoingTransfers(level, cells, pos, source, processedEdges, outgoingBySource);

            cells.dampVelocity(pos, false);
        }

        for (Long2ObjectOpenHashMap.Entry<List<GasTransfer>> entry : outgoingBySource.long2ObjectEntrySet()) {
            long entrySourcePos = entry.getLongKey();
            GasStack entrySource = cells.getGas(entrySourcePos);

            if (entrySource.isEmpty()) {
                continue;
            }

            transfers.addScaled(entry.getValue(), entrySource.amount());
        }

        transfers.apply(cells);
    }

    private static void calculateOutgoingTransfers(
            Level level,
            @NotNull GasCellAccess cells,
            long pos,
            GasStack source,
            ObjectOpenHashSet<GasEdge> processedEdges,
            Long2ObjectOpenHashMap<List<GasTransfer>> outgoingBySource
    ) {
        Vec3 velocity = cells.getVelocity(pos);

        for (Direction direction : DIRECTIONS) {
            long neighborPos = relative(pos, direction);
            int neighborY = BlockPos.getY(neighborPos);

            if (direction == Direction.UP && neighborY >= level.getMaxBuildHeight()) {
                double transfer = calculateAdvectionTransfer(source, velocity, direction);

                if (transfer > MIN_GAS_TRANSFER_AMOUNT) {
                    addOutgoing(outgoingBySource, GasTransfer.vent(pos, transfer, direction));
                }

                continue;
            }

            if (!cells.canGasOccupy(level, neighborPos)) {
                continue;
            }

            GasEdge edge = GasEdge.of(pos, neighborPos);

            if (!processedEdges.add(edge)) {
                continue;
            }

            calculatePairTransfer(
                    cells,
                    outgoingBySource,
                    pos,
                    neighborPos,
                    direction,
                    source
            );
        }
    }

    private static void calculatePairTransfer(
            @NotNull GasCellAccess cells,
            Long2ObjectOpenHashMap<List<GasTransfer>> outgoingBySource,
            long firstPos,
            long secondPos,
            Direction firstToSecond,
            @NotNull GasStack first
    ) {
        GasStack second = cells.getGas(secondPos);

        // TODO: Remove limitation for 1 kind of gas per cell
        boolean canFirstMoveToSecond = second.isEmpty() || second.is(first.gas());
        boolean canSecondMoveToFirst = first.isEmpty() || first.is(second.gas());

        if (!canFirstMoveToSecond && !canSecondMoveToFirst) {
            return;
        }

        double firstToSecondTransfer = 0.0D;

        if (canFirstMoveToSecond) {
            Vec3 firstVelocity = cells.getVelocity(firstPos);
            firstToSecondTransfer = calculateTransfer(first, second, firstVelocity, firstToSecond);
        }

        double secondToFirstTransfer = 0.0D;

        if (canSecondMoveToFirst) {
            Vec3 secondVelocity = cells.getVelocity(secondPos);
            secondToFirstTransfer = calculateTransfer(second, first, secondVelocity, firstToSecond.getOpposite());
        }

        double netTransfer = firstToSecondTransfer - secondToFirstTransfer;

        if (netTransfer > MIN_GAS_TRANSFER_AMOUNT) {
            addOutgoing(outgoingBySource, GasTransfer.move(firstPos, secondPos, netTransfer, firstToSecond));
            return;
        }

        if (netTransfer < -MIN_GAS_TRANSFER_AMOUNT) {
            addOutgoing(outgoingBySource, GasTransfer.move(secondPos, firstPos, -netTransfer, firstToSecond.getOpposite()));
        }
    }

    private static void addOutgoing(
            Long2ObjectOpenHashMap<List<GasTransfer>> outgoingBySource,
            @NotNull GasTransfer transfer
    ) {
        if (transfer.amount() <= MIN_GAS_TRANSFER_AMOUNT) {
            return;
        }

        outgoingBySource.computeIfAbsent(transfer.from(), ignored -> new ArrayList<>())
                .add(transfer);
    }

    private static double calculateTransfer(
            GasStack source,
            GasStack target,
            Vec3 velocity,
            Direction direction
    ) {
        double advectionTransfer = calculateAdvectionTransfer(source, velocity, direction);
        double diffusionTransfer = calculateDiffusionTransfer(source, target);

        return advectionTransfer + diffusionTransfer;
    }

    private static double calculateAdvectionTransfer(
            GasStack source,
            @NotNull Vec3 velocity,
            @NotNull Direction direction
    ) {
        Vec3 normal = DIRECTION_NORMALS[direction.ordinal()];
        double directionalVelocity = velocity.dot(normal);

        if (directionalVelocity <= 0.0D) {
            return 0.0D;
        }

        double rawTransfer = source.amount()
                * directionalVelocity
                * GAS_ADVECTION_RATE
                * Math.max(0.0D, source.gas().flowScale());

        double maxTransfer = source.amount() * MAX_GAS_ADVECTION_FRACTION;

        return Math.min(rawTransfer, maxTransfer);
    }

    private static double calculateDiffusionTransfer(
            @NotNull GasStack source,
            GasStack target
    ) {
        double targetAmount = target == null || target.isEmpty() ? 0.0D : target.amount();
        double amountDifference = source.amount() - targetAmount;
        double effectiveEpsilon = Math.min(GAS_DIFFUSION_EPSILON, source.amount() * 0.01D);

        if (amountDifference <= effectiveEpsilon) {
            return 0.0D;
        }

        double rawTransfer = amountDifference
                * GAS_DIFFUSION_RATE
                * Math.max(0.0D, source.gas().flowScale());

        double maxTransfer = source.amount() * MAX_GAS_DIFFUSION_FRACTION_PER_EDGE;

        return Math.min(rawTransfer, maxTransfer);
    }

    private static void applyForces(GasCellAccess cells, long pos, @NotNull GasStack stack) {
        double buoyancyAccel = GasMath.buoyancyAcceleration(
                stack.gas(),
                stack.temperatureC(),
                AMBIENT_TEMPERATURE_C
        );

        if (buoyancyAccel != 0.0D) {
            cells.addVelocity(pos, new Vec3(
                    0.0D,
                    buoyancyAccel,
                    0.0D
            ), false);
        }
    }

    private static long relative(long pos, Direction direction) {
        return BlockPos.asLong(
                BlockPos.getX(pos) + direction.getStepX(),
                BlockPos.getY(pos) + direction.getStepY(),
                BlockPos.getZ(pos) + direction.getStepZ()
        );
    }

    private static Vec3[] createDirectionNormals() {
        Direction[] directions = DIRECTIONS;
        Vec3[] normals = new Vec3[directions.length];

        for (Direction direction : directions) {
            normals[direction.ordinal()] = Vec3.atLowerCornerOf(direction.getNormal());
        }

        return normals;
    }
}
