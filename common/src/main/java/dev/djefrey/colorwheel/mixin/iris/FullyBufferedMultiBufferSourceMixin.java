package dev.djefrey.colorwheel.mixin.iris;

import dev.djefrey.colorwheel.accessors.iris.FullyBufferedMultiBufferSourceAccessor;
import net.irisshaders.batchedentityrendering.impl.BlendingStateHolder;
import net.irisshaders.batchedentityrendering.impl.FullyBufferedMultiBufferSource;
import net.irisshaders.batchedentityrendering.impl.TransparencyType;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FullyBufferedMultiBufferSource.class)
public abstract class FullyBufferedMultiBufferSourceMixin implements FullyBufferedMultiBufferSourceAccessor
{
    @Shadow
    public abstract void endBatchWithType(TransparencyType transparencyType);

    @Unique
    private boolean colorwheel$shouldSkipTranslucentTerrain = false;

    @Redirect(method = "endBatchWithType",
            at = @At(value = "INVOKE", target = "Lnet/irisshaders/batchedentityrendering/impl/BlendingStateHolder;getTransparencyType()Lnet/irisshaders/batchedentityrendering/impl/TransparencyType;"),
            remap = false)
    private TransparencyType test(BlendingStateHolder instance)
    {
        if (colorwheel$shouldSkipTranslucentTerrain)
        {
            if (instance instanceof RenderType type && type == RenderType.translucent())
            {
                return TransparencyType.OPAQUE;
            }
        }

        return instance.getTransparencyType();
    }

    @Unique
    public void colorwheel$endTranslucentNonTerrainBatch()
    {
        colorwheel$shouldSkipTranslucentTerrain = true;
        endBatchWithType(TransparencyType.GENERAL_TRANSPARENT);
        colorwheel$shouldSkipTranslucentTerrain = false;
    }
}
