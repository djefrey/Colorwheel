// https://github.com/Engine-Room/Flywheel/blob/610b1683f3ed0fef5cd387a126bd2c530a9c2ead/common/src/backend/resources/assets/flywheel/flywheel/internal/uniforms/fog.glsl

layout(std140) uniform _FlwFogUniforms {
    vec4 flw_fogColor;
    float flw_fogEnvironmentalStart;
    float flw_fogEnvironmentalEnd;
    float flw_fogRenderDistanceStart;
    float flw_fogRenderDistanceEnd;
    float flw_fogSkyEnd;
    float flw_fogCloudEnd;
};
