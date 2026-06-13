package dev.djefrey.colorwheel.mixin.flw;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.djefrey.colorwheel.Colorwheel;
import dev.djefrey.colorwheel.ColorwheelBufferBuilder;
import dev.djefrey.colorwheel.accessors.flw.MeshEmitterAccessor;
import dev.engine_room.flywheel.lib.model.baked.MeshEmitter;
import dev.engine_room.flywheel.lib.vertex.FlywheelVertexFormats;
import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder;
import net.irisshaders.iris.vertices.IrisVertexFormats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MeshEmitter.class)
public abstract class MeshEmitterMixin implements MeshEmitterAccessor
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

    @Redirect(method = "getBuffer(Ldev/engine_room/flywheel/api/material/Material;)Lcom/mojang/blaze3d/vertex/BufferBuilder;",
                    at = @At(value = "FIELD",
                             target = "Ldev/engine_room/flywheel/lib/vertex/FlywheelVertexFormats;BLOCK_VERTEX_FORMAT:Lcom/mojang/blaze3d/vertex/VertexFormat;"))
    private VertexFormat changeVertexFormat()
    {
        if (Colorwheel.getSafeFlw().isColorwheelCurrentBackend())
        {
            return IrisVertexFormats.TERRAIN;
        }
        else
        {
            return FlywheelVertexFormats.BLOCK_VERTEX_FORMAT;
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
    }

    @Override
    public void colorwheel$beginTerrain()
    {
        colorwheel$isTerrain = true;
    }

    @Override
    public void colorwheel$endTerrain()
    {
        colorwheel$isTerrain = false;
    }

    @Override
    public void colorwheel$beginBlock(BufferBuilder bufferBuilder)
    {
        if (bufferBuilder instanceof ColorwheelBufferBuilder clrwlBuilder)
        {
            clrwlBuilder.clrwlBeginBlock(colorwheel$currentBlock, colorwheel$currentRenderType, colorwheel$currentBlockEmission, colorwheel$isTerrain, colorwheel$currentLocalPosX, colorwheel$currentLocalPosY, colorwheel$currentLocalPosZ);
        }
        else if (bufferBuilder instanceof BlockSensitiveBufferBuilder blockBuilder)
        {
            blockBuilder.beginBlock(colorwheel$currentBlock, colorwheel$currentRenderType, colorwheel$currentBlockEmission, colorwheel$currentLocalPosX, colorwheel$currentLocalPosY, colorwheel$currentLocalPosZ);
        }
    }

    @Override
    public void colorwheel$endBlock(BufferBuilder bufferBuilder)
    {
        if (bufferBuilder instanceof ColorwheelBufferBuilder clrwlBuilder)
        {
            clrwlBuilder.endBlock();
        }
        else if (bufferBuilder instanceof BlockSensitiveBufferBuilder blockBuilder)
        {
            blockBuilder.endBlock();
        }
    }
}
