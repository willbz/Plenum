package com.willbz.plenum.api.registry;

import com.willbz.plenum.Plenum;
import com.willbz.plenum.api.gas.GasType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class DevGases {
    public static final DeferredRegister<GasType> GAS_TYPE =
            DeferredRegister.create(PlenumRegistries.GAS_TYPE, Plenum.MODID);

    public static final DeferredHolder<GasType, GasType> AIR = GAS_TYPE.register("air", () ->
            GasType.builder("gas.plenum.air")
                    .relativeDensity(1.0D)
                    .color(0x99FFFFFF)
                    .build()
    );

    public static final DeferredHolder<GasType, GasType> THICK = GAS_TYPE.register("thick", () ->
            GasType.builder("gas.plenum.thick")
                    .relativeDensity(1.7D)
                    .color(0x99FFFFFF)
                    .build()
    );

    public static final DeferredHolder<GasType, GasType> THIN = GAS_TYPE.register("thin", () ->
            GasType.builder("gas.plenum.thin")
                    .relativeDensity(0.59D)
                    .color(0x99FFFFFF)
                    .build()
    );
}
