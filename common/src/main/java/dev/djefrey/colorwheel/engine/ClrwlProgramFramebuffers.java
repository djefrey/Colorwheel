package dev.djefrey.colorwheel.engine;

import com.google.common.collect.ImmutableList;
import dev.djefrey.colorwheel.ClrwlBlendModeOverride;
import dev.djefrey.colorwheel.ClrwlProgramGroup;
import dev.djefrey.colorwheel.ClrwlProgramId;
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
    public ClrwlOitFramebuffers getOitFramebuffers(boolean isShadow, ClrwlOitPrograms oitPrograms, IrisRenderingPipeline pipeline,  ProgramSet programSet)
    {
        if (!isShadow)
        {
            if (gbuffersOitFramebuffer == null)
            {
                gbuffersOitFramebuffer = new ClrwlOitFramebuffers(oitPrograms, pipeline, false, programSet.getPackDirectives());
            }

            return gbuffersOitFramebuffer;
        }
        else
        {
            if (shadowOitFramebuffer == null)
            {
                shadowOitFramebuffer = new ClrwlOitFramebuffers(oitPrograms, pipeline, true, programSet.getPackDirectives());
            }

            return shadowOitFramebuffer;
        }
    }

    public Optional<ClrwlBlendModeOverride> getBlendModeOverride(ClrwlProgramId programId, ShaderPack pack, ProgramSet programSet)
    {
        var properties = ((ShaderPackAccessor) pack).colorwheel$getProperties();
        var realProgramId = ((ProgramSetAccessor) programSet).colorwheel$getRealClrwlProgram(programId);

        return realProgramId.flatMap(properties::getBlendModeOverride);
    }

    public List<BufferBlendInformation> getBufferBlendModeOverrides(ClrwlProgramId programId, ShaderPack pack, ProgramSet programSet)
    {
        return bufferBlendOverrides.computeIfAbsent(programId, (key) ->
        {
            var properties = ((ShaderPackAccessor) pack).colorwheel$getProperties();
            var programSetAccessor = ((ProgramSetAccessor) programSet);
            var maybeSrc = programSetAccessor.colorwheel$getClrwlProgramSource(key);

            return maybeSrc
                    .map(src -> computeBufferBlendOff(src, properties.getBufferBlendModeOverrides(key)))
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
