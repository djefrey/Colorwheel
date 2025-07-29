#include "flywheel:internal/packed_material.glsl"

uniform uvec2 _flw_packedMaterial;

void main() {
    _flw_unpackUint2x16(_flw_packedMaterial.x, _flw_uberFogIndex, _flw_uberCutoutIndex);
    _flw_unpackMaterialProperties(_flw_packedMaterial.y, flw_material);

    _clrwl_shader_main();

    #ifdef CLRWL_POST_SHADER
    _clrwl_post_shader();
    #endif
}
