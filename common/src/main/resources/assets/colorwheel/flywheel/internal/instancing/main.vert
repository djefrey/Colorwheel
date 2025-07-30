#include "flywheel:internal/packed_material.glsl"
#include "flywheel:internal/instancing/light.glsl"
#include "colorwheel:internal/fog_distance.glsl"

#ifdef _FLW_CRUMBLING
const int _CLRWL_DOWN = 0;
const int _CLRWL_UP = 1;
const int _CLRWL_NORTH = 2;
const int _CLRWL_SOUTH = 3;
const int _CLRWL_WEST = 4;
const int _CLRWL_EAST = 5;

// based on net.minecraftforge.client.ForgeHooksClient.getNearestStable
int _clrwl_getNearestFacing(vec3 normal) {
    float maxAlignment = -2;
    int face = 2;

    // Calculate the alignment of the normal vector with each axis.
    // Note that `-dot(normal, axis) == dot(normal, -axis)`.
    vec3 alignment = vec3(
    dot(normal, vec3(1., 0., 0.)),
    dot(normal, vec3(0., 1., 0.)),
    dot(normal, vec3(0., 0., 1.))
    );

    if (-alignment.y > maxAlignment) {
        maxAlignment = -alignment.y;
        face = _CLRWL_DOWN;
    }
    if (alignment.y > maxAlignment) {
        maxAlignment = alignment.y;
        face = _CLRWL_UP;
    }
    if (-alignment.z > maxAlignment) {
        maxAlignment = -alignment.z;
        face = _CLRWL_NORTH;
    }
    if (alignment.z > maxAlignment) {
        maxAlignment = alignment.z;
        face = _CLRWL_SOUTH;
    }
    if (-alignment.x > maxAlignment) {
        maxAlignment = -alignment.x;
        face = _CLRWL_WEST;
    }
    if (alignment.x > maxAlignment) {
        maxAlignment = alignment.x;
        face = _CLRWL_EAST;
    }

    return face;
}

vec2 _clrwl_getCrumblingTexCoord() {
    switch (_clrwl_getNearestFacing(flw_vertexNormal)) {
        case _CLRWL_DOWN: return vec2(flw_vertexPos.x, -flw_vertexPos.z);
        case _CLRWL_UP: return vec2(flw_vertexPos.x, flw_vertexPos.z);
        case _CLRWL_NORTH: return vec2(-flw_vertexPos.x, -flw_vertexPos.y);
        case _CLRWL_SOUTH: return vec2(flw_vertexPos.x, -flw_vertexPos.y);
        case _CLRWL_WEST: return vec2(-flw_vertexPos.z, -flw_vertexPos.y);
        case _CLRWL_EAST: return vec2(flw_vertexPos.z, -flw_vertexPos.y);
    }

    // default to north
    return vec2(-flw_vertexPos.x, -flw_vertexPos.y);
}
#endif

uniform uvec2 _flw_packedMaterial;
uniform int _flw_baseInstance = 0;

#ifdef FLW_EMBEDDED
uniform mat4 _flw_modelMatrixUniform;
uniform mat3 _flw_normalMatrixUniform;
#endif

uniform uint _flw_vertexOffset;

uniform vec3 _clrwl_meshCenter;

void main()
{
    _flw_unpackMaterialProperties(_flw_packedMaterial.y, flw_material);

    FlwInstance instance = _flw_unpackInstance(_flw_baseInstance + gl_InstanceID);

    #ifdef FLW_EMBEDDED
    mat4 _flw_modelMatrix = _flw_modelMatrixUniform;
    mat3 _flw_normalMatrix = _flw_normalMatrixUniform;
    #endif

    _clrwl_layoutVertex();

    // --- Compute mesh center and vertex tangent

    vec4 flw_vertexPos_bkp = flw_vertexPos;
    vec4 flw_vertexColor_bkp = flw_vertexColor;
    vec2 flw_vertexTexCoord_bkp = flw_vertexTexCoord;
    ivec2 flw_vertexOverlay_bkp = flw_vertexOverlay;
    vec2 flw_vertexLight_bkp = flw_vertexLight;
    vec3 flw_vertexNormal_bkp = flw_vertexNormal;

    flw_vertexPos = vec4(_clrwl_meshCenter, 1.0);
    flw_vertexNormal = flw_vertexTangent.xyz;

    flw_instanceVertex(instance);

    vec4 transformedMeshCenter = flw_vertexPos;
    float emission = flw_vertexLight_bkp.x;
    flw_vertexTangent.xyz = flw_vertexNormal;

    flw_vertexPos = flw_vertexPos_bkp;
    flw_vertexColor = flw_vertexColor_bkp;
    flw_vertexTexCoord = flw_vertexTexCoord_bkp;
    flw_vertexOverlay = flw_vertexOverlay_bkp;
    flw_vertexLight = flw_vertexLight_bkp;
    flw_vertexNormal = flw_vertexNormal_bkp;

    // ---

    flw_instanceVertex(instance);
    flw_materialVertex();

    flw_atMidBlock = vec4((transformedMeshCenter.xyz - flw_vertexPos.xyz) * 64.0, emission * 15.0);

    #ifdef _FLW_CRUMBLING
    flw_vertexTexCoord = _clrwl_getCrumblingTexCoord();
    #endif

    #ifdef _FLW_DEBUG
    _flw_ids = uvec2(stableInstanceID, modelID);
    #endif

    #ifdef FLW_EMBEDDED
    flw_vertexPos = _flw_modelMatrix * flw_vertexPos;
    flw_vertexNormal = _flw_normalMatrix * flw_vertexNormal;
    flw_vertexTangent = vec4(_flw_normalMatrix * flw_vertexTangent.xyz, flw_vertexTangent.w);
    #endif

    flw_vertexNormal = normalize(flw_vertexNormal);
    flw_distance = _clrwl_fogDistance(flw_vertexPos.xyz, flw_cameraPos, flw_fogShape);

    _clrwl_shader_main();
}
