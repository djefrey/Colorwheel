#ifdef _CLRWL_HAS_SABLE

struct FlwMatrices
{
    mat4 pose;
    vec4 normalA;
    vec4 normalB;
    vec4 normalC;
    float skyLightScale;
    uint sceneID;
    float _padding1;
    float _padding2;
    mat4 lightingSceneMatrix;
};

void _flw_unpackMatrices(in FlwMatrices mats, out mat4 pose, out mat3 normal, out uint lightingSceneId, out float skyLightScale, out mat4 lightingSceneMatrix)
{
    pose = mats.pose;
    normal = mat3(mats.normalA.xyz, mats.normalB.xyz, mats.normalC.xyz);
    lightingSceneId = mats.sceneID;
    skyLightScale = mats.skyLightScale;
    lightingSceneMatrix = mats.lightingSceneMatrix;
}

#else

struct FlwMatrices
{
    mat4 pose;
    vec4 normalA;
    vec4 normalB;
    vec4 normalC;
};

void _flw_unpackMatrices(in FlwMatrices mats, out mat4 pose, out mat3 normal)
{
    pose = mats.pose;
    normal = mat3(mats.normalA.xyz, mats.normalB.xyz, mats.normalC.xyz);
}

#endif
