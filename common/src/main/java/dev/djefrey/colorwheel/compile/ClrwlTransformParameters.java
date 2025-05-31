package dev.djefrey.colorwheel.compile;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.irisshaders.iris.gl.blending.AlphaTest;
import net.irisshaders.iris.gl.texture.TextureType;
import net.irisshaders.iris.helpers.Tri;
import net.irisshaders.iris.pipeline.transform.Patch;
import net.irisshaders.iris.pipeline.transform.PatchShaderType;
import net.irisshaders.iris.pipeline.transform.parameter.Parameters;
import net.irisshaders.iris.shaderpack.texture.TextureStage;

// ColorwheelTransformParameters extends from Parameters so that CommonTransformer.transform can be used
// This requires a Patch assigned, Vanilla is assigned as a default value
// DO NOT USE IN TRANSFORM PATCHER
public class ClrwlTransformParameters extends Parameters
{
	private final ClrwlPipelineCompiler.OitMode oit;
	private final boolean isCrumbling;

	public ClrwlTransformParameters(PatchShaderType type, ClrwlPipelineCompiler.OitMode oit, boolean isCrumbling, Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> textureMap)
	{
		super(Patch.VANILLA, textureMap);
		super.type = type;
		this.oit = oit;
		this.isCrumbling = isCrumbling;
	}

	public ClrwlPipelineCompiler.OitMode getOit()
	{
		return oit;
	}

	public boolean isCrumbling()
	{
		return isCrumbling;
	}

	@Override
	public AlphaTest getAlphaTest() { return AlphaTest.ALWAYS; }

	@Override
	public TextureStage getTextureStage() { return TextureStage.GBUFFERS_AND_SHADOW; }

	@Override
	public int hashCode() {
		final int prime = 61; // Another prime is used to prevent conflict with base TransformParameters
		int result = 1;
		result = prime * result + ((patch == null) ? 0 : patch.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((getTextureMap() == null) ? 0 : getTextureMap().hashCode());
		return result;
	}
}
