package dev.djefrey.colorwheel.gl;

import dev.engine_room.flywheel.backend.gl.GlObject;
import dev.engine_room.flywheel.backend.gl.shader.ShaderType;
import org.lwjgl.opengl.GL20;

public class ClrwlGlShader extends GlObject
{
	public final ClrwlShaderType type;
	private final String name;

	public ClrwlGlShader(int handle, ClrwlShaderType type, String name)
	{
		this.type = type;
		this.name = name;

		handle(handle);
	}

	@Override
	protected void deleteInternal(int handle) {
		GL20.glDeleteShader(handle);
	}

	@Override
	public String toString() {
		return "ClrwlGlShader{" + type.name + handle() + " " + name + "}";
	}

}
