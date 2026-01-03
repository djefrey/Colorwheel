package dev.djefrey.colorwheel.mixin.mc;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.djefrey.colorwheel.ColorwheelBufferBuilder;
import net.irisshaders.iris.vertices.IrisVertexFormats;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BufferBuilder.class)
public abstract class BufferBuilderMixin implements ColorwheelBufferBuilder
{
    @Shadow
    private long vertexPointer;
    @Shadow @Final
    private int[] offsetsByElement;

    @Shadow @Final
    private int initialElementsToFill;
    @Shadow
    private int elementsToFill;

    @Unique
    private boolean colorwheel$isFlwBuffer = false;

    @Unique
    private byte colorwheel$lightEmission = -1;

    @Unique
    private boolean colorwheel$isTerrain = false;

    @Override
    public void clrwlBeginBlock(int block, byte renderType, byte lightEmission, boolean isTerrain, int posX, int posY, int posZ)
    {
        colorwheel$isFlwBuffer = true;
        colorwheel$lightEmission = lightEmission;
        colorwheel$isTerrain = isTerrain;

        ColorwheelBufferBuilder.super.clrwlBeginBlock(block, renderType, lightEmission, isTerrain, posX, posY, posZ);
    }

    @Inject(method = "addVertex(FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;",
            at = @At("RETURN"),
            order = 2000) // After Iris
    private void injectEmission(float x, float y, float z, CallbackInfoReturnable<VertexConsumer> cir)
    {
        if (colorwheel$isFlwBuffer)
        {
            var wasRequested = (this.initialElementsToFill & IrisVertexFormats.MID_BLOCK_ELEMENT.mask()) != 0;
            var hasBeenFilled = (this.elementsToFill & IrisVertexFormats.MID_BLOCK_ELEMENT.mask()) == 0;

            if (wasRequested && hasBeenFilled)
            {
                long midBlockOffset = this.vertexPointer + (long) this.offsetsByElement[IrisVertexFormats.MID_BLOCK_ELEMENT.id()];

                MemoryUtil.memPutByte(midBlockOffset + 3, colorwheel$isTerrain ? colorwheel$lightEmission : (byte) -1);
            }
        }
    }
}
