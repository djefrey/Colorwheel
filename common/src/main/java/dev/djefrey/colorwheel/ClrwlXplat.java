package dev.djefrey.colorwheel;

import org.jetbrains.annotations.Nullable;

import java.util.ServiceLoader;

public interface ClrwlXplat
{
    ClrwlXplat INSTANCE = ServiceLoader.load(ClrwlXplat.class).findFirst().get();

    String getFormattedVersion();
    boolean doesHaveFlywheel();
    @Nullable
    Version getFlywheelVersion();
    boolean doesHaveCreate();
    boolean doesHavePonder();
    @Nullable
    String getCustomModCompatClasspath();
}
