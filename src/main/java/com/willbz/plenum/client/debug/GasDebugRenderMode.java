package com.willbz.plenum.client.debug;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public enum GasDebugRenderMode {
    GAS_COLOR,
    RED,
    CONCENTRATION,
    AMOUNT,
    TEMPERATURE;

    public static @NotNull GasDebugRenderMode byName(@NotNull String name) {
        String normalized = name.toUpperCase(Locale.ROOT);

        for (GasDebugRenderMode mode : values()) {
            if (mode.name().equals(normalized)) {
                return mode;
            }
        }

        throw new IllegalArgumentException("Unknown gas debug render mode: " + name);
    }

    public @NotNull String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
