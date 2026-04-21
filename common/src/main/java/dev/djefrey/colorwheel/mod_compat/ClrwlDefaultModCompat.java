package dev.djefrey.colorwheel.mod_compat;

import com.google.common.collect.ImmutableList;
import dev.djefrey.colorwheel.engine.ClrwlEngine;
import dev.djefrey.colorwheel.engine.ClrwlInstanceVisual;
import dev.djefrey.colorwheel.engine.embed.EmbeddedEnvironment;
import dev.engine_room.flywheel.backend.engine.LightStorage;
import net.irisshaders.iris.helpers.StringPair;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.Nullable;

public class ClrwlDefaultModCompat implements ClrwlModCompat
{
    @Override
    public ImmutableList<StringPair> getShaderDefines()
    {
        return ImmutableList.of();
    }

    @Override
    public LightStorage makeLightStorage(LevelAccessor level)
    {
        return new LightStorage(level);
    }

    @Override
    public EmbeddedEnvironment makeEmbeddedEnvironment(ClrwlEngine engine, ClrwlInstanceVisual visual, Vec3i renderOrigin, @Nullable EmbeddedEnvironment parent)
    {
        return new EmbeddedEnvironment(engine, visual, renderOrigin, parent);
    }

    @Override
    public int getEmbeddedEnvironmentMatricesByteSize()
    {
        return EmbeddedEnvironment.MATRIX_SIZE_BYTES;
    }
}
