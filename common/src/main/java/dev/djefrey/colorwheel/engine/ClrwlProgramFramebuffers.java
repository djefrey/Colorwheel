package dev.djefrey.colorwheel.engine;

import com.google.common.collect.ImmutableList;
import dev.djefrey.colorwheel.shaderpack.ClrwlProgramGroup;
import dev.djefrey.colorwheel.shaderpack.ClrwlProgramId;
import dev.djefrey.colorwheel.shaderpack.ClrwlShaderProperties;
import dev.djefrey.colorwheel.accessors.IrisRenderingPipelineAccessor;
import dev.djefrey.colorwheel.accessors.ProgramSetAccessor;
import dev.djefrey.colorwheel.accessors.ShaderPackAccessor;
import dev.djefrey.colorwheel.compile.oit.ClrwlOitPrograms;
import net.irisshaders.iris.gl.blending.BlendModeOverride;
import net.irisshaders.iris.gl.blending.BufferBlendInformation;
import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.shaderpack.ShaderPack;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shaderpack.properties.ProgramDirectives;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ClrwlProgramFramebuffers
{
    private final Map<ClrwlProgramId, GlFramebuffer> framebuffers = new HashMap<>();

    @Nullable
    private ClrwlOitFramebuffers gbuffersOitFramebuffer;

    @Nullable
    private ClrwlOitFramebuffers shadowOitFramebuffer;

    private final Map<ClrwlProgramId, List<BufferBlendInformation>> bufferBlendOverrides = new HashMap<>();

    @Nullable
    public GlFramebuffer getFramebuffer(ClrwlProgramId program, IrisRenderingPipeline pipeline, ProgramSet programSet)
    {
        var pipelineAccessor = ((IrisRenderingPipelineAccessor) pipeline);
        var programSetAccessor = ((ProgramSetAccessor) programSet);

        // Reset gbuffers framebuffers when resizing window
        if (pipelineAccessor.colorwheel$consumeFramebufferChanged())
        {
            for (var entry : framebuffers.entrySet())
            {
                if (entry.getKey().group() == ClrwlProgramGroup.GBUFFERS)
                {
                    pipelineAccessor.colorwheel$destroyGbuffersFramebuffer(entry.getValue());
                }
            }

            for (var key : ClrwlProgramId.gbuffers())
            {
                framebuffers.remove(key);
            }
        }

        return framebuffers.computeIfAbsent(program, (key) ->
        {
            var maybeSrc = programSetAccessor.colorwheel$getClrwlProgramSource(key);

            if (maybeSrc.isEmpty())
            {
                return null;
            }

            var src = maybeSrc.get();

            switch (key.group())
            {
                case GBUFFERS ->
                {
                    return program.afterTranslucent()
                        ? pipelineAccessor.colorwheel$createTranslucentGbuffersFramebuffer(src)
                        : pipelineAccessor.colorwheel$createSolidGbuffersFramebuffer(src);
                }

                case SHADOW ->
                {
                    return pipelineAccessor.colorwheel$createShadowFramebuffer(src);
                }
            }

            throw new RuntimeException("Unknown program group: " + key.group().groupName());
        });
    }

    @Nullable
    public ClrwlOitFramebuffers getOitFramebuffers(ClrwlProgramGroup programGroup, ClrwlOitPrograms oitPrograms, IrisRenderingPipeline pipeline, ClrwlShaderProperties properties, ProgramDirectives directives)
    {
        switch (programGroup)
        {
            case GBUFFERS ->
            {
                if (gbuffersOitFramebuffer == null)
                {
                    gbuffersOitFramebuffer = new ClrwlOitFramebuffers(programGroup, oitPrograms, pipeline, properties, directives);
                }

                return gbuffersOitFramebuffer;
            }
            case SHADOW ->
            {
                if (shadowOitFramebuffer == null)
                {
                    shadowOitFramebuffer = new ClrwlOitFramebuffers(programGroup, oitPrograms, pipeline, properties, directives);
                }

                return shadowOitFramebuffer;
            }
        }

        throw new RuntimeException("Unknown program group: " + programGroup);
    }

    public Optional<ClrwlBlendModeOverride> getBlendModeOverride(ClrwlProgramId programId, ShaderPack pack, ProgramSet programSet)
    {
        var properties = ((ShaderPackAccessor) pack).colorwheel$getProperties();
        var realProgramId = ((ProgramSetAccessor) programSet).colorwheel$getRealClrwlProgram(programId);

        return realProgramId.flatMap(properties::getBlendModeOverride).or(programId::defaultBlendOverride);
    }

    public List<BufferBlendInformation> getBufferBlendModeOverrides(ClrwlProgramId programId, ShaderPack pack, ProgramSet programSet)
    {
        var programSetAccessor = ((ProgramSetAccessor) programSet);
        var realProgram = programSetAccessor.colorwheel$getRealClrwlProgram(programId);

        if (realProgram.isEmpty())
        {
            return Collections.emptyList();
        }

        return bufferBlendOverrides.computeIfAbsent(realProgram.get(), (key) ->
        {
            var properties = ((ShaderPackAccessor) pack).colorwheel$getProperties();
            var maybeSrc = programSetAccessor.colorwheel$getClrwlProgramSource(key);

            return maybeSrc
                    .map(src -> computeBufferBlendOff(src, properties.getBufferBlendModeOverrides(realProgram.get())))
                    .orElse(Collections.emptyList());
        });
    }

    private List<BufferBlendInformation> computeBufferBlendOff(ProgramSource source, List<BufferBlendInformation> blendOverrides)
    {
        if (blendOverrides.isEmpty())
        {
            return Collections.emptyList();
        }

        var drawBuffers = source.getDirectives().getDrawBuffers();
        var list = new ArrayList<BufferBlendInformation>();

        for (int i = 0; i < drawBuffers.length; i++)
        {
            int buf = drawBuffers[i];
            BlendModeOverride override = null;

            for (var entry : blendOverrides)
            {
                if (entry.index() == buf)
                {
                    list.add(new BufferBlendInformation(i, entry.blendMode()));
                }
            }
        }

        return ImmutableList.copyOf(list);
    }

    public void delete(IrisRenderingPipeline pipeline)
    {
        var pipelineAccessor = ((IrisRenderingPipelineAccessor) pipeline);

        for (var entry : framebuffers.entrySet())
        {
            switch (entry.getKey().group())
            {
                case GBUFFERS -> pipelineAccessor.colorwheel$destroyGbuffersFramebuffer(entry.getValue());
                case SHADOW -> pipelineAccessor.colorwheel$destroyShadowFramebuffer(entry.getValue());
            }
        }

        if (gbuffersOitFramebuffer != null)
        {
            gbuffersOitFramebuffer.delete();
            gbuffersOitFramebuffer = null;
        }

        if (shadowOitFramebuffer != null)
        {
            shadowOitFramebuffer.delete();
            shadowOitFramebuffer = null;
        }

        framebuffers.clear();
        bufferBlendOverrides.clear();
    }
}
