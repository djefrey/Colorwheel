package dev.djefrey.colorwheel.compile.oit;

import dev.djefrey.colorwheel.engine.ClrwlOitCoeffDirective;

import java.util.List;
import java.util.Map;

public record ClrwlOitCompositeShaderKey(Map<Integer, Integer> translucentCoeffs,
                                         List<Integer> opaques,
                                         Map<Integer, Integer> ranks)
{
}
