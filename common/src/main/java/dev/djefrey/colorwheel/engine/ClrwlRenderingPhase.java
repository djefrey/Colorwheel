package dev.djefrey.colorwheel.engine;

import org.apache.commons.lang3.StringUtils;

import java.util.Locale;

public enum ClrwlRenderingPhase
{
    SOLID,
    TRANSLUCENT,
    OIT_DEPTH_RANGE,
    OIT_COEFFICIENTS,
    OIT_ACCUMULATE,
    OIT_COMPOSITE,
    CRUMBLING,
    STAGING_BUFFER_FLUSH,
    INDIRECT_DEPTH_PYRAMID,
    INDIRECT_CULL,
    INDIRECT_CULL_APPLY,
    INDIRECT_CULL_RESET,
    INDIRECT_CULL_TRANSFORM;

    private final String debugName;
    private final String shadowDebugName;

    ClrwlRenderingPhase()
    {
        var name = StringUtils.capitalize(name().toLowerCase(Locale.ROOT).replace("_", " "));

        debugName = "Clrwl " + name;
        shadowDebugName = "Clrwl Shadow " + name;
    }

    public int getValue()
    {
        // ordinal is shifted to prevent collision with Iris or other mod
        return 110800 + ordinal();
    }

    public String getDebugName(boolean shadow)
    {
        return shadow ? shadowDebugName : debugName;
    }
}
