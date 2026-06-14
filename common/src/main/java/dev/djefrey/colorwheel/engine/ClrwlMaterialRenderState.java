package dev.djefrey.colorwheel.engine;

import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import dev.engine_room.flywheel.api.material.DepthTest;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.backend.Samplers;
import dev.engine_room.flywheel.backend.engine.TextureBinder;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gl.blending.BufferBlendInformation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.util.Comparator;
import java.util.List;

public final class ClrwlMaterialRenderState
{
    public static final Comparator<Material> COMPARATOR = ClrwlMaterialRenderState::compare;

    private ClrwlMaterialRenderState() {
    }

    public static void setup(Material material, @Nullable ClrwlBlendModeOverride blendOverride, List<BufferBlendInformation> bufferBlendOverrides)
    {
        setupTexture(material);
        setupBackfaceCulling(material.backfaceCulling());
        setupPolygonOffset(material.polygonOffset());
        setupDepthTest(material.depthTest());
        setupTransparency(material.transparency(), blendOverride, bufferBlendOverrides);
        setupWriteMask(material.writeMask());
    }

    public static void setupOit(Material material)
    {
        setupTexture(material);
        setupBackfaceCulling(material.backfaceCulling());
        setupPolygonOffset(material.polygonOffset());
        setupDepthTest(material.depthTest());

        WriteMask mask = material.writeMask();
        boolean writeColor = mask.color();
        GlStateManager._colorMask(writeColor ? ColorTargetState.WRITE_ALL : ColorTargetState.WRITE_NONE);
    }

    private static void setupTexture(Material material)
    {
        AbstractTexture texture = Minecraft.getInstance()
                .getTextureManager()
                .getTexture(material.texture());

        FilterMode filterMode = material.blur() ? FilterMode.LINEAR : FilterMode.NEAREST;
        GpuSampler defaultSampler = texture.getSampler();
        GpuSampler sampler = RenderSystem.getSamplerCache()
                .getSampler(defaultSampler.getAddressModeU(), defaultSampler.getAddressModeV(), filterMode, filterMode, material.mipmap());

        TextureBinder.bind(Samplers.DIFFUSE.number, texture.getTextureView(), sampler);
    }

    private static void setupBackfaceCulling(boolean backfaceCulling)
    {
        if (backfaceCulling)
        {
            GlStateManager._enableCull();
        }
        else
        {
            GlStateManager._disableCull();
        }
    }

    private static void setupPolygonOffset(boolean polygonOffset)
    {
        if (polygonOffset)
        {
            GlStateManager._polygonOffset(-1.0F, -10.0F);
            GlStateManager._enablePolygonOffset();
        }
        else
        {
            GlStateManager._polygonOffset(0.0F, 0.0F);
            GlStateManager._disablePolygonOffset();
        }
    }

    private static void setupDepthTest(DepthTest depthTest)
    {
        switch (depthTest)
        {
            case OFF -> {
                GlStateManager._disableDepthTest();
            }
            case NEVER -> {
                GlStateManager._enableDepthTest();
                GlStateManager._depthFunc(GL11.GL_NEVER);
            }
            case LESS -> {
                GlStateManager._enableDepthTest();
                GlStateManager._depthFunc(GlConst.GL_LESS);
            }
            case EQUAL -> {
                GlStateManager._enableDepthTest();
                GlStateManager._depthFunc(GlConst.GL_EQUAL);
            }
            case LEQUAL -> {
                GlStateManager._enableDepthTest();
                GlStateManager._depthFunc(GlConst.GL_LEQUAL);
            }
            case GREATER -> {
                GlStateManager._enableDepthTest();
                GlStateManager._depthFunc(GlConst.GL_GREATER);
            }
            case NOTEQUAL -> {
                GlStateManager._enableDepthTest();
                GlStateManager._depthFunc(GL11.GL_NOTEQUAL);
            }
            case GEQUAL -> {
                GlStateManager._enableDepthTest();
                GlStateManager._depthFunc(GlConst.GL_GEQUAL);
            }
            case ALWAYS -> {
                GlStateManager._enableDepthTest();
                GlStateManager._depthFunc(GlConst.GL_ALWAYS);
            }
        }
    }

