package dev.djefrey.colorwheel.neoforge.mixin.flw;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.BufferBuilder;
import dev.djefrey.colorwheel.accessors.flw.MeshEmitterAccessor;
import dev.engine_room.flywheel.lib.model.baked.NeoForgeMeshEmitter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NeoForgeMeshEmitter.class)
public abstract class NeoForgeMeshEmitterMixin implements MeshEmitterAccessor
{
    @Inject(method = "putBakedQuad",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;putBakedQuad(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/resources/model/geometry/BakedQuad;Lcom/mojang/blaze3d/vertex/QuadInstance;)V"))
    private void putBakedQuad_injectBeginBlock(CallbackInfo ci, @Local(name = "bufferBuilder") BufferBuilder bufferBuilder)
    {
        colorwheel$beginBlock(bufferBuilder);
    }

    @Inject(method = "putBakedQuad",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;putBakedQuad(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/resources/model/geometry/BakedQuad;Lcom/mojang/blaze3d/vertex/QuadInstance;)V",
                    shift = At.Shift.AFTER))
    private void putBakedQuad_injectEndBlock(CallbackInfo ci, @Local(name = "bufferBuilder") BufferBuilder bufferBuilder)
    {
        colorwheel$endBlock(bufferBuilder);
    }

    @Inject(method = "putBlockBakedQuad",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;putBlockBakedQuad(FFFLnet/minecraft/client/resources/model/geometry/BakedQuad;Lcom/mojang/blaze3d/vertex/QuadInstance;)V"))
    private void putBlockBakedQuad_injectBeginBlock(CallbackInfo ci, @Local(name = "bufferBuilder") BufferBuilder bufferBuilder)
    {
        colorwheel$beginBlock(bufferBuilder);
    }

    @Inject(method = "putBlockBakedQuad",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;putBlockBakedQuad(FFFLnet/minecraft/client/resources/model/geometry/BakedQuad;Lcom/mojang/blaze3d/vertex/QuadInstance;)V",
                    shift = At.Shift.AFTER))
    private void putBlockBakedQuad_injectEndBlock(CallbackInfo ci, @Local(name = "bufferBuilder") BufferBuilder bufferBuilder)
    {
        colorwheel$endBlock(bufferBuilder);
    }
}
