package dev.djefrey.colorwheel;

import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder;

public interface ColorwheelBufferBuilder extends BlockSensitiveBufferBuilder
{
    default void clrwlBeginBlock(short block, short renderType, boolean isTerrain, int posX, int posY, int posZ)
    {
        beginBlock(block, renderType, posX, posY, posZ);
    }

    default void clrwlEndBlock()
    {
        endBlock();
    }
}
