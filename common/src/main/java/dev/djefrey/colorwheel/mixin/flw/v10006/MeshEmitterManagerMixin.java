package dev.djefrey.colorwheel.mixin.flw.v10006;

import dev.djefrey.colorwheel.accessors.flw10006.MeshEmitterManagerAccessor;
import dev.engine_room.flywheel.lib.model.SimpleModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "dev.engine_room.flywheel.lib.model.baked.MeshEmitterManager")
public class MeshEmitterManagerMixin implements MeshEmitterManagerAccessor
{
    @Unique
    private boolean colorwheel$isTerrain = false;

    @Inject(method = "prepareForBlock",
            at = @At("TAIL"),
            require = 0,
            remap = false)
    private void injectBeginTerrain(CallbackInfo ci)
    {
        colorwheel$isTerrain = true;
    }

    @Inject(method = "end",
            at = @At("HEAD"),
            require = 0,
            remap = false
    )
    private void injectEndTerrain(CallbackInfoReturnable<SimpleModel> cir)
    {
        this.colorwheel$isTerrain = false;
    }

    public boolean colorwheel$isTerrain()
    {
        return colorwheel$isTerrain;
    }
}
