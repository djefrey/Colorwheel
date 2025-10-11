in ClrwlFallbackVertexData
{
    vec4 clrwl_overlayColor;
};

#ifdef _FLW_CRUMBLING
uniform sampler2D _flw_crumblingTex;
#endif

void clrwl_computeDiscard(vec4 color)
{
    #ifdef _FLW_USE_DISCARD
    if (flw_discardPredicate(color))
    {
        discard;
    }
    #endif
}
