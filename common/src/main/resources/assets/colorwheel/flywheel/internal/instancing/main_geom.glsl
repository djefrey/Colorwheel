#include "flywheel:internal/packed_material.glsl"

uniform uvec2 _flw_packedMaterial;

FlwMaterial flw_material;

void main()
{
    _flw_unpackMaterialProperties(_flw_packedMaterial.y, flw_material);

    if (flw_material.useOverlay)
    {
        clrwl_overlayColor = texelFetch(flw_overlayTex, clrwl_in[0].flw_vertexOverlay, 0);
        clrwl_overlayColor.a = 1.0 - clrwl_overlayColor.a;
    }

    _clrwl_shader_main();
}
