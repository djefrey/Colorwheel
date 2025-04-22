package dev.djefrey.colorwheel;

import dev.engine_room.flywheel.backend.compile.FlwPrograms;
import dev.engine_room.flywheel.backend.glsl.LoadError;
import dev.engine_room.flywheel.backend.glsl.LoadResult;
import dev.engine_room.flywheel.backend.glsl.ShaderSources;
import dev.engine_room.flywheel.backend.glsl.SourceFile;
import dev.engine_room.flywheel.lib.util.StringUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * The main object for loading and parsing source files.
 */
public class ClrwlShaderSources
{
	@VisibleForTesting
	protected final Map<ResourceLocation, LoadResult> cache;

	public ClrwlShaderSources(ResourceManager manager)
	{
		var finder = new SourceFinder(manager);

		long loadStart = System.nanoTime();

		manager.listResources("flywheel", ClrwlShaderSources::isShader)
				.forEach(finder::rootLoad);

		manager.listResources("colorwheel", ClrwlShaderSources::isShader)
				.forEach(finder::rootLoad);

		long loadEnd = System.nanoTime();

		Colorwheel.LOGGER.info("Loaded {} shader sources in {}", finder.results.size(), StringUtil.formatTime(loadEnd - loadStart));

		this.cache = finder.results;
	}

	private static ResourceLocation locationWithoutPrefix(ResourceLocation loc, String prefix)
	{
		return new ResourceLocation(loc.getNamespace(), loc.getPath()
				.substring(prefix.length()));
	}

	public LoadResult find(ResourceLocation location) {
		return cache.computeIfAbsent(location, loc -> new LoadResult.Failure(new LoadError.ResourceError(loc)));
	}

	public SourceFile get(ResourceLocation location) {
		return find(location).unwrap();
	}

	private static boolean isShader(ResourceLocation loc) {
		var path = loc.getPath();
		return path.endsWith(".glsl") || path.endsWith(".vert") || path.endsWith(".frag") || path.endsWith(".comp");
	}

	private static class SourceFinder
	{
		private final Deque<ResourceLocation> findStack = new ArrayDeque<>();
		private final Map<ResourceLocation, LoadResult> results = new HashMap<>();
		private final ResourceManager manager;

		public SourceFinder(ResourceManager manager)
		{
			this.manager = manager;
		}

		public void rootLoad(ResourceLocation loc, Resource resource)
		{
			var strippedLoc = locationWithoutPrefix(loc, loc.getNamespace() + "/");

			if (results.containsKey(strippedLoc)) {
				// Some other source already #included this one.
				return;
			}

			this.results.put(strippedLoc, readResource(strippedLoc, resource));
		}

		public LoadResult recursiveLoad(ResourceLocation location)
		{
			if (findStack.contains(location)) {
				// Make a copy of the find stack with the offending location added on top to show the full path.
				findStack.addLast(location);
				var copy = List.copyOf(findStack);
				findStack.removeLast();
				return new LoadResult.Failure(new LoadError.CircularDependency(location, copy));
			}
			findStack.addLast(location);

			LoadResult out = _find(location);

			findStack.removeLast();
			return out;
		}

		private LoadResult _find(ResourceLocation location)
		{
			// Can't use computeIfAbsent because mutual recursion causes ConcurrentModificationExceptions
			var out = results.get(location);
			if (out == null) {
				out = load(location);
				results.put(location, out);
			}
			return out;
		}

		private LoadResult load(ResourceLocation loc)
		{
			return manager.getResource(loc.withPrefix(loc.getNamespace() + "/"))
					.map(resource -> readResource(loc, resource))
					.orElseGet(() -> new LoadResult.Failure(new LoadError.ResourceError(loc)));
		}

		private LoadResult readResource(ResourceLocation loc, Resource resource)
		{
			try (InputStream stream = resource.open()) {
				String sourceString = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
				return SourceFile.parse(this::recursiveLoad, loc, sourceString);
			} catch (IOException e) {
				return new LoadResult.Failure(new LoadError.IOError(loc, e));
			}
		}
	}
}
