package dev.djefrey.colorwheel.util;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
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

    public static <T> Optional<T> findFirst(List<T> list, Predicate<T> predicate)
    {
        for (T e : list)
        {
            if (predicate.test(e))
            {
                return Optional.of(e);
            }
        }

        return Optional.empty();
    }
}
