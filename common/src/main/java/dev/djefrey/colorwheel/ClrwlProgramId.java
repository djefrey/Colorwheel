package dev.djefrey.colorwheel;

import dev.engine_room.flywheel.api.material.Transparency;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public enum ClrwlProgramId
{
    GBUFFERS(ClrwlProgramGroup.GBUFFERS, "clrwl_gbuffers", null, false),
    GBUFFERS_ADDITIVE(ClrwlProgramGroup.GBUFFERS,"clrwl_gbuffers_additive", GBUFFERS, false),
    GBUFFERS_GLINT(ClrwlProgramGroup.GBUFFERS,"clrwl_gbuffers_glint", GBUFFERS, false),
    GBUFFERS_LIGHTNING(ClrwlProgramGroup.GBUFFERS,"clrwl_gbuffers_lightning", GBUFFERS, false),
    GBUFFERS_DAMAGEDBLOCK(ClrwlProgramGroup.GBUFFERS,"clrwl_gbuffers_damagedblock", GBUFFERS, false),
    GBUFFERS_TRANSLUCENT(ClrwlProgramGroup.GBUFFERS,"clrwl_gbuffers_translucent", GBUFFERS, true),

    SHADOW(ClrwlProgramGroup.SHADOW, "clrwl_shadow", null, false),
    SHADOW_TRANSLUCENT(ClrwlProgramGroup.SHADOW,"clrwl_shadow_translucent", SHADOW, true);

    private final ClrwlProgramGroup group;
    private final String name;
    @Nullable
    private final ClrwlProgramId base;
    private final boolean afterTranslucent;

    ClrwlProgramId(ClrwlProgramGroup group, String name, ClrwlProgramId base, boolean afterTranslucent)
    {
        this.group = group;
        this.name = name;
        this.base = base;
        this.afterTranslucent = afterTranslucent;
    }

    public ClrwlProgramGroup group()
    {
        return group;
    }

    public String programName()
    {
        return name;
    }

    @Nullable
    public ClrwlProgramId base()
    {
        return base;
    }

    public boolean afterTranslucent()
    {
        return afterTranslucent;
    }

    public static ClrwlProgramId[] gbuffers()
    {
        return new ClrwlProgramId[] { GBUFFERS, GBUFFERS_ADDITIVE, GBUFFERS_GLINT, GBUFFERS_LIGHTNING, GBUFFERS_TRANSLUCENT, GBUFFERS_DAMAGEDBLOCK };
    }

    public static ClrwlProgramId[] shadow()
    {
        return new ClrwlProgramId[] { SHADOW, SHADOW_TRANSLUCENT };
    }

    public static Optional<ClrwlProgramId> fromName(String name)
    {
        for (var program : values())
        {
            if (program.name.equals(name))
            {
                return Optional.of(program);
            }
        }

        return Optional.empty();
    }

    public static ClrwlProgramId fromTransparency(Transparency transparency, boolean isShadow)
    {
        if (!isShadow)
        {
            switch (transparency)
            {
                case OPAQUE ->
                {
                    return ClrwlProgramId.GBUFFERS;
                }
                case ADDITIVE ->
                {
                    return ClrwlProgramId.GBUFFERS_ADDITIVE;
                }
                case LIGHTNING ->
                {
                    return ClrwlProgramId.GBUFFERS_LIGHTNING;
                }
                case GLINT ->
                {
                    return ClrwlProgramId.GBUFFERS_GLINT;
                }
                case CRUMBLING ->
                {
                    return ClrwlProgramId.GBUFFERS_DAMAGEDBLOCK;
                }
                case TRANSLUCENT, ORDER_INDEPENDENT ->
                {
                    return ClrwlProgramId.GBUFFERS_TRANSLUCENT;
                }
            }

            throw new RuntimeException("Unknown transparency: " + transparency);
        }
        else
        {
            if (transparency != Transparency.TRANSLUCENT && transparency != Transparency.ORDER_INDEPENDENT)
            {
                return ClrwlProgramId.SHADOW;
            }
            else
            {
                return ClrwlProgramId.SHADOW_TRANSLUCENT;
            }
        }
    }
}
