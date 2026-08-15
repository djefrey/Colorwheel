package dev.djefrey.colorwheel.compile.core;

import dev.djefrey.colorwheel.compile.ClrwlProgramSources;
import dev.engine_room.flywheel.backend.glsl.ShaderSources;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;

public class ClrwlShaderSources
{
    private final ShaderSources flwSources;
    private final ProgramSet programSet;
    private final ClrwlProgramSources clrwlSources;

    public ClrwlShaderSources(ShaderSources flwSources, IrisRenderingPipeline irisPipeline, ProgramSet programSet)
    {
        this.flwSources = flwSources;
        this.programSet = programSet;
        this.clrwlSources = new ClrwlProgramSources(irisPipeline, programSet);
    }

    public ShaderSources flwSources()
    {
        return flwSources;
    }

    public ProgramSet programSet()
    {
        return programSet;
    }

    public ClrwlProgramSources clrwlSources()
    {
        return clrwlSources;
    }
}
