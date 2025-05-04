package dev.djefrey.colorwheel.compile.oit;

import dev.djefrey.colorwheel.Colorwheel;
import dev.djefrey.colorwheel.accessors.ProgramDirectivesAccessor;
import dev.djefrey.colorwheel.accessors.ProgramSetAccessor;
import dev.engine_room.flywheel.backend.glsl.SourceComponent;
import dev.engine_room.flywheel.backend.glsl.generate.GlslBuilder;
import dev.engine_room.flywheel.backend.glsl.generate.GlslUniform;
import net.irisshaders.iris.shaderpack.ShaderPack;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;

import java.util.Collection;
import java.util.List;

public class OitCoefficientsSamplersComponent implements SourceComponent
{
    private final ShaderPack pack;
    private final NamespacedId dimension;
    private final boolean isShadow;

    public OitCoefficientsSamplersComponent(ShaderPack pack, NamespacedId dimension, boolean isShadow)
    {
        this.pack = pack;
        this.dimension = dimension;
        this.isShadow = isShadow;
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
        ProgramSource programSet;

        if (!isShadow)
        {
            programSet = ((ProgramSetAccessor) pack.getProgramSet(dimension)).colorwheel$getFlwGbuffers().orElseThrow();
        }
        else
        {
            programSet = ((ProgramSetAccessor) pack.getProgramSet(dimension)).colorwheel$getFlwShadow().orElseThrow();
        }

        var coeffs = ((ProgramDirectivesAccessor) programSet.getDirectives()).colorwheel$getOitCoefficients();

        addSamplers(builder, coeffs.size());

        return builder.build();
    }

    @Override
    public String name()
    {
        return Colorwheel.rl("oit_coefficients_samplers").toString();
    }

    public static void addSamplers(GlslBuilder builder, int coeffCount)
    {
        for (int i = 0; i < coeffCount; i++)
        {
            var uniform = new GlslUniform()
                    .type("sampler2DArray")
                    .name("clrwl_coefficients" + i);

            builder.add(uniform);
        }
    }
}
