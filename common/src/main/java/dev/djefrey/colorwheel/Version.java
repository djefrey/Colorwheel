package dev.djefrey.colorwheel;

public record Version(int major, int minor, int patch) implements Comparable<Version>
{
    public static Version fromArray(int[] version)
    {
        return new Version(version[0], version[1], version[2]);
    }

    public String toString()
    {
        return major + "." + minor + "." + patch;
    }

    public int compareTo(Version rhs)
    {
        return (this.major - rhs.major) * 10000 + (this.minor - rhs.minor) * 100 + (this.patch - rhs.patch);
    }
}
