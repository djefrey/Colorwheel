package dev.djefrey.colorwheel.mixin;

import com.google.common.collect.ImmutableList;
import dev.djefrey.colorwheel.accessors.ProgramSetAccessor;
import dev.djefrey.colorwheel.accessors.ShaderPackAccessor;
import dev.engine_room.flywheel.backend.compile.FlwPrograms;
import net.irisshaders.iris.helpers.StringPair;
import net.irisshaders.iris.shaderpack.ShaderPack;
import net.irisshaders.iris.shaderpack.include.AbsolutePackPath;
import net.irisshaders.iris.shaderpack.include.IncludeGraph;
import net.irisshaders.iris.shaderpack.include.IncludeProcessor;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.irisshaders.iris.shaderpack.option.ProfileSet;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.shaderpack.properties.ShaderProperties;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Mixin(ShaderPack.class)
public class ShaderPackMixin implements ShaderPackAccessor
{
	@Shadow(remap = false)
	@Final
	private ProgramSet base;

	@Shadow(remap = false)
	private Map<NamespacedId, String> dimensionMap;

	@Shadow(remap = false)
	@Final
	private ShaderProperties shaderProperties;

	@Unique
	private String colorwheel$packName;

	@Unique
	private ImmutableList<StringPair> colorwheel$environmentDefines;

	@Inject(method = "<init>(Ljava/nio/file/Path;Ljava/util/Map;Lcom/google/common/collect/ImmutableList;)V",
			at = @At("RETURN"),
			locals = LocalCapture.CAPTURE_FAILEXCEPTION,
			remap = false)
	private void injectBeforeProgramSet(Path root, Map changedConfigs, ImmutableList environmentDefines, CallbackInfo ci, ArrayList envDefines1, ImmutableList.Builder starts, ImmutableList potentialFileNames, boolean[] hasDimensionIds, List dimensionIdCreator, IncludeGraph graph, List finalEnvironmentDefines, List invalidFlagList, List invalidFeatureFlags, List newEnvDefines, List optionalFeatureFlags, ProfileSet profiles, List disabledPrograms, IncludeProcessor includeProcessor, Iterable<StringPair> finalEnvironmentDefines1, int userOptionsChanged)
	{
		this.colorwheel$packName = root.getFileName().toString();

		finalEnvironmentDefines1.forEach(kv -> FlwPrograms.LOGGER.warn(kv.toString()));

		this.colorwheel$environmentDefines = ImmutableList.copyOf(finalEnvironmentDefines1);
		Function<AbsolutePackPath, String> sourceProviderNoPreprocess = (path) ->
		{
			String pathString = path.getPathString();
			// Removes the first "/" in the path if present, and the file
			// extension in order to represent the path as its program name
			String programString = pathString.substring(pathString.indexOf("/") == 0 ? 1 : 0, pathString.lastIndexOf("."));

			// Return an empty program source if the program is disabled by the current profile
			if (disabledPrograms.contains(programString)) {
				return null;
			}

			ImmutableList<String> lines = includeProcessor.getIncludedFile(path);

			if (lines == null) {
				return null;
			}

			StringBuilder builder = new StringBuilder();

			for (String line : lines) {
				builder.append(line);
				builder.append('\n');
			}

			return builder.toString();
		};

		var directory = AbsolutePackPath.fromAbsolutePath("/" + dimensionMap.getOrDefault(new NamespacedId("*", "*"), ""));

		ProgramSetAccessor programSet = ((ProgramSetAccessor) base);
		programSet.colorwheel$setupFlwPrograms(directory, sourceProviderNoPreprocess, shaderProperties);
	}

	public String colorwheel$getPackName()
	{
		return colorwheel$packName;
	}

	public ImmutableList<StringPair> colorwheel$getEnvironmentDefines()
	{
		return colorwheel$environmentDefines;
	}
}
