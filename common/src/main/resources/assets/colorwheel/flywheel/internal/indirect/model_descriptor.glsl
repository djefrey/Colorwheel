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
    uint lightEmission; // only needs 4 bits (0 -> 15 range)
};

void _flw_unpackBoundingSphere(in FlwBoundingSphere sphere, out vec3 center, out float radius)
{
    center = vec3(sphere.x, sphere.y, sphere.z);
    radius = sphere.radius;
}

void _clrwl_unpackData(in FlwModelDescriptor desc, out uint entityId, out uint blockEntityId, out uint lightEmission)
{
    entityId      = (desc.irisIds & 0xFFFF0000u) >> 16;
    blockEntityId = (desc.irisIds & 0x0000FFFFu) >> 0;
    lightEmission = (desc.lightEmission & 0x000000FFu) >> 0;
}
