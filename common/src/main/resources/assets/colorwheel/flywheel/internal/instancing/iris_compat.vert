vec4 ftransform()
{
    return flw_viewProjection * flw_vertexPos;
}

#ifdef _FLW_CRUMBLING
uniform sampler2D _flw_crumblingTex;
#endif

vec4 clrwl_overlayColor = vec4(0.0);
