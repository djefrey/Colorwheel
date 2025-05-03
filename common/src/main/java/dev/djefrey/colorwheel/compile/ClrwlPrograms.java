package dev.djefrey.colorwheel.compile;

import com.google.common.collect.ImmutableList;
import dev.engine_room.flywheel.backend.gl.GlCompat;
import dev.engine_room.flywheel.backend.glsl.GlslVersion;
import dev.engine_room.flywheel.backend.glsl.ShaderSources;
import dev.engine_room.flywheel.backend.util.AtomicReferenceCounted;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ClrwlPrograms extends AtomicReferenceCounted {
	public static final List<String> EXTENSIONS = getExtensions(GlCompat.MAX_GLSL_VERSION);

	@Nullable
	private static ClrwlPrograms instance;

	private final ClrwlPipelineCompiler compiler;

	private ClrwlPrograms(ClrwlPipelineCompiler compiler)
	{
		this.compiler = compiler;
	}

	private static List<String> getExtensions(GlslVersion glslVersion)
	{
		var extensions = ImmutableList.<String>builder();
		if (glslVersion.compareTo(GlslVersion.V330) < 0) {
			extensions.add("GL_ARB_shader_bit_encoding");
		}
		return extensions.build();
	}

	public static void reload(ShaderSources sources)
	{
		if (!GlCompat.SUPPORTS_INSTANCING) {
			return;
		}

		var compiler = new ClrwlPipelineCompiler(sources, ClrwlPipelines.INSTANCING);
		ClrwlPrograms newInstance = new ClrwlPrograms(compiler);

		setInstance(newInstance);
	}

	public static void setInstance(@Nullable ClrwlPrograms newInstance)
	{
		if (instance != null) {
			instance.release();
		}
		if (newInstance != null) {
			newInstance.acquire();
		}
		instance = newInstance;
	}

	private final Map<ClrwlShaderKey, ClrwlProgram> programCache = new HashMap<>();

	@Nullable
	public static ClrwlPrograms get() {
		return instance;
	}

	public static boolean allLoaded() {
		return instance != null;
	}

	public static void kill() {
		setInstance(null);
	}

	public ClrwlProgram get(ClrwlShaderKey key)
	{
		return programCache.computeIfAbsent(key, this.compiler::get);
	}

	public void handleUberShaderUpdate()
	{
		programCache.clear();
		ClrwlPipelineCompiler.refreshUberShaders();
	}

	@Override
	protected void _delete()
	{

	}
}
