package com.willbz.plenum.client.debug;

import com.willbz.plenum.Plenum;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public record ClientboundGasDebugPacket(List<GasDebugCell> cells) implements CustomPacketPayload {
    public static final Type<ClientboundGasDebugPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Plenum.MODID, "gas_debug")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundGasDebugPacket> STREAM_CODEC =
            StreamCodec.of(
                    ClientboundGasDebugPacket::write,
                    ClientboundGasDebugPacket::read
            );

    private static void write(RegistryFriendlyByteBuf buf, ClientboundGasDebugPacket packet) {
        buf.writeVarInt(packet.cells().size());

        for (GasDebugCell cell : packet.cells) {
            buf.writeBlockPos(cell.pos());
            buf.writeInt(cell.color());
            buf.writeDouble(cell.amount());
            buf.writeDouble(cell.temperatureC());
        }
    }

    private static ClientboundGasDebugPacket read(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<GasDebugCell> cells = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            BlockPos pos = buf.readBlockPos();
            int color = buf.readInt();
            double amount = buf.readDouble();
            double temperatureC = buf.readDouble();

            cells.add(new GasDebugCell(pos, color, amount, temperatureC));
        }

        return new ClientboundGasDebugPacket(cells);
    }

    public static void handle(ClientboundGasDebugPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientGasDebugData.setCells(packet.cells()));
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
