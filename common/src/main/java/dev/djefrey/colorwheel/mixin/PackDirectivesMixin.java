package dev.djefrey.colorwheel.mixin;

import dev.djefrey.colorwheel.Colorwheel;
import dev.djefrey.colorwheel.accessors.PackDirectivesAccessor;
import net.irisshaders.iris.gl.IrisLimits;
import net.irisshaders.iris.gl.texture.InternalTextureFormat;
import net.irisshaders.iris.shaderpack.parsing.DirectiveHolder;
import net.irisshaders.iris.shaderpack.properties.PackDirectives;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(PackDirectives.class)
public class PackDirectivesMixin implements PackDirectivesAccessor
{
    // ----- GBUFFERS -----

    @Unique
    private Map<Integer, Integer> colorwheel$gbuffersCoefficientsRanks = new HashMap<>();

    @Unique
    private Map<Integer, Integer> colorwheel$gbuffersTranslucentAccumulateCoefficients = new HashMap<>();

    @Unique
    private Map<Integer, InternalTextureFormat> colorwheel$gbuffersTranslucentAccumulateFormats = new HashMap<>();

    @Unique
    private Map<Integer, InternalTextureFormat> colorwheel$gbuffersOpaqueAccumulateFormats = new HashMap<>();

    @Unique
    private Map<Integer, Integer> colorwheel$gbuffersTranslucentRenderTargets = new HashMap<>();

    @Unique
    private Map<Integer, Integer> colorwheel$gbuffersOpaqueRenderTargets = new HashMap<>();

    // ----- SHADOW -----

    @Unique
    private Map<Integer, Integer> colorwheel$shadowCoefficientsRanks = new HashMap<>();

    @Unique
    private Map<Integer, Integer> colorwheel$shadowTranslucentAccumulateCoefficients = new HashMap<>();

    @Unique
    private Map<Integer, InternalTextureFormat> colorwheel$shadowTranslucentAccumulateFormats = new HashMap<>();

    @Unique
    private Map<Integer, InternalTextureFormat> colorwheel$shadowOpaqueAccumulateFormats = new HashMap<>();

    @Unique
    private Map<Integer, Integer> colorwheel$shadowTranslucentRenderTargets = new HashMap<>();

    @Unique
    private Map<Integer, Integer> colorwheel$shadowOpaqueRenderTargets = new HashMap<>();

