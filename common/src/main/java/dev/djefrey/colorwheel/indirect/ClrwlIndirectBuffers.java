package dev.djefrey.colorwheel.indirect;

import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER;
import static org.lwjgl.opengl.GL44.nglBindBuffersRange;

import dev.engine_room.flywheel.backend.engine.indirect.ObjectStorage;
import dev.engine_room.flywheel.backend.engine.indirect.ResizableStorageArray;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Pointer;

import dev.engine_room.flywheel.backend.gl.buffer.GlBufferType;
import dev.engine_room.flywheel.lib.memory.MemoryBlock;

public class ClrwlIndirectBuffers
{
    // Number of vbos created.
    public static final int BUFFER_COUNT = 5;

    public static final long INT_SIZE = Integer.BYTES;
    public static final long PTR_SIZE = Pointer.POINTER_SIZE;

    public static final long MODEL_STRIDE = 28 + 8;

    // Byte size of a draw command, plus our added mesh data.
    public static final long DRAW_COMMAND_STRIDE = 36 + 12;
    public static final long DRAW_COMMAND_OFFSET = 0;

    // Offsets to the 3 segments
    private static final long HANDLE_OFFSET = 0;
    private static final long OFFSET_OFFSET = BUFFER_COUNT * INT_SIZE;
    private static final long SIZE_OFFSET = OFFSET_OFFSET + BUFFER_COUNT * PTR_SIZE;
    // Total size of the buffer.
    private static final long BUFFERS_SIZE_BYTES = SIZE_OFFSET + BUFFER_COUNT * PTR_SIZE;

    // Offsets to the vbos
    private static final long PAGE_FRAME_DESCRIPTOR_HANDLE_OFFSET = HANDLE_OFFSET + ClrwlBufferBindings.PAGE_FRAME_DESCRIPTOR * INT_SIZE;
    private static final long INSTANCE_HANDLE_OFFSET = HANDLE_OFFSET + ClrwlBufferBindings.INSTANCE * INT_SIZE;
    private static final long DRAW_INSTANCE_INDEX_HANDLE_OFFSET = HANDLE_OFFSET + ClrwlBufferBindings.DRAW_INSTANCE_INDEX * INT_SIZE;
    private static final long MODEL_HANDLE_OFFSET = HANDLE_OFFSET + ClrwlBufferBindings.MODEL * INT_SIZE;
    private static final long DRAW_HANDLE_OFFSET = HANDLE_OFFSET + ClrwlBufferBindings.DRAW * INT_SIZE;

    // Offsets to the sizes
    private static final long PAGE_FRAME_DESCRIPTOR_SIZE_OFFSET = SIZE_OFFSET + ClrwlBufferBindings.PAGE_FRAME_DESCRIPTOR * PTR_SIZE;
    private static final long INSTANCE_SIZE_OFFSET = SIZE_OFFSET + ClrwlBufferBindings.INSTANCE * PTR_SIZE;
    private static final long DRAW_INSTANCE_INDEX_SIZE_OFFSET = SIZE_OFFSET + ClrwlBufferBindings.DRAW_INSTANCE_INDEX * PTR_SIZE;
    private static final long MODEL_SIZE_OFFSET = SIZE_OFFSET + ClrwlBufferBindings.MODEL * PTR_SIZE;
    private static final long DRAW_SIZE_OFFSET = SIZE_OFFSET + ClrwlBufferBindings.DRAW * PTR_SIZE;


    private static final float INSTANCE_GROWTH_FACTOR = 1.25f;
    private static final float MODEL_GROWTH_FACTOR = 2f;
    private static final float DRAW_GROWTH_FACTOR = 2f;

