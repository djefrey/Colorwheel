package dev.djefrey.colorwheel.compile.oit;

import dev.djefrey.colorwheel.Colorwheel;
import dev.djefrey.colorwheel.compile.GlslFragmentOutput;
import dev.engine_room.flywheel.backend.glsl.ShaderSources;
import dev.engine_room.flywheel.backend.glsl.SourceComponent;
import dev.engine_room.flywheel.backend.glsl.generate.*;
import dev.engine_room.flywheel.lib.util.ResourceUtil;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class OitCompositeComponent implements SourceComponent
{
    private final ShaderSources sources;
    private final Map<Integer, Integer> translucents;
    private final List<Integer> opaques;
    private final Map<Integer, Integer> ranks;

    public OitCompositeComponent(ShaderSources sources, Map<Integer, Integer> translucents, List<Integer> opaques, Map<Integer, Integer> ranks)
    {
        this.sources = sources;
        this.translucents = translucents;
        this.opaques = opaques;
        this.ranks = ranks;
    }

    @Override
    public Collection<? extends SourceComponent> included()
    {
        return List.of(
                sources.get(Colorwheel.rl("internal/oit/wavelet.glsl")),
                sources.get(Colorwheel.rl("internal/uniform/frame.glsl")),
                sources.get(Colorwheel.rl("internal/depth.glsl"))
            );
    }

    @Override
    public String source()
    {
        var builder = new GlslBuilder();
        int targetCnt = translucents.size() + opaques.size();

        var sortedCoeffs = ranks.keySet().stream().sorted().toList();
        var sortedTranslucents = translucents.keySet().stream().sorted().toList();
        var sortedOpaques = opaques.stream().sorted().toList();

        OitCoefficientsSamplersComponent.addSamplers(builder, sortedCoeffs);
        addAccumulateSamplers(builder, sortedTranslucents, sortedOpaques);
        builder.uniform().type("sampler2D").name("_flw_depthRange");

        addOutputs(builder, targetCnt);

        var body = new GlslBlock();

        body.add(GlslStmt.raw("float minDepth = -texelFetch(_flw_depthRange, ivec2(gl_FragCoord.xy), 0).r;"));

        // Required otherwise depth buffer is corrupted
        body.add(GlslStmt.raw("if (minDepth == _flw_cullData.zfar) { discard; }"));

        body.add(GlslStmt.raw("gl_FragDepth = _clrwl_delinearize_depth(minDepth, _flw_cullData.znear, _flw_cullData.zfar);"));

        for (int k : sortedCoeffs)
        {
            var rank = ranks.get(k);

            if (rank == null)
            {
                rank = 3;
            }

            var name = "total_transmittance" + k;
            var coeffName = "clrwl_coefficients" + k;

            body.add(GlslStmt.raw("float " + name + " = _clrwl_total_transmittance(" + coeffName + ", " + rank + ");"));
        }

        for (int k : sortedTranslucents)
        {
            var coeffId = sortedTranslucents.get(k);

            if (coeffId == null)
            {
                coeffId = 0;
            }

            var texelName = "texelTranslucent" + k;
            var accumulate = "_clrwl_accumulate" + k;
            var out = "frag" + k;
            var totalName = "total_transmittance" + coeffId;

            body.add(GlslStmt.raw("vec4 " + texelName + " = texelFetch(" + accumulate + ", ivec2(gl_FragCoord.xy), 0);"));
            body.add(GlslStmt.raw(out + " = vec4(" + texelName + ".rgb / " + texelName + ".a, 1. - " + totalName + ");"));
        }

        for (int k : sortedOpaques)
        {
            var accumulate = "_clrwl_opaque" + k;
            var out = "frag" + (translucents.size() + k);

            body.add(GlslStmt.raw(out + " = texelFetch(" + accumulate + ", ivec2(gl_FragCoord.xy), 0);"));
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

    public static void addAccumulateSamplers(GlslBuilder builder, List<Integer> translucents, List<Integer> opaques)
    {
        for (int i : translucents)
        {
            var uniform = new GlslUniform()
                    .type("sampler2D")
                    .name("_clrwl_accumulate" + i);

            builder.add(uniform);
        }

        for (int i : opaques)
        {
            var uniform = new GlslUniform()
                    .type("sampler2D")
                    .name("_clrwl_opaque" + i);

            builder.add(uniform);
        }
    }
}
