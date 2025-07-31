package dev.djefrey.colorwheel.compile.oit;

import dev.djefrey.colorwheel.Colorwheel;
import dev.djefrey.colorwheel.Utils;
import dev.djefrey.colorwheel.compile.GlslFragmentOutput;
import dev.djefrey.colorwheel.engine.ClrwlOitAccumulateOverride;
import dev.engine_room.flywheel.backend.glsl.ShaderSources;
import dev.engine_room.flywheel.backend.glsl.SourceComponent;
import dev.engine_room.flywheel.backend.glsl.generate.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class OitCompositeComponent implements SourceComponent
{
    private final ShaderSources sources;
    private final int[] drawBuffers;
    private final int[] ranks;
    private final List<ClrwlOitAccumulateOverride> overrides;

    public OitCompositeComponent(ShaderSources sources, int[] drawBuffers, int[] ranks, List<ClrwlOitAccumulateOverride> overrides)
    {
        this.sources = sources;
        this.drawBuffers = drawBuffers;
        this.ranks = ranks;
        this.overrides = overrides;
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

        OitCoefficientsSamplersComponent.addSamplers(builder, ranks.length);
        addAccumulateSamplers(builder, drawBuffers);
        builder.uniform().type("sampler2D").name("_flw_depthRange");

        addOutputs(builder, drawBuffers.length);

        var body = new GlslBlock();

        body.add(GlslStmt.raw("float minDepth = -texelFetch(_flw_depthRange, ivec2(gl_FragCoord.xy), 0).r;"));

        // Required otherwise depth buffer is corrupted
        body.add(GlslStmt.raw("if (minDepth == _flw_cullData.zfar) { discard; }"));

        body.add(GlslStmt.raw("gl_FragDepth = _clrwl_delinearize_depth(minDepth, _flw_cullData.znear, _flw_cullData.zfar);"));

        for (int i = 0; i < ranks.length; i++)
        {
            var rank = ranks[i];

            var name = "total_transmittance" + i;
            var coeffName = "clrwl_coefficients" + i;

            body.add(GlslStmt.raw("float " + name + " = _clrwl_total_transmittance(" + coeffName + ", " + rank + ");"));
        }

        for (int i = 0; i < drawBuffers.length; i++)
        {
            var drawBuffer = drawBuffers[i];
            Optional<Integer> coeffId = Utils.findFirst(overrides, e -> e.drawBuffer() == drawBuffer)
                .flatMap(ClrwlOitAccumulateOverride::coefficientId);

            var accumulate = "_clrwl_accumulate" + drawBuffer;
            var out = "frag" + i;

            if (coeffId.isPresent())
            {
                var texelName = "texelTranslucent" + drawBuffer;
                var totalName = "total_transmittance" + coeffId.get();

                body.add(GlslStmt.raw("vec4 " + texelName + " = texelFetch(" + accumulate + ", ivec2(gl_FragCoord.xy), 0);"));
                body.add(GlslStmt.raw(out + " = vec4(" + texelName + ".rgb / " + texelName + ".a, 1. - " + totalName + ");"));
            }
            else // Frontmost
            {
                body.add(GlslStmt.raw(out + " = texelFetch(" + accumulate + ", ivec2(gl_FragCoord.xy), 0);"));
            }
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

    public static void addAccumulateSamplers(GlslBuilder builder, int[] drawBuffers)
    {
        for (int drawBuffer : drawBuffers)
        {
            var uniform = new GlslUniform()
                    .type("sampler2D")
                    .name("_clrwl_accumulate" + drawBuffer);

            builder.add(uniform);
        }
    }
}
