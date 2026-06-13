package com.willbz.plenum.simulation.constants;

public final class GasSimulationConstants {
    public static final int GAS_UNITS_PER_BLOCK = 1000;

    public static final double ATMOSPHERIC_PRESSURE = 1.0D;
    public static final double AMBIENT_TEMPERATURE_C = 20.0D;

    /**
     * Minimum amount worth storing in a gas cell.
     * Cells at or below this amount are removed from the simulation.
     */
    public static final double MIN_GAS_AMOUNT = 0.01D;

    /**
     * Minimum amount worth moving in a single transfer.
     * This value should always be lower than MIN_GAS_AMOUNT to prevent cells becoming frozen.
     */
    public static final double MIN_GAS_TRANSFER_AMOUNT = 0.0001D;

    /**
     * Differences below this value are ignored by local concentration diffusion.
     * This value should always be lower than MIN_GAS_AMOUNT to prevent cells becoming frozen.
     */
    public static final double GAS_DIFFUSION_EPSILON = 0.001D;
    public static final double GAS_DIFFUSION_RATE = 0.08D;

    public static final double GAS_ADVECTION_RATE = 0.28D;
    public static final double MAX_GAS_ADVECTION_FRACTION = 0.45D;
    public static final double MAX_GAS_DIFFUSION_FRACTION_PER_EDGE = 0.18D;

    public static final double GAS_MIN_VELOCITY_SQR = 0.0001D;
    public static final double GAS_MAX_VELOCITY = 1.0D;
    public static final double GAS_VELOCITY_DAMPING = 0.78D;
    public static final double GAS_TRANSFER_VELOCITY_IMPULSE = 0.06D;

    public static final int SIMULATION_INTERVAL_TICKS = 2;

    // TEMP DEBUG
    public static boolean DEBUG_SYNC_GAS_CELLS = true;
    public static final int DEBUG_SYNC_INTERVAL_TICKS = 2;
    public static final int DEBUG_SYNC_HORIZONTAL_RANGE = 48;
    public static final int DEBUG_SYNC_VERTICAL_RANGE = 32;
    public static final int DEBUG_SYNC_MAX_CELLS_PER_PLAYER = 32768;
}
