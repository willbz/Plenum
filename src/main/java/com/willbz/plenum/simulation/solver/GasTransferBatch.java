package com.willbz.plenum.simulation.solver;

import com.willbz.plenum.api.gas.GasStack;
import com.willbz.plenum.api.simulation.GasCellAccess;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static com.willbz.plenum.simulation.constants.GasSimulationConstants.*;

public final class GasTransferBatch {
    private final Vec3[] directionNormals;
    public final List<GasTransfer> transfers = new ArrayList<>();

    public GasTransferBatch(Vec3[] directionNormals) {
        this.directionNormals = directionNormals;
    }

    public boolean isEmpty() {
        return transfers.isEmpty();
    }

    public void add(GasTransfer transfer) {
        if (transfer.amount() > MIN_GAS_TRANSFER_AMOUNT) {
            transfers.add(transfer);
        }
    }

    public void addScaled(List<GasTransfer> outgoing, double sourceAmount) {
        double requestedOutflow = 0.0D;

        for (GasTransfer transfer : outgoing) {
            requestedOutflow += transfer.amount();
        }

        if (requestedOutflow <= MIN_GAS_TRANSFER_AMOUNT) {
            return;
        }

        double scale = Math.min(1.0D, sourceAmount / requestedOutflow);

        for (GasTransfer transfer : outgoing) {
            double amount  = transfer.amount() * scale;

            add(transfer.withAmount(amount));
        }
    }

    public void apply(GasCellAccess cells) {
        Long2ObjectOpenHashMap<TransferSourceState> sources = new Long2ObjectOpenHashMap<>();

        for (GasTransfer transfer : transfers) {
            sources.computeIfAbsent(transfer.from(), pos -> new TransferSourceState())
                    .requestedOutflow += transfer.amount();
        }

        for (Long2ObjectOpenHashMap.Entry<TransferSourceState> entry : sources.long2ObjectEntrySet()) {
            long sourcePos = entry.getLongKey();
            TransferSourceState state = entry.getValue();

            if (state.requestedOutflow <= MIN_GAS_TRANSFER_AMOUNT) {
                continue;
            }

            state.velocity = cells.getVelocity(sourcePos);
            state.removedGas = cells.removeGas(sourcePos, state.requestedOutflow);
        }

        for (GasTransfer transfer : transfers) {
            if (transfer.vents()) {
                continue;
            }

            TransferSourceState state = sources.get(transfer.from());

            if (state == null || state.removedGas == null || state.removedGas.isEmpty()) {
                continue;
            }

            if (state.requestedOutflow <= MIN_GAS_TRANSFER_AMOUNT) {
                continue;
            }

            double deliveredAmount = state.removedGas.amount() * (transfer.amount() / state.requestedOutflow);

            if (deliveredAmount <= MIN_GAS_TRANSFER_AMOUNT) {
                continue;
            }

            Vec3 sourceVelocity = state.velocity == null ? Vec3.ZERO : state.velocity;

            cells.addGas(transfer.to(), state.removedGas.copyWithAmount(deliveredAmount));

            double movedFraction = deliveredAmount / Math.max(MIN_GAS_AMOUNT, state.removedGas.amount());
            Vec3 transferDirection = directionNormals[transfer.direction().ordinal()];
            Vec3 transferImpulse = transferDirection.scale(GAS_TRANSFER_VELOCITY_IMPULSE * movedFraction);

            cells.addVelocity(transfer.to(), sourceVelocity.add(transferImpulse), false);
            cells.addVelocity(transfer.from(), transferImpulse.reverse(), false);
        }
    }

    private static class TransferSourceState {
        private double requestedOutflow;
        private GasStack removedGas = GasStack.EMPTY;
        private Vec3 velocity = Vec3.ZERO;
    }
}