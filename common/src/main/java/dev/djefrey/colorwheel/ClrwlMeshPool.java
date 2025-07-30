package dev.djefrey.colorwheel;

import dev.engine_room.flywheel.api.model.Mesh;
import dev.engine_room.flywheel.backend.engine.IndexPool;
import dev.engine_room.flywheel.backend.gl.GlPrimitive;
import dev.engine_room.flywheel.backend.gl.array.GlVertexArray;
import dev.engine_room.flywheel.backend.gl.buffer.GlBuffer;
import dev.engine_room.flywheel.backend.gl.buffer.GlBufferUsage;
import dev.engine_room.flywheel.backend.util.ReferenceCounted;
import dev.engine_room.flywheel.lib.memory.MemoryBlock;
import dev.engine_room.flywheel.lib.model.QuadMesh;
import dev.engine_room.flywheel.lib.model.RetexturedMesh;
import net.irisshaders.iris.vertices.NormalHelper;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4fc;
import org.lwjgl.opengl.GL32;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Copy/Paste from MeshPool
// Changes are:
// - use of ColorwheelVertex instead of InternalVertex
// - tangents and midTextCoord being computed on upload
public class ClrwlMeshPool {
	private final ClrwlVertexView vertexView;
	private final Map<Mesh, PooledMesh> meshes = new HashMap<>();
	private final List<PooledMesh> meshList = new ArrayList<>();
	private final List<PooledMesh> recentlyAllocated = new ArrayList<>();

	private final GlBuffer vbo;
	private final IndexPool indexPool;

	private boolean dirty;
	private boolean anyToRemove;

	/**
	 * Create a new mesh pool.
	 */
	public ClrwlMeshPool() {
		vertexView = ClrwlVertex.createVertexView();
		vbo = new GlBuffer(GlBufferUsage.DYNAMIC_DRAW);
		indexPool = new IndexPool();
	}

	/**
	 * Allocate a model in the arena.
	 *
	 * @param mesh The model to allocate.
	 * @return A handle to the allocated model.
	 */
	public PooledMesh alloc(Mesh mesh) {
		return meshes.computeIfAbsent(mesh, this::_alloc);
	}

	private PooledMesh _alloc(Mesh m) {
		PooledMesh bufferedModel = new PooledMesh(m);
		meshList.add(bufferedModel);
		recentlyAllocated.add(bufferedModel);

		dirty = true;
		return bufferedModel;
	}

	@Nullable
	public ClrwlMeshPool.PooledMesh get(Mesh mesh) {
		return meshes.get(mesh);
	}

	public void flush() {
        if (!dirty) {
            return;
        }

		if (anyToRemove) {
			anyToRemove = false;
			processDeletions();
		}

		if (!recentlyAllocated.isEmpty()) {
			// Otherwise, just update the index with the new counts.
			for (PooledMesh mesh : recentlyAllocated) {
				indexPool.updateCount(mesh.mesh.indexSequence(), mesh.indexCount());
			}
			indexPool.flush();
			recentlyAllocated.clear();
		}

		uploadAll();
        dirty = false;
    }

	private void processDeletions() {
		// remove deleted meshes
		meshList.removeIf(pooledMesh -> {
			boolean deleted = pooledMesh.isDeleted();
			if (deleted) {
				meshes.remove(pooledMesh.mesh);
			}
			return deleted;
		});
	}

	private void uploadAll() {
		long neededSize = 0;
		for (PooledMesh mesh : meshList) {
			neededSize += mesh.byteSize();
		}

		final var vertexBlock = MemoryBlock.malloc(neededSize);
		final long vertexPtr = vertexBlock.ptr();

		int byteIndex = 0;
		int baseVertex = 0;
		for (PooledMesh mesh : meshList) {
			mesh.baseVertex = baseVertex;

			vertexView.ptr(vertexPtr + byteIndex);
			vertexView.vertexCount(mesh.vertexCount());
			mesh.mesh.write(vertexView);

			Mesh baseMesh = mesh.mesh;

			while (baseMesh instanceof RetexturedMesh retextured)
			{
				baseMesh = retextured.mesh();
			}

			if (baseMesh instanceof QuadMesh quad)
			{
				computeExtendedQuadData(quad, vertexView);
			}
			else
			{
				computeExtendedData(baseMesh, vertexView);
			}

			byteIndex += mesh.byteSize();
			baseVertex += mesh.vertexCount();
		}

		vbo.upload(vertexBlock);

		vertexBlock.free();
	}

