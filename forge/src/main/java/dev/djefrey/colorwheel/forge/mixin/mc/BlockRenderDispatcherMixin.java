package dev.djefrey.colorwheel.forge.mixin.mc;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockRenderDispatcher.class)
public class BlockRenderDispatcherMixin
{
    @Inject(method = "renderLiquid",
            at = @At("HEAD"))
    private void injectBeginBlock(BlockPos pos, BlockAndTintGetter level, VertexConsumer consumer, BlockState blockState, FluidState fluidState, CallbackInfo ci)
    {
        if (consumer instanceof BlockSensitiveBufferBuilder blockBuilder && WorldRenderingSettings.INSTANCE.getBlockStateIds() != null)
        {
            blockBuilder.beginBlock((short) WorldRenderingSettings.INSTANCE.getBlockStateIds().getInt(blockState),
                                    (byte) 0,
                                    pos.getX(), pos.getY(), pos.getZ());
        }
    }

    @Inject(method = "renderLiquid",
            at = @At("RETURN"))
    private void injectEndBlock(BlockPos pos, BlockAndTintGetter level, VertexConsumer consumer, BlockState blockState, FluidState fluidState, CallbackInfo ci)
    {
        if (consumer instanceof BlockSensitiveBufferBuilder blockBuilder)
        {
            blockBuilder.endBlock();
        }
    }
}
