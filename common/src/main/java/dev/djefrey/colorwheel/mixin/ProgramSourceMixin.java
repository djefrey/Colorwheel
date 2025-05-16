package dev.djefrey.colorwheel.mixin;

import dev.djefrey.colorwheel.accessors.ProgramSourceAccessor;
import net.irisshaders.iris.gl.blending.BlendModeOverride;
import net.irisshaders.iris.gl.shader.ShaderType;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shaderpack.properties.ShaderProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mixin(ProgramSource.class)
public class ProgramSourceMixin implements ProgramSourceAccessor
{
	@Unique
	ShaderProperties colorwheel$shaderProperties;

	@Unique
	BlendModeOverride colorwheel$blendMode;

	@Unique
	Map<ShaderType, List<String>> colorwheel$shaderExtensions;

	@Override
	public ShaderProperties colorwheel$getShaderProperties() {
		return colorwheel$shaderProperties;
	}

	@Override
	public BlendModeOverride colorwheel$getBlendModeOverride() {
		return colorwheel$blendMode;
	}

	@Override
	public Map<ShaderType, List<String>> colorwheel$getShaderExtensions() { return colorwheel$shaderExtensions; }

	@Inject(method = "<init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lnet/irisshaders/iris/shaderpack/programs/ProgramSet;Lnet/irisshaders/iris/shaderpack/properties/ShaderProperties;Lnet/irisshaders/iris/gl/blending/BlendModeOverride;)V",
			at = @At("TAIL"),
			remap = false)
	private void injectInit(String name, String vertex, String geometry, String tess, String tessEval, String fragment, ProgramSet programs, ShaderProperties properties, BlendModeOverride blendModeOverride, CallbackInfo ci)
	{
		this.colorwheel$shaderExtensions = Map.of(
				ShaderType.VERTEX,   colorwheel$parseShaderExtensions(vertex),
				ShaderType.FRAGMENT, colorwheel$parseShaderExtensions(fragment)
		);
		this.colorwheel$shaderProperties = properties;
		this.colorwheel$blendMode = blendModeOverride;
	}

	@Unique
	private List<String> colorwheel$parseShaderExtensions(String str)
	{
		List<String> res = new ArrayList<>();

		if (str == null)
		{
			return res;
		}

		str.lines().forEach(line ->
		{
			line = line.trim();

			if (line.startsWith("#extension ") && line.contains(":"))
			{
				var name = line.substring(11).split(":")[0].trim();

				res.add(name);
			}
		});

		return res;
	}
}
