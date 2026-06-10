package com.willbz.plenum.simulation.solver;

import org.jetbrains.annotations.NotNull;

// TODO: Make this a packed primitive key in the future
public record GasEdge(long first, long second) {
    static @NotNull GasEdge of(long a, long b) {
        return a <= b ? new GasEdge(a, b) : new GasEdge(b, a);
    }
}

