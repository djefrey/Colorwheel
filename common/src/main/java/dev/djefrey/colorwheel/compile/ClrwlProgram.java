package dev.djefrey.colorwheel.compile;

import com.google.common.collect.ImmutableSet;
import com.mojang.blaze3d.shaders.ProgramManager;
import dev.djefrey.colorwheel.engine.ClrwlBlendModeOverride;
import dev.djefrey.colorwheel.engine.ClrwlRenderingPhase;
import dev.djefrey.colorwheel.shaderpack.ClrwlProgramGroup;
import dev.djefrey.colorwheel.ClrwlSamplers;
import dev.djefrey.colorwheel.shaderpack.ClrwlProgramId;
import dev.djefrey.colorwheel.shaderpack.ClrwlShaderProperties;
import dev.djefrey.colorwheel.Colorwheel;
import dev.djefrey.colorwheel.engine.ClrwlInstanceVisual;
import dev.djefrey.colorwheel.engine.ClrwlMaterialEncoder;
import dev.djefrey.colorwheel.engine.uniform.ClrwlUniforms;
import dev.djefrey.colorwheel.util.Utils;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.backend.engine.embed.EmbeddingUniforms;
import dev.engine_room.flywheel.backend.gl.shader.GlProgram;
import net.irisshaders.iris.gl.blending.BlendMode;
import net.irisshaders.iris.gl.program.ProgramImages;
import net.irisshaders.iris.gl.program.ProgramSamplers;
import net.irisshaders.iris.gl.program.ProgramUniforms;
import net.irisshaders.iris.gl.shader.GlShader;
import net.irisshaders.iris.gl.shader.ShaderType;
import net.irisshaders.iris.gl.state.FogMode;
import net.irisshaders.iris.mixin.texture.TextureAtlasAccessor;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.uniforms.CommonUniforms;
import net.irisshaders.iris.uniforms.custom.CustomUniforms;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3fc;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL31;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class ClrwlProgram
{
	private final GlShader vertex;
	@Nullable
	private final GlShader geometry;
	private final GlShader fragment;
	private final int handle;
	private final ProgramUniforms uniforms;
	private final CustomUniforms customUniforms;
	private final ProgramSamplers samplers;
	private final ProgramImages images;

	public final int baseVertexUniform;
	public final int baseInstanceUniform;
	public final int packedMaterialUniform;
	public final int modelMatrixUniform;
	public final int normalMatrixUniform;
	public final int blockEntityUniform;
	public final int entityUniform;
	public final int meshCenterUniform;
	public final int renderPhaseUniform;
	public final int blendFuncUniform;
	public final int atlasSizeUniform;

	public static ImmutableSet<Integer> getReservedTextureUnits(int coeffCount)
	{
		List<Integer> res = new ArrayList<>();

		res.add(ClrwlSamplers.DIFFUSE.number);
		res.add(ClrwlSamplers.OVERLAY.number);
		res.add(ClrwlSamplers.LIGHT.number);
		res.add(ClrwlSamplers.CRUMBLING.number);
		res.add(ClrwlSamplers.INSTANCE_BUFFER.number);
		res.add(ClrwlSamplers.LIGHT_LUT.number);
		res.add(ClrwlSamplers.LIGHT_SECTIONS.number);

		// OIT Samplers
		res.add(ClrwlSamplers.DEPTH_RANGE.number);
		res.add(ClrwlSamplers.NOISE.number);

		for (int i = 0; i < coeffCount; i++)
		{
			res.add(ClrwlSamplers.getCoefficient(i).number);
		}

		return ImmutableSet.copyOf(res);
	}

	private ClrwlProgram(String name, ClrwlProgramId programId, ClrwlShaderProperties properties,
						 String vertex, Optional<String> geometry, String fragment,
						 CustomUniforms customUniforms, IrisRenderingPipeline pipeline)
	{
		this.vertex = new GlShader(ShaderType.VERTEX, name + ".vsh", vertex);
		this.geometry = geometry.map(sh -> new GlShader(ShaderType.GEOMETRY, name + ".gsh", sh)).orElse(null);
		this.fragment = new GlShader(ShaderType.FRAGMENT, name + ".fsh", fragment);

		this.handle = GL20.glCreateProgram();

		GL20.glAttachShader(this.handle, this.vertex.getHandle());

		if (this.geometry != null)
		{
			GL20.glAttachShader(this.handle, this.geometry.getHandle());
		}

		GL20.glAttachShader(this.handle, this.fragment.getHandle());

		GL20.glBindAttribLocation(this.handle, 0, "_flw_aPos");
		GL20.glBindAttribLocation(this.handle, 1, "_flw_aColor");
		GL20.glBindAttribLocation(this.handle, 2, "_flw_aTexCoord");
		GL20.glBindAttribLocation(this.handle, 3, "_flw_aLight");
		GL20.glBindAttribLocation(this.handle, 4, "_flw_aNormal");
		GL20.glBindAttribLocation(this.handle, 5, "_clrwl_aEntity");
		GL20.glBindAttribLocation(this.handle, 6, "_clrwl_aMidTexCoord");
		GL20.glBindAttribLocation(this.handle, 7, "_clrwl_aTangent");
		GL20.glBindAttribLocation(this.handle, 8, "_clrwl_aMidBlock");
		GL20.glBindAttribLocation(this.handle, 9, "_flw_aOverlay");

		GL20.glLinkProgram(this.handle);

		if (GL20.glGetProgrami(this.handle, GL20.GL_LINK_STATUS) != GL20.GL_TRUE)
		{
			var err = new RuntimeException("Shader link error in Colorwheel program: " + GL20.glGetProgramInfoLog(this.handle));
			GL20.glDeleteProgram(this.handle);

			this.vertex.destroy();
			if (this.geometry != null)
			{
				this.geometry.destroy();
			}
			this.fragment.destroy();

			throw err;
		}

		var oitCoeffs = properties.getOitCoeffRanks(programId.group());

		ProgramUniforms.Builder uniformBuilder = ProgramUniforms.builder(name, this.handle);
		ProgramSamplers.Builder samplerBuilder = ProgramSamplers.builder(this.handle, getReservedTextureUnits(oitCoeffs.length));
		ProgramImages.Builder   imageBuilder   = ProgramImages.builder(this.handle);

		samplerBuilder.addExternalSampler(ClrwlSamplers.DIFFUSE.number, "flw_diffuseTex");
		samplerBuilder.addExternalSampler(ClrwlSamplers.OVERLAY.number, "flw_overlayTex");
		// samplerBuilder.addExternalSampler(ClrwlSamplers.LIGHT.number, "flw_lightTex");
		samplerBuilder.addExternalSampler(ClrwlSamplers.CRUMBLING.number, "_flw_crumblingTex");
		samplerBuilder.addExternalSampler(ClrwlSamplers.INSTANCE_BUFFER.number, "_flw_instances");
		samplerBuilder.addExternalSampler(ClrwlSamplers.LIGHT_LUT.number, "_flw_lightLut");
		samplerBuilder.addExternalSampler(ClrwlSamplers.LIGHT_SECTIONS.number, "_flw_lightSections");
		samplerBuilder.addExternalSampler(ClrwlSamplers.DEPTH_RANGE.number, "_flw_depthRange");
		samplerBuilder.addExternalSampler(ClrwlSamplers.NOISE.number, "_flw_blueNoise");

		for (int i = 0; i < oitCoeffs.length; i++)
		{
			samplerBuilder.addExternalSampler(ClrwlSamplers.getCoefficient(i).number, "clrwl_coefficients" + i);
		}

		var isShadowPass = programId.group() == ClrwlProgramGroup.SHADOW;
		Supplier<ImmutableSet<Integer>> flipped = isShadowPass
				? pipeline::getFlippedBeforeShadow
				: programId.afterTranslucent()
					? pipeline::getFlippedAfterTranslucent
					: pipeline::getFlippedAfterPrepare;

		CommonUniforms.addDynamicUniforms(uniformBuilder, FogMode.PER_VERTEX);
		customUniforms.assignTo(uniformBuilder);
		pipeline.addGbufferOrShadowSamplers(samplerBuilder, imageBuilder,
				flipped, isShadowPass,
				false, true, false); // Use Flywheel texture and overlay samplers
		customUniforms.mapholderToPass(uniformBuilder, this);

		this.uniforms = uniformBuilder.buildUniforms();
		this.customUniforms = customUniforms;
		this.samplers = samplerBuilder.build();
		this.images = imageBuilder.build();

		this.baseVertexUniform = tryGetUniformLocation2("_flw_baseVertex");
		this.baseInstanceUniform = tryGetUniformLocation2("_flw_baseInstance");
		this.packedMaterialUniform = tryGetUniformLocation2("_clrwl_packedMaterial");
		this.modelMatrixUniform = tryGetUniformLocation2(EmbeddingUniforms.MODEL_MATRIX);
		this.normalMatrixUniform = tryGetUniformLocation2(EmbeddingUniforms.NORMAL_MATRIX);
		this.blockEntityUniform = tryGetUniformLocation2("_clrwl_blockEntityId");
		this.entityUniform = tryGetUniformLocation2("_clrwl_entityId");
		this.meshCenterUniform = tryGetUniformLocation2("_clrwl_meshCenter");
		this.renderPhaseUniform = tryGetUniformLocation2("_clrwl_renderPhase");
		this.blendFuncUniform = tryGetUniformLocation2("_clrwl_blendFunc");
		this.atlasSizeUniform = tryGetUniformLocation2("_clrwl_atlasSize");

		ClrwlUniforms.setUniformBlockBinding(this);
	}

	private int tryGetUniformLocation2(CharSequence name) {
		return GL20.glGetUniformLocation(this.handle, name);
	}

	public static ClrwlProgram createProgram(String name, ClrwlProgramId programId, ClrwlProgramSource source, ClrwlShaderProperties properties, CustomUniforms customUniforms, IrisRenderingPipeline pipeline)
	{
		return new ClrwlProgram(name, programId, properties,
							    source.vertex(), source.geometry(), source.fragment(),
							    customUniforms, pipeline);
	}

	public void bind(int baseVertex, int baseInstance, Material material, ClrwlInstanceVisual visual, Vector3fc meshCenter, ClrwlRenderingPhase phase, ClrwlBlendModeOverride blendModeOverride)
	{
		ProgramManager.glUseProgram(this.handle);

		int packedMaterialProperties = ClrwlMaterialEncoder.packProperties(material);

		var abstractTexture = Minecraft.getInstance().getTextureManager().getTexture(material.texture());
		int atlasWidth = 0;
		int atlasHeight = 0;

		if (abstractTexture instanceof TextureAtlas atlas)
		{
			atlasWidth = ((TextureAtlasAccessor) atlas).callGetWidth();
			atlasHeight = ((TextureAtlasAccessor) atlas).callGetWidth();
		}

		BlendMode blendMode;

		if (blendModeOverride != null)
		{
			if (blendModeOverride.blendMode() != null)
			{
				blendMode = blendModeOverride.blendMode();
			}
			else
			{
				blendMode = Utils.transparencyToBlendMode(Transparency.OPAQUE);
			}
		}
		else
		{
			blendMode = Utils.transparencyToBlendMode(material.transparency());
		}

		setUniformU(baseVertexUniform, baseVertex);
		setUniformS(baseInstanceUniform, baseInstance);
		setUniformU(packedMaterialUniform, packedMaterialProperties);

		setUniformS(blockEntityUniform, visual.getBlockEntity());
		setUniformS(entityUniform, visual.getEntity());
		setUniform(meshCenterUniform, meshCenter.x(), meshCenter.y(), meshCenter.z(), (float) visual.lightEmission());
		setUniformS(renderPhaseUniform, phase.getValue());
		setUniformI(blendFuncUniform, blendMode.srcRgb(), blendMode.dstRgb(), blendMode.srcAlpha(), blendMode.dstAlpha());
		setUniformI(atlasSizeUniform, atlasWidth, atlasHeight);

		samplers.update();
		uniforms.update();
		customUniforms.push(this);
		images.update();
	}

	public void unbind()
	{
		ProgramUniforms.clearActiveUniforms();
		ProgramSamplers.clearActiveSamplers();
	}

	public void setEmbeddedMatrices(Matrix4f model,  Matrix3f normal)
	{
		if (modelMatrixUniform != -1)
		{
			setUniform(modelMatrixUniform, model);
		}

		if (normalMatrixUniform != -1)
		{
			setUniform(modelMatrixUniform, normal);
		}
	}

	public void setUniformBlockBinding(String name, int binding)
	{
		int index = GL31.glGetUniformBlockIndex(handle, name);

		if (index == GL31.GL_INVALID_INDEX)
		{
			Colorwheel.LOGGER.debug("No uniform block for {}", name);
			return;
		}

		GL31.glUniformBlockBinding(handle, index, binding);
	}

	public void free()
	{
		GL31.glDeleteProgram(this.handle);
		this.vertex.destroy();
        this.fragment.destroy();
	}

	private void setUniformS(int index, int i) {
		GL31.glUniform1i(index, i);
	}

	private void setUniformU(int index, int i) {
		GL31.glUniform1ui(index, i);
	}

	private void setUniformI(int index, int x, int y) {
		GL31.glUniform2i(index, x, y);
	}

	private void setUniformI(int index, int x, int y, int z, int w) {
		GL31.glUniform4i(index, x, y, z, w);
	}

	private void setUniform(int index, float x, float y, float z, float w) {
		GL31.glUniform4f(index, x, y, z, w);
	}

	private void setUniform(int index, Matrix3f mat) {
		GL31.glUniformMatrix3fv(index, false, mat.get(new float[12]));
	}

	private void setUniform(int index, Matrix4f mat) {
		GL31.glUniformMatrix4fv(index, false, mat.get(new float[16]));
	}

	private GlProgram flwProgram;

	public GlProgram getProgram()
	{
		if (flwProgram == null)
		{
			flwProgram = new GlProgram(this.handle);
		}

		return flwProgram;
	}
}
