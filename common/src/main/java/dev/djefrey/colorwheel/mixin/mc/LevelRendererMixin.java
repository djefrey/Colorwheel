package dev.djefrey.colorwheel.mixin.mc;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.djefrey.colorwheel.Colorwheel;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, priority = 2000)
public class LevelRendererMixin
{
    @Shadow
    @Nullable
    private ClientLevel level;

    @Inject(method = "renderLevel",
            at = @At(value = "CONSTANT", args = "stringValue=string"),
            order = 2000) // After Iris
    public void colorwheel$injectRenderTranslucents(PoseStack poseStack, float tickDelta, long finishNanoTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci)
    {
        if (Colorwheel.getSafeFlw().isColorwheelCurrentBackend())
        {
            Colorwheel.getSafeFlw().submitTranslucentRenderContext(level, poseStack, camera, projectionMatrix, tickDelta);
        }
    }
}
