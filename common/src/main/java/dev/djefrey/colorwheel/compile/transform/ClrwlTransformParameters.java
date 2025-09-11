package dev.djefrey.colorwheel.compile.transform;

import dev.djefrey.colorwheel.compile.ClrwlPipelineCompiler;
import dev.engine_room.flywheel.api.material.Transparency;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.irisshaders.iris.gl.blending.AlphaTest;
import net.irisshaders.iris.gl.texture.TextureType;
import net.irisshaders.iris.helpers.Tri;
import net.irisshaders.iris.pipeline.transform.Patch;
import net.irisshaders.iris.pipeline.transform.PatchShaderType;
import net.irisshaders.iris.pipeline.transform.parameter.Parameters;
import net.irisshaders.iris.shaderpack.properties.ProgramDirectives;
import net.irisshaders.iris.shaderpack.texture.TextureStage;

// ColorwheelTransformParameters extends from Parameters so that CommonTransformer.transform can be used
// This requires a Patch assigned, Vanilla is assigned as a default value
// DO NOT USE IN TRANSFORM PATCHER
public class ClrwlTransformParameters extends Parameters
{
	private final boolean isCrumbling;
	private final boolean customOutputs;
	private final Directives directives;

	public ClrwlTransformParameters(PatchShaderType type, boolean isCrumbling, boolean customOutputs, Directives directives, Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> textureMap)
	{
		super(Patch.VANILLA, textureMap);
		super.type = type;
		this.isCrumbling = isCrumbling;
		this.customOutputs = customOutputs;
		this.directives = directives;
	}

	public boolean usesCustomOutputs()
	{
		return customOutputs;
	}

	public boolean isCrumbling()
	{
		return isCrumbling;
	}

	public Directives directives()
	{
		return directives;
	}

	@Override
	public AlphaTest getAlphaTest() { return AlphaTest.ALWAYS; }

	@Override
	public TextureStage getTextureStage() { return TextureStage.GBUFFERS_AND_SHADOW; }

	@Override
	public int hashCode()
	{
		final int prime = 61; // Another prime is used to prevent conflict with base TransformParameters
		int result = 1;
		result = prime * result + ((patch == null) ? 0 : patch.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + (isCrumbling ? 0 : 1);
		result = prime * result + (customOutputs ? 0 : 1);
		result = prime * result + directives.hashCode();
		return result;
	}

	public record Directives()
	{
		public static Directives fromVertex(ProgramDirectives directives)
		{
			return new Directives();
		}

		public static Directives fromFragment(ProgramDirectives directives)
		{
			return new Directives();
		}
	}
}
