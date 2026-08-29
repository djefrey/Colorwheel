package dev.djefrey.colorwheel.mixin.iris;

import dev.djefrey.colorwheel.shaderpack.ClrwlPackDirectives;
import dev.djefrey.colorwheel.shaderpack.ClrwlProgramId;
import dev.djefrey.colorwheel.accessors.iris.PackShadowDirectivesAccessor;
import dev.djefrey.colorwheel.accessors.iris.ProgramSetAccessor;
import dev.djefrey.colorwheel.accessors.iris.ShaderPackAccessor;
import dev.djefrey.colorwheel.shaderpack.ClrwlShaderProperties;
import net.irisshaders.iris.shaderpack.ShaderPack;
import net.irisshaders.iris.shaderpack.include.AbsolutePackPath;
import net.irisshaders.iris.shaderpack.loading.ProgramId;
import net.irisshaders.iris.shaderpack.parsing.ConstDirectiveParser;
import net.irisshaders.iris.shaderpack.parsing.DispatchingDirectiveHolder;
import net.irisshaders.iris.shaderpack.programs.ComputeSource;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shaderpack.properties.PackDirectives;
import net.irisshaders.iris.shaderpack.properties.ShaderProperties;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Mixin(ProgramSet.class)
public abstract class ProgramSetMixin implements ProgramSetAccessor
{
	@Shadow
	@Final
	private PackDirectives packDirectives;

	@Shadow
	public abstract Optional<ProgramSource> get(ProgramId programId);

    @Shadow
    private static ProgramSource readProgramSource(AbsolutePackPath directory, Function<AbsolutePackPath, String> sourceProvider, String program, ProgramSet programSet, ShaderProperties properties, boolean readTesselation)
	{
		throw new RuntimeException();
	}

	@Shadow
	private static ComputeSource readComputeSource(AbsolutePackPath directory, Function<AbsolutePackPath, String> sourceProvider, String program, ProgramSet programSet, ShaderProperties properties) {
		throw new UnsupportedOperationException("Implemented via mixin");
	}

	@Unique
	private ClrwlPackDirectives colorwheel$clrwlDirectives;

	@Unique
	@Final
	private Map<ClrwlProgramId, ProgramSource> colorwheel$programSrcs = new HashMap<>();

	@Unique
	@Nullable
	private ComputeSource colorwheel$shadowDistortSrc;

	@Unique
	private boolean colorwheel$isFallbackMode = false;

	@Inject(method = "<init>(Lnet/irisshaders/iris/shaderpack/include/AbsolutePackPath;Ljava/util/function/Function;Lnet/irisshaders/iris/shaderpack/properties/ShaderProperties;Lnet/irisshaders/iris/shaderpack/ShaderPack;)V",
			at = @At("RETURN"))
	private void injectInit(AbsolutePackPath directory, Function<AbsolutePackPath, String> sourceProvider, ShaderProperties shaderProperties, ShaderPack pack, CallbackInfo ci)
	{
		var clrwlGbuffers = readProgramSource(directory, sourceProvider, ClrwlProgramId.GBUFFERS.programName(), (ProgramSet) (Object) this, shaderProperties, false)
				.requireValid();

		if (clrwlGbuffers.isPresent())
		{
			colorwheel$programSrcs.put(ClrwlProgramId.GBUFFERS, clrwlGbuffers.get());

			for (var program : ClrwlProgramId.values())
			{
				if (program == ClrwlProgramId.GBUFFERS)
				{
					continue;
				}

				readProgramSource(directory, sourceProvider, program.programName(), (ProgramSet) (Object) this, shaderProperties, false)
						.requireValid()
						.ifPresent(programSource -> colorwheel$programSrcs.put(program, programSource));
			}

			var shadowTransform = readComputeSource(directory, sourceProvider, "clrwl_shadow_distort", (ProgramSet) (Object) this, shaderProperties);

			if (shadowTransform != null && shadowTransform.isValid())
			{
				this.colorwheel$shadowDistortSrc = shadowTransform;
			}
		}
		else
		{
			colorwheel$isFallbackMode = true;

			for (var program : ClrwlProgramId.values())
			{
				colorwheel$getFallbackProgramSrc(program.fallbackProgram())
						.ifPresent(programSource -> colorwheel$programSrcs.put(program, programSource));
			}
		}

		colorwheel$locateClrwlDirectives();

		var clrwlProperties = ((ShaderPackAccessor) pack).colorwheel$getProperties();

		if (clrwlProperties != null)
		{
			colorwheel$setupClrwlDirectives(clrwlProperties);
		}
	}

