package dev.djefrey.colorwheel;

import java.util.ServiceLoader;

public interface ClrwlXplat
{
    ClrwlXplat INSTANCE = ServiceLoader.load(ClrwlXplat.class).findFirst().get();

    String getFormattedVersion();
    boolean doesHaveFlywheel();
}
