package dev.djefrey.colorwheel.fabric.mixin.mc;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder;
import net.irisshaders.iris.vertices.ExtendedDataHelper;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = ModelBlockRenderer.class, priority = 300)
public class ModelBlockRendererMixin
{
    @WrapMethod(method = "tesselateBlock")
    private void injectBeginEndBlock(BlockAndTintGetter level, BakedModel model, BlockState state, BlockPos pos, PoseStack poseStack, VertexConsumer consumer, boolean checkSides, RandomSource random, long seed, int packedOverlay, Operation<Void> original)
    {
        if (consumer instanceof BlockSensitiveBufferBuilder blockBuilder && WorldRenderingSettings.INSTANCE.getBlockStateIds() != null)
        {
            short id = (short) WorldRenderingSettings.INSTANCE.getBlockStateIds().getOrDefault(state, -1);
            blockBuilder.beginBlock(id, (byte) ExtendedDataHelper.BLOCK_RENDER_TYPE,  (byte) state.getLightEmission(), pos.getX(), pos.getY(), pos.getZ());
        }

        try
        {
            original.call(level, model, state, pos, poseStack, consumer, checkSides, random, seed, packedOverlay);
        }
        finally
        {
            if (consumer instanceof BlockSensitiveBufferBuilder blockBuilder)
            {
                blockBuilder.endBlock();
            }
        }
    }
}
