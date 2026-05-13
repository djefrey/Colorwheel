package dev.djefrey.colorwheel.mixin.iris;

import dev.djefrey.colorwheel.accessors.iris.BlendModeOverrideAccessor;
import dev.djefrey.colorwheel.engine.ClrwlBlendModeOverride;
import net.irisshaders.iris.gl.blending.BlendMode;
import net.irisshaders.iris.gl.blending.BlendModeOverride;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BlendModeOverride.class)
public class BlendModeOverrideMixin implements BlendModeOverrideAccessor
{
    @Shadow @Final private BlendMode blendMode;

    @Override
    public ClrwlBlendModeOverride colorwheel$convert()
    {
        return new ClrwlBlendModeOverride(blendMode);
    }
}
