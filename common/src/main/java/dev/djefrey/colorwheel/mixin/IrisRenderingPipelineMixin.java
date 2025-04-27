package dev.djefrey.colorwheel.mixin;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderTarget;
import dev.djefrey.colorwheel.accessors.IrisRenderingPipelineAccessor;
import dev.djefrey.colorwheel.accessors.ShadowRenderTargetsAccessor;
import dev.djefrey.colorwheel.engine.ClrwlEngine;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.impl.visualization.VisualizationManagerImpl;
import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import net.irisshaders.iris.gl.texture.DepthBufferFormat;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shadows.ShadowRenderTargets;
import net.irisshaders.iris.shadows.ShadowRenderer;
import net.irisshaders.iris.targets.RenderTargets;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import org.joml.Vector3d;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(IrisRenderingPipeline.class)
public abstract class IrisRenderingPipelineMixin implements IrisRenderingPipelineAccessor
{
	@Shadow(remap = false)
	@Final
	private RenderTargets renderTargets;

	@Shadow(remap = false)
	@Final
	private ImmutableSet<Integer> flippedAfterPrepare;

	@Shadow(remap = false)
	@Final
	private ShadowRenderer shadowRenderer;

	public GlFramebuffer colorwheel$createGbuffersFramebuffer(ProgramSource sources)
	{
		var drawBuffers = sources.getDirectives().getDrawBuffers();

		return renderTargets.createGbufferFramebuffer(flippedAfterPrepare, drawBuffers);
	}

	public GlFramebuffer colorwheel$createShadowFramebuffer(ProgramSource sources)
	{
		var drawBuffers = sources.getDirectives().getDrawBuffers();
		ShadowRenderTargets targets = ((ShadowRendererAccessor) shadowRenderer).getTargets();

		return targets.createShadowFramebuffer(ImmutableSet.of(), drawBuffers);
	}

	@Unique
	private boolean colorwheel$hasFramebufferChanged = false;

	public boolean colorwheel$consumeFramebufferChanged()
	{
		boolean val = colorwheel$hasFramebufferChanged;

		colorwheel$hasFramebufferChanged = false;
		return val;
	}

	@Inject(method = "beginLevelRendering()V",
			at = @At("RETURN"),
			remap = false)
	public void onBeginLevelRendering(CallbackInfo ci, @Local boolean changed)
	{
		if (changed)
		{
			colorwheel$hasFramebufferChanged = true;
		}
	}

	public void colorwheel$destroyColorFramebuffer(GlFramebuffer framebuffer)
	{
		renderTargets.destroyFramebuffer(framebuffer);
	}

	public void colorwheel$destroyShadowFramebuffer(GlFramebuffer framebuffer)
	{
		ShadowRendererAccessor renderer = (ShadowRendererAccessor) shadowRenderer;
		ShadowRenderTargetsAccessor renderTargets = (ShadowRenderTargetsAccessor) renderer.getTargets();
		renderTargets.colorwheel$destroyFramebuffer(framebuffer);
	}

	@Inject(method = "destroy()V",
			at = @At("HEAD"),
			remap = false)
	public void colorwheel$onDelete(CallbackInfo ci)
	{
		ClrwlEngine engine = ClrwlEngine.ENGINES.get((IrisRenderingPipeline) (Object) this);

		if (engine != null)
		{
			// Direct access to the implementation is required as no method in the public API
			// allows you to reset a visualization manager

			VisualizationManagerImpl.reset(engine.level());
		}
	}
}
