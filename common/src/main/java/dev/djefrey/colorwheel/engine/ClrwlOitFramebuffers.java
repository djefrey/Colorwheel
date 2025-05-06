package dev.djefrey.colorwheel.engine;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.djefrey.colorwheel.ClrwlSamplers;
import dev.djefrey.colorwheel.accessors.IrisRenderingPipelineAccessor;
import dev.djefrey.colorwheel.accessors.ProgramDirectivesAccessor;
import dev.djefrey.colorwheel.accessors.RenderTargetAccessor;
import dev.djefrey.colorwheel.compile.oit.ClrwlOitPrograms;
import dev.engine_room.flywheel.backend.NoiseTextures;
import dev.engine_room.flywheel.backend.gl.GlCompat;
import dev.engine_room.flywheel.backend.gl.GlTextureUnit;
import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.shaderpack.properties.ProgramDirectives;
import net.irisshaders.iris.shadows.ShadowRenderTargets;
import net.irisshaders.iris.targets.RenderTarget;
import net.irisshaders.iris.targets.RenderTargets;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL46;

import java.util.List;

public class ClrwlOitFramebuffers
{
    public static final float[] CLEAR_TO_ZERO = {0, 0, 0, 0};
    public static final int[] DEPTH_RANGE_DRAW_BUFFERS = {GL46.GL_COLOR_ATTACHMENT0};
    public static final int[] DEPTH_ONLY_DRAW_BUFFERS = {};

    private int[] renderTransmittanceDrawBuffers;
    private int[] accumulateDrawBuffers;

    private final ClrwlOitPrograms programs;
    private final IrisRenderingPipeline irisPipeline;
    private final boolean isShadow;
    private final ProgramDirectives directives;

    private final int vao;

    public int mainFbo = -1;
    public int coeffsFbo = -1;

    public int depthBounds = -1;
    public int[] coefficients;
    public int[] accumulate;

    private int lastWidth = -1;
    private int lastHeight = -1;

    public ClrwlOitFramebuffers(ClrwlOitPrograms programs, IrisRenderingPipeline irisPipeline, boolean isShadow, ProgramDirectives directives)
    {
        this.programs = programs;
        this.irisPipeline = irisPipeline;
        this.isShadow = isShadow;
        this.directives = directives;

        if (GlCompat.SUPPORTS_DSA)
        {
            vao = GL46.glCreateVertexArrays();
        }
        else
        {
            vao = GL32.glGenVertexArrays();
        }
    }

    public void delete()
    {
        deleteTextures();
        GL32.glDeleteVertexArrays(vao);
    }

    private void deleteTextures()
    {
        // We sometimes get the same texture ID back when creating new textures,
        // so bind zero to clear the GlStateManager

        if (depthBounds != -1)
        {
            GL32.glDeleteTextures(depthBounds);
            depthBounds = -1;
        }

        if (coefficients != null)
        {
            for (int i = 0; i < coefficients.length; i++)
            {
                ClrwlSamplers.getCoefficient(i).makeActive();
                RenderSystem.bindTexture(0);
            }

            GL32.glDeleteTextures(coefficients);
            coefficients = null;
        }

        if (accumulate != null)
        {
            GL32.glDeleteTextures(accumulate);
            accumulate = null;
        }

        if (mainFbo != -1)
        {
            GL32.glDeleteFramebuffers(mainFbo);
            mainFbo = -1;
        }

        if (coeffsFbo != -1)
        {
            GL32.glDeleteFramebuffers(coeffsFbo);
            coeffsFbo = -1;
        }

        ClrwlSamplers.DEPTH_RANGE.makeActive();
        RenderSystem.bindTexture(0);
    }

