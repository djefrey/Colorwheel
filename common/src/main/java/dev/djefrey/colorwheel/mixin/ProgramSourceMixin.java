package dev.djefrey.colorwheel.mixin;

import dev.djefrey.colorwheel.accessors.ProgramSourceAccessor;
import net.irisshaders.iris.gl.blending.BlendModeOverride;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shaderpack.properties.ShaderProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ProgramSource.class)
public class ProgramSourceMixin implements ProgramSourceAccessor
{
	@Unique
	ShaderProperties colorwheel$shaderProperties;

	@Unique
	BlendModeOverride colorwheel$blendMode;

	@Override
	public ShaderProperties colorwheel$getShaderProperties() {
		return colorwheel$shaderProperties;
	}

	@Override
	public BlendModeOverride colorwheel$getBlendModeOverride() {
		return colorwheel$blendMode;
	}

	@Inject(method = "<init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lnet/irisshaders/iris/shaderpack/programs/ProgramSet;Lnet/irisshaders/iris/shaderpack/properties/ShaderProperties;Lnet/irisshaders/iris/gl/blending/BlendModeOverride;)V",
			at = @At("TAIL"),
			remap = false)
	private void injectInit(String par1, String par2, String par3, String par4, String par5, String par6, ProgramSet par7, ShaderProperties properties, BlendModeOverride blendModeOverride, CallbackInfo ci)
	{
		this.colorwheel$shaderProperties = properties;
		this.colorwheel$blendMode = blendModeOverride;
	}
}
