package dev.djefrey.colorwheel.mixin.iris;

import com.google.common.collect.ImmutableList;
import dev.djefrey.colorwheel.ClrwlShaderProperties;
import dev.djefrey.colorwheel.accessors.ShaderPackAccessor;
import net.irisshaders.iris.helpers.StringPair;
import net.irisshaders.iris.shaderpack.ShaderPack;
import net.irisshaders.iris.shaderpack.include.IncludeGraph;
import net.irisshaders.iris.shaderpack.include.IncludeProcessor;
import net.irisshaders.iris.shaderpack.option.ProfileSet;
import net.irisshaders.iris.shaderpack.option.ShaderPackOptions;
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
import java.util.Optional;

@Mixin(ShaderPack.class)
public abstract class ShaderPackMixin implements ShaderPackAccessor
{
    @Shadow(remap = false)
	@Final
	private ShaderPackOptions shaderPackOptions;

	@Unique
	private String colorwheel$packName;

	@Unique
	private ImmutableList<StringPair> colorwheel$environmentDefines;

	@Unique
	private ClrwlShaderProperties colorwheel$properties;

	@Shadow(remap = false)
	private static Optional<String> loadProperties(Path shaderPath, String name) { return Optional.empty(); }

	@Inject(method = "<init>(Ljava/nio/file/Path;Ljava/util/Map;Lcom/google/common/collect/ImmutableList;Z)V",
			at = @At("RETURN"),
			locals = LocalCapture.CAPTURE_FAILEXCEPTION,
			remap = false)
	private void injectInit(Path root, Map changedConfigs, ImmutableList environmentDefines, boolean isZip, CallbackInfo ci, ArrayList envDefines1, ImmutableList.Builder starts, ImmutableList potentialFileNames, boolean[] hasDimensionIds, List dimensionIdCreator, IncludeGraph graph, List finalEnvironmentDefines, List invalidFlagList, List invalidFeatureFlags, List newEnvDefines, List optionalFeatureFlags, ProfileSet profiles, List disabledPrograms, IncludeProcessor includeProcessor, Iterable<StringPair> finalEnvironmentDefines1, int userOptionsChanged)
	{
		if (isZip)
		{
			var separator = root.getFileSystem().getSeparator();
			var split = root.getFileSystem().toString().split(separator);

			if (split.length > 0)
			{
				this.colorwheel$packName = split[split.length - 1];
			}
			else
			{
				this.colorwheel$packName = "shaders";
			}
		}
		else
		{
			this.colorwheel$packName = root.getParent().getFileName().toString();
		}

		this.colorwheel$environmentDefines = ImmutableList.copyOf(finalEnvironmentDefines1);
		this.colorwheel$properties = loadProperties(root, "colorwheel.properties")
				.map((str) -> new ClrwlShaderProperties(str, shaderPackOptions, finalEnvironmentDefines1))
				.orElseGet(ClrwlShaderProperties::new);
	}

	public String colorwheel$getPackName()
	{
		return colorwheel$packName;
	}

	public ImmutableList<StringPair> colorwheel$getEnvironmentDefines()
	{
		return colorwheel$environmentDefines;
	}

	public ClrwlShaderProperties colorwheel$getProperties() { return colorwheel$properties; };
}
