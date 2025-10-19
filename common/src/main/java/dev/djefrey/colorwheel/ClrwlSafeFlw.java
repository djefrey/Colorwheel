package dev.djefrey.colorwheel;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.djefrey.colorwheel.engine.ShadowRenderingPhase;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.minecraft.client.Camera;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import org.joml.Vector3d;

public interface ClrwlSafeFlw
{
    boolean isColorwheelCurrentBackend();
    void updateOptionsUniform(Options options);
    void resetVisuals(IrisRenderingPipeline pipeline);
    void submitShadowRenderContext(ClientLevel level, PoseStack modelView, Camera playerCamera, Vector3d cameraPos, float tickDelta, ShadowRenderingPhase phase);
}
