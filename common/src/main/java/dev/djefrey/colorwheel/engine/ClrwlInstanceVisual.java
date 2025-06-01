package dev.djefrey.colorwheel.engine;

public record ClrwlInstanceVisual(Type type, int irisId)
{
    private static final int UNDEFINED_ID = -1;
    private static final ClrwlInstanceVisual UNDEFINED = new ClrwlInstanceVisual(Type.UNDEFINIED, UNDEFINED_ID);

    public static ClrwlInstanceVisual undefined()
    {
        return UNDEFINED;
    }

    public static ClrwlInstanceVisual blockEntity(int irisId)
    {
        return new ClrwlInstanceVisual(Type.BLOCKENTITY, irisId);
    }

    public static ClrwlInstanceVisual entity(int irisId)
    {
        return new ClrwlInstanceVisual(Type.ENTITY, irisId);
    }

    public int getBlockEntity()
    {
        if (type == Type.BLOCKENTITY)
        {
            return irisId;
        }
        return UNDEFINED_ID;
    }

    public int getEntity()
    {
        if (type == Type.ENTITY)
        {
            return irisId;
        }

        return UNDEFINED_ID;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o instanceof ClrwlInstanceVisual rhs)
        {
            switch (type)
            {
                case UNDEFINIED ->
                {
                    return rhs.type == Type.UNDEFINIED;
                }
                case BLOCKENTITY ->
                {
                    return rhs.type == Type.BLOCKENTITY && rhs.irisId == this.irisId;
                }
                case ENTITY ->
                {
                    return rhs.type == Type.ENTITY && rhs.irisId == this.irisId;
                }
            }
        }

        return false;
    }

    @Override
    public int hashCode()
    {
        int prime = 31;
        int result = 1;

        result = prime * result + this.type.hashCode();

        if (type != Type.UNDEFINIED)
        {
            result = prime * result + this.irisId;
        }

        return result;
    }

    public enum Type
    {
        UNDEFINIED,
        BLOCKENTITY,
        ENTITY
    }
}
