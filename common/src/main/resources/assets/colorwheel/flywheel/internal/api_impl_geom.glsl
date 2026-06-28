#include "colorwheel:internal/material.glsl"
#include "colorwheel:internal/uniforms.glsl"

#ifndef _CLRWL_IS_FALLBACK
in ClrwlVertexData
{
    vec4 flw_vertexPos;
    vec4 flw_vertexColor;
    vec2 flw_vertexTexCoord;
    flat ivec2 flw_vertexOverlay;
    vec2 flw_vertexLight;
    vec3 flw_vertexNormal;
    vec4 clrwl_vertexTangent;

#ifdef FLW_EMBEDDED
    #ifdef _CLRWL_HAS_SABLE
        flat uint flw_vertexLightingSceneId;
        flat float flw_skyLightScale;
        vec4 flw_vertexLightingPos;
    #endif
#endif

#ifdef CLRWL_IS_INDIRECT
    flat uint _clrwl_packedMaterial;
#endif

#ifdef _FLW_DEBUG
    vec2 clrwl_vertexEntity;
    vec2 clrwl_vertexMidTexCoord;
    vec4 clrwl_vertexMidMesh;
    flat uvec2 clrwl_debugIds;
#endif
} clrwl_in[3];

out ClrwlVertexData
{
    vec4 flw_vertexPos;
    vec4 flw_vertexColor;
    vec2 flw_vertexTexCoord;
    flat ivec2 flw_vertexOverlay;
    vec2 flw_vertexLight;
    vec3 flw_vertexNormal;
    vec4 clrwl_vertexTangent;

#ifdef FLW_EMBEDDED
    #ifdef _CLRWL_HAS_SABLE
        flat uint flw_vertexLightingSceneId;
        flat float flw_skyLightScale;
        vec4 flw_vertexLightingPos;
    #endif
#endif

#ifdef CLRWL_IS_INDIRECT
    flat uint _clrwl_packedMaterial;
#endif

#ifdef _FLW_DEBUG
    vec2 clrwl_vertexEntity;
    vec2 clrwl_vertexMidTexCoord;
    vec4 clrwl_vertexMidMesh;
    flat uvec2 clrwl_debugIds;
#endif
} clrwl_out;

vec4 clrwl_overlayColor = vec4(0.0);

#else // _CLRWL_IS_FALLBACK

in ClrwlFallbackVertexData
{
    vec4 clrwl_overlayColor;

#ifdef CLRWL_IS_INDIRECT
    flat uint _clrwl_packedMaterial;
#endif
} clrwl_in[3];

out ClrwlFallbackVertexData
{
    vec4 clrwl_overlayColor;

#ifdef CLRWL_IS_INDIRECT
    flat uint _clrwl_packedMaterial;
#endif
} clrwl_out;
#endif

uniform sampler2D flw_diffuseTex;
uniform sampler2D flw_overlayTex;

#ifdef _FLW_CRUMBLING
uniform sampler2D _flw_crumblingTex;
#endif