    /**
     * Set up the framebuffer.
     */
    public void prepare()
    {
        int depthTexture;
        int width;
        int height;

        if (!isShadow)
        {
            RenderTargets targets = ((IrisRenderingPipelineAccessor) irisPipeline).colorwheel$getGbuffersRenderTargets();

            depthTexture = targets.getDepthTexture();
            width = targets.getCurrentWidth();
            height = targets.getCurrentHeight();
        }
        else
        {
            ShadowRenderTargets targets = ((IrisRenderingPipelineAccessor) irisPipeline).colorwheel$getShadowRenderTargets();

            depthTexture = targets.getDepthTexture().getTextureId();
            width = targets.getResolution();
            height = targets.getResolution();
        }

        maybeResizeFBOS(width, height);

        for (int i = 0; i < coefficients.length; i++)
        {
            int buffer = coefficients[i];

            ClrwlSamplers.getCoefficient(i).makeActive();

            // Bind zero to render system to make sure we clear their internal state
            RenderSystem.bindTexture(0);
            GL32.glBindTexture(GL32.GL_TEXTURE_2D_ARRAY, buffer);
        }

        ClrwlSamplers.DEPTH_RANGE.makeActive();
        RenderSystem.bindTexture(depthBounds);

        ClrwlSamplers.NOISE.makeActive();
        NoiseTextures.BLUE_NOISE.bind();

        GlStateManager._glBindFramebuffer(GL32.GL_FRAMEBUFFER, coeffsFbo);
        GL32.glFramebufferTexture(GL32.GL_FRAMEBUFFER, GL32.GL_DEPTH_ATTACHMENT, depthTexture, 0);

        GlStateManager._glBindFramebuffer(GL32.GL_FRAMEBUFFER, mainFbo);
        GL32.glFramebufferTexture(GL32.GL_FRAMEBUFFER, GL32.GL_DEPTH_ATTACHMENT, depthTexture, 0);
    }

    /**
     * Render out the min and max depth per fragment.
     */
    public void depthRange()
    {
        // No depth writes, but we'll still use the depth test.
        RenderSystem.depthMask(false);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        RenderSystem.blendEquation(GL32.GL_MAX);

        var far =  Minecraft.getInstance().gameRenderer.getDepthFar();

        if (GlCompat.SUPPORTS_DSA)
        {
            GL46.glNamedFramebufferDrawBuffers(mainFbo, DEPTH_RANGE_DRAW_BUFFERS);
            GL46.glClearNamedFramebufferfv(mainFbo, GL46.GL_COLOR, 0, new float[]{-far, -far, 0, 0});
        }
        else
        {
            GL32.glDrawBuffers(DEPTH_RANGE_DRAW_BUFFERS);
            RenderSystem.clearColor(-far, -far, 0, 0);
            RenderSystem.clear(GL32.GL_COLOR_BUFFER_BIT, false);
        }
    }

    /**
     * Generate the coefficients to the transmittance function.
     */
    public void renderTransmittance()
    {
        GlStateManager._glBindFramebuffer(GL32.GL_FRAMEBUFFER, coeffsFbo);

        // No depth writes, but we'll still use the depth test
        RenderSystem.depthMask(false);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        RenderSystem.blendEquation(GL32.GL_FUNC_ADD);

        if (GlCompat.SUPPORTS_DSA)
        {
            GL46.glNamedFramebufferDrawBuffers(coeffsFbo, renderTransmittanceDrawBuffers);

            for (int i = 0; i < renderTransmittanceDrawBuffers.length; i++)
            {
                GL46.glClearNamedFramebufferfv(coeffsFbo, GL46.GL_COLOR, i, CLEAR_TO_ZERO);
            }
        }
        else
        {
            GL32.glDrawBuffers(renderTransmittanceDrawBuffers);
            RenderSystem.clearColor(0, 0, 0, 0);
            RenderSystem.clear(GL32.GL_COLOR_BUFFER_BIT, false);
        }
    }

    /**
     * If any fragment has its transmittance fall off to zero, search the transmittance
     * function to determine at what depth that occurs and write out to the depth buffer.
     */
    public void renderDepthFromTransmittance()
    {
        // Only write to depth, not color.
        RenderSystem.depthMask(true);
        RenderSystem.colorMask(false, false, false, false);
        RenderSystem.disableBlend();
        RenderSystem.depthFunc(GL32.GL_ALWAYS);

        if (GlCompat.SUPPORTS_DSA)
        {
            GL46.glNamedFramebufferDrawBuffers(coeffsFbo, DEPTH_ONLY_DRAW_BUFFERS);
        }
        else
        {
            GL32.glDrawBuffers(DEPTH_ONLY_DRAW_BUFFERS);
        }

        programs.getOitDepthProgram()
                .bind();

        drawFullscreenQuad();
    }

