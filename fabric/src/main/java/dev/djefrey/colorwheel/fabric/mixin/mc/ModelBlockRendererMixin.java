package dev.djefrey.colorwheel.fabric.mixin.mc;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelBlockRenderer.class)
public class ModelBlockRendererMixin
{
    @Inject(method = "tesselateBlock",
            at = @At("HEAD"))
    private void injectBeingBlock(BlockAndTintGetter level, BakedModel model, BlockState state, BlockPos pos, PoseStack poseStack, VertexConsumer consumer, boolean checkSides, RandomSource random, long seed, int packedOverlay, CallbackInfo ci)
    {
        if (consumer instanceof ColorwheelBufferBuilder blockBuilder && WorldRenderingSettings.INSTANCE.getBlockStateIds() != null)
        {
            blockBuilder.beginBlock((short) WorldRenderingSettings.INSTANCE.getBlockStateIds().getInt(state),
                                    (byte) 0,
                                    pos.getX(), pos.getY(), pos.getZ());
        }
    }

    @Inject(method = "tesselateBlock",
            at = @At("RETURN"),
            remap = false)
    private void injectEndBlock(BlockAndTintGetter level, BakedModel model, BlockState state, BlockPos pos, PoseStack poseStack, VertexConsumer consumer, boolean checkSides, RandomSource random, long seed, int packedOverlay, CallbackInfo ci)
    {
        if (consumer instanceof BlockSensitiveBufferBuilder blockBuilder)
        {
            blockBuilder.endBlock();
        }
    }
}
