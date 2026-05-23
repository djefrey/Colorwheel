package dev.djefrey.colorwheel.forge.mixin.mc;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.djefrey.colorwheel.ColorwheelBufferBuilder;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder;
import net.irisshaders.iris.vertices.ExtendedDataHelper;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = BlockRenderDispatcher.class, priority = 300)
public class BlockRenderDispatcherMixin
{
    @WrapMethod(method = "renderLiquid")
    private void injectBeginEndBlock(BlockPos pos, BlockAndTintGetter level, VertexConsumer consumer, BlockState blockState, FluidState fluidState, Operation<Void> original)
    {
        if (WorldRenderingSettings.INSTANCE.getBlockStateIds() != null)
        {
            short id = (short) WorldRenderingSettings.INSTANCE.getBlockStateIds().getOrDefault(fluidState.createLegacyBlock(), -1);

            if (consumer instanceof ColorwheelBufferBuilder clrwlBlockBuilder)
            {
                clrwlBlockBuilder.clrwlBeginBlock(id, ExtendedDataHelper.FLUID_RENDER_TYPE, (byte) blockState.getLightEmission(), true, pos.getX(), pos.getY(), pos.getZ());
            }
            else if (consumer instanceof BlockSensitiveBufferBuilder blockBuilder)
            {
                blockBuilder.beginBlock(id, ExtendedDataHelper.FLUID_RENDER_TYPE, pos.getX(), pos.getY(), pos.getZ());
            }
        }

        try
        {
            original.call(pos, level, consumer, blockState, fluidState);
        }
        finally
        {
            if (consumer instanceof ColorwheelBufferBuilder clrwlBlockBuilder)
            {
                clrwlBlockBuilder.clrwlEndBlock();
            }
            else if (consumer instanceof BlockSensitiveBufferBuilder blockBuilder)
            {
                blockBuilder.endBlock();
            }
        }
    }
}
