package dev.djefrey.colorwheel.compile.oit;

import dev.djefrey.colorwheel.Colorwheel;
import dev.djefrey.colorwheel.compile.GlslFragmentOutput;
import dev.djefrey.colorwheel.engine.ClrwlOitCoeffDirective;
import dev.engine_room.flywheel.backend.glsl.ShaderSources;
import dev.engine_room.flywheel.backend.glsl.SourceComponent;
import dev.engine_room.flywheel.backend.glsl.generate.*;
import dev.engine_room.flywheel.lib.util.ResourceUtil;

import java.util.Collection;
import java.util.List;

public class OitCompositeComponent implements SourceComponent
{
    private final ShaderSources sources;
    private final int[] renderTargets;
    private final List<ClrwlOitCoeffDirective> coeffs;

    public OitCompositeComponent(ShaderSources sources, int[] renderTargets, List<ClrwlOitCoeffDirective> coeffs)
    {
        this.sources = sources;
        this.renderTargets = renderTargets;
        this.coeffs = coeffs;
    }

    @Override
    public Collection<? extends SourceComponent> included()
    {
        return List.of(
                sources.get(Colorwheel.rl("internal/oit/wavelet.glsl")),
                sources.get(Colorwheel.rl("internal/uniform/frame.glsl")),
                sources.get(ResourceUtil.rl("internal/depth.glsl"))
            );
    }

    @Override
    public String source()
    {
        var builder = new GlslBuilder();

        int targetCnt = renderTargets.length;
        int coeffCount = coeffs.size();
        var reverseMap = ClrwlOitCoeffDirective.getReverseMap(coeffs);

        OitCoefficientsSamplersComponent.addSamplers(builder, coeffCount);
        addAccumulateSamplers(builder, targetCnt);
        builder.uniform().type("sampler2D").name("_flw_depthRange");

        addOutputs(builder, targetCnt);

        var body = new GlslBlock();

        body.add(GlslStmt.raw("float minDepth = -texelFetch(_flw_depthRange, ivec2(gl_FragCoord.xy), 0).r;"));

        // Required otherwise depth buffer is corrupted
        body.add(GlslStmt.raw("if (minDepth == _flw_cullData.zfar) { discard; }"));

        body.add(GlslStmt.raw("gl_FragDepth = delinearize_depth(minDepth, _flw_cullData.znear, _flw_cullData.zfar);"));

        for (int i = 0; i < coeffCount; i++)
        {
            var name = "total_transmittance" + i;
            var coeffName = "clrwl_coefficients" + i;

            body.add(GlslStmt.raw("float " + name + " = total_transmittance(" + coeffName + ", " + coeffs.get(i).rank() + ");"));
        }

        for (int i = 0; i < targetCnt; i++)
        {
            var texelName = "texel" + i;
            var accumulate = "_clrwl_accumulate" + i;
            var out = "frag" + i;
            var totalName = "total_transmittance" + reverseMap.get(i);

            body.add(GlslStmt.raw("vec4 " + texelName + " = texelFetch(" + accumulate + ", ivec2(gl_FragCoord.xy), 0);"));
            body.add(GlslStmt.raw(out + " = vec4(" + texelName + ".rgb / " + texelName + ".a, 1. - " + totalName + ");"));
        }

        builder.function()
                .signature(FnSignature.create().returnType("void").name("main").build())
                .body(body);

        return builder.build();
    }

    @Override
    public String name()
    {
        return Colorwheel.rl("oit_composite").toString();
    }

    public static void addOutputs(GlslBuilder builder, int targetCnt)
    {
        for (int i = 0; i < targetCnt; i++)
        {
            var out = new GlslFragmentOutput()
                    .binding(i)
                    .type("vec4")
                    .name("frag" + i);

            builder.add(out);
        }
    }

    public static void addAccumulateSamplers(GlslBuilder builder, int targetCnt)
    {
        for (int i = 0; i < targetCnt; i++)
        {
            var uniform = new GlslUniform()
                    .type("sampler2D")
                    .name("_clrwl_accumulate" + i);

            builder.add(uniform);
        }
    }
}
