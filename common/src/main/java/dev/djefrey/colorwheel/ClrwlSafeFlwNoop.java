package dev.djefrey.colorwheel;

import dev.djefrey.colorwheel.engine.ShadowRenderingPhase;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3d;

public class ClrwlSafeFlwNoop implements ClrwlSafeFlw
{
    @Override
    public boolean isColorwheelCurrentBackend()
    {
        return false;
    }

    @Override
    public void updateOptionsUniform(Options options)
    {

    }

    @Override
    public void onIrisPipelineDestroy(IrisRenderingPipeline pipeline)
    {

    }

    @Override
    public void submitShadowRenderContext(ClientLevel level, CameraRenderState cameraRenderState, LevelRenderState levelRenderState, Vector3d cameraPos, float tickDelta, ShadowRenderingPhase phase)
    {

    }

    @Override
    public void submitTranslucentRenderContext(ClientLevel level, CameraRenderState cameraRenderState, LevelRenderState levelRenderState, Matrix4fc modelMatrix, Matrix4f projectionMatrix, float tickDelta)
    {

    }
}
