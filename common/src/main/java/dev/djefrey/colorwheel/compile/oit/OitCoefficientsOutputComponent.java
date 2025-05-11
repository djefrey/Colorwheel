package dev.djefrey.colorwheel.compile.oit;

import dev.djefrey.colorwheel.Colorwheel;
import dev.djefrey.colorwheel.accessors.PackDirectivesAccessor;
import dev.djefrey.colorwheel.accessors.ProgramSetAccessor;
import dev.djefrey.colorwheel.compile.ClrwlFragDataOutComponent;
import dev.djefrey.colorwheel.compile.GlslAssignment;
import dev.djefrey.colorwheel.compile.GlslFragmentOutput;
import dev.engine_room.flywheel.backend.glsl.SourceComponent;
import dev.engine_room.flywheel.backend.glsl.generate.GlslBuilder;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class OitCoefficientsOutputComponent implements SourceComponent
{
    private final Map<Integer, Integer> ranks;
    private final int drawBufferCnt;

    public OitCoefficientsOutputComponent(Map<Integer, Integer> ranks, int drawBufferCnt)
    {
        this.ranks = ranks;
        this.drawBufferCnt = drawBufferCnt;
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

        var sorted = ranks.keySet().stream().sorted().toList();
        int idx = 0;

        for (int k : sorted)
        {
            int rank = ranks.get(k);
            int depth = 1 << (rank - 1);

            for (int d = 0; d < depth; d++)
            {
                var out = new GlslFragmentOutput()
                        .binding(idx)
                        .type("vec4")
                        .name(("clrwl_coeffs" + k) + d);

                builder.add(out);
                idx += 1;
            }
        }

        ClrwlFragDataOutComponent.addFragDataOuts(builder, drawBufferCnt);

        return builder.build();
    }

    @Override
    public String name()
    {
        return Colorwheel.rl("oit_coefficients_output").toString();
    }
}
