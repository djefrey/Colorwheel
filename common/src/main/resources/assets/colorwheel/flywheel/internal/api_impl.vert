#include "colorwheel:internal/material.glsl"
#include "flywheel:internal/api_impl.glsl"
#include "colorwheel:internal/uniforms.glsl"

#ifndef CLRWL_IS_FALLBACK
    #define CLRWL_FLAT_OUT flat
#else
    #define CLRWL_FLAT_OUT
#endif

#ifndef CLRWL_IS_FALLBACK
out ClrwlVertexData
{
#endif
    vec4 flw_vertexPos;
    vec4 flw_vertexColor;
    vec2 flw_vertexTexCoord;
    CLRWL_FLAT_OUT ivec2 flw_vertexOverlay;
    vec2 flw_vertexLight;
    vec3 flw_vertexNormal;
    vec4 clrwl_vertexTangent;

#ifdef _FLW_DEBUG
    vec2 clrwl_vertexEntity;
    vec2 clrwl_vertexMidTexCoord;
    vec4 clrwl_vertexMidMesh;
    CLRWL_FLAT_OUT uvec2 clrwl_debugIds;
#endif

#ifndef CLRWL_IS_FALLBACK
};
#endif

#ifndef _FLW_DEBUG
vec2 clrwl_vertexEntity;
vec2 clrwl_vertexMidTexCoord;
vec4 clrwl_vertexMidMesh;
uvec2 clrwl_debugIds;
#endif

FlwMaterial flw_material;

uint flw_vertexId;

uniform sampler2D flw_diffuseTex;
uniform sampler2D flw_overlayTex;

#ifdef _FLW_CRUMBLING
uniform sampler2D _flw_crumblingTex;
#endif
