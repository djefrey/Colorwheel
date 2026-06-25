#include "colorwheel:internal/common.vert"
#include "colorwheel:internal/packed_material.glsl"
#include "colorwheel:internal/indirect/buffer_bindings.glsl"
#include "colorwheel:internal/indirect/draw_command.glsl"
#include "colorwheel:internal/indirect/light.glsl"
#include "colorwheel:internal/indirect/matrices.glsl"

layout(std430, binding = _FLW_DRAW_INSTANCE_INDEX_BUFFER_BINDING) restrict readonly buffer TargetBuffer
{
    uint _flw_instanceIndices[];
};

layout(std430, binding = _FLW_DRAW_BUFFER_BINDING) restrict readonly buffer DrawBuffer
{
    FlwMeshDrawCommand _flw_drawCommands[];
};

#ifdef FLW_EMBEDDED
layout(std430, binding = _FLW_MATRIX_BUFFER_BINDING) restrict buffer MatrixBuffer
{
    FlwMatrices _flw_matrices[];
};
#endif

uniform uint _flw_baseDraw;

#if __VERSION__ < 460
#define flw_baseInstance gl_BaseInstanceARB
#define flw_drawId gl_DrawIDARB
#else
#define flw_baseInstance gl_BaseInstance
#define flw_drawId gl_DrawID
#endif

void main()
{
    uint drawIndex = flw_drawId + _flw_baseDraw;
    FlwMeshDrawCommand draw = _flw_drawCommands[drawIndex];

    uint packedMaterialProperties = draw.packedMaterialProperties;
    _flw_unpackMaterialProperties(packedMaterialProperties, flw_material);
    _clrwl_packedMaterial = packedMaterialProperties;

    #ifdef FLW_EMBEDDED
    _flw_unpackMatrices(_flw_matrices[draw.matrixIndex], _flw_modelMatrix, _flw_normalMatrix);
    #endif

    #ifdef _FLW_CRUMBLING
    uint instanceIndex = flw_baseInstance;
    #else
    uint instanceIndex = _flw_instanceIndices[flw_baseInstance + gl_InstanceID];
    #endif

    FlwInstance instance = _flw_unpackInstance(instanceIndex);

    _clrwl_main(instance, instanceIndex, draw.vertexOffset);
}
