package dev.djefrey.colorwheel.compile;

import com.google.common.collect.ImmutableList;
import dev.djefrey.colorwheel.ClrwlShaderSources;
import dev.engine_room.flywheel.backend.compile.*;
import dev.engine_room.flywheel.backend.gl.GlCompat;
import dev.engine_room.flywheel.backend.glsl.GlslVersion;
import dev.engine_room.flywheel.backend.glsl.ShaderSources;
import dev.engine_room.flywheel.backend.util.AtomicReferenceCounted;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	public static void reload(ClrwlShaderSources sources)
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
		return compiler.get(key);
	}

	@Override
	protected void _delete()
	{

	}
}
