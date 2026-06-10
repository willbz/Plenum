package com.willbz.plenum.api.gas;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface GasEffect {
    default void applyToEntity(Level level, Entity entity, GasStack stack) {
    }

    default void applyToBlock(Level level, BlockPos pos, BlockState state, GasStack stack) {
    }
}