    /**
     * Sample the transmittance function and accumulate.
     */
    public void accumulate()
    {
        GlStateManager._glBindFramebuffer(GL32.GL_FRAMEBUFFER, mainFbo);

        // No depth writes, but we'll still use the depth test
        RenderSystem.depthMask(false);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        RenderSystem.blendEquation(GL32.GL_FUNC_ADD);

        if (GlCompat.SUPPORTS_DSA)
        {
            GL46.glNamedFramebufferDrawBuffers(mainFbo, accumulateDrawBuffers);

            for (int i = 0; i < accumulateDrawBuffers.length; i++)
            {
                GL46.glClearNamedFramebufferfv(mainFbo, GL46.GL_COLOR, i, CLEAR_TO_ZERO);
            }
        }
        else
        {
            GL32.glDrawBuffers(accumulateDrawBuffers);
            RenderSystem.clearColor(0, 0, 0, 0);
            RenderSystem.clear(GL32.GL_COLOR_BUFFER_BIT, false);
        }
    }

    /**
     * Composite the accumulated luminance onto the main framebuffer.
     */
    public void composite(GlFramebuffer target)
    {
        target.bind();

        // The composite shader writes out the closest depth to gl_FragDepth.
        // depthMask = true: OIT stuff renders on top of other transparent stuff.
        // depthMask = false: other transparent stuff renders on top of OIT stuff.
        // If Neo gets wavelet OIT we can use their hooks to be correct with everything.
        RenderSystem.depthMask(true);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.enableBlend();

        // We rely on the blend func to achieve:
        // final color = (1 - transmittance_total) * sum(color_f * alpha_f * transmittance_f) / sum(alpha_f * transmittance_f)
        //			+ color_dst * transmittance_total
        //
        // Though note that the alpha value we emit in the fragment shader is actually (1. - transmittance_total).
        // The extra inversion step is so we can have a sane alpha value written out for the fabulous blit shader to consume.
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.blendEquation(GL32.GL_FUNC_ADD);
        RenderSystem.depthFunc(GL32.GL_ALWAYS);

        for (int i = 0; i < accumulate.length; i++)
        {
            ClrwlSamplers.getAccumulate(i).makeActive();
            RenderSystem.bindTexture(accumulate[i]);
        }

        int[] renderTargets = directives.getDrawBuffers();
        List<ClrwlOitCoeffDirective> coeffs = ((ProgramDirectivesAccessor) directives).colorwheel$getOitCoefficients();

        programs.getOitCompositeProgram(renderTargets, coeffs)
                .bind();

        drawFullscreenQuad();
    }

    private void drawFullscreenQuad()
    {
        // Empty VAO, the actual full screen triangle is generated in the vertex shader
        GlStateManager._glBindVertexArray(vao);
        GL32.glDrawArrays(GL32.GL_TRIANGLES, 0, 3);
    }

    private RenderTarget[] getTargets(int[] drawBuffers)
    {
        RenderTarget[] actualTargets = new RenderTarget[drawBuffers.length];

        if (!isShadow)
        {
            RenderTargets targets = ((IrisRenderingPipelineAccessor) irisPipeline).colorwheel$getGbuffersRenderTargets();

            for (int i = 0; i < drawBuffers.length; i++)
            {
                actualTargets[i] = targets.getOrCreate(drawBuffers[i]);
            }
        }
        else
        {
            ShadowRenderTargets targets = ((IrisRenderingPipelineAccessor) irisPipeline).colorwheel$getShadowRenderTargets();

            for (int i = 0; i < drawBuffers.length; i++)
            {
                actualTargets[i] = targets.getOrCreate(drawBuffers[i]);
            }
        }

        return actualTargets;
    }

    private void maybeResizeFBOS(int width, int height)
    {
        if (lastWidth == width && lastHeight == height)
        {
            return;
        }

        lastWidth = width;
        lastHeight = height;

        deleteTextures();

        resizeMainFBO(width, height);
        resizeCoeffsFBO(width, height);
    }

