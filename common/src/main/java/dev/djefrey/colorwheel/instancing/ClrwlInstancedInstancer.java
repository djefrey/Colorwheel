package dev.djefrey.colorwheel.instancing;

import dev.djefrey.colorwheel.engine.ClrwlAbstractInstancer;
import dev.djefrey.colorwheel.engine.ClrwlBaseInstancer;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.InstanceWriter;
import dev.engine_room.flywheel.backend.engine.BaseInstancer;
import dev.engine_room.flywheel.backend.engine.InstancerKey;
import dev.engine_room.flywheel.backend.gl.TextureBuffer;
import dev.engine_room.flywheel.backend.gl.buffer.GlBuffer;
import dev.engine_room.flywheel.backend.gl.buffer.GlBufferUsage;
import dev.engine_room.flywheel.lib.math.MoreMath;
import dev.engine_room.flywheel.lib.memory.MemoryBlock;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ClrwlInstancedInstancer<I extends Instance> extends ClrwlBaseInstancer<I>
{
	private final int instanceStride;

	private final InstanceWriter<I> writer;
	@Nullable
	private GlBuffer vbo;

	private final List<ClrwlInstancedDraw> draws = new ArrayList<>();

	public ClrwlInstancedInstancer(InstancerKey<I> key, ClrwlBaseInstancer.Recreate<I> recreate) {
		super(key, recreate);
		var layout = type.layout();
		// Align to one texel in the texture buffer
		instanceStride = MoreMath.align16(layout.byteSize());
		writer = type.writer();
	}

	public List<ClrwlInstancedDraw> draws() {
		return draws;
	}

	public void init() {
		if (vbo != null) {
			return;
		}

		vbo = new GlBuffer(GlBufferUsage.DYNAMIC_DRAW);
	}

	public void updateBuffer() {
		if (changed.isEmpty() || vbo == null) {
			return;
		}

		int byteSize = instanceStride * instances.size();
		if (needsToGrow(byteSize)) {
			// TODO: Should this memory block be persistent?
			var temp = MemoryBlock.malloc(increaseSize(byteSize));

			writeAll(temp.ptr());

			vbo.upload(temp);

			temp.free();
		} else {
			writeChanged();
		}

		changed.clear();
	}

	private void writeChanged() {
		changed.forEachSetSpan((startInclusive, endInclusive) -> {
			// Generally we're good about ensuring we don't have changed bits set out of bounds, but check just in case
			if (startInclusive >= instances.size()) {
				return;
			}
			int actualEnd = Math.min(endInclusive, instances.size() - 1);
			var temp = MemoryBlock.malloc((long) instanceStride * (actualEnd - startInclusive + 1));
			long ptr = temp.ptr();
			for (int i = startInclusive; i <= actualEnd; i++) {
				writer.write(ptr, instances.get(i));
				ptr += instanceStride;
			}

			vbo.uploadSpan((long) startInclusive * instanceStride, temp);

			temp.free();
		});
	}

	private void writeAll(long ptr) {
		for (I instance : instances) {
			writer.write(ptr, instance);
			ptr += instanceStride;
		}
	}

	private long increaseSize(long capacity) {
		return Math.max(capacity + (long) instanceStride * 16, (long) (capacity * 1.6));
	}

	public boolean needsToGrow(long capacity) {
		if (capacity < 0) {
			throw new IllegalArgumentException("Size " + capacity + " < 0");
		}

		if (capacity == 0) {
			return false;
		}

        return capacity > vbo.size();
    }

	public void parallelUpdate() {
		if (deleted.isEmpty()) {
			return;
		}

		// Figure out which elements are to be removed.
		final int oldSize = this.instances.size();
		int removeCount = deleted.cardinality();

		if (oldSize == removeCount) {
			clear();
			return;
		}

		final int newSize = oldSize - removeCount;

		// Start from the first deleted index.
		int writePos = deleted.nextSetBit(0);

		if (writePos < newSize) {
			// Since we'll be shifting everything into this space we can consider it all changed.
			changed.set(writePos, newSize);
		}

		// We definitely shouldn't consider the deleted instances as changed though,
		// else we might try some out of bounds accesses later.
		changed.clear(newSize, oldSize);

		// Punch out the deleted instances, shifting over surviving instances to fill their place.
		for (int scanPos = writePos; (scanPos < oldSize) && (writePos < newSize); scanPos++, writePos++) {
			// Find next non-deleted element.
			scanPos = deleted.nextClearBit(scanPos);

			if (scanPos != writePos) {
				// Grab the old instance/handle from scanPos...
				var handle = handles.get(scanPos);
				I instance = instances.get(scanPos);

				// ... and move it to writePos.
				handles.set(writePos, handle);
				instances.set(writePos, instance);

				// Make sure the handle knows it's been moved
				handle.index = writePos;
			}
		}

		deleted.clear();
		instances.subList(newSize, oldSize)
				.clear();
		handles.subList(newSize, oldSize)
				.clear();
	}

	public void delete() {
		if (vbo == null) {
			return;
		}
		vbo.delete();
		vbo = null;

		for (ClrwlInstancedDraw instancedDraw : draws) {
			instancedDraw.delete();
		}
	}

	public void addDrawCall(ClrwlInstancedDraw instancedDraw) {
		draws.add(instancedDraw);
	}

	public void bind(TextureBuffer buffer) {
		if (vbo == null) {
			return;
		}

		buffer.bind(vbo.handle());
	}
}
