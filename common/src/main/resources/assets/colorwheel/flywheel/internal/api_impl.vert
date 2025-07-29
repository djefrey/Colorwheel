#include "flywheel:internal/material.glsl"
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
    vec4 flw_vertexTangent;

    float flw_distance;
};

vec2 flw_vertexMidTexCoord;

FlwMaterial flw_material;

#define flw_vertexId gl_VertexID
