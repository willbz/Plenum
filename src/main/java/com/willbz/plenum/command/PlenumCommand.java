package com.willbz.plenum.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.willbz.plenum.api.gas.GasStack;
import com.willbz.plenum.api.gas.GasType;
import com.willbz.plenum.api.registry.PlenumRegistries;
import com.willbz.plenum.api.simulation.GasCell;
import com.willbz.plenum.api.simulation.GasCellWorld;
import com.willbz.plenum.client.debug.GasDebugRenderMode;
import com.willbz.plenum.client.debug.GasDebugRenderer;
import com.willbz.plenum.command.arguments.GasArgument;
import com.willbz.plenum.math.GasMath;
import com.willbz.plenum.simulation.constants.GasSimulationConstants;
import com.willbz.plenum.simulation.manager.GasSimulationManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public class PlenumCommand {
    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
        final LiteralArgumentBuilder<CommandSourceStack> gasBuilder = Commands.literal("gas")
                .requires(commandSourceStack -> commandSourceStack.hasPermission(2));

        gasBuilder
                .then(Commands.literal("info")
                    .executes(ctx -> gasInfo(ctx, BlockPos.containing(ctx.getSource().getPosition())))
                    .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(ctx -> gasInfo(ctx, BlockPosArgument.getBlockPos(ctx, "pos")))
                    )
                )

                .then(Commands.literal("add")
                    .then(Commands.argument("gas", GasArgument.gas())
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0D))
                            .executes(ctx -> gasAdd(
                                    ctx,
                                    GasArgument.getGas(ctx, "gas"),
                                    DoubleArgumentType.getDouble(ctx, "amount"),
                                    20.0D,
                                    BlockPos.containing(ctx.getSource().getPosition())
                            ))
                            .then(Commands.argument("temperature", DoubleArgumentType.doubleArg())
                                    .executes(ctx -> gasAdd(
                                            ctx,
                                            GasArgument.getGas(ctx, "gas"),
                                            DoubleArgumentType.getDouble(ctx, "amount"),
                                            DoubleArgumentType.getDouble(ctx, "temperature"),
                                            BlockPos.containing(ctx.getSource().getPosition())
                                    ))
                                    .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                            .executes(ctx -> gasAdd(
                                                    ctx,
                                                    GasArgument.getGas(ctx, "gas"),
                                                    DoubleArgumentType.getDouble(ctx, "amount"),
                                                    DoubleArgumentType.getDouble(ctx, "temperature"),
                                                    BlockPosArgument.getLoadedBlockPos(ctx, "pos")
                                            ))
                                    )
                            )
                        )
                    )
                )

                .then(Commands.literal("debug")
                        .then(Commands.literal("mode")
                            .executes(PlenumCommand::showDebugMode)
                            .then(Commands.argument("mode", StringArgumentType.string())
                                .suggests((ctx, builder) -> {
                                    for (GasDebugRenderMode mode : GasDebugRenderMode.values()) {
                                        builder.suggest(mode.name());
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> setDebugMode(
                                        ctx,
                                        StringArgumentType.getString(ctx, "mode")
                                ))
                            )
                        )
                        .then(Commands.literal("toggle")
                                .executes(PlenumCommand::toggleDebugMode)
                        )
                );

        dispatcher.register(gasBuilder);
    }

    private static int gasInfo(final CommandContext<CommandSourceStack> ctx, final BlockPos pos) {
        ServerLevel level = ctx.getSource().getLevel();
        GasStack gas = GasSimulationManager.get(level).getGas(pos);
        GasCell cell = GasSimulationManager.get(level).getCell(pos);

        if (gas.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.translatable("commands.plenum.gas.info.empty", pos.getX(), pos.getY(), pos.getZ()), false);
            return 1;
        }

        ResourceLocation gasId = level.registryAccess()
                .registryOrThrow(PlenumRegistries.GAS_TYPE)
                .getKey(gas.gas());

        ctx.getSource().sendSuccess(() -> Component.translatable("commands.plenum.gas.info.position", pos.getX(), pos.getY(), pos.getZ(), String.valueOf(gasId)),false);
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.plenum.gas.info.amount", gas.amount()), false);
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.plenum.gas.info.temperature", gas.temperatureC()), false);
        ctx.getSource().sendSuccess(() -> {
            double pressure = GasMath.pressure(gas);
            return Component.translatable("commands.plenum.gas.info.pressure", pressure);
        }, false);
        ctx.getSource().sendSuccess(() -> {
            Vec3 cellVel = cell.getVelocity();
            return Component.translatable("commands.plenum.gas.info.velocity", cellVel.x, cellVel.y, cellVel.z);
        }, false);

        ctx.getSource().sendSuccess(() -> Component.translatable("commands.plenum.gas.info.active", String.valueOf(GasSimulationManager.get(level).isCellActive(pos))), false);

        return 1;
    }

    private static int gasAdd(
            final CommandContext<CommandSourceStack> ctx,
            final GasType gas,
            final double amount,
            final double temperature,
            final BlockPos pos
    ) {
        ServerLevel level = ctx.getSource().getLevel();

        GasCellWorld gasWorld = GasSimulationManager.get(level);

        if (!gasWorld.canGasOccupy(level, pos)) {
            ctx.getSource().sendFailure(Component.translatable("commands.plenum.gas.error.cannot_occupy"));
            return 0;
        }

        gasWorld.addGas(pos, new GasStack(gas, amount, temperature));

        ResourceLocation gasId = level.registryAccess()
                .registryOrThrow(PlenumRegistries.GAS_TYPE)
                .getKey(gas);

        ctx.getSource().sendSuccess(() -> Component.translatable("commands.plenum.gas.add.success", String.valueOf(gasId), amount, temperature), false);

        return 1;
    }

    private static int showDebugMode(final CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() ->
                Component.translatable("commands.plenum.debug_mode.show", GasDebugRenderer.mode.serializedName()),
                false
        );

        return 1;
    }

    private static int setDebugMode(final CommandContext<CommandSourceStack> ctx, String modeStr) {
        try {
            GasDebugRenderer.mode = GasDebugRenderMode.valueOf(modeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            ctx.getSource().sendFailure(Component.translatable("commands.plenum.debug_mode.error.invalid_mode", modeStr));
            return 0;
        }

        ctx.getSource().sendSuccess(() ->
                Component.translatable("commands.plenum.debug_mode.set.success", modeStr),
                false
        );

        return 1;
    }

    private static int toggleDebugMode(final CommandContext<CommandSourceStack> ctx) {
        GasSimulationConstants.DEBUG_SYNC_GAS_CELLS = !GasSimulationConstants.DEBUG_SYNC_GAS_CELLS;
        ctx.getSource().sendSuccess(() ->
                Component.translatable("commands.plenum.debug_mode.set.success", Boolean.toString(GasSimulationConstants.DEBUG_SYNC_GAS_CELLS)),
                false
        );

        return 1;
    }
}
