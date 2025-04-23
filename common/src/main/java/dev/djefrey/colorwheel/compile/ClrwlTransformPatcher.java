package dev.djefrey.colorwheel.compile;

import io.github.douira.glsl_transformer.ast.node.TranslationUnit;
import io.github.douira.glsl_transformer.ast.node.statement.Statement;
import io.github.douira.glsl_transformer.ast.print.PrintType;
import io.github.douira.glsl_transformer.ast.query.Root;
import io.github.douira.glsl_transformer.ast.query.RootSupplier;
import io.github.douira.glsl_transformer.ast.transform.SingleASTTransformer;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.irisshaders.iris.gl.texture.TextureType;
import net.irisshaders.iris.helpers.Tri;
import net.irisshaders.iris.pipeline.transform.PatchShaderType;
import net.irisshaders.iris.pipeline.transform.transformer.CommonTransformer;
import net.irisshaders.iris.shaderpack.texture.TextureStage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClrwlTransformPatcher
{
	private static final SingleASTTransformer<ClrwlTransformParameters> transformer;
	private static final Pattern versionPattern = Pattern.compile("^.*#version\\s+(\\d+)", Pattern.DOTALL);

	static
	{
		transformer = new SingleASTTransformer<>()
		{
			{
				setRootSupplier(RootSupplier.PREFIX_UNORDERED_ED_EXACT);
			}

			@Override
			public TranslationUnit parseTranslationUnit(Root rootInstance, String input)
			{
				Matcher matcher = versionPattern.matcher(input);

				if (!matcher.find()) {
					throw new IllegalArgumentException(
							"No #version directive found in source code! See debugging.md for more information.");
				}

				input = matcher.replaceAll(""); // Remove version tag, replaced by Flywheel's one

				return super.parseTranslationUnit(rootInstance, input);
			}
		};

		transformer.setPrintType(PrintType.INDENTED);

		transformer.setTransformation((tree, root, parameters) ->
		{
			tree.outputOptions.enablePrintInfo();

			root.indexBuildSession(() ->
			{
				root.replaceExpressionMatches(transformer, CommonTransformer.glTextureMatrix0, "mat4(1.0)");
				root.replaceExpressionMatches(transformer, CommonTransformer.glTextureMatrix1, "mat4(1.0)");
				root.replaceExpressionMatches(transformer, CommonTransformer.glTextureMatrix2, "mat4(1.0)");

				root.rename("gl_MultiTexCoord2", "gl_MultiTexCoord1");

				root.replaceReferenceExpressions(transformer, "gl_Vertex", "vec4(_flw_aPos, 1.0)");
				root.replaceReferenceExpressions(transformer, "gl_Normal", "_flw_aNormal");
				root.replaceReferenceExpressions(transformer, "gl_Color", "_flw_aColor");
				root.replaceReferenceExpressions(transformer, "gl_MultiTexCoord0", "vec4(_flw_aTexCoord, 0.0, 1.0)");
				root.replaceReferenceExpressions(transformer, "gl_MultiTexCoord1",  "vec4(_flw_aLight, 0.0, 1.0)");

//				root.replaceReferenceExpressions(transformer, "gl_NormalMatrix", "clrwl_normal");

				root.replaceReferenceExpressions(transformer, "blockEntityId", "2147483647");
				root.replaceReferenceExpressions(transformer, "entityId", "2147483647");

				// TODO: remove duplicated uniforms

				CommonTransformer.transform(transformer, tree, root, parameters, false);

				root.rename("main", "_flw_shader_main");

				if (parameters.type == PatchShaderType.FRAGMENT && root.identifierIndex.has("iris_FragData0"))
				{
					// Insert assign to ensure that discard test is correct
					var statement = transformer.parseStatement(root, "flw_fragColor = iris_FragData0;");
					tree.appendFunctionBody("_flw_shader_main", statement);
				}
			});
		});
	}

	public static String patchVertex(String vertex, Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> textureMap)
	{
		var parameters = new ClrwlTransformParameters(PatchShaderType.VERTEX, textureMap);

		return transformer.transform(vertex, parameters);
	}

	public static String patchFragment(String fragment, Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> textureMap)
	{
		var parameters = new ClrwlTransformParameters(PatchShaderType.FRAGMENT, textureMap);

		return transformer.transform(fragment, parameters);
	}
}
