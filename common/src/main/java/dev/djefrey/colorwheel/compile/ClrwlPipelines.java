package dev.djefrey.colorwheel.compile;

import dev.djefrey.colorwheel.ClrwlVertex;
import dev.djefrey.colorwheel.Colorwheel;
import dev.djefrey.colorwheel.IrisShaderComponent;
import dev.djefrey.colorwheel.accessors.ShaderPackAccessor;
import dev.engine_room.flywheel.api.material.CutoutShader;
import dev.engine_room.flywheel.backend.compile.ContextShader;
import dev.engine_room.flywheel.backend.compile.component.BufferTextureInstanceComponent;
import dev.engine_room.flywheel.backend.compile.component.InstanceStructComponent;
import dev.engine_room.flywheel.backend.gl.GlCompat;
import dev.engine_room.flywheel.backend.glsl.GlslVersion;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.util.ResourceUtil;
import net.irisshaders.iris.helpers.StringPair;
import net.irisshaders.iris.shaderpack.preprocessor.JcppProcessor;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class ClrwlPipelines
{
    public static final ResourceLocation API_IMPL_VERT = Colorwheel.rl("internal/api_impl.vert");
    public static final ResourceLocation API_IMPL_FRAG = Colorwheel.rl("internal/api_impl.frag");

    public static final ResourceLocation IRIS_COMPAT_VERT = Colorwheel.rl("internal/instancing/iris_compat.vert");
    public static final ResourceLocation IRIS_COMPAT_FRAG = Colorwheel.rl("internal/instancing/iris_compat.frag");

    public static final ResourceLocation MAIN_VERT = Colorwheel.rl("internal/instancing/main.vert");
    public static final ResourceLocation MAIN_FRAG = Colorwheel.rl("internal/instancing/main.frag");

    public static final ResourceLocation COMPONENTS_HEADER_FRAG = ResourceUtil.rl("internal/components_header.frag");

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
            .vertex(ClrwlPipeline.stage()
                    .define("IS_FLYWHEEL")
                    .onCompile(($, c) -> c.define("fma(a, b, c)", "((a) * (b) + (c))"))
                    .onCompile((k, c) -> setContextDefine(k.context(), c))
                    // TODO: Light smoothness
                    .withResource(API_IMPL_VERT)
                    .withComponent((k) -> new InstanceStructComponent(k.instanceType()))
                    .withLoader((k, sources) -> sources.get(k.instanceType().vertexShader()))
                    .withLoader((k, sources) -> sources.get(k.material().shaders().vertexSource()))
                    .withLoader(($, sources) -> sources.get(ClrwlVertex.LAYOUT_SHADER))
                    .withLoader(($, sources) -> sources.get(IRIS_COMPAT_VERT))
                    .withComponent((k) -> new BufferTextureInstanceComponent(k.instanceType()))
                    .with((k, c) ->
                    {
                        var pipeline = c.getIrisPipeline();
                        var sources = c.getIrisSources();

                        String vertexSource = sources.getVertexSource().orElseThrow();

                        List<StringPair> irisDefines = ((ShaderPackAccessor) k.pack()).colorwheel$getEnvironmentDefines();
                        List<StringPair> defines = new ArrayList<>(irisDefines);
                        defines.addAll(c.defines);

                        String preprocessed = JcppProcessor.glslPreprocessSource(vertexSource, defines);
                        String transformed = ClrwlTransformPatcher.patchVertex(preprocessed, pipeline.getTextureMap());

                        return new IrisShaderComponent(sources.getName(), transformed);
                    })
                    .withResource(MAIN_VERT)
                    .build())
            .fragment(ClrwlPipeline.stage()
                    .define("IS_FLYWHEEL")
                    .enableExtension("GL_ARB_conservative_depth")
                    .onCompile(($, c) -> c.define("fma(a, b, c)", "((a) * (b) + (c))"))
                    .onCompile((k, c) -> setContextDefine(k.context(), c))
                    .onCompile((k, c) -> setCutoutDefine(k.material().cutout(), c))
                    // TODO: Light smoothness
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
                    .with((k, c) ->
                    {
                        var pipeline = c.getIrisPipeline();
                        var sources = c.getIrisSources();

                        String fragmentSource = sources.getFragmentSource().orElseThrow();

                        List<StringPair> irisDefines = ((ShaderPackAccessor) k.pack()).colorwheel$getEnvironmentDefines();
                        List<StringPair> defines = new ArrayList<>(irisDefines);
                        defines.addAll(c.defines);

                        String preprocessed = JcppProcessor.glslPreprocessSource(fragmentSource, defines);
                        String transformed = ClrwlTransformPatcher.patchFragment(preprocessed, pipeline.getTextureMap());

                        return new IrisShaderComponent(sources.getName(), transformed);
                    })
                    .withResource(MAIN_FRAG)
                    .build())
            .build();

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

    private ClrwlPipelines() {}
}
