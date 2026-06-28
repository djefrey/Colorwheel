package dev.djefrey.colorwheel.compile;

import dev.djefrey.colorwheel.*;
import dev.djefrey.colorwheel.compile.oit.*;
import dev.djefrey.colorwheel.engine.ClrwlOitAccumulateOverride;
import dev.djefrey.colorwheel.engine.ClrwlVertex;
import dev.djefrey.colorwheel.shaderpack.ClrwlProgramGroup;
import dev.djefrey.colorwheel.shaderpack.ClrwlProgramId;
import dev.djefrey.colorwheel.util.Utils;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.api.material.CutoutShader;
import dev.engine_room.flywheel.backend.BackendConfig;
import dev.engine_room.flywheel.backend.compile.ContextShader;
import dev.engine_room.flywheel.backend.compile.component.BufferTextureInstanceComponent;
import dev.engine_room.flywheel.backend.compile.component.InstanceAssemblerComponent;
import dev.engine_room.flywheel.backend.compile.component.InstanceStructComponent;
import dev.engine_room.flywheel.backend.compile.component.SsboInstanceComponent;
import dev.engine_room.flywheel.backend.gl.GlCompat;
import dev.engine_room.flywheel.backend.glsl.GlslVersion;
import dev.engine_room.flywheel.backend.glsl.SourceComponent;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.util.ResourceUtil;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public class ClrwlPipelines
{
    public static final ResourceLocation API_IMPL_VERT = Colorwheel.rl("internal/api_impl.vert");
    public static final ResourceLocation API_IMPL_GEOM = Colorwheel.rl("internal/api_impl_geom.glsl");
    public static final ResourceLocation API_IMPL_FRAG = Colorwheel.rl("internal/api_impl.frag");

    public static final ResourceLocation IRIS_COMPAT_VERT = Colorwheel.rl("internal/iris_compat.vert");
    public static final ResourceLocation IRIS_COMPAT_GEOM = Colorwheel.rl("internal/iris_compat_geom.glsl");
    public static final ResourceLocation IRIS_COMPAT_FRAG = Colorwheel.rl("internal/iris_compat.frag");

    public static final ResourceLocation INSTANCING_MAIN_VERT = Colorwheel.rl("internal/instancing/main.vert");
    public static final ResourceLocation INSTANCING_MAIN_GEOM = Colorwheel.rl("internal/instancing/main_geom.glsl");
    public static final ResourceLocation INSTANCING_MAIN_FRAG = Colorwheel.rl("internal/instancing/main.frag");

    public static final ResourceLocation INDIRECT_MAIN_VERT = Colorwheel.rl("internal/indirect/main.vert");
    public static final ResourceLocation INDIRECT_MAIN_GEOM = Colorwheel.rl("internal/indirect/main_geom.glsl");
    public static final ResourceLocation INDIRECT_MAIN_FRAG = Colorwheel.rl("internal/indirect/main.frag");

    public static final ResourceLocation OIT_DEPTH_RANGE_FRAG = Colorwheel.rl("internal/oit/depth_range.frag");

    public static final ResourceLocation COMPONENTS_HEADER_FRAG = ResourceUtil.rl("internal/components_header.frag");

    private static final ResourceLocation FULLSCREEN = ResourceUtil.rl("internal/fullscreen.vert");

    public static final String CLRWL_POST_FRAGMENT_FCT = "_clrwl_post_shader";

    private static final SimpleClrwlPipelineBuilder INSTANCING_BUILDER = SimpleClrwlPipelineBuilder.builder()
            .id("instancing")
            .extensions(ClrwlInstancedPrograms.EXTENSIONS)
            .assembler(BufferTextureInstanceComponent::new)
            .vertex(INSTANCING_MAIN_VERT)
            .geometry(INSTANCING_MAIN_GEOM)
            .fragment(INSTANCING_MAIN_FRAG);

    private static final SimpleClrwlPipelineBuilder INDIRECT_BUILDER = SimpleClrwlPipelineBuilder.builder()
            .id("indirect")
            .extensions(ClrwlIndirectPrograms.EXTENSIONS)
            .assembler(SsboInstanceComponent::new)
            .vertex(INDIRECT_MAIN_VERT)
            .geometry(INDIRECT_MAIN_GEOM)
            .fragment(INDIRECT_MAIN_FRAG);

    public static ClrwlPipeline INSTANCING = INSTANCING_BUILDER.build(false);
    public static ClrwlPipeline INSTANCING_FALLBACK = INSTANCING_BUILDER.build(true);

    public static ClrwlPipeline INDIRECT = INDIRECT_BUILDER.build(false);
    public static ClrwlPipeline INDIRECT_FALLBACK = INDIRECT_BUILDER.build(true);

    public static ClrwlOitCompositePipeline OIT_COMPOSITE = ClrwlOitCompositePipeline.builder()
            .id("oit_composite")
            .minVersion(GlCompat.MAX_GLSL_VERSION)
            .vertex(ClrwlOitCompositePipeline.vertexStage()
                    .onCompile((k, c) ->
                    {
                        if (k.isShadow())
                        {
                            c.define("_CLRWL_IS_SHADOW_PASS");
                        }
                        else
                        {
                            c.define("_CLRWL_IS_GBUFFERS_PASS");
                        }
                    })
                    .withResource(FULLSCREEN)
                    .build()
            )
            .fragment(ClrwlOitCompositePipeline.fragmentStage()
                    .onCompile((k, c) ->
                    {
                        if (k.isShadow())
                        {
                            c.define("_CLRWL_IS_SHADOW_PASS");
                        }
                        else
                        {
                            c.define("_CLRWL_IS_GBUFFERS_PASS");
                        }
                    })
                    .onCompile(($, c) ->
                    {
                        if (GlCompat.MAX_GLSL_VERSION.compareTo(GlslVersion.V400) < 0 && !c.extensions.contains("GL_ARB_gpu_shader5"))
                        {
                            c.define("fma(a, b, c)", "((a) * (b) + (c))");
                        }
                    })
                    .with((k, c) -> new OitCompositeComponent(c.getLoader(), k.drawBuffers(), k.ranks(), k.overrides()))
                    .build())
            .build();

    public static class SimpleClrwlPipelineBuilder
    {
        public interface InstanceAssembler
        {
            SourceComponent assemble(InstanceType<?> type);
        }

        private String id;
        private List<String> extensions;
        private InstanceAssembler assembler;
        private ResourceLocation vertexMain;
        private ResourceLocation geometryMain;
        private ResourceLocation fragmentMain;

        private SimpleClrwlPipelineBuilder() {}

        public static SimpleClrwlPipelineBuilder builder()
        {
            return new SimpleClrwlPipelineBuilder();
        }

        public SimpleClrwlPipelineBuilder id(String id)
        {
            this.id = id;
            return this;
        }

        public SimpleClrwlPipelineBuilder extensions(List<String> extensions)
        {
            this.extensions = extensions;
            return this;
        }

        public SimpleClrwlPipelineBuilder assembler(InstanceAssembler assembler)
        {
            this.assembler = assembler;
            return this;
        }

        public SimpleClrwlPipelineBuilder vertex(ResourceLocation vertex)
        {
            this.vertexMain = vertex;
            return this;
        }

        public SimpleClrwlPipelineBuilder geometry(ResourceLocation geometry)
        {
            this.geometryMain = geometry;
            return this;
        }

        public SimpleClrwlPipelineBuilder fragment(ResourceLocation fragment)
        {
            this.fragmentMain = fragment;
            return this;
        }

        private ClrwlPipelineStage<ClrwlShaderKey> buildVertexStage(boolean fallback)
        {
            var stage = ClrwlPipeline.vertexStage()
                .define("IS_COLORWHEEL")
                .define("CLRWL_IS_" + id.toUpperCase())
                .onCompile(ClrwlPipelines::setClrwlPassDefine);

            if (fallback)
            {
                stage = stage
                        .define("_CLRWL_IS_FALLBACK");
            }

            stage = stage
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
                    var exts = c.getIrisSources().extensions().get(ShaderType.VERTEX);

                    for (var ext : exts)
                    {
                        c.enableExtension(ext);
                    }
                })
                .onCompile(($, c) ->
                {
                    if (GlCompat.MAX_GLSL_VERSION.compareTo(GlslVersion.V400) < 0 && !c.extensions.contains("GL_ARB_gpu_shader5"))
                    {
                        c.define("fma(a, b, c)", "((a) * (b) + (c))");
                    }
                })
                .onCompile((k, c) -> setContextDefine(k.context(), c));

            if (!fallback)
            {
                stage = stage
                    .onCompile((k, c) ->
                    {
                        if (k.oit() != ClrwlPipelineCompiler.OitMode.OFF)
                        {
                            c.define("CLRWL_OIT");
                            c.define(k.oit().define);
                        }
                    });
            }

            stage = stage
                .onCompile(ClrwlPipelines::setLightSmoothness)
                .withResource(API_IMPL_VERT)
                .withComponent((k) -> new InstanceStructComponent(k.instanceType()))
                .withLoader((k, sources) -> sources.get(k.instanceType().vertexShader()))
                .withLoader((k, sources) -> sources.get(k.material().vertexSource()))
                .withResource(ClrwlVertex.LAYOUT_SHADER)
                .withResource(IRIS_COMPAT_VERT)
                .withComponent((k) -> assembler.assemble(k.instanceType()))
                .with(ClrwlPipelines::getIrisShaderVertexSource)
                .withResource(vertexMain);

            return stage.build();
        }

        private ClrwlPipelineStage<ClrwlShaderKey> buildGeometryStage(boolean fallback)
        {
            var stage = ClrwlPipeline.geometryStage()
                    .define("IS_COLORWHEEL")
                    .define("CLRWL_IS_" + id.toUpperCase())
                    .onCompile(ClrwlPipelines::setClrwlPassDefine);

            if (fallback)
            {
                stage = stage
                        .define("_CLRWL_IS_FALLBACK");
            }

            stage = stage
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
                        var exts = c.getIrisSources().extensions().get(ShaderType.GEOMETRY);

                        for (var ext : exts)
                        {
                            c.enableExtension(ext);
                        }
                    })
                    .withResource(API_IMPL_GEOM)
                    .withResource(IRIS_COMPAT_GEOM)
                    .with(ClrwlPipelines::getIrisShaderGeometrySource)
                    .withResource(geometryMain);

            return stage.build();
        }

        private ClrwlPipelineStage<ClrwlShaderKey> buildFragmentStage(boolean fallback)
        {
            var stage = ClrwlPipeline.fragmentStage()
                .define("IS_COLORWHEEL")
                .define("CLRWL_IS_" + id.toUpperCase())
                .onCompile(ClrwlPipelines::setClrwlPassDefine);

            if (fallback)
            {
                stage = stage
                    .define("_CLRWL_IS_FALLBACK");
            }

            stage = stage
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
                    var exts = c.getIrisSources().extensions().get(ShaderType.FRAGMENT);

                    for (var ext : exts)
                    {
                        c.enableExtension(ext);
                    }
                })
                .onCompile(($, c) ->
                {
                    if (GlCompat.MAX_GLSL_VERSION.compareTo(GlslVersion.V400) < 0 && !c.extensions.contains("GL_ARB_gpu_shader5"))
                    {
                        c.define("fma(a, b, c)", "((a) * (b) + (c))");
                    }
                })
                .onCompile((k, c) -> setContextDefine(k.context(), c))
                .onCompile((k, c) -> setCutoutDefine(k.cutout(), c))
                .onCompile((k, c) -> setSeparateAoDefine(c));

            if (!fallback)
            {
                stage = stage
                    .onCompile((k, c) ->
                    {
                        if (k.oit() != ClrwlPipelineCompiler.OitMode.OFF)
                        {
                            c.define("CLRWL_OIT");
                            c.define(k.oit().define);
                        }
                    });
            }

            stage = stage
                .onCompile(ClrwlPipelines::setLightSmoothness)
                .withResource(COMPONENTS_HEADER_FRAG)
                .withResource(API_IMPL_FRAG)
                .withLoader((k, sources) ->
                        k.cutout() == CutoutShaders.OFF
                            ? sources.get(CutoutShaders.OFF.source())
                            : sources.get(k.cutout().source()));

            if (!fallback)
            {
                stage = stage
                    .withLoader((k, sources) -> sources.get(k.material().fragmentSource()))
                    .withLoader((k, sources) -> sources.get(k.fog().source()))
                    .withLoader((k, sources) -> sources.get(k.light().source()))
                    .withLoader(($, sources) -> sources.get(IRIS_COMPAT_FRAG))
                    .with(ClrwlPipelines::getOitInouts)
                    .with(ClrwlPipelines::getIrisShaderFragmentSource)
                    .withResource(fragmentMain)
                    .with(ClrwlPipelines::getPostShaderFragmentSource);
            }
            else
            {
                stage = stage
                    .withLoader(($, sources) -> sources.get(IRIS_COMPAT_FRAG))
                    .with(ClrwlPipelines::getIrisShaderFragmentSource)
                    .withResource(fragmentMain)
                    .with(ClrwlPipelines::getFallbackCutoutFragmentSource);
            }

            return stage.build();
        }

        public ClrwlPipeline build(boolean fallback)
        {
            return ClrwlPipeline.builder()
                    .id(id + (fallback ? "_fallback" : ""))
                    .minVersion(GlCompat.MAX_GLSL_VERSION)
                    .onSetup((b) ->
                    {
                        for (String ext : extensions)
                        {
                            b.requireExtension(ext);
                        }
                    })
                    .vertex(buildVertexStage(fallback))
                    .geometry(buildGeometryStage(fallback))
                    .fragment(buildFragmentStage(fallback))
                    .build();
        }
    }

    private static void setClrwlPassDefine(ClrwlShaderKey k, ClrwlCompilation c)
    {
        if (k.isShadow())
        {
            c.define("_CLRWL_IS_SHADOW_PASS");
        }
        else
        {
            c.define("_CLRWL_IS_GBUFFERS_PASS");
        }
    }

    private static void setIrisDefines(ClrwlShaderKey k, ClrwlCompilation c)
    {
        if (c.getProgramSet().getPackDirectives().isOldLighting())
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
        var programId = ClrwlProgramId.fromTransparency(k.transparency(), k.isShadow());
        var name = programId.programName() + ".vsh";
        var patched = c.getIrisSources().vertex();

        return new IrisShaderComponent(name, patched);
    }

    private static SourceComponent getIrisShaderGeometrySource(ClrwlShaderKey k, ClrwlCompilation c)
    {
        var programId = ClrwlProgramId.fromTransparency(k.transparency(), k.isShadow());
        var name = programId.programName() + ".gsh";
        var patched = c.getIrisSources().geometry().orElseThrow();

        return new IrisShaderComponent(name, patched);
    }

    private static SourceComponent getIrisShaderFragmentSource(ClrwlShaderKey k, ClrwlCompilation c)
    {
        var programId = ClrwlProgramId.fromTransparency(k.transparency(), k.isShadow());
        var name = programId.programName() + ".fsh";
        var patched = c.getIrisSources().fragment().code();

        return new IrisShaderComponent(name, patched);
    }

    private static SourceComponent getOitInouts(ClrwlShaderKey k, ClrwlCompilation c)
    {
        var programGroup = k.isShadow()
                ? ClrwlProgramGroup.SHADOW
                : ClrwlProgramGroup.GBUFFERS;

        if (k.oit() == ClrwlPipelineCompiler.OitMode.GENERATE_COEFFICIENTS)
        {
            var ranks = c.getProperties().getOitCoeffRanks(programGroup);

            return new OitCoefficientsOutputComponent(ranks);
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
                return c.getLoader().get(OIT_DEPTH_RANGE_FRAG);
            }

            case GENERATE_COEFFICIENTS ->
            {
                var drawBuffers = c.getIrisSources().drawBuffers();
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

                return new OitCollectCoeffsComponent(ranks, coeffFrag, c.getIrisSources().fragment().outputs());
            }

            case EVALUATE ->
            {
                var drawBuffers = c.getIrisSources().drawBuffers();
                var ranks = c.getProperties().getOitCoeffRanks(programGroup);
                var overrides = c.getProperties().getOitAccumulateOverrides(programGroup);
                var outputs = c.getIrisSources().fragment().outputs();

                return new OitEvaluateComponent(drawBuffers, ranks, overrides, outputs);
            }
        }

        return new SourceComponent()
        {
            @Override
            public Collection<? extends SourceComponent> included()
            {
                return Collections.emptyList();
            }

            @Override
            public String source()
            {
                return "void " + CLRWL_POST_FRAGMENT_FCT + "() {}";
            }

            @Override
            public String name()
            {
                return Colorwheel.rl("noop_post_fragment").toString();
            }
        };
    }

    private static void setSeparateAoDefine(ClrwlCompilation c)
    {
        if (c.getProgramSet().getPackDirectives().shouldUseSeparateAo())
        {
            c.define("_CLRWL_SEPARATE_AO");
        }
    }

    private static SourceComponent getFallbackCutoutFragmentSource(ClrwlShaderKey k, ClrwlCompilation c)
    {
        return new FallbackCutoutComponent(c.getIrisSources().fragment().outputs());
    }

    private ClrwlPipelines() {}
}
