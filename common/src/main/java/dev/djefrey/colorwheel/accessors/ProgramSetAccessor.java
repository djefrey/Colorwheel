package dev.djefrey.colorwheel.accessors;

import net.irisshaders.iris.gl.blending.BlendModeOverride;
import net.irisshaders.iris.shaderpack.include.AbsolutePackPath;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shaderpack.properties.ShaderProperties;

import java.util.Optional;
import java.util.function.Function;

public interface ProgramSetAccessor
{
	ProgramSource callReadProgramSource(AbsolutePackPath directory, Function<AbsolutePackPath, String> sourceProvider, String program, ProgramSet programSet, ShaderProperties properties, boolean readTessellation);
	ProgramSource callReadProgramSource(AbsolutePackPath directory, Function<AbsolutePackPath, String> sourceProvider, String program, ProgramSet programSet, ShaderProperties properties, BlendModeOverride var5, boolean readTessellation);

	Optional<ProgramSource> colorwheel$getFlwGbuffers();
	Optional<ProgramSource> colorwheel$getFlwShadow();
	Optional<ProgramSource> colorwheel$getFlwDamagedblock();
}
