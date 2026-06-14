package dev.djefrey.colorwheel.mixin.mc;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.djefrey.colorwheel.Colorwheel;
import dev.djefrey.colorwheel.ColorwheelBufferBuilder;
import dev.engine_room.flywheel.lib.vertex.FlywheelVertexFormats;
import net.irisshaders.iris.vertices.IrisVertexFormats;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// After Iris
@Mixin(value = BufferBuilder.class, priority = 2000)
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

    @Shadow(aliases = { "extending", "iris$extending" })
    private boolean extending;

    @Shadow(aliases = { "injectNormalAndUV1", "iris$injectNormalAndUV1" })
    private boolean injectNormalAndUV1;

    @ModifyVariable(method = "<init>",
                    at = @At(value = "FIELD",
                             target = "Lcom/mojang/blaze3d/vertex/VertexFormatElement;POSITION:Lcom/mojang/blaze3d/vertex/VertexFormatElement;",
                             ordinal = 0),
                    argsOnly = true)
    private VertexFormat forceFormatExtension(VertexFormat format)
    {
        if (Colorwheel.getSafeFlw().isColorwheelCurrentBackend() && format == FlywheelVertexFormats.BLOCK_VERTEX_FORMAT)
        {
            extending = true;
            injectNormalAndUV1 = true;
            return IrisVertexFormats.TERRAIN;
        }

        return format;
    }

    @Override
    public void clrwlBeginBlock(int block, byte renderType, byte lightEmission, boolean isTerrain, int posX, int posY, int posZ)
    {
        colorwheel$isFlwBuffer = true;
        colorwheel$lightEmission = lightEmission;
        colorwheel$isTerrain = isTerrain;

        ColorwheelBufferBuilder.super.clrwlBeginBlock(block, renderType, lightEmission, isTerrain, posX, posY, posZ);
    }

    @Inject(method = "addVertex(FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;",
            at = @At("RETURN"))
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
