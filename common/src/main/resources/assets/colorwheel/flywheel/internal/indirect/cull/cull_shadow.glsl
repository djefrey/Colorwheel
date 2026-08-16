#include "colorwheel:internal/indirect/buffer_bindings.glsl"
#include "colorwheel:internal/indirect/model_descriptor.glsl"
#include "colorwheel:internal/indirect/matrices.glsl"
#include "colorwheel:internal/uniforms.glsl"
#include "flywheel:util/matrix.glsl"

layout(local_size_x = 32) in;

layout(std430, binding = _FLW_DRAW_INSTANCE_INDEX_BUFFER_BINDING) restrict writeonly buffer TargetBuffer {
    uint _flw_instanceIndices[];
};

// High 6 bits for the number of instances in the page.
const uint _FLW_PAGE_COUNT_OFFSET = 26u;
// Bottom 26 bits for the model index.
const uint _FLW_MODEL_INDEX_MASK = 0x3FFFFFF;

layout(std430, binding = _FLW_PAGE_FRAME_DESCRIPTOR_BUFFER_BINDING) restrict readonly buffer PageFrameDescriptorBuffer {
    uint _flw_pageFrameDescriptors[];
};

layout(std430, binding = _FLW_BOUNDING_SPHERE_BUFFER_BINDING) restrict readonly buffer BoundingSphereBuffer {
    FlwBoundingSphere _flw_boundingSpheres[];
};

layout(std430, binding = _FLW_MODEL_BUFFER_BINDING) restrict buffer ModelBuffer {
    FlwModelDescriptor _flw_models[];
};

layout(std430, binding = _FLW_MATRIX_BUFFER_BINDING) restrict readonly buffer MatrixBuffer {
    FlwMatrices _flw_matrices[];
};

bool _clrwl_isSphereInCube(vec3 center, float radius, vec3 origin, float halfLength)
{
    vec3 v = abs(center - origin);
    float maxD = max(v.x, max(v.y, v.z));
    return maxD <= halfLength - radius;
}

bool _clrwl_testSphereOn4Planes(vec3 center, float radius, _ClrwlShadowFrustumPlanesGroup planes)
{
    return all(greaterThanEqual(fma(planes.X, center.xxxx, fma(planes.Y, center.yyyy, fma(planes.Z, center.zzzz, planes.W))), -radius.xxxx));
}

bool _clrwl_testSphereOn3Planes(vec3 center, float radius, _ClrwlShadowFrustumPlanesGroup planes)
{
    return all(greaterThanEqual(fma(planes.X.xyz, center.xxx, fma(planes.Y.xyz, center.yyy, fma(planes.Z.xyz, center.zzz, planes.W.xyz))), -radius.xxx));
}

bool _clrwl_testSphere(vec3 center, float radius)
{
    float reversedCullDist = clrwl_shadowFrustumPlanes.groups[2].X.w;
    float cullDist = clrwl_shadowFrustumPlanes.groups[2].Y.w;

    if (_clrwl_isSphereInCube(center, radius, flw_cameraPos, reversedCullDist))
    {
        return true;
    }

    if (!_clrwl_isSphereInCube(center, radius, flw_cameraPos, cullDist))
    {
        return false;
    }

    return _clrwl_testSphereOn4Planes(center, radius, clrwl_shadowFrustumPlanes.groups[0])
        && _clrwl_testSphereOn4Planes(center, radius, clrwl_shadowFrustumPlanes.groups[1])
        && _clrwl_testSphereOn3Planes(center, radius, clrwl_shadowFrustumPlanes.groups[2]);
}

bool _clrwl_isVisible(uint instanceIndex, uint modelIndex)
{
    vec3 center;
    float radius;
    _flw_unpackBoundingSphere(_flw_boundingSpheres[instanceIndex], center, radius);

    bool isVisible = _clrwl_testSphere(center, radius);

    return isVisible;
}

void main()
{
    uint pageIndex = gl_WorkGroupID.x << 1u;

    if (pageIndex >= _flw_pageFrameDescriptors.length())
    {
        return;
    }

    uint modelIndex = _flw_pageFrameDescriptors[pageIndex];
    uint pageValidity = _flw_pageFrameDescriptors[pageIndex + 1];
    uint localInvocationMask = 1u << gl_LocalInvocationID.x;

    if ((localInvocationMask & pageValidity) == 0)
    {
        return;
    }

    uint instanceIndex = gl_GlobalInvocationID.x;

    if (_clrwl_isVisible(instanceIndex, modelIndex))
    {
        uint localIndex = atomicAdd(_flw_models[modelIndex].instanceCount, 1);
        uint targetIndex = _flw_models[modelIndex].baseInstance + localIndex;
        _flw_instanceIndices[targetIndex] = instanceIndex;
    }
}
