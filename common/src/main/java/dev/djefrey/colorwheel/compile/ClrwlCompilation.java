package dev.djefrey.colorwheel.compile;

import dev.djefrey.colorwheel.shaderpack.ClrwlShaderProperties;
import dev.engine_room.flywheel.backend.glsl.GlslVersion;
import dev.engine_room.flywheel.backend.glsl.ShaderSources;
import dev.engine_room.flywheel.backend.glsl.SourceComponent;
import net.irisshaders.iris.helpers.StringPair;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shaderpack.properties.PackDirectives;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ClrwlCompilation
{
    public final StringBuilder sources = new StringBuilder();
    public final List<StringPair> defines = new ArrayList<>();
    public final List<String> extensions = new ArrayList<>();

    private final IrisRenderingPipeline pipeline;
    private final PackDirectives directives;
    private final ClrwlShaderProperties properties;
    private final ProgramSource irisSources;
    private final ShaderSources sourceLoader;

    private Map<Integer, String> shaderOutputs = Collections.emptyMap();

    public ClrwlCompilation(IrisRenderingPipeline pipeline, PackDirectives directives, ClrwlShaderProperties properties, ProgramSource irisSources, ShaderSources sourceLoader)
    {
        this.pipeline = pipeline;
        this.directives = directives;
        this.properties = properties;
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
        if (component == null)
        {
            return;
        }

        sources.append("\n// ----- ")
                .append(component.name())
                .append(" -----\n");

        sources.append(component.source());
    }

    public String getShaderCode()
    {
        return sources.toString();
    }

    @Nullable
    public IrisRenderingPipeline getIrisPipeline()
    {
        return pipeline;
    }

    @Nullable
    public PackDirectives getPackDirectives()
    {
        return directives;
    }

    @Nullable
    public ClrwlShaderProperties getProperties()
    {
        return properties;
    }

    @Nullable
    public ProgramSource getIrisSources()
    {
        return irisSources;
    }

    public ShaderSources getLoader()
    {
        return sourceLoader;
    }

    public Map<Integer, String> getShaderOutputs()
    {
        return shaderOutputs;
    }

    public void setShaderOutputs(Map<Integer, String> outputs)
    {
        this.shaderOutputs = outputs;
    }
}
