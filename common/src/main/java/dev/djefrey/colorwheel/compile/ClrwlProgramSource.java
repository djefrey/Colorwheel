package dev.djefrey.colorwheel.compile;

import java.util.Optional;

public record ClrwlProgramSource(String name,
                                 String vertex, String fragment)
{
    public Optional<String> getVertexSource()
    {
        return Optional.of(vertex);
    }

    public Optional<String> getFragmentSource()
    {
        return Optional.of(fragment);
    }
}
