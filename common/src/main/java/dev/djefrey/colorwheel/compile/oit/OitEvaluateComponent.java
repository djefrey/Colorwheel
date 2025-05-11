package dev.djefrey.colorwheel.compile.oit;

import dev.djefrey.colorwheel.Colorwheel;
import dev.engine_room.flywheel.backend.glsl.SourceComponent;
import dev.engine_room.flywheel.backend.glsl.generate.FnSignature;
import dev.engine_room.flywheel.backend.glsl.generate.GlslBlock;
import dev.engine_room.flywheel.backend.glsl.generate.GlslBuilder;
import dev.engine_room.flywheel.backend.glsl.generate.GlslStmt;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class OitEvaluateComponent implements SourceComponent
{
    private final Map<Integer, Integer> translucents;
    private final Map<Integer, Integer> opaques;
    private final Map<Integer, Integer> ranks;

    public OitEvaluateComponent(Map<Integer, Integer> translucents, Map<Integer, Integer> opaques, Map<Integer, Integer> ranks)
    {
        this.translucents = translucents;
        this.opaques = opaques;
        this.ranks = ranks;
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

        var sortedTranslucents = translucents.keySet().stream().sorted().toList();
        var sortedOpaques = opaques.keySet().stream().sorted().toList();

        for (var k : sortedTranslucents)
        {
            int location = translucents.get(k);
            int rank = ranks.get(k);

            String outName = "iris_FragData" + location;
            String transmittance = "transmittance" + k;
            String coeffs = "clrwl_coefficients" + k;
            String depth = "depth" + k;
            String correctedTransmittance = "correctedTransmittance" + k;

            body.add(GlslStmt.raw("float " + transmittance + " = 1. - " + outName + ".a;"));
            body.add(GlslStmt.raw("float " + depth + " = flw_depth;"));
            // Don't do the depth adjustment if this fragment is opaque.
            body.add(GlslStmt.raw("if (" + transmittance + " > 1e-5) { " + depth + " -= depth_adjustment; }"));
            body.add(GlslStmt.raw("float " + correctedTransmittance + " = _clrwl_signal_corrected_transmittance(" + coeffs + ", " + depth + ", " + transmittance + ", " + rank + ");"));
            body.add(GlslStmt.raw(outName + ".rgb *= " + outName + ".a;"));
            body.add(GlslStmt.raw(outName + " *= " + correctedTransmittance + ";"));
        }

        if (!opaques.isEmpty())
        {
            String transmittance_from_depth = "depth_transmittance";

            body.add(GlslStmt.raw("float " + transmittance_from_depth + " = _clrwl_opaque_transmittance_from_depth(linearDepth, depthRange);"));

            for (var k : sortedOpaques)
            {
                int location = opaques.get(k);

                String outName = "iris_FragData" + location;

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
