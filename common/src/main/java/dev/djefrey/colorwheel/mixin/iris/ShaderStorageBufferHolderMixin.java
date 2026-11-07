package dev.djefrey.colorwheel.mixin.iris;

import dev.djefrey.colorwheel.accessors.iris.ShaderStorageBufferAccessor;
import dev.djefrey.colorwheel.accessors.iris.ShaderStorageBufferHolderAccessor;
import net.irisshaders.iris.gl.buffer.ShaderStorageBuffer;
import net.irisshaders.iris.gl.buffer.ShaderStorageBufferHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ShaderStorageBufferHolder.class)
public class ShaderStorageBufferHolderMixin implements ShaderStorageBufferHolderAccessor
{
    @Shadow
    private boolean destroyed;

    @Shadow
    private ShaderStorageBuffer[] buffers;

    public void colorwheel$setupBuffersWithIndexOffset(int offset)
    {
        if (destroyed)
        {
            throw new IllegalStateException("Tried to use destroyed buffer objects");
        }

        for (ShaderStorageBuffer buffer : buffers)
        {
            if (buffer instanceof ShaderStorageBufferAccessor accessor)
            {
                accessor.colorwheel$bindWithIndexOffset(offset);
            }
        }
    }
}
