package dev.djefrey.colorwheel.mixin.iris;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.djefrey.colorwheel.Colorwheel;
import dev.djefrey.colorwheel.ShadowRenderContext;
import dev.djefrey.colorwheel.accessors.PackShadowDirectivesAccessor;
import dev.djefrey.colorwheel.accessors.ShadowRendererAccessor;
import dev.engine_room.flywheel.api.backend.RenderContext;
import dev.engine_room.flywheel.api.internal.FlwApiLink;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.irisshaders.iris.mixin.LevelRendererAccessor;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shaderpack.properties.PackDirectives;
import net.irisshaders.iris.shadows.ShadowCompositeRenderer;
import net.irisshaders.iris.shadows.ShadowRenderTargets;
import net.irisshaders.iris.shadows.ShadowRenderer;
import net.irisshaders.iris.uniforms.CameraUniforms;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.uniforms.custom.CustomUniforms;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShadowRenderer.class)
public abstract class ShadowRendererMixin implements ShadowRendererAccessor
{
    @Shadow
    @Final
    private boolean shouldRenderTranslucent;

    @Shadow
    @Final
    private float sunPathRotation;

    @Shadow
    @Final
    private float intervalSize;

    @Shadow
    public static PoseStack createShadowModelView(float sunPathRotation, float intervalSize)
    {
        return null;
    }

    @Accessor
    public abstract ShadowRenderTargets getTargets();

    @Unique
    private boolean colorwheel$shouldRenderShadow;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void injectInit(ProgramSource shadow, PackDirectives directives, ShadowRenderTargets shadowRenderTargets, ShadowCompositeRenderer compositeRenderer, CustomUniforms customUniforms, boolean separateHardwareSamplers, CallbackInfo ci)
    {
        colorwheel$shouldRenderShadow = ((PackShadowDirectivesAccessor) directives.getShadowDirectives()).colorwheel$shouldRenderFlywheelShadow();
    }

    @Inject(
            method = "renderShadows(Lnet/irisshaders/iris/mixin/LevelRendererAccessor;Lnet/minecraft/client/Camera;)V",
            at = @At(value = "CONSTANT", args = "stringValue=build blockentities")
    )
    private void injectRenderShadows(LevelRendererAccessor levelRenderer, Camera playerCamera, CallbackInfo ci)
    {
        if (colorwheel$shouldRenderShadow && FlwApiLink.INSTANCE.getCurrentBackend() == Colorwheel.IRIS_INSTANCING)
        {
            ClientLevel level = Minecraft.getInstance().level;
            VisualizationManager manager = VisualizationManager.get(level);

            // Determine the player camera position
            Vector3d cameraPos = CameraUniforms.getUnshiftedCameraPosition();
            PoseStack modelView = createShadowModelView(this.sunPathRotation, this.intervalSize);

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
                        modelView,
                        ShadowRenderer.PROJECTION,
                        playerCamera,
                        (float) cameraX, (float) cameraY, (float) cameraZ,
                        tickDelta,
                        ShadowRenderContext.RenderPhase.SOLID
                );

                manager.renderDispatcher().afterEntities(ctx);
            }
        }
    }

    @Inject(
            method = "renderShadows(Lnet/irisshaders/iris/mixin/LevelRendererAccessor;Lnet/minecraft/client/Camera;)V",
            at = @At(value = "CONSTANT", args = "stringValue=translucent terrain")
    )
    private void injectRenderShadowsTranslucent(LevelRendererAccessor levelRenderer, Camera playerCamera, CallbackInfo ci)
    {
        if (shouldRenderTranslucent && colorwheel$shouldRenderShadow && FlwApiLink.INSTANCE.getCurrentBackend() == Colorwheel.IRIS_INSTANCING)
        {
            ClientLevel level = Minecraft.getInstance().level;
            VisualizationManager manager = VisualizationManager.get(level);

            // Determine the player camera position
            Vector3d cameraPos = CameraUniforms.getUnshiftedCameraPosition();
            PoseStack modelView = createShadowModelView(this.sunPathRotation, this.intervalSize);

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
                        modelView,
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
