package dev.djefrey.colorwheel.fabric.mixin.flw.v10006;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.djefrey.colorwheel.ColorwheelBufferBuilder;
import dev.engine_room.flywheel.lib.model.SimpleModel;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "dev.engine_room.flywheel.lib.model.baked.FabricMeshEmitterManager")
@Pseudo
public abstract class FabricMeshEmitterManagerMixin implements VertexConsumer, BlockSensitiveBufferBuilder
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
    private @Nullable BufferBuilder currentDelegate;

    @Inject(method = "prepareForBlock",
            at = @At("TAIL"),
            require = 0,
            remap = false)
    private void injectBeginBlock(CallbackInfo ci)
    {
        colorwheel$isTerrain = true;
    }

    @Inject(method = "end",
            at = @At("HEAD"),
            require = 0,
            remap = false
    )
    private void injectEndBlock(CallbackInfoReturnable<SimpleModel> cir)
    {
        this.colorwheel$isTerrain = false;
    }

    @Inject(method = "prepareForGeometry",
            at = @At("RETURN"),
            require = 0,
            remap = false
    )
    private void injectBeginBlock(RenderMaterial material, CallbackInfo ci)
    {
        if (currentDelegate instanceof ColorwheelBufferBuilder clrwlBuilder)
        {
            clrwlBuilder.clrwlBeginBlock(colorwheel$currentBlock, colorwheel$currentRenderType, colorwheel$currentBlockEmission, colorwheel$isTerrain, colorwheel$currentLocalPosX, colorwheel$currentLocalPosY, colorwheel$currentLocalPosZ);
        }
        else if (currentDelegate instanceof BlockSensitiveBufferBuilder blockBuilder)
        {
            blockBuilder.beginBlock(colorwheel$currentBlock, colorwheel$currentRenderType, colorwheel$currentBlockEmission, colorwheel$currentLocalPosX, colorwheel$currentLocalPosY, colorwheel$currentLocalPosZ);
        }
    }

    // Called by ModelBlockRendererMixin::injectBeginEndBlock
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

    // Called by ModelBlockRendererMixin::injectBeginEndBlock
    @Override
    public void endBlock()
    {
        this.colorwheel$currentBlock = -1;
        this.colorwheel$currentRenderType = -1;
        this.colorwheel$currentBlockEmission = -1;
        this.colorwheel$currentLocalPosX = 0;
        this.colorwheel$currentLocalPosY = 0;
        this.colorwheel$currentLocalPosZ = 0;

        if (this.currentDelegate instanceof ColorwheelBufferBuilder clrwlBuilder)
        {
            clrwlBuilder.endBlock();
        }
        else if (this.currentDelegate instanceof BlockSensitiveBufferBuilder blockBuilder)
        {
            blockBuilder.endBlock();
        }
    }
}
