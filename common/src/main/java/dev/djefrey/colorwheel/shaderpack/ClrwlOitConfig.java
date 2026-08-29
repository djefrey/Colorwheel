package dev.djefrey.colorwheel.shaderpack;

import dev.djefrey.colorwheel.engine.ClrwlOitAccumulateOverride;

import java.util.List;

public record ClrwlOitConfig(boolean enabled, int[] coeffRanks, List<ClrwlOitAccumulateOverride> accumulateOverrides)
{
}
