#include "colorwheel:internal/common_geom.glsl"
#include "colorwheel:internal/packed_material.glsl"

uniform uint _clrwl_packedMaterial;

void main()
{
    #ifndef _CLRWL_IS_FALLBACK
    _flw_unpackMaterialProperties(_clrwl_packedMaterial, flw_material);
    #endif

    _clrwl_main();
}
