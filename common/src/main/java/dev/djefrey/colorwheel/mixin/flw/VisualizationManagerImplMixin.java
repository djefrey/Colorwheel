package dev.djefrey.colorwheel.mixin.flw;

import dev.djefrey.colorwheel.ExtendedEngine;
import dev.djefrey.colorwheel.accessors.flw.LateInitAccessor;
import dev.engine_room.flywheel.api.backend.RenderContext;
import dev.engine_room.flywheel.impl.visualization.VisualizationManagerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Mixin(VisualizationManagerImpl.class)
public abstract class VisualizationManagerImplMixin
{
    @Unique
    private Method colorwheel$lateInitMethod;

    @Inject(method = "beginFrame",
            at = @At("RETURN"),
            remap = false)
    private void injectExtendedEngineBeginFrame(RenderContext context, CallbackInfo ci)
    {
        var engine = colorwheel$getLateInit().colorwheel$getEngine();

        if (engine instanceof ExtendedEngine extendedEngine)
        {
            extendedEngine.beginFrame(context);
        }
    }

    @Unique
    private LateInitAccessor colorwheel$getLateInit()
    {
        try
        {
            if (colorwheel$lateInitMethod == null)
            {
                colorwheel$lateInitMethod = ((VisualizationManagerImpl) (Object) this).getClass().getDeclaredMethod("lateInit");
            }

            return (LateInitAccessor) colorwheel$lateInitMethod.invoke(this);
        }
        catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }
}
