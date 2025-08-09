package dev.djefrey.colorwheel.compile;

import dev.djefrey.colorwheel.*;
import dev.djefrey.colorwheel.accessors.ProgramSourceAccessor;
import dev.djefrey.colorwheel.compile.oit.*;
import dev.djefrey.colorwheel.engine.ClrwlOitAccumulateOverride;
import dev.engine_room.flywheel.api.material.CutoutShader;
import dev.engine_room.flywheel.backend.BackendConfig;
import dev.engine_room.flywheel.backend.compile.ContextShader;
import dev.engine_room.flywheel.backend.compile.component.BufferTextureInstanceComponent;
import dev.engine_room.flywheel.backend.compile.component.InstanceStructComponent;
import dev.engine_room.flywheel.backend.gl.GlCompat;
import dev.engine_room.flywheel.backend.glsl.SourceComponent;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.util.ResourceUtil;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class ClrwlPipelines
{
    public static final ResourceLocation API_IMPL_VERT = Colorwheel.rl("internal/api_impl.vert");
    public static final ResourceLocation API_IMPL_GEOM = Colorwheel.rl("internal/api_impl_geom.glsl");
    public static final ResourceLocation API_IMPL_FRAG = Colorwheel.rl("internal/api_impl.frag");

    public static final ResourceLocation IRIS_COMPAT_VERT = Colorwheel.rl("internal/instancing/iris_compat.vert");
    public static final ResourceLocation IRIS_COMPAT_FRAG = Colorwheel.rl("internal/instancing/iris_compat.frag");

    public static final ResourceLocation MAIN_VERT = Colorwheel.rl("internal/instancing/main.vert");
    public static final ResourceLocation MAIN_GEOM = Colorwheel.rl("internal/instancing/main_geom.glsl");
    public static final ResourceLocation MAIN_FRAG = Colorwheel.rl("internal/instancing/main.frag");

    public static final ResourceLocation OIT_DEPTH_RANGE_FRAG = Colorwheel.rl("internal/oit/depth_range.frag");

    public static final ResourceLocation COMPONENTS_HEADER_FRAG = ResourceUtil.rl("internal/components_header.frag");

    private static final ResourceLocation FULLSCREEN = ResourceUtil.rl("internal/fullscreen.vert");

    public static ClrwlPipeline INSTANCING = ClrwlPipeline.builder()
            .id("instancing")
            .minVersion(GlCompat.MAX_GLSL_VERSION)
            .onSetup((b) ->
            {
                for (String ext : ClrwlPrograms.EXTENSIONS)
                {
                    b.requireExtension(ext);
                }
            })
            .vertex(ClrwlPipeline.vertexStage()
                        .define("IS_COLORWHEEL")
                        .onCompile((k, c) ->
                        {
                            if (k.isDebugEnabled())
                            {
                                c.define("_FLW_DEBUG");
                            }
                        })
                        .onCompile(ClrwlPipelines::setIrisDefines)
                        .onCompile((k, c) ->
                        {
                            var exts = ((ProgramSourceAccessor) c.getIrisSources()).colorwheel$getShaderExtensions().get(ShaderType.VERTEX);

                            for (var ext : exts)
                            {
                                c.enableExtension(ext);
                            }
                        })
                    .onCompile(($, c) -> c.define("fma(a, b, c)", "((a) * (b) + (c))"))
                    .onCompile((k, c) -> setContextDefine(k.context(), c))
                    .onCompile((k, c) ->
                    {
                        if (k.oit() != ClrwlPipelineCompiler.OitMode.OFF)
                        {
                            c.define("CLRWL_OIT");
                            c.define(k.oit().define);
                        }
                    })
                    .onCompile(ClrwlPipelines::setLightSmoothness)
                    .withResource(API_IMPL_VERT)
                    .withComponent((k) -> new InstanceStructComponent(k.instanceType()))
                    .withLoader((k, sources) -> sources.get(k.instanceType().vertexShader()))
                    .withLoader((k, sources) -> sources.get(k.material().vertexSource()))
                    .withLoader(($, sources) -> sources.get(ClrwlVertex.LAYOUT_SHADER))
                    .withLoader(($, sources) -> sources.get(IRIS_COMPAT_VERT))
                    .withComponent((k) -> new BufferTextureInstanceComponent(k.instanceType()))
                    .with(ClrwlPipelines::getIrisShaderVertexSource)
                    .withResource(MAIN_VERT)
                    .build())
            .geometry(ClrwlPipeline.geometryStage()
                    .define("IS_COLORWHEEL")
                    .onCompile((k, c) ->
                    {
                        if (k.isDebugEnabled())
                        {
                            c.define("_FLW_DEBUG");
                        }
                    })
                    .onCompile(ClrwlPipelines::setIrisDefines)
                    .onCompile((k, c) ->
                    {
                        var exts = ((ProgramSourceAccessor) c.getIrisSources()).colorwheel$getShaderExtensions().get(ShaderType.GEOMETRY);

                        for (var ext : exts)
                        {
                            c.enableExtension(ext);
                        }
                    })
                    .withResource(API_IMPL_GEOM)
                    .with(ClrwlPipelines::getIrisShaderGeometrySource)
                    .withResource(MAIN_GEOM)
                    .build())
            .fragment(ClrwlPipeline.fragmentStage()
                    .define("IS_COLORWHEEL")
                    .onCompile((k, c) ->
                    {
                        if (k.isDebugEnabled())
                        {
                            c.define("_FLW_DEBUG");
                        }
                    })
                    .onCompile(ClrwlPipelines::setIrisDefines)
                    .enableExtension("GL_ARB_conservative_depth")
                    .onCompile((k, c) ->
                    {
                        var exts = ((ProgramSourceAccessor) c.getIrisSources()).colorwheel$getShaderExtensions().get(ShaderType.FRAGMENT);

                        for (var ext : exts)
                        {
                            c.enableExtension(ext);
                        }
                    })
                    .onCompile(($, c) -> c.define("fma(a, b, c)", "((a) * (b) + (c))"))
                    .onCompile((k, c) -> setContextDefine(k.context(), c))
                    .onCompile((k, c) -> setCutoutDefine(k.cutout(), c))
                    .onCompile((k, c) ->
                    {
                        if (k.oit() != ClrwlPipelineCompiler.OitMode.OFF)
                        {
                            c.define("CLRWL_OIT");
                            c.define(k.oit().define);
                        }
                    })
                    .onCompile(ClrwlPipelines::setLightSmoothness)
                    .withResource(COMPONENTS_HEADER_FRAG)
                    .withResource(API_IMPL_FRAG)
                    .withLoader((k, sources) -> sources.get(k.material().fragmentSource()))
                    .withComponent(($) -> ClrwlPipelineCompiler.FOG)
                    .withLoader((k, sources) -> sources.get(k.light().source()))
                    .withLoader((k, sources) ->
                            k.cutout() == CutoutShaders.OFF
                                    ? sources.get(CutoutShaders.OFF.source())
                                    : ClrwlPipelineCompiler.CUTOUT)
                    .withLoader(($, sources) -> sources.get(IRIS_COMPAT_FRAG))
                    .with(ClrwlPipelines::getOitInouts)
                    .with(ClrwlPipelines::getIrisShaderFragmentSource)
                    .with(ClrwlPipelines::getPostShaderFragmentSource)
                    .withResource(MAIN_FRAG)
                    .build())
            .build();

    public static ClrwlOitCompositePipeline OIT_COMPOSITE = ClrwlOitCompositePipeline.builder()
            .id("oit_composite")
            .minVersion(GlCompat.MAX_GLSL_VERSION)
            .vertex(ClrwlOitCompositePipeline.vertexStage()
                    .withResource(FULLSCREEN)
                    .build()
            )
            .fragment(ClrwlOitCompositePipeline.fragmentStage()
                    .onCompile(($, c) -> c.define("fma(a, b, c)", "((a) * (b) + (c))"))
                    .with((k, c) -> new OitCompositeComponent(c.getLoader(), k.drawBuffers(), k.ranks(), k.overrides()))
                    .build())
            .build();

    private static void setIrisDefines(ClrwlShaderKey k, ClrwlCompilation c)
    {
        if (c.getPackDirectives().isOldLighting())
        {
            c.define("CLRWL_OLD_LIGHTING");
        }
    }

    private static void setLightSmoothness(ClrwlShaderKey k, ClrwlCompilation c)
    {
        var smoothness = BackendConfig.INSTANCE.lightSmoothness();

        switch (smoothness)
        {
            case FLAT ->
            {
                c.define("_FLW_LIGHT_SMOOTHNESS", "0");
            }

            case TRI_LINEAR ->
            {
                c.define("_FLW_LIGHT_SMOOTHNESS", "1");
            }

            case SMOOTH ->
            {
                c.define("_FLW_LIGHT_SMOOTHNESS", "2");
            }

            case SMOOTH_INNER_FACE_CORRECTED ->
            {
                c.define("_FLW_LIGHT_SMOOTHNESS", "2");
                c.define("_FLW_INNER_FACE_CORRECTION");
            }
        }
    }

    private static void setContextDefine(ContextShader ctx, ClrwlCompilation c)
    {
        switch (ctx)
        {
            case DEFAULT -> {}
            case CRUMBLING -> c.define("_FLW_CRUMBLING");
            case EMBEDDED -> c.define("FLW_EMBEDDED");
        }
    }

    private static void setCutoutDefine(CutoutShader cutout, ClrwlCompilation c)
    {
        if (cutout != CutoutShaders.OFF)
        {
            c.define("_FLW_USE_DISCARD");
        }
    }

    private static SourceComponent getIrisShaderVertexSource(ClrwlShaderKey k, ClrwlCompilation c)
    {
        var pipeline = c.getIrisPipeline();
        var sources = c.getIrisSources();

        String vertexSource = sources.getVertexSource().orElseThrow();

//        List<StringPair> irisDefines = ((ShaderPackAccessor) k.pack()).colorwheel$getEnvironmentDefines();
//        List<StringPair> defines = new ArrayList<>(irisDefines);
//        defines.addAll(c.defines);
//
//        String preprocessed = JcppProcessor.glslPreprocessSource(vertexSource, defines);

        String preprocessed = vertexSource;
        String transformed = ClrwlTransformPatcher.patchVertex(preprocessed, k.transparency(), sources.getDirectives(), pipeline.getTextureMap());

        return new IrisShaderComponent(sources.getName(), transformed);
    }

    private static SourceComponent getIrisShaderGeometrySource(ClrwlShaderKey k, ClrwlCompilation c)
    {
        var pipeline = c.getIrisPipeline();
        var sources = c.getIrisSources();

        String geometrySource = sources.getGeometrySource().orElseThrow();

//        List<StringPair> irisDefines = ((ShaderPackAccessor) k.pack()).colorwheel$getEnvironmentDefines();
//        List<StringPair> defines = new ArrayList<>(irisDefines);
//        defines.addAll(c.defines);
//
//        String preprocessed = JcppProcessor.glslPreprocessSource(fragmentSource, defines);

        String preprocessed = geometrySource;
        String transformed = ClrwlTransformPatcher.patchGeometry(preprocessed, k.transparency(), sources.getDirectives(), pipeline.getTextureMap());

        return new IrisShaderComponent(sources.getName(), transformed);
    }

    private static SourceComponent getIrisShaderFragmentSource(ClrwlShaderKey k, ClrwlCompilation c)
    {
        var pipeline = c.getIrisPipeline();
        var sources = c.getIrisSources();

        String fragmentSource = sources.getFragmentSource().orElseThrow();

//        List<StringPair> irisDefines = ((ShaderPackAccessor) k.pack()).colorwheel$getEnvironmentDefines();
//        List<StringPair> defines = new ArrayList<>(irisDefines);
//        defines.addAll(c.defines);
//
//        String preprocessed = JcppProcessor.glslPreprocessSource(fragmentSource, defines);

        String preprocessed = fragmentSource;
        String transformed = ClrwlTransformPatcher.patchFragment(preprocessed, k.oit(), k.transparency(), sources.getDirectives(), pipeline.getTextureMap());

        return new IrisShaderComponent(sources.getName(), transformed);
    }

    private static SourceComponent getOitInouts(ClrwlShaderKey k, ClrwlCompilation c)
    {
        var programGroup = k.isShadow()
                ? ClrwlProgramGroup.SHADOW
                : ClrwlProgramGroup.GBUFFERS;

        if (k.oit() == ClrwlPipelineCompiler.OitMode.DEPTH_RANGE)
        {
            var drawBuffers = c.getIrisSources().getDirectives().getDrawBuffers();

            return new ClrwlFragDataOutComponent(drawBuffers.length);
        }
        else if (k.oit() == ClrwlPipelineCompiler.OitMode.GENERATE_COEFFICIENTS)
        {
            var ranks = c.getProperties().getOitCoeffRanks(programGroup);
            var drawBuffers = c.getIrisSources().getDirectives().getDrawBuffers();

            return new OitCoefficientsOutputComponent(ranks, drawBuffers.length);
        }
        else if (k.oit() == ClrwlPipelineCompiler.OitMode.EVALUATE)
        {
            var ranks = c.getProperties().getOitCoeffRanks(programGroup);

            return new OitCoefficientsSamplersComponent(ranks.length);
        }

        return null;
    }

    private static SourceComponent getPostShaderFragmentSource(ClrwlShaderKey k, ClrwlCompilation c)
    {
        var programGroup = k.isShadow()
                ? ClrwlProgramGroup.SHADOW
                : ClrwlProgramGroup.GBUFFERS;

        switch (k.oit())
        {
            case DEPTH_RANGE ->
            {
                c.define("CLRWL_POST_SHADER");
                return c.getLoader().get(OIT_DEPTH_RANGE_FRAG);
            }

            case GENERATE_COEFFICIENTS ->
            {
                var drawBuffers = c.getIrisSources().getDirectives().getDrawBuffers();
                var ranks = c.getProperties().getOitCoeffRanks(programGroup);
                var overrides = c.getProperties().getOitAccumulateOverrides(programGroup);

                Map<Integer, Integer> coeffFrag = new HashMap<>();

                for (int i = 0; i < drawBuffers.length; i++)
                {
                    int buffer = drawBuffers[i];
                    var maybeCoeffId = Utils.findFirst(overrides, e -> e.drawBuffer() == buffer)
                            .flatMap(ClrwlOitAccumulateOverride::coefficientId);

                    if (maybeCoeffId.isPresent())
                    {
                        coeffFrag.putIfAbsent(maybeCoeffId.get(), i);
                    }
                }

                c.define("CLRWL_POST_SHADER");
                return new OitCollectCoeffsComponent(ranks, coeffFrag);
            }

            case EVALUATE ->
            {
                var drawBuffers = c.getIrisSources().getDirectives().getDrawBuffers();
                var ranks = c.getProperties().getOitCoeffRanks(programGroup);
                var overrides = c.getProperties().getOitAccumulateOverrides(programGroup);

                c.define("CLRWL_POST_SHADER");
                return new OitEvaluateComponent(drawBuffers, ranks, overrides);
            }
        }

        return null;
    }

    private ClrwlPipelines() {}
}
