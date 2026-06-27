package dev.djefrey.colorwheel.engine.uniform;

import dev.djefrey.colorwheel.engine.ShadowRenderContext;
import dev.djefrey.colorwheel.indirect.ClrwlDepthPyramid;
import dev.engine_room.flywheel.api.backend.RenderContext;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.backend.engine.indirect.DepthPyramid;
import dev.engine_room.flywheel.backend.engine.uniform.UniformBuffer;
import dev.engine_room.flywheel.backend.mixin.LevelRendererAccessor;
import dev.engine_room.flywheel.lib.instance.PosedInstance;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.util.ExtraMemoryOps;
import net.irisshaders.iris.shaderpack.ShaderPack;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.irisshaders.iris.shaderpack.properties.PackShadowDirectives;
import net.irisshaders.iris.shadows.ShadowMatrices;
import net.irisshaders.iris.shadows.frustum.advanced.BaseClippingPlanes;
import net.irisshaders.iris.shadows.frustum.advanced.NeighboringPlaneSet;
import net.irisshaders.iris.uniforms.CelestialUniforms;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.*;
import org.joml.Math;
import org.lwjgl.system.MemoryUtil;

import java.util.ArrayList;
import java.util.List;

public final class ClrwlShadowPassUniforms extends UniformWriter
{
	private static final int SIZE = 12 * 16				// Frustum
								  + 32 	        		// Cull
								  + 64 * 9      		// View + Projection
								  + 64 * 4      		// Shadow View + Projection
								  + 48 		    		// Normal
								  + 6 * 8;     			// Remaining

	public static final UniformBuffer BUFFER = new UniformBuffer(ClrwlUniforms.PASS_INDEX, SIZE);

	private static final Vector4f ZERO_V4F = new Vector4f(0.0f);

	private static final Matrix4f VIEW = new Matrix4f();
	private static final Matrix4f VIEW_INVERSE = new Matrix4f();
	private static final Matrix4f VIEW_PREV = new Matrix4f();
	private static final Matrix4f PROJECTION = new Matrix4f();
	private static final Matrix4f PROJECTION_INVERSE = new Matrix4f();
	private static final Matrix4f PROJECTION_PREV = new Matrix4f();
	private static final Matrix4f VIEW_PROJECTION = new Matrix4f();
	private static final Matrix4f VIEW_PROJECTION_INVERSE = new Matrix4f();
	private static final Matrix4f VIEW_PROJECTION_PREV = new Matrix4f();

	private static final Matrix3f NORMAL = new Matrix3f();

	private static boolean firstWrite = true;

	private static boolean frustumPaused = false;
	private static boolean frustumCapture = false;
	private static Matrix4fc capturedPlayerProjView = null;

	private ClrwlShadowPassUniforms() {
	}

	public static void captureFrustum()
	{
		frustumPaused = true;
		frustumCapture = true;
	}

	public static void unpauseFrustum()
	{
		frustumPaused = false;
		capturedPlayerProjView = null;
	}

	public static void update(ShadowRenderContext context, ShaderPack pack, NamespacedId dimension)
	{
		var directives = pack.getProgramSet(dimension).getPackDirectives();
		var shadowDirectives = directives.getShadowDirectives();

		long ptr = BUFFER.ptr();
		setPrev();

		Vec3i renderOrigin = VisualizationManager.getOrThrow(context.level())
				.renderOrigin();
		var camera = context.camera();
		var camX = (context.camX() - renderOrigin.getX());
		var camY = (context.camY() - renderOrigin.getY());
		var camZ = (context.camZ() - renderOrigin.getZ());

		int resolution = shadowDirectives.getResolution();
		float zNear;
		float zFar;

		if (shadowDirectives.getFov() != null)
		{
			zNear = ShadowMatrices.NEAR;
			zFar = ShadowMatrices.FAR;
		}
		else
		{
			zNear = shadowDirectives.getNearPlane();
			zFar = shadowDirectives.getFarPlane();
		}

		VIEW.set(context.modelView());
		VIEW.translate(-camX, -camY, -camZ);
		VIEW.invert(VIEW_INVERSE);
		PROJECTION.set(context.projection());
		PROJECTION.invert(PROJECTION_INVERSE);
		VIEW_PROJECTION.set(context.viewProjection());
		VIEW_PROJECTION.translate(-camX, -camY, -camZ);
		VIEW_PROJECTION.invert(VIEW_PROJECTION_INVERSE);

		Matrix4f normal = new Matrix4f(context.modelView())
				.translate(-camX, -camY, -camZ)
				.invert()
				.transpose();
		normal.get3x3(NORMAL);

		var playerViewProj = context.playerViewProjection()
				.translate(-camX, -camY, -camZ, new Matrix4f());

		var sunPathRotation = directives.getSunPathRotation();
		Vector4f shadowLightPosition = new CelestialUniforms(sunPathRotation).getShadowLightPositionInWorldSpace();
		Vector3f shadowLightVectorFromOrigin = new Vector3f(shadowLightPosition.x(), shadowLightPosition.y(), shadowLightPosition.z());

		if (firstWrite)
		{
			setPrev();
		}

		if (frustumCapture)
		{
			capturedPlayerProjView = playerViewProj;
			frustumCapture = false;
		}

		writePackedFrustumPlanes(ptr, shadowLightVectorFromOrigin, !frustumPaused ? playerViewProj : capturedPlayerProjView);
		ptr += 12 * 16;

		ptr = writeCullData(ptr, resolution, zNear, zFar);

		ptr = writeMatrices(ptr);

		var window = Minecraft.getInstance().getWindow();
		ptr = writeVec2(ptr, resolution, resolution);
		ptr = writeFloat(ptr, 1.0f);
		// default line width: net.minecraft.client.renderer.RenderStateShard.LineStateShard
		ptr = writeFloat(ptr, Math.max(2.5F, (float) window.getWidth() / 1920.0F * 2.5F));
		ptr = writeFloat(ptr, zFar);

		ptr = writeTime(ptr, context);

		firstWrite = false;
		BUFFER.markDirty();
	}

