package dev.djefrey.colorwheel.mixin;

import dev.djefrey.colorwheel.accessors.ShadowRenderTargetsAccessor;
import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import net.irisshaders.iris.shadows.ShadowRenderTargets;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(ShadowRenderTargets.class)
public class ShadowRenderTargetsMixin implements ShadowRenderTargetsAccessor
{
    @Shadow(remap = false)
    @Final
    private List<GlFramebuffer> ownedFramebuffers;

    public void colorwheel$destroyFramebuffer(GlFramebuffer framebuffer)
    {
        framebuffer.destroy();
        this.ownedFramebuffers.remove(framebuffer);
    }
}
