package com.willbz.plenum.api.simulation;

import com.willbz.plenum.api.gas.GasStack;
import com.willbz.plenum.api.gas.GasType;
import net.minecraft.world.phys.Vec3;

import static com.willbz.plenum.simulation.constants.GasSimulationConstants.*;

public class GasCell {
    private GasStack gas;
    private boolean active = true;
    private Vec3 velocity = Vec3.ZERO;

    public GasCell(GasStack gas) {
        this.gas = gas.isEmpty() ? GasStack.EMPTY : gas.copy();
        this.active = !this.gas.isEmpty();
    }

    public GasStack getGas() {
        return gas;
    }

    public void setGas(GasStack gas) {
        this.gas = gas.isEmpty() ? GasStack.EMPTY : gas.copy();
        this.active = !this.gas.isEmpty();
    }

    public boolean isEmpty() {
        return gas.isEmpty();
    }

    public boolean isActive() {
        return active;
    }

    public boolean canAccept(GasStack incoming) {
        return incoming.isEmpty() || gas.isEmpty() || gas.is(incoming.gas());
    }

    public boolean contains(GasType gasType) {
        return !gas.isEmpty() && gas.is(gasType);
    }

    public double addGas(GasStack incoming) {
        if (incoming.isEmpty()) {
            return 0.0D;
        }

        if (!canAccept(incoming)) {
            return 0.0D;
        }

        double added = incoming.amount();

        if (gas.isEmpty()) {
            setGas(incoming);
            return added;
        }

        double oldAmount = gas.amount();
        double newAmount = oldAmount + added;

        if (newAmount <= 0.0D) {
            setGas(GasStack.EMPTY);
            return 0.0D;
        }

        double mixedTemperature = ((gas.temperatureC() * oldAmount) + (incoming.temperatureC() * added)) / newAmount;

        gas.setAmount(newAmount);
        gas.setTemperatureC(mixedTemperature);
        markActive();

        return added;
    }

    public GasStack removeGas(double amount) {
        if (gas.isEmpty() || amount <= 0.0D) {
            return GasStack.EMPTY;
        }

        double removed = Math.min(amount, gas.amount());
        GasStack removedStack = gas.copyWithAmount(removed);

        gas.shrink(removed);

        if (gas.isEmpty()) {
            setGas(GasStack.EMPTY);
        } else {
            markActive();
        }

        return removedStack;
    }

    public void markActive() {
        this.active = true;
    }

    public void markInactive() {
        this.active = false;
    }

    public Vec3 getVelocity() {
        return velocity;
    }

    public void setVelocity(Vec3 velocity) {
        if (velocity.lengthSqr() < GAS_MIN_VELOCITY_SQR) {
            this.velocity = Vec3.ZERO;
            return;
        }

        if (velocity.lengthSqr() > GAS_MAX_VELOCITY * GAS_MAX_VELOCITY) {
            this.velocity = velocity.normalize().scale(GAS_MAX_VELOCITY);
            return;
        }

        this.velocity = velocity;
    }

    public void addVelocity(Vec3 addedVelocity) {
        setVelocity(this.velocity.add(addedVelocity));
    }

    public void dampVelocity() {
        setVelocity(this.velocity.scale(GAS_VELOCITY_DAMPING));
    }
}

