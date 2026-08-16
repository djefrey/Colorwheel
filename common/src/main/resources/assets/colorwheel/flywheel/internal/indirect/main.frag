#include "colorwheel:internal/common.frag"
#include "colorwheel:internal/indirect/buffer_bindings.glsl"
#include "colorwheel:internal/indirect/light.glsl"

void main()
{
    _flw_unpackMaterialProperties(_clrwl_packedMaterial, flw_material);
    _clrwl_main();
}
