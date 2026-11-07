// Per culling group
#define _FLW_PAGE_FRAME_DESCRIPTOR_BUFFER_BINDING 0// transform, cull
#define _FLW_BOUNDING_SPHERE_BUFFER_BINDING 1// transform, cull
#define _FLW_INSTANCE_BUFFER_BINDING 2// transform, draw
#define _FLW_DRAW_INSTANCE_INDEX_BUFFER_BINDING 3// transform, cull, draw
#define _FLW_MODEL_BUFFER_BINDING 4// transform, cull, zero, apply, draw
#define _FLW_DRAW_BUFFER_BINDING 5// apply, draw

#define _FLW_INSTANCE_VISIBILITY_BUFFER_BINDING 6

// Global to the engine
#define _FLW_LIGHT_LUT_BUFFER_BINDING 7
#define _FLW_LIGHT_SECTIONS_BUFFER_BINDING 8

#define _FLW_MATRIX_BUFFER_BINDING 9
