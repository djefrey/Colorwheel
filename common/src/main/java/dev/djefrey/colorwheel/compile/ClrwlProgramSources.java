package dev.djefrey.colorwheel.compile;

import dev.djefrey.colorwheel.ShaderType;
import dev.djefrey.colorwheel.accessors.iris.ProgramSetAccessor;
import dev.djefrey.colorwheel.accessors.iris.ProgramSourceAccessor;
import dev.djefrey.colorwheel.compile.transform.ClrwlTransformOutput;
import dev.djefrey.colorwheel.compile.transform.ClrwlTransformPatcher;
import dev.djefrey.colorwheel.shaderpack.ClrwlProgramId;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ClrwlProgramSources
{
    private final Map<Key, PatchedSources> sources;

    public ClrwlProgramSources()
    {
        this.sources = new HashMap<>();
    }

    public PatchedSources getSources(ClrwlProgramId programId, ClrwlPipelineCompiler.OitMode oit, ProgramSet programSet, IrisRenderingPipeline pipeline)
    {
        ProgramSetAccessor programAccessor = (ProgramSetAccessor) programSet;
        ClrwlProgramId realProgramId = programAccessor.colorwheel$getRealClrwlProgram(programId).orElseThrow();

        var isCrumbling = programId == ClrwlProgramId.GBUFFERS_DAMAGEDBLOCK;
        var customOutputs = oit == ClrwlPipelineCompiler.OitMode.DEPTH_RANGE || oit == ClrwlPipelineCompiler.OitMode.GENERATE_COEFFICIENTS;

        var key = new Key(realProgramId, isCrumbling, customOutputs);

        return sources.computeIfAbsent(key, (k) ->
        {
            ProgramSource sources = programAccessor.colorwheel$getClrwlProgramSource(k.realProgramId()).orElseThrow();
            Map<ShaderType, List<String>> extensions = ((ProgramSourceAccessor) sources).colorwheel$getShaderExtensions();
            int[] drawBuffers = sources.getDirectives().getDrawBuffers();

            String vertexSource = ClrwlTransformPatcher.patchVertex(sources.getVertexSource().orElseThrow(), k.isCrumbling(), sources.getDirectives(), pipeline.getTextureMap());
            Optional<String> geometrySource = sources.getGeometrySource().map(s -> ClrwlTransformPatcher.patchGeometry(s, k.isCrumbling(), sources.getDirectives(), pipeline.getTextureMap()));
            ClrwlTransformOutput fragmentSource = ClrwlTransformPatcher.patchFragment(sources.getFragmentSource().orElseThrow(), k.isCrumbling(), k.customOutputs(), sources.getDirectives(), pipeline.getTextureMap());

            return new PatchedSources(vertexSource, geometrySource, fragmentSource, extensions, drawBuffers);
        });
    }

    private record Key(ClrwlProgramId realProgramId, boolean isCrumbling, boolean customOutputs)
    {
    }

    public record PatchedSources(String vertex, Optional<String> geometry, ClrwlTransformOutput fragment, Map<ShaderType, List<String>> extensions, int[] drawBuffers)
    {
    }
}
