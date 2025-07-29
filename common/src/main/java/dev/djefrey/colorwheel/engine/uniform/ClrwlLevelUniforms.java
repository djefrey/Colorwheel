package dev.djefrey.colorwheel.engine.uniform;

import dev.engine_room.flywheel.api.backend.RenderContext;
import dev.engine_room.flywheel.backend.engine.uniform.LevelUniforms;
import dev.engine_room.flywheel.backend.engine.uniform.UniformBuffer;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class ClrwlLevelUniforms extends UniformWriter
{
	private static final int SIZE = 3 * 16 + 3 * 4;
	static final UniformBuffer BUFFER = new UniformBuffer(ClrwlUniforms.LEVEL_INDEX, SIZE);

	public static void update(RenderContext context) {
		long ptr = BUFFER.ptr();

		ClientLevel level = context.level();
		float partialTick = context.partialTick();

		Vec3 cloudColor = level.getCloudColor(partialTick);
		ptr = writeVec4(ptr, (float) cloudColor.x, (float) cloudColor.y, (float) cloudColor.z, 1f);

		ptr = writeVec3(ptr, LevelUniforms.LIGHT0_DIRECTION);
		ptr = writeVec3(ptr, LevelUniforms.LIGHT1_DIRECTION);

		ptr = writeFloat(ptr, level.getMoonBrightness());
		ptr = writeFloat(ptr, level.getSkyDarken(partialTick));

		// TODO: use defines for custom dimension ids
		int dimensionId;
		ResourceKey<Level> dimension = level.dimension();
		if (Level.OVERWORLD.equals(dimension)) {
			dimensionId = 0;
		} else if (Level.NETHER.equals(dimension)) {
			dimensionId = 1;
		} else if (Level.END.equals(dimension)) {
			dimensionId = 2;
		} else {
			dimensionId = -1;
		}
		ptr = writeInt(ptr, dimensionId);

		BUFFER.markDirty();
	}
}
