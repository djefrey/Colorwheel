package dev.djefrey.colorwheel.compile;

import dev.djefrey.colorwheel.accessors.iris.ProgramSetAccessor;
import dev.djefrey.colorwheel.accessors.iris.ProgramSourceAccessor;
import dev.djefrey.colorwheel.compile.transform.ClrwlComputeTransformPatcher;
import dev.djefrey.colorwheel.compile.transform.ClrwlTransformOutput;
import dev.djefrey.colorwheel.compile.transform.ClrwlTransformPatcher;
import dev.djefrey.colorwheel.gl.ClrwlShaderType;
import dev.djefrey.colorwheel.shaderpack.ClrwlProgramId;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.shaderpack.programs.ComputeSource;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;

import java.util.*;

public class ClrwlProgramSources
{
    private final IrisRenderingPipeline irisPipeline;
    private final ProgramSet programSet;
    private final Map<GbuffersKey, PatchedGbuffersSources> gbuffersSources;
    private final Map<ComputeKey, PatchedComputeSource> computeSources;

    public ClrwlProgramSources(IrisRenderingPipeline irisPipeline, ProgramSet programSet)
    {
        this.irisPipeline = irisPipeline;
        this.programSet = programSet;
        this.gbuffersSources = new HashMap<>();
        this.computeSources = new HashMap<>();
    }

    public PatchedGbuffersSources getGbuffersSources(ClrwlProgramId programId, ClrwlPrograms.OitMode oit, int ssboOffset)
    {
        ProgramSetAccessor programAccessor = (ProgramSetAccessor) programSet;
        ClrwlProgramId realProgramId = programAccessor.colorwheel$getRealClrwlProgram(programId).orElseThrow();

        var isCrumbling = programId == ClrwlProgramId.GBUFFERS_DAMAGEDBLOCK;
        var customOutputs = oit == ClrwlPrograms.OitMode.DEPTH_RANGE || oit == ClrwlPrograms.OitMode.GENERATE_COEFFICIENTS;

        var key = new GbuffersKey(realProgramId, isCrumbling, customOutputs, ssboOffset);

        return gbuffersSources.computeIfAbsent(key, this::patchGbuffersSources);
    }

    public PatchedComputeSource getComputeSource(ComputeSource source, int ssboOffset)
    {
        var key = new ComputeKey(source.getSource().orElseThrow(), ssboOffset);
        return computeSources.computeIfAbsent(key, this::patchComputeSource);
    }

    private PatchedGbuffersSources patchGbuffersSources(GbuffersKey k)
    {
        ProgramSetAccessor programAccessor = (ProgramSetAccessor) programSet;
        ProgramSource sources = programAccessor.colorwheel$getClrwlProgramSource(k.realProgramId()).orElseThrow();
        EnumMap<ClrwlShaderType, List<String>> extensions = ((ProgramSourceAccessor) sources).colorwheel$getShaderExtensions();
        int[] drawBuffers = sources.getDirectives().getDrawBuffers();

        String vertexSource = ClrwlTransformPatcher.patchVertex(sources.getVertexSource().orElseThrow(), k.isCrumbling(), k.ssboOffset(), irisPipeline.getTextureMap());
        Optional<String> geometrySource = sources.getGeometrySource().map(s -> ClrwlTransformPatcher.patchGeometry(s, k.isCrumbling(), k.ssboOffset(), irisPipeline.getTextureMap()));
        ClrwlTransformOutput fragmentSource = ClrwlTransformPatcher.patchFragment(sources.getFragmentSource().orElseThrow(), k.isCrumbling(), k.customOutputs(), k.ssboOffset(), irisPipeline.getTextureMap());

        return new PatchedGbuffersSources(vertexSource, geometrySource, fragmentSource, extensions, drawBuffers);
    }

    private PatchedComputeSource patchComputeSource(ComputeKey k)
    {
        var patchedSrc = ClrwlComputeTransformPatcher.patchCompute(k.shader(), k.ssboOffset(), irisPipeline.getTextureMap());
        return new PatchedComputeSource(patchedSrc);
    }

    private record GbuffersKey(ClrwlProgramId realProgramId, boolean isCrumbling, boolean customOutputs, int ssboOffset)
    {
    }

    public record PatchedGbuffersSources(String vertex, Optional<String> geometry, ClrwlTransformOutput fragment,
                                         EnumMap<ClrwlShaderType, List<String>> extensions, int[] drawBuffers)
    {
    }

    public record ComputeKey(String shader, int ssboOffset)
    {
    }

    public record PatchedComputeSource(String shader)
    {
    }
}
