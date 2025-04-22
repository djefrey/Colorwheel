#include "colorwheel:internal/uniform.glsl"

vec4 ftransform()
{
    return flw_viewProjection * flw_vertexPos;
}
