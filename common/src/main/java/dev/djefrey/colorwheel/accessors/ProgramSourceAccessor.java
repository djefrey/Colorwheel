package dev.djefrey.colorwheel.accessors;

import dev.djefrey.colorwheel.ShaderType;
import net.irisshaders.iris.gl.blending.BlendModeOverride;
import net.irisshaders.iris.shaderpack.properties.ShaderProperties;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ProgramSourceAccessor
{
	ShaderProperties colorwheel$getShaderProperties();
	BlendModeOverride colorwheel$getBlendModeOverride();
	Optional<Integer> colorwheel$getShaderVersion(ShaderType type);
	Map<ShaderType, List<String>> colorwheel$getShaderExtensions();
}
