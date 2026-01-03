package dev.djefrey.colorwheel;

import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder;

public interface ColorwheelBufferBuilder extends BlockSensitiveBufferBuilder
{
    default void clrwlBeginBlock(int block, byte renderType, byte lightEmission, boolean isTerrain, int posX, int posY, int posZ)
    {
        beginBlock(block, renderType, lightEmission, posX, posY, posZ);
    }

    default void clrwlEndBlock()
    {
        endBlock();
    }
}
