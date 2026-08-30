package dev.djefrey.colorwheel.accessors.iris;

import dev.djefrey.colorwheel.gl.ClrwlShaderType;
import net.irisshaders.iris.gl.blending.BlendModeOverride;
import net.irisshaders.iris.shaderpack.properties.ShaderProperties;

import java.util.EnumMap;
import java.util.List;
import java.util.Optional;

public interface ProgramSourceAccessor
{
	ShaderProperties colorwheel$getShaderProperties();
	BlendModeOverride colorwheel$getBlendModeOverride();
	Optional<Integer> colorwheel$getShaderVersion(ClrwlShaderType type);
	EnumMap<ClrwlShaderType, List<String>> colorwheel$getShaderExtensions();
}
