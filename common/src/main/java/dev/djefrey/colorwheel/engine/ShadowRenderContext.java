package dev.djefrey.colorwheel.engine;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.backend.RenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public record ShadowRenderContext(LevelRenderer renderer, ClientLevel level, RenderBuffers buffers,
                                  Matrix4fc modelView, Matrix4fc projection, Matrix4fc viewProjection,
                                  CameraRenderState cameraRenderState, LevelRenderState levelRenderState,
                                  float camX, float camY, float camZ,
                                  float partialTick, ShadowRenderingPhase phase) implements RenderContext
{
    public static ShadowRenderContext create(LevelRenderer renderer, ClientLevel level, RenderBuffers buffers,
                                             Matrix4fc modelView, Matrix4f projection,
                                             CameraRenderState cameraRenderState, LevelRenderState levelRenderState,
                                             float camX, float camY, float camZ,
                                             float partialTick, ShadowRenderingPhase phase)
    {
        Matrix4f viewProjection = new Matrix4f(projection);
        viewProjection.mul(modelView);

        return new ShadowRenderContext(renderer, level, buffers, modelView, projection, viewProjection, cameraRenderState, levelRenderState, camX, camY, camZ, partialTick, phase);
    }
}
