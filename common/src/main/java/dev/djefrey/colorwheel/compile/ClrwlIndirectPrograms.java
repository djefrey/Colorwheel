package dev.djefrey.colorwheel.compile;

import com.google.common.collect.ImmutableList;
import dev.djefrey.colorwheel.Colorwheel;
import dev.djefrey.colorwheel.compile.oit.ClrwlOitPrograms;
import dev.djefrey.colorwheel.engine.uniform.ClrwlUniforms;
import dev.djefrey.colorwheel.shaderpack.ClrwlProgramGroup;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.backend.compile.IndirectPrograms;
import dev.engine_room.flywheel.backend.compile.OitPrograms;
import dev.engine_room.flywheel.backend.compile.PipelineCompiler;
import dev.engine_room.flywheel.backend.compile.component.InstanceStructComponent;
import dev.engine_room.flywheel.backend.compile.core.Compilation;
import dev.engine_room.flywheel.backend.compile.core.CompilationHarness;
import dev.engine_room.flywheel.backend.compile.core.Compile;
import dev.engine_room.flywheel.backend.gl.GlCompat;
import dev.engine_room.flywheel.backend.gl.shader.GlProgram;
import dev.engine_room.flywheel.backend.gl.shader.ShaderType;
import dev.engine_room.flywheel.backend.glsl.GlslVersion;
import dev.engine_room.flywheel.backend.glsl.ShaderSources;
import dev.engine_room.flywheel.lib.util.ResourceUtil;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.shaderpack.ShaderPack;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClrwlIndirectPrograms
{
	public enum Culling
	{
		GBUFFERS_FULL("gbuffers_full", Colorwheel.rl("internal/indirect/cull/cull_gbuffers_full.glsl"), ClrwlProgramGroup.GBUFFERS),
		GBUFFERS_EARLY("gbuffers_early", Colorwheel.rl("internal/indirect/cull/cull_gbuffers_early.glsl"), ClrwlProgramGroup.GBUFFERS),
		GBUFFERS_LATE("gbuffers_late", Colorwheel.rl("internal/indirect/cull/cull_gbuffers_late.glsl"), ClrwlProgramGroup.GBUFFERS),
		SHADOW("shadow", Colorwheel.rl("internal/indirect/cull/cull_shadow.glsl"), ClrwlProgramGroup.SHADOW);

		private final String name;
		private final ResourceLocation shader;
		private final ClrwlProgramGroup group;

		Culling(String name, ResourceLocation shader, ClrwlProgramGroup group)
		{
			this.name = name;
			this.shader = shader;
			this.group = group;
		}

		public String shaderName()
		{
			return this.name;
		}

		public ResourceLocation shader()
		{
			return this.shader;
		}

		public String passDefine()
		{
			switch (group)
			{
                case GBUFFERS ->
				{
					return "_CLRWL_IS_GBUFFERS_PASS";
                }

                case SHADOW ->
				{
					return "_CLRWL_IS_SHADOW_PASS";
                }

				default ->
				{
					return "";
				}
            }
		}

		public boolean isShadow()
		{
			return group == ClrwlProgramGroup.SHADOW;
		}
	}

	public record TransformKey(InstanceType<?> instanceType, ClrwlProgramGroup group)
	{
		public ResourceLocation cullShader()
		{
			return instanceType.cullShader();
		}

		public String passDefine()
		{
			switch (group)
			{
				case GBUFFERS ->
				{
					return "_CLRWL_IS_GBUFFERS_PASS";
				}

				case SHADOW ->
				{
					return "_CLRWL_IS_SHADOW_PASS";
				}

				default ->
				{
					return "";
				}
			}
		}
	}

	private static final ResourceLocation CULL_SHADER_API_IMPL = Colorwheel.rl("internal/indirect/cull/cull_api_impl.glsl");
	private static final ResourceLocation TRANSFORM_SHADER_MAIN = Colorwheel.rl("internal/indirect/transform.glsl");
	private static final ResourceLocation APPLY_SHADER_MAIN = Colorwheel.rl("internal/indirect/apply.glsl");
	private static final ResourceLocation ZERO_SHADER_MAIN = Colorwheel.rl("internal/indirect/zero_models.glsl");

	public static final List<String> EXTENSIONS = getExtensions(GlCompat.MAX_GLSL_VERSION);
	private static final List<String> COMPUTE_EXTENSIONS = getComputeExtensions(GlCompat.MAX_GLSL_VERSION);

	private static final Compile<TransformKey> TRANSFORM = new Compile<>();
	private static final Compile<Culling> CULL = new Compile<>();
	private static final Compile<ResourceLocation> UTIL = new Compile<>();

	private final ClrwlPipelineCompiler compiler;
	private final CompilationHarness<TransformKey> transform;
	private final CompilationHarness<Culling> culling;
	private final CompilationHarness<ResourceLocation> utils;
	private final ClrwlOitPrograms oitPrograms;

	// WARNING: this can ONLY be used for utils ! (otherwise, kaboom)
	private final IndirectPrograms flwPrograms;

	private ClrwlIndirectPrograms(ClrwlPipelineCompiler compiler, CompilationHarness<TransformKey> transform, CompilationHarness<Culling> culling, CompilationHarness<ResourceLocation> utils, ClrwlOitPrograms oitPrograms)
	{
		this.compiler = compiler;
		this.transform = transform;
		this.culling = culling;
		this.utils = utils;
		this.oitPrograms = oitPrograms;

		try
		{
			var constructor = IndirectPrograms.class.getDeclaredConstructor(PipelineCompiler.class, CompilationHarness.class, CompilationHarness.class, OitPrograms.class);
			constructor.setAccessible(true);
			this.flwPrograms = constructor.newInstance(null, null, utils, null);
		}
		catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e)
		{
			throw new RuntimeException(e);
		}
	}

	private static List<String> getExtensions(GlslVersion glslVersion)
	{
		var extensions = ImmutableList.<String>builder();
		if (glslVersion.compareTo(GlslVersion.V400) < 0)
		{
			extensions.add("GL_ARB_gpu_shader5");
		}
		if (glslVersion.compareTo(GlslVersion.V420) < 0)
		{
			extensions.add("GL_ARB_shading_language_420pack");
			extensions.add("GL_ARB_shader_image_load_store");
		}
		if (glslVersion.compareTo(GlslVersion.V430) < 0)
		{
			extensions.add("GL_ARB_shader_storage_buffer_object");
			extensions.add("GL_ARB_shader_image_size");
		}
		if (glslVersion.compareTo(GlslVersion.V460) < 0)
		{
			extensions.add("GL_ARB_shader_draw_parameters");
		}
		return extensions.build();
	}

	private static List<String> getComputeExtensions(GlslVersion glslVersion)
	{
		var extensions = ImmutableList.<String>builder();

		extensions.addAll(EXTENSIONS);

		if (glslVersion.compareTo(GlslVersion.V430) < 0) {
			extensions.add("GL_ARB_compute_shader");
		}
		return extensions.build();
	}

	public static ClrwlIndirectPrograms build(ShaderSources sources, ShaderPack pack, ProgramSet programSet, boolean fallback)
	{
		if (!GlCompat.SUPPORTS_INDIRECT)
		{
			return null;
		}

		var pipeline = fallback
				? ClrwlPipelines.INDIRECT_FALLBACK
				: ClrwlPipelines.INDIRECT;

		var directives = programSet.getPackDirectives();
		var occlusionCulling = directives.shouldUseOcclusionCulling();
		var frustumCulling = directives.shouldUseFrustumCulling();

		var compiler = new ClrwlPipelineCompiler(sources, pipeline, pack, programSet);
		var transform = createTransformCompiler(sources);
		var culling = createCullingCompiler(sources, occlusionCulling, frustumCulling);
		var util = createUtilCompiler(sources);
		var oitPrograms = new ClrwlOitPrograms(sources);

        return new ClrwlIndirectPrograms(compiler, transform, culling, util, oitPrograms);
	}

	private static CompilationHarness<TransformKey> createTransformCompiler(ShaderSources sources)
	{
		var shader = TRANSFORM.shader(GlCompat.MAX_GLSL_VERSION, ShaderType.COMPUTE)
				.nameMapper(instanceType -> "colorwheel/transform_bounding_spheres/" + ResourceUtil.toDebugFileNameNoExtension(instanceType.cullShader()))
				.requireExtensions(COMPUTE_EXTENSIONS)
				.define("_FLW_SUBGROUP_SIZE", GlCompat.SUBGROUP_SIZE)
				.onCompile((k, c) -> c.define(k.passDefine()));

		shader = shader
				.onCompile(($, c) -> setModCompatDefines(c))
				.withResource(CULL_SHADER_API_IMPL)
				.withComponent(k -> new InstanceStructComponent(k.instanceType()))
				.withResource(TransformKey::cullShader)
				.withComponent(k -> new SsboInstanceComponent(k.instanceType()))
				.withResource(TRANSFORM_SHADER_MAIN);

		return TRANSFORM.program()
				.link(shader)
				.postLink((key, program) -> ClrwlUniforms.setUniformsBlockBindings(program))
				.harness("transform_bounding_spheres", sources);
	}

	/**
	 * A compiler for cull shaders, parameterized by the instance type.
	 */
	private static CompilationHarness<Culling> createCullingCompiler(ShaderSources sources, boolean occlusion, boolean frustum)
	{
		var shader = CULL.shader(GlCompat.MAX_GLSL_VERSION, ShaderType.COMPUTE)
				.nameMapper(cull -> "colorwheel/cull/" + cull.shaderName())
				.requireExtensions(COMPUTE_EXTENSIONS)
				.enableExtension("GL_KHR_shader_subgroup_basic")
				.enableExtension("GL_KHR_shader_subgroup_ballot")
				.define("_FLW_SUBGROUP_SIZE", GlCompat.SUBGROUP_SIZE)
				.onCompile((k, c) -> c.define(k.passDefine()));

		if (occlusion)
		{
			shader = shader.define("_CLRWL_OCCLUSION_CULLING", 1);
		}

		if (frustum)
		{
			shader = shader.define("_CLRWL_FRUSTUM_CULLING", 1);
		}

		shader = shader
				.onCompile(($, c) -> setModCompatDefines(c))
				.withResource(Culling::shader);

		return CULL.program()
				.link(shader)
				.postLink((key, program) -> ClrwlUniforms.setUniformsBlockBindings(program))
				.harness("culling", sources);
	}

	/**
	 * A compiler for utility shaders, directly compiles the shader at the resource location specified by the parameter.
	 */
	private static CompilationHarness<ResourceLocation> createUtilCompiler(ShaderSources sources)
	{
		return UTIL.program()
				.link(UTIL.shader(GlCompat.MAX_GLSL_VERSION, ShaderType.COMPUTE)
						.nameMapper(resourceLocation -> "colorwheel/utilities/" + ResourceUtil.toDebugFileNameNoExtension(resourceLocation))
						.requireExtensions(COMPUTE_EXTENSIONS)
						.define("_FLW_SUBGROUP_SIZE", GlCompat.SUBGROUP_SIZE)
						.onCompile(($, c) -> setModCompatDefines(c))
						.withResource(s -> s))
				.harness("utilities", sources);
	}

	private static void setModCompatDefines(Compilation c)
	{
		for (var define : Colorwheel.getModCompat().getShaderDefines())
		{
			c.define(define.key(), define.value());
		}
	}

	public PipelineProgramCache createPipelineProgramsCache()
	{
		return new PipelineProgramCache();
	}

	public GlProgram getTransformProgram(InstanceType<?> instanceType, ClrwlProgramGroup group)
	{
		return this.transform.get(new TransformKey(instanceType, group));
	}

	public GlProgram getCullingProgram(Culling culling)
	{
		return this.culling.get(culling);
	}

	public GlProgram getApplyProgram()
	{
		return utils.get(APPLY_SHADER_MAIN);
	}

	public GlProgram getZeroModelsProgram()
	{
		return utils.get(ZERO_SHADER_MAIN);
	}

	public GlProgram getScatterProgram()
	{
		return flwPrograms.getScatterProgram();
	}

	public GlProgram getDownsampleFirstProgram()
	{
		return flwPrograms.getDownsampleFirstProgram();
	}

	public GlProgram getDownsampleSecondProgram()
	{
		return flwPrograms.getDownsampleSecondProgram();
	}

	public ClrwlOitPrograms getOitPrograms()
	{
		return oitPrograms;
	}

	// WARNING: Should ONLY be used for utils programs
	public IndirectPrograms getFlwPrograms()
	{
		return flwPrograms;
	}

	public void delete()
	{
		transform.delete();
		culling.delete();
		utils.delete();
		oitPrograms.delete();

		// flwPrograms is not deleted as it contains null references (=> kaboom)
	}

	public class PipelineProgramCache
	{
		private final Map<ClrwlShaderKey, ClrwlProgram> programCache = new HashMap<>();

		public ClrwlProgram get(ClrwlShaderKey key, IrisRenderingPipeline irisPipeline)
		{
			ClrwlProgram program = programCache.get(key);

			if (program == null)
			{
				program = ClrwlIndirectPrograms.this.compiler.get(key, irisPipeline);
				programCache.put(key, program);
			}

			return program;
		}

		public void delete()
		{
			for (ClrwlProgram program : programCache.values())
			{
				program.free();
			}

			programCache.clear();
		}
	}
}
