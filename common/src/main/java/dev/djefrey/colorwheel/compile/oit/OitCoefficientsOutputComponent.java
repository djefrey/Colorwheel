package dev.djefrey.colorwheel.compile.oit;

import dev.djefrey.colorwheel.Colorwheel;
import dev.djefrey.colorwheel.accessors.ProgramDirectivesAccessor;
import dev.djefrey.colorwheel.accessors.ProgramSetAccessor;
import dev.djefrey.colorwheel.compile.GlslFragmentOutput;
import dev.engine_room.flywheel.backend.glsl.SourceComponent;
import dev.engine_room.flywheel.backend.glsl.generate.GlslBuilder;
import net.irisshaders.iris.shaderpack.ShaderPack;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;

import java.util.Collection;
import java.util.List;

public class OitCoefficientsOutputComponent implements SourceComponent
{
    private final ShaderPack pack;
    private final NamespacedId dimension;
    private final boolean isShadow;

    public OitCoefficientsOutputComponent(ShaderPack pack, NamespacedId dimension, boolean isShadow)
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
        int idx = 0;

        for (int i = 0; i < coeffs.size(); i++)
        {
            int slices = divRoundUp(1 << (coeffs.get(i).rank() + 1), 4);

            for (int l = 0; l < slices; l++)
            {
                var out = new GlslFragmentOutput()
                        .binding(idx)
                        .type("vec4")
                        .name(("clrwl_coeffs" + i) + l);

                builder.add(out);
                idx += 1;
            }
        }

        return builder.build();
    }

    @Override
    public String name()
    {
        return Colorwheel.rl("oit_coefficients_output").toString();
    }

    private static int divRoundUp(int num, int den)
    {
        return (num + den - 1) / den;
    }
}
