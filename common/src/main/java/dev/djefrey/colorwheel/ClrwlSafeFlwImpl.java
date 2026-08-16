package dev.djefrey.colorwheel;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.djefrey.colorwheel.engine.ClrwlEngine;
import dev.djefrey.colorwheel.engine.ShadowRenderContext;
import dev.djefrey.colorwheel.engine.ShadowRenderingPhase;
import dev.djefrey.colorwheel.engine.TranslucentRenderContext;
import dev.djefrey.colorwheel.engine.uniform.ClrwlOptionsUniforms;
import dev.engine_room.flywheel.api.backend.RenderContext;
import dev.engine_room.flywheel.api.internal.FlwApiLink;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.impl.visualization.VisualizationManagerImpl;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.shadows.ShadowRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import org.joml.Matrix4f;
import org.joml.Vector3d;

public class ClrwlSafeFlwImpl implements ClrwlSafeFlw
{
    @Override
    public boolean isColorwheelCurrentBackend()
    {
        var backend = FlwApiLink.INSTANCE.getCurrentBackend();
        return backend == ClrwlBackend.IRIS_INSTANCING || backend == ClrwlBackend.IRIS_INDIRECT;
    }

    @Override
    public void updateOptionsUniform(Options options)
    {
        ClrwlOptionsUniforms.update(options);
    }

    @Override
    public void onIrisPipelineDestroy(IrisRenderingPipeline pipeline)
    {
        for (var engine : ClrwlEngine.ENGINES)
        {
            engine.onIrisPipelineDestroy(pipeline);
        }
    }

    @Override
    public void submitShadowRenderContext(ClientLevel level, PoseStack modelView, Camera playerCamera, Vector3d cameraPos, float tickDelta, ShadowRenderingPhase phase)
    {
        VisualizationManager manager = VisualizationManager.get(level);

        if (manager != null)
        {
            RenderContext ctx = ShadowRenderContext.create(
                    Minecraft.getInstance().levelRenderer,
                    level,
                    Minecraft.getInstance().renderBuffers(),
                    modelView,
                    ShadowRenderer.PROJECTION,
                    playerCamera,
                    (float) cameraPos.x(), (float) cameraPos.y(), (float) cameraPos.z(),
                    tickDelta,
                    phase
            );

            manager.renderDispatcher().afterEntities(ctx);
        }
    }

    @Override
    public void submitTranslucentRenderContext(ClientLevel level, PoseStack stack, Camera playerCamera, Matrix4f projectionMatrix, float tickDelta)
    {
        VisualizationManager manager = VisualizationManager.get(level);

        if (manager != null)
        {
            RenderContext ctx = TranslucentRenderContext.create(
                    Minecraft.getInstance().levelRenderer,
                    level,
                    Minecraft.getInstance().renderBuffers(),
                    stack,
                    projectionMatrix,
                    playerCamera,
                    tickDelta
            );

            manager.renderDispatcher().afterEntities(ctx);
        }
    }
}
