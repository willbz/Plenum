package com.willbz.plenum.api.registry;

import com.willbz.plenum.Plenum;
import com.willbz.plenum.api.gas.GasType;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public final class PlenumRegistries {
    public static final ResourceKey<Registry<GasType>> GAS_TYPE =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(Plenum.MODID, "gas_type"));

    private PlenumRegistries() {}
}
