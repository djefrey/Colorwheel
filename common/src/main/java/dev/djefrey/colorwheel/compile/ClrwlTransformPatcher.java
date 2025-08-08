package dev.djefrey.colorwheel.compile;

import dev.engine_room.flywheel.api.material.Transparency;
import io.github.douira.glsl_transformer.ast.node.Identifier;
import io.github.douira.glsl_transformer.ast.node.TranslationUnit;
import io.github.douira.glsl_transformer.ast.node.Version;
import io.github.douira.glsl_transformer.ast.node.expression.LiteralExpression;
import io.github.douira.glsl_transformer.ast.node.expression.ReferenceExpression;
import io.github.douira.glsl_transformer.ast.node.expression.binary.ArrayAccessExpression;
import io.github.douira.glsl_transformer.ast.print.PrintType;
import io.github.douira.glsl_transformer.ast.query.Root;
import io.github.douira.glsl_transformer.ast.query.RootSupplier;
import io.github.douira.glsl_transformer.ast.transform.SingleASTTransformer;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.irisshaders.iris.gl.texture.TextureType;
import net.irisshaders.iris.helpers.Tri;
import net.irisshaders.iris.pipeline.transform.PatchShaderType;
import net.irisshaders.iris.pipeline.transform.transformer.CommonTransformer;
import net.irisshaders.iris.shaderpack.properties.ProgramDirectives;
import net.irisshaders.iris.shaderpack.texture.TextureStage;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClrwlTransformPatcher
{
	private static final SingleASTTransformer<ClrwlTransformParameters> transformer;
	public static final Pattern versionPattern = Pattern.compile("^.*#version\\h+(\\d+)\\V*", Pattern.DOTALL);
	public static final Pattern extensionPattern = Pattern.compile("^.*#extension\\s+([a-zA-Z0-9_]+)\\s+:\\s+([a-zA-Z0-9_]+)", Pattern.DOTALL);

	private static final String LIGHTMAP_SCALE = "0.935543854"; // 0.966793854 - 0.03125
	private static final String LIGHTMAP_OFFSET = "0.03125";
	private static final String LIGHTMAP_MATRIX = String.format("mat4(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)",
				LIGHTMAP_SCALE, "0.0",			  "0.0", "0.0",
				"0.0", 			LIGHTMAP_SCALE,   "0.0", "0.0",
				"0.0", 			"0.0", 			  "1.0", "0.0",
				LIGHTMAP_OFFSET, LIGHTMAP_OFFSET, "0.0", "1.0"
			);

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
				Matcher versionMatcher = versionPattern.matcher(input);

				if (!versionMatcher.find()) {
					throw new IllegalArgumentException(
							"No #version directive found in source code! See debugging.md for more information.");
				}

				transformer.getLexer().version = Version.fromNumber(Integer.parseInt(versionMatcher.group(1)));

				input = versionMatcher.replaceAll(""); // Remove version tag, replaced by Flywheel's one

				Matcher extensionMatcher = extensionPattern.matcher(input);  // Remove all extensions
				input = extensionMatcher.replaceAll("");

				return super.parseTranslationUnit(rootInstance, input);
			}
		};

		transformer.setPrintType(PrintType.INDENTED);

		transformer.setTransformation((tree, root, parameters) ->
		{
			tree.outputOptions.enablePrintInfo();

			root.indexBuildSession(() ->
			{
				root.replaceReferenceExpressions(transformer, "gl_Vertex", "flw_vertexPos");
				root.replaceReferenceExpressions(transformer, "gl_Normal", "flw_vertexNormal");
				root.replaceReferenceExpressions(transformer, "gl_Color", "flw_vertexColor");
				root.replaceReferenceExpressions(transformer, "at_midBlock", "clrwl_vertexMidMesh");
				root.replaceReferenceExpressions(transformer, "at_tangent", "clrwl_vertexTangent");
				root.replaceReferenceExpressions(transformer, "mc_Entity", "vec2(-1.0)");
				root.replaceReferenceExpressions(transformer, "mc_midTexCoord", "vec4(clrwl_vertexMidTexCoord, 0.0, 1.0)");

				root.replaceReferenceExpressions(transformer, "gl_MultiTexCoord0", "vec4(flw_vertexTexCoord, 0.0, 1.0)");
				root.replaceReferenceExpressions(transformer, "gl_MultiTexCoord1",  "vec4(flw_vertexLight, 0.0, 1.0)");
				root.replaceReferenceExpressions(transformer, "gl_MultiTexCoord2",  "vec4(flw_vertexLight, 0.0, 1.0)");

				root.rename("blockEntityId", "_clrwl_blockEntityId");
				root.rename("entityId", "_clrwl_entityId");
				root.replaceReferenceExpressions(transformer, "entityColor", "clrwl_overlayColor");

				root.replaceReferenceExpressions(transformer, "gl_ModelViewMatrix", "flw_view");
				root.replaceReferenceExpressions(transformer, "modelViewMatrix", "flw_view");
				root.replaceReferenceExpressions(transformer, "gl_ModelViewMatrixInverse", "flw_viewInverse");
				root.replaceReferenceExpressions(transformer, "modelViewMatrixInverse", "flw_viewInverse");

				root.replaceReferenceExpressions(transformer, "gl_ProjectionMatrix", "flw_projection");
				root.replaceReferenceExpressions(transformer, "projectionMatrix", "flw_projection");
				root.replaceReferenceExpressions(transformer, "gl_ProjectionMatrixInverse", "flw_projectionInverse");
				root.replaceReferenceExpressions(transformer, "projectionMatrixInverse", "flw_projectionInverse	");

				root.replaceReferenceExpressions(transformer, "gl_ModelViewProjectionMatrix", "flw_viewProjection");
				root.replaceReferenceExpressions(transformer, "modelViewProjectionMatrix", "flw_viewProjection");
				root.replaceReferenceExpressions(transformer, "gl_ModelViewProjectionMatrixInverse", "flw_viewProjectionInverse");
				root.replaceReferenceExpressions(transformer, "modelViewProjectionMatrixInverse", "flw_viewProjectionInverse");

				root.replaceExpressionMatches(transformer, CommonTransformer.glTextureMatrix0, "mat4(1.0)");
				root.replaceReferenceExpressions(transformer, "textureMatrix", "mat4(1.0)");
				root.replaceExpressionMatches(transformer, CommonTransformer.glTextureMatrix1, LIGHTMAP_MATRIX);
				root.replaceExpressionMatches(transformer, CommonTransformer.glTextureMatrix2, LIGHTMAP_MATRIX);

				root.replaceReferenceExpressions(transformer, "gl_NormalMatrix", "clrwl_normal");
				root.replaceReferenceExpressions(transformer, "normalMatrix", "clrwl_normal");

				root.replaceReferenceExpressions(transformer, "clrwl_view", "flw_view");
				root.replaceReferenceExpressions(transformer, "clrwl_viewInverse", "flw_viewInverse");
				root.replaceReferenceExpressions(transformer, "clrwl_viewPrev", "flw_viewPrev");
				root.replaceReferenceExpressions(transformer, "clrwl_projection", "flw_projection");
				root.replaceReferenceExpressions(transformer, "clrwl_projectionInverse", "flw_projectionInverse");
				root.replaceReferenceExpressions(transformer, "clrwl_projectionPrev", "flw_projectionPrev");
				root.replaceReferenceExpressions(transformer, "clrwl_viewProjection", "flw_viewProjection");
				root.replaceReferenceExpressions(transformer, "clrwl_viewProjectionInverse", "flw_viewProjectionInverse");
				root.replaceReferenceExpressions(transformer, "clrwl_viewProjectionPrev", "flw_viewProjectionPrev");
				root.replaceReferenceExpressions(transformer, "clrwl_renderOrigin", "flw_renderOrigin");
				root.replaceReferenceExpressions(transformer, "clrwl_cameraPos", "flw_cameraPos");

				root.rename("clrwl_vertexPos", "flw_vertexPos");
				root.rename("clrwl_vertexColor", "flw_vertexColor");
				root.rename("clrwl_vertexTexCoord", "flw_vertexTexCoord");
				root.rename("clrwl_vertexOverlay", "flw_vertexOverlay");
				root.rename("clrwl_vertexLight", "flw_vertexLight");
				root.rename("clrwl_vertexNormal", "flw_vertexNormal");
				root.rename("clrwl_distance", "flw_distance");

				root.replaceReferenceExpressions(transformer, "clrwl_materialFragment", "_clrwl_materialFragment_hook");
				root.replaceReferenceExpressions(transformer, "clrwl_shaderLight", "_clrwl_shaderLight_hook");

				// TODO: remove duplicated uniforms

				if (parameters.type == PatchShaderType.FRAGMENT)
				{
					if (root.identifierIndex.has("gl_FragColor"))
					{
						root.replaceReferenceExpressions(transformer, "gl_FragColor", "gl_FragData[0]");
					}

					var oit = parameters.getOit();

					if (oit == ClrwlPipelineCompiler.OitMode.DEPTH_RANGE
							||  oit == ClrwlPipelineCompiler.OitMode.GENERATE_COEFFICIENTS)
					{
						Map<ArrayAccessExpression, Long> toReplace = new HashMap<>();

						for (Identifier id : root.identifierIndex.get("gl_FragData"))
						{
							var access = id.getAncestor(ArrayAccessExpression.class);
							var idx = ((LiteralExpression) access.getRight()).getInteger();

							toReplace.put(access, idx);
						}

						for (var kv : toReplace.entrySet())
						{
							kv.getKey().replaceByAndDelete(new ReferenceExpression(new Identifier("clrwl_FragData" + kv.getValue())));
						}
					}
				}

				CommonTransformer.transform(transformer, tree, root, parameters, false);

				if (!parameters.isCrumbling())
				{
					root.replaceReferenceExpressions(transformer, "tex", "flw_diffuseTex");
					root.replaceReferenceExpressions(transformer, "gtexture", "flw_diffuseTex");
				}
				else
				{
					root.replaceReferenceExpressions(transformer, "tex", "_flw_crumblingTex");
					root.replaceReferenceExpressions(transformer, "gtexture", "_flw_crumblingTex");
				}

				root.rename("main", "_clrwl_shader_main");
			});
		});
	}

	public static String patchVertex(String vertex, Transparency transparency, ProgramDirectives programDirectives, Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> textureMap)
	{
		var directives =  ClrwlTransformParameters.Directives.fromVertex(programDirectives);
		var parameters = new ClrwlTransformParameters(PatchShaderType.VERTEX, ClrwlPipelineCompiler.OitMode.OFF, transparency, directives, textureMap);

		return transformer.transform(vertex, parameters);
	}

	public static String patchGeometry(String vertex, Transparency transparency, ProgramDirectives programDirectives, Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> textureMap)
	{
		var directives =  ClrwlTransformParameters.Directives.fromVertex(programDirectives);
		var parameters = new ClrwlTransformParameters(PatchShaderType.GEOMETRY, ClrwlPipelineCompiler.OitMode.OFF, transparency, directives, textureMap);

		return transformer.transform(vertex, parameters);
	}

	public static String patchFragment(String fragment, ClrwlPipelineCompiler.OitMode oit, Transparency transparency, ProgramDirectives programDirectives, Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> textureMap)
	{
		var directives =  ClrwlTransformParameters.Directives.fromFragment(programDirectives);
		var parameters = new ClrwlTransformParameters(PatchShaderType.FRAGMENT, oit, transparency, directives, textureMap);

		return transformer.transform(fragment, parameters);
	}
}
