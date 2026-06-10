package com.willbz.plenum.command.arguments;

import com.willbz.plenum.Plenum;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class PlenumArgumentTypes {
    public static final DeferredRegister<ArgumentTypeInfo<?, ?>> ARGUMENT_TYPES =
            DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, Plenum.MODID);

    public static final Supplier<ArgumentTypeInfo<?, ?>> GAS =
            ARGUMENT_TYPES.register(
                    "gas",
                    () -> ArgumentTypeInfos.registerByClass(
                            GasArgument.class,
                            SingletonArgumentInfo.contextFree(GasArgument::gas)
                    )
            );

    public static void register(IEventBus bus) {
        ARGUMENT_TYPES.register(bus);
    }
}
