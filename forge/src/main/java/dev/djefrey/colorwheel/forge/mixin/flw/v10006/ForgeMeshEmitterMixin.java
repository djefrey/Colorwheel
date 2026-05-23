package dev.djefrey.colorwheel.forge.mixin.flw.v10006;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.djefrey.colorwheel.ColorwheelBufferBuilder;
import dev.engine_room.flywheel.api.model.Model;
import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder;
import net.minecraft.client.renderer.block.model.BakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(targets = "dev.engine_room.flywheel.lib.model.baked.ForgeMeshEmitter")
@Pseudo
public abstract class ForgeMeshEmitterMixin implements ColorwheelBufferBuilder, VertexConsumer
{
    @Unique
    private short colorwheel$currentBlock = -1;
    @Unique
    private short colorwheel$currentRenderType = -1;
    @Unique
    private byte colorwheel$lightEmission = 0;
    @Unique
    private int colorwheel$currentLocalPosX = 0;
    @Unique
    private int colorwheel$currentLocalPosY = 0;
    @Unique
    private int colorwheel$currentLocalPosZ = 0;

    @Unique
    private boolean colorwheel$isTerrain = false;

    @Override
    public void clrwlBeginBlock(short block, short renderType, byte lightEmission, boolean isTerrain, int posX, int posY, int posZ)
    {
        ColorwheelBufferBuilder.super.clrwlBeginBlock(block, renderType, lightEmission, isTerrain, posX, posY, posZ);
        this.colorwheel$lightEmission = lightEmission;
    }

    // Called by ModelBlockRendererMixin::injectBeginEndBlock
    @Override
    public void beginBlock(short block, short renderType, int localPosX, int localPosY, int localPosZ)
    {
        this.colorwheel$currentBlock = block;
        this.colorwheel$currentRenderType = renderType;
        this.colorwheel$lightEmission = 0;
        this.colorwheel$currentLocalPosX = localPosX;
        this.colorwheel$currentLocalPosY = localPosY;
        this.colorwheel$currentLocalPosZ = localPosZ;
    }

    // Called by ModelBlockRendererMixin::injectBeginEndBlock
    @Override
    public void endBlock()
    {
        this.colorwheel$currentBlock = -1;
        this.colorwheel$currentRenderType = -1;
        this.colorwheel$lightEmission = 0;
        this.colorwheel$currentLocalPosX = 0;
        this.colorwheel$currentLocalPosY = 0;
        this.colorwheel$currentLocalPosZ = 0;
    }

    @Inject(method = "prepareForBlock",
            at = @At("TAIL"),
            require = 0,
            remap = false)
    private void injectBeginTerrain(CallbackInfo ci)
    {
        colorwheel$isTerrain = true;
    }

    @Inject(method = "end",
            at = @At("TAIL"),
            require = 0,
            remap = false)
    private void injectEndTerrain(ImmutableList.Builder<Model.ConfiguredMesh> out, CallbackInfo ci)
    {
        this.colorwheel$isTerrain = false;
    }

    @Unique
    private void colorwheel$beforePutBulkData(BufferBuilder bufferBuilder)
    {
        if (bufferBuilder instanceof ColorwheelBufferBuilder clrwlBuilder)
        {
            clrwlBuilder.clrwlBeginBlock(colorwheel$currentBlock, colorwheel$currentRenderType, colorwheel$lightEmission, colorwheel$isTerrain, colorwheel$currentLocalPosX, colorwheel$currentLocalPosY, colorwheel$currentLocalPosZ);
        }
        else if (bufferBuilder instanceof BlockSensitiveBufferBuilder blockBuilder)
        {
            blockBuilder.beginBlock(colorwheel$currentBlock, colorwheel$currentBlock, colorwheel$currentLocalPosX, colorwheel$currentLocalPosY, colorwheel$currentLocalPosZ);
        }
    }

    @Unique
    private void colorwheel$afterPutBulkData(BufferBuilder bufferBuilder)
    {
        if (bufferBuilder instanceof ColorwheelBufferBuilder clrwlBuilder)
        {
            clrwlBuilder.clrwlEndBlock();
        }
        else if (bufferBuilder instanceof BlockSensitiveBufferBuilder blockBuilder)
        {
            blockBuilder.endBlock();
        }
    }

