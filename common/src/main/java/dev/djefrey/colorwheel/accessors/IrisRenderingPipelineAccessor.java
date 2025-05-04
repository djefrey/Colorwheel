package dev.djefrey.colorwheel.accessors;

import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shadows.ShadowRenderTargets;
import net.irisshaders.iris.targets.RenderTargets;

public interface IrisRenderingPipelineAccessor
{
	GlFramebuffer colorwheel$createGbuffersFramebuffer(ProgramSource sources);
	GlFramebuffer colorwheel$createShadowFramebuffer(ProgramSource sources);

	RenderTargets colorwheel$getGbuffersRenderTargets();
	ShadowRenderTargets colorwheel$getShadowRenderTargets();

	boolean colorwheel$consumeFramebufferChanged();
	void colorwheel$destroyColorFramebuffer(GlFramebuffer framebuffer);
	void colorwheel$destroyShadowFramebuffer(GlFramebuffer framebuffer);
}
