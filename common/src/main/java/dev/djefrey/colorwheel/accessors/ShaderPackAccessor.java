package dev.djefrey.colorwheel.accessors;

import com.google.common.collect.ImmutableList;
import net.irisshaders.iris.helpers.StringPair;

public interface ShaderPackAccessor
{
	String colorwheel$getPackName();
	ImmutableList<StringPair> colorwheel$getEnvironmentDefines();
}
