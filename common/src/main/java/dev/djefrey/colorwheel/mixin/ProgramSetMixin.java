package dev.djefrey.colorwheel.mixin;

import com.google.common.collect.ImmutableList;
import dev.djefrey.colorwheel.accessors.PackDirectivesAccessor;
import dev.djefrey.colorwheel.accessors.ProgramSetAccessor;
import net.irisshaders.iris.gl.blending.BlendModeOverride;
import net.irisshaders.iris.shaderpack.ShaderPack;
import net.irisshaders.iris.shaderpack.include.AbsolutePackPath;
import net.irisshaders.iris.shaderpack.include.IncludeGraph;
import net.irisshaders.iris.shaderpack.include.IncludeProcessor;
import net.irisshaders.iris.shaderpack.parsing.ConstDirectiveParser;
import net.irisshaders.iris.shaderpack.parsing.DispatchingDirectiveHolder;
import net.irisshaders.iris.shaderpack.preprocessor.JcppProcessor;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shaderpack.properties.PackDirectives;
import net.irisshaders.iris.shaderpack.properties.ShaderProperties;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Mixin(ProgramSet.class)
public abstract class ProgramSetMixin implements ProgramSetAccessor
{
	@Shadow(remap = false)
	@Final
	private PackDirectives packDirectives;

	@Invoker(remap = false)
	@Override
	public abstract ProgramSource callReadProgramSource(AbsolutePackPath directory, Function<AbsolutePackPath, String> sourceProvider, String program, ProgramSet programSet, ShaderProperties properties, boolean readTessellation);

	@Invoker(remap = false)
	@Override
	public abstract ProgramSource callReadProgramSource(AbsolutePackPath directory, Function<AbsolutePackPath, String> sourceProvider, String program, ProgramSet programSet, ShaderProperties properties, BlendModeOverride var5, boolean readTessellation);

	@Unique
	@Nullable
	private ProgramSource clrwl_gbuffers;

	@Unique
	@Nullable
	private ProgramSource clrwl_shadow;

	@Unique
	@Nullable
	private ProgramSource clrwl_damagedblock;

	@Inject(method = "<init>(Lnet/irisshaders/iris/shaderpack/include/AbsolutePackPath;Ljava/util/function/Function;Lnet/irisshaders/iris/shaderpack/properties/ShaderProperties;Lnet/irisshaders/iris/shaderpack/ShaderPack;)V",
			at = @At("RETURN"),
			remap = false)
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

		this.clrwl_gbuffers = callReadProgramSource(directory, sourceProvider, "clrwl_gbuffers", (ProgramSet) (Object) this, shaderProperties, false);
		this.clrwl_shadow = callReadProgramSource(directory, sourceProvider, "clrwl_shadow",  (ProgramSet) (Object) this, shaderProperties, false);
		this.clrwl_damagedblock = callReadProgramSource(directory, sourceProvider, "clrwl_damagedblock", (ProgramSet) (Object) this, shaderProperties, false);

		colorwheel$locateClrwlDirectives();
	}

	@Unique
	private void colorwheel$locateClrwlDirectives()
	{
		List<ProgramSource> clrwlPrograms = new ArrayList<>();

		clrwlPrograms.add(this.clrwl_gbuffers);
		clrwlPrograms.add(this.clrwl_shadow);
		clrwlPrograms.add(this.clrwl_damagedblock);

		DispatchingDirectiveHolder packDirectiveHolder = new DispatchingDirectiveHolder();

		((PackDirectivesAccessor) packDirectives).colorwheel$acceptColorwheelDirectives(packDirectiveHolder);

		for (ProgramSource source : clrwlPrograms)
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

	public Optional<ProgramSource> colorwheel$getClrwlGbuffers()
	{
		if (clrwl_gbuffers == null)
		{
			return Optional.empty();
		}

        return clrwl_gbuffers.requireValid();
	}

	public Optional<ProgramSource> colorwheel$getClrwlShadow()
	{
		if (clrwl_shadow == null)
		{
			return Optional.empty();
		}

		return clrwl_shadow.requireValid();
	}


	public Optional<ProgramSource> colorwheel$getClrwlDamagedblock()
	{
		if (clrwl_damagedblock == null)
		{
			return Optional.empty();
		}

		return clrwl_damagedblock.requireValid();
	}
}
