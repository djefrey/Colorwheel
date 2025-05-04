package dev.djefrey.colorwheel.mixin;

import dev.djefrey.colorwheel.accessors.RenderTargetAccessor;
import net.irisshaders.iris.gl.texture.PixelFormat;
import net.irisshaders.iris.gl.texture.PixelType;
import net.irisshaders.iris.targets.RenderTarget;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RenderTarget.class)
public class RenderTargetMixin implements RenderTargetAccessor
{
    @Shadow(remap = false)
    @Final
    private PixelFormat format;

    @Shadow(remap = false)
    @Final
    private PixelType type;

    public PixelFormat colorwheel$getPixelFormat()
    {
        return format;
    }

    public PixelType colorwheel$getPixelType()
    {
        return type;
    }
}
