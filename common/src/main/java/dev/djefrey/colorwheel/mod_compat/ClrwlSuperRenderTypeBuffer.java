// Derived from Ponder's DefaultSuperRenderTypeBuffer

package dev.djefrey.colorwheel.mod_compat;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.djefrey.colorwheel.accessors.iris.FullyBufferedMultiBufferSourceAccessor;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.irisshaders.batchedentityrendering.impl.FullyBufferedMultiBufferSource;
import net.irisshaders.batchedentityrendering.impl.TransparencyType;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.minecraft.client.renderer.RenderType;

public class ClrwlSuperRenderTypeBuffer implements SuperRenderTypeBuffer
{
    private static final ClrwlSuperRenderTypeBuffer INSTANCE = new ClrwlSuperRenderTypeBuffer();

    public static ClrwlSuperRenderTypeBuffer getInstance()
    {
        return INSTANCE;
    }

    protected FullyBufferedMultiBufferSource earlyBuffer;
    protected FullyBufferedMultiBufferSource defaultBuffer;
    protected FullyBufferedMultiBufferSource lateBuffer;

    public ClrwlSuperRenderTypeBuffer()
    {
        earlyBuffer = new FullyBufferedMultiBufferSource();
        defaultBuffer = new FullyBufferedMultiBufferSource();
        lateBuffer = new FullyBufferedMultiBufferSource();
    }

    @Override
    public VertexConsumer getEarlyBuffer(RenderType type)
    {
        return earlyBuffer.getBuffer(type);
    }

    @Override
    public VertexConsumer getBuffer(RenderType type)
    {
        return defaultBuffer.getBuffer(type);
    }

    @Override
    public VertexConsumer getLateBuffer(RenderType type)
    {
        return lateBuffer.getBuffer(type);
    }

    @Override
    public void draw()
    {
        earlyBuffer.endBatch();
        defaultBuffer.endBatch();
        lateBuffer.endBatch();
    }

    @Override
    public void draw(RenderType type)
    {
        earlyBuffer.endBatch(type);
        defaultBuffer.endBatch(type);
        lateBuffer.endBatch(type);
    }

    private void draw(TransparencyType type)
    {
        earlyBuffer.endBatchWithType(type);
        defaultBuffer.endBatchWithType(type);
        lateBuffer.endBatchWithType(type);
    }

    private void drawTranslucentNonTerrain()
    {
        ((FullyBufferedMultiBufferSourceAccessor) earlyBuffer).colorwheel$endTranslucentNonTerrainBatch();
        ((FullyBufferedMultiBufferSourceAccessor) defaultBuffer).colorwheel$endTranslucentNonTerrainBatch();
        ((FullyBufferedMultiBufferSourceAccessor) lateBuffer).colorwheel$endTranslucentNonTerrainBatch();
    }

    public void drawSolid()
    {
        draw(TransparencyType.OPAQUE);
        draw(TransparencyType.OPAQUE_DECAL);
        draw(TransparencyType.WATER_MASK);

        if (!WorldRenderingSettings.INSTANCE.shouldSeparateEntityDraws())
        {
            drawTranslucentNonTerrain();
        }
    }

    public void drawTranslucent()
    {
        // Draw remaining
        // This ensures that every buffer is rendered and cleaned
        draw();
    }
}
