// https://github.com/IThundxr/Flywheel/blob/74bc696bbaacdf1c96bc52f03d3635d31f6b56f6/common/src/backend/resources/assets/flywheel/flywheel/internal/fog_distance.glsl

float _clrwl_sphericalDistance(vec3 worldPos, vec3 cameraPos) {
    return length(worldPos - cameraPos);
}

float _clrwl_cylindricalDistance(vec3 worldPos, vec3 cameraPos) {
    vec3 relativePos = worldPos - cameraPos;
    float distXZ = length(relativePos.xz);
    float distY = abs(relativePos.y);
    return max(distXZ, distY);
}
