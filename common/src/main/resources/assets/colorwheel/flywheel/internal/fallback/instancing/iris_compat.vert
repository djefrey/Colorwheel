out ClrwlFallbackVertexData
{
    vec4 clrwl_overlayColor;
};

vec4 ftransform()
{
    return flw_viewProjection * flw_vertexPos;
}
