package com.willbz.plenum.simulation.solver;

import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;

public record GasTransfer(long from, long to, double amount, boolean vents, Direction direction) {
    static @NotNull GasTransfer move(long from, long to, double amount, Direction direction) {
        return new GasTransfer(from, to, amount, false, direction);
    }

    static @NotNull GasTransfer vent(long pos, double amount, Direction direction) {
        return new GasTransfer(pos, 0L, amount, true, direction);
    }

    @NotNull GasTransfer withAmount(double amount) {
        return new GasTransfer(from, to, amount, vents, direction);
    }
}
