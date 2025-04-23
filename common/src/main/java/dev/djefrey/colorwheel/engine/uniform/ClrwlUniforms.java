package dev.djefrey.colorwheel.engine.uniform;

import dev.djefrey.colorwheel.ShadowRenderContext;
import dev.djefrey.colorwheel.compile.ClrwlProgram;
import dev.engine_room.flywheel.api.backend.RenderContext;

public class ClrwlUniforms
{
    public static final int FRAME_INDEX = 0;
    public static final String FRAME_BLOCK_NAME = "_ClrwlFrameUniforms";

    public static void update(RenderContext context)
    {
        if (context instanceof ShadowRenderContext shadowContext)
        {
            ClrwlShadowFrameUniforms.update(shadowContext);
        }
        else
        {
            ClrwlFrameUniforms.update(context);
        }
    }

    public static void bind(boolean isShadow)
    {
        if (isShadow)
        {
            bindShadow();
        }
        else
        {
            bindColor();
        }
    }

    public static void bindColor()
    {
        ClrwlFrameUniforms.BUFFER.bind();
    }

    public static void bindShadow()
    {
        ClrwlShadowFrameUniforms.BUFFER.bind();
    }

    public static void setUniformBlockBinding(ClrwlProgram program)
    {
        program.setUniformBlockBinding(FRAME_BLOCK_NAME, FRAME_INDEX);
    }
}
