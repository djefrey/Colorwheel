package dev.djefrey.colorwheel.compile.oit;

import dev.djefrey.colorwheel.ClrwlSamplers;
import dev.djefrey.colorwheel.Colorwheel;
import dev.djefrey.colorwheel.compile.ClrwlCompilation;
import dev.djefrey.colorwheel.compile.ClrwlPipelineStage;
import dev.djefrey.colorwheel.compile.ClrwlPipelines;
import dev.djefrey.colorwheel.engine.ClrwlOitAccumulateOverride;
import dev.djefrey.colorwheel.engine.uniform.ClrwlUniforms;
import dev.engine_room.flywheel.backend.compile.core.FailedCompilation;
import dev.engine_room.flywheel.backend.compile.core.ProgramLinker;
import dev.engine_room.flywheel.backend.compile.core.ShaderResult;
import dev.engine_room.flywheel.backend.gl.GlCompat;
import dev.engine_room.flywheel.backend.gl.shader.GlProgram;
import dev.engine_room.flywheel.backend.gl.shader.GlShader;
import dev.engine_room.flywheel.backend.glsl.ShaderSources;
import dev.engine_room.flywheel.backend.glsl.SourceComponent;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL20;

import java.util.*;
import java.util.function.Consumer;

public class ClrwlOitPrograms
{
    private static final ResourceLocation DEPTH = Colorwheel.rl("internal/oit/depth.frag");

    private final ShaderSources sources;

    public ClrwlOitPrograms(ShaderSources sources)
    {
        this.sources = sources;
    }

    private final Map<ClrwlOitCompositeShaderKey, GlProgram> compositeProgramCache = new HashMap<>();

    public GlProgram getOitCompositeProgram(int[] drawBuffers, int[] ranks, List<ClrwlOitAccumulateOverride> overrides)
    {
        ClrwlOitCompositeShaderKey key = new ClrwlOitCompositeShaderKey(drawBuffers, ranks, overrides);

        return compositeProgramCache.computeIfAbsent(key, this::compileComposite);
    }

    private GlProgram compileComposite(ClrwlOitCompositeShaderKey key)
    {
        String id = ClrwlPipelines.OIT_COMPOSITE.id();
        var vertex = compileStage(id, ClrwlPipelines.OIT_COMPOSITE.vertex(), key).unwrap();
        var fragment = compileStage(id, ClrwlPipelines.OIT_COMPOSITE.fragment(), key).unwrap();

        var linker = new ProgramLinker();
        var program = linker.link(List.of(vertex, fragment), ($) -> {});

        program.bind();
        program.setUniformBlockBinding(ClrwlUniforms.FRAME_BLOCK_NAME, ClrwlUniforms.FRAME_INDEX);

        program.setSamplerBinding("_flw_depthRange", ClrwlSamplers.DEPTH_RANGE);

        for (int i = 0; i < key.ranks().length; i++)
        {
            program.setSamplerBinding("clrwl_coefficients" + i, ClrwlSamplers.getCoefficient(i));
        }

        var drawBuffers = key.drawBuffers();

        for (int i = 0; i < drawBuffers.length; i++)
        {
            program.setSamplerBinding("_clrwl_accumulate" + drawBuffers[i], ClrwlSamplers.getAccumulate(i));
        }

        GlProgram.unbind();

        return program;
    }

    private <K> ShaderResult compileStage(String name, ClrwlPipelineStage<K> stage, K key)
    {
        var compile = new ClrwlCompilation(null, null, null, null, sources);

        compile.version(GlCompat.MAX_GLSL_VERSION);

        for (var ext : stage.extensions())
        {
            compile.enableExtension(ext);
        }

        for (var defines : stage.defines())
        {
            compile.define(defines);
        }

        stage.compile().accept(key, compile);

        for (var fetcher : stage.fetchers())
        {
            expand(fetcher.apply(key, compile), compile::appendComponent);
        }

        String source = compile.getShaderCode();

        int handle = GL20.glCreateShader(stage.type().glEnum);

        GlCompat.safeShaderSource(handle, source);
        GL20.glCompileShader(handle);

        var shaderName = name + "." + stage.type().extension;
//        dumpSource(source, shaderName);

        var infoLog = GL20.glGetShaderInfoLog(handle);

        if (GL20.glGetShaderi(handle, GL20.GL_COMPILE_STATUS) == GL20.GL_TRUE)
        {
            return ShaderResult.success(new GlShader(handle, stage.type().toFlw().orElseThrow(), shaderName), infoLog);
        }

        GL20.glDeleteShader(handle);
        return ShaderResult.failure(new FailedCompilation(shaderName, List.of(), "", source, infoLog));
    }

    private static void expand(SourceComponent rootSource, Consumer<SourceComponent> out)
    {
        var included = new LinkedHashSet<SourceComponent>(); // use hash set to deduplicate. linked to preserve order

        recursiveDepthFirstInclude(included, rootSource);
        included.add(rootSource);

        included.forEach(out);
    }

    private static void recursiveDepthFirstInclude(Set<SourceComponent> included, SourceComponent component)
    {
        for (var include : component.included())
        {
            recursiveDepthFirstInclude(included, include);
        }

        included.addAll(component.included());
    }
}
