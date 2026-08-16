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

#ifdef _CLRWL_OCCLUSION_CULLING
layout(binding = 0) uniform sampler2D _flw_depthPyramid;
#endif

bool _flw_isVisible(uint instanceIndex, uint modelIndex)
{
    vec3 center;
    float radius;
    _flw_unpackBoundingSphere(_flw_boundingSpheres[instanceIndex], center, radius);

    bool isVisible = true;

#ifdef _CLRWL_FRUSTUM_CULLING
    isVisible = _flw_testSphere(center, radius);
#endif

#ifdef _CLRWL_OCCLUSION_CULLING
    if (isVisible)
    {
        transformBoundingSphere(flw_view, center, radius);

        vec4 aabb;
        if (projectSphere(center, radius, _flw_cullData.znear, _flw_cullData.P00, _flw_cullData.P11, _flw_cullData.orthographic == 1, aabb))
        {
            aabb = clipToUV(aabb);

            float width = (aabb.z - aabb.x) * _flw_cullData.pyramidWidth;
            float height = (aabb.w - aabb.y) * _flw_cullData.pyramidHeight;

            int level = clamp(int(ceil(log2(max(width, height)))), 0, _flw_cullData.pyramidLevels);

            ivec2 levelSize = textureSize(_flw_depthPyramid, level);

            ivec4 levelSizePair = ivec4(levelSize, levelSize);

            ivec4 bounds = ivec4(aabb * vec4(levelSizePair));

            // Clamp to the texture bounds.
            // Since we're not going through a sampler out of bounds texel fetches will return 0.
            bounds = clamp(bounds, ivec4(0), levelSizePair - ivec4(1));

            float depth01 = texelFetch(_flw_depthPyramid, bounds.xw, level).r;
            float depth11 = texelFetch(_flw_depthPyramid, bounds.zw, level).r;
            float depth10 = texelFetch(_flw_depthPyramid, bounds.zy, level).r;
            float depth00 = texelFetch(_flw_depthPyramid, bounds.xy, level).r;

            float depth = max(max(depth00, depth01), max(depth10, depth11));
            float depthSphere = computeSphereMinDepth(center, radius, flw_projection);

            isVisible = isVisible && depthSphere <= depth;
        }
    }
#endif

    return isVisible;
}

uniform uint clrwl_materialFilter;

void main()
{
    uint pageIndex = gl_WorkGroupID.x << 1u;

    if (pageIndex >= _flw_pageFrameDescriptors.length())
    {
        return;
    }

    uint modelIndex = _flw_pageFrameDescriptors[pageIndex];
    uint materialBitset = _clrwl_unpackMaterialBitset(_flw_models[modelIndex]);

    if ((materialBitset & clrwl_materialFilter) == 0)
    {
        return;
    }

    uint pageValidity = _flw_pageFrameDescriptors[pageIndex + 1];
    uint localInvocationMask = 1u << gl_LocalInvocationID.x;

    if ((localInvocationMask & pageValidity) == 0)
    {
        return;
    }

    uint instanceIndex = gl_GlobalInvocationID.x;

    if (_flw_isVisible(instanceIndex, modelIndex))
    {
        uint localIndex = atomicAdd(_flw_models[modelIndex].instanceCount, 1);
        uint targetIndex = _flw_models[modelIndex].baseInstance + localIndex;
        _flw_instanceIndices[targetIndex] = instanceIndex;
    }
}
