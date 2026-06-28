#include "colorwheel:internal/common.vert"
#include "colorwheel:internal/packed_material.glsl"
#include "colorwheel:internal/instancing/light.glsl"

uniform uint _clrwl_packedMaterial;
uniform int _flw_baseInstance = 0;

#ifdef FLW_EMBEDDED
    uniform mat4 _flw_modelMatrixUniform;
    uniform mat3 _flw_normalMatrixUniform;

    #ifdef _CLRWL_HAS_SABLE
        uniform uint _flw_lightingSceneUniform;
        uniform float _flw_lightingSkyLightScaleUniform;
        uniform mat4 _flw_lightingSceneMatrixUniform;
    #endif
#endif

uniform uint _flw_baseVertex;

uniform vec4 _clrwl_meshCenterUniform;

void main()
{
    _flw_unpackMaterialProperties(_clrwl_packedMaterial, flw_material);

    FlwInstance instance = _flw_unpackInstance(_flw_baseInstance + gl_InstanceID);

    #ifdef FLW_EMBEDDED
        _flw_modelMatrix = _flw_modelMatrixUniform;
        _flw_normalMatrix = _flw_normalMatrixUniform;

        #ifdef _CLRWL_HAS_SABLE
            _flw_lightingSceneId = _flw_lightingSceneUniform;
            _flw_lightingSkyLightScale = _flw_lightingSkyLightScaleUniform;
            _flw_lightingSceneMatrix = _flw_lightingSceneMatrixUniform;
        #endif
    #endif

    _clrwl_meshCenter = _clrwl_meshCenterUniform;

    _clrwl_main(instance, gl_InstanceID, _flw_baseVertex);
}
