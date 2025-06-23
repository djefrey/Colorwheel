package dev.djefrey.colorwheel.engine.uniform;

import dev.djefrey.colorwheel.ShadowRenderContext;
import dev.djefrey.colorwheel.compile.ClrwlProgram;
import dev.engine_room.flywheel.api.backend.RenderContext;

public class ClrwlUniforms
{
    public static final int FRAME_INDEX = 0;
    public static final int OPTIONS_INDEX = 1;

    public static final String FRAME_BLOCK_NAME = "_ClrwlFrameUniforms";
    public static final String OPTIONS_BLOCK_NAME = "_ClrwlOptionsUniforms";

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
        ClrwlOptionsUniforms.BUFFER.bind();
    }

    public static void bindShadow()
    {
        ClrwlShadowFrameUniforms.BUFFER.bind();
        ClrwlOptionsUniforms.BUFFER.bind();
    }

    public static void setUniformBlockBinding(ClrwlProgram program)
    {
        program.setUniformBlockBinding(FRAME_BLOCK_NAME, FRAME_INDEX);
        program.setUniformBlockBinding(OPTIONS_BLOCK_NAME, OPTIONS_INDEX);
    }
}
