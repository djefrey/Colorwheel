package dev.djefrey.colorwheel.mixin;

import dev.djefrey.colorwheel.Colorwheel;
import dev.djefrey.colorwheel.accessors.ProgramDirectivesAccessor;
import dev.djefrey.colorwheel.engine.ClrwlOitCoeffDirective;
import net.irisshaders.iris.gl.blending.BlendModeOverride;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shaderpack.properties.ProgramDirectives;
import net.irisshaders.iris.shaderpack.properties.ShaderProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(ProgramDirectives.class)
public class ProgramDirectivesMixin  implements ProgramDirectivesAccessor
{
    private static final String COEFFS_DIRECTIVE = "CLRWL_COEFFS";

    @Unique
    private List<ClrwlOitCoeffDirective> colorwheel$oitCoefficients;

    @Invoker(remap = false)
    private static int[] callParseDigits(char[] directiveChars)
    {
        return null;
    }

    @Invoker(remap = false)
    private static int[] callParseDigitList(String digitListString)
    {
        return null;
    }

    @Inject(method = " <init>(Lnet/irisshaders/iris/shaderpack/programs/ProgramSource;Lnet/irisshaders/iris/shaderpack/properties/ShaderProperties;Ljava/util/Set;Lnet/irisshaders/iris/gl/blending/BlendModeOverride;)V",
            at = @At("RETURN"),
            remap = false)
    private void injectInit(ProgramSource source, ShaderProperties properties, Set supportedRenderTargets, BlendModeOverride defaultBlendOverride, CallbackInfo ci)
    {
        colorwheel$oitCoefficients = source.getFragmentSource()
                .flatMap(ProgramDirectivesMixin::colorwheel$findCoefficientsDirective)
                .orElse(List.of(new ClrwlOitCoeffDirective(3, new int[]{ 0 })));
    }

    @Unique
    private static Optional<List<ClrwlOitCoeffDirective>> colorwheel$findCoefficientsDirective(String source)
    {
        String prefix = COEFFS_DIRECTIVE + ":";
        String suffix = "*/";

        int prefixIdx = source.lastIndexOf(prefix);

        if (prefixIdx == -1)
        {
            return Optional.empty();
        }

        int suffixIdx = source.indexOf(suffix, prefixIdx);

        if (suffixIdx == -1)
        {
            return Optional.empty();
        }

        String directive = source.substring(prefixIdx + prefix.length(), suffixIdx).trim();
        String[] maps = directive.split(";");

        List<ClrwlOitCoeffDirective> directives = new ArrayList<>();

        for (String map : maps)
        {
            String[] split = Arrays.stream(map.split(":")).map(String::trim).toArray(String[]::new);

            if (split.length != 2 || split[0].length() != 1)
            {
                Colorwheel.LOGGER.error("'{}' is invalid", directive);
                return Optional.empty();
            }

            char rank = split[0].charAt(0);

            if (rank < '1' || rank > '3')
            {
                Colorwheel.LOGGER.error("'{}' has an invalid rank", directive);
                return Optional.empty();
            }

            int[] buffers = callParseDigitList(split[1]);

            directives.add(new ClrwlOitCoeffDirective(rank - '0', buffers));
        }

        return Optional.of(directives);
    }

    public List<ClrwlOitCoeffDirective> colorwheel$getOitCoefficients()
    {
        return colorwheel$oitCoefficients;
    }
}