	private static void setPrev()
	{
		VIEW_PREV.set(VIEW);
		PROJECTION_PREV.set(PROJECTION);
		VIEW_PROJECTION_PREV.set(VIEW_PROJECTION);
	}

	private static long writeMatrices(long ptr)
	{
		ptr = writeMat4(ptr, VIEW);
		ptr = writeMat4(ptr, VIEW_INVERSE);
		ptr = writeMat4(ptr, VIEW_PREV);
		ptr = writeMat4(ptr, PROJECTION);
		ptr = writeMat4(ptr, PROJECTION_INVERSE);
		ptr = writeMat4(ptr, PROJECTION_PREV);
		ptr = writeMat4(ptr, VIEW_PROJECTION);
		ptr = writeMat4(ptr, VIEW_PROJECTION_INVERSE);
		ptr = writeMat4(ptr, VIEW_PROJECTION_PREV);
		ptr = writeMat4(ptr, VIEW); // Shadow Matrices
		ptr = writeMat4(ptr, VIEW_INVERSE);
		ptr = writeMat4(ptr, PROJECTION);
		ptr = writeMat4(ptr, PROJECTION_INVERSE);
		ptr = writeMat3(ptr, NORMAL);
		return ptr;
	}

	private static long writeTime(long ptr, RenderContext context)
	{
		int ticks = ((LevelRendererAccessor) context.renderer()).flywheel$getTicks();
		float partialTick = context.partialTick();
		float renderTicks = ticks + partialTick;
		float renderSeconds = renderTicks / 20f;
		float systemSeconds = Util.getMillis() / 1000f;
		int systemMillis = (int) (Util.getMillis() % Integer.MAX_VALUE);

		ptr = writeInt(ptr, ticks);
		ptr = writeFloat(ptr, partialTick);
		ptr = writeFloat(ptr, renderTicks);
		ptr = writeFloat(ptr, renderSeconds);
		ptr = writeFloat(ptr, systemSeconds);
		ptr = writeInt(ptr, systemMillis);
		return ptr;
	}

	private static long writeCullData(long ptr, int resolution, float zNear, float zFar)
	{
		int pyramidRes = DepthPyramid.mip0Size(resolution);
		int pyramidDepth = DepthPyramid.getImageMipLevels(pyramidRes, pyramidRes);

		ptr = writeFloat(ptr, zNear);
		ptr = writeFloat(ptr, zFar);
		ptr = writeFloat(ptr, PROJECTION.m00()); // P00
		ptr = writeFloat(ptr, PROJECTION.m11()); // P11
		ptr = writeFloat(ptr, pyramidRes); // pyramidWidth
		ptr = writeFloat(ptr, pyramidRes); // pyramidHeight
		ptr = writeInt(ptr, pyramidDepth - 1); // pyramidLevels
		ptr = writeInt(ptr, 0); // useMin

		return ptr;
	}

	private static Vector3f getPlaneNormal(Vector4f plane)
	{
		return new Vector3f(plane.x(), plane.y(), plane.z());
	}

	private static Vector4f normalizePlane(Vector4f plane)
	{
		float scale = Math.invsqrt(plane.x * plane.x + plane.y * plane.y + plane.z * plane.z);
		plane.x *= scale;
		plane.y *= scale;
		plane.z *= scale;
		plane.w *= scale;
		return plane;
	}

