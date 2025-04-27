#include "flywheel:internal/packed_material.glsl"

uniform uvec2 _flw_packedMaterial;

void main() {
    _flw_unpackUint2x16(_flw_packedMaterial.x, _flw_uberFogIndex, _flw_uberCutoutIndex);
    _flw_unpackMaterialProperties(_flw_packedMaterial.y, flw_material);

    _flw_shader_main();

    #ifdef _FLW_USE_DISCARD
    // flw_fragColor is set to iris_FragData0
    if (flw_discardPredicate(flw_fragColor)) {
        discard;
    }
    #endif
}
