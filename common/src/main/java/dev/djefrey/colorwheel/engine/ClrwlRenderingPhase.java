package dev.djefrey.colorwheel.engine;

public enum ClrwlRenderingPhase
{
    SOLID,
    TRANSLUCENT,
    OIT_DEPTH_RANGE,
    OIT_COEFFICIENTS,
    OIT_ACCUMULATE,
    OIT_COMPOSITE,
    CRUMBLING;

    public int getValue()
    {
        // ordinal is shifted to prevent collision with Iris or other mod
        return 110800 + ordinal();
    }
}
