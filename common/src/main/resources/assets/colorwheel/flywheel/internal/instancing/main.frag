#include "flywheel:internal/packed_material.glsl"

uniform uvec2 _flw_packedMaterial;

void main() {
    _flw_unpackUint2x16(_flw_packedMaterial.x, _flw_uberFogIndex, _flw_uberCutoutIndex);
    _flw_unpackMaterialProperties(_flw_packedMaterial.y, flw_material);

    flw_sampleColor = texture(flw_diffuseTex, flw_vertexTexCoord);
    flw_fragColor = flw_vertexColor * flw_sampleColor;
    flw_fragOverlay = flw_vertexOverlay;
    flw_fragLight = flw_vertexLight;

    flw_materialFragment();

    #ifdef _FLW_CRUMBLING
    vec4 crumblingSampleColor = texture(_flw_crumblingTex, _flw_crumblingTexCoord);

    // Make the crumbling overlay transparent when the fragment color after the material shader is transparent.
    flw_fragColor.rgb = crumblingSampleColor.rgb;
    flw_fragColor.a *= crumblingSampleColor.a;
    #endif

    flw_shaderLight();

    _flw_shader_main();

    #ifdef _FLW_USE_DISCARD
    // flw_fragColor is set to iris_FragData0
    if (flw_discardPredicate(flw_fragColor)) {
        discard;
    }
    #endif
}
