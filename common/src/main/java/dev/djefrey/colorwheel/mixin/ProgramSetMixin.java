package dev.djefrey.colorwheel.mixin;

import dev.djefrey.colorwheel.accessors.ProgramSetAccessor;
import net.irisshaders.iris.gl.blending.BlendModeOverride;
import net.irisshaders.iris.shaderpack.include.AbsolutePackPath;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shaderpack.properties.ShaderProperties;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Optional;
import java.util.function.Function;

@Mixin(ProgramSet.class)
public abstract class ProgramSetMixin implements ProgramSetAccessor
{
	@Invoker(remap = false)
	@Override
	public abstract ProgramSource callReadProgramSource(AbsolutePackPath directory, Function<AbsolutePackPath, String> sourceProvider, String program, ProgramSet programSet, ShaderProperties properties, boolean readTessellation);

	@Invoker(remap = false)
	@Override
	public abstract ProgramSource callReadProgramSource(AbsolutePackPath directory, Function<AbsolutePackPath, String> sourceProvider, String program, ProgramSet programSet, ShaderProperties properties, BlendModeOverride var5, boolean readTessellation);

	@Unique
	@Nullable
	private ProgramSource flw_gbuffers;

	@Unique
	@Nullable
	private ProgramSource flw_shadows;

	public void colorwheel$setupFlwPrograms(AbsolutePackPath directory, Function<AbsolutePackPath, String> sourceProvider, ShaderProperties shaderProperties)
	{
		this.flw_gbuffers = callReadProgramSource(directory, sourceProvider, "flw_gbuffers", (ProgramSet) (Object) this, shaderProperties, BlendModeOverride.OFF, false);
		this.flw_shadows  = callReadProgramSource(directory, sourceProvider, "flw_shadows",  (ProgramSet) (Object) this, shaderProperties, BlendModeOverride.OFF, false);
	}

	public Optional<ProgramSource> colorwheel$getFlwGbuffers()
	{
		if (flw_gbuffers == null)
		{
			return Optional.empty();
		}

        return flw_gbuffers.requireValid();
	}

	public Optional<ProgramSource> colorwheel$getFlwShadows()
	{
		if (flw_shadows == null)
		{
			return colorwheel$getFlwGbuffers();
		}

		return flw_shadows.requireValid().or(this::colorwheel$getFlwGbuffers);
	}
}
