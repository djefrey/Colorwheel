package dev.djefrey.colorwheel.shaderpack;

import dev.djefrey.colorwheel.Colorwheel;
import dev.engine_room.flywheel.api.material.Transparency;

import java.util.Optional;

public enum ClrwlProgramGroup
{
    GBUFFERS("gbuffers"),
    SHADOW("shadow");

    private final String name;

    ClrwlProgramGroup(String name)
    {
        this.name = name;
    }

    public String groupName()
    {
        return name;
    }

    public static ClrwlProgramGroup fromShadow(boolean isShadow)
    {
        return isShadow ? SHADOW : GBUFFERS;
    }
}
