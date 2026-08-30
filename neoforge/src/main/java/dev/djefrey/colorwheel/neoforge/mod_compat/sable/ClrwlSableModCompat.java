package dev.djefrey.colorwheel.neoforge.mod_compat.sable;

import com.google.common.collect.ImmutableList;
import dev.djefrey.colorwheel.engine.ClrwlEngine;
import dev.djefrey.colorwheel.engine.ClrwlInstanceVisual;
import dev.djefrey.colorwheel.engine.embed.EmbeddedEnvironment;
import dev.djefrey.colorwheel.mod_compat.ClrwlModCompat;
import dev.engine_room.flywheel.backend.engine.LightStorage;
import dev.ryanhcode.sable.neoforge.compatibility.flywheel.SableFlywheelLightStorage;
import net.irisshaders.iris.helpers.StringPair;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.Nullable;

public class ClrwlSableModCompat implements ClrwlModCompat
{
    private static final ImmutableList<StringPair> DEFINES = ImmutableList.of(
            new StringPair("_CLRWL_HAS_SABLE", "")
    );

    @Override
    public ImmutableList<StringPair> getShaderDefines()
    {
        return DEFINES;
    }

    @Override
    public LightStorage makeLightStorage(LevelAccessor level)
    {
        return new SableFlywheelLightStorage(level);
    }

    @Override
    public EmbeddedEnvironment makeEmbeddedEnvironment(ClrwlEngine engine, ClrwlInstanceVisual visual, Vec3i renderOrigin, @Nullable EmbeddedEnvironment parent)
    {
        return new SableEmbeddedEnvironment(engine, visual, renderOrigin, parent);
    }

    @Override
    public int getEmbeddedEnvironmentMatricesByteSize()
    {
        return SableEmbeddedEnvironment.MATRIX_SIZE_BYTES;
    }
}
