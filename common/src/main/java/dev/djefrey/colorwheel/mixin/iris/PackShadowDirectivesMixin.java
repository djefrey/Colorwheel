package dev.djefrey.colorwheel.mixin.iris;

import dev.djefrey.colorwheel.accessors.PackShadowDirectivesAccessor;
import net.irisshaders.iris.shaderpack.properties.PackShadowDirectives;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PackShadowDirectives.class)
public class PackShadowDirectivesMixin implements PackShadowDirectivesAccessor
{
    @Unique
    private boolean colorwheel$renderFlywheelShadow = false;

    @Unique
    public boolean colorwheel$shouldRenderFlywheelShadow()
    {
        return colorwheel$renderFlywheelShadow;
    }

    @Unique
    public void colorwheel$setFlywheelShadowRendering(boolean bool)
    {
        this.colorwheel$renderFlywheelShadow = bool;
    }
}
