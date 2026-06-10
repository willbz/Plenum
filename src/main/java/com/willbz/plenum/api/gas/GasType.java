package com.willbz.plenum.api.gas;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class GasType {
    public static final double STANDARD_TEMPERATURE_C = 20.0D;

    private final String descriptionId;
    private final int color;
    private final ResourceLocation texture;

    /**
     * 1.0 = behaves like air<br>
     * <1.0 = lighter than air and tends to rise<br>
     * >1.0 = heavier than air and tends to sink<br>
     */
    private final double relativeDensityToAir;

    /**
     * 1.0 = normal<br>
     * <1.0 = heats/cools quickly<br>
     * >1.0 = heats/cools slowly<br>
     */
    private final double thermalMass;

    /**
     * Temperature where this gas condenses as normal pressure.
     */
    private final double condensationTemperatureC;

    private final boolean flammable;
    private final double lowerExplosiveLimit;
    private final double upperExplosiveLimit;
    private final double ignitionTemperatureC;
    private final float explosionPower;

    /**
     * Tuning multipliers.
     * Keep near 1.0 for normal behavior.
     */
    private final double flowScale;
    private final double buoyancyScale;
    private final double visibilityScale;

    private final List<GasEffect> effects;

    private GasType(Builder builder) {
        this.descriptionId = builder.descriptionId;
        this.color = builder.color;
        this.texture = builder.texture;
        this.relativeDensityToAir = builder.relativeDensityToAir;
        this.thermalMass = builder.thermalMass;
        this.condensationTemperatureC = builder.condensationTemperatureC;
        this.flammable = builder.flammable;
        this.lowerExplosiveLimit = builder.lowerExplosiveLimit;
        this.upperExplosiveLimit = builder.upperExplosiveLimit;
        this.ignitionTemperatureC = builder.ignitionTemperatureC;
        this.explosionPower = builder.explosionPower;
        this.flowScale = builder.flowScale;
        this.buoyancyScale = builder.buoyancyScale;
        this.visibilityScale = builder.visibilityScale;
        this.effects = builder.effects;
    }

    public String descriptionId() {
        return descriptionId;
    }

    public int color() {
        return color;
    }

    public ResourceLocation texture() {
        return texture;
    }

    public double relativeDensityToAir() {
        return relativeDensityToAir;
    }

    public double thermalMass() {
        return thermalMass;
    }

    public double normalCondensationTemperatureC() {
        return condensationTemperatureC;
    }

    public boolean flammable() {
        return flammable;
    }

    public double lowerExplosiveLimit() {
        return lowerExplosiveLimit;
    }

    public double upperExplosiveLimit() {
        return upperExplosiveLimit;
    }

    public double ignitionTemperatureC() {
        return ignitionTemperatureC;
    }

    public float explosionPower() {
        return explosionPower;
    }

    public double flowScale() {
        return flowScale;
    }

    public double buoyancyScale() {
        return buoyancyScale;
    }

    public double visibilityScale() {
        return visibilityScale;
    }

    public List<GasEffect> effects() {
        return effects;
    }

    public boolean isLighterThanAir() {
        return relativeDensityToAir < 1.0D;
    }

    public boolean isHeavierThanAir() {
        return relativeDensityToAir > 1.0D;
    }

    public boolean canCondenseAt(double temperatureC) {
        return temperatureC <= condensationTemperatureC;
    }

    public boolean isExplosiveConcentration(double concentration) {
        return flammable
                && concentration >= lowerExplosiveLimit
                && concentration <= upperExplosiveLimit;
    }

    public boolean canIgniteAt(double temperatureC) {
        return flammable && temperatureC >= ignitionTemperatureC;
    }

    public static Builder builder(String descriptionId) {
        return new Builder(descriptionId);
    }

    public static class Builder {
        private final String descriptionId;

        private int color = 0x55FFFFFF;
        private ResourceLocation texture = ResourceLocation.withDefaultNamespace("textures/misc/unknown_pack.png");

        private double relativeDensityToAir = 1.0D;
        private double thermalMass = 1.0D;
        private double condensationTemperatureC = Double.NEGATIVE_INFINITY;

        private boolean flammable = false;
        private double lowerExplosiveLimit = 0.0D;
        private double upperExplosiveLimit = 0.0D;
        private double ignitionTemperatureC = Double.POSITIVE_INFINITY;
        private float explosionPower = 0.0F;

        private double flowScale = 1.0D;
        private double buoyancyScale = 1.0D;
        private double visibilityScale = 1.0D;

        private ArrayList<GasEffect> effects = new ArrayList<>();

        private Builder(String descriptionId) {
            this.descriptionId = descriptionId;
        }

        public Builder color(int color) {
            this.color = color;
            return this;
        }

        public Builder texture(ResourceLocation texture) {
            this.texture = texture;
            return this;
        }

        /**
         * 1.0 = air-like, <1.0 = rises, >1.0 = sinks.
         */
        public Builder relativeDensity(double relativeDensityToAir) {
            this.relativeDensityToAir = relativeDensityToAir;
            return this;
        }

        /**
         * 1.0 = normal, <1.0 = changes temperature faster, >1.0 = slower.
         */
        public Builder thermalMass(double thermalMass) {
            this.thermalMass = thermalMass;
            return this;
        }

        public Builder condensationTemperature(double condensationTemperatureC) {
            this.condensationTemperatureC = condensationTemperatureC;
            return this;
        }

        public Builder flammable(double lowerExplosiveLimit, double upperExplosiveLimit, double ignitionTemperatureC, float explosionPower) {
            this.flammable = true;
            this.lowerExplosiveLimit = lowerExplosiveLimit;
            this.upperExplosiveLimit = upperExplosiveLimit;
            this.ignitionTemperatureC = ignitionTemperatureC;
            this.explosionPower = explosionPower;
            return this;
        }

        public Builder flowScale(double flowScale) {
            this.flowScale = flowScale;
            return this;
        }

        public Builder buoyancyScale(double buoyancyScale) {
            this.buoyancyScale = buoyancyScale;
            return this;
        }

        public Builder visibilityScale(double visibilityScale) {
            this.visibilityScale = visibilityScale;
            return this;
        }

        public Builder addEffect(GasEffect effect) {
            this.effects.add(effect);
            return this;
        }

        public GasType build() {
            return new GasType(this);
        }
    }
}
