package com.willbz.plenum.client.debug;

import java.util.List;

public class ClientGasDebugData {
    private static List<GasDebugCell> cells = List.of();

    private ClientGasDebugData() {}

    public static void setCells(List<GasDebugCell> newCells) {
        cells = List.copyOf(newCells);
    }

    public static List<GasDebugCell> cells() {
        return cells;
    }

    public static void clear() {
        cells = List.of();
    }
}

