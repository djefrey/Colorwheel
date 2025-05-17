package dev.djefrey.colorwheel;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.backend.RenderContext;
import dev.engine_room.flywheel.impl.event.RenderContextImpl;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public record ShadowRenderContext(LevelRenderer renderer, ClientLevel level, RenderBuffers buffers,
                                  Matrix4fc modelView, Matrix4fc projection, Matrix4fc viewProjection,
                                  Camera camera, float camX, float camY, float camZ,
                                  float partialTick,  RenderPhase phase) implements RenderContext
{
    public static ShadowRenderContext create(LevelRenderer renderer, ClientLevel level, RenderBuffers buffers,
                                             Matrix4fc modelView, Matrix4f projection,
                                             Camera camera, float camX, float camY, float camZ,
                                             float partialTick, RenderPhase phase)
    {
        Matrix4f viewProjection = new Matrix4f(projection);
        viewProjection.mul(modelView);

        return new ShadowRenderContext(renderer, level, buffers, modelView, projection, viewProjection, camera, camX, camY, camZ, partialTick, phase);
    }

    public enum RenderPhase
    {
        SOLID,
        TRANSLUCENT
    }
}
