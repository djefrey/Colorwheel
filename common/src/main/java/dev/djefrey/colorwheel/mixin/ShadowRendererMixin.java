package dev.djefrey.colorwheel.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.djefrey.colorwheel.Colorwheel;
import dev.djefrey.colorwheel.ShadowRenderContext;
import dev.djefrey.colorwheel.accessors.ShadowRendererAccessor;
import dev.engine_room.flywheel.api.backend.RenderContext;
import dev.engine_room.flywheel.api.internal.FlwApiLink;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.irisshaders.iris.mixin.LevelRendererAccessor;
import net.irisshaders.iris.shadows.ShadowRenderTargets;
import net.irisshaders.iris.shadows.ShadowRenderer;
import net.irisshaders.iris.uniforms.CameraUniforms;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShadowRenderer.class)
public abstract class ShadowRendererMixin implements ShadowRendererAccessor
{
    @Shadow(remap = false)
    @Final
    private boolean shouldRenderTranslucent;

    @Shadow(remap = false)
    @Final
    private boolean shouldRenderBlockEntities;

    @Shadow(remap = false)
    @Final
    private float sunPathRotation;

    @Shadow(remap = false)
    @Final
    private float intervalSize;

    @Accessor(remap = false)
    public abstract ShadowRenderTargets getTargets();

    @Inject(
            method = "renderShadows(Lnet/irisshaders/iris/mixin/LevelRendererAccessor;Lnet/minecraft/client/Camera;)V",
            at = @At(value = "INVOKE",
                    target = "net/irisshaders/iris/shadows/ShadowRenderer.copyPreTranslucentDepth (Lnet/irisshaders/iris/mixin/LevelRendererAccessor;)V",
            shift = At.Shift.AFTER),
            remap = false
    )
    private void injectRenderShadowsTranslucent(LevelRendererAccessor levelRenderer, Camera playerCamera, CallbackInfo ci)
    {
        if (shouldRenderTranslucent && shouldRenderBlockEntities && FlwApiLink.INSTANCE.getCurrentBackend() == Colorwheel.IRIS_INSTANCING)
        {
            ClientLevel level = Minecraft.getInstance().level;
            VisualizationManager manager = VisualizationManager.get(level);

            // Determine the player camera position
            Vector3d cameraPos = CameraUniforms.getUnshiftedCameraPosition();

            double cameraX = cameraPos.x();
            double cameraY = cameraPos.y();
            double cameraZ = cameraPos.z();

            final float tickDelta = CapturedRenderingState.INSTANCE.getTickDelta();

            if (manager != null)
            {
                RenderContext ctx = ShadowRenderContext.create(
                        Minecraft.getInstance().levelRenderer,
                        level,
                        Minecraft.getInstance().renderBuffers(),
                        ShadowRenderer.MODELVIEW,
                        ShadowRenderer.PROJECTION,
                        playerCamera,
                        (float) cameraX, (float) cameraY, (float) cameraZ,
                        tickDelta,
                        ShadowRenderContext.RenderPhase.TRANSLUCENT
                );

                manager.renderDispatcher().afterEntities(ctx);
            }
        }
    }
}
