package dev.djefrey.colorwheel.engine;

import dev.engine_room.flywheel.api.backend.RenderContext;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.backend.engine.uniform.UniformBuffer;
import dev.engine_room.flywheel.backend.gl.shader.GlProgram;
import dev.engine_room.flywheel.lib.util.ExtraMemoryOps;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class ClrwlUniform
{
	public static final int COLORWHEEL_INDEX = 5;
	public static final String COLORWHEEL_BLOCK_NAME = "_ColorwheelUniforms";

	private static final int SIZE = 48;
	static final UniformBuffer BUFFER = new UniformBuffer(COLORWHEEL_INDEX, SIZE);

	private static final Matrix3f NORMAL = new Matrix3f();

	private ClrwlUniform() {}

	public static void update(RenderContext context)
	{
		long ptr = BUFFER.ptr();

		Vec3i renderOrigin = VisualizationManager.getOrThrow(context.level())
				.renderOrigin();
		var camera = context.camera();
		Vec3 cameraPos = camera.getPosition();
		var camX = (float) (cameraPos.x - renderOrigin.getX());
		var camY = (float) (cameraPos.y - renderOrigin.getY());
		var camZ = (float) (cameraPos.z - renderOrigin.getZ());

		Matrix4f normal = new Matrix4f(context.stack().last().pose())
				.translate(-camX, -camY, -camZ)
				.invert()
				.transpose();
		normal.get3x3(NORMAL);

		ptr = writeMat3(ptr, NORMAL);

		BUFFER.markDirty();
	}

	private static long writeMat3(long ptr, Matrix3f mat)
	{
		ExtraMemoryOps.putMatrix3fPadded(ptr, mat);
		return ptr + 48;
	}

	private static long writeMat4WthInv(long ptr, Matrix4f mat, Matrix4f inv)
	{
		mat.invert(inv);

		ExtraMemoryOps.putMatrix4f(ptr, 		 mat);
		ExtraMemoryOps.putMatrix4f(ptr + 64, inv);

		return ptr + 128;
	}

	public static void bind()
	{
		BUFFER.bind();
	}

	public static void delete()
	{
		BUFFER.delete();
	}

	public static void setUniformBlockBinding(GlProgram program)
	{
		program.setUniformBlockBinding(COLORWHEEL_BLOCK_NAME, COLORWHEEL_INDEX);
	}

	// TODO: connect signal
	public static void onReloadLevelRenderer() {
		delete();
	}
}
