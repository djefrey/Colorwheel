struct FlwBoundingSphere
{
    float x;
    float y;
    float z;
    float radius;
};

struct FlwModelDescriptor
{
    uint instanceCount;
    uint baseInstance;
    uint matrixIndex;
    FlwBoundingSphere boundingSphere;
    uint irisIds; // entityId + blockEntityId
    uint clrwlData; // only needs 4 bits (0 -> 15 range)
};

void _flw_unpackBoundingSphere(in FlwBoundingSphere sphere, out vec3 center, out float radius)
{
    center = vec3(sphere.x, sphere.y, sphere.z);
    radius = sphere.radius;
}

FlwBoundingSphere _flw_packBoundingSphere(vec3 center, float radius)
{
    return FlwBoundingSphere(center.x, center.y, center.z, radius);
}

void _clrwl_unpackData(in FlwModelDescriptor desc, out int entityId, out int blockEntityId, out int lightEmission)
{
    entityId      = int((desc.irisIds & 0xFFFF0000u) >> 16);
    blockEntityId = int((desc.irisIds & 0x0000FFFFu) >> 0);
    lightEmission = int((desc.clrwlData & 0x00000008u) >> 0);
}

uint _clrwl_unpackMaterialBitset(in FlwModelDescriptor desc)
{
    return (desc.clrwlData & 0x00000030u) >> 4;
}
