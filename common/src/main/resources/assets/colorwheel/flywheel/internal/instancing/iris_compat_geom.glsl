vec4 clrwl_overlayColor = vec4(0.0);

#ifdef _FLW_CRUMBLING
uniform sampler2D _flw_crumblingTex;
#endif

void clrwl_setVertexOut(int i)
{
    clrwl_out.flw_vertexPos = clrwl_in[i].flw_vertexPos;
    clrwl_out.flw_vertexColor = clrwl_in[i].flw_vertexColor;
    clrwl_out.flw_vertexTexCoord = clrwl_in[i].flw_vertexTexCoord;
    clrwl_out.flw_vertexOverlay = clrwl_in[i].flw_vertexOverlay;
    clrwl_out.flw_vertexLight = clrwl_in[i].flw_vertexLight;
    clrwl_out.flw_vertexNormal = clrwl_in[i].flw_vertexNormal;
    clrwl_out.clrwl_vertexTangent = clrwl_in[i].clrwl_vertexTangent;

#ifdef _FLW_DEBUG
    clrwl_out.clrwl_vertexEntity = clrwl_in[i].clrwl_vertexEntity;
    clrwl_out.clrwl_vertexMidTexCoord = clrwl_in[i].clrwl_vertexMidTexCoord;
    clrwl_out.clrwl_vertexMidMesh = clrwl_in[i].clrwl_vertexMidMesh;
    clrwl_out.clrwl_debugIds = clrwl_in[i].clrwl_debugIds;
#endif
}