	private void computeExtendedQuadData(QuadMesh mesh, ClrwlVertexView vertexView)
	{
		var quadCnt = mesh.vertexCount() / 4;

		for (int i = 0; i < quadCnt; i++)
		{
			int base = i * 4;

			float normalX = 0;
			float normalY = 0;
			float normalZ = 0;

			float midU = 0;
			float midV = 0;

			for (int vId = 0; vId < 4; vId++)
			{
				int idx = base + vId;

				normalX += vertexView.normalX(idx);
				normalY += vertexView.normalY(idx);
				normalZ += vertexView.normalZ(idx);

				midU += vertexView.u(idx);
				midV += vertexView.v(idx);
			}

			normalX /= 4.0F;
			normalY /= 4.0F;
			normalZ /= 4.0F;

			midU /= 4.0F;
			midV /= 4.0F;

			int tangent = NormalHelper.computeTangent(normalX, normalY, normalZ,
										vertexView.x(base + 0), vertexView.y(base + 0), vertexView.z(base + 0), vertexView.u(base + 0), vertexView.v(base + 0),
										vertexView.x(base + 1), vertexView.y(base + 1), vertexView.z(base + 1), vertexView.u(base + 1), vertexView.v(base + 1),
										vertexView.x(base + 2), vertexView.y(base + 2), vertexView.z(base + 2), vertexView.u(base + 2), vertexView.v(base + 2));

			for (int vId = 0; vId < 4; vId++)
			{
				vertexView.packedTangent(base + vId, tangent);
				vertexView.midU(base + vId, midU);
				vertexView.midV(base + vId, midV);
			}
		}
	}

	private void computeExtendedData(Mesh mesh, ClrwlVertexView vertexView)
	{
		// TODO

		for (int i = 0; i < mesh.vertexCount(); i++)
		{
			vertexView.packedTangent(i, 0);
			vertexView.midU(i, 0);
			vertexView.midV(i, 0);
		}
	}

	public void bind(GlVertexArray vertexArray) {
		indexPool.bind(vertexArray);
		vertexArray.bindVertexBuffer(0, vbo.handle(), 0, ClrwlVertex.STRIDE);
		vertexArray.bindAttributes(0, 0, ClrwlVertex.ATTRIBUTES);
	}

	public void delete() {
		vbo.delete();
		indexPool.delete();
		meshes.clear();
		meshList.clear();
	}

	public class PooledMesh extends ReferenceCounted {
		public static final int INVALID_BASE_VERTEX = -1;

		private final Mesh mesh;
		private int baseVertex = INVALID_BASE_VERTEX;

		private PooledMesh(Mesh mesh) {
			this.mesh = mesh;
		}

		public int vertexCount() {
			return mesh.vertexCount();
		}

		public int byteSize() {
			return mesh.vertexCount() * ClrwlVertex.STRIDE;
		}

		public int indexCount() {
			return mesh.indexCount();
		}

		public int baseVertex() {
			return baseVertex;
		}

		public int firstIndex() {
			return ClrwlMeshPool.this.indexPool.firstIndex(mesh.indexSequence());
		}

		public long firstIndexByteOffset() {
			return (long) firstIndex() * Integer.BYTES;
		}

		public boolean isInvalid() {
			return mesh.vertexCount() == 0 || baseVertex == INVALID_BASE_VERTEX || isDeleted();
		}

		public Vector4fc boundingSphere()
		{
			return mesh.boundingSphere();
		}

		public void draw(int instanceCount) {
			if (instanceCount > 1) {
				GL32.glDrawElementsInstancedBaseVertex(GlPrimitive.TRIANGLES.glEnum, mesh.indexCount(), GL32.GL_UNSIGNED_INT, firstIndexByteOffset(), instanceCount, baseVertex);
			} else {
				GL32.glDrawElementsBaseVertex(GlPrimitive.TRIANGLES.glEnum, mesh.indexCount(), GL32.GL_UNSIGNED_INT, firstIndexByteOffset(), baseVertex);
			}
		}

		@Override
		protected void _delete() {
			ClrwlMeshPool.this.dirty = true;
			ClrwlMeshPool.this.anyToRemove = true;
		}
	}
}
