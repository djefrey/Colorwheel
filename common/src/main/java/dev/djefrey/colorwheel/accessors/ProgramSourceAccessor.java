package dev.djefrey.colorwheel.accessors;

import net.irisshaders.iris.gl.blending.BlendModeOverride;
import net.irisshaders.iris.shaderpack.properties.ShaderProperties;

public interface ProgramSourceAccessor
{
	ShaderProperties colorwheel$getShaderProperties();
	BlendModeOverride colorwheel$getBlendModeOverride();
}
