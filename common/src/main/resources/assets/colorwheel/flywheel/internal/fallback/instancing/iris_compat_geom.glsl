in ClrwlFallbackVertexData
{
    vec4 clrwl_overlayColor;
} clrwl_in[3];

out ClrwlFallbackVertexData
{
    vec4 clrwl_overlayColor;
} clrwl_out;

#ifdef _FLW_CRUMBLING
uniform sampler2D _flw_crumblingTex;
#endif

vec4 clrwl_overlayColor;