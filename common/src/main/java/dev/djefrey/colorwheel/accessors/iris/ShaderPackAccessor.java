package dev.djefrey.colorwheel.accessors.iris;

import com.google.common.collect.ImmutableList;
import dev.djefrey.colorwheel.shaderpack.ClrwlShaderProperties;
import net.irisshaders.iris.helpers.StringPair;
import net.irisshaders.iris.shaderpack.properties.ShaderProperties;

public interface ShaderPackAccessor
{
	ImmutableList<StringPair> colorwheel$getEnvironmentDefines();
	ClrwlShaderProperties colorwheel$getProperties();
	ShaderProperties colorwheel$getPackProperties();
}
