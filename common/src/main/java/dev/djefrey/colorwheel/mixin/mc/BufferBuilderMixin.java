package dev.djefrey.colorwheel.mixin.mc;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import dev.djefrey.colorwheel.Colorwheel;
import dev.djefrey.colorwheel.ColorwheelBufferBuilder;
import net.irisshaders.iris.vertices.IrisVertexFormats;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.ByteBuffer;

@Mixin(value = BufferBuilder.class, priority = 2000) // After Iris
public abstract class BufferBuilderMixin implements ColorwheelBufferBuilder
{
    @Shadow
    private VertexFormat format;

    @Shadow
    private int nextElementByte;

    @Shadow
    private ByteBuffer buffer;

    @Unique
    private boolean colorwheel$isFlwBuffer = false;

    @Unique
    private boolean colorwheel$isTerrain = false;

    @Override
    public void clrwlBeginBlock(short block, short renderType, boolean isTerrain, int posX, int posY, int posZ)
    {
        colorwheel$isFlwBuffer = true;
        colorwheel$isTerrain = isTerrain;
        beginBlock(block, renderType, posX, posY, posZ);
    }

    @Inject(method = "endVertex",
            at = @At("HEAD"))
    private void injectEmission(CallbackInfo ci)
    {
        if (colorwheel$isFlwBuffer && format == IrisVertexFormats.TERRAIN)
        {
            buffer.put(nextElementByte - 1, colorwheel$isTerrain ? (byte) 0 : (byte) -1);
        }
    }
}
