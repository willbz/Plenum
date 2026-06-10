package com.willbz.plenum.math;

import com.willbz.plenum.api.gas.GasStack;
import com.willbz.plenum.api.gas.GasType;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import static com.willbz.plenum.simulation.constants.GasSimulationConstants.GAS_UNITS_PER_BLOCK;

public class GasMath {
    public static final double GRAVITY_BLOCKS_PER_TICK_SQUARED = 0.08D;
    public static final double MIN_TEMPERATURE_K = 1.0D;

    private GasMath() {}

    public static double celsiusToKelvin(double temperatureC) {
        return Math.max(MIN_TEMPERATURE_K, temperatureC + 273.15D);
    }

    public static double kelvinToCelsius(double temperatureK) {
        return temperatureK - 273.15D;
    }

    public static double effectiveDensity(@NotNull GasType gas, double gasTemperatureC, double airTemperatureC) {
        double gasTemperatureK = celsiusToKelvin(gasTemperatureC);
        double airTemperatureK = celsiusToKelvin(airTemperatureC);

        return gas.relativeDensityToAir() * (airTemperatureK / gasTemperatureK);
    }

    public static double buoyancyAcceleration(GasType gas, double gasTemperatureC, double airTemperatureC) {
        double relativeDensity = effectiveDensity(gas, gasTemperatureC, airTemperatureC);
        double lift = 1.0D - relativeDensity;

        return lift * GRAVITY_BLOCKS_PER_TICK_SQUARED * gas.buoyancyScale();
    }

    /**
     * Simple concentration calculation.<br>
     * 0.0 = none, 1.0 = full block at normal capacity.<br>
     * >1.0 = compressed gas.
     * @param amount amount of gas in the block
     * @return The concentration
     */
    public static double concentration(double amount) {
        return Math.max(0.0D, amount / GAS_UNITS_PER_BLOCK);
    }

    public static double clampedConcentration(double amount) {
        return Mth.clamp(concentration(amount), 0.0D, 1.0D);
    }

    /**
     * The amount of pressure in PU given an amount and temperature.<br>
     * 1.0 PU = standard Minecraft atmosphere
     * @param amount The amount of gas
     * @param temperatureC The temperature of the gas in Celsius
     * @return The pressure in PU (Pressure Units)
     */
    public static double pressure(double amount, double temperatureC) {
        double concentration = concentration(amount);
        double temperatureK = celsiusToKelvin(temperatureC);
        double standardTemperatureK = celsiusToKelvin(GasType.STANDARD_TEMPERATURE_C);

        return concentration * (temperatureK / standardTemperatureK);
    }

    public static double pressure(@NotNull GasStack stack) {
        return pressure(stack.amount(), stack.temperatureC());
    }

    public static boolean isExplosive(@NotNull GasStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        return stack.gas().isExplosiveConcentration(concentration(stack.amount()));
    }
}
