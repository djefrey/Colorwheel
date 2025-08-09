package dev.djefrey.colorwheel.instancing;

import dev.djefrey.colorwheel.engine.ClrwlMeshPool;
import dev.djefrey.colorwheel.engine.ClrwlInstanceVisual;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.backend.engine.GroupKey;
import dev.engine_room.flywheel.backend.gl.TextureBuffer;

public class ClrwlInstancedDraw {
	public final GroupKey<?> groupKey;
	private final ClrwlInstancedInstancer<?> instancer;
	private final ClrwlMeshPool.PooledMesh mesh;
	private final Material material;
	private final int bias;
	private final int indexOfMeshInModel;

	private boolean deleted;

	public ClrwlInstancedDraw(ClrwlInstancedInstancer<?> instancer, ClrwlMeshPool.PooledMesh mesh, GroupKey<?> groupKey, Material material, int bias, int indexOfMeshInModel) {
		this.instancer = instancer;
		this.mesh = mesh;
		this.groupKey = groupKey;
		this.material = material;
		this.bias = bias;
		this.indexOfMeshInModel = indexOfMeshInModel;

		mesh.acquire();
	}

	public ClrwlInstanceVisual visual() { return instancer.visual; }

	public int bias() {
		return bias;
	}

	public int indexOfMeshInModel() {
		return indexOfMeshInModel;
	}

	public Material material() {
		return material;
	}

	public boolean deleted() {
		return deleted;
	}

	public ClrwlMeshPool.PooledMesh mesh() {
		return mesh;
	}

	public void render(TextureBuffer buffer) {
		if (mesh.isInvalid()) {
			return;
		}

		instancer.bind(buffer);

		mesh.draw(instancer.instanceCount());
	}

	public void renderOne(TextureBuffer buffer) {
		if (mesh.isInvalid()) {
			return;
		}

		instancer.bind(buffer);

		mesh.draw(1);
	}

	public void delete() {
		if (deleted) {
			return;
		}

		mesh.release();

		deleted = true;
	}
}
