package dev.djefrey.colorwheel.compile;

import dev.djefrey.colorwheel.Colorwheel;
import dev.engine_room.flywheel.backend.glsl.ShaderSources;
import dev.engine_room.flywheel.backend.glsl.SourceComponent;
import dev.engine_room.flywheel.backend.glsl.SourceFile;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;

public class ExtendedInstanceShaderComponent implements SourceComponent
{
    private final ResourceLocation resource;
    private final SourceFile source;

    public ExtendedInstanceShaderComponent(ShaderSources sources, ResourceLocation resource)
    {
        this.resource = resource;
        this.source = sources.get(resource);
    }

    @Override
    public Collection<? extends SourceComponent> included()
    {
        return source.included();
    }

    @Override
    public String source()
    {
        var code = source.source();
        var source = new StringBuilder();

        code.lines().forEach(line ->
        {
            source.append(line).append('\n');

            // Hope it does not break (but it will eventually)
            if (line.contains("flw_vertexNormal ="))
            {
                String tangentAssign = line.replaceAll("flw_vertexNormal", "flw_vertexTangent.xyz");

                source.append(tangentAssign).append('\n');
            }
        });

        return source.toString();
    }

    @Override
    public String name()
    {
        return Colorwheel.rl("extended_instance_shader") + " - " + resource.toString();
    }
}
