package dev.djefrey.colorwheel.mixin.iris;

import dev.djefrey.colorwheel.gl.ClrwlShaderType;
import dev.djefrey.colorwheel.accessors.iris.ProgramSourceAccessor;
import dev.djefrey.colorwheel.compile.transform.ClrwlTransformPatcher;
import net.irisshaders.iris.gl.blending.BlendModeOverride;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shaderpack.properties.ShaderProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(ProgramSource.class)
public class ProgramSourceMixin implements ProgramSourceAccessor
{
	@Unique
	ShaderProperties colorwheel$shaderProperties;

	@Unique
	BlendModeOverride colorwheel$blendMode;

	@Unique
	EnumMap<ClrwlShaderType, Integer> colorwheel$shaderVersions;

	@Unique
	EnumMap<ClrwlShaderType, List<String>> colorwheel$shaderExtensions;

	@Override
	public ShaderProperties colorwheel$getShaderProperties() {
		return colorwheel$shaderProperties;
	}

	@Override
	public BlendModeOverride colorwheel$getBlendModeOverride() {
		return colorwheel$blendMode;
	}

	@Override
	public Optional<Integer> colorwheel$getShaderVersion(ClrwlShaderType type)
	{
		return Optional.ofNullable(this.colorwheel$shaderVersions.get(type));
	}

	@Override
	public EnumMap<ClrwlShaderType, List<String>> colorwheel$getShaderExtensions() { return colorwheel$shaderExtensions; }

	@Inject(method = "<init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lnet/irisshaders/iris/shaderpack/programs/ProgramSet;Lnet/irisshaders/iris/shaderpack/properties/ShaderProperties;Lnet/irisshaders/iris/gl/blending/BlendModeOverride;)V",
			at = @At("TAIL"))
	private void injectInit(String name, String vertex, String geometry, String tess, String tessEval, String fragment, ProgramSet programs, ShaderProperties properties, BlendModeOverride blendModeOverride, CallbackInfo ci)
	{
		this.colorwheel$shaderVersions = new EnumMap<>(ClrwlShaderType.class);
		colorwheel$parseShaderVersion(vertex).ifPresent(v -> this.colorwheel$shaderVersions.put(ClrwlShaderType.VERTEX, v));
		colorwheel$parseShaderVersion(geometry).ifPresent(v -> this.colorwheel$shaderVersions.put(ClrwlShaderType.GEOMETRY, v));
		colorwheel$parseShaderVersion(fragment).ifPresent(v -> this.colorwheel$shaderVersions.put(ClrwlShaderType.FRAGMENT, v));

		this.colorwheel$shaderExtensions = new EnumMap<>(ClrwlShaderType.class);
		this.colorwheel$shaderExtensions.put(ClrwlShaderType.VERTEX,   colorwheel$parseShaderExtensions(vertex));
		this.colorwheel$shaderExtensions.put(ClrwlShaderType.GEOMETRY, colorwheel$parseShaderExtensions(geometry));
		this.colorwheel$shaderExtensions.put(ClrwlShaderType.FRAGMENT, colorwheel$parseShaderExtensions(fragment));

		this.colorwheel$shaderProperties = properties;
		this.colorwheel$blendMode = blendModeOverride;
	}

	@Unique
	private Optional<Integer> colorwheel$parseShaderVersion(String str)
	{
		if (str == null)
		{
			return Optional.empty();
		}

		var it = str.lines().iterator();

		while (it.hasNext())
		{
			String line = it.next();
			var matcher = ClrwlTransformPatcher.versionPattern.matcher(line);

			if (matcher.matches())
			{
				return Optional.of(Integer.parseInt(matcher.group(1)));
			}
		}

		return Optional.empty();
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
