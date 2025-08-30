package dev.djefrey.colorwheel.fabric.mixin.flw;

import com.mojang.blaze3d.vertex.BufferBuilder;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder;
import org.jetbrains.annotations.UnknownNullability;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "dev.engine_room.flywheel.lib.model.baked.UniversalMeshEmitter")
public abstract class UniversalMeshEmitterMixin implements BlockSensitiveBufferBuilder
{
    @Unique
    private short colorwheel$currentBlock = -1;
    @Unique
    private short colorwheel$currentRenderType = -1;
    @Unique
    private int colorwheel$currentLocalPosX;
    @Unique
    private int colorwheel$currentLocalPosY;
    @Unique
    private int colorwheel$currentLocalPosZ;

    @Shadow
    private @UnknownNullability BufferBuilder currentDelegate;

    @Inject(method = "prepareForGeometry",
            at = @At("TAIL"),
            remap = false)
    private void injectBeginBlock(RenderMaterial material, CallbackInfo ci)
    {
        if (this.currentDelegate instanceof BlockSensitiveBufferBuilder blockBuilder)
        {
            blockBuilder.beginBlock(colorwheel$currentBlock, colorwheel$currentRenderType, colorwheel$currentLocalPosX, colorwheel$currentLocalPosY, colorwheel$currentLocalPosZ);
        }
    }

    @Override
    public void beginBlock(short block, short renderType, int localPosX, int localPosY, int localPosZ)
    {
        this.colorwheel$currentBlock = block;
        this.colorwheel$currentRenderType = renderType;
        this.colorwheel$currentLocalPosX = localPosX;
        this.colorwheel$currentLocalPosY = localPosY;
        this.colorwheel$currentLocalPosZ = localPosZ;
    }

    @Override
    public void endBlock()
    {
        this.colorwheel$currentBlock = -1;
        this.colorwheel$currentRenderType = -1;
        this.colorwheel$currentLocalPosX = 0;
        this.colorwheel$currentLocalPosY = 0;
        this.colorwheel$currentLocalPosZ = 0;

        if (this.currentDelegate instanceof BlockSensitiveBufferBuilder blockBuilder)
        {
            blockBuilder.endBlock();
        }
    }
}
