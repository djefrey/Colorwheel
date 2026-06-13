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

public record TranslucentRenderContext(LevelRenderer renderer, ClientLevel level, RenderBuffers buffers,
                                       Matrix4fc modelView, Matrix4fc projection, Matrix4fc viewProjection,
                                       CameraRenderState cameraRenderState, LevelRenderState levelRenderState,
                                       float partialTick) implements RenderContext
{
    public static TranslucentRenderContext create(LevelRenderer renderer, ClientLevel level, RenderBuffers buffers,
                                                  Matrix4fc modelView, Matrix4f projection,
                                                  CameraRenderState cameraRenderState, LevelRenderState levelRenderState,
                                                  float partialTick)
    {
        Matrix4f viewProjection = new Matrix4f(projection);
        viewProjection.mul(modelView);

        return new TranslucentRenderContext(renderer, level, buffers, modelView, projection, viewProjection, cameraRenderState, levelRenderState, partialTick);
    }
}
