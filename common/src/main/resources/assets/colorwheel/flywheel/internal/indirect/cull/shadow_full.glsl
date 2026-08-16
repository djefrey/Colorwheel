#include "colorwheel:internal/indirect/buffer_bindings.glsl"
#include "colorwheel:internal/indirect/model_descriptor.glsl"
#include "colorwheel:internal/indirect/matrices.glsl"
#include "colorwheel:internal/uniforms.glsl"
#include "flywheel:util/matrix.glsl"
#include "colorwheel:internal/indirect/cull/common.glsl"

layout(local_size_x = 32) in;

layout(std430, binding = _FLW_DRAW_INSTANCE_INDEX_BUFFER_BINDING) restrict writeonly buffer TargetBuffer {
    uint _flw_instanceIndices[];
};

layout(std430, binding = _FLW_PAGE_FRAME_DESCRIPTOR_BUFFER_BINDING) restrict readonly buffer PageFrameDescriptorBuffer {
    uint _flw_pageFrameDescriptors[];
};

layout(std430, binding = _FLW_BOUNDING_SPHERE_BUFFER_BINDING) restrict readonly buffer BoundingSphereBuffer {
    FlwBoundingSphere _flw_boundingSpheres[];
};

layout(std430, binding = _FLW_MODEL_BUFFER_BINDING) restrict buffer ModelBuffer {
    FlwModelDescriptor _flw_models[];
};

layout(binding = 0) uniform sampler2D _flw_depthPyramid;

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

    bool inFrustum = _clrwl_testSphereOn4Planes(center, radius, clrwl_shadowFrustumPlanes.groups[0])
                  && _clrwl_testSphereOn4Planes(center, radius, clrwl_shadowFrustumPlanes.groups[1])
                  && _clrwl_testSphereOn3Planes(center, radius, clrwl_shadowFrustumPlanes.groups[2]);

    if (!inFrustum)
    {
        return false;
    }

    transformBoundingSphere(flw_view, center, radius);

    vec4 aabb;
    if (!projectSphere(center, radius, _flw_cullData.znear, _flw_cullData.P00, _flw_cullData.P11, _flw_cullData.orthographic == 1, aabb))
    {
        return true;
    }

    vec2 p01 = clipToUV(clrwl_distortShadowClipXY(aabb.xw));
    vec2 p11 = clipToUV(clrwl_distortShadowClipXY(aabb.zw));
    vec2 p10 = clipToUV(clrwl_distortShadowClipXY(aabb.zy));
    vec2 p00 = clipToUV(clrwl_distortShadowClipXY(aabb.xy));

    float width  = max(abs(p10.x - p00.x), abs(p11.x - p01.x)) * _flw_cullData.pyramidWidth;
    float height = max(abs(p01.y - p00.y), abs(p11.y - p10.y)) * _flw_cullData.pyramidHeight;

    int level = clamp(int(ceil(log2(max(width, height)))), 0, _flw_cullData.pyramidLevels);

    ivec2 levelSize = textureSize(_flw_depthPyramid, level);

    ivec2 ip01 = ivec2(p01 * levelSize);
    ivec2 ip11 = ivec2(p11 * levelSize);
    ivec2 ip10 = ivec2(p10 * levelSize);
    ivec2 ip00 = ivec2(p00 * levelSize);

    // Clamp to the texture bounds.
    // Since we're not going through a sampler out of bounds texel fetches will return 0.
    ip01 = clamp(ip01, ivec2(0), levelSize - ivec2(1));
    ip11 = clamp(ip11, ivec2(0), levelSize - ivec2(1));
    ip10 = clamp(ip10, ivec2(0), levelSize - ivec2(1));
    ip00 = clamp(ip00, ivec2(0), levelSize - ivec2(1));

    float depth01 = texelFetch(_flw_depthPyramid, ip01, level).r;
    float depth11 = texelFetch(_flw_depthPyramid, ip11, level).r;
    float depth10 = texelFetch(_flw_depthPyramid, ip10, level).r;
    float depth00 = texelFetch(_flw_depthPyramid, ip00, level).r;

    float depth = max(max(depth00, depth01), max(depth10, depth11));
    float depthSphere = computeSphereMinDepth(center, radius, flw_projection);

    return depthSphere <= depth;
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
