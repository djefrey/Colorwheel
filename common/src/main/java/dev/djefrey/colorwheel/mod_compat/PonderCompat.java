package dev.djefrey.colorwheel.mod_compat;

public class PonderCompat
{
    private static final ClrwlSuperRenderTypeBuffer BUFFER = new ClrwlSuperRenderTypeBuffer();

    public static ClrwlSuperRenderTypeBuffer getBufferInstance()
    {
        return BUFFER;
    }
}