	public void colorwheel$setupClrwlDirectives(ClrwlShaderProperties properties)
	{
		this.colorwheel$clrwlDirectives = new ClrwlPackDirectives((ProgramSet) (Object) this, properties);
		((PackShadowDirectivesAccessor) this.packDirectives.getShadowDirectives()).colorwheel$setFlywheelShadowRendering(properties.shouldRenderShadow());
	}

	public ClrwlPackDirectives colorwheel$getClrwlDirectives()
	{
		return colorwheel$clrwlDirectives;
	}

	@Unique
	private Optional<ProgramSource> colorwheel$getFallbackProgramSrc(@Nullable ProgramId programId)
	{
		if (programId == null)
		{
			return Optional.empty();
		}

		var src = get(programId);

		if (src.isPresent())
		{
			return src;
		}
		else
		{
			return colorwheel$getFallbackProgramSrc(programId.getFallback().orElse(null));
		}
	}

	@Unique
	private void colorwheel$locateClrwlDirectives()
	{
		DispatchingDirectiveHolder packDirectiveHolder = new DispatchingDirectiveHolder();

		for (ProgramSource source : colorwheel$programSrcs.values())
		{
			if (source == null)
			{
				continue;
			}

			source.getFragmentSource().map(ConstDirectiveParser::findDirectives).ifPresent(directives ->
			{
				for (ConstDirectiveParser.ConstDirective directive : directives)
				{
					packDirectiveHolder.processDirective(directive);
				}
			});
		}
	}

	public Optional<ClrwlProgramId> colorwheel$getRealClrwlProgram(ClrwlProgramId programId)
	{
		ClrwlProgramId cur = programId;

		while (cur != null)
		{
			if (colorwheel$programSrcs.containsKey(cur))
			{
				return Optional.of(cur);
			}

			cur = cur.base();
		}

		return Optional.empty();
	}

	public Optional<ProgramId> colorwheel$getRealFallbackProgram(ClrwlProgramId programId)
	{
		var clrwlCur = programId;

		while (clrwlCur != null && clrwlCur.fallbackProgram() == null)
		{
			clrwlCur = clrwlCur.base();
		}

		if (clrwlCur == null)
		{
			return Optional.empty();
		}

		var cur = Optional.of(clrwlCur.fallbackProgram());

		while (cur.isPresent())
		{
			if (get(cur.get()).isPresent())
			{
				return cur;
			}

			cur = cur.get().getFallback();
		}

		return Optional.empty();
	}

	public Optional<ProgramSource> colorwheel$getClrwlProgramSource(ClrwlProgramId programId)
	{
		ClrwlProgramId cur = programId;

		while (cur != null)
		{
			var nullableSrc = colorwheel$programSrcs.get(cur);

			if (nullableSrc != null)
			{
				return Optional.of(nullableSrc);
			}

			cur = cur.base();
		}

		return Optional.empty();
	}

	public Optional<ComputeSource> colorwheel$getShadowDistortSource()
	{
		return Optional.ofNullable(colorwheel$shadowDistortSrc);
	}

	@Unique
	public boolean colorwheel$isFallbackMode()
	{
		return colorwheel$isFallbackMode;
	}
}
