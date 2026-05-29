package dev.djefrey.colorwheel.fabric.mixin.flw.v10000;

import com.mojang.blaze3d.vertex.BufferBuilder;
import dev.djefrey.colorwheel.ColorwheelBufferBuilder;
import dev.djefrey.colorwheel.accessors.flw10000.UniversalMeshEmitterAccessor;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.UnknownNullability;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "dev.engine_room.flywheel.lib.model.baked.UniversalMeshEmitter")
@Pseudo
public abstract class UniversalMeshEmitterMixin implements ColorwheelBufferBuilder, UniversalMeshEmitterAccessor
{
    @Unique
    private boolean colorwheel$isTerrain = false;

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

    @Shadow
    private @UnknownNullability BufferBuilder currentDelegate;

    @Shadow
    public abstract void prepare(RenderType defaultLayer);

    @Inject(method = "prepareForGeometry",
            at = @At("TAIL"),
            require = 0,
            remap = false)
    private void injectBeginBlock(RenderMaterial material, CallbackInfo ci)
    {
        if (this.currentDelegate instanceof ColorwheelBufferBuilder clrwlBuilder)
        {
            clrwlBuilder.clrwlBeginBlock(colorwheel$currentBlock, colorwheel$currentRenderType, colorwheel$lightEmission, colorwheel$isTerrain, colorwheel$currentLocalPosX, colorwheel$currentLocalPosY, colorwheel$currentLocalPosZ);
        }
        else if (this.currentDelegate instanceof BlockSensitiveBufferBuilder blockBuilder)
        {
            blockBuilder.beginBlock(colorwheel$currentBlock, colorwheel$currentRenderType, colorwheel$currentLocalPosX, colorwheel$currentLocalPosY, colorwheel$currentLocalPosZ);
        }
    }

    @Override
    public void clrwlBeginBlock(short block, short renderType, byte lightEmission, boolean isTerrain, int posX, int posY, int posZ)
    {
        beginBlock(block, renderType, posX, posY, posZ);
        this.colorwheel$lightEmission = lightEmission;
    }

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

    @Override
    public void endBlock()
    {
        this.colorwheel$currentBlock = -1;
        this.colorwheel$currentRenderType = -1;
        this.colorwheel$lightEmission = 0;
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

    @Inject(method = "clear",
            at = @At("TAIL"),
            require = 0,
            remap = false)
    private void injectEnd(CallbackInfo ci)
    {
        this.colorwheel$isTerrain = false;
    }

    public void colorwheel$prepareTerrain(RenderType renderType)
    {
        this.colorwheel$isTerrain = true;
        this.prepare(renderType);
    }
}
