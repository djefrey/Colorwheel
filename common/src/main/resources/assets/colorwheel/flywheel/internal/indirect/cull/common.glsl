#include "colorwheel:internal/uniforms.glsl"
#include "flywheel:util/matrix.glsl"

#ifdef _CLRWL_IS_GBUFFERS_PASS
// Disgustingly vectorized sphere frustum intersection taking advantage of ahead of time packing.
// Only uses 6 fmas and some boolean ops.
// See also:
// flywheel:uniform/flywheel.glsl
// dev.engine_room.flywheel.lib.math.MatrixMath.writePackedFrustumPlanes
// org.joml.FrustumIntersection.testSphere
bool _flw_testSphere(vec3 center, float radius)
{
    bvec4 xyInside = greaterThanEqual(fma(flw_frustumPlanes.xyX, center.xxxx, fma(flw_frustumPlanes.xyY, center.yyyy, fma(flw_frustumPlanes.xyZ, center.zzzz, flw_frustumPlanes.xyW))), -radius.xxxx);
    bvec2 zInside = greaterThanEqual(fma(flw_frustumPlanes.zX, center.xx, fma(flw_frustumPlanes.zY, center.yy, fma(flw_frustumPlanes.zZ, center.zz, flw_frustumPlanes.zW))), -radius.xx);

    return all(xyInside) && all(zInside);
}
#else
bool _clrwl_testSphereOn4Planes(vec3 center, float radius, _ClrwlShadowFrustumPlanesGroup planes)
{
    return all(greaterThanEqual(fma(planes.X, center.xxxx, fma(planes.Y, center.yyyy, fma(planes.Z, center.zzzz, planes.W))), -radius.xxxx));
}

bool _clrwl_testSphereOn3Planes(vec3 center, float radius, _ClrwlShadowFrustumPlanesGroup planes)
{
    return all(greaterThanEqual(fma(planes.X.xyz, center.xxx, fma(planes.Y.xyz, center.yyy, fma(planes.Z.xyz, center.zzz, planes.W.xyz))), -radius.xxx));
}
#endif

bool _clrwl_isSphereInCube(vec3 center, float radius, vec3 origin, float halfLength)
{
    vec3 v = abs(center - origin);
    float maxD = max(v.x, max(v.y, v.z));
    return maxD <= halfLength - radius;
}

#ifdef _CLRWL_OCCLUSION_CULLING

bool projectSphere(vec3 c, float r, float znear, float P00, float P11, bool orthographic, out vec4 aabb)
{
    // Closest point on the sphere is between the camera and the near plane, don't even attempt to cull.
    if (c.z + r > -znear)
    {
        return false;
    }

    if (orthographic)
    {
        aabb = vec4(c.x - r,  c.y - r, c.x + r, c.y + r);
    }
    else
    {
        vec3 cr = c * r;
        float czr2 = c.z * c.z - r * r;

        float vx = sqrt(c.x * c.x + czr2);
        float minx = (vx * c.x - cr.z) / (vx * c.z + cr.x);
        float maxx = (vx * c.x + cr.z) / (vx * c.z - cr.x);

        float vy = sqrt(c.y * c.y + czr2);
        float miny = (vy * c.y + cr.z) / (vy * c.z - cr.y);
        float maxy = (vy * c.y - cr.z) / (vy * c.z + cr.y);

        aabb = -vec4(minx, miny, maxx, maxy);
    }

    aabb = aabb * vec4(P00, P11, P00, P11);

    return true;
}

vec2 clipToUV(vec2 p)
{
    return p * vec2(0.5f) + vec2(0.5f); // clip space -> uv space
}

vec4 clipToUV(vec4 bounds)
{
    return bounds * vec4(0.5f) + vec4(0.5f); // clip space -> uv space
}

float computeSphereMinDepth(vec3 c, float r, mat4 p)
{
    float z = c.z + r;
    float clip_z = fma(z, p[2][2], p[3][2]);
    float clip_w = fma(z, p[2][3], p[3][3]);

    #ifdef _CLRWL_IS_SHADOW_PASS
    clip_z = clrwl_distortShadowClipZ(clip_z);
    #endif

    return (clip_z / clip_w) / 2.0 + 0.5;
}

#endif
