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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelBlockRenderer.class)
public class ModelBlockRendererMixin
{
    @Unique
    private boolean colorwheel$isFirstCall = true;

    // This is required as Sodium injects a Mixin that cancels the call

    @Inject(method = "tesselateBlock",
            at = @At("HEAD"),
            cancellable = true,
            order = 500)
    private void injectBeginEndBlock(BlockAndTintGetter level, BakedModel model, BlockState state, BlockPos pos, PoseStack poseStack, VertexConsumer consumer, boolean checkSides, RandomSource random, long seed, int packedOverlay, CallbackInfo ci)
    {
        if (colorwheel$isFirstCall)
        {
            if (consumer instanceof BlockSensitiveBufferBuilder blockBuilder && WorldRenderingSettings.INSTANCE.getBlockStateIds() != null)
            {
                    blockBuilder.beginBlock(WorldRenderingSettings.INSTANCE.getBlockStateIds().getInt(state),
                                            (byte) 0, (byte) state.getLightEmission(),
                                            pos.getX(), pos.getY(), pos.getZ());
            }

            colorwheel$isFirstCall = false;

            try
            {
                ((ModelBlockRenderer) (Object) this).tesselateBlock(level, model, state, pos, poseStack, consumer, checkSides, random, seed, packedOverlay);
            }
            finally
            {
                this.colorwheel$isFirstCall = true;

                if (consumer instanceof BlockSensitiveBufferBuilder blockBuilder)
                {
                    blockBuilder.endBlock();
                }

                ci.cancel();
            }
        }
    }
}
