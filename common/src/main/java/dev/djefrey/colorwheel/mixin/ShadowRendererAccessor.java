package dev.djefrey.colorwheel.mixin;

import net.irisshaders.iris.shadows.ShadowRenderTargets;
import net.irisshaders.iris.shadows.ShadowRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ShadowRenderer.class)
public interface ShadowRendererAccessor
{
    @Accessor(remap = false)
    ShadowRenderTargets getTargets();
}
