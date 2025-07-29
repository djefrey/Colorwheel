package dev.djefrey.colorwheel.compile;

import java.util.Optional;

public record ClrwlProgramSource(String name,
                                 String vertex, Optional<String> geometry, String fragment)
{
}
