package dev.djefrey.colorwheel.mixin.iris;

import com.google.common.collect.ImmutableSet;
import com.llamalad7.mixinextras.sugar.Local;
import dev.djefrey.colorwheel.Colorwheel;
import dev.djefrey.colorwheel.accessors.iris.IrisRenderingPipelineAccessor;
import dev.djefrey.colorwheel.accessors.iris.ShadowRenderTargetsAccessor;
import dev.djefrey.colorwheel.accessors.iris.ShadowRendererAccessor;
import net.irisshaders.iris.gl.buffer.ShaderStorageBufferHolder;
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

	@Shadow
	private ShaderStorageBufferHolder shaderStorageBufferHolder;

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
		Colorwheel.getSafeFlw().onIrisPipelineDestroy((IrisRenderingPipeline) (Object) this);
	}

	public ShaderStorageBufferHolder colorwheel$getSSBOHolder()
	{
		return shaderStorageBufferHolder;
	}
}
