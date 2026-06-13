package dev.djefrey.colorwheel.fabric.mixin.flw;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.djefrey.colorwheel.accessors.flw.MeshEmitterAccessor;
import dev.engine_room.flywheel.lib.model.baked.FabricMeshEmitter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FabricMeshEmitter.class)
public abstract class FabricMeshEmitterMixin implements MeshEmitterAccessor
{
    @Inject(method = "addVertex(FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;addVertex(FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
    private void addVertex_injectBeginBlock(CallbackInfoReturnable<VertexConsumer> cir, @Local(name = "bufferBuilder") BufferBuilder bufferBuilder)
    {
        colorwheel$beginBlock(bufferBuilder);
    }

    @Inject(method = "addVertex(FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;addVertex(FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;",
                    shift = At.Shift.AFTER))
    private void addVertex_injectEndBlock(CallbackInfoReturnable<VertexConsumer> cir, @Local(name = "bufferBuilder") BufferBuilder bufferBuilder)
    {
        colorwheel$endBlock(bufferBuilder);
    }

    @Inject(method = "addVertex(FFFIFFIIFFF)V",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;addVertex(FFFIFFIIFFF)V"))
    private void addVertex2_injectBeginBlock(CallbackInfo ci, @Local(name = "bufferBuilder") BufferBuilder bufferBuilder)
    {
        colorwheel$beginBlock(bufferBuilder);
    }

    @Inject(method = "addVertex(FFFIFFIIFFF)V",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;addVertex(FFFIFFIIFFF)V",
                    shift = At.Shift.AFTER))
    private void addVertex2_injectEndBlock(CallbackInfo ci, @Local(name = "bufferBuilder") BufferBuilder bufferBuilder)
    {
        colorwheel$endBlock(bufferBuilder);
    }

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
