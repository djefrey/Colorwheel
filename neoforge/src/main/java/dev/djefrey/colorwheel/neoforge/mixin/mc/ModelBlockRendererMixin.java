package dev.djefrey.colorwheel.neoforge.mixin.mc;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.djefrey.colorwheel.Colorwheel;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ModelBlockRenderer.class, priority = 300)
public class ModelBlockRendererMixin
{
    @Unique
    private boolean colorwheel$isFirstCall = true;

    // This is required as Sodium (Fabric) and Indigo (Forge) injects a Mixin that cancels the call,
    // which prevent cleanups done with @At("RETURN")
    //
    // The mixin priority is aggressive to ensure that this is called first,
    // the method is never truly cancelled so that's okay

    @Inject(method = "tesselateBlock(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;JILnet/neoforged/neoforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private void injectBeginEndBlock(BlockAndTintGetter level, BakedModel model, BlockState state, BlockPos pos, PoseStack poseStack, VertexConsumer consumer, boolean checkSides, RandomSource random, long seed, int packedOverlay, ModelData modelData, RenderType renderType, CallbackInfo ci)
    {
        if (colorwheel$isFirstCall && consumer instanceof BlockSensitiveBufferBuilder blockBuilder && WorldRenderingSettings.INSTANCE.getBlockStateIds() != null)
        {
            blockBuilder.beginBlock(WorldRenderingSettings.INSTANCE.getBlockStateIds().getInt(state),
                                    (byte) 0, (byte) state.getLightEmission(),
                                    pos.getX(), pos.getY(), pos.getZ());
            
            colorwheel$isFirstCall = false;

            try
            {
                ((ModelBlockRenderer) (Object) this).tesselateBlock(level, model, state, pos, poseStack, consumer, checkSides, random, seed, packedOverlay, modelData, renderType);
            }
            finally
            {
                this.colorwheel$isFirstCall = true;

                blockBuilder.endBlock();
                ci.cancel();
            }
        }
    }
}
