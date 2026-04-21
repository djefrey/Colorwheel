#include "colorwheel:internal/material.glsl"
#include "flywheel:internal/api_impl.glsl"
#include "colorwheel:internal/uniforms.glsl"

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
    #ifdef HAS_SABLE
        flat uint flw_vertexLightingSceneId;
        flat float flw_skyLightScale;
        vec4 flw_vertexLightingPos;
    #endif
#endif

#ifdef _FLW_DEBUG
    vec2 clrwl_vertexEntity;
    vec2 clrwl_vertexMidTexCoord;
    vec4 clrwl_vertexMidMesh;
    flat uvec2 clrwl_debugIds;
#endif
};

#ifndef _FLW_DEBUG
vec2 clrwl_vertexEntity;
vec2 clrwl_vertexMidTexCoord;
vec4 clrwl_vertexMidMesh;
#endif

FlwMaterial flw_material;

uint flw_vertexId;

uniform sampler2D flw_diffuseTex;
uniform sampler2D flw_overlayTex;
