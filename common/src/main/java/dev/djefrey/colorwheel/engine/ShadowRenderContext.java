package dev.djefrey.colorwheel.engine;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.backend.RenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public record ShadowRenderContext(LevelRenderer renderer, ClientLevel level, RenderBuffers buffers,
                                  Matrix4fc modelView, Matrix4fc projection, Matrix4fc viewProjection,
                                  Matrix4fc playerModelView, Matrix4fc playerProjection, Matrix4fc playerViewProjection,
                                  Camera camera, float camX, float camY, float camZ,
                                  float partialTick, ShadowRenderingPhase phase) implements RenderContext
{
    public static ShadowRenderContext create(LevelRenderer renderer, ClientLevel level, RenderBuffers buffers,
                                             Matrix4fc modelView, Matrix4f projection,
                                             Matrix4fc playerModelView, Matrix4fc playerProjection,
                                             Camera camera, float camX, float camY, float camZ,
                                             float partialTick, ShadowRenderingPhase phase)
    {
        Matrix4f viewProjection = new Matrix4f(projection);
        viewProjection.mul(modelView);
        Matrix4f playerViewProjection = new Matrix4f(playerProjection);
        playerViewProjection.mul(playerModelView);

        return new ShadowRenderContext(renderer, level, buffers,
                                       modelView, projection, viewProjection,
                                       playerModelView, playerProjection, playerViewProjection,
                                       camera, camX, camY, camZ, partialTick, phase);
    }
}
