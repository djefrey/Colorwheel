package dev.djefrey.colorwheel.neoforge.mod_compat.sable;

import dev.djefrey.colorwheel.engine.ClrwlEngine;
import dev.djefrey.colorwheel.engine.ClrwlInstanceVisual;
import dev.djefrey.colorwheel.engine.embed.EmbeddedEnvironment;
import dev.engine_room.flywheel.backend.gl.shader.GlProgram;
import dev.engine_room.flywheel.lib.util.ExtraMemoryOps;
import dev.ryanhcode.sable.neoforge.compatibility.flywheel.SableFlywheelEmbeddingUniforms;
import dev.ryanhcode.sable.neoforge.compatibility.flywheel.SableFlywheelLightStorage;
import dev.ryanhcode.sable.neoforge.mixinterface.compatibility.flywheel.EmbeddedEnvironmentExtension;
import net.minecraft.core.Vec3i;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.system.MemoryUtil;

public class SableEmbeddedEnvironment extends EmbeddedEnvironment implements EmbeddedEnvironmentExtension
{
    public static final int MATRIX_SIZE_BYTES = EmbeddedEnvironment.MATRIX_SIZE_BYTES
            + Float.BYTES // sky light scale
            + Integer.BYTES // scene ID
            + 2 * Float.BYTES // padding
            + 16 * Float.BYTES; // scene matrix

    private final Matrix4f scene = new Matrix4f();
    private int sceneId = SableFlywheelLightStorage.STATIC_SCENE_ID;
    private float skyLightScale = 1.0f;

    public SableEmbeddedEnvironment(ClrwlEngine engine, ClrwlInstanceVisual visual, Vec3i renderOrigin, @Nullable EmbeddedEnvironment parent)
    {
        super(engine, visual, renderOrigin, parent);
    }

    @Override
    public void sable$setLightingInfo(Matrix4fc sceneMatrix, int scene, float skyLightScale)
    {
        this.scene.set(sceneMatrix);
        this.sceneId = scene;
        this.skyLightScale = skyLightScale;
    }

    @Override
    public void setupDraw(GlProgram program)
    {
        super.setupDraw(program);

        program.setUInt(SableFlywheelEmbeddingUniforms.SCENE, this.sceneId);
        program.setFloat(SableFlywheelEmbeddingUniforms.SKY_LIGHT_SCALE, this.skyLightScale);

        if (this.sceneId == 0)
        {
            program.setMat4(SableFlywheelEmbeddingUniforms.SCENE_MATRIX, this.poseComposed);
        }
        else
        {
            program.setMat4(SableFlywheelEmbeddingUniforms.SCENE_MATRIX, this.scene);
        }
    }

    @Override
    public void flush(long ptr)
    {
        super.flush(ptr);

        MemoryUtil.memPutFloat(ptr + 28 * Float.BYTES, this.skyLightScale);
        MemoryUtil.memPutInt(ptr + 29 * Float.BYTES, this.sceneId);
        MemoryUtil.memPutFloat(ptr + 30 * Float.BYTES, 0);
        MemoryUtil.memPutFloat(ptr + 31 * Float.BYTES, 0);

        final long sceneMatrixOffset = ptr + 32 * Float.BYTES;

        if (this.sceneId == 0)
        {
            ExtraMemoryOps.putMatrix4f(sceneMatrixOffset, this.poseComposed);
        }
        else
        {
            ExtraMemoryOps.putMatrix4f(sceneMatrixOffset, this.scene);
        }
    }
}
