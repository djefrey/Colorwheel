#ifdef CLRWL_IS_FALLBACK
out ClrwlFallbackVertexData
{
    vec4 clrwl_overlayColor;
};
#else
vec4 clrwl_overlayColor = vec4(0.0);
#endif

vec4 ftransform()
{
    return flw_viewProjection * flw_vertexPos;
}
