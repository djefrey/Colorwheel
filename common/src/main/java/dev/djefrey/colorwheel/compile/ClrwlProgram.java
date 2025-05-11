package dev.djefrey.colorwheel.compile;

import com.google.common.collect.ImmutableSet;
import dev.djefrey.colorwheel.ClrwlSamplers;
import dev.djefrey.colorwheel.Colorwheel;
import dev.djefrey.colorwheel.accessors.PackDirectivesAccessor;
import dev.djefrey.colorwheel.engine.ClrwlMaterialEncoder;
import dev.djefrey.colorwheel.engine.uniform.ClrwlUniforms;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.backend.engine.embed.EmbeddingUniforms;
import dev.engine_room.flywheel.backend.gl.shader.GlProgram;
import net.irisshaders.iris.gl.program.ProgramImages;
import net.irisshaders.iris.gl.program.ProgramSamplers;
import net.irisshaders.iris.gl.program.ProgramUniforms;
import net.irisshaders.iris.gl.shader.GlShader;
import net.irisshaders.iris.gl.shader.ShaderType;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shaderpack.properties.PackDirectives;
import net.irisshaders.iris.uniforms.custom.CustomUniforms;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL31;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ClrwlProgram
{
	private final int handle;
	private final ProgramUniforms uniforms;
	private final CustomUniforms customUniforms;
	private final ProgramSamplers samplers;
	private final ProgramImages images;

	public final int vertexOffsetUniform;
	public final int packedMaterialUniform;
	public final int modelMatrixUniform;
	public final int normalMatrixUniform;

	public static ImmutableSet<Integer> getReservedTextureUnits(Set<Integer> coeffs)
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

		for (int k : coeffs)
		{
			res.add(ClrwlSamplers.getCoefficient(k).number);
		}

		return ImmutableSet.copyOf(res);
	}

	private ClrwlProgram(String name, boolean isShadowPass, PackDirectives directives, String vertex, String fragment, CustomUniforms customUniforms, IrisRenderingPipeline pipeline)
	{
		var oitCoeffs = ((PackDirectivesAccessor) directives).getCoefficientsRanks(isShadowPass).keySet();

		this.handle = GL20.glCreateProgram();

		GL20.glBindAttribLocation(this.handle, 0, "_flw_aPos");
		GL20.glBindAttribLocation(this.handle, 1, "_flw_aColor");
		GL20.glBindAttribLocation(this.handle, 2, "_flw_aTexCoord");
		GL20.glBindAttribLocation(this.handle, 3, "_flw_aOverlay");
		GL20.glBindAttribLocation(this.handle, 4, "_flw_aLight");
		GL20.glBindAttribLocation(this.handle, 5, "_flw_aNormal");
		GL20.glBindAttribLocation(this.handle, 6, "_flw_aTangent");
		GL20.glBindAttribLocation(this.handle, 7, "_flw_aMidTexCoord");

		GlShader vert = new GlShader(ShaderType.VERTEX, name + ".vsh", vertex);
		GL20.glAttachShader(this.handle, vert.getHandle());

		GlShader frag = new GlShader(ShaderType.FRAGMENT, name + ".fsh", fragment);
		GL20.glAttachShader(this.handle, frag.getHandle());

		GL20.glLinkProgram(this.handle);

		if (GL20.glGetProgrami(this.handle, GL20.GL_LINK_STATUS) != GL20.GL_TRUE)
		{
			var err = new RuntimeException("Shader link error in Colorwheel program: " + GL20.glGetProgramInfoLog(this.handle));
			GL20.glDeleteProgram(this.handle);
			throw err;
		}

		ProgramUniforms.Builder uniformBuilder = ProgramUniforms.builder(name, this.handle);
		ProgramSamplers.Builder samplerBuilder = ProgramSamplers.builder(this.handle, getReservedTextureUnits(oitCoeffs));
		ProgramImages.Builder   imageBuilder   = ProgramImages.builder(this.handle);

		samplerBuilder.addExternalSampler(ClrwlSamplers.DIFFUSE.number, "flw_diffuseTex");
		samplerBuilder.addExternalSampler(ClrwlSamplers.OVERLAY.number, "flw_overlayTex");
		samplerBuilder.addExternalSampler(ClrwlSamplers.LIGHT.number, "flw_lightTex");
		samplerBuilder.addExternalSampler(ClrwlSamplers.CRUMBLING.number, "_flw_crumblingTex");
		samplerBuilder.addExternalSampler(ClrwlSamplers.INSTANCE_BUFFER.number, "_flw_instances");
		samplerBuilder.addExternalSampler(ClrwlSamplers.LIGHT_LUT.number, "_flw_lightLut");
		samplerBuilder.addExternalSampler(ClrwlSamplers.LIGHT_SECTIONS.number, "_flw_lightSections");
		samplerBuilder.addExternalSampler(ClrwlSamplers.DEPTH_RANGE.number, "_flw_depthRange");
		samplerBuilder.addExternalSampler(ClrwlSamplers.NOISE.number, "_flw_blueNoise");

		for (int k : oitCoeffs)
		{
			samplerBuilder.addExternalSampler(ClrwlSamplers.getCoefficient(k).number, "clrwl_coefficients" + k);
		}

		customUniforms.assignTo(uniformBuilder);
		pipeline.addGbufferOrShadowSamplers(samplerBuilder, imageBuilder,
				isShadowPass ? pipeline::getFlippedBeforeShadow : pipeline::getFlippedAfterPrepare,
				isShadowPass,
				false, false, false); // All false as we'll bind Flywheel samplers
		customUniforms.mapholderToPass(uniformBuilder, this);

		this.uniforms = uniformBuilder.buildUniforms();
		this.customUniforms = customUniforms;
		this.samplers = samplerBuilder.build();
		this.images = imageBuilder.build();

		this.vertexOffsetUniform = tryGetUniformLocation2("_flw_vertexOffset");
		this.packedMaterialUniform = tryGetUniformLocation2("_flw_packedMaterial");
		this.modelMatrixUniform = tryGetUniformLocation2(EmbeddingUniforms.MODEL_MATRIX);
		this.normalMatrixUniform = tryGetUniformLocation2(EmbeddingUniforms.NORMAL_MATRIX);

		ClrwlUniforms.setUniformBlockBinding(this);
	}

	private int tryGetUniformLocation2(CharSequence name) {
		return GL20.glGetUniformLocation(this.handle, name);
	}

	public static ClrwlProgram createProgram(String name, boolean isShadowPass, ProgramSource source, PackDirectives directives, CustomUniforms customUniforms, IrisRenderingPipeline pipeline)
	{
		String vertex = source.getVertexSource().orElseThrow(RuntimeException::new);
		String fragment = source.getFragmentSource().orElseThrow(RuntimeException::new);

		return new ClrwlProgram(name, isShadowPass,
							   directives,
							   vertex, fragment,
							   customUniforms, pipeline);
	}

	public void bind(int vertexOffset, Material material)
	{
		GL20.glUseProgram(this.handle);

		int packedFogAndCutout = ClrwlMaterialEncoder.packUberShader(material);
		int packedMaterialProperties = ClrwlMaterialEncoder.packProperties(material);

		setUniform(vertexOffsetUniform, vertexOffset);
		setUniform(packedMaterialUniform, packedFogAndCutout, packedMaterialProperties);

		samplers.update();
		uniforms.update();
		customUniforms.push(this);
		images.update();
	}

	public void unbind()
	{
		GL20.glUseProgram(0);
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

	public void free() {
		GL31.glDeleteProgram(this.handle);
	}

	private void setUniform(int index, int i) {
		GL31.glUniform1ui(index, i);
	}

	private void setUniform(int index, int x, int y) {
		GL31.glUniform2ui(index, x, y);
	}

	private void setUniform(int index, Matrix3f mat) {
		GL31.glUniformMatrix3fv(index, false, mat.get(new float[12]));
	}

	private void setUniform(int index, Matrix4f mat) {
		GL31.glUniformMatrix4fv(index, false, mat.get(new float[16]));
	}

	public GlProgram getProgram() { return new GlProgram(this.handle); }
}
