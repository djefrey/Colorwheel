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

layout(std430, binding = _FLW_MODEL_BUFFER_BINDING) restrict buffer ModelBuffer {
    FlwModelDescriptor _flw_models[];
};

layout(std430, binding = _FLW_MATRIX_BUFFER_BINDING) restrict readonly buffer MatrixBuffer {
    FlwMatrices _flw_matrices[];
};

bool _clrwl_testSphereOnPlanes(vec3 center, float radius, _ClrwlShadowFrustumPlanesGroup planes)
{
    return greaterThanEqual(fma(planes.X, center.xxxx, fma(planes.Y, center.yyyy, fma(planes.Z, center.zzzz, planes.W))), -radius.xxxx).x;
}

bool _clrwl_testSphere(vec3 center, float radius)
{
    return _clrwl_testSphereOnPlanes(center, radius, clrwl_shadowFrustumPlanes.groups[0])
        && _clrwl_testSphereOnPlanes(center, radius, clrwl_shadowFrustumPlanes.groups[0])
        && _clrwl_testSphereOnPlanes(center, radius, clrwl_shadowFrustumPlanes.groups[2]);
}

bool _clrwl_isVisible(uint instanceIndex, uint modelIndex)
{
    uint matrixIndex = _flw_models[modelIndex].matrixIndex;
    FlwBoundingSphere sphere = _flw_models[modelIndex].boundingSphere;

    vec3 center;
    float radius;
    _flw_unpackBoundingSphere(sphere, center, radius);

    FlwInstance instance = _flw_unpackInstance(instanceIndex);

    flw_transformBoundingSphere(instance, center, radius);

    if (matrixIndex > 0)
    {
        transformBoundingSphere(_flw_matrices[matrixIndex].pose, center, radius);
    }

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

    if (((1u << gl_LocalInvocationID.x) & pageValidity) == 0)
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
