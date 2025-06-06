package dev.djefrey.colorwheel;

public enum ClrwlShaderPrograms
{
    GBUFFERS("clrwl_gbuffers"),
    GBUFFERS_TRANSLUCENT("clrwl_gbuffers_translucent"),
    SHADOW("clrwl_shadows"),
    DAMAGEDBLOCK("clrwl_damagedblock");

    private String name;

    ClrwlShaderPrograms(String name)
    {
        this.name = name;
    }
}