	private static Vector4f processNeighborPlane(Vector4f plane, Vector4f backPlane, Vector3f lightFromOrigin)
	{
		var planeNormal = getPlaneNormal(plane);
		var backPlaneNormal = getPlaneNormal(backPlane);
		var intersection =  backPlaneNormal.cross(planeNormal, new Vector3f());
		var edgePlaneNormal = intersection.cross(lightFromOrigin, new Vector3f());

		Vector3f point;
		{
			// "Line of intersection between two planes"
			// https://stackoverflow.com/a/32410473 by ideasman42, CC BY-SA 3.0
			// (a modified version of "Intersection of 2-planes" from Graphics Gems 1, page 305
			var ixb = intersection.cross(backPlaneNormal, new Vector3f());
			var fxi = planeNormal.cross(intersection, new Vector3f());

			ixb.mul(-plane.w());
			fxi.mul(-backPlane.w());

			ixb.add(fxi);

			point = ixb;
			point.mul(1.0F / intersection.lengthSquared());
		}

		float d = edgePlaneNormal.dot(point);
		return normalizePlane(new Vector4f(edgePlaneNormal.x(), edgePlaneNormal.y(), edgePlaneNormal.z(), -d));
	}

	// planes MUST have at least 11 entries
	public static int computeFrustumPlanes(Vector3f lightFromOrigin, Matrix4fc playerViewProj, Vector4f[] frustumPlanes)
	{
		Vector4f[] planes =
		{
			normalizePlane(playerViewProj.frustumPlane(Matrix4fc.PLANE_NX, new Vector4f())),
			normalizePlane(playerViewProj.frustumPlane(Matrix4fc.PLANE_PX, new Vector4f())),
			normalizePlane(playerViewProj.frustumPlane(Matrix4fc.PLANE_NY, new Vector4f())),
			normalizePlane(playerViewProj.frustumPlane(Matrix4fc.PLANE_PY, new Vector4f())),
			normalizePlane(playerViewProj.frustumPlane(Matrix4fc.PLANE_NZ, new Vector4f())),
			normalizePlane(playerViewProj.frustumPlane(Matrix4fc.PLANE_PZ, new Vector4f())),
		};

		boolean[] isBack = new boolean[6];
		boolean[] isBackOrEdge = new boolean[6];
		int frustumPlaneCount = 0;

		for (int i = 0; i < 6; i++)
		{
			var plane = planes[i];
			var normal = getPlaneNormal(plane);
			var dot = lightFromOrigin.dot(normal);

			var isBackPlane = dot > 0.0f;
			var isEdgePlane = dot == 0.0f;

			isBack[i] = isBackPlane;
			isBackOrEdge[i] = isBackPlane || isEdgePlane;

			if (isBackPlane || isEdgePlane)
			{
				frustumPlanes[frustumPlaneCount] = plane;
				frustumPlaneCount++;
			}
		}

		for (int i = 0; i < 6; i++)
		{
			if (!isBack[i])
			{
				continue;
			}

			var backPlane = planes[i];
			var neighbors = NeighboringPlaneSet.forPlane(i);

			if (!isBackOrEdge[neighbors.plane0()])
			{
				frustumPlanes[frustumPlaneCount] = processNeighborPlane(planes[neighbors.plane0()], backPlane, lightFromOrigin);
				frustumPlaneCount++;
			}

			if (!isBackOrEdge[neighbors.plane1()])
			{
				frustumPlanes[frustumPlaneCount] = processNeighborPlane(planes[neighbors.plane1()], backPlane, lightFromOrigin);
				frustumPlaneCount++;
			}

			if (!isBackOrEdge[neighbors.plane2()])
			{
				frustumPlanes[frustumPlaneCount] = processNeighborPlane(planes[neighbors.plane2()], backPlane, lightFromOrigin);
				frustumPlaneCount++;
			}

			if (!isBackOrEdge[neighbors.plane3()])
			{
				frustumPlanes[frustumPlaneCount] = processNeighborPlane(planes[neighbors.plane3()], backPlane, lightFromOrigin);
				frustumPlaneCount++;
			}
		}

		return frustumPlaneCount;
	}

	// Derived from Iris' Advanced Shadow Culling Frustum
	// https://github.com/IrisShaders/Iris/blob/37c020371845f1426a65d4e8615a078024cc4b02/common/src/main/java/net/irisshaders/iris/shadows/frustum/advanced/AdvancedShadowCullingFrustum.java
	private static void writePackedFrustumPlanes(long ptr, Vector3f shadowLightVectorFromOrigin, Matrix4fc playerViewProj)
	{
		Vector4f[] frustumPlanes = new Vector4f[11];
		int frustumPlaneCount = computeFrustumPlanes(shadowLightVectorFromOrigin, playerViewProj, frustumPlanes);

		for (int i = 0; i < 12; i++)
		{
			var plane = i < frustumPlaneCount ? frustumPlanes[i] : ZERO_V4F;
			var idx = i / 4;
			var subidx = i % 4;

			MemoryUtil.memPutFloat(ptr + idx * 64L + subidx * 4L +  0L, plane.x());
			MemoryUtil.memPutFloat(ptr + idx * 64L + subidx * 4L + 16L, plane.y());
			MemoryUtil.memPutFloat(ptr + idx * 64L + subidx * 4L + 32L, plane.z());
			MemoryUtil.memPutFloat(ptr + idx * 64L + subidx * 4L + 48L, plane.w());
		}
	}
}