    private void resizeMainFBO(int width, int height)
    {
        int[] drawBuffers = directives.getDrawBuffers();
        RenderTarget[] renderTargets = getTargets(drawBuffers);

        if (GlCompat.SUPPORTS_DSA)
        {
            mainFbo = GL46.glCreateFramebuffers();
            accumulate = new int[renderTargets.length];

            depthBounds = GL46.glCreateTextures(GL46.GL_TEXTURE_2D);

            GL46.glTextureStorage2D(depthBounds, 1, GL32.GL_RG32F, width, height);

            for (int i = 0; i < renderTargets.length; i++)
            {
                RenderTarget target = renderTargets[i];
                int buffer = GL46.glCreateTextures(GL46.GL_TEXTURE_2D);

                // TODO: Allow shader dev to set format
                // Secondary buffers may not require 16b floats

                GL46.glTextureStorage2D(buffer, 1, GL32.GL_RGBA16F, target.getWidth(), target.getHeight());
                accumulate[i] = buffer;
            }

            GL46.glNamedFramebufferTexture(mainFbo, GL32.GL_COLOR_ATTACHMENT0, depthBounds, 0);

            accumulateDrawBuffers = new int[drawBuffers.length];

            for (int i = 0; i < drawBuffers.length; i++)
            {
                GL46.glNamedFramebufferTexture(mainFbo, GL32.GL_COLOR_ATTACHMENT1 + i, accumulate[i], 0);
                accumulateDrawBuffers[i] = GL32.GL_COLOR_ATTACHMENT1 + i;
            }
        }
        else
        {
            mainFbo = GL46.glGenFramebuffers();

            depthBounds = GL32.glGenTextures();
            accumulate = new int[renderTargets.length];

            GlTextureUnit.T0.makeActive();
            RenderSystem.bindTexture(0);

            GL32.glBindTexture(GL32.GL_TEXTURE_2D, depthBounds);
            GL32.glTexImage2D(GL32.GL_TEXTURE_2D, 0, GL32.GL_RG32F, width, height, 0, GL46.GL_RGBA, GL46.GL_BYTE, 0);

            GL32.glTexParameteri(GL32.GL_TEXTURE_2D, GL32.GL_TEXTURE_MIN_FILTER, GL32.GL_NEAREST);
            GL32.glTexParameteri(GL32.GL_TEXTURE_2D, GL32.GL_TEXTURE_MAG_FILTER, GL32.GL_NEAREST);
            GL32.glTexParameteri(GL32.GL_TEXTURE_2D, GL32.GL_TEXTURE_WRAP_S, GL32.GL_CLAMP_TO_EDGE);
            GL32.glTexParameteri(GL32.GL_TEXTURE_2D, GL32.GL_TEXTURE_WRAP_T, GL32.GL_CLAMP_TO_EDGE);

            for (int i = 0; i < renderTargets.length; i++)
            {
                RenderTarget target = renderTargets[i];
                int buffer = GL32.glGenTextures();

                // TODO: Allow shader dev to set format
                // Secondary buffers may not require 16b floats

                GL32.glBindTexture(GL32.GL_TEXTURE_2D, buffer);
                GL32.glTexImage2D(GL32.GL_TEXTURE_2D, 0, GL32.GL_RGBA16F, target.getWidth(), target.getHeight(), 0, GL32.GL_RGBA, GL32.GL_BACK, 0);

                GL32.glTexParameteri(GL32.GL_TEXTURE_2D, GL32.GL_TEXTURE_MIN_FILTER, GL32.GL_NEAREST);
                GL32.glTexParameteri(GL32.GL_TEXTURE_2D, GL32.GL_TEXTURE_MAG_FILTER, GL32.GL_NEAREST);
                GL32.glTexParameteri(GL32.GL_TEXTURE_2D, GL32.GL_TEXTURE_WRAP_S, GL32.GL_CLAMP_TO_EDGE);
                GL32.glTexParameteri(GL32.GL_TEXTURE_2D, GL32.GL_TEXTURE_WRAP_T, GL32.GL_CLAMP_TO_EDGE);

                accumulate[i] = buffer;
            }

            GlStateManager._glBindFramebuffer(GL32.GL_FRAMEBUFFER, mainFbo);

            GL46.glFramebufferTexture(GL32.GL_FRAMEBUFFER, GL32.GL_COLOR_ATTACHMENT0, depthBounds, 0);

            accumulateDrawBuffers = new int[drawBuffers.length];

            for (int i = 0; i < drawBuffers.length; i++)
            {
                GL46.glFramebufferTexture(GL32.GL_FRAMEBUFFER, GL32.GL_COLOR_ATTACHMENT1 + i, accumulate[i], 0);
                accumulateDrawBuffers[i] = GL32.GL_COLOR_ATTACHMENT1 + i;
            }
        }
    }

