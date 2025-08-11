package dev.djefrey.colorwheel.compile;

import com.google.common.collect.ImmutableList;
import dev.djefrey.colorwheel.compile.oit.ClrwlOitPrograms;
import dev.engine_room.flywheel.backend.gl.GlCompat;
import dev.engine_room.flywheel.backend.glsl.GlslVersion;
import dev.engine_room.flywheel.backend.glsl.ShaderSources;
import net.irisshaders.iris.shaderpack.ShaderPack;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClrwlPrograms
{
	private static final List<ClrwlPrograms> PROGRAMS = new ArrayList<>();

	public static final List<String> EXTENSIONS = getExtensions(GlCompat.MAX_GLSL_VERSION);

	private final ClrwlPipelineCompiler compiler;
	private final ClrwlOitPrograms oitPrograms;

	private ClrwlPrograms(ClrwlPipelineCompiler compiler, ClrwlOitPrograms oitPrograms)
	{
		this.compiler = compiler;
		this.oitPrograms = oitPrograms;
	}

	private static List<String> getExtensions(GlslVersion glslVersion)
	{
		var extensions = ImmutableList.<String>builder();
		if (glslVersion.compareTo(GlslVersion.V330) < 0) {
			extensions.add("GL_ARB_shader_bit_encoding");
		}
		return extensions.build();
	}

	public static ClrwlPrograms build(ShaderSources sources, ShaderPack pack, NamespacedId dimension)
	{
		if (!GlCompat.SUPPORTS_INSTANCING)
		{
			return null;
		}

		var compiler = new ClrwlPipelineCompiler(sources, ClrwlPipelines.INSTANCING, pack, dimension);
		var oit = new ClrwlOitPrograms(sources);

		ClrwlPrograms programs = new ClrwlPrograms(compiler, oit);

		PROGRAMS.add(programs);

		return programs;
	}

	private final Map<ClrwlShaderKey, ClrwlProgram> programCache = new HashMap<>();

	public ClrwlProgram get(ClrwlShaderKey key)
	{
		ClrwlProgram program = programCache.get(key);

		if (program == null)
		{
			program = this.compiler.get(key);
			programCache.put(key, program);
		}

		return program;
	}

	private void deleteCache()
	{
		for (ClrwlProgram program : programCache.values())
		{
			program.free();
		}

		programCache.clear();
	}

	public void delete()
	{
		deleteCache();
		PROGRAMS.remove(this);
	}

	public ClrwlOitPrograms getOitPrograms()
	{
		return oitPrograms;
	}

	public static void handleUberShaderUpdate()
	{
		for (var program : PROGRAMS)
		{
			program.deleteCache();
		}

		ClrwlPipelineCompiler.refreshUberShaders();
	}
}
