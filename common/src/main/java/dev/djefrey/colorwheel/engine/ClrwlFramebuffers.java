package dev.djefrey.colorwheel.engine;

import com.google.common.collect.ImmutableList;
import dev.djefrey.colorwheel.Colorwheel;
import dev.djefrey.colorwheel.accessors.iris.BlendModeOverrideAccessor;
import dev.djefrey.colorwheel.compile.ClrwlOitPrograms;
import dev.djefrey.colorwheel.shaderpack.*;
import dev.djefrey.colorwheel.accessors.iris.IrisRenderingPipelineAccessor;
import dev.djefrey.colorwheel.accessors.iris.ProgramSetAccessor;
import dev.djefrey.colorwheel.accessors.iris.ShaderPackAccessor;
import net.irisshaders.iris.gl.blending.BufferBlendInformation;
import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.shaderpack.ShaderPack;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shaderpack.properties.PackDirectives;
import net.irisshaders.iris.shaderpack.properties.ProgramDirectives;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ClrwlFramebuffers
{
    private final IrisRenderingPipeline irisPipeline;
    private final ProgramSet programSet;

    private final Map<ClrwlProgramId, GlFramebuffer> framebuffers = new HashMap<>();

    @Nullable
    private ClrwlOitFramebuffers gbuffersOitFramebuffer;
    @Nullable
    private ClrwlOitFramebuffers shadowOitFramebuffer;

    private final Map<ClrwlProgramId, List<BufferBlendInformation>> bufferBlendOverrides = new HashMap<>();

    public ClrwlFramebuffers(IrisRenderingPipeline irisPipeline, ProgramSet programSet)
    {
        this.irisPipeline = irisPipeline;
        this.programSet = programSet;
    }

    @Nullable
    public GlFramebuffer getFramebuffer(ClrwlProgramId program)
    {
        var pipelineAccessor = ((IrisRenderingPipelineAccessor) irisPipeline);
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
    public ClrwlOitFramebuffers getOitFramebuffers(ClrwlProgramGroup programGroup, ClrwlOitPrograms oitPrograms, ProgramSet programSet, ProgramDirectives directives)
    {
        switch (programGroup)
        {
            case GBUFFERS ->
            {
                if (gbuffersOitFramebuffer == null)
                {
                    gbuffersOitFramebuffer = new ClrwlOitFramebuffers(programGroup, oitPrograms, irisPipeline, programSet, directives);
                }

                return gbuffersOitFramebuffer;
            }
            case SHADOW ->
            {
                if (shadowOitFramebuffer == null)
                {
                    shadowOitFramebuffer = new ClrwlOitFramebuffers(programGroup, oitPrograms, irisPipeline, programSet, directives);
                }

                return shadowOitFramebuffer;
            }
        }

        throw new RuntimeException("Unknown program group: " + programGroup);
    }

    public Optional<ClrwlBlendModeOverride> getBlendModeOverride(ClrwlProgramId programId)
    {
        var programSetAccessor = ((ProgramSetAccessor) programSet);

        if (!programSetAccessor.colorwheel$isFallbackMode())
        {
            var directives = programSetAccessor.colorwheel$getClrwlDirectives();
            var realProgramId = programSetAccessor.colorwheel$getRealClrwlProgram(programId);

            return realProgramId.flatMap(directives::getBlendModeOverride).or(programId::defaultBlendOverride);
        }
        else
        {
            var maybeSrc = programSetAccessor.colorwheel$getClrwlProgramSource(programId);

            return maybeSrc.flatMap(src -> src.getDirectives().getBlendModeOverride()
                                    .map(bm -> ((BlendModeOverrideAccessor) bm).colorwheel$convert()))
                            .or(programId::defaultBlendOverride);
        }
    }

    public List<BufferBlendInformation> getBufferBlendModeOverrides(ClrwlProgramId programId)
    {
        var programSetAccessor = ((ProgramSetAccessor) programSet);

        if (!programSetAccessor.colorwheel$isFallbackMode())
        {
            var realProgram = programSetAccessor.colorwheel$getRealClrwlProgram(programId);

            if (realProgram.isEmpty())
            {
                return Collections.emptyList();
            }

            return bufferBlendOverrides.computeIfAbsent(realProgram.get(), (key) ->
            {
                var directives = programSetAccessor.colorwheel$getClrwlDirectives();
                var maybeSrc = programSetAccessor.colorwheel$getClrwlProgramSource(key);

                return maybeSrc
                        .map(src -> computeBufferBlendOff(src, directives.getBufferBlendModeOverrides(realProgram.get())))
                        .orElse(Collections.emptyList());
            });
        }
        else
        {
            return bufferBlendOverrides.computeIfAbsent(programId, (key) ->
            {
                var maybeSrc = programSetAccessor.colorwheel$getClrwlProgramSource(programId);

                return maybeSrc
                        .map(src -> src.getDirectives().getBufferBlendOverrides())
                        .orElse(Collections.emptyList());
            });
        }
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

    public void delete()
    {
        var pipelineAccessor = ((IrisRenderingPipelineAccessor) irisPipeline);

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
