package dev.djefrey.colorwheel.compile.oit;

import dev.djefrey.colorwheel.engine.ClrwlOitAccumulateOverride;

import java.util.List;

public record ClrwlOitCompositeShaderKey(int[] drawBuffers, int[] ranks, List<ClrwlOitAccumulateOverride> overrides)
{
}
