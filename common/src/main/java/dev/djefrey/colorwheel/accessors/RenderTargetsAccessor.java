package dev.djefrey.colorwheel.accessors;

import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;

public interface RenderTargetsAccessor
{
    GlFramebuffer callCreateEmptyFramebuffer();
}
