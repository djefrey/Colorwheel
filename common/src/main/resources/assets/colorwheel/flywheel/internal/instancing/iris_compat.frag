#include "flywheel:internal/instancing/light.glsl"
#include "flywheel:internal/depth.glsl"
#include "colorwheel:internal/oit/wavelet.glsl"

#ifdef CLRWL_OIT

uniform sampler2D _flw_depthRange;
uniform sampler2D _flw_blueNoise;

float tented_blue_noise(float normalizedDepth)
{
    float tentIn = abs(normalizedDepth * 2. - 1);
    float tentIn2 = tentIn * tentIn;
    float tentIn4 = tentIn2 * tentIn2;
    float tent = 1 - (tentIn2 * tentIn4);

    float b = texture(_flw_blueNoise, gl_FragCoord.xy / vec2(64)).r;

    return b * tent;
}

float linear_depth()
{
    return linearize_depth(gl_FragCoord.z, _flw_cullData.znear, _flw_cullData.zfar);
}

#endif
