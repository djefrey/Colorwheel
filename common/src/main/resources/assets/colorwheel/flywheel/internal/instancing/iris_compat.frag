#include "flywheel:internal/instancing/light.glsl"
#include "colorwheel:internal/depth.glsl"
#include "colorwheel:internal/oit/wavelet.glsl"

#ifdef _FLW_CRUMBLING
uniform sampler2D _flw_crumblingTex;

in vec2 _flw_crumblingTexCoord;
#endif

#ifdef CLRWL_OIT

uniform sampler2D _flw_depthRange;
uniform sampler2D _flw_blueNoise;

float _clrwl_tented_blue_noise(float normalizedDepth)
{
    float tentIn = abs(normalizedDepth * 2. - 1);
    float tentIn2 = tentIn * tentIn;
    float tentIn4 = tentIn2 * tentIn2;
    float tent = 1 - (tentIn2 * tentIn4);

    float b = texture(_flw_blueNoise, gl_FragCoord.xy / vec2(64)).r;

    return b * tent;
}

float _clrwl_linear_depth()
{
    return _clrwl_linearize_depth(gl_FragCoord.z, _flw_cullData.znear, _flw_cullData.zfar);
}

#ifdef CLRWL_EVALUATE

float _clrwl_opaque_transmittance_from_depth(float linear, vec2 range)
{
    return linear <= -range.x + 1e-4 ? 1.0 : 0.0;
}

#endif

#endif
