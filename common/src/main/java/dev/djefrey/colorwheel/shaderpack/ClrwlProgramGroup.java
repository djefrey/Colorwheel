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

    public Optional<ClrwlProgramId> getProgramFromTransparency(Transparency transparency)
    {
        switch (this)
        {
            case GBUFFERS ->
            {
                return gbuffersFromTransparency(transparency);
            }
            case SHADOW ->
            {
                return shadowFromTransparency(transparency);
            }
        }

        Colorwheel.LOGGER.error("Unknown program group: {}", this);
        return Optional.empty();
    }

    private static Optional<ClrwlProgramId> gbuffersFromTransparency(Transparency transparency)
    {
        switch (transparency)
        {
            case OPAQUE ->
            {
                return Optional.of(ClrwlProgramId.GBUFFERS);
            }
            case ADDITIVE ->
            {
                return Optional.of(ClrwlProgramId.GBUFFERS_ADDITIVE);
            }
            case LIGHTNING ->
            {
                return Optional.of(ClrwlProgramId.GBUFFERS_LIGHTNING);
            }
            case GLINT ->
            {
                return Optional.of(ClrwlProgramId.GBUFFERS_GLINT);
            }
            case CRUMBLING ->
            {
                return Optional.of(ClrwlProgramId.GBUFFERS_DAMAGEDBLOCK);
            }
            case TRANSLUCENT, ORDER_INDEPENDENT ->
            {
                return Optional.of(ClrwlProgramId.GBUFFERS_TRANSLUCENT);
            }
        }

        Colorwheel.LOGGER.error("Unknown transparency: {}", transparency);
        return Optional.empty();
    }

    private static Optional<ClrwlProgramId> shadowFromTransparency(Transparency transparency)
    {
        switch (transparency)
        {
            case OPAQUE ->
            {
                return Optional.of(ClrwlProgramId.SHADOW);
            }
            case ADDITIVE ->
            {
                return Optional.of(ClrwlProgramId.SHADOW_ADDITIVE);
            }
            case LIGHTNING ->
            {
                return Optional.of(ClrwlProgramId.SHADOW_LIGHTNING);
            }
            case GLINT ->
            {
                return Optional.of(ClrwlProgramId.SHADOW_GLINT);
            }
            case CRUMBLING ->
            {
                return Optional.empty();
            }
            case TRANSLUCENT, ORDER_INDEPENDENT ->
            {
                return Optional.of(ClrwlProgramId.SHADOW_TRANSLUCENT);
            }
        }

        Colorwheel.LOGGER.error("Unknown transparency: {}", transparency);
        return Optional.empty();
    }
}
