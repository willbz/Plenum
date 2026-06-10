package com.willbz.plenum.api.gas;

import com.willbz.plenum.api.registry.PlenumRegistries;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public class GasStack {
    public static final GasStack EMPTY = new GasStack(null, 0.0D, 20.0D);

    private final GasType gas;
    private double amount;
    private double temperatureC;

    public GasStack(GasType gas, double amount, double temperatureC) {
        this.gas = gas;
        this.amount = Math.max(0.0D, amount);
        this.temperatureC = temperatureC;
    }

    public boolean isEmpty() {
        return gas == null || amount <= 0.0D;
    }

    public GasType gas() {
        return gas;
    }

    public double amount() {
        return amount;
    }

    public double temperatureC() {
        return temperatureC;
    }

    public void setAmount(double amount) {
        this.amount = Math.max(0.0D, amount);
    }

    public void grow(double amount) {
        this.amount = Math.max(0.0D, this.amount + amount);
    }

    public void shrink(double amount) {
        this.amount = Math.max(0.0D, this.amount - Math.max(0.0D, amount));
    }

    public void setTemperatureC(double temperatureC) {
        this.temperatureC = temperatureC;
    }

    public boolean is(GasType gas) {
        return this.gas == gas;
    }

    public GasStack copy() {
        if (isEmpty()) {
            return EMPTY;
        }

        return new GasStack(gas, amount, temperatureC);
    }

    public GasStack copyWithAmount(double amount) {
        if (isEmpty() || amount <= 0.0D) {
            return EMPTY;
        }

        return new GasStack(gas, amount, temperatureC);
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();

        if (isEmpty()) {
            return tag;
        }

        Registry<GasType> gasRegistry = (Registry<GasType>) registries.lookupOrThrow(PlenumRegistries.GAS_TYPE);
        ResourceLocation gasId = gasRegistry.getKey(gas);

        if (gasId == null) {
            return tag;
        }

        tag.putString("Gas", gasId.toString());
        tag.putDouble("Amount", amount);
        tag.putDouble("TemperatureC", temperatureC);

        return tag;
    }

    public static GasStack load(HolderLookup.Provider registries, CompoundTag tag) {
        if (!tag.contains("Gas")) {
            return EMPTY;
        }

        Registry<GasType> gasRegistry = (Registry<GasType>) registries.lookupOrThrow(PlenumRegistries.GAS_TYPE);
        ResourceLocation gasId = ResourceLocation.parse(tag.getString("Gas"));
        GasType gas = gasRegistry.get(gasId);

        if (gas == null) {
            return EMPTY;
        }

        return new GasStack(
                gas,
                tag.getDouble("Amount"),
                tag.getDouble("TemperatureC")
        );
    }
}
