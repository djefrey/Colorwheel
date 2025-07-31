package dev.djefrey.colorwheel.compile;

import dev.djefrey.colorwheel.ClrwlMaterialShaderIndices;
import dev.djefrey.colorwheel.ClrwlProgramId;
import dev.djefrey.colorwheel.ClrwlShaderProperties;
import dev.djefrey.colorwheel.accessors.ProgramSetAccessor;
import dev.djefrey.colorwheel.accessors.ShaderPackAccessor;
import dev.engine_room.flywheel.backend.compile.FlwPrograms;
import dev.engine_room.flywheel.backend.compile.component.UberShaderComponent;
import dev.engine_room.flywheel.backend.compile.core.Compilation;
import dev.engine_room.flywheel.backend.gl.GlCompat;
import dev.engine_room.flywheel.backend.glsl.ShaderSources;
import dev.engine_room.flywheel.backend.glsl.SourceComponent;
import dev.engine_room.flywheel.backend.glsl.generate.FnSignature;
import dev.engine_room.flywheel.backend.glsl.generate.GlslExpr;
import dev.engine_room.flywheel.lib.util.ResourceUtil;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shaderpack.ShaderPack;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shaderpack.properties.PackDirectives;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileWriter;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class ClrwlPipelineCompiler
{
	private final ShaderSources sources;
	private final ClrwlPipeline pipeline;
	private final ShaderPack pack;
	private final NamespacedId dimension;

	public static UberShaderComponent FOG;
	public static UberShaderComponent CUTOUT;

	public ClrwlPipelineCompiler(ShaderSources sources, ClrwlPipeline pipeline, ShaderPack pack, NamespacedId dimension)
	{
		this.sources = sources;
		this.pipeline = pipeline;
		this.pack = pack;
		this.dimension = dimension;
	}

	public static void refreshUberShaders()
	{
		createFogComponent();
		createCutoutComponent();
	}

	public ClrwlProgram get(ClrwlShaderKey key)
	{
		// This will index the fog and cutout shaders
		ClrwlMaterialShaderIndices.fogSources().index(key.fog().source());
		ClrwlMaterialShaderIndices.cutoutSources().index(key.cutout().source());

		return this.compile(key);
	}

	private ClrwlProgram compile(ClrwlShaderKey key)
	{
		WorldRenderingPipeline worldPipeline = Iris.getPipelineManager().getPipelineNullable();

		if (worldPipeline instanceof IrisRenderingPipeline irisPipeline)
		{
			ProgramSet programSet = pack.getProgramSet(dimension);
			ProgramSetAccessor programAccessor = (ProgramSetAccessor) programSet;
			ClrwlShaderProperties properties = ((ShaderPackAccessor) pack).colorwheel$getProperties();
			boolean isShadow = key.isShadow();

			var instanceName = ResourceUtil.toDebugFileNameNoExtension(key.instanceType().vertexShader());
			var materialName = ResourceUtil.toDebugFileNameNoExtension(key.material().vertexSource());
			var contextName = key.context().nameLowerCase();
			var oitName = key.oit().name;

			ClrwlProgramId baseProgramId = ClrwlProgramId.fromTransparency(key.transparency(), isShadow);
			ClrwlProgramId realProgramId = programAccessor.colorwheel$getRealClrwlProgram(baseProgramId).orElseThrow();

			String name = String.format("%s/%s/%s_%s%s", realProgramId.programName(), instanceName, materialName, contextName, oitName);
			ProgramSource sources = programAccessor.colorwheel$getClrwlProgramSource(realProgramId).orElseThrow();

			var vertex = compileStage(pipeline.vertex(), key, irisPipeline, sources);
			var geometry = compileOptionalStage(pipeline.geometry(), key, irisPipeline, sources);
			var fragment = compileStage(pipeline.fragment(), key, irisPipeline, sources);

			var basePath = "/pipeline/" + Iris.getCurrentPackName() + "/" + realProgramId.programName() + "/" + key.getPath();

			dumpSources(basePath + ".vsh", vertex);
			geometry.ifPresent(sh -> dumpSources(basePath + ".gsh", sh));
			dumpSources(basePath + ".fsh", fragment);

			var customSource = new ClrwlProgramSource(name, vertex, geometry, fragment);

			return ClrwlProgram.createProgram(name, isShadow, customSource, properties, irisPipeline.getCustomUniforms(), irisPipeline);
		}

		return null;
	}

	private Optional<String> compileOptionalStage(ClrwlPipelineStage<ClrwlShaderKey> stage, ClrwlShaderKey key, IrisRenderingPipeline irisPipeline, ProgramSource irisSources)
	{
		if (irisSources.getGeometrySource().isPresent())
		{
			return Optional.of(compileStage(stage, key, irisPipeline, irisSources));
		}
		else
		{
			return Optional.empty();
		}
	}

	private String compileStage(ClrwlPipelineStage<ClrwlShaderKey> stage, ClrwlShaderKey key, IrisRenderingPipeline irisPipeline, ProgramSource irisSources)
	{
		ProgramSet programSet = pack.getProgramSet(dimension);
		PackDirectives directives = programSet.getPackDirectives();
		ClrwlShaderProperties properties = ((ShaderPackAccessor) pack).colorwheel$getProperties();

		var compile = new ClrwlCompilation(irisPipeline, directives, properties, irisSources, sources);

		compile.version(GlCompat.MAX_GLSL_VERSION);

		for (var ext : pipeline.extensions())
		{
			compile.requireExtension(ext);
		}

		for (var ext : stage.extensions())
		{
			compile.enableExtension(ext);
		}

		for (var defines : stage.defines())
		{
			compile.define(defines);
		}

		stage.compile().accept(key, compile);

		for (var fetcher : stage.fetchers())
		{
			expand(fetcher.apply(key, compile), compile::appendComponent);
		}

		return compile.getShaderCode();
	}

	private static void expand(SourceComponent rootSource, Consumer<SourceComponent> out)
	{
		if (rootSource == null)
		{
			return;
		}

		var included = new LinkedHashSet<SourceComponent>(); // use hash set to deduplicate. linked to preserve order

		recursiveDepthFirstInclude(included, rootSource);
		included.add(rootSource);

		included.forEach(out);
	}

	private static void recursiveDepthFirstInclude(Set<SourceComponent> included, SourceComponent component)
	{
		for (var include : component.included())
		{
			recursiveDepthFirstInclude(included, include);
		}

		included.addAll(component.included());
	}

	public static void createFogComponent()
	{
		FOG = UberShaderComponent.builder(ResourceUtil.rl("fog"))
				.materialSources(ClrwlMaterialShaderIndices.fogSources().all())
				.adapt(FnSignature.create()
						.returnType("vec4")
						.name("flw_fogFilter")
						.arg("vec4", "color")
						.build(), GlslExpr.variable("color"))
				.switchOn(GlslExpr.variable("_flw_uberFogIndex"))
				.build(FlwPrograms.SOURCES);
	}

	private static void createCutoutComponent()
	{
		CUTOUT = UberShaderComponent.builder(ResourceUtil.rl("cutout"))
				.materialSources(ClrwlMaterialShaderIndices.cutoutSources().all())
				.adapt(FnSignature.create()
						.returnType("bool")
						.name("flw_discardPredicate")
						.arg("vec4", "color")
						.build(), GlslExpr.boolLiteral(false))
				.switchOn(GlslExpr.variable("_flw_uberCutoutIndex"))
				.build(FlwPrograms.SOURCES);
	}

	private static void dumpSources(String fileName, String source)
	{
		boolean shouldDump = Compilation.DUMP_SHADER_SOURCE || Iris.getIrisConfig().areDebugOptionsEnabled();

		if (!shouldDump)
		{
			return;
		}

		File file = new File(new File(Minecraft.getInstance().gameDirectory, "colorwheel_sources"), fileName);
		// mkdirs of the parent so we don't create a directory named by the leaf file we want to write
		file.getParentFile()
				.mkdirs();
		try (FileWriter writer = new FileWriter(file)) {
			writer.write(source);
		} catch (Exception e) {
			FlwPrograms.LOGGER.error("Could not dump source.", e);
		}
	}

	public enum OitMode
	{
		OFF("", ""),
		DEPTH_RANGE("CLRWL_DEPTH_RANGE", "_depth_range"),
		GENERATE_COEFFICIENTS("CLRWL_COLLECT_COEFFS", "_generate_coefficients"),
		EVALUATE("CLRWL_EVALUATE", "_resolve"),
		;

		public final String define;
		public final String name;

		OitMode(String define, String name)
		{
			this.define = define;
			this.name = name;
		}
	}
}
