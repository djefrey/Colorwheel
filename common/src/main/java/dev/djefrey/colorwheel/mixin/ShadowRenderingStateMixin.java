package dev.djefrey.colorwheel.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.djefrey.colorwheel.Colorwheel;
import dev.djefrey.colorwheel.ShadowRenderContext;
import dev.engine_room.flywheel.api.backend.RenderContext;
import dev.engine_room.flywheel.api.internal.FlwApiLink;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.impl.event.RenderContextImpl;
import net.irisshaders.iris.shadows.ShadowRenderer;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShadowRenderingState.class)
public abstract class ShadowRenderingStateMixin
{
	@Inject(method = "renderBlockEntities(Lnet/irisshaders/iris/shadows/ShadowRenderer;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/Camera;DDDFZZ)I",
			at = @At("HEAD"),
			remap = false,
			cancellable = true)
	private static void injectRenderBlockEntities(ShadowRenderer shadowRenderer, MultiBufferSource.BufferSource bufferSource, PoseStack modelView, Camera camera, double cameraX, double cameraY, double cameraZ, float tickDelta, boolean hasEntityFrustum, boolean lightsOnly, CallbackInfoReturnable<Integer> cir)
	{
		if (FlwApiLink.INSTANCE.getCurrentBackend() == Colorwheel.IRIS_INSTANCING)
		{
			ClientLevel level = Minecraft.getInstance().level;
			VisualizationManager manager = VisualizationManager.get(level);

			if (manager != null)
			{
				RenderContext ctx = ShadowRenderContext.create(
						Minecraft.getInstance().levelRenderer,
						level,
						Minecraft.getInstance().renderBuffers(),
						modelView,
						ShadowRenderer.PROJECTION,
						camera,
						(float) cameraX, (float) cameraY, (float) cameraZ,
						tickDelta,
						ShadowRenderContext.RenderPhase.SOLID
				);

				manager.renderDispatcher().afterEntities(ctx);
				cir.setReturnValue(0); // Cannot know how many have been drawn - too bad
			}
		}
	}
}
