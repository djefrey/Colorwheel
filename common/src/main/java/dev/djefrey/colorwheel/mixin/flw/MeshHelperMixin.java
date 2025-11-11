package dev.djefrey.colorwheel.mixin.flw;

import com.mojang.blaze3d.vertex.BufferBuilder;
import dev.djefrey.colorwheel.engine.IrisTerrainVertexView;
import dev.engine_room.flywheel.lib.memory.MemoryBlock;
import dev.engine_room.flywheel.lib.model.SimpleQuadMesh;
import net.irisshaders.iris.vertices.IrisVertexFormats;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.ByteBuffer;

@Mixin(targets = "dev.engine_room.flywheel.lib.model.baked.MeshHelper")
public class MeshHelperMixin
{
    @Inject(method = "blockVerticesToMesh",
            at = @At("HEAD"),
            require = 0,
            cancellable = true)
    private static void injectCustomVertexData(BufferBuilder.RenderedBuffer data, String meshDescriptor, CallbackInfoReturnable<SimpleQuadMesh> cir)
    {
        if (data.drawState().format() == IrisVertexFormats.TERRAIN)
        {
            BufferBuilder.DrawState drawState = data.drawState();
            int vertexCount = drawState.vertexCount();
            long srcStride = drawState.format().getVertexSize();

            IrisTerrainVertexView vertexView = new IrisTerrainVertexView();
            long dstStride = vertexView.stride();

            ByteBuffer src = data.vertexBuffer();
            MemoryBlock dst = MemoryBlock.mallocTracked((long) vertexCount * dstStride);
            long srcPtr = MemoryUtil.memAddress(src);
            long dstPtr = dst.ptr();

            MemoryUtil.memCopy(srcPtr, dstPtr, srcStride * vertexCount);

            vertexView.ptr(dstPtr);
            vertexView.vertexCount(vertexCount);
            vertexView.nativeMemoryOwner(dst);

            cir.setReturnValue(new SimpleQuadMesh(vertexView, meshDescriptor));
        }
    }
}
