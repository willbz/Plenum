package com.willbz.plenum.client.debug;

import net.minecraft.core.BlockPos;

public record GasDebugCell(
        BlockPos pos,
        int color,
        double amount,
        double temperatureC
) {}