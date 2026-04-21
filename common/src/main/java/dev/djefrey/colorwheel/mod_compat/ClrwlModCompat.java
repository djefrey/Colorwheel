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

public interface ClrwlModCompat
{
    ImmutableList<StringPair> getShaderDefines();
    LightStorage makeLightStorage(LevelAccessor level);
    EmbeddedEnvironment makeEmbeddedEnvironment(ClrwlEngine engine, ClrwlInstanceVisual visual, Vec3i renderOrigin, @Nullable EmbeddedEnvironment parent);
    int getEmbeddedEnvironmentMatricesByteSize();
}
