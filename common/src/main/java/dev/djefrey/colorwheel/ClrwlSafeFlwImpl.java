package dev.djefrey.colorwheel;

import dev.djefrey.colorwheel.engine.ClrwlEngine;
import dev.djefrey.colorwheel.engine.ShadowRenderContext;
import dev.djefrey.colorwheel.engine.ShadowRenderingPhase;
import dev.djefrey.colorwheel.engine.TranslucentRenderContext;
import dev.djefrey.colorwheel.engine.uniform.ClrwlOptionsUniforms;
import dev.engine_room.flywheel.api.backend.RenderContext;
import dev.engine_room.flywheel.api.internal.FlwApiLink;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.shadows.ShadowRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3d;

public class ClrwlSafeFlwImpl implements ClrwlSafeFlw
{
    @Override
    public boolean isColorwheelCurrentBackend()
    {
        return FlwApiLink.INSTANCE.getCurrentBackend() == ClrwlBackend.IRIS_INSTANCING;
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
    public void submitShadowRenderContext(ClientLevel level, CameraRenderState cameraRenderState, LevelRenderState levelRenderState, Vector3d cameraPos, float tickDelta, ShadowRenderingPhase phase)
    {
        VisualizationManager manager = VisualizationManager.get(level);

        if (manager != null)
        {
            RenderContext ctx = ShadowRenderContext.create(
                    Minecraft.getInstance().levelRenderer,
                    level,
                    Minecraft.getInstance().renderBuffers(),
                    ShadowRenderer.MODELVIEW,
                    ShadowRenderer.PROJECTION,
                    cameraRenderState,
                    levelRenderState,
                    (float) cameraPos.x(), (float) cameraPos.y(), (float) cameraPos.z(),
                    tickDelta,
                    phase
            );

            manager.renderDispatcher().afterEntities(ctx);
        }
    }

    @Override
    public void submitTranslucentRenderContext(ClientLevel level, CameraRenderState cameraRenderState, LevelRenderState levelRenderState, Matrix4fc modelMatrix, Matrix4fc projectionMatrix, float tickDelta)
    {
        VisualizationManager manager = VisualizationManager.get(level);

        if (manager != null)
        {
            RenderContext ctx = TranslucentRenderContext.create(
                    Minecraft.getInstance().levelRenderer,
                    level,
                    Minecraft.getInstance().renderBuffers(),
                    modelMatrix,
                    projectionMatrix,
                    cameraRenderState,
                    levelRenderState,
                    tickDelta
            );

            manager.renderDispatcher().afterEntities(ctx);
        }
    }
}