    @Inject(method = "putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;FFFII)V",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;FFFII)V"
            ),
            locals = LocalCapture.CAPTURE_FAILSOFT,
            require = 0,
            remap = false
    )
    private void injectBegin_putBulkDataA(PoseStack.Pose pose, BakedQuad quad, float red, float green, float blue, int light, int overlay, CallbackInfo ci, BufferBuilder bufferBuilder)
    {
        colorwheel$beforePutBulkData(bufferBuilder);
    }

    @Inject(method = "putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;FFFII)V",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;FFFII)V",
                    shift = At.Shift.AFTER
            ),
            locals = LocalCapture.CAPTURE_FAILSOFT,
            require = 0,
            remap = false
    )
    private void injectEnd_putBulkDataA(PoseStack.Pose pose, BakedQuad quad, float red, float green, float blue, int light, int overlay, CallbackInfo ci, BufferBuilder bufferBuilder)
    {
        colorwheel$afterPutBulkData(bufferBuilder);
    }


    @Inject(method = "putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;FFFFIIZ)V",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;FFFFIIZ)V"
            ),
            locals = LocalCapture.CAPTURE_FAILSOFT,
            require = 0,
            remap = false
    )
    private void injectBegin_putBulkDataB(PoseStack.Pose pose, BakedQuad quad, float red, float green, float blue, float alpha, int light, int overlay, boolean readExistingColor, CallbackInfo ci, BufferBuilder bufferBuilder)
    {
        colorwheel$beforePutBulkData(bufferBuilder);
    }

    @Inject(method = "putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;FFFFIIZ)V",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;FFFFIIZ)V",
                    shift = At.Shift.AFTER
            ),
            locals = LocalCapture.CAPTURE_FAILSOFT,
            require = 0,
            remap = false
    )
    private void injectEnd_putBulkDataB(PoseStack.Pose pose, BakedQuad quad, float red, float green, float blue, float alpha, int light, int overlay, boolean readExistingColor, CallbackInfo ci, BufferBuilder bufferBuilder)
    {
        colorwheel$afterPutBulkData(bufferBuilder);
    }


    @Inject(method = "putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;[FFFFF[IIZ)V",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;[FFFFF[IIZ)V"
            ),
            locals = LocalCapture.CAPTURE_FAILSOFT,
            require = 0,
            remap = false
    )
    private void injectBegin_putBulkDataC(PoseStack.Pose pose, BakedQuad quad, float[] brightnesses, float red, float green, float blue, float alpha, int[] lights, int overlay, boolean readExistingColor, CallbackInfo ci, BufferBuilder bufferBuilder)
    {
        colorwheel$beforePutBulkData(bufferBuilder);
    }

    @Inject(method = "putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;[FFFFF[IIZ)V",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;[FFFFF[IIZ)V",
                    shift = At.Shift.AFTER
            ),
            locals = LocalCapture.CAPTURE_FAILSOFT,
            require = 0,
            remap = false
    )
    private void injectEnd_putBulkDataC(PoseStack.Pose pose, BakedQuad quad, float[] brightnesses, float red, float green, float blue, float alpha, int[] lights, int overlay, boolean readExistingColor, CallbackInfo ci, BufferBuilder bufferBuilder)
    {
        colorwheel$afterPutBulkData(bufferBuilder);
    }


    @Inject(method = "putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;[FFFF[IIZ)V",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;[FFFF[IIZ)V"
            ),
            locals = LocalCapture.CAPTURE_FAILSOFT,
            require = 0,
            remap = false
    )
    private void injectBegin_putBulkDataD(PoseStack.Pose pose, BakedQuad quad, float[] brightnesses, float red, float green, float blue, int[] lights, int overlay, boolean readExistingColor, CallbackInfo ci, BufferBuilder bufferBuilder)
    {
        colorwheel$beforePutBulkData(bufferBuilder);
    }

    @Inject(method = "putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;[FFFF[IIZ)V",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;[FFFF[IIZ)V",
                    shift = At.Shift.AFTER
            ),
            locals = LocalCapture.CAPTURE_FAILSOFT,
            require = 0,
            remap = false
    )
    private void injectEnd_putBulkDataD(PoseStack.Pose pose, BakedQuad quad, float[] brightnesses, float red, float green, float blue, int[] lights, int overlay, boolean readExistingColor, CallbackInfo ci, BufferBuilder bufferBuilder)
    {
        colorwheel$afterPutBulkData(bufferBuilder);
    }
}
