package com.willbz.plenum.client.debug;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.willbz.plenum.simulation.constants.GasSimulationConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import static com.willbz.plenum.simulation.constants.GasSimulationConstants.GAS_UNITS_PER_BLOCK;

public class GasDebugRenderer {
    public static GasDebugRenderMode mode = GasDebugRenderMode.AMOUNT;

    private GasDebugRenderer() {}

    public static void render(PoseStack poseStack) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        if (!GasSimulationConstants.DEBUG_SYNC_GAS_CELLS) {
            return;
        }

        Vec3 cameraPos = minecraft.gameRenderer.getMainCamera().getPosition();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        VertexConsumer lines = bufferSource.getBuffer(RenderType.lines());

        for (GasDebugCell cell : ClientGasDebugData.cells()) {
            renderCellBox(poseStack, lines, cell);
        }

        poseStack.popPose();

        bufferSource.endBatch(RenderType.lines());

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void renderCellBox(PoseStack poseStack, VertexConsumer lines, GasDebugCell cell) {
        DebugColor color = colorFor(cell);

        AABB box = new AABB(cell.pos()).inflate(-0.03D);

        LevelRenderer.renderLineBox(
                poseStack,
                lines,
                box,
                color.red(),
                color.green(),
                color.blue(),
                color.alpha()
        );
    }

    private static DebugColor colorFor(GasDebugCell cell) {
        return switch (mode) {
            case GAS_COLOR -> gasColor(cell);
            case RED -> new DebugColor(1.0F, 0.0F, 0.0F, 1.0F);
            case CONCENTRATION -> concentrationColor(cell);
            case AMOUNT -> amountColor(cell);
            case TEMPERATURE -> temperatureColor(cell);
        };
    }

    private static @NotNull DebugColor gasColor(@NotNull GasDebugCell cell) {
        int color = cell.color();

        float red = ((color >> 16) & 255) / 255.0F;
        float green = ((color >> 8) & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;

        return new DebugColor(red, green, blue, 1.0F);
    }

    private static @NotNull DebugColor concentrationColor(@NotNull GasDebugCell cell) {
        double concentration = Mth.clamp(
                cell.amount() / GAS_UNITS_PER_BLOCK,
                0.0D,
                1.0D
        );

        return heatmap(concentration);
    }

    private static @NotNull DebugColor amountColor(@NotNull GasDebugCell cell) {
        double amount = Math.max(0.0D, cell.amount());

        if (amount <= 0.0D) {
            return new DebugColor(0.0F, 0.0F, 0.0F, 0.0F);
        }

        double minVisible = 0.001D;
        double maxVisible = GAS_UNITS_PER_BLOCK;

        double normalized = (Math.log10(amount) - Math.log10(minVisible)) / (Math.log10(maxVisible) - Math.log10(minVisible));

        normalized = Mth.clamp(normalized, 0.0D, 1.0D);

        return heatmap(normalized);
    }

    // TODO: Fix this when I add a proper thermodynamic model
    private static @NotNull DebugColor temperatureColor(@NotNull GasDebugCell cell) {
        double normalized = Mth.clamp((cell.temperatureC() + 50.0D) / 350.0D, 0.0D, 1.0D);

        return heatmap(normalized);
    }

    /**
     * 0.00 = blue
     * 0.25 = cyan
     * 0.50 = green
     * 0.75 = yellow
     * 1.00 = red
     */
    private static DebugColor heatmap(double value) {
        value = Mth.clamp(value, 0.0D, 1.0D);

        float red;
        float green;
        float blue;

        if (value < 0.25D) {
            double t = value / 0.25D;
            red = 0.0F;
            green = (float) t;
            blue = 1.0F;
        } else if (value < 0.5D) {
            double t = (value - 0.25D) / 0.25D;
            red = 0.0F;
            green = 1.0F;
            blue = (float) (1.0D - t);
        } else if (value < 0.75D) {
            double t = (value - 0.5D) / 0.25D;
            red = (float) t;
            green = 1.0F;
            blue = 0.0F;
        } else {
            double t = (value - 0.75D) / 0.25D;
            red = 1.0F;
            green = (float) (1.0D - t);
            blue = 0.0F;
        }

        return new DebugColor(red, green, blue, 1.0F);
    }

    private record DebugColor(float red, float green, float blue, float alpha) {}
}
