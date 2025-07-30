package dev.djefrey.colorwheel.compile.oit;

import dev.djefrey.colorwheel.Colorwheel;
import dev.engine_room.flywheel.backend.glsl.SourceComponent;
import dev.engine_room.flywheel.backend.glsl.generate.GlslBuilder;
import dev.engine_room.flywheel.backend.glsl.generate.GlslUniform;

import java.util.Collection;
import java.util.List;

public class OitCoefficientsSamplersComponent implements SourceComponent
{
    private final List<Integer> coeffs;

    public OitCoefficientsSamplersComponent(List<Integer> coeffs)
    {
        this.coeffs = coeffs;
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

        addSamplers(builder, coeffs);

        return builder.build();
    }

    @Override
    public String name()
    {
        return Colorwheel.rl("oit_coefficients_samplers").toString();
    }

    public static void addSamplers(GlslBuilder builder, List<Integer> coeffs)
    {
        for (int i : coeffs)
        {
            var uniform = new GlslUniform()
                    .type("sampler2DArray")
                    .name("clrwl_coefficients" + i);

            builder.add(uniform);
        }
    }
}
