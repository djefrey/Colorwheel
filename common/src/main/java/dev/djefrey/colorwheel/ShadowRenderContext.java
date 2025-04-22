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

public record ShadowRenderContext(LevelRenderer renderer, ClientLevel level, RenderBuffers buffers, PoseStack stack,
                                  Matrix4fc projection, Matrix4fc viewProjection,
                                  Camera camera, float camX, float camY, float camZ,
                                  float partialTick) implements RenderContext
{
    public static ShadowRenderContext create(LevelRenderer renderer, ClientLevel level, RenderBuffers buffers,
                                             PoseStack stack, Matrix4f projection,
                                             Camera camera, float camX, float camY, float camZ,
                                             float partialTick)
    {
        Matrix4f viewProjection = new Matrix4f(projection);
        viewProjection.mul(stack.last()
                .pose());

        return new ShadowRenderContext(renderer, level, buffers, stack, projection, viewProjection, camera, camX, camY, camZ, partialTick);
    }
}
