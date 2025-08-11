package dev.djefrey.colorwheel.accessors;

import com.google.common.collect.ImmutableList;
import dev.djefrey.colorwheel.shaderpack.ClrwlShaderProperties;
import net.irisshaders.iris.helpers.StringPair;

public interface ShaderPackAccessor
{
	ImmutableList<StringPair> colorwheel$getEnvironmentDefines();
	ClrwlShaderProperties colorwheel$getProperties();
}
