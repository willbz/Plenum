package com.willbz.plenum;

import com.mojang.logging.LogUtils;
import com.willbz.plenum.api.registry.DevGases;
import com.willbz.plenum.api.registry.PlenumRegistries;
import com.willbz.plenum.client.debug.ClientGasDebugData;
import com.willbz.plenum.client.debug.ClientboundGasDebugPacket;
import com.willbz.plenum.client.debug.GasDebugRenderer;
import com.willbz.plenum.command.PlenumCommand;
import com.willbz.plenum.command.arguments.PlenumArgumentTypes;
import com.willbz.plenum.simulation.manager.GasSimulationManager;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

@Mod(Plenum.MODID)
public class Plenum {
    public static final String MODID = "plenum";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Plenum(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::registerRegistries);
        modEventBus.addListener(this::registerPayloadHandlers);

        PlenumArgumentTypes.register(modEventBus);

        if (!FMLEnvironment.production) {
            DevGases.GAS_TYPE.register(modEventBus);
        }

        NeoForge.EVENT_BUS.register(this);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    public void registerRegistries(@NotNull NewRegistryEvent event) {
        event.register(new RegistryBuilder<>(PlenumRegistries.GAS_TYPE)
                .sync(true)
                .create()
        );
    }

    public void registerPayloadHandlers(@NotNull RegisterPayloadHandlersEvent event) {
        event.registrar(MODID)
                .playToClient(
                        ClientboundGasDebugPacket.TYPE,
                        ClientboundGasDebugPacket.STREAM_CODEC,
                        ClientboundGasDebugPacket::handle
                );
    }

    @SubscribeEvent
    public void onLevelTickPost(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        GasSimulationManager.tick(serverLevel);
    }

    @SubscribeEvent
    public void onBlockBroken(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            GasSimulationManager.onBlockChanged(serverLevel, event.getPos());
        }
    }

    @SubscribeEvent
    public void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            GasSimulationManager.onBlockChanged(serverLevel, event.getPos());
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(@NotNull RegisterCommandsEvent event) {
        PlenumCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
    }

    @EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
        }

        @SubscribeEvent
        public static void renderGasDebugCells(RenderLevelStageEvent event) {
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
                return;
            }

            GasDebugRenderer.render(event.getPoseStack());
        }

        @SubscribeEvent
        public static void clearGasDebugCells(ClientPlayerNetworkEvent.LoggingOut event) {
            ClientGasDebugData.clear();
        }
    }
}
