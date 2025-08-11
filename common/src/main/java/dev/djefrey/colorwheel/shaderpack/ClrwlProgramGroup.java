package dev.djefrey.colorwheel.shaderpack;

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
}
