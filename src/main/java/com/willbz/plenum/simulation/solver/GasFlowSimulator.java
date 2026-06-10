package com.willbz.plenum.simulation.solver;

import com.willbz.plenum.api.gas.GasStack;
import com.willbz.plenum.api.simulation.GasCellAccess;
import com.willbz.plenum.math.GasMath;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static com.willbz.plenum.simulation.constants.GasSimulationConstants.*;

public class GasFlowSimulator {
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final Vec3[] DIRECTION_NORMALS = createDirectionNormals();

    private GasFlowSimulator() {}

    public static void simulate(Level level, GasCellAccess cells, @NotNull LongList positions) {
        GasTransferBatch transfers = new GasTransferBatch(DIRECTION_NORMALS);

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

            List<GasTransfer> outgoing = calculateOutgoingTransfers(level, cells, pos, source);

            if (outgoing.isEmpty()) {
                cells.dampVelocity(pos, false);
                continue;
            }

            transfers.addScaled(outgoing, source.amount());
            cells.dampVelocity(pos, false);
        }

        transfers.apply(cells);
    }

    private static @NotNull List<GasTransfer> calculateOutgoingTransfers(
            Level level,
            @NotNull GasCellAccess cells,
            long pos,
            GasStack source
    ) {
        List<GasTransfer> outgoing = new ArrayList<>();
        Vec3 velocity = cells.getVelocity(pos);

        for (Direction direction : DIRECTIONS) {
            long neighborPos = relative(pos, direction);
            int neighborY = BlockPos.getY(neighborPos);

            if (direction == Direction.UP && neighborY >= level.getMaxBuildHeight()) {
                double transfer = calculatePressureTransfer(source, null, velocity, direction);

                if (transfer > MIN_GAS_AMOUNT) {
                    outgoing.add(GasTransfer.vent(pos, transfer, direction));
                }

                continue;
            }

            if (!cells.canGasOccupy(level, neighborPos)) {
                continue;
            }

            GasStack target = cells.getGas(neighborPos);

            if (!target.isEmpty() && !target.is(source.gas())) {
                continue;
            }

            double transfer = calculatePressureTransfer(source, target, velocity, direction);

            if (transfer > 0.0D) {
                outgoing.add(GasTransfer.move(pos, neighborPos, transfer, direction));
            }
        }

        return outgoing;
    }

    private static double calculatePressureTransfer(GasStack source, @Nullable GasStack target, Vec3 velocity, Direction direction) {
        double sourcePressure = directionalPressure(source, direction);
        double targetPressure = 0.0D;

        if (target != null && !target.isEmpty()) {
            targetPressure = directionalPressure(target, direction.getOpposite());
        }

        double pressureGradient = sourcePressure - targetPressure;
        double pressureFlow = pressureGradient <= 0.0D
                ? 0.0D
                : pressureGradient
                * GAS_UNITS_PER_BLOCK
                * GAS_PRESSURE_FLOW_RATE
                * Math.max(0.0D, source.gas().flowScale());

        double velocityFlow = calculateVelocityFlow(source, velocity, direction);

        return Math.max(0.0D, pressureFlow + velocityFlow);
    }

    private static double calculateVelocityFlow(GasStack source, @NotNull Vec3 velocity, @NotNull Direction direction) {
        Vec3 normal = Vec3.atLowerCornerOf(direction.getNormal());
        double directionalVelocity = velocity.dot(normal);

        if (directionalVelocity <= 0.0D) {
            return 0.0D;
        }

        return source.amount()
                * directionalVelocity
                * Math.max(0.0D, source.gas().flowScale());
    }

    private static double directionalPressure(@NotNull GasStack stack, @NotNull Direction direction) {
        double pressure = GasMath.pressure(stack.amount(), stack.temperatureC());

        if (direction.getAxis() != Direction.Axis.Y) {
            return pressure;
        }

        double concentration = GasMath.concentration(stack.amount());

        if (concentration <= 0.0D) {
            return pressure;
        }

        double effectiveDensity = GasMath.effectiveDensity(
                stack.gas(),
                stack.temperatureC(),
                AMBIENT_TEMPERATURE_C
        );

        double lift = 1.0D - effectiveDensity;
        double buoyancyPressure = lift
                * stack.gas().buoyancyScale()
                * concentration;

        if (direction == Direction.UP) {
            return pressure + buoyancyPressure;
        }

        return pressure - buoyancyPressure;
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
