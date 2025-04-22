package dev.djefrey.colorwheel.mixin;

import com.google.common.collect.ImmutableSet;
import dev.djefrey.colorwheel.accessors.IrisRenderingPipelineAccessor;
import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shadows.ShadowRenderTargets;
import net.irisshaders.iris.shadows.ShadowRenderer;
import net.irisshaders.iris.targets.RenderTargets;
import net.irisshaders.iris.uniforms.FrameUpdateNotifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

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

		return targets.createShadowFramebuffer(ImmutableSet.of(), sources.getDirectives().getDrawBuffers());
	}
}
