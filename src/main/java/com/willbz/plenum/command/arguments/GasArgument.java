package com.willbz.plenum.command.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.willbz.plenum.api.gas.GasType;
import com.willbz.plenum.api.registry.PlenumRegistries;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.concurrent.CompletableFuture;

public class GasArgument implements ArgumentType<ResourceLocation> {
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_GAS =
            new DynamicCommandExceptionType(id -> Component.translatable("commands.plenum.gas.error.not_found", id));

    public static GasArgument gas() {
        return new GasArgument();
    }

    public static GasType getGas(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        ResourceLocation gasId = context.getArgument(name, ResourceLocation.class);

        Registry<GasType> gasRegistry = context.getSource()
                .registryAccess()
                .registryOrThrow(PlenumRegistries.GAS_TYPE);

        GasType gas = gasRegistry.get(gasId);

        if (gas == null) {
            throw ERROR_UNKNOWN_GAS.create(gasId);
        }

        return gas;
    }

    @Override
    public ResourceLocation parse(StringReader reader) throws CommandSyntaxException {
        return ResourceLocation.read(reader);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        S source = context.getSource();

        if (!(source instanceof SharedSuggestionProvider suggestionProvider)) {
            return Suggestions.empty();
        }

        Registry<GasType> gasRegistry = suggestionProvider.registryAccess()
                .registryOrThrow(PlenumRegistries.GAS_TYPE);

        return SharedSuggestionProvider.suggestResource(gasRegistry.keySet(), builder);
    }
}