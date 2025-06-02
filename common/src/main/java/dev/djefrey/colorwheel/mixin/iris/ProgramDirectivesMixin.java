package dev.djefrey.colorwheel.mixin.iris;

import dev.djefrey.colorwheel.accessors.ProgramDirectivesAccessor;
import net.irisshaders.iris.gl.blending.BlendModeOverride;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shaderpack.properties.ProgramDirectives;
import net.irisshaders.iris.shaderpack.properties.ShaderProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.Set;

@Mixin(ProgramDirectives.class)
public class ProgramDirectivesMixin implements ProgramDirectivesAccessor
{
    @Unique
    private boolean colorwheel$disableAutoFrag = false;

    @Inject(method = "<init>(Lnet/irisshaders/iris/shaderpack/programs/ProgramSource;Lnet/irisshaders/iris/shaderpack/properties/ShaderProperties;Ljava/util/Set;Lnet/irisshaders/iris/gl/blending/BlendModeOverride;)V",
            at = @At("RETURN"),
            remap = false)
    private void injectInit(ProgramSource source, ShaderProperties properties, Set supportedRenderTargets, BlendModeOverride defaultBlendOverride, CallbackInfo ci)
    {
        this.colorwheel$disableAutoFrag = colorwheel$containsFlagDirective(source.getFragmentSource(), "CLRWL_DISABLE_AUTO_FRAGCOLOR");
    }

    @Unique
    private static boolean colorwheel$containsFlagDirective(Optional<String> source, String directive)
    {
        return source.map(s -> s.lines().anyMatch(line ->
        {
            line = line.trim();

            if (!line.startsWith("//")) {
                return false;
            }

            line = line.substring(2).trim();

            if (!line.startsWith(directive)) {
                return false;
            }

            line = line.substring(directive.length()).trim();

            return line.isEmpty();
        }))
        .orElse(false);
    }

    @Override
    public boolean colorwheel$isAutoFragColorDisable()
    {
        return colorwheel$disableAutoFrag;
    }
}
