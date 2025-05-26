package dev.djefrey.colorwheel.compile;

import dev.djefrey.colorwheel.ClrwlVertex;
import dev.djefrey.colorwheel.Colorwheel;
import dev.djefrey.colorwheel.IrisShaderComponent;
import dev.djefrey.colorwheel.Utils;
import dev.djefrey.colorwheel.accessors.PackDirectivesAccessor;
import dev.djefrey.colorwheel.accessors.ProgramSourceAccessor;
import dev.djefrey.colorwheel.accessors.ShaderPackAccessor;
import dev.djefrey.colorwheel.compile.oit.*;
import dev.djefrey.colorwheel.engine.ClrwlOitCoeffDirective;
import dev.engine_room.flywheel.api.material.CutoutShader;
import dev.engine_room.flywheel.backend.BackendConfig;
import dev.engine_room.flywheel.backend.compile.ContextShader;
import dev.engine_room.flywheel.backend.compile.component.BufferTextureInstanceComponent;
import dev.engine_room.flywheel.backend.compile.component.InstanceStructComponent;
import dev.engine_room.flywheel.backend.gl.GlCompat;
import dev.engine_room.flywheel.backend.glsl.SourceComponent;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.util.ResourceUtil;
import net.irisshaders.iris.gl.shader.ShaderType;
import net.irisshaders.iris.helpers.StringPair;
import net.irisshaders.iris.shaderpack.preprocessor.JcppProcessor;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClrwlPipelines
{
    public static final ResourceLocation API_IMPL_VERT = Colorwheel.rl("internal/api_impl.vert");
    public static final ResourceLocation API_IMPL_FRAG = Colorwheel.rl("internal/api_impl.frag");

    public static final ResourceLocation IRIS_COMPAT_VERT = Colorwheel.rl("internal/instancing/iris_compat.vert");
    public static final ResourceLocation IRIS_COMPAT_FRAG = Colorwheel.rl("internal/instancing/iris_compat.frag");

    public static final ResourceLocation MAIN_VERT = Colorwheel.rl("internal/instancing/main.vert");
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
                    .define("IS_FLYWHEEL")
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
                    .with((k, c) -> new ExtendedInstanceShaderComponent(c.getLoader(), k.instanceType().vertexShader()))
                    .withLoader((k, sources) -> sources.get(k.material().shaders().vertexSource()))
                    .withLoader(($, sources) -> sources.get(ClrwlVertex.LAYOUT_SHADER))
                    .withLoader(($, sources) -> sources.get(IRIS_COMPAT_VERT))
                    .withComponent((k) -> new BufferTextureInstanceComponent(k.instanceType()))
                    .with(ClrwlPipelines::getIrisShaderVertexSource)
                    .withResource(MAIN_VERT)
                    .build())
            .fragment(ClrwlPipeline.fragmentStage()
                    .define("IS_FLYWHEEL")
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
                    .onCompile((k, c) -> setCutoutDefine(k.material().cutout(), c))
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
                    .withLoader((k, sources) -> sources.get(k.material().shaders().fragmentSource()))
                    .withComponent(($) -> ClrwlPipelineCompiler.FOG)
                    .withLoader((k, sources) -> sources.get(k.material().light().source()))
                    .withLoader((k, sources) ->
                            k.material().cutout() == CutoutShaders.OFF
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
                    .with((k, c) -> new OitCompositeComponent(c.getLoader(), k.translucentCoeffs(), k.opaques(), k.ranks()))
                    .build())
            .build();

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
        String transformed = ClrwlTransformPatcher.patchVertex(preprocessed, pipeline.getTextureMap());

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
        String transformed = ClrwlTransformPatcher.patchFragment(preprocessed, k.oit(), pipeline.getTextureMap());

        return new IrisShaderComponent(sources.getName(), transformed);
    }

    private static SourceComponent getOitInouts(ClrwlShaderKey k, ClrwlCompilation c)
    {
        if (k.oit() == ClrwlPipelineCompiler.OitMode.DEPTH_RANGE)
        {
            var drawBuffers = c.getIrisSources().getDirectives().getDrawBuffers();

            return new ClrwlFragDataOutComponent(drawBuffers.length);
        }
        else if (k.oit() == ClrwlPipelineCompiler.OitMode.GENERATE_COEFFICIENTS)
        {
            PackDirectivesAccessor directives = (PackDirectivesAccessor) k.packDirectives();
            var ranks = directives.getCoefficientsRanks(k.isShadow());

            var drawBuffers = c.getIrisSources().getDirectives().getDrawBuffers();

            return new OitCoefficientsOutputComponent(ranks, drawBuffers.length);
        }
        else if (k.oit() == ClrwlPipelineCompiler.OitMode.EVALUATE)
        {
            PackDirectivesAccessor directives = (PackDirectivesAccessor) k.packDirectives();
            var coeffs = directives.getCoefficientsRanks(k.isShadow()).keySet().stream().sorted().toList();

            return new OitCoefficientsSamplersComponent(coeffs);
        }

        return null;
    }

    private static SourceComponent getPostShaderFragmentSource(ClrwlShaderKey k, ClrwlCompilation c)
    {
        switch (k.oit())
        {
            case DEPTH_RANGE ->
            {
                c.define("CLRWL_POST_SHADER");
                return c.getLoader().get(OIT_DEPTH_RANGE_FRAG);
            }

            case GENERATE_COEFFICIENTS ->
            {
                PackDirectivesAccessor directives = (PackDirectivesAccessor) k.packDirectives();
                var ranks = directives.getCoefficientsRanks(k.isShadow());
                var coeffs = directives.getTranslucentCoefficients(k.isShadow());
                var renderTargets = Utils.reverse(directives.getTranslucentRenderTargets(k.isShadow()));
                var drawBuffers = c.getIrisSources().getDirectives().getDrawBuffers();

                Map<Integer, Integer> locations = new HashMap<>();

                for (int i = 0; i < drawBuffers.length; i++)
                {
                    int buffer = drawBuffers[i];

                    var acc = renderTargets.get(buffer);
                    if (acc != null)
                    {
                        locations.put(coeffs.get(acc), i);
                        continue;
                    }
                }

                c.define("CLRWL_POST_SHADER");
                return new OitCollectCoeffsComponent(ranks, locations);
            }

            case EVALUATE ->
            {
                PackDirectivesAccessor directives = (PackDirectivesAccessor) k.packDirectives();
                var ranks = directives.getCoefficientsRanks(k.isShadow());
                var translucentCoeffs = directives.getTranslucentCoefficients(k.isShadow());
                var translucentRenderTargets = Utils.reverse(directives.getTranslucentRenderTargets(k.isShadow()));
                var opaqueRenderTargets = Utils.reverse(directives.getOpaqueRenderTargets(k.isShadow()));
                var drawBuffers = c.getIrisSources().getDirectives().getDrawBuffers();

                Map<Integer, Integer> translucentLocations = new HashMap<>();
                Map<Integer, Integer> opaqueLocations = new HashMap<>();

                for (int i = 0; i < drawBuffers.length; i++)
                {
                    int buffer = drawBuffers[i];

                    var acc = translucentRenderTargets.get(buffer);
                    if (acc != null)
                    {
                        translucentLocations.put(acc, i);
                        continue;
                    }

                    acc = opaqueRenderTargets.get(buffer);
                    if (acc != null)
                    {
                        opaqueLocations.put(acc, i);
                        continue;
                    }
                }

                c.define("CLRWL_POST_SHADER");
                return new OitEvaluateComponent(translucentLocations, opaqueLocations, translucentCoeffs, ranks);
            }
        }

        return null;
    }

    private ClrwlPipelines() {}
}
