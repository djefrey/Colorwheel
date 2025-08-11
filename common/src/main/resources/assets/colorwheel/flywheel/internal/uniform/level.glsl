layout(std140) uniform _ClrwlLevelUniforms
{
    vec4 flw_cloudColor;

    vec4 _flw_light0Direction;
    vec4 _flw_light1Direction;

    float flw_moonBrightness;

    float flw_skyDarken;

/** Use FLW_DIMENSION_* ids to determine the dimension. May eventually be implemented for custom dimensions. */
    uint flw_dimension;
};

#define flw_light0Direction (_flw_light0Direction.xyz)
#define flw_light1Direction (_flw_light1Direction.xyz)

#define FLW_DIMENSION_OVERWORLD 0
#define FLW_DIMENSION_NETHER 1
#define FLW_DIMENSION_END 2
#define FLW_DIMENSION_UNKNOWN 0xFFFFFFFFu
