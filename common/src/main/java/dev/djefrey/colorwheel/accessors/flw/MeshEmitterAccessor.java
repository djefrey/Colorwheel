package dev.djefrey.colorwheel.accessors.flw;

import com.mojang.blaze3d.vertex.BufferBuilder;
import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder;

public interface MeshEmitterAccessor extends BlockSensitiveBufferBuilder
{
    void colorwheel$beginTerrain();
    void colorwheel$endTerrain();
    void colorwheel$beginBlock(BufferBuilder bufferBuilder);
    void colorwheel$endBlock(BufferBuilder bufferBuilder);
}
