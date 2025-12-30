package dev.djefrey.colorwheel.mixin.flw;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.djefrey.colorwheel.ColorwheelBufferBuilder;
import dev.djefrey.colorwheel.accessors.MeshEmitterAccessor;
import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder;
import org.jetbrains.annotations.UnknownNullability;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Mixin(targets = "dev.engine_room.flywheel.lib.model.baked.MeshEmitter", remap = false)
public abstract class MeshEmitterMixin implements VertexConsumer, BlockSensitiveBufferBuilder, MeshEmitterAccessor
{
    @Unique
    private boolean colorwheel$isTerrain = false;

    @Unique
    private int colorwheel$currentBlock = -1;
    @Unique
    private byte colorwheel$currentRenderType = -1;
    @Unique
    private byte colorwheel$currentBlockEmission = -1;
    @Unique
    private int colorwheel$currentLocalPosX = 0;
    @Unique
    private int colorwheel$currentLocalPosY = 0;
    @Unique
    private int colorwheel$currentLocalPosZ = 0;

    @Shadow
    private @UnknownNullability BufferBuilder bufferBuilder;

    @Inject(method = "prepareForGeometry(Z)V",
            at = @At("TAIL"),
            require = 0,
            remap = false)
    private void injectBeginBlock(boolean shade, CallbackInfo ci)
    {
        if (!colorwheel$isTerrain)
        {
            return;
        }

        if (this.bufferBuilder instanceof ColorwheelBufferBuilder clrwlBuilder)
        {
            clrwlBuilder.clrwlBeginBlock(colorwheel$currentBlock, colorwheel$currentRenderType, colorwheel$currentBlockEmission, colorwheel$currentLocalPosX, colorwheel$currentLocalPosY, colorwheel$currentLocalPosZ);
        }
        else if (this.bufferBuilder instanceof BlockSensitiveBufferBuilder blockBuilder)
        {
            blockBuilder.beginBlock(colorwheel$currentBlock, colorwheel$currentRenderType, colorwheel$currentBlockEmission, colorwheel$currentLocalPosX, colorwheel$currentLocalPosY, colorwheel$currentLocalPosZ);
        }
    }

    @Override
    public void beginBlock(int block, byte renderType, byte blockEmission, int localPosX, int localPosY, int localPosZ)
    {
        this.colorwheel$currentBlock = block;
        this.colorwheel$currentRenderType = renderType;
        this.colorwheel$currentBlockEmission = blockEmission;
        this.colorwheel$currentLocalPosX = localPosX;
        this.colorwheel$currentLocalPosY = localPosY;
        this.colorwheel$currentLocalPosZ = localPosZ;
    }

    @Override
    public void endBlock()
    {
        this.colorwheel$currentBlock = -1;
        this.colorwheel$currentRenderType = -1;
        this.colorwheel$currentBlockEmission = -1;
        this.colorwheel$currentLocalPosX = 0;
        this.colorwheel$currentLocalPosY = 0;
        this.colorwheel$currentLocalPosZ = 0;

        if (!colorwheel$isTerrain)
        {
            return;
        }

        if (this.bufferBuilder instanceof ColorwheelBufferBuilder clrwlBuilder)
        {
            clrwlBuilder.endBlock();
        }
        else if (this.bufferBuilder instanceof BlockSensitiveBufferBuilder blockBuilder)
        {
            blockBuilder.endBlock();
        }
    }

    @Inject(method = "end",
            require = 0,
            at = @At("TAIL"))
    private void injectEnd(CallbackInfo ci)
    {
        this.colorwheel$isTerrain = false;
    }

    @Unique
    private Method colorwheel$emitterPrepare = null;

    public void colorwheel$prepareTerrain(Object resultConsumer)
    {
        this.colorwheel$isTerrain = true;

        // The issue here is that MeshEmitter and BakedModelBufferer are private classes that can't be referenced
        // This prevents usage of mixins, thus requiring it to get the method manually
        // If you're able to write this properly, I would appreciate a pull request

        try
        {
            if (colorwheel$emitterPrepare == null)
            {
                var consumerClazz = Class.forName("dev.engine_room.flywheel.lib.model.baked.BakedModelBufferer$ResultConsumer");
                var emitterClazz = Class.forName("dev.engine_room.flywheel.lib.model.baked.MeshEmitter");
                colorwheel$emitterPrepare = emitterClazz.getMethod("prepare", consumerClazz);
            }

            colorwheel$emitterPrepare.invoke(this, resultConsumer);
        }
        catch (ClassNotFoundException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }
}
