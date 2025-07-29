package dev.djefrey.colorwheel.accessors;

import dev.djefrey.colorwheel.ShaderType;
import net.irisshaders.iris.gl.blending.BlendModeOverride;
import net.irisshaders.iris.shaderpack.properties.ShaderProperties;

import java.util.List;
import java.util.Map;

public interface ProgramSourceAccessor
{
	ShaderProperties colorwheel$getShaderProperties();
	BlendModeOverride colorwheel$getBlendModeOverride();
	Map<ShaderType, List<String>> colorwheel$getShaderExtensions();
}