    public void colorwheel$acceptColorwheelDirectives(DirectiveHolder directives)
    {
        int maxCoeffs = 4;
        int maxTranslucents = IrisLimits.MAX_COLOR_BUFFERS;
        int maxOpaques = IrisLimits.MAX_COLOR_BUFFERS;

        // ----- GBUFFERS -----

        for (int i = 0; i < maxCoeffs; i++)
        {
            int finalI = i;
            directives.acceptConstIntDirective("clrwl_coefficients" + i + "Rank",
                    rank -> colorwheel$gbuffersCoefficientsRanks.put(finalI, rank));
        }

        for (int i = 0; i < maxTranslucents; i++)
        {
            int finalI = i;
            String base = "clrwl_accumulate" + i;

            directives.acceptConstStringDirective( base + "Format", str ->
            {
                var format = InternalTextureFormat.fromString(str);

                if (format.isPresent())
                {
                    colorwheel$gbuffersTranslucentAccumulateFormats.put(finalI, format.get());
                }
                else
                {
                    Colorwheel.LOGGER.warn("Unknown format " + str);
                }
            });

            directives.acceptConstIntDirective(base + "Target",
                    target -> colorwheel$gbuffersTranslucentRenderTargets.put(finalI, target));

            directives.acceptConstIntDirective(base + "Coefficients",
                    id -> colorwheel$gbuffersTranslucentAccumulateCoefficients.put(finalI, id));
        }

        for (int i = 0; i < maxOpaques; i++)
        {
            int finalI = i;
            String base = "clrwl_opaque" + i;

            directives.acceptConstStringDirective( base + "Format", str ->
            {
                var format = InternalTextureFormat.fromString(str);

                if (format.isPresent())
                {
                    colorwheel$gbuffersOpaqueAccumulateFormats.put(finalI, format.get());
                }
                else
                {
                    Colorwheel.LOGGER.warn("Unknown format " + str);
                }
            });

            directives.acceptConstIntDirective(base + "Target",
                    target -> colorwheel$gbuffersOpaqueRenderTargets.put(finalI, target));
        }


        // ----- SHADOW -----

        for (int i = 0; i < maxCoeffs; i++)
        {
            int finalI = i;
            directives.acceptConstIntDirective("clrwl_shadowCoefficients" + i + "Rank",
                    rank -> colorwheel$shadowCoefficientsRanks.put(finalI, rank));
        }

        for (int i = 0; i < maxTranslucents; i++)
        {
            int finalI = i;
            String base = "clrwl_shadowAccumulate" + i;

            directives.acceptConstStringDirective( base + "Format", str ->
            {
                var format = InternalTextureFormat.fromString(str);

                if (format.isPresent())
                {
                    colorwheel$shadowTranslucentAccumulateFormats.put(finalI, format.get());
                }
                else
                {
                    Colorwheel.LOGGER.warn("Unknown format " + str);
                }
            });

            directives.acceptConstIntDirective(base + "Target",
                    target -> colorwheel$shadowTranslucentRenderTargets.put(finalI, target));

            directives.acceptConstIntDirective(base + "Coefficients",
                    id -> colorwheel$shadowTranslucentAccumulateCoefficients.put(finalI, id));
        }

        for (int i = 0; i < maxOpaques; i++)
        {
            int finalI = i;
            String base = "clrwl_shadowOpaque" + i;

            directives.acceptConstStringDirective( base + "Format", str ->
            {
                var format = InternalTextureFormat.fromString(str);

                if (format.isPresent())
                {
                    colorwheel$shadowOpaqueAccumulateFormats.put(finalI, format.get());
                }
                else
                {
                    Colorwheel.LOGGER.warn("Unknown format " + str);
                }
            });

            directives.acceptConstIntDirective(base + "Target",
                    target -> colorwheel$shadowOpaqueRenderTargets.put(finalI, target));
        }
    }

    @Unique
    public Map<Integer, Integer> colorwheel$getGbuffersCoefficientsRanks()
    {
        return colorwheel$gbuffersCoefficientsRanks;
    }

    @Unique
    public Map<Integer, Integer> colorwheel$getGbuffersTranslucentCoefficients()
    {
        return colorwheel$gbuffersTranslucentAccumulateCoefficients;
    }

    @Unique
    public Map<Integer, InternalTextureFormat> colorwheel$getGbuffersTranslucentAccumulateFormats()
    {
        return colorwheel$gbuffersTranslucentAccumulateFormats;
    }

    @Unique
    public Map<Integer, Integer> colorwheel$getGbuffersTranslucentRenderTargets()
    {
        return colorwheel$gbuffersTranslucentRenderTargets;
    }

    @Unique
    public Map<Integer, InternalTextureFormat> colorwheel$getGbuffersOpaqueAccumulateFormats()
    {
        return colorwheel$gbuffersOpaqueAccumulateFormats;
    }

    @Unique
    public Map<Integer, Integer> colorwheel$getGbuffersOpaqueRenderTargets()
    {
        return colorwheel$gbuffersOpaqueRenderTargets;
    }



    @Unique
    public Map<Integer, Integer> colorwheel$getShadowCoefficientsRanks()
    {
        return colorwheel$shadowCoefficientsRanks;
    }

    @Unique
    public Map<Integer, Integer> colorwheel$getShadowTranslucentCoefficients()
    {
        return colorwheel$shadowTranslucentAccumulateCoefficients;
    }

    @Unique
    public Map<Integer, InternalTextureFormat> colorwheel$getShadowTranslucentAccumulateFormats()
    {
        return colorwheel$shadowTranslucentAccumulateFormats;
    }

    @Unique
    public Map<Integer, Integer> colorwheel$getShadowTranslucentRenderTargets()
    {
        return colorwheel$shadowTranslucentRenderTargets;
    }

    @Unique
    public Map<Integer, InternalTextureFormat> colorwheel$getShadowOpaqueAccumulateFormats()
    {
        return colorwheel$shadowOpaqueAccumulateFormats;
    }

    @Unique
    public Map<Integer, Integer> colorwheel$getShadowOpaqueRenderTargets()
    {
        return colorwheel$shadowOpaqueRenderTargets;
    }
}
