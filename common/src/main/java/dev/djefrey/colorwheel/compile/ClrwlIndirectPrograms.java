package dev.djefrey.colorwheel.compile;

import com.google.common.collect.ImmutableList;
import dev.djefrey.colorwheel.Colorwheel;
import dev.djefrey.colorwheel.compile.oit.ClrwlOitPrograms;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.backend.compile.IndirectPrograms;
import dev.engine_room.flywheel.backend.compile.OitPrograms;
import dev.engine_room.flywheel.backend.compile.PipelineCompiler;
import dev.engine_room.flywheel.backend.compile.component.InstanceStructComponent;
import dev.engine_room.flywheel.backend.compile.component.SsboInstanceComponent;
import dev.engine_room.flywheel.backend.compile.core.CompilationHarness;
import dev.engine_room.flywheel.backend.compile.core.Compile;
import dev.engine_room.flywheel.backend.engine.uniform.Uniforms;
import dev.engine_room.flywheel.backend.gl.GlCompat;
import dev.engine_room.flywheel.backend.gl.shader.GlProgram;
import dev.engine_room.flywheel.backend.gl.shader.ShaderType;
import dev.engine_room.flywheel.backend.glsl.GlslVersion;
import dev.engine_room.flywheel.backend.glsl.ShaderSources;
import dev.engine_room.flywheel.lib.util.ResourceUtil;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.shaderpack.ShaderPack;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClrwlIndirectPrograms
{
	private static final ResourceLocation CULL_SHADER_API_IMPL = Colorwheel.rl("internal/indirect/cull_api_impl.glsl");
	private static final ResourceLocation CULL_SHADER_MAIN = Colorwheel.rl("internal/indirect/cull.glsl");

	private static final ResourceLocation APPLY_SHADER_MAIN = Colorwheel.rl("internal/indirect/apply.glsl");

	public static final List<String> EXTENSIONS = getExtensions(GlCompat.MAX_GLSL_VERSION);
	private static final List<String> COMPUTE_EXTENSIONS = getComputeExtensions(GlCompat.MAX_GLSL_VERSION);

	private static final Compile<InstanceType<?>> CULL = new Compile<>();
	private static final Compile<ResourceLocation> UTIL = new Compile<>();

	private final ClrwlPipelineCompiler compiler;
	private final CompilationHarness<InstanceType<?>> culling;
	private final CompilationHarness<ResourceLocation> utils;
	private final ClrwlOitPrograms oitPrograms;

	// WARNING: this can ONLY be used for utils ! (otherwise, kaboom)
	private final IndirectPrograms flwPrograms;

	private ClrwlIndirectPrograms(ClrwlPipelineCompiler compiler, CompilationHarness<InstanceType<?>> culling, CompilationHarness<ResourceLocation> utils, ClrwlOitPrograms oitPrograms)
	{
		this.compiler = compiler;
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

		var compiler = new ClrwlPipelineCompiler(sources, pipeline, pack, programSet);
		var culling = createCullingCompiler(sources);
		var util = createUtilCompiler(sources);
		var oitPrograms = new ClrwlOitPrograms(sources);

        return new ClrwlIndirectPrograms(compiler, culling, util, oitPrograms);
	}

	/**
	 * A compiler for cull shaders, parameterized by the instance type.
	 */
	private static CompilationHarness<InstanceType<?>> createCullingCompiler(ShaderSources sources)
	{
		return CULL.program()
				.link(CULL.shader(GlCompat.MAX_GLSL_VERSION, ShaderType.COMPUTE)
						.nameMapper(instanceType -> "culling/" + ResourceUtil.toDebugFileNameNoExtension(instanceType.cullShader()))
						.requireExtensions(COMPUTE_EXTENSIONS)
						.define("_FLW_SUBGROUP_SIZE", GlCompat.SUBGROUP_SIZE)
						.withResource(CULL_SHADER_API_IMPL)
						.withComponent(InstanceStructComponent::new)
						.withResource(InstanceType::cullShader)
						.withComponent(SsboInstanceComponent::new)
						.withResource(CULL_SHADER_MAIN))
				.postLink((key, program) -> Uniforms.setUniformBlockBindings(program))
				.harness("culling", sources);
	}

	/**
	 * A compiler for utility shaders, directly compiles the shader at the resource location specified by the parameter.
	 */
	private static CompilationHarness<ResourceLocation> createUtilCompiler(ShaderSources sources)
	{
		return UTIL.program()
				.link(UTIL.shader(GlCompat.MAX_GLSL_VERSION, ShaderType.COMPUTE)
						.nameMapper(resourceLocation -> "utilities/" + ResourceUtil.toDebugFileNameNoExtension(resourceLocation))
						.requireExtensions(COMPUTE_EXTENSIONS)
						.define("_FLW_SUBGROUP_SIZE", GlCompat.SUBGROUP_SIZE)
						.withResource(s -> s))
				.harness("utilities", sources);
	}


	public PipelineProgramCache createPipelineProgramsCache()
	{
		return new PipelineProgramCache();
	}

	public GlProgram getCullingProgram(InstanceType<?> instanceType)
	{
		return culling.get(instanceType);
	}

	public GlProgram getApplyProgram()
	{
		return utils.get(APPLY_SHADER_MAIN);
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