    /**
     * A small block of memory divided into 3 contiguous segments:
     * <br>
     * {@code buffers}: an array of {@link dev.djefrey.colorwheel.indirect.ClrwlIndirectBuffers#INT_SIZE} buffer handles.
     * <br>
     * {@code offsets}: an array of {@link dev.djefrey.colorwheel.indirect.ClrwlIndirectBuffers#PTR_SIZE} offsets into the buffers, currently just zeroed.
     * <br>
     * {@code sizes}: an array of {@link dev.djefrey.colorwheel.indirect.ClrwlIndirectBuffers#PTR_SIZE} byte lengths of the buffers.
     * <br>
     * Each segment stores {@link dev.djefrey.colorwheel.indirect.ClrwlIndirectBuffers#BUFFER_COUNT} elements,
     * one for the instance buffer, target buffer, model index buffer, model buffer, and draw buffer.
     */
    private final MemoryBlock multiBindBlock;

    public final ObjectStorage objectStorage;
    public final ResizableStorageArray drawInstanceIndex;
    public final ResizableStorageArray model;
    public final ResizableStorageArray draw;

    public ClrwlIndirectBuffers(long instanceStride)
    {
        this.multiBindBlock = MemoryBlock.calloc(BUFFERS_SIZE_BYTES, 1);

        objectStorage = new ObjectStorage(instanceStride);
        drawInstanceIndex = new ResizableStorageArray(INT_SIZE, INSTANCE_GROWTH_FACTOR);
        model = new ResizableStorageArray(MODEL_STRIDE, MODEL_GROWTH_FACTOR);
        draw = new ResizableStorageArray(DRAW_COMMAND_STRIDE, DRAW_GROWTH_FACTOR);
    }

    public void updateCounts(int instanceCount, int modelCount, int drawCount)
    {
        final long ptr = multiBindBlock.ptr();

        drawInstanceIndex.ensureCapacity(instanceCount);
        model.ensureCapacity(modelCount);
        draw.ensureCapacity(drawCount);

        MemoryUtil.memPutInt(ptr + PAGE_FRAME_DESCRIPTOR_HANDLE_OFFSET, objectStorage.frameDescriptorBuffer.handle());
        MemoryUtil.memPutInt(ptr + INSTANCE_HANDLE_OFFSET, objectStorage.objectBuffer.handle());
        MemoryUtil.memPutInt(ptr + DRAW_INSTANCE_INDEX_HANDLE_OFFSET, drawInstanceIndex.handle());
        MemoryUtil.memPutInt(ptr + MODEL_HANDLE_OFFSET, model.handle());
        MemoryUtil.memPutInt(ptr + DRAW_HANDLE_OFFSET, draw.handle());

        MemoryUtil.memPutAddress(ptr + PAGE_FRAME_DESCRIPTOR_SIZE_OFFSET, objectStorage.frameDescriptorBuffer.capacity());
        MemoryUtil.memPutAddress(ptr + INSTANCE_SIZE_OFFSET, objectStorage.objectBuffer.capacity());
        MemoryUtil.memPutAddress(ptr + DRAW_INSTANCE_INDEX_SIZE_OFFSET, INT_SIZE * instanceCount);
        MemoryUtil.memPutAddress(ptr + MODEL_SIZE_OFFSET, MODEL_STRIDE * modelCount);
        MemoryUtil.memPutAddress(ptr + DRAW_SIZE_OFFSET, DRAW_COMMAND_STRIDE * drawCount);
    }

    public void bindForCull()
    {
        multiBind(0, 4);
    }

    public void bindForApply()
    {
        multiBind(3, 2);
    }

    public void bindForDraw()
    {
        multiBind(1, 4);
        GlBufferType.DRAW_INDIRECT_BUFFER.bind(draw.handle());
    }

    public void bindForCrumbling()
    {
        // All we need is the instance buffer. Crumbling uses its own draw buffer.
        multiBind(ClrwlBufferBindings.INSTANCE, 1);
    }

    private void multiBind(int base, int count)
    {
        final long ptr = multiBindBlock.ptr();
        nglBindBuffersRange(GL_SHADER_STORAGE_BUFFER, base, count, ptr + base * INT_SIZE, ptr + OFFSET_OFFSET + base * PTR_SIZE, ptr + SIZE_OFFSET + base * PTR_SIZE);
    }

    public void delete()
    {
        multiBindBlock.free();

        objectStorage.delete();
        drawInstanceIndex.delete();
        model.delete();
        draw.delete();
    }
}
