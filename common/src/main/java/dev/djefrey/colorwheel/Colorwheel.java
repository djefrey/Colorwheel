package dev.djefrey.colorwheel;

import dev.djefrey.colorwheel.accessors.ProgramSetAccessor;
import dev.djefrey.colorwheel.compile.ClrwlPrograms;
import dev.djefrey.colorwheel.engine.ClrwlEngine;
import dev.djefrey.colorwheel.instancing.ClrwlInstancingDrawManager;
import dev.engine_room.flywheel.api.backend.Backend;
import dev.engine_room.flywheel.backend.compile.FlwPrograms;
import dev.engine_room.flywheel.backend.gl.GlCompat;
import dev.engine_room.flywheel.backend.glsl.ShaderSources;
import dev.engine_room.flywheel.lib.backend.SimpleBackend;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.shaderpack.ShaderPack;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public final class Colorwheel {
    public static final String MOD_ID = "colorwheel";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final Backend IRIS_INSTANCING = SimpleBackend.builder()
            .engineFactory(level -> new ClrwlEngine(level, new ClrwlInstancingDrawManager(ClrwlPrograms.get()), 256))
            .priority(450)
            .supported(() -> GlCompat.SUPPORTS_INSTANCING && isUsingCompatibleShaderPack())
            .register(rl("instancing"));

    public static void init()
    {

    }

    public static ResourceLocation rl(String path)
    {
        return new ResourceLocation(MOD_ID, path);
    }

    public static boolean isUsingCompatibleShaderPack()
    {
        Optional<ShaderPack> pack = Iris.getCurrentPack();

        if (pack.isEmpty())
        {
            return false;
        }

        ProgramSet programSet = pack.get().getProgramSet(Iris.getCurrentDimension());
        return ((ProgramSetAccessor) programSet).colorwheel$getFlwGbuffers().isPresent();

    }

    public static ClrwlShaderSources SOURCES;

    public static void reload(ResourceManager manager)
    {
        ClrwlPrograms.setInstance(null);

        SOURCES = new ClrwlShaderSources(manager);

        ClrwlPrograms.reload(SOURCES);
    }
}
