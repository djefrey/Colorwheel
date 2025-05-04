package dev.djefrey.colorwheel.compile.oit;

import dev.djefrey.colorwheel.engine.ClrwlOitCoeffDirective;

import java.util.List;

public record ClrwlOitCompositeShaderKey(int[] renderTargets,
                                         List<ClrwlOitCoeffDirective> coefficients)
{
}
