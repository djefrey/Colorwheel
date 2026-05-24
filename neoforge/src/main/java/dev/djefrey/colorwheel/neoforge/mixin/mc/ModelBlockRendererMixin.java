package dev.djefrey.colorwheel.neoforge.mixin.mc;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.djefrey.colorwheel.ColorwheelBufferBuilder;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder;
import net.irisshaders.iris.vertices.ExtendedDataHelper;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = ModelBlockRenderer.class, priority = 300)
public class ModelBlockRendererMixin
{
    @WrapMethod(
        method = "tesselateBlock(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;JILnet/neoforged/neoforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)V",
        remap = false
    )
    private void injectBeginEndBlock(BlockAndTintGetter level, BakedModel model, BlockState state, BlockPos pos, PoseStack poseStack, VertexConsumer consumer, boolean checkSides, RandomSource random, long seed, int packedOverlay, ModelData modelData, RenderType renderType, Operation<Void> original)
    {
        if (WorldRenderingSettings.INSTANCE.getBlockStateIds() != null)
        {
            short id = (short) WorldRenderingSettings.INSTANCE.getBlockStateIds().getOrDefault(state, -1);

            if (consumer instanceof ColorwheelBufferBuilder clrwlBlockBuilder)
            {
                clrwlBlockBuilder.clrwlBeginBlock(id, (byte) ExtendedDataHelper.BLOCK_RENDER_TYPE, (byte) state.getLightEmission(), true, pos.getX(), pos.getY(), pos.getZ());
            }
            else if (consumer instanceof BlockSensitiveBufferBuilder blockBuilder)
            {
                blockBuilder.beginBlock(id, (byte) ExtendedDataHelper.BLOCK_RENDER_TYPE, (byte) state.getLightEmission(), pos.getX(), pos.getY(), pos.getZ());
            }
        }

        try
        {
            original.call(level, model, state, pos, poseStack, consumer, checkSides, random, seed, packedOverlay, modelData, renderType);
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
