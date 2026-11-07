package dev.djefrey.colorwheel.mixin.iris;

import dev.djefrey.colorwheel.accessors.iris.ShaderStorageBufferAccessor;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gl.buffer.ShaderStorageBuffer;
import org.lwjgl.opengl.GL43C;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ShaderStorageBuffer.class)
public class ShaderStorageBufferMixin implements ShaderStorageBufferAccessor
{
    @Shadow
    protected int id;

    @Shadow
    @Final
    protected int index;

    @Override
    public void colorwheel$bindWithIndexOffset(int offset)
    {
        IrisRenderSystem.bindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, offset + index, id);
    }
}