    private static void setupTransparency(Transparency transparency, @Nullable ClrwlBlendModeOverride blendOverride, List<BufferBlendInformation> bufferBlendOverrides)
    {
        if (blendOverride == null)
        {
            switch (transparency)
            {
                case OPAQUE -> {
                    GlStateManager._disableBlend();
                }
                case ADDITIVE -> {
                    GlStateManager._enableBlend();
                    GlStateManager._blendFuncSeparate(GlConst.GL_ONE, GlConst.GL_ONE, GlConst.GL_ONE, GlConst.GL_ONE);
                }
                case LIGHTNING -> {
                    GlStateManager._enableBlend();
                    GlStateManager._blendFuncSeparate(GlConst.GL_SRC_ALPHA, GlConst.GL_ONE, GlConst.GL_SRC_ALPHA, GlConst.GL_ONE);
                }
                case GLINT -> {
                    GlStateManager._enableBlend();
                    GlStateManager._blendFuncSeparate(GlConst.GL_SRC_COLOR, GlConst.GL_ONE, GlConst.GL_ZERO, GlConst.GL_ONE);
                }
                case CRUMBLING -> {
                    GlStateManager._enableBlend();
                    GlStateManager._blendFuncSeparate(GlConst.GL_DST_COLOR, GlConst.GL_SRC_COLOR, GlConst.GL_ONE, GlConst.GL_ZERO);
                }
                case TRANSLUCENT , ORDER_INDEPENDENT -> {
                    GlStateManager._enableBlend();
                    GlStateManager._blendFuncSeparate(GlConst.GL_SRC_ALPHA, GlConst.GL_ONE_MINUS_SRC_ALPHA, GlConst.GL_ONE, GlConst.GL_ONE_MINUS_SRC_ALPHA);
                }
            }
        }
        else
        {
            if (blendOverride.blendMode() == null)
            {
                GlStateManager._disableBlend();
            }
            else
            {
                GlStateManager._enableBlend();
                GlStateManager._blendFuncSeparate(blendOverride.blendMode().srcRgb(),
                        blendOverride.blendMode().dstRgb(),
                        blendOverride.blendMode().srcAlpha(),
                        blendOverride.blendMode().dstAlpha());
            }
        }

        for (var entry : bufferBlendOverrides)
        {
            if (entry.blendMode() == null)
            {
                IrisRenderSystem.disableBufferBlend(entry.index());
            }
            else
            {
                IrisRenderSystem.enableBufferBlend(entry.index());
                IrisRenderSystem.blendFuncSeparatei(entry.index(),
                        entry.blendMode().srcRgb(),
                        entry.blendMode().dstRgb(),
                        entry.blendMode().srcAlpha(),
                        entry.blendMode().dstAlpha());
            }
        }
    }

    private static void setupWriteMask(WriteMask mask)
    {
        GlStateManager._depthMask(mask.depth());
        boolean writeColor = mask.color();
        GlStateManager._colorMask(writeColor ? ColorTargetState.WRITE_ALL : ColorTargetState.WRITE_NONE);
    }

    public static boolean materialEquals(Material lhs, Material rhs)
    {
        if (lhs == rhs)
        {
            return true;
        }

        // Not here because ubershader: useLight, useOverlay, diffuse, fog shader, ambient occlusion
        // Everything in the comparator should be here.
        // @formatter:off
        return lhs.blur() == rhs.blur()
                && lhs.mipmap() == rhs.mipmap()
                && lhs.backfaceCulling() == rhs.backfaceCulling()
                && lhs.polygonOffset() == rhs.polygonOffset()
                && lhs.depthTest() == rhs.depthTest()
                && lhs.transparency() == rhs.transparency()
                && lhs.writeMask() == rhs.writeMask()
                && lhs.light().source().equals(rhs.light().source())
                && lhs.texture().equals(rhs.texture())
                && lhs.cutout().source().equals(rhs.cutout().source())
                && lhs.shaders().fragmentSource().equals(rhs.shaders().fragmentSource())
                && lhs.shaders().vertexSource().equals(rhs.shaders().vertexSource());
        // @formatter:on
    }

    public static boolean materialIsAllNonNull(@Nullable Material material)
    {
        // We do not trust people to give us valid NotNull objects.
        // @formatter:off
        return material != null &&
                material.shaders() != null &&
                material.shaders().fragmentSource() != null &&
                material.shaders().vertexSource() != null &&
                material.fog() != null &&
                material.fog().source() != null &&
                material.cutout() != null &&
                material.cutout().source() != null &&
                material.light() != null &&
                material.light().source() != null &&
                material.texture() != null &&
                material.depthTest() != null &&
                material.transparency() != null &&
                material.writeMask() != null &&
                material.cardinalLightingMode() != null;
        // @formatter:on
    }

    public static int compare(Material lhs, Material rhs)
    {
        if (lhs == rhs) {
            return 0;
        }

        int cmp;
        cmp = lhs.transparency()
                .compareTo(rhs.transparency());
        if (cmp != 0) {
            return cmp;
        }
        cmp = lhs.light()
                .source()
                .compareTo(rhs.light()
                        .source());
        if (cmp != 0) {
            return cmp;
        }
        cmp = lhs.cutout()
                .source()
                .compareTo(rhs.cutout()
                        .source());
        if (cmp != 0) {
            return cmp;
        }
        cmp = lhs.shaders()
                .fragmentSource()
                .compareTo(rhs.shaders()
                        .fragmentSource());
        if (cmp != 0) {
            return cmp;
        }
        cmp = lhs.shaders()
                .vertexSource()
                .compareTo(rhs.shaders()
                        .vertexSource());
        if (cmp != 0) {
            return cmp;
        }
        cmp = lhs.texture()
                .compareTo(rhs.texture());
        if (cmp != 0) {
            return cmp;
        }
        cmp = Boolean.compare(lhs.blur(), rhs.blur());
        if (cmp != 0) {
            return cmp;
        }
        cmp = Boolean.compare(lhs.mipmap(), rhs.mipmap());
        if (cmp != 0) {
            return cmp;
        }
        cmp = Boolean.compare(lhs.backfaceCulling(), rhs.backfaceCulling());
        if (cmp != 0) {
            return cmp;
        }
        cmp = Boolean.compare(lhs.polygonOffset(), rhs.polygonOffset());
        if (cmp != 0) {
            return cmp;
        }
        cmp = lhs.depthTest()
                .compareTo(rhs.depthTest());
        if (cmp != 0) {
            return cmp;
        }
        cmp = lhs.writeMask()
                .compareTo(rhs.writeMask());
        if (cmp != 0) {
            return cmp;
        }
        return 0;
    }
}
