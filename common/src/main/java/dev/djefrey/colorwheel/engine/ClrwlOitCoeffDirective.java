package dev.djefrey.colorwheel.engine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record ClrwlOitCoeffDirective(int rank,
                                     int[] buffers)
{
    public static Map<Integer, Integer> getReverseMap(List<ClrwlOitCoeffDirective> directives)
    {
        var map = new HashMap<Integer, Integer>();

        for (int i = 0; i < directives.size(); i++)
        {
            var directive = directives.get(i);

            for (int buf : directive.buffers())
            {
                map.put(buf, i);
            }
        }

        return map;
    }
}
