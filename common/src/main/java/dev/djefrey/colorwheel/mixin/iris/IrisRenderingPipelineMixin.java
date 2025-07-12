package dev.djefrey.colorwheel.mixin.iris;

import com.google.common.collect.ImmutableSet;
import com.llamalad7.mixinextras.sugar.Local;
import dev.djefrey.colorwheel.accessors.IrisRenderingPipelineAccessor;
import dev.djefrey.colorwheel.accessors.ShadowRenderTargetsAccessor;
import dev.djefrey.colorwheel.accessors.ShadowRendererAccessor;
import dev.djefrey.colorwheel.engine.BeginTranslucentRenderFunction;
import dev.djefrey.colorwheel.engine.ClrwlEngine;
import dev.engine_room.flywheel.impl.visualization.VisualizationManagerImpl;
import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shadows.ShadowRenderTargets;
import net.irisshaders.iris.shadows.ShadowRenderer;
import net.irisshaders.iris.targets.RenderTargets;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IrisRenderingPipeline.class)
public abstract class IrisRenderingPipelineMixin implements IrisRenderingPipelineAccessor
{
	@Shadow
	@Final
	private RenderTargets renderTargets;

	@Shadow
	@Final
	private ImmutableSet<Integer> flippedAfterPrepare;

	@Shadow
	@Final
	private ImmutableSet<Integer> flippedAfterTranslucent;

	@Shadow
	@Final
	private ShadowRenderer shadowRenderer;

	public GlFramebuffer colorwheel$createSolidGbuffersFramebuffer(ProgramSource sources)
	{
		var drawBuffers = sources.getDirectives().getDrawBuffers();

		return renderTargets.createGbufferFramebuffer(flippedAfterPrepare, drawBuffers);
	}

	public GlFramebuffer colorwheel$createTranslucentGbuffersFramebuffer(ProgramSource sources)
	{
		var drawBuffers = sources.getDirectives().getDrawBuffers();

		return renderTargets.createGbufferFramebuffer(flippedAfterTranslucent, drawBuffers);
	}

	public GlFramebuffer colorwheel$createShadowFramebuffer(ProgramSource sources)
	{
		var drawBuffers = sources.getDirectives().getDrawBuffers();
		ShadowRenderTargets targets = ((ShadowRendererAccessor) shadowRenderer).getTargets();

		return targets.createShadowFramebuffer(ImmutableSet.of(), drawBuffers);
	}

	public RenderTargets colorwheel$getGbuffersRenderTargets()
	{
		return renderTargets;
	}

	public ShadowRenderTargets colorwheel$getShadowRenderTargets()
	{
		return ((ShadowRendererAccessor) shadowRenderer).getTargets();
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
	private void onBeginLevelRendering(CallbackInfo ci, @Local boolean changed)
	{
		if (changed)
		{
			colorwheel$hasFramebufferChanged = true;
		}
	}

	@Unique
	private BeginTranslucentRenderFunction colorwheel$beginTranslucentCallback = null;

	@Inject(method = "beginTranslucents()V",
			at = @At("RETURN"),
			remap = false)
	private void onBeginTranslucents(CallbackInfo ci)
	{
		if (colorwheel$beginTranslucentCallback != null)
		{
			colorwheel$beginTranslucentCallback.onBeginTranslucent();
		}
	}

	public void colorwheel$setBeginTranslucentsCallback(BeginTranslucentRenderFunction fct)
	{
		this.colorwheel$beginTranslucentCallback = fct;
	}

	public void colorwheel$destroyGbuffersFramebuffer(GlFramebuffer framebuffer)
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
