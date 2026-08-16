package dev.djefrey.colorwheel.compile;

import com.google.common.collect.ImmutableList;
import dev.djefrey.colorwheel.compile.oit.ClrwlOitPrograms;
import dev.engine_room.flywheel.backend.gl.GlCompat;
import dev.engine_room.flywheel.backend.glsl.GlslVersion;
import dev.engine_room.flywheel.backend.glsl.ShaderSources;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.shaderpack.ShaderPack;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClrwlInstancedPrograms
{
	public static final List<String> EXTENSIONS = getExtensions(GlCompat.MAX_GLSL_VERSION);

	private final ClrwlPipelineCompiler compiler;
	private final ClrwlOitPrograms oitPrograms;

	private ClrwlInstancedPrograms(ClrwlPipelineCompiler compiler, ClrwlOitPrograms oitPrograms)
	{
		this.compiler = compiler;
		this.oitPrograms = oitPrograms;
	}

	private static List<String> getExtensions(GlslVersion glslVersion)
	{
		var extensions = ImmutableList.<String>builder();

		if (glslVersion.compareTo(GlslVersion.V330) < 0)
		{
			extensions.add("GL_ARB_shader_bit_encoding");
		}

		return extensions.build();
	}

	public static ClrwlInstancedPrograms build(ShaderSources sources, ShaderPack pack, ProgramSet programSet, boolean fallback)
	{
		if (!GlCompat.SUPPORTS_INSTANCING)
		{
			return null;
		}

		var pipeline = fallback
				? ClrwlPipelines.INSTANCING_FALLBACK
				: ClrwlPipelines.INSTANCING;

		var compiler = new ClrwlPipelineCompiler(sources, pipeline, pack, programSet);
		var oitPrograms = new ClrwlOitPrograms(sources);

        return new ClrwlInstancedPrograms(compiler, oitPrograms);
	}

	public PipelineProgramCache createPipelineProgramsCache()
	{
		return new PipelineProgramCache();
	}

	public ClrwlOitPrograms getOitPrograms()
	{
		return oitPrograms;
	}

	public void delete()
	{
		oitPrograms.delete();
	}

	public class PipelineProgramCache
	{
		private final Map<ClrwlShaderKey, ClrwlProgram> programCache = new HashMap<>();

		public ClrwlProgram get(ClrwlShaderKey key, IrisRenderingPipeline irisPipeline)
		{
			ClrwlProgram program = programCache.get(key);

			if (program == null)
			{
				program = ClrwlInstancedPrograms.this.compiler.get(key, irisPipeline);
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
