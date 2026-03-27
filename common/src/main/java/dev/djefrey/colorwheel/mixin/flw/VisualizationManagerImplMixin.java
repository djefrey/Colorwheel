package dev.djefrey.colorwheel.mixin.flw;

import dev.djefrey.colorwheel.Colorwheel;
import dev.engine_room.flywheel.impl.visualization.VisualizationManagerImpl;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VisualizationManagerImpl.class)
public class VisualizationManagerImplMixin
{
    // This mixin is used to handle mods messing with Iris rendering pipeline, such as Vista.
    // Vista injects a mixin into Iris PipelineManager::preparePipeline,
    // which returns a VanillaRenderingPipeline instead of the expected IrisRenderingPipeline under some conditions.
    // This is used to render the level in a separate pass for their cameras.
    //
    // This causes a crash with Colorwheel in the ClwlEngine init, as it expects an IrisRenderingPipeline.
    // This mixin ensures we won't try to render something if we're not in the required conditions
    // This could in theory cause issues if the other mod isn't careful, but at that point
    // the mod messes things up so much, there may not be much that can be done.

    @Unique
    private static boolean colorwheel$hasPipelineChanged = false;

    @Inject(method = "supportsVisualization",
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private static void injectIrisPipelineCheck(LevelAccessor level, CallbackInfoReturnable<Boolean> cir)
    {
        if (Colorwheel.getSafeFlw().isColorwheelCurrentBackend() && level instanceof ClientLevel clientLevel)
        {
            // preparePipeline can't be used, as it could create a new pipeline on a non-main thread
            WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();

            if (!(pipeline instanceof IrisRenderingPipeline))
            {
                if (!colorwheel$hasPipelineChanged)
                {
                    Colorwheel.LOGGER.error("Rendering pipeline has changed unexpectedly, a mod is messing with Iris internals !");
                    colorwheel$hasPipelineChanged = true;
                }

                cir.setReturnValue(false);
            }
        }
    }
}
