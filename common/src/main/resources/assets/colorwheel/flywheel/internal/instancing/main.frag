#include "colorwheel:internal/common.frag"
#include "flywheel:internal/instancing/light.glsl"

uniform uint _clrwl_packedMaterial;

void main()
{
    _flw_unpackMaterialProperties(_clrwl_packedMaterial, flw_material);
    _clrwl_main();
}
