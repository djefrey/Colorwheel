package dev.djefrey.colorwheel.compile;

import dev.djefrey.colorwheel.Colorwheel;
import dev.engine_room.flywheel.backend.glsl.SourceComponent;
import dev.engine_room.flywheel.backend.glsl.generate.GlslBuilder;

import java.util.Collection;
import java.util.List;

public class ClrwlFragDataOutComponent implements SourceComponent
{
    private final int drawBufferCnt;

    public ClrwlFragDataOutComponent(int drawBufferCnt)
    {
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

        addFragDataOuts(builder, drawBufferCnt);

        return builder.build();
    }

    @Override
    public String name()
    {
        return Colorwheel.rl("fragdata_out").toString();
    }

    public static void addFragDataOuts(GlslBuilder builder, int drawBufferCnt)
    {
        for (int i = 0; i < drawBufferCnt; i++)
        {
            var global = new GlslAssignment()
                    .type("vec4")
                    .name("clrwl_FragData" + i);

            builder.add(global);
        }
    }
}
