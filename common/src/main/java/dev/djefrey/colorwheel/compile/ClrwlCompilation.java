package dev.djefrey.colorwheel.compile;

import dev.djefrey.colorwheel.ClrwlShaderSources;
import dev.engine_room.flywheel.backend.glsl.GlslVersion;
import dev.engine_room.flywheel.backend.glsl.ShaderSources;
import dev.engine_room.flywheel.backend.glsl.SourceComponent;
import net.irisshaders.iris.helpers.StringPair;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;

import java.util.ArrayList;
import java.util.List;

public class ClrwlCompilation
{
    public final StringBuilder sources = new StringBuilder();
    public final List<StringPair> defines = new ArrayList<>();
    public final List<String> extensions = new ArrayList<>();

    private final IrisRenderingPipeline pipeline;
    private final ProgramSource irisSources;
    private final ClrwlShaderSources sourceLoader;

    public ClrwlCompilation(IrisRenderingPipeline pipeline, ProgramSource irisSources, ClrwlShaderSources sourceLoader)
    {
        this.pipeline = pipeline;
        this.irisSources = irisSources;
        this.sourceLoader = sourceLoader;
    }

    public void version(GlslVersion version)
    {
        sources.append("#version ")
                .append(version.version)
                .append('\n');
    }

    public void enableExtension(String ext)
    {
        sources.append("#extension ")
                .append(ext)
                .append(" : enable\n");
    }

    public void requireExtension(String ext)
    {
        sources.append("#extension ")
                .append(ext)
                .append(" : require\n");
    }

    public void define(StringPair pair)
    {
        defines.add(pair);
        sources.append("#define ")
                .append(pair.key())
                .append(' ')
                .append(pair.value())
                .append('\n');
    }

    public void define(String key, String value)
    {
        define(new StringPair(key, value));
    }

    public void define(String key)
    {
        define(new StringPair(key, ""));
    }

    public void appendComponent(SourceComponent component)
    {
        sources.append("\n// ----- ")
                .append(component.name())
                .append(" -----\n");

        sources.append(component.source());
    }

    public String getShaderCode()
    {
        return sources.toString();
    }

    public IrisRenderingPipeline getIrisPipeline()
    {
        return pipeline;
    }

    public ProgramSource getIrisSources()
    {
        return irisSources;
    }

    public ClrwlShaderSources getLoader()
    {
        return sourceLoader;
    }
}