    private void resizeCoeffsFBO(int width, int height)
    {
        var coeffs = ((ProgramDirectivesAccessor) directives).colorwheel$getOitCoefficients();

        if (GlCompat.SUPPORTS_DSA)
        {
            coeffsFbo = GL46.glCreateFramebuffers();
            coefficients = new int[coeffs.size()];

            int coeffsSum = 0;

            for (int i = 0; i < coeffs.size(); i++)
            {
                int buffer = GL46.glCreateTextures(GL46.GL_TEXTURE_2D_ARRAY);
                int depth = divRoundUp(1 << (coeffs.get(i).rank() + 1), 4);

                GL46.glTextureStorage3D(buffer, 1, GL32.GL_RGBA16F, width, height, depth);
                coefficients[i] = buffer;
                coeffsSum += depth;
            }

            renderTransmittanceDrawBuffers = new int[coeffsSum];
            int transmittanceIdx = 0;

            for (int i = 0; i < coeffs.size(); i++)
            {
                int depth = divRoundUp(1 << (coeffs.get(i).rank() + 1), 4);

                for (int j = 0; j < depth; j++)
                {
                    GL46.glNamedFramebufferTextureLayer(coeffsFbo, GL32.GL_COLOR_ATTACHMENT0 + transmittanceIdx, coefficients[i], 0, j);
                    renderTransmittanceDrawBuffers[transmittanceIdx] = GL32.GL_COLOR_ATTACHMENT0 + transmittanceIdx;
                    transmittanceIdx += 1;
                }
            }
        }
        else
        {
            coeffsFbo = GL46.glGenFramebuffers();

            coefficients = new int[coeffs.size()];

            int coeffsSum = 0;

            for (int i = 0; i < coeffs.size(); i++)
            {
                int buffer = GL32.glGenTextures();
                int depth = divRoundUp(1 << (coeffs.get(i).rank() + 1), 4);

                GL32.glBindTexture(GL32.GL_TEXTURE_2D_ARRAY, buffer);
                GL32.glTexImage3D(GL32.GL_TEXTURE_2D_ARRAY, 0, GL32.GL_RGBA16F, width, height, depth, 0, GL46.GL_RGBA, GL46.GL_BYTE, 0);

                GL32.glTexParameteri(GL32.GL_TEXTURE_2D_ARRAY, GL32.GL_TEXTURE_MIN_FILTER, GL32.GL_NEAREST);
                GL32.glTexParameteri(GL32.GL_TEXTURE_2D_ARRAY, GL32.GL_TEXTURE_MAG_FILTER, GL32.GL_NEAREST);
                GL32.glTexParameteri(GL32.GL_TEXTURE_2D_ARRAY, GL32.GL_TEXTURE_WRAP_S, GL32.GL_CLAMP_TO_EDGE);
                GL32.glTexParameteri(GL32.GL_TEXTURE_2D_ARRAY, GL32.GL_TEXTURE_WRAP_T, GL32.GL_CLAMP_TO_EDGE);

                coefficients[i] = buffer;
                coeffsSum += depth;
            }

            GlStateManager._glBindFramebuffer(GL32.GL_FRAMEBUFFER, coeffsFbo);

            renderTransmittanceDrawBuffers = new int[coeffsSum];
            int transmittanceIdx = 0;

            for (int i = 0; i < coeffs.size(); i++)
            {
                int depth = divRoundUp(1 << (coeffs.get(i).rank() + 1), 4);

                for (int j = 0; j < depth; j++)
                {
                    GL46.glFramebufferTextureLayer(GL32.GL_FRAMEBUFFER, GL32.GL_COLOR_ATTACHMENT0 + transmittanceIdx, coefficients[i], 0, j);
                    renderTransmittanceDrawBuffers[transmittanceIdx] = GL32.GL_COLOR_ATTACHMENT0 + transmittanceIdx;
                    transmittanceIdx += 1;
                }
            }
        }
    }

    private static int divRoundUp(int num, int den)
    {
        return (num + den - 1) / den;
    }
}
