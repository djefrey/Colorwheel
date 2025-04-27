package dev.djefrey.colorwheel.accessors;

import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;

public interface IrisRenderingPipelineAccessor
{
	GlFramebuffer colorwheel$createGbuffersFramebuffer(ProgramSource sources);
	GlFramebuffer colorwheel$createShadowFramebuffer(ProgramSource sources);
	boolean colorwheel$consumeFramebufferChanged();
	void colorwheel$destroyColorFramebuffer(GlFramebuffer framebuffer);
	void colorwheel$destroyShadowFramebuffer(GlFramebuffer framebuffer);
}
