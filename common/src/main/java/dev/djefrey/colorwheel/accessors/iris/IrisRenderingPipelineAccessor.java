package dev.djefrey.colorwheel.accessors.iris;

import dev.djefrey.colorwheel.shaderpack.ClrwlProgramGroup;
import net.irisshaders.iris.gl.buffer.ShaderStorageBufferHolder;
import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shadows.ShadowRenderTargets;
import net.irisshaders.iris.targets.RenderTargets;

public interface IrisRenderingPipelineAccessor
{
	GlFramebuffer colorwheel$createSolidGbuffersFramebuffer(ProgramSource sources);
	GlFramebuffer colorwheel$createTranslucentGbuffersFramebuffer(ProgramSource sources);
	GlFramebuffer colorwheel$createShadowFramebuffer(ProgramSource sources);

	RenderTargets colorwheel$getGbuffersRenderTargets();
	ShadowRenderTargets colorwheel$getShadowRenderTargets();

	boolean colorwheel$consumeFramebufferChanged();
	void colorwheel$destroyGbuffersFramebuffer(GlFramebuffer framebuffer);
	void colorwheel$destroyShadowFramebuffer(GlFramebuffer framebuffer);

	ShaderStorageBufferHolder colorwheel$getSSBOHolder();

	record ProgramGroupDepthInfo(int textureId, int width, int height)
	{
	}

	default ProgramGroupDepthInfo getProgramGroupDepthInfo(ClrwlProgramGroup programGroup)
	{
		int depthTexture;
		int width;
		int height;

		switch (programGroup)
		{
			case GBUFFERS ->
			{
				RenderTargets targets = this.colorwheel$getGbuffersRenderTargets();

				depthTexture = targets.getDepthTexture();
				width = targets.getCurrentWidth();
				height = targets.getCurrentHeight();
			}
			case SHADOW ->
			{
				ShadowRenderTargets targets = this.colorwheel$getShadowRenderTargets();

				depthTexture = targets.getDepthTexture().getTextureId();
				width = targets.getResolution();
				height = targets.getResolution();
			}

			default ->
			{
				throw new RuntimeException("Unknown program group: " + programGroup);
			}
		}

		return new ProgramGroupDepthInfo(depthTexture, width, height);
	}
}
