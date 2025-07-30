package dev.djefrey.colorwheel.mixin.iris;

import dev.djefrey.colorwheel.ClrwlProgramId;
import dev.djefrey.colorwheel.accessors.PackDirectivesAccessor;
import dev.djefrey.colorwheel.accessors.ProgramSetAccessor;
import net.irisshaders.iris.shaderpack.ShaderPack;
import net.irisshaders.iris.shaderpack.include.AbsolutePackPath;
import net.irisshaders.iris.shaderpack.parsing.ConstDirectiveParser;
import net.irisshaders.iris.shaderpack.parsing.DispatchingDirectiveHolder;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shaderpack.properties.PackDirectives;
import net.irisshaders.iris.shaderpack.properties.ShaderProperties;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
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

	@Invoker
	@Override
	public abstract ProgramSource callReadProgramSource(AbsolutePackPath directory, Function<AbsolutePackPath, String> sourceProvider, String program, ProgramSet programSet, ShaderProperties properties, boolean readTessellation);

	@Unique
	@Final
	private Map<ClrwlProgramId, ProgramSource> programSrcs = new HashMap<>();

	@Inject(method = "<init>(Lnet/irisshaders/iris/shaderpack/include/AbsolutePackPath;Ljava/util/function/Function;Lnet/irisshaders/iris/shaderpack/properties/ShaderProperties;Lnet/irisshaders/iris/shaderpack/ShaderPack;)V",
			at = @At("RETURN"))
	private void injectInit(AbsolutePackPath directory, Function sourceProvider, ShaderProperties shaderProperties, ShaderPack pack, CallbackInfo ci)
	{
		// TODO: Sources are preprocessed otherwise the PackDirectives / ProgramDirectives are not correct
		// Current method does not allow to have specific code for OIT pass, which could provide performance gain

//		IncludeGraph graph = pack.getShaderPackOptions().getIncludes();
//		IncludeProcessor includeProcessor = new IncludeProcessor(graph);
//
//		Function<AbsolutePackPath, String> sourceProviderNoPreprocess = (path) ->
//		{
//			ImmutableList<String> lines = includeProcessor.getIncludedFile(path);
//
//			if (lines == null) {
//				return null;
//			}
//
//			StringBuilder builder = new StringBuilder();
//
//			for (String line : lines) {
//				builder.append(line);
//				builder.append('\n');
//			}
//
//			return builder.toString();
//		};

		for (var program : ClrwlProgramId.values())
		{
			callReadProgramSource(directory, sourceProvider, program.programName(), (ProgramSet) (Object) this, shaderProperties, false)
					.requireValid()
					.ifPresent(programSource -> programSrcs.put(program, programSource));
		}

		colorwheel$locateClrwlDirectives();
	}

	@Unique
	private void colorwheel$locateClrwlDirectives()
	{
		DispatchingDirectiveHolder packDirectiveHolder = new DispatchingDirectiveHolder();

		((PackDirectivesAccessor) packDirectives).colorwheel$acceptColorwheelDirectives(packDirectiveHolder);

		for (ProgramSource source : programSrcs.values())
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
			if (programSrcs.containsKey(cur))
			{
				return Optional.of(cur);
			}

			cur = cur.base();
		}

		return Optional.empty();
	}

	public Optional<ProgramSource> colorwheel$getClrwlProgramSource(ClrwlProgramId programId)
	{
		ClrwlProgramId cur = programId;

		while (cur != null)
		{
			var nullableSrc = programSrcs.get(cur);

			if (nullableSrc != null)
			{
				return Optional.of(nullableSrc);
			}

			cur = cur.base();
		}

		return Optional.empty();
	}
}
