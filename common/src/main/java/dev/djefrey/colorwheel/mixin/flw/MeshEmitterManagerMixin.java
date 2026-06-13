package dev.djefrey.colorwheel.mixin.flw;

import dev.djefrey.colorwheel.accessors.flw.MeshEmitterAccessor;
import dev.djefrey.colorwheel.accessors.flw.MeshEmitterManagerAccessor;
import dev.engine_room.flywheel.lib.model.baked.MeshEmitter;
import dev.engine_room.flywheel.lib.model.baked.MeshEmitterManager;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.function.Consumer;

@Mixin(MeshEmitterManager.class)
public abstract class MeshEmitterManagerMixin implements MeshEmitterManagerAccessor, BlockSensitiveBufferBuilder
{
    @Shadow
    @Final
    private Reference2ReferenceMap<ChunkSectionLayer, MeshEmitter> emitterMap;

    @Override
    public void colorwheel$beginTerrain()
    {
        colorwheel$executeOnEmitters(MeshEmitterAccessor::colorwheel$beginTerrain);
    }

    @Override
    public void colorwheel$endTerrain()
    {
        colorwheel$executeOnEmitters(MeshEmitterAccessor::colorwheel$endTerrain);
    }

    @Override
    public void beginBlock(int block, byte renderType, byte blockEmission, int localPosX, int localPosY, int localPosZ)
    {
        colorwheel$executeOnEmitters((emitter) -> emitter.beginBlock(block, renderType, blockEmission, localPosX, localPosY, localPosZ));
    }

    @Override
    public void endBlock()
    {
        colorwheel$executeOnEmitters(BlockSensitiveBufferBuilder::endBlock);
    }

    @Unique
    private void colorwheel$executeOnEmitters(Consumer<MeshEmitterAccessor> consumer)
    {
        for (MeshEmitter emitter : emitterMap.values())
        {
            if (emitter instanceof MeshEmitterAccessor accessor)
            {
                consumer.accept(accessor);
            }
        }
    }
}
