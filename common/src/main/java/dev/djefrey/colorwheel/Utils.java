package dev.djefrey.colorwheel;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Utils
{
    public static int divRoundUp(int num, int den)
    {
        return (num + den - 1) / den;
    }

    public static <V, K> Map<V, K> reverse(Map<K, V> map)
    {
        return map.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    }
}
