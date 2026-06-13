package dev.djefrey.colorwheel.mixin.mc;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.ResourceHandle;
import dev.djefrey.colorwheel.Colorwheel;
import net.irisshaders.iris.MojLambdas;
import net.irisshaders.iris.NeoLambdas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// After Iris
@Mixin(value = LevelRenderer.class, priority = 2000)
public class LevelRendererMixin
{
    @Shadow
    @Nullable
    private ClientLevel level;

    @Inject(method = { MojLambdas.RENDER_MAIN_PASS, NeoLambdas.NEO_RENDER_MAIN_PASS },
            require = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;renderTranslucentFeatures()V"))
    public void colorwheel$injectRenderTranslucents(CallbackInfo ci, @Local(ordinal = 0, argsOnly = true) LevelRenderState levelRenderState, @Local(ordinal = 0, argsOnly = true) Matrix4fc modelViewMatrix)
    {
        if (Colorwheel.getSafeFlw().isColorwheelCurrentBackend())
        {
            Colorwheel.getSafeFlw().submitTranslucentRenderContext(level, levelRenderState.cameraRenderState, levelRenderState, modelViewMatrix, levelRenderState.cameraRenderState.projectionMatrix, Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
        }
    }
}
