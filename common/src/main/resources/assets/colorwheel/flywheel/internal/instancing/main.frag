#include "flywheel:internal/packed_material.glsl"
#include "colorwheel:internal/fog_distance.glsl"

uniform uvec2 _flw_packedMaterial;

void main() {
    _flw_unpackUint2x16(_flw_packedMaterial.x, _flw_uberFogIndex, _flw_uberCutoutIndex);
    _flw_unpackMaterialProperties(_flw_packedMaterial.y, flw_material);

    vec3 screenPos = vec3(gl_FragCoord.xy / flw_viewportSize, gl_FragCoord.z);
    vec3 ndc = screenPos * 2.0 - 1.0;
    vec4 viewPos = flw_projectionInverse * vec4(ndc, 1.0);
    viewPos /= viewPos.w;
    vec4 flwPos = flw_viewInverse * viewPos;

    flw_distance = _clrwl_fogDistance(flwPos.xyz, flw_cameraPos, flw_fogShape);

    _clrwl_shader_main();

    #ifdef CLRWL_POST_SHADER
    _clrwl_post_shader();
    #endif
}
