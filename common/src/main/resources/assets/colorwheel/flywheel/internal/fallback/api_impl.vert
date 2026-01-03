#include "colorwheel:internal/material.glsl"
#include "flywheel:internal/api_impl.glsl"
#include "colorwheel:internal/uniforms.glsl"

vec4 flw_vertexPos;
vec4 flw_vertexColor;
vec2 flw_vertexTexCoord;
ivec2 flw_vertexOverlay;
vec2 flw_vertexLight;
vec3 flw_vertexNormal;
vec4 clrwl_vertexTangent;

vec2 clrwl_vertexEntity;
vec2 clrwl_vertexMidTexCoord;
vec4 clrwl_vertexMidMesh;
uvec2 clrwl_debugIds;

FlwMaterial flw_material;

#define flw_vertexId gl_VertexID

uniform sampler2D flw_diffuseTex;
uniform sampler2D flw_overlayTex;
