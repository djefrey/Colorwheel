package dev.djefrey.colorwheel.mixin.flw;

import dev.djefrey.colorwheel.engine.ClrwlEngine;
import dev.engine_room.flywheel.api.visual.BlockEntityVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.impl.visualization.storage.BlockEntityStorage;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntityStorage.class)
public abstract class BlockEntityStorageMixin
{
    @Shadow
    protected abstract @Nullable BlockEntityVisual<?> createRaw(VisualizationContext visualizationContext, BlockEntity obj, float partialTick);

    @Inject(method = "createRaw(Ldev/engine_room/flywheel/api/visualization/VisualizationContext;Lnet/minecraft/world/level/block/entity/BlockEntity;F)Ldev/engine_room/flywheel/api/visual/BlockEntityVisual;",
            at = @At("HEAD"),
            cancellable = true)
    private void injectCreateRaw(VisualizationContext visualizationContext, BlockEntity obj, float partialTick, CallbackInfoReturnable<BlockEntityVisual<?>> cir)
    {
        if (visualizationContext instanceof ClrwlEngine.ClrwlMainVisualizationContext mainCtx)
        {
            var blockIds = WorldRenderingSettings.INSTANCE.getBlockStateIds();

            if (blockIds != null)
            {
                int id = blockIds.applyAsInt(obj.getBlockState());
                var res = createRaw(mainCtx.getBlockEntityVisualCtx(id), obj, partialTick);

                cir.setReturnValue(res);
            }
        }
    }
}
