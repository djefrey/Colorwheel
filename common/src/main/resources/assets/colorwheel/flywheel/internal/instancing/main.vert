#include "flywheel:internal/common.vert"
#include "flywheel:internal/packed_material.glsl"
#include "flywheel:internal/instancing/light.glsl"

uniform uvec2 _flw_packedMaterial;
uniform int _flw_baseInstance = 0;

#ifdef FLW_EMBEDDED
uniform mat4 _flw_modelMatrixUniform;
uniform mat3 _flw_normalMatrixUniform;
#endif

uniform uint _flw_vertexOffset;

void main() {
    _flw_unpackMaterialProperties(_flw_packedMaterial.y, flw_material);

    FlwInstance instance = _flw_unpackInstance(_flw_baseInstance + gl_InstanceID);

    #ifdef FLW_EMBEDDED
    _flw_modelMatrix = _flw_modelMatrixUniform;
    _flw_normalMatrix = _flw_normalMatrixUniform;
    #endif

    _flw_layoutVertex();
    flw_instanceVertex(instance);
    flw_materialVertex();

    #ifdef _FLW_DEBUG
    _flw_ids = uvec2(stableInstanceID, modelID);
    #endif

    #ifdef FLW_EMBEDDED
    flw_vertexPos = _flw_modelMatrix * flw_vertexPos;
    flw_vertexNormal = _flw_normalMatrix * flw_vertexNormal;
    #endif

    flw_vertexNormal = normalize(flw_vertexNormal);
    flw_distance = fogDistance(flw_vertexPos.xyz, flw_cameraPos, flw_fogShape);

    _flw_shader_main();
}
