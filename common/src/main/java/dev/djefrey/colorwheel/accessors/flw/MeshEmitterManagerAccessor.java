package dev.djefrey.colorwheel.accessors.flw;

import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder;

public interface MeshEmitterManagerAccessor extends BlockSensitiveBufferBuilder
{
    void colorwheel$beginTerrain();
    void colorwheel$endTerrain();
}
