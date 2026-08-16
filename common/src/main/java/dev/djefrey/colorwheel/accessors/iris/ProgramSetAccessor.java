package dev.djefrey.colorwheel.accessors.iris;

import dev.djefrey.colorwheel.shaderpack.ClrwlProgramId;
import net.irisshaders.iris.shaderpack.loading.ProgramId;
import net.irisshaders.iris.shaderpack.programs.ComputeSource;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;

import java.util.Optional;

public interface ProgramSetAccessor
{
	Optional<ClrwlProgramId> colorwheel$getRealClrwlProgram(ClrwlProgramId programId);
	Optional<ProgramId> colorwheel$getRealFallbackProgram(ClrwlProgramId programId);
	Optional<ProgramSource> colorwheel$getClrwlProgramSource(ClrwlProgramId programId);
	Optional<ComputeSource> colorwheel$getShadowTransformSource();
	boolean colorwheel$isFallbackMode();
}
