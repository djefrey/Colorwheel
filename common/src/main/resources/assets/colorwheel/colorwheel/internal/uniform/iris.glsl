// #####  _FlwFogUniforms #####

uniform vec3 iris_fogColor;
uniform float iris_fogDensity;
uniform float iris_fogStart;
uniform float iris_fogEnd;
uniform int iris_fogShape;

#define flw_fogColor (vec4(iris_fogColor, iris_fogDensity)) // Not sure if fogDensity should be the alpha value
#define flw_fogRange (vec2(iris_fogStart, iris_fogEnd))
#define flw_fogShape (iris_fogShape)

// #####  _FlwPlayerUniforms #####

uniform vec3 iris_eyePosition;
uniform ivec2 iris_eyeBrightness;
uniform int iris_heldBlockLightValue;
uniform int iris_isEyeInWater;
uniform bool iris_is_sneaking;
uniform bool iris_is_on_ground;

#define flw_eyePos (iris_eyePosition)
// flw_fovOption
#define flw_eyeBrightness (vec2(iris_eyeBrightness / 15))
#define flw_heldLight ((float) (iris_heldBlockLightValue / 15))
#define flw_playerEyeInFluid (iris_isEyeInWater)
#define flw_playerEyeInBlock ((iris_isEyeInWater == 3) ? FLW_PLAYER_EYE_IN_BLOCK_POWDER_SNOW : FLW_PLAYER_EYE_IN_BLOCK_UNKNOWN)
#define flw_playerCrouching ((uint) iris_is_sneaking)
#define flw_playerFallFlying ((uint) !iris_is_on_ground)
// flw_shiftKeyDown
// flw_gameMode

#define FLW_PLAYER_EYE_IN_FLUID_WATER 1
#define FLW_PLAYER_EYE_IN_FLUID_LAVA 2
#define FLW_PLAYER_EYE_IN_FLUID_UNKNOWN 0xFFFFFFFFu

#define FLW_PLAYER_EYE_IN_BLOCK_POWDER_SNOW 1
#define FLW_PLAYER_EYE_IN_BLOCK_UNKNOWN 0xFFFFFFFFu

//  ##### _FlwLevelUniforms #####

uniform vec3 iris_skyColor;
uniform int iris_worldDay;
uniform int iris_worldTime;
uniform bool iris_hasSkylight;
uniform bool iris_sunAngle;
uniform int iris_moonPhase;
uniform float iris_rainStrength;
uniform float iris_thunderStrength;
uniform float iris_ambientLight;

#define flw_skyColor (iris_skyColor)
// flw_cloudColor
// flw_light0Direction;
// flw_light1Direction;
#define flw_levelDay ((uint) iris_worldDay)
#define flw_timeOfDay (((float) iris_worldTime) / 24000.0)
#define flw_levelHasSkyLight ((uint) iris_hasSkylight)
#define flw_sunAngle (iris_sunAngle)
// flw_moonBrightness
#define flw_moonPhase ((uint) iris_moonPhase)
// flw_isRaining
#define flw_rainLevel (iris_rainStrength)
// flw_isThundering
#define flw_thunderLevel (iris_thunderStrength)
// flw_skyDarken
#define flw_constantAmbientLight (iris_ambientLight)
// flw_dimension
