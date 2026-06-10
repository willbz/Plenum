package com.willbz.plenum.api.simulation;

import com.willbz.plenum.api.gas.GasStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public interface GasCellAccess {
    private static long key(@NotNull BlockPos pos) {
        return pos.asLong();
    }

    default void beginSimulationTick(Level level) {}

    default void endSimulationTick(Level level) {}

    GasStack getGas(long pos);

    default GasStack getGas(BlockPos pos) {
        return getGas(key(pos));
    }

    boolean canGasOccupy(Level level, long pos);

    default boolean canGasOccupy(Level level, BlockPos pos) {
        return canGasOccupy(level, key(pos));
    }

    void addGas(long pos, GasStack gas);

    default void addGas(BlockPos pos, GasStack gas) {
        addGas(key(pos), gas);
    }

    GasStack removeGas(long pos, double amount);

    default GasStack removeGas(BlockPos pos, double amount) {
        return removeGas(key(pos), amount);
    }

    void moveGas(long from, long to, double amount);

    default void moveGas(BlockPos from, BlockPos to, double amount) {
        moveGas(key(from), key(to), amount);
    }

    default void ventGas(long pos, double amount) {
        removeGas(pos, amount);
    }

    default void ventGas(BlockPos pos, double amount) {
        ventGas(key(pos), amount);
    }

    Vec3 getVelocity(long pos);

    default Vec3 getVelocity(BlockPos pos) {
        return getVelocity(key(pos));
    }

    void setVelocity(long pos, Vec3 velocity);

    default void setVelocity(long pos, Vec3 velocity, boolean wake) {
        setVelocity(pos, velocity);
    }

    default void setVelocity(BlockPos pos, Vec3 velocity) {
        setVelocity(key(pos), velocity);
    }

    void addVelocity(long pos, Vec3 velocity);

    default void addVelocity(long pos, Vec3 velocity, boolean wake) {
        addVelocity(pos, velocity);
    }

    default void addVelocity(BlockPos pos, Vec3 velocity) {
        addVelocity(key(pos), velocity);
    }

    void dampVelocity(long pos);

    default void dampVelocity(long pos, boolean wake) {
        dampVelocity(pos);
    }

    default void dampVelocity(BlockPos pos) {
        dampVelocity(key(pos));
    }
}
