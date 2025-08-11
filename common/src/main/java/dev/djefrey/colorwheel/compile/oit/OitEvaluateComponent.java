package dev.djefrey.colorwheel.compile.oit;

import dev.djefrey.colorwheel.Colorwheel;
import dev.djefrey.colorwheel.util.Utils;
import dev.djefrey.colorwheel.engine.ClrwlOitAccumulateOverride;
import dev.engine_room.flywheel.backend.glsl.SourceComponent;
import dev.engine_room.flywheel.backend.glsl.generate.FnSignature;
import dev.engine_room.flywheel.backend.glsl.generate.GlslBlock;
import dev.engine_room.flywheel.backend.glsl.generate.GlslBuilder;
import dev.engine_room.flywheel.backend.glsl.generate.GlslStmt;

import java.util.Collection;
import java.util.List;

public class OitEvaluateComponent implements SourceComponent
{
    private int[] drawBuffers;
    private int[] ranks;
    private List<ClrwlOitAccumulateOverride> overrides;

    public OitEvaluateComponent(int[] drawBuffers, int[] ranks, List<ClrwlOitAccumulateOverride> overrides)
    {
        this.drawBuffers = drawBuffers;
        this.ranks = ranks;
        this.overrides = overrides;
    }

    @Override
    public Collection<? extends SourceComponent> included()
    {
        return List.of();
    }

    @Override
    public String source()
    {
        var builder = new GlslBuilder();
        var body = new GlslBlock();

        body.add(GlslStmt.raw("float linearDepth = _clrwl_linear_depth();"));
        body.add(GlslStmt.raw("vec2 depthRange = texelFetch(_flw_depthRange, ivec2(gl_FragCoord.xy), 0).rg;"));
        body.add(GlslStmt.raw("float delta = depthRange.x + depthRange.y;"));
        body.add(GlslStmt.raw("float flw_depth = (linearDepth + depthRange.x) / delta;"));
        body.add(GlslStmt.raw("float depth_adjustment = _clrwl_tented_blue_noise(flw_depth) * _flw_oitNoise;"));
        body.add(GlslStmt.raw(""));

        String transmittance_from_depth = "depth_transmittance";
        body.add(GlslStmt.raw("float " + transmittance_from_depth + " = _clrwl_frontmost_transmittance_from_depth(linearDepth, depthRange);"));

        for (int i = 0; i < drawBuffers.length; i++)
        {
            int drawBuffer = drawBuffers[i];
            String outName = "iris_FragData" + i;

            var maybeCoeffId = Utils.findFirst(overrides, e -> e.drawBuffer() == drawBuffer)
                    .flatMap(ClrwlOitAccumulateOverride::coefficientId);

            if (maybeCoeffId.isPresent())
            {
                int coeffId = maybeCoeffId.get();
                int rank = ranks[coeffId];

                String transmittance = "transmittance" + drawBuffer;
                String coeffs = "clrwl_coefficients" + coeffId;
                String depth = "depth" + drawBuffer;
                String correctedTransmittance = "correctedTransmittance" + drawBuffer;

                body.add(GlslStmt.raw("float " + transmittance + " = 1. - " + outName + ".a;"));
                body.add(GlslStmt.raw("float " + depth + " = flw_depth;"));
                // Don't do the depth adjustment if this fragment is opaque.
                body.add(GlslStmt.raw("if (" + transmittance + " > 1e-5) { " + depth + " -= depth_adjustment; }"));
                body.add(GlslStmt.raw("float " + correctedTransmittance + " = _clrwl_signal_corrected_transmittance(" + coeffs + ", " + depth + ", " + transmittance + ", " + rank + ");"));
                body.add(GlslStmt.raw(outName + ".rgb *= " + outName + ".a;"));
                body.add(GlslStmt.raw(outName + " *= " + correctedTransmittance + ";"));
            }
            else // Frontmost
            {
                body.add(GlslStmt.raw(outName + " *= " + transmittance_from_depth +";"));
            }
        }

        builder.function()
                .signature(FnSignature.create()
                        .returnType("void")
                        .name("_clrwl_post_shader")
                        .build())
                .body(body);

        return builder.build();
    }

    @Override
    public String name()
    {
        return Colorwheel.rl("oit_evaluate").toString();
    }
}
