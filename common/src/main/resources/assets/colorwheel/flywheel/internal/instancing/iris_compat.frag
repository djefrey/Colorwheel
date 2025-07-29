#include "flywheel:internal/instancing/light.glsl"
#include "colorwheel:internal/depth.glsl"
#include "colorwheel:internal/diffuse.glsl"
#include "colorwheel:internal/oit/wavelet.glsl"

vec4 clrwl_overlayColor = vec4(0.0);

#ifdef _FLW_CRUMBLING
uniform sampler2D _flw_crumblingTex;
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

float _clrwl_diffuseFactor()
{
    if (flw_material.cardinalLightingMode == 2u)
    {
        return diffuseFromLightDirections(flw_vertexNormal);
    }
    else if (flw_material.cardinalLightingMode == 1u)
    {
        if (flw_constantAmbientLight == 1u)
        {
            return diffuseNether(flw_vertexNormal);
        }
        else
        {
            return diffuse(flw_vertexNormal);
        }
    }
    else
    {
        return 1.;
    }
}

void _clrwl_materialFragment_hook()
{
    flw_materialFragment();

    if (flw_material.useOverlay)
    {
        clrwl_overlayColor = texelFetch(flw_overlayTex, flw_fragOverlay, 0);
        clrwl_overlayColor.a = 1.0 - clrwl_overlayColor.a;
    }
}

void _clrwl_shaderLight_hook()
{
    flw_shaderLight();

    #ifdef CLRWL_OLD_LIGHTING
    flw_fragColor *= _clrwl_diffuseFactor();
    #endif
}

void clrwl_computeDiscard(vec4 color)
{
    #ifdef _FLW_USE_DISCARD
    if (flw_discardPredicate(color))
    {
        discard;
    }
    #endif
}

void clrwl_computeFragment(vec4 sampleColor, out vec4 fragColor, out vec2 fragLight, out vec4 fragOverlay)
{
    flw_sampleColor = sampleColor;
    flw_fragColor = flw_sampleColor * flw_vertexColor;
    flw_fragLight = flw_vertexLight;
    flw_fragOverlay = flw_vertexOverlay;

    _clrwl_materialFragment_hook();
    _clrwl_shaderLight_hook();

    clrwl_computeDiscard(flw_fragColor);

    fragColor = flw_fragColor;
    fragLight = flw_fragLight;
    fragOverlay = clrwl_overlayColor;
}
