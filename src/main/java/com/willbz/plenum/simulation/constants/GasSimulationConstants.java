package com.willbz.plenum.simulation.constants;

public final class GasSimulationConstants {
    public static final int GAS_UNITS_PER_BLOCK = 1000;

    public static final double ATMOSPHERIC_PRESSURE = 1.0D;
    public static final double AMBIENT_TEMPERATURE_C = 20.0D;

    /**
     * Minimum amount worth storing in a gas cell.
     * Cell at or below this amount are removed from the simulation.
     */
    public static final double MIN_GAS_AMOUNT = 0.01D;

    public static final double GAS_PRESSURE_FLOW_RATE = 0.65D;
    public static final double GAS_MAX_PRESSURE_EQUALIZATION_FRACTION = 0.92D;
    public static final double GAS_MIN_DIFFUSION_FLOW = 0.3D;

    public static final double GAS_BUOYANCY_DRIFT_RATE = 10.45D;
    public static final double GAS_VELOCITY_DRIFT_RATE = 0.25D;

    public static final double GAS_MIN_VELOCITY_SQR = 0.0001D;
    public static final double GAS_MAX_VELOCITY = 1.0D;
    public static final double GAS_VELOCITY_DAMPING = 0.78D;
    public static final double GAS_TRANSFER_VELOCITY_IMPULSE = 0.06D;

    public static final double GAS_MAX_PRESSURE_TRANSFER_FRACTION = 0.35D;
    public static final double GAS_MAX_DRIFT_TRANSFER_FRACTION = 0.25D;

    public static final int SIMULATION_INTERVAL_TICKS = 2;

    // TEMP DEBUG
    public static boolean DEBUG_SYNC_GAS_CELLS = true;
    public static final int DEBUG_SYNC_INTERVAL_TICKS = 2;
    public static final int DEBUG_SYNC_HORIZONTAL_RANGE = 48;
    public static final int DEBUG_SYNC_VERTICAL_RANGE = 32;
    public static final int DEBUG_SYNC_MAX_CELLS_PER_PLAYER = 32768;
}
